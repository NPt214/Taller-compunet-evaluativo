package chat.server.privatechat;

import PrivateChatApp.DeliveryReceipt;
import PrivateChatApp.NicknameInUseException;
import PrivateChatApp.PrivateCallbackPrx;
import PrivateChatApp.PrivateChat;
import PrivateChatApp.PrivateChatException;
import PrivateChatApp.PrivateMessage;
import PrivateChatApp.UserNotFoundException;
import PrivateChatApp.UserOfflineException;
import com.zeroc.Ice.ACMClose;
import com.zeroc.Ice.ACMHeartbeat;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Connection;
import com.zeroc.Ice.Current;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class PrivateChatI implements PrivateChat {

    static final int SESSION_TIMEOUT_SECONDS = 10;
    private static final int CALLBACK_TIMEOUT_MS = 5000;

    private final Map<String, PrivateSession> sessions = new ConcurrentHashMap<>();
    // Todo nickname que alguna vez inicio sesion: distingue "no existe" de "desconectado"
    private final Set<String> knownUsers = ConcurrentHashMap.newKeySet();
    private final AtomicLong idCounter = new AtomicLong(0);
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void login(String nickname, PrivateCallbackPrx callback, Current current)
            throws PrivateChatException {

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new PrivateChatException("El nickname no puede ser nulo o vacio.");
        }
        if (callback == null) {
            throw new PrivateChatException(
                    "Debe registrar un proxy de callback para iniciar sesion.");
        }

        String cleanNick = nickname.trim();

        if (sessions.containsKey(cleanNick)) {
            throw new NicknameInUseException(
                    "El nickname '" + cleanNick + "' ya se encuentra conectado.", cleanNick);
        }

        PrivateCallbackPrx cb = callback.ice_invocationTimeout(CALLBACK_TIMEOUT_MS);

        try {
            cb.ice_ping();
        } catch (com.zeroc.Ice.LocalException e) {
            throw new PrivateChatException(
                    "El servidor no pudo contactar el callback del cliente: "
                            + e.getClass().getSimpleName());
        }

        Connection con = current.con;
        PrivateSession session = new PrivateSession(cleanNick, cb, con);

        if (sessions.putIfAbsent(cleanNick, session) != null) {
            throw new NicknameInUseException(
                    "El nickname '" + cleanNick + "' ya se encuentra conectado.", cleanNick);
        }

        knownUsers.add(cleanNick);
        watchConnection(con);

        log("Usuario conectado: " + cleanNick + " (en linea: " + sessions.size() + ")");
    }

    @Override
    public void logout(String nickname, Current current) {
        if (nickname == null) {
            return;
        }
        PrivateSession session = sessions.get(nickname.trim());
        if (session != null) {
            endSession(session, "cerro sesion");
        }
    }

    @Override
    public String[] getOnlineUsers(Current current) {
        return sessions.keySet().stream().sorted().toArray(String[]::new);
    }

    @Override
    public DeliveryReceipt sendPrivateMessage(String sender, String recipient, String text,
                                              Current current) throws PrivateChatException {

        PrivateSession from = requireSession(sender, current);

        if (recipient == null || recipient.trim().isEmpty()) {
            throw new PrivateChatException("Debe indicar el nickname del destinatario.");
        }
        String to = recipient.trim();

        if (to.equals(from.getNickname())) {
            throw new PrivateChatException("No puede enviarse un mensaje privado a si mismo.");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new PrivateChatException("El mensaje no puede estar vacio.");
        }

        PrivateSession target = sessions.get(to);
        if (target == null) {
            if (knownUsers.contains(to)) {
                throw new UserOfflineException(
                        "El usuario '" + to + "' se desconecto y no puede recibir mensajes.", to);
            }
            throw new UserNotFoundException(
                    "El usuario '" + to + "' no existe en el servidor.", to);
        }

        long id = idCounter.incrementAndGet();
        String now = now();
        PrivateMessage msg = new PrivateMessage(id, from.getNickname(), to, text.trim(), now);

        notifyClient(target, prx -> prx.onPrivateMessageAsync(msg))
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log("Fallo la entrega del privado #" + id + " " + msg.sender
                                + " -> " + to + ": " + ex.getClass().getSimpleName());
                        PrivateSession senderNow = sessions.get(msg.sender);
                        if (senderNow != null) {
                            notifyClient(senderNow, prx -> prx.onPrivateMessageFailedAsync(
                                    id, to, "el destinatario se desconecto durante el envio"));
                        }
                    }
                });

        log("Privado #" + id + " " + msg.sender + " -> " + to);
        return new DeliveryReceipt(id, to, now);
    }

    private PrivateSession requireSession(String nickname, Current current)
            throws PrivateChatException {
        PrivateSession s = nickname == null ? null : sessions.get(nickname.trim());
        if (s == null) {
            throw new PrivateChatException("Acceso denegado: el usuario debe iniciar sesion primero.");
        }
        if (s.getConnection() != null && current.con != null
                && s.getConnection() != current.con) {
            throw new PrivateChatException("Acceso denegado: la sesion de '"
                    + s.getNickname() + "' pertenece a otro cliente.");
        }
        return s;
    }

    private void watchConnection(Connection con) {
        if (con == null) {
            return;
        }
        try {
            con.setACM(OptionalInt.of(SESSION_TIMEOUT_SECONDS),
                    Optional.of(ACMClose.CloseOnIdleForceful),
                    Optional.of(ACMHeartbeat.HeartbeatOff));
            con.setCloseCallback(closed -> {
                for (PrivateSession s : sessions.values()) {
                    if (s.getConnection() == closed) {
                        endSession(s, "conexion perdida");
                    }
                }
            });
        } catch (com.zeroc.Ice.LocalException e) {
            log("No se pudo vigilar la conexion: " + e);
        }
    }

    private void endSession(PrivateSession session, String reason) {
        String nick = session.getNickname();
        if (!sessions.remove(nick, session)) {
            return;
        }
        log("Usuario desconectado: " + nick + " [" + reason + "] (en linea: "
                + sessions.size() + ")");
    }

    private <T> CompletableFuture<T> notifyClient(
            PrivateSession s, Function<PrivateCallbackPrx, CompletableFuture<T>> call) {
        CompletableFuture<T> future;
        try {
            future = call.apply(s.getCallback());
        } catch (com.zeroc.Ice.LocalException e) {
            future = CompletableFuture.failedFuture(e);
        }
        future.whenComplete((r, ex) -> {
            if (ex instanceof com.zeroc.Ice.LocalException
                    && !(ex instanceof CommunicatorDestroyedException)) {
                endSession(s, "callback inalcanzable: " + ex.getClass().getSimpleName());
            }
        });
        return future;
    }

    private String now() {
        return LocalTime.now().format(timeFormat);
    }

    private void log(String text) {
        System.out.println("[" + now() + "] [PRIVATE-SERVER] " + text);
    }
}
