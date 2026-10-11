package chat.client;

import ChatApp.ClientCallback;
import ChatApp.PrivateMessage;
import com.zeroc.Ice.Current;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servant del objeto de retorno (callback) que vive en el cliente.
 * El servidor invoca estos metodos de forma asincrona a traves del proxy
 * que el cliente registro en login (RF-01).
 */
public class ClientCallbackI implements ClientCallback {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Vista local de la presencia, actualizada en tiempo real por el servidor
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

    // ---- RF-01: presencia --------------------------------------------

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

    // ---- RF-02: mensajeria privada -----------------------------------

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

    private static String now() {
        return LocalTime.now().format(TIME);
    }
}
