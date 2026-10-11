package chat.client;

import ChatApp.ChatException;
import ChatApp.ChatRoomPrx;
import ChatApp.ClientCallbackPrx;
import ChatApp.NicknameInUseException;
import com.zeroc.Ice.ACMClose;
import com.zeroc.Ice.ACMHeartbeat;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * Cliente CLI del chat.
 *
 * Uso: ClientMain [host] [puerto]   (por defecto localhost 10000)
 *
 * El hilo principal solo lee comandos del teclado; los eventos que envia el
 * servidor (presencia, mensajes) llegan por callbacks en hilos de Ice y se
 * imprimen con {@link Console}, de modo que la entrada nunca se bloquea.
 */
public class ClientMain {

    // Debe coincidir con ChatRoomI.SESSION_TIMEOUT_SECONDS del servidor
    private static final int SESSION_TIMEOUT_SECONDS = 10;

    private final Communicator communicator;
    private final ChatRoomPrx server;
    private final ClientCallbackI callback;
    private final ClientCallbackPrx callbackPrx;

    // Nickname de la sesion activa (null si no hay sesion)
    private volatile String nickname;

    public static void main(String[] args) {
        int exitCode = 0;
        List<String> rest = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, rest)) {
            String host = rest.size() > 0 ? rest.get(0) : "localhost";
            String port = rest.size() > 1 ? rest.get(1) : "10000";

            ClientMain client = new ClientMain(communicator, host, port);
            client.run();
        } catch (com.zeroc.Ice.LocalException e) {
            System.err.println("[ERROR] No se pudo conectar con el servidor: " + e);
            exitCode = 1;
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    public ClientMain(Communicator communicator, String host, String port) {
        this.communicator = communicator;

        // 1. Proxy directo al servicio del servidor
        this.server = ChatRoomPrx.checkedCast(
                communicator.stringToProxy("ChatService:default -h " + host + " -p " + port));
        if (server == null) {
            throw new IllegalStateException("El proxy ChatService no es valido.");
        }

        // 2. ObjectAdapter propio del cliente para recibir callbacks.
        //    "default" escucha en un puerto libre de todas las interfaces;
        //    se puede cambiar con --CallbackAdapter.Endpoints="tcp -h IP"
        String endpoints = communicator.getProperties()
                .getPropertyWithDefault("CallbackAdapter.Endpoints", "default");
        ObjectAdapter adapter =
                communicator.createObjectAdapterWithEndpoints("CallbackAdapter", endpoints);

        // 3. Registrar el servant del callback y obtener su proxy
        this.callback = new ClientCallbackI();
        Identity id = new Identity(UUID.randomUUID().toString(), "client");
        this.callbackPrx = ClientCallbackPrx.uncheckedCast(adapter.add(callback, id));

        // 4. Activar el adaptador para que el servidor pueda invocarnos
        adapter.activate();

        // Cierre ordenado si el proceso termina con Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(this::logoutQuietly));
    }

    // ---------------------------------------------------------------------
    // Bucle principal de lectura de comandos
    // ---------------------------------------------------------------------

    private void run() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Console.println("=================================================");
        Console.println(" CHAT ZEROC ICE - cliente CLI");
        Console.println("=================================================");

        // Pedir nickname hasta lograr iniciar sesion
        while (nickname == null) {
            Console.println("Ingrese su nickname (o /quit para salir):");
            Console.prompt();
            String line = in.readLine();
            Console.hidePrompt();
            if (line == null || line.trim().equalsIgnoreCase("/quit")) {
                return;
            }
            login(line.trim());
        }
        printHelp();

        while (true) {
            Console.prompt();
            String line = in.readLine();
            Console.hidePrompt();
            if (line == null) {
                break; // fin de entrada (Ctrl+D / Ctrl+Z)
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!handleCommand(line)) {
                break;
            }
        }
        logoutQuietly();
        Console.println("Hasta luego.");
    }

    /** @return false si el usuario pidio salir. */
    private boolean handleCommand(String line) {
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/help":
                printHelp();
                break;
            case "/login":
                if (parts.length < 2) {
                    Console.println("Uso: /login <nickname>");
                } else if (nickname != null) {
                    Console.println("Ya tiene una sesion activa como '" + nickname
                            + "'. Use /logout primero.");
                } else {
                    login(parts[1]);
                }
                break;
            case "/logout":
                logout();
                break;
            case "/users":
                listUsers();
                break;
            case "/msg":
                if (parts.length < 3) {
                    Console.println("Uso: /msg <nickname> <texto>");
                } else {
                    sendPrivate(parts[1], parts[2]);
                }
                break;
            case "/quit":
            case "/exit":
                return false;
            default:
                Console.println("Comando no reconocido. Use /help para ver los comandos.");
        }
        return true;
    }

    private void printHelp() {
        Console.println("Comandos disponibles:");
        Console.println("  /msg <nickname> <txt>  Envia un mensaje privado");
        Console.println("  /users                 Lista los usuarios conectados");
        Console.println("  /login <nickname>      Inicia sesion (si no hay una activa)");
        Console.println("  /logout                Cierra la sesion actual");
        Console.println("  /quit                  Cierra sesion y sale");
        Console.println("  /help                  Muestra esta ayuda");
    }

    // ---------------------------------------------------------------------
    // RF-01: sesion y presencia
    // ---------------------------------------------------------------------

    private void login(String nick) {
        if (nick.isEmpty() || nick.startsWith("/") || nick.contains(" ")) {
            Console.println("[ERROR] Nickname invalido (no puede estar vacio, "
                    + "empezar por '/' ni contener espacios).");
            return;
        }
        try {
            server.login(nick, callbackPrx);
            nickname = nick;
            keepConnectionAlive();
            callback.resetOnlineUsers(server.getOnlineUsers());
            Console.println("[OK] Sesion iniciada como '" + nick + "'.");
            listUsers();
        } catch (NicknameInUseException e) {
            Console.println("[ERROR] " + e.reason);
        } catch (ChatException e) {
            Console.println("[ERROR] " + e.reason);
        } catch (com.zeroc.Ice.LocalException e) {
            Console.println("[ERROR] Fallo de comunicacion con el servidor: " + e);
        }
    }

    private void logout() {
        String nick = nickname;
        if (nick == null) {
            Console.println("No hay una sesion activa.");
            return;
        }
        try {
            server.logout(nick);
            Console.println("[OK] Sesion de '" + nick + "' cerrada. Use /login <nickname> o /quit.");
        } catch (com.zeroc.Ice.LocalException e) {
            Console.println("[AVISO] El servidor no respondio al logout: " + e);
        }
        nickname = null;
        callback.getOnlineUsers().clear();
    }

    private void logoutQuietly() {
        String nick = nickname;
        nickname = null;
        if (nick != null) {
            try {
                server.logout(nick);
            } catch (Exception ignored) {
                // el communicator puede estar cerrandose
            }
        }
    }

    private void listUsers() {
        if (nickname == null) {
            Console.println("Debe iniciar sesion primero.");
            return;
        }
        try {
            String[] users = server.getOnlineUsers();
            callback.resetOnlineUsers(users);
            Console.println("Usuarios en linea (" + users.length + "):");
            for (String u : users) {
                Console.println("  - " + u + (u.equals(nickname) ? " (tu)" : ""));
            }
        } catch (com.zeroc.Ice.LocalException e) {
            Console.println("[ERROR] Fallo de comunicacion con el servidor: " + e);
        }
    }

    // ---------------------------------------------------------------------
    // RF-02: mensajeria privada 1 a 1
    // ---------------------------------------------------------------------

    /**
     * Envia el mensaje con una invocacion asincrona (AMI): el hilo de la
     * consola no se bloquea esperando al servidor. La confirmacion o el
     * error se imprimen cuando llega la respuesta.
     */
    private void sendPrivate(String to, String text) {
        String me = nickname;
        if (me == null) {
            Console.println("Debe iniciar sesion primero.");
            return;
        }
        server.sendPrivateMessageAsync(me, to, text).whenComplete((receipt, ex) -> {
            if (ex == null) {
                Console.event("[" + receipt.timestamp + "] [PRIVADO para " + receipt.recipient
                        + "] " + text + "  (entregado al servidor, #" + receipt.messageId + ")");
                return;
            }
            Throwable cause = ex instanceof CompletionException && ex.getCause() != null
                    ? ex.getCause() : ex;
            // UserNotFoundException y UserOfflineException heredan de
            // ChatException y traen un motivo descriptivo en 'reason'
            if (cause instanceof ChatException) {
                Console.event("[ERROR] " + ((ChatException) cause).reason);
            } else {
                Console.event("[ERROR] Fallo de comunicacion con el servidor: " + cause);
            }
        });
    }

    /**
     * Configura la conexion con el servidor para enviar heartbeats
     * continuamente y no cerrarla por inactividad. Asi el servidor puede
     * distinguir un cliente inactivo (sigue enviando heartbeats) de uno
     * caido (deja de enviarlos).
     */
    private void keepConnectionAlive() {
        try {
            server.ice_getConnection().setACM(
                    OptionalInt.of(SESSION_TIMEOUT_SECONDS),
                    Optional.of(ACMClose.CloseOff),
                    Optional.of(ACMHeartbeat.HeartbeatAlways));
        } catch (com.zeroc.Ice.LocalException e) {
            Console.println("[AVISO] No se pudo configurar el heartbeat: " + e);
        }
    }
}
