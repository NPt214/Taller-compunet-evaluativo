package chat.server;

import ChatApp.ClientCallbackPrx;
import com.zeroc.Ice.Connection;

/**
 * Sesion activa de un cliente (RF-01).
 * Agrupa el nickname, el proxy de retorno (callback) que el cliente registro
 * en login y la conexion Ice por la que inicio sesion. Es inmutable, por lo
 * que puede compartirse entre hilos del servidor sin sincronizacion extra.
 */
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
