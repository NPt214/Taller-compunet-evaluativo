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

    // Archivos multimedia
    sequence<byte> ByteSeq;

    struct FileInfo {
        string fileId;
        string fileName;
        string sender;
        string target;
        bool toRoom;
        long totalSize;
        int chunkSize;
        int totalChunks;
        string sha256;
    };

    // Objeto que el cliente expone para que el servidor le avise de archivos nuevos
    interface FileCallback {
        void onFileAvailable(FileInfo info);
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


        // Registra el callback de archivos; se llama justo despues de login
        void registerFileCallback(
            string nickname,
            FileCallback* callback
        ) throws ChatException;

        // Abre una sesion de subida y devuelve su id (que tambien es el fileId)
        string startUpload(FileInfo info) throws ChatException;

        // Los fragmentos se envian en orden: 0, 1, 2, ...
        void writeChunk(
            string uploadId,
            int chunkIndex,
            ByteSeq data
        ) throws ChatException;

        // Cierra la subida, verifica el archivo y avisa a los destinatarios
        void closeUpload(string uploadId) throws ChatException;

        // Descarga un fragmento de un archivo disponible para este usuario
        ByteSeq readChunk(
            string nickname,
            string fileId,
            int chunkIndex
        ) throws ChatException;

        // El receptor termino; cuando todos terminan el servidor borra el archivo
        void closeDownload(string nickname, string fileId);
    };
};