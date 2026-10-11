module ChatApp {

    struct ChatMessage {
        long id;
        string sender;
        string text;
        string timestamp;
    };

    sequence<ChatMessage> MessageSeq;
    sequence<string> UserSeq;

    exception ChatException {
        string reason;
    };

    exception NicknameInUseException extends ChatException {
        string nickname;
    };

    struct PrivateMessage {
        long id;
        string sender;
        string recipient;
        string text;
        string timestamp;
    };

    struct DeliveryReceipt {
        long messageId;
        string recipient;
        string timestamp;
    };

    exception UserNotFoundException extends ChatException {
        string nickname;
    };

    exception UserOfflineException extends ChatException {
        string nickname;
    };

    struct RoomMessage {
        long id;
        string room;
        string sender;
        string text;
        string timestamp;
    };

    struct RoomInfo {
        string name;
        string owner;
        int memberCount;
        bool joined;
    };

    sequence<RoomInfo> RoomSeq;

    exception RoomAlreadyExistsException extends ChatException {
        string room;
    };

    exception RoomNotFoundException extends ChatException {
        string room;
    };

    exception NotRoomMemberException extends ChatException {
        string room;
    };

    interface ClientCallback {

        void onUserConnected(string nickname);

        void onUserDisconnected(string nickname);

        void onPrivateMessage(PrivateMessage message);

        void onPrivateMessageFailed(long messageId, string recipient, string reason);

        void onRoomCreated(string room, string owner);

        void onRoomMessage(RoomMessage message);

        void onRoomMemberJoined(string room, string nickname);

        void onRoomMemberLeft(string room, string nickname);
    };

    interface ChatRoom {

        void login(string nickname, ClientCallback* callback) throws ChatException;

        void postMessage(string nickname, string message) throws ChatException;

        idempotent MessageSeq getPendingMessages(
            string nickname,
            long lastMessageId
        );

        idempotent UserSeq getOnlineUsers();

        void logout(string nickname);

        DeliveryReceipt sendPrivateMessage(string sender, string recipient, string text)
            throws ChatException;

        void createRoom(string nickname, string room) throws ChatException;

        idempotent RoomSeq listRooms(string nickname) throws ChatException;

        UserSeq joinRoom(string nickname, string room) throws ChatException;

        void leaveRoom(string nickname, string room) throws ChatException;

        idempotent UserSeq getRoomMembers(string nickname, string room) throws ChatException;

        long sendRoomMessage(string sender, string room, string text) throws ChatException;
    };
};
