module ChatApp {

    // Estructura que modela un mensaje del chat
    struct ChatMessage {
        long id;
        string sender;
        string text;
        string timestamp;
        string room;
    };

    // Secuencias tipadas (equivalentes a arreglos/listas en Java)
    sequence<ChatMessage> MessageSeq;
    sequence<string> UserSeq;
    sequence<string> RoomSeq;

    // Excepción de usuario para validaciones de negocio
    exception ChatException {
        string reason;
    };

    // Interfaz remota provista por el servidor Ice
    interface ChatRoom {

        // Registra un usuario; falla si el nickname ya existe
        void login(string nickname) throws ChatException;


        // Crea una nueva sala de chat
        void createRoom(
            string nickname,
            string room
        ) throws ChatException;


        // Retorna la lista de salas disponibles
        idempotent RoomSeq listRooms();


        // Permite que un usuario se una a una sala
        void joinRoom(
            string nickname,
            string room
        ) throws ChatException;


        // Permite que un usuario abandone una sala
        void leaveRoom(
            string nickname,
            string room
        ) throws ChatException;


        // Envía un mensaje a la especifica compartida
        void postMessage(
            string nickname,
            string room,
            string message
        ) throws ChatException;


        // Consulta de mensajes pendientes
        // idempotent permite reintentos seguros de red
        idempotent MessageSeq getPendingMessages(
            string nickname,
            long lastMessageId
        );

        // Retorna la lista de usuarios activos actualmente
        idempotent UserSeq getOnlineUsers();

        // Cierra sesión y desvincula al usuario
        void logout(string nickname);
    };
};