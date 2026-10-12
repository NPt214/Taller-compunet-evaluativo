// Mensajeria privada 1 a 1.
// Archivo independiente de Chat.ice: no usa #include y vive en su propio
// modulo para que no choque con los nombres del chatroom.
module PrivateChatApp {

    // Mensaje que viaja del remitente al destinatario
    struct PrivateMessage {
        long id;
        string sender;
        string recipient;
        string text;
        string timestamp;
    };

    // Confirmacion de que el servidor recibio y despacho el mensaje
    struct DeliveryReceipt {
        long messageId;
        string recipient;
        string timestamp;
    };

    sequence<string> UserSeq;

    // Excepcion base de este servicio
    exception PrivateChatException {
        string reason;
    };

    exception NicknameInUseException extends PrivateChatException {
        string nickname;
    };

    // El destinatario nunca existio en el servidor
    exception UserNotFoundException extends PrivateChatException {
        string nickname;
    };

    // El destinatario existio pero ya no esta conectado
    exception UserOfflineException extends PrivateChatException {
        string nickname;
    };

    // Objeto que el cliente expone para que el servidor le entregue mensajes
    interface PrivateCallback {
        void onPrivateMessage(PrivateMessage message);
        void onPrivateMessageFailed(long messageId, string recipient, string reason);
    };

    // Interfaz remota provista por el servidor
    interface PrivateChat {

        // Registra al usuario junto con su proxy de callback
        void login(string nickname, PrivateCallback* callback) throws PrivateChatException;

        void logout(string nickname);

        // Para saber a quien se le puede escribir
        idempotent UserSeq getOnlineUsers();

        DeliveryReceipt sendPrivateMessage(string sender, string recipient, string text)
            throws PrivateChatException;
    };
};
