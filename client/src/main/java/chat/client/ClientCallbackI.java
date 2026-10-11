package chat.client;

import ChatApp.ClientCallback;
import ChatApp.PrivateMessage;
import ChatApp.RoomMessage;
import com.zeroc.Ice.Current;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientCallbackI implements ClientCallback {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    public void resetOnlineUsers(String[] users) {
        onlineUsers.clear();
        for (String u : users) {
            onlineUsers.add(u);
        }
    }

    @Override
    public void onUserConnected(String nickname, Current current) {
        onlineUsers.add(nickname);
        Console.event("[" + now() + "] [PRESENCIA] + " + nickname + " se conecto.");
    }

    @Override
    public void onUserDisconnected(String nickname, Current current) {
        onlineUsers.remove(nickname);
        Console.event("[" + now() + "] [PRESENCIA] - " + nickname + " se desconecto.");
    }

    @Override
    public void onPrivateMessage(PrivateMessage message, Current current) {
        Console.event("[" + message.timestamp + "] [PRIVADO de " + message.sender + "] "
                + message.text);
    }

    @Override
    public void onPrivateMessageFailed(long messageId, String recipient, String reason,
                                       Current current) {
        Console.event("[" + now() + "] [ERROR] El mensaje #" + messageId + " para '"
                + recipient + "' no se entrego: " + reason + ".");
    }

    @Override
    public void onRoomCreated(String room, String owner, Current current) {
        Console.event("[" + now() + "] [SALAS] " + owner + " creo la sala #" + room
                + " (usa /join " + room + ")");
    }

    @Override
    public void onRoomMessage(RoomMessage message, Current current) {
        Console.event("[" + message.timestamp + "] [#" + message.room + "] "
                + message.sender + ": " + message.text);
    }

    @Override
    public void onRoomMemberJoined(String room, String nickname, Current current) {
        Console.event("[" + now() + "] [#" + room + "] + " + nickname + " se unio a la sala.");
    }

    @Override
    public void onRoomMemberLeft(String room, String nickname, Current current) {
        Console.event("[" + now() + "] [#" + room + "] - " + nickname + " salio de la sala.");
    }

    private static String now() {
        return LocalTime.now().format(TIME);
    }
}
