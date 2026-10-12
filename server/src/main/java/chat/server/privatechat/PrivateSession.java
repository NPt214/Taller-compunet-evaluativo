package chat.server.privatechat;

import PrivateChatApp.PrivateCallbackPrx;
import com.zeroc.Ice.Connection;

public final class PrivateSession {

    private final String nickname;
    private final PrivateCallbackPrx callback;
    private final Connection connection;

    public PrivateSession(String nickname, PrivateCallbackPrx callback, Connection connection) {
        this.nickname = nickname;
        this.callback = callback;
        this.connection = connection;
    }

    public String getNickname() {
        return nickname;
    }

    public PrivateCallbackPrx getCallback() {
        return callback;
    }

    public Connection getConnection() {
        return connection;
    }
}
