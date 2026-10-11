package chat.server;

import ChatApp.ClientCallbackPrx;
import com.zeroc.Ice.Connection;

public final class ClientSession {

    private final String nickname;
    private final ClientCallbackPrx callback;
    private final Connection connection;

    public ClientSession(String nickname, ClientCallbackPrx callback, Connection connection) {
        this.nickname = nickname;
        this.callback = callback;
        this.connection = connection;
    }

    public String getNickname() {
        return nickname;
    }

    public ClientCallbackPrx getCallback() {
        return callback;
    }

    public Connection getConnection() {
        return connection;
    }
}
