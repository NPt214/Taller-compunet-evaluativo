package chat.server;

import ChatApp.ChatException;
import ChatApp.ChatMessage;
import ChatApp.ChatRoom;
import ChatApp.ClientCallbackPrx;
import ChatApp.DeliveryReceipt;
import ChatApp.NicknameInUseException;
import ChatApp.PrivateMessage;
import ChatApp.UserNotFoundException;
import ChatApp.UserOfflineException;
import com.zeroc.Ice.ACMClose;
import com.zeroc.Ice.ACMHeartbeat;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Connection;
import com.zeroc.Ice.Current;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class ChatRoomI implements ChatRoom {

    // Segundos sin trafico (ni heartbeats) tras los cuales se considera
    // perdida la conexion de un cliente. El cliente envia heartbeats con
    // este mismo valor, asi que solo se cierra si el cliente deja de responder.
    static final int SESSION_TIMEOUT_SECONDS = 10;

    // Tiempo maximo de espera de una invocacion de callback hacia un cliente.
    private static final int CALLBACK_TIMEOUT_MS = 5000;

    // ---- Estado global thread-safe ------------------------------------
    // RF-01: sesiones activas indexadas por nickname (nickname -> sesion)
    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    // RF-02: nicknames que alguna vez iniciaron sesion; permite distinguir
    // "destinatario inexistente" de "destinatario desconectado"
    private final Set<String> knownUsers = ConcurrentHashMap.newKeySet();
    private final AtomicLong privateIdCounter = new AtomicLong(0);

    // Sala compartida original (historial consultado por polling)
    private final List<ChatMessage> messageHistory = new CopyOnWriteArrayList<>();
    private final AtomicLong messageIdCounter = new AtomicLong(0);
    private final DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =====================================================================
    // RF-01: Sesion, presencia y registro de callbacks
    // =====================================================================

    @Override
    public void login(String nickname, ClientCallbackPrx callback, Current current)
            throws ChatException {

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new ChatException("El nickname no puede ser nulo o vacio.");
        }
        if (callback == null) {
            throw new ChatException(
                    "Debe registrar un proxy de callback para iniciar sesion.");
        }

        String cleanNick = nickname.trim();

        // Chequeo rapido antes de contactar al cliente
        if (sessions.containsKey(cleanNick)) {
            throw new NicknameInUseException(
                    "El nickname '" + cleanNick + "' ya se encuentra conectado.",
                    cleanNick);
        }

        // Limita el tiempo de espera de cualquier callback hacia este cliente
        ClientCallbackPrx cb = callback.ice_invocationTimeout(CALLBACK_TIMEOUT_MS);

        // Verifica que el objeto de retorno sea alcanzable antes de aceptarlo
        try {
            cb.ice_ping();
        } catch (com.zeroc.Ice.LocalException e) {
            throw new ChatException(
                    "El servidor no pudo contactar el callback del cliente: "
                            + e.getClass().getSimpleName());
        }

        Connection con = current.con;
        ClientSession session = new ClientSession(cleanNick, cb, con);

        // putIfAbsent es atomico: dos logins simultaneos con el mismo
        // nickname nunca pueden registrarse a la vez
        if (sessions.putIfAbsent(cleanNick, session) != null) {
            throw new NicknameInUseException(
                    "El nickname '" + cleanNick + "' ya se encuentra conectado.",
                    cleanNick);
        }

        knownUsers.add(cleanNick);
        watchConnection(con);

        log("Usuario conectado: " + cleanNick + " (en linea: " + sessions.size() + ")");
        addSystemMessage(cleanNick + " se unio a la sala.");

        // Monitor de presencia: avisa a todos los demas clientes
        broadcast(cleanNick, prx -> prx.onUserConnectedAsync(cleanNick));
    }

    @Override
    public void logout(String nickname, Current current) {
        if (nickname == null) {
            return;
        }
        ClientSession session = sessions.get(nickname.trim());
        if (session != null) {
            endSession(session, "cerro sesion");
        }
    }

    @Override
    public String[] getOnlineUsers(Current current) {
        return sessions.keySet().stream().sorted().toArray(String[]::new);
    }

    /**
     * Configura la conexion del cliente para detectar caidas abruptas:
     * - ACM: si no llega trafico ni heartbeats en SESSION_TIMEOUT_SECONDS,
     *   el servidor cierra la conexion a la fuerza.
     * - CloseCallback: al cerrarse la conexion (logout, proceso terminado,
     *   red caida o timeout ACM) se eliminan las sesiones asociadas.
     */
    private void watchConnection(Connection con) {
        if (con == null) {
            return; // invocacion colocada (mismo proceso): no hay conexion
        }
        try {
            con.setACM(OptionalInt.of(SESSION_TIMEOUT_SECONDS),
                    Optional.of(ACMClose.CloseOnIdleForceful),
                    Optional.of(ACMHeartbeat.HeartbeatOff));
            con.setCloseCallback(closed -> {
                for (ClientSession s : sessions.values()) {
                    if (s.getConnection() == closed) {
                        endSession(s, "conexion perdida");
                    }
                }
            });
        } catch (com.zeroc.Ice.LocalException e) {
            log("No se pudo vigilar la conexion: " + e);
        }
    }

    /**
     * Elimina la sesion (solo si sigue siendo la registrada, para evitar
     * dobles notificaciones) y avisa al resto de usuarios.
     */
    private void endSession(ClientSession session, String reason) {
        String nick = session.getNickname();
        if (!sessions.remove(nick, session)) {
            return; // ya fue eliminada por otro hilo
        }
        log("Usuario desconectado: " + nick + " [" + reason + "] (en linea: "
                + sessions.size() + ")");
        addSystemMessage(nick + " ha abandonado la sala.");
        broadcast(nick, prx -> prx.onUserDisconnectedAsync(nick));
    }

    /**
     * Envia una notificacion asincrona a todos los clientes excepto
     * {@code excludedNick}. Si un cliente no responde se considera caido
     * y su sesion se elimina.
     */
    private void broadcast(String excludedNick,
                           Function<ClientCallbackPrx, CompletableFuture<Void>> call) {
        for (ClientSession s : sessions.values()) {
            if (!s.getNickname().equals(excludedNick)) {
                notifyClient(s, call);
            }
        }
    }

    /**
     * Invoca un callback de forma asincrona (AMI) sin bloquear el hilo de
     * despacho del servidor. Ante un error de red, retira al cliente.
     */
    private <T> CompletableFuture<T> notifyClient(
            ClientSession s, Function<ClientCallbackPrx, CompletableFuture<T>> call) {
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

    // =====================================================================
    // RF-02: Mensajeria privada directa (1 a 1)
    // =====================================================================

    @Override
    public DeliveryReceipt sendPrivateMessage(
            String sender,
            String recipient,
            String text,
            Current current
    ) throws ChatException {

        ClientSession from = requireSession(sender, current);

        if (recipient == null || recipient.trim().isEmpty()) {
            throw new ChatException("Debe indicar el nickname del destinatario.");
        }
        String to = recipient.trim();

        if (to.equals(from.getNickname())) {
            throw new ChatException("No puede enviarse un mensaje privado a si mismo.");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new ChatException("El mensaje no puede estar vacio.");
        }

        // Localiza el proxy de callback del destinatario
        ClientSession target = sessions.get(to);
        if (target == null) {
            if (knownUsers.contains(to)) {
                throw new UserOfflineException(
                        "El usuario '" + to + "' se desconecto y no puede recibir mensajes.", to);
            }
            throw new UserNotFoundException(
                    "El usuario '" + to + "' no existe en el servidor.", to);
        }

        long id = privateIdCounter.incrementAndGet();
        String now = now();
        PrivateMessage msg = new PrivateMessage(id, from.getNickname(), to, text.trim(), now);

        // Despacho asincrono (AMI): el servidor no espera al destinatario.
        // Si la entrega falla despues, se avisa al remitente por su callback.
        notifyClient(target, prx -> prx.onPrivateMessageAsync(msg))
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log("Fallo la entrega del privado #" + id + " " + msg.sender
                                + " -> " + to + ": " + ex.getClass().getSimpleName());
                        ClientSession senderNow = sessions.get(msg.sender);
                        if (senderNow != null) {
                            notifyClient(senderNow, prx -> prx.onPrivateMessageFailedAsync(
                                    id, to, "el destinatario se desconecto durante el envio"));
                        }
                    }
                });

        log("Privado #" + id + " " + msg.sender + " -> " + to);
        return new DeliveryReceipt(id, to, now);
    }

    /**
     * Verifica que el nickname tenga una sesion activa y que la invocacion
     * llegue por la misma conexion con la que inicio sesion (evita que un
     * cliente envie mensajes suplantando a otro).
     */
    private ClientSession requireSession(String nickname, Current current) throws ChatException {
        ClientSession s = nickname == null ? null : sessions.get(nickname.trim());
        if (s == null) {
            throw new ChatException("Acceso denegado: el usuario debe iniciar sesion primero.");
        }
        if (s.getConnection() != null && current.con != null
                && s.getConnection() != current.con) {
            throw new ChatException(
                    "Acceso denegado: la sesion de '" + s.getNickname()
                            + "' pertenece a otro cliente.");
        }
        return s;
    }

    // =====================================================================
    // Sala compartida original (polling)
    // =====================================================================

    @Override
    public void postMessage(
            String nickname,
            String message,
            Current current
    ) throws ChatException {

        if (nickname == null || !sessions.containsKey(nickname)) {
            throw new ChatException(
                    "Acceso denegado: El usuario debe iniciar sesion primero."
            );
        }

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        long id = messageIdCounter.incrementAndGet();
        String now = now();

        ChatMessage msg = new ChatMessage(id, nickname, message.trim(), now);
        messageHistory.add(msg);

        System.out.println("[" + now + "] <" + nickname + "> " + message.trim());
    }

    @Override
    public ChatMessage[] getPendingMessages(
            String nickname,
            long lastMessageId,
            Current current
    ) {

        List<ChatMessage> pending = new ArrayList<>();

        for (ChatMessage msg : messageHistory) {
            if (msg.id > lastMessageId) {
                pending.add(msg);
            }
        }

        return pending.toArray(new ChatMessage[0]);
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private void addSystemMessage(String text) {
        messageHistory.add(new ChatMessage(
                messageIdCounter.incrementAndGet(), "SISTEMA", text, now()));
    }

    private String now() {
        return LocalTime.now().format(timeFormat);
    }

    private void log(String text) {
        System.out.println("[" + now() + "] [ICE-SERVER] " + text);
    }
}
