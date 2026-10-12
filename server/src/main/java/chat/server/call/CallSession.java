package chat.server.call;

import CallApp.CallCallbackPrx;
import com.zeroc.Ice.Connection;

public final class CallSession {

    private final String nickname;
    private final CallCallbackPrx callback;
    private final Connection connection;

    public CallSession(String nickname, CallCallbackPrx callback, Connection connection) {
        this.nickname = nickname;
        this.callback = callback;
        this.connection = connection;
    }

    public String getNickname() {
        return nickname;
    }

    public CallCallbackPrx getCallback() {
        return callback;
    }

    public Connection getConnection() {
        return connection;
    }
}
