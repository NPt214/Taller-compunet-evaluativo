module ChatApp {

    // Estructura que modela un mensaje del chat
    struct ChatMessage {
        long id;
        string sender;
        string text;
        string timestamp;
    };

    // Secuencias tipadas (equivalentes a arreglos/listas en Java)
    sequence<ChatMessage> MessageSeq;
    sequence<string> UserSeq;

    // Excepción de usuario para validaciones de negocio
    exception ChatException {
        string reason;
    };

    // =====================================================================
    // RF-01: Gestión de Sesión, Presencia y Registro de Clientes
    // =====================================================================

    // Se lanza en login cuando el nickname ya pertenece a otra sesión activa
    exception NicknameInUseException extends ChatException {
        string nickname;
    };

    // =====================================================================
    // RF-02: Mensajería Privada Directa (1 a 1) - tipos
    // =====================================================================

    // Mensaje privado entregado al destinatario a través de su callback
    struct PrivateMessage {
        long id;
        string sender;
        string recipient;
        string text;
        string timestamp;
    };

    // Confirmación devuelta al remitente: el servidor recibió el mensaje
    // y lo despachó hacia el callback del destinatario
    struct DeliveryReceipt {
        long messageId;
        string recipient;
        string timestamp;
    };

    // El destinatario nunca se ha registrado en el servidor
    exception UserNotFoundException extends ChatException {
        string nickname;
    };

    // El destinatario existió pero ya no tiene una sesión activa
    exception UserOfflineException extends ChatException {
        string nickname;
    };

    // =====================================================================
    // RF-03: Grupos / Salas de Chat (Canales) - tipos
    // =====================================================================

    // Mensaje difundido a los miembros de una sala; identifica remitente y sala
    struct RoomMessage {
        long id;
        string room;
        string sender;
        string text;
        string timestamp;
    };

    // Entrada del catálogo de salas
    struct RoomInfo {
        string name;
        string owner;
        int memberCount;
        bool joined;        // true si quien consulta es miembro de la sala
    };

    sequence<RoomInfo> RoomSeq;

    // Ya existe una sala con ese nombre (no distingue mayúsculas)
    exception RoomAlreadyExistsException extends ChatException {
        string room;
    };

    // La sala indicada no existe
    exception RoomNotFoundException extends ChatException {
        string room;
    };

    // El usuario intenta operar sobre una sala a la que no pertenece
    exception NotRoomMemberException extends ChatException {
        string room;
    };

    // =====================================================================
    // Interfaces remotas
    // =====================================================================

    // Interfaz remota implementada por el CLIENTE (objeto de retorno).
    // El cliente la registra en su propio ObjectAdapter y entrega el proxy
    // al servidor en login, para que este le notifique eventos de forma
    // asíncrona.
    interface ClientCallback {

        // RF-01: otro usuario inició sesión en la plataforma
        void onUserConnected(string nickname);

        // RF-01: otro usuario cerró sesión o su conexión se perdió
        void onUserDisconnected(string nickname);

        // RF-02: llegó un mensaje privado dirigido a este cliente
        void onPrivateMessage(PrivateMessage message);

        // RF-02: un mensaje privado que este cliente envió no pudo
        // entregarse (el destinatario se desconectó durante el despacho)
        void onPrivateMessageFailed(long messageId, string recipient, string reason);

        // RF-03: alguien creó una sala nueva (aviso a todos los conectados)
        void onRoomCreated(string room, string owner);

        // RF-03: mensaje en una sala a la que pertenece este cliente
        // (nunca se recibe el propio mensaje como duplicado)
        void onRoomMessage(RoomMessage message);

        // RF-03: otro usuario se unió a una sala de este cliente
        void onRoomMemberJoined(string room, string nickname);

        // RF-03: otro usuario salió (o se desconectó) de una sala de este cliente
        void onRoomMemberLeft(string room, string nickname);
    };

    // Interfaz remota provista por el servidor Ice
    interface ChatRoom {

        // RF-01: registra un usuario junto a su proxy de retorno (callback).
        // Lanza NicknameInUseException si el nickname ya está en uso.
        void login(string nickname, ClientCallback* callback) throws ChatException;

        // Envía un mensaje a la sala compartida
        void postMessage(string nickname, string message) throws ChatException;

        // Consulta de mensajes pendientes
        // idempotent permite reintentos seguros de red
        idempotent MessageSeq getPendingMessages(
            string nickname,
            long lastMessageId
        );

        // Retorna la lista de usuarios activos actualmente
        idempotent UserSeq getOnlineUsers();

        // RF-01: cierra sesión y libera la sesión y el proxy de callback
        void logout(string nickname);

        // RF-02: envía un mensaje privado a otro usuario. El servidor busca
        // el callback del destinatario y le despacha el mensaje de forma
        // asíncrona. Devuelve la confirmación de recepción en el servidor o
        // lanza UserNotFoundException / UserOfflineException / ChatException.
        DeliveryReceipt sendPrivateMessage(string sender, string recipient, string text)
            throws ChatException;

        // RF-03: crea una sala con nombre único; el creador queda unido.
        // Lanza RoomAlreadyExistsException si el nombre ya está tomado.
        void createRoom(string nickname, string room) throws ChatException;

        // RF-03: catálogo de salas disponibles (marca en cuáles está el usuario)
        idempotent RoomSeq listRooms(string nickname) throws ChatException;

        // RF-03: une al usuario a una sala; devuelve los miembros actuales.
        // Un usuario puede pertenecer a varias salas a la vez.
        UserSeq joinRoom(string nickname, string room) throws ChatException;

        // RF-03: el usuario abandona la sala
        void leaveRoom(string nickname, string room) throws ChatException;

        // RF-03: miembros de una sala (solo para sus miembros)
        idempotent UserSeq getRoomMembers(string nickname, string room) throws ChatException;

        // RF-03: difunde un mensaje a todos los miembros de la sala excepto
        // al emisor. Devuelve el id asignado al mensaje (confirmación).
        long sendRoomMessage(string sender, string room, string text) throws ChatException;
    };
};
