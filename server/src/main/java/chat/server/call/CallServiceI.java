package chat.server.call;

import CallApp.CallCallbackPrx;
import CallApp.CallException;
import CallApp.CallService;
import CallApp.Endpoint;
import CallApp.NotRoomMemberException;
import CallApp.UserBusyException;
import CallApp.UserUnavailableException;
import chat.server.chatroom.ChatRoomI;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Current;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class CallServiceI implements CallService {

    private static final int CALLBACK_TIMEOUT_MS = 5000;
    private static final int RING_TIMEOUT_SECONDS = 30;
    private static final int MAX_NAME_BYTES = 100; // limite del formato de AudioPacket (255) con margen

    private final ChatRoomI chat;
    private final UdpAudioRelay relay;

    private final Map<String, CallSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ActiveCall> calls = new ConcurrentHashMap<>();   // callId -> llamada 1 a 1
    private final Map<String, String> userCall = new ConcurrentHashMap<>();    // nick -> callId
    private final Map<String, GroupCall> groups = new ConcurrentHashMap<>();   // sala -> llamada grupal
    private final Map<String, String> userGroup = new ConcurrentHashMap<>();   // nick -> sala
    private final Object lock = new Object();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "call-timeouts");
                t.setDaemon(true);
                return t;
            });

    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    public CallServiceI(ChatRoomI chat, UdpAudioRelay relay) {
        this.chat = chat;
        this.relay = relay;
    }


    @Override
    public void login(String nickname, CallCallbackPrx callback, Current current)
            throws CallException {

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new CallException("El nickname no puede ser nulo o vacio.");
        }
        if (callback == null) {
            throw new CallException("Debe registrar un proxy de callback para iniciar sesion.");
        }
        String nick = nickname.trim();
        if (nick.getBytes(StandardCharsets.UTF_8).length > MAX_NAME_BYTES) {
            throw new CallException("El nickname es demasiado largo para el servicio de voz.");
        }
        if (sessions.containsKey(nick)) {
            throw new CallException("El nickname '" + nick + "' ya esta registrado en el servicio de voz.");
        }

        CallCallbackPrx cb = callback.ice_invocationTimeout(CALLBACK_TIMEOUT_MS);
        try {
            cb.ice_ping();
        } catch (com.zeroc.Ice.LocalException e) {
            throw new CallException("El servidor no pudo contactar el callback de voz del cliente: "
                    + e.getClass().getSimpleName());
        }

        CallSession session = new CallSession(nick, cb, current.con);
        if (sessions.putIfAbsent(nick, session) != null) {
            throw new CallException("El nickname '" + nick + "' ya esta registrado en el servicio de voz.");
        }
        log("Sesion de voz iniciada: " + nick + " (en linea: " + sessions.size() + ")");
    }

    @Override
    public void logout(String nickname, Current current) {
        if (nickname == null) {
            return;
        }
        CallSession s = sessions.get(nickname.trim());
        if (s != null && owns(s, current)) {
            endSession(s, "cerro sesion");
        }
    }


    public void onUserGone(String nickname) {
        CallSession s = sessions.get(nickname);
        if (s != null) {
            endSession(s, "conexion perdida");
        }
    }

    public void onRoomLeft(String room, String nickname) {
        if (room.equals(userGroup.get(nickname))) {
            removeFromGroup(nickname, room, true);
        }
    }

    private void endSession(CallSession session, String reason) {
        String nick = session.getNickname();
        if (!sessions.remove(nick, session)) {
            return;
        }
        String callId = userCall.get(nick);
        if (callId != null) {
            endCall(callId, nick, reason);
        }
        String room = userGroup.get(nick);
        if (room != null) {
            removeFromGroup(nick, room, false);
        }
        log("Sesion de voz terminada: " + nick + " [" + reason + "] (en linea: " + sessions.size() + ")");
    }


    @Override
    public String startCall(String caller, String callee, Endpoint myUdp, Current current)
            throws CallException {

        CallSession from = requireSession(caller, current);
        String to = callee == null ? "" : callee.trim();
        if (to.isEmpty()) {
            throw new CallException("Debe indicar el usuario a llamar.");
        }
        if (to.equals(from.getNickname())) {
            throw new CallException("No puede llamarse a si mismo.");
        }
        checkEndpoint(myUdp);

        CallSession target = sessions.get(to);
        if (target == null) {
            throw new UserUnavailableException("El usuario '" + to + "' no esta conectado.", to);
        }

        ActiveCall call;
        synchronized (lock) {
            if (isBusy(from.getNickname())) {
                throw new UserBusyException("Ya esta en una llamada.", from.getNickname());
            }
            if (isBusy(to)) {
                throw new UserBusyException("El usuario '" + to + "' esta ocupado en otra llamada.", to);
            }
            call = new ActiveCall(UUID.randomUUID().toString(), from.getNickname(), to);
            calls.put(call.id, call);
            userCall.put(call.caller, call.id);
            userCall.put(call.callee, call.id);
        }

        final String id = call.id;
        final String callerNick = call.caller;
        scheduler.schedule(() -> ringTimeout(id), RING_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        notifyClient(target, prx -> prx.onIncomingCallAsync(id, callerNick, myUdp))
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        endCall(id, "servidor", "no se pudo contactar al destinatario");
                    }
                });

        log("Llamada " + shortId(id) + ": " + callerNick + " -> " + to + " (timbrando)");
        return id;
    }

    @Override
    public void acceptCall(String callId, String callee, Endpoint myUdp, Current current)
            throws CallException {

        CallSession me = requireSession(callee, current);
        checkEndpoint(myUdp);

        ActiveCall call;
        synchronized (lock) {
            call = calls.get(callId);
            if (call == null || !call.callee.equals(me.getNickname())) {
                throw new CallException("No hay una llamada pendiente con ese identificador.");
            }
            if (call.getState() != ActiveCall.State.RINGING) {
                throw new CallException("La llamada ya fue contestada.");
            }
            call.setState(ActiveCall.State.ACTIVE);
        }

        CallSession callerSession = sessions.get(call.caller);
        if (callerSession == null) {
            endCall(callId, me.getNickname(), "el usuario que llamaba ya no esta conectado");
            throw new CallException("El usuario que llamaba ya no esta conectado.");
        }

        final String calleeNick = me.getNickname();
        notifyClient(callerSession, prx -> prx.onCallAcceptedAsync(callId, calleeNick, myUdp));
        log("Llamada " + shortId(callId) + ": " + call.caller + " <-> " + calleeNick + " (activa)");
    }

    @Override
    public void rejectCall(String callId, String callee, Current current) throws CallException {

        CallSession me = requireSession(callee, current);

        ActiveCall call;
        synchronized (lock) {
            call = calls.get(callId);
            if (call == null || !call.callee.equals(me.getNickname())) {
                throw new CallException("No hay una llamada pendiente con ese identificador.");
            }
            if (call.getState() != ActiveCall.State.RINGING) {
                throw new CallException("La llamada ya fue contestada; use hangup.");
            }
            removeCall(call);
        }

        CallSession callerSession = sessions.get(call.caller);
        if (callerSession != null) {
            final String calleeNick = me.getNickname();
            notifyClient(callerSession,
                    prx -> prx.onCallRejectedAsync(callId, calleeNick, "la llamada fue rechazada"));
        }
        log("Llamada " + shortId(callId) + ": rechazada por " + me.getNickname());
    }

    @Override
    public void hangup(String callId, String nickname, Current current) {
        if (callId == null || nickname == null) {
            return;
        }
        String nick = nickname.trim();
        CallSession s = sessions.get(nick);
        ActiveCall call = calls.get(callId);
        if (s == null || !owns(s, current) || call == null || !call.involves(nick)) {
            return;
        }
        endCall(callId, nick, "colgo la llamada");
    }

    private void ringTimeout(String callId) {
        ActiveCall c = calls.get(callId);
        if (c != null && c.getState() == ActiveCall.State.RINGING) {
            endCall(callId, "servidor", "nadie contesto la llamada");
        }
    }

    private void endCall(String callId, String by, String reason) {
        ActiveCall c;
        synchronized (lock) {
            c = calls.get(callId);
            if (c == null) {
                return;
            }
            removeCall(c);
        }
        for (String nick : new String[]{c.caller, c.callee}) {
            if (nick.equals(by)) {
                continue;
            }
            CallSession s = sessions.get(nick);
            if (s != null) {
                notifyClient(s, prx -> prx.onCallEndedAsync(callId, by, reason));
            }
        }
        log("Llamada " + shortId(callId) + " terminada por " + by + ": " + reason);
    }

    private void removeCall(ActiveCall c) { // se invoca con lock tomado
        calls.remove(c.id);
        userCall.remove(c.caller, c.id);
        userCall.remove(c.callee, c.id);
    }


    @Override
    public String[] joinGroupCall(String nickname, String room, Current current)
            throws CallException {

        CallSession me = requireSession(nickname, current);
        String nick = me.getNickname();
        String r = room == null ? "" : room.trim();

        if (r.isEmpty()) {
            throw new CallException("Debe indicar la sala.");
        }
        if (r.getBytes(StandardCharsets.UTF_8).length > MAX_NAME_BYTES) {
            throw new CallException("El nombre de la sala es demasiado largo para el servicio de voz.");
        }
        if (!chat.isMember(r, nick)) {
            throw new NotRoomMemberException("Debe pertenecer a la sala '" + r
                    + "' (use /join) para unirse a su llamada de voz.", r);
        }

        Set<String> members;
        boolean added;
        synchronized (lock) {
            if (userCall.containsKey(nick)) {
                throw new UserBusyException("Esta en una llamada 1 a 1; cuelgue primero.", nick);
            }
            String cur = userGroup.get(nick);
            if (cur != null && !cur.equals(r)) {
                throw new UserBusyException("Ya esta en la llamada de la sala '" + cur + "'.", nick);
            }
            GroupCall g = groups.computeIfAbsent(r, GroupCall::new);
            added = g.add(nick);
            userGroup.put(nick, r);
            relay.allow(r, nick); // el relay solo acepta audio de quien pasa por aqui
            members = g.snapshot();
        }

        if (added) {
            broadcastGroupEvent(r, nick, true, false);
            log("Llamada grupal [" + r + "]: se unio " + nick + " (participantes: " + members.size() + ")");
        }
        return members.toArray(new String[0]);
    }

    @Override
    public void leaveGroupCall(String nickname, String room, Current current) {
        if (nickname == null || room == null) {
            return;
        }
        CallSession s = sessions.get(nickname.trim());
        if (s != null && owns(s, current)) {
            removeFromGroup(s.getNickname(), room.trim(), false);
        }
    }


    private void removeFromGroup(String nick, String room, boolean includeSelf) {
        boolean removed;
        synchronized (lock) {
            GroupCall g = groups.get(room);
            removed = g != null && g.remove(nick);
            userGroup.remove(nick, room);
            if (g != null && g.isEmpty()) {
                groups.remove(room, g);
            }
            if (removed) {
                relay.remove(room, nick);
            }
        }
        if (removed) {
            broadcastGroupEvent(room, nick, false, includeSelf);
            log("Llamada grupal [" + room + "]: salio " + nick);
        }
    }

   private void broadcastGroupEvent(String room, String who, boolean joined, boolean includeSelf) {
        Set<String> recipients = new HashSet<>(chat.membersOf(room));
        GroupCall g = groups.get(room);
        if (g != null) {
            recipients.addAll(g.snapshot());
        }
        if (includeSelf) {
            recipients.add(who);
        } else {
            recipients.remove(who);
        }
        for (String n : recipients) {
            CallSession s = sessions.get(n);
            if (s != null) {
                notifyClient(s, prx -> prx.onGroupCallEventAsync(room, who, joined));
            }
        }
    }

    @Override
    public int getRelayPort(Current current) {
        return relay.getPort();
    }


    private boolean isBusy(String nick) { // se invoca con lock tomado
        return userCall.containsKey(nick) || userGroup.containsKey(nick);
    }

    private void checkEndpoint(Endpoint ep) throws CallException {
        if (ep == null || ep.host == null || ep.host.trim().isEmpty()
                || ep.port < 1 || ep.port > 65535) {
            throw new CallException("Endpoint UDP invalido.");
        }
    }

    private CallSession requireSession(String nickname, Current current) throws CallException {
        CallSession s = nickname == null ? null : sessions.get(nickname.trim());
        if (s == null) {
            throw new CallException("Acceso denegado: el usuario debe iniciar sesion primero.");
        }
        if (!owns(s, current)) {
            throw new CallException("Acceso denegado: la sesion de '" + s.getNickname()
                    + "' pertenece a otro cliente.");
        }
        return s;
    }

    private boolean owns(CallSession s, Current current) {
        return s.getConnection() == null || current == null || current.con == null
                || s.getConnection() == current.con;
    }

    private <T> CompletableFuture<T> notifyClient(
            CallSession s, Function<CallCallbackPrx, CompletableFuture<T>> call) {
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

    private static String shortId(String id) {
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    private void log(String text) {
        System.out.println("[" + LocalTime.now().format(timeFormat) + "] [CALL-SERVER] " + text);
    }
}
