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
    };
};
