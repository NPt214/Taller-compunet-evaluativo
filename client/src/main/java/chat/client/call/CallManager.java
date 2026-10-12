package chat.client.call;

import CallApp.CallException;
import CallApp.CallServicePrx;
import CallApp.Endpoint;

import java.net.SocketException;
import java.util.function.Consumer;

/**
 * Estado de las llamadas de voz del cliente (RF-05 y RF-06) y puente entre
 * la señalizacion Ice (CallServicePrx / CallCallbackI) y el audio UDP (VoiceSession).
 *
 * Un cliente participa como maximo en UNA cosa a la vez: una llamada 1 a 1 o una
 * llamada grupal. Los metodos son synchronized porque los invocan tanto el hilo de la
 * consola (comandos) como los hilos de Ice (callbacks del servidor).
 */
public final class CallManager {

    private enum State { IDLE, CALLING, RINGING_IN, ACTIVE }

    private final CallServicePrx svc;
    private final String serverHost;
    private final Consumer<String> say;    // respuesta a un comando del usuario
    private final Consumer<String> event;  // evento asincrono (no pisa lo que se esta escribiendo)

    private volatile String nickname;

    // ---- llamada 1 a 1 ----
    private State state = State.IDLE;
    private String callId;
    private String peer;
    private Endpoint peerEndpoint;   // endpoint UDP del otro (llamada entrante)
    private VoiceSession voice;

    // ---- llamada grupal ----
    private String groupRoom;
    private VoiceSession groupVoice;

