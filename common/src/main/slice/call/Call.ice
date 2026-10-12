module CallApp {

    // Direccion UDP donde un cliente recibe audio
    struct Endpoint {
        string host;
        int port;
    };

    sequence<string> NickSeq;

    // Excepcion base de este servicio
    exception CallException {
        string reason;
    };

    // El usuario (o el destino) ya esta en otra llamada
    exception UserBusyException extends CallException {
        string nickname;
    };

    // El destino no existe o no esta conectado
    exception UserUnavailableException extends CallException {
        string nickname;
    };

    // Se intento iniciar/unirse a una llamada de una sala a la que no se pertenece
    exception NotRoomMemberException extends CallException {
        string room;
    };

    // Objeto que el cliente expone para que el servidor lo notifique
    interface CallCallback {
        void onIncomingCall(string callId, string caller, Endpoint callerUdp);
        void onCallAccepted(string callId, string callee, Endpoint calleeUdp);
        void onCallRejected(string callId, string callee, string reason);
        void onCallEnded(string callId, string by, string reason);
        // Alguien entro (joined=true) o salio (joined=false) de la llamada grupal de una sala
        void onGroupCallEvent(string room, string nickname, bool joined);
    };

    // Interfaz remota provista por el servidor
    interface CallService {

        void login(string nickname, CallCallback* callback) throws CallException;

        void logout(string nickname);

        // ---- RF-05: llamada 1 a 1 ----
        // Devuelve el callId. myUdp es donde el iniciador recibira audio.
        string startCall(string caller, string callee, Endpoint myUdp) throws CallException;

        // El destino acepta y publica su propio endpoint UDP
        void acceptCall(string callId, string callee, Endpoint myUdp) throws CallException;

        void rejectCall(string callId, string callee) throws CallException;

        // Cualquiera de los dos cuelga (tambien cancela una llamada que aun timbra)
        void hangup(string callId, string nickname);

        // ---- RF-06: llamada grupal (relay UDP en el servidor) ----
        // Devuelve los participantes actuales de la llamada, incluido quien se une
        NickSeq joinGroupCall(string nickname, string room) throws CallException;

        void leaveGroupCall(string nickname, string room);

        // Puerto UDP del relay de audio del servidor
        idempotent int getRelayPort();
    };
};