    public CallManager(CallServicePrx svc, String serverHost,
                       Consumer<String> say, Consumer<String> event) {
        this.svc = svc;
        this.serverHost = serverHost;
        this.say = say;
        this.event = event;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    // ====================================================== comandos del usuario

    /** /call <nick> */
    public synchronized void call(String target) throws CallException {
        if (state != State.IDLE || groupVoice != null) {
            say.accept("Ya estas en una llamada. Usa /hangup o /gleave primero.");
            return;
        }
        VoiceSession v = openSession("");
        if (v == null) {
            return;
        }
        String id;
        try {
            id = svc.startCall(nickname, target, localEndpoint(v));
        } catch (CallException | RuntimeException e) {
            v.close();
            throw e;
        }
        callId = id;
        peer = target;
        voice = v;
        state = State.CALLING;
        say.accept("Llamando a " + target + "... (esperando respuesta; /hangup para cancelar)");
    }

    /** /accept */
    public synchronized void accept() throws CallException {
        if (state != State.RINGING_IN) {
            say.accept("No tienes ninguna llamada entrante.");
            return;
        }
        VoiceSession v = openSession("");
        if (v == null) {
            return;
        }
        try {
            svc.acceptCall(callId, nickname, localEndpoint(v));
        } catch (CallException | RuntimeException e) {
            v.close();
            resetCall();
            throw e;
        }
        try {
            v.start(VoiceSession.resolve(peerEndpoint.host, peerEndpoint.port), false, peer);
        } catch (Exception e) {
            say.accept("[ERROR] No se pudo iniciar el audio: " + e.getMessage());
            hangupQuietly(callId);
            v.close();
            resetCall();
            return;
        }
        voice = v;
        state = State.ACTIVE;
        say.accept("Llamada establecida con " + peer + ". /mute para silenciar, /hangup para colgar.");
    }

    /** /reject */
    public synchronized void reject() throws CallException {
        if (state != State.RINGING_IN) {
            say.accept("No tienes ninguna llamada entrante.");
            return;
        }
        try {
            svc.rejectCall(callId, nickname);
        } finally {
            resetCall();
        }
        say.accept("Llamada rechazada.");
    }

    /** /hangup (tambien cancela una llamada que aun esta timbrando) */
    public synchronized void hangup() {
        if (state == State.IDLE) {
            say.accept("No hay ninguna llamada 1 a 1 activa.");
            return;
        }
        hangupQuietly(callId);
        closeVoice();
        resetCall();
        say.accept("Llamada finalizada.");
    }

    /** /gcall <sala> : inicia o se une a la llamada de voz de una sala */
    public synchronized void joinGroup(String room) throws CallException {
        if (state != State.IDLE) {
            say.accept("Estas en una llamada 1 a 1. Usa /hangup primero.");
            return;
        }
        if (groupVoice != null) {
            say.accept("Ya estas en la llamada de la sala '" + groupRoom + "'. Usa /gleave primero.");
            return;
        }
        VoiceSession v = openSession(room);
        if (v == null) {
            return;
        }
        String[] members;
        try {
            members = svc.joinGroupCall(nickname, room);
        } catch (CallException | RuntimeException e) {
            v.close();
            throw e;
        }
        try {
            v.start(VoiceSession.resolve(serverHost, svc.getRelayPort()), true, null);
        } catch (Exception e) {
            say.accept("[ERROR] No se pudo iniciar el audio grupal: " + e.getMessage());
            leaveQuietly(room);
            v.close();
            return;
        }
        groupVoice = v;
        groupRoom = room;
        say.accept("En la llamada de la sala '" + room + "' con: " + String.join(", ", members)
                + ". /mute para silenciar, /gleave para salir.");
    }

    /** /gleave */
    public synchronized void leaveGroup() {
        if (groupVoice == null) {
            say.accept("No estas en ninguna llamada grupal.");
            return;
        }
        String room = groupRoom;
        leaveQuietly(room);
        closeGroup();
        say.accept("Saliste de la llamada de la sala '" + room + "'.");
    }

    /** /mute (alterna silenciar/activar microfono en la llamada actual) */
    public synchronized void toggleMute() {
        VoiceSession v = groupVoice != null ? groupVoice : (state == State.ACTIVE ? voice : null);
        if (v == null) {
            say.accept("No estas en una llamada.");
            return;
        }
        boolean muted = !v.isMuted();
        v.setMuted(muted);
        say.accept(muted ? "Microfono silenciado." : "Microfono activado.");
    }

    /** Libera audio y sockets locales (al salir del programa). */
    public synchronized void shutdown() {
        closeVoice();
        closeGroup();
        resetCall();
    }

    // ================================================ callbacks del servidor (Ice)

    public synchronized void onIncomingCall(String id, String caller, Endpoint callerUdp) {
        if (state != State.IDLE || groupVoice != null) {
            svc.rejectCallAsync(id, nickname); // ya estamos ocupados
            return;
        }
        state = State.RINGING_IN;
        callId = id;
        peer = caller;
        peerEndpoint = callerUdp;
        event.accept("[LLAMADA] " + caller + " te esta llamando. /accept para contestar, /reject para rechazar.");
    }

    public synchronized void onCallAccepted(String id, String callee, Endpoint calleeUdp) {
        if (state != State.CALLING || !id.equals(callId)) {
            return;
        }
        try {
            voice.start(VoiceSession.resolve(calleeUdp.host, calleeUdp.port), false, callee);
            state = State.ACTIVE;
            event.accept("[LLAMADA] " + callee + " contesto. Llamada establecida. /mute, /hangup.");
        } catch (Exception e) {
            event.accept("[ERROR] No se pudo iniciar el audio: " + e.getMessage());
            hangupQuietly(id);
            closeVoice();
            resetCall();
        }
    }

    public synchronized void onCallRejected(String id, String callee, String reason) {
        if (state != State.CALLING || !id.equals(callId)) {
            return;
        }
        closeVoice();
        resetCall();
        event.accept("[LLAMADA] " + callee + " rechazo la llamada.");
    }

    public synchronized void onCallEnded(String id, String by, String reason) {
        if (state == State.IDLE || !id.equals(callId)) {
            return;
        }
        String other = peer;
        closeVoice();
        resetCall();
        event.accept("[LLAMADA] La llamada con " + other + " termino (" + by + ": " + reason + ").");
    }

    public synchronized void onGroupCallEvent(String room, String who, boolean joined) {
        boolean me = who.equals(nickname);
        if (me) {
            if (!joined && groupVoice != null && room.equals(groupRoom)) {
                // el servidor nos saco (abandonamos la sala): cerrar el audio local
                closeGroup();
                event.accept("[LLAMADA] Saliste de la llamada de la sala '" + room + "'.");
            }
            return;
        }
        String hint = (joined && groupVoice == null)
                ? " Usa /gcall " + room + " para unirte." : "";
        event.accept("[LLAMADA " + room + "] " + who
                + (joined ? " se unio a la llamada de voz." : " salio de la llamada de voz.") + hint);
    }

    // ===================================================================== utiles

    private VoiceSession openSession(String room) {
        try {
            return VoiceSession.open(nickname, room, event);
        } catch (SocketException e) {
            say.accept("[ERROR] No se pudo abrir el socket UDP: " + e.getMessage());
            return null;
        }
    }

    private Endpoint localEndpoint(VoiceSession v) {
        return new Endpoint(VoiceSession.localAddressToward(serverHost), v.getLocalPort());
    }

    private void hangupQuietly(String id) {
        try {
            svc.hangup(id, nickname);
        } catch (RuntimeException ignored) {
        }
    }

    private void leaveQuietly(String room) {
        try {
            svc.leaveGroupCall(nickname, room);
        } catch (RuntimeException ignored) {
        }
    }

    private void closeVoice() {
        if (voice != null) {
            voice.close();
            voice = null;
        }
    }

    private void closeGroup() {
        if (groupVoice != null) {
            groupVoice.close();
            groupVoice = null;
        }
        groupRoom = null;
    }

    private void resetCall() {
        state = State.IDLE;
        callId = null;
        peer = null;
        peerEndpoint = null;
    }
}