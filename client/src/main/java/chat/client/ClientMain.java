package chat.client;

import CallApp.CallCallbackPrx;
import CallApp.CallException;
import CallApp.CallServicePrx;
import ChatApp.ChatException;
import ChatApp.ChatMessage;
import ChatApp.ChatRoomPrx;
import PrivateChatApp.DeliveryReceipt;
import PrivateChatApp.PrivateCallback;
import PrivateChatApp.PrivateCallbackPrx;
import PrivateChatApp.PrivateChatException;
import PrivateChatApp.PrivateChatPrx;
import PrivateChatApp.PrivateMessage;
import chat.client.call.CallCallbackI;
import chat.client.call.CallManager;
import com.zeroc.Ice.ACMClose;
import com.zeroc.Ice.ACMHeartbeat;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cliente unico: CHATROOM (Chat.ice) + 1 A 1 (PrivateChat.ice) en la misma terminal.
 *
 * Un solo nickname se registra en los dos servicios del servidor:
 *   - ChatService         -> interfaz ChatRoom    (salas, polling con getPendingMessages)
 *   - PrivateChatService  -> interfaz PrivateChat (mensajes privados por callback)
 */
public class ClientMain {

    private static final int SESSION_TIMEOUT_SECONDS = 10;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ---------------------------------------------------------------- estado
    private static volatile boolean running = true;
    private static volatile long lastReceivedId = 0;

    // Salida de consola compartida entre el hilo principal, el polling y los callbacks
    private static final Object OUT = new Object();
    private static volatile boolean promptVisible = false;
    private static volatile String currentPrompt = "> ";

    private static final AtomicBoolean loggedOut = new AtomicBoolean(false);

    public static void main(String[] args) {

        List<String> rest = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, rest)) {

            String host = rest.size() > 0 ? rest.get(0) : "127.0.0.1";
            String port = rest.size() > 1 ? rest.get(1) : "10000";

            // ===================== PROXIES DE LOS DOS SERVICIOS =====================
            ChatRoomPrx chatPrx = ChatRoomPrx.checkedCast(
                    communicator.stringToProxy("ChatService:default -h " + host + " -p " + port));
            PrivateChatPrx privatePrx = PrivateChatPrx.checkedCast(
                    communicator.stringToProxy("PrivateChatService:default -h " + host + " -p " + port));

            CallServicePrx callPrx = CallServicePrx.checkedCast(
                    communicator.stringToProxy("CallService:default -h " + host + " -p " + port));

            if (chatPrx == null || privatePrx == null || callPrx == null) {
                System.err.println("[ERROR] No se pudo conectar con el servidor.");
                return;
            }

            // ===================== 1 A 1: ADAPTADOR DE CALLBACK =====================
            // El servidor entrega los mensajes privados invocando este objeto
            String endpoints = communicator.getProperties()
                    .getPropertyWithDefault("CallbackAdapter.Endpoints", "default");
            ObjectAdapter callbackAdapter =
                    communicator.createObjectAdapterWithEndpoints("CallbackAdapter", endpoints);
            PrivateCallbackPrx callbackPrx = PrivateCallbackPrx.uncheckedCast(
                    callbackAdapter.add(new PrivateCallbackI(),
                            new Identity(UUID.randomUUID().toString(), "client")));

            // ===================== VOZ: CALLBACKS DE LLAMADAS (Call.ice) =====================
            // Mismo adaptador, otro servant: el servidor notifica llamadas entrantes, etc.
            CallManager callManager = new CallManager(callPrx, host, ClientMain::say, ClientMain::event);
            CallCallbackPrx callCallbackPrx = CallCallbackPrx.uncheckedCast(
                    callbackAdapter.add(new CallCallbackI(callManager),
                            new Identity(UUID.randomUUID().toString(), "client")));

            callbackAdapter.activate();

            Scanner scanner = new Scanner(System.in);
            String nickname;

            say("=== CHAT DISTRIBUIDO ZEROC ICE (chatroom + 1 a 1) ===");

            // ===================== LOGIN (en los dos servicios) =====================
            while (true) {

                String line = readLine(scanner, "Ingrese su nickname: ");

                if (line == null) {
                    return;
                }

                nickname = line.trim();

                if (nickname.isEmpty()) {
                    continue;
                }

                try {
                    chatPrx.login(nickname);

                    try {
                        privatePrx.login(nickname, callbackPrx);
                    } catch (PrivateChatException e) {
                        // Si falla el segundo servicio, se deshace el primero
                        try {
                            chatPrx.logout(nickname);
                        } catch (Exception ignored) {
                        }
                        throw e;
                    }

                    try {
                        callPrx.login(nickname, callCallbackPrx);
                    } catch (CallException e) {
                        // Si falla el tercer servicio, se deshacen los dos anteriores
                        try {
                            privatePrx.logout(nickname);
                        } catch (Exception ignored) {
                        }
                        try {
                            chatPrx.logout(nickname);
                        } catch (Exception ignored) {
                        }
                        throw e;
                    }

                    callManager.setNickname(nickname);
                    say("Conectado correctamente como " + nickname);
                    break;

                } catch (ChatException e) {
                    say("[ERROR] " + e.reason);
                } catch (PrivateChatException e) {
                    say("[ERROR] " + e.reason);
                } catch (CallException e) {
                    say("[ERROR] " + e.reason);
                }
            }

            final String activeUser = nickname;

            keepConnectionAlive(privatePrx);

            // Si se cierra la terminal con Ctrl+C tambien se libera el nickname
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> logoutQuietly(chatPrx, privatePrx, callPrx, callManager, activeUser)));

            // ===================== CHATROOM: HILO DE POLLING =====================
            Thread listenerThread = new Thread(() -> {

                while (running) {

                    try {

                        ChatMessage[] messages = chatPrx.getPendingMessages(activeUser, lastReceivedId);

                        for (ChatMessage msg : messages) {

                            if (msg.id > lastReceivedId) {
                                lastReceivedId = msg.id;
                            }

                            event("[" + msg.timestamp + "] [" + msg.room + "] <" + msg.sender + "> " + msg.text);
                        }

                        Thread.sleep(500);

                    } catch (Exception e) {

                        if (running) {
                            event("[ERROR] Se perdio la conexion con el servidor.");
                        }

                        running = false;
                    }
                }
            });

            listenerThread.setDaemon(true);
            listenerThread.start();

            printHelp();

            // ===================== BUCLE DE COMANDOS =====================
            while (running) {

                String input = readLine(scanner, "> ");

                if (input == null) {
                    break;
                }

                input = input.trim();

                if (input.isEmpty()) {
                    continue;
                }

                try {

                    String[] commandParts = input.split("\\s+", 2);
                    String command = commandParts[0].toLowerCase();
                    String arguments = commandParts.length > 1 ? commandParts[1].trim() : "";

                    switch (command) {

                        case "/help": {

                            printHelp();
                            break;
                        }

                        case "/users": {

                            String[] users = chatPrx.getOnlineUsers();

                            say("Usuarios conectados:");

                            if (users.length == 0) {
                                say("No hay usuarios conectados.");
                            } else {
                                for (String user : users) {
                                    say(" - " + user + (user.equals(activeUser) ? " (tu)" : ""));
                                }
                            }

                            break;
                        }

                        // ------------------------- CHATROOM -------------------------

                        case "/rooms": {

                            String[] rooms = chatPrx.listRooms();

                            say("Salas disponibles:");

                            if (rooms.length == 0) {
                                say("No hay salas disponibles.");
                            } else {
                                for (String room : rooms) {
                                    say(" - " + room);
                                }
                            }

                            break;
                        }

                        case "/create": {

                            if (arguments.isEmpty()) {
                                say("Uso: /create <sala>");
                                break;
                            }

                            chatPrx.createRoom(activeUser, arguments);
                            say("Sala creada: " + arguments);

                            break;
                        }

                        case "/join": {

                            if (arguments.isEmpty()) {
                                say("Uso: /join <sala>");
                                break;
                            }

                            chatPrx.joinRoom(activeUser, arguments);
                            say("Te uniste a: " + arguments);

                            break;
                        }

                        case "/leave": {

                            if (arguments.isEmpty()) {
                                say("Uso: /leave <sala>");
                                break;
                            }

                            chatPrx.leaveRoom(activeUser, arguments);
                            say("Saliste de: " + arguments);

                            break;
                        }

                        case "/say": {

                            if (arguments.isEmpty()) {
                                say("Uso: /say <sala> <mensaje>");
                                break;
                            }

                            String[] sayParts = arguments.split("\\s+", 2);

                            if (sayParts.length < 2) {
                                say("Uso: /say <sala> <mensaje>");
                                break;
                            }

                            String room = sayParts[0];
                            String message = sayParts[1];

                            chatPrx.postMessage(activeUser, room, message);
                            say("[" + room + "] <" + activeUser + "> " + message);

                            break;
                        }

                        // --------------------------- 1 A 1 ---------------------------

                        case "/msg": {

                            String[] msgParts = arguments.split("\\s+", 2);

                            if (arguments.isEmpty() || msgParts.length < 2) {
                                say("Uso: /msg <nickname> <mensaje>");
                                break;
                            }

                            String to = msgParts[0];
                            String text = msgParts[1];

                            DeliveryReceipt receipt = privatePrx.sendPrivateMessage(activeUser, to, text);
                            say("[" + receipt.timestamp + "] [PRIVADO para " + receipt.recipient + "] "
                                    + text + "  (entregado al servidor, #" + receipt.messageId + ")");

                            break;
                        }

                        // ----------------------- LLAMADAS DE VOZ (UDP) -----------------------

                        case "/call": {

                            if (arguments.isEmpty()) {
                                say("Uso: /call <nickname>");
                                break;
                            }

                            callManager.call(arguments);
                            break;
                        }

                        case "/accept": {

                            callManager.accept();
                            break;
                        }

                        case "/reject": {

                            callManager.reject();
                            break;
                        }

                        case "/hangup": {

                            callManager.hangup();
                            break;
                        }

                        case "/gcall": {

                            if (arguments.isEmpty()) {
                                say("Uso: /gcall <sala>");
                                break;
                            }

                            callManager.joinGroup(arguments);
                            break;
                        }

                        case "/gleave": {

                            callManager.leaveGroup();
                            break;
                        }

                        case "/mute": {

                            callManager.toggleMute();
                            break;
                        }

                        case "/exit":
                        case "/quit": {

                            running = false;

                            logoutQuietly(chatPrx, privatePrx, callPrx, callManager, activeUser);

                            say("Sesion cerrada.");
                            break;
                        }

                        default: {

                            say("Comando desconocido.");
                            say("Escribe /help para ver los comandos.");
                            break;
                        }
                    }

                } catch (ChatException e) {

                    say("[ERROR] " + e.reason);

                } catch (PrivateChatException e) {

                    say("[ERROR] " + e.reason);

                } catch (CallException e) {

                    say("[ERROR] " + e.reason);

                } catch (Exception e) {

                    System.err.println("[ERROR DE RED] " + e.getMessage());
                }
            }

            logoutQuietly(chatPrx, privatePrx, callPrx, callManager, activeUser);
            scanner.close();

        } catch (Exception e) {

            System.err.println("[ERROR FATAL] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // 1 A 1: objeto que el servidor invoca para entregar mensajes privados
    // ============================================================================
    private static class PrivateCallbackI implements PrivateCallback {

        @Override
        public void onPrivateMessage(PrivateMessage message, Current current) {
            event("[" + message.timestamp + "] [PRIVADO de " + message.sender + "] " + message.text);
        }

        @Override
        public void onPrivateMessageFailed(long messageId, String recipient, String reason,
                                           Current current) {
            event("[" + now() + "] [ERROR] El mensaje #" + messageId + " para '"
                    + recipient + "' no se entrego: " + reason + ".");
        }
    }

    // ============================================================================
    // Utilidades
    // ============================================================================

    private static void printHelp() {

        say("");
        say("========== COMANDOS ==========");
        say("--- Chatroom ---");
        say("/rooms                Lista salas");
        say("/create <sala>        Crea una sala");
        say("/join <sala>          Entra a una sala");
        say("/leave <sala>         Sale de una sala");
        say("/say <sala> <mensaje> Envia mensaje a una sala");
        say("--- 1 a 1 ---");
        say("/msg <nick> <mensaje> Envia un mensaje privado");
        say("--- Voz (UDP) ---");
        say("/call <nick>         Llama a un usuario");
        say("/accept              Contesta la llamada entrante");
        say("/reject              Rechaza la llamada entrante");
        say("/hangup              Cuelga la llamada 1 a 1");
        say("/gcall <sala>        Inicia o se une a la llamada de una sala");
        say("/gleave              Sale de la llamada grupal");
        say("/mute                Silencia / activa el microfono");
        say("--- General ---");
        say("/users                Lista usuarios");
        say("/help                 Muestra ayuda");
        say("/exit                 Cierra sesion");
        say("==============================");
        say("");
    }

    // Heartbeat: el servidor cierra las conexiones inactivas durante 10 s
    private static void keepConnectionAlive(PrivateChatPrx privatePrx) {
        try {
            privatePrx.ice_getConnection().setACM(
                    OptionalInt.of(SESSION_TIMEOUT_SECONDS),
                    Optional.of(ACMClose.CloseOff),
                    Optional.of(ACMHeartbeat.HeartbeatAlways));
        } catch (com.zeroc.Ice.LocalException e) {
            say("[AVISO] No se pudo configurar el heartbeat: " + e);
        }
    }

    private static void logoutQuietly(ChatRoomPrx chatPrx, PrivateChatPrx privatePrx,
                                      CallServicePrx callPrx, CallManager callManager, String nick) {

        if (!loggedOut.compareAndSet(false, true)) {
            return;
        }

        // Primero voz: el servidor cuelga las llamadas y avisa a los demas participantes
        try {
            callPrx.logout(nick);
        } catch (Exception ignored) {
        }

        callManager.shutdown();

        try {
            chatPrx.logout(nick);
        } catch (Exception ignored) {
        }

        try {
            privatePrx.logout(nick);
        } catch (Exception ignored) {
        }
    }

    private static String now() {
        return LocalTime.now().format(TIME);
    }

    // Linea normal de salida
    private static void say(String text) {
        synchronized (OUT) {
            System.out.println(text);
            System.out.flush();
        }
    }

    // Mensaje asincrono (polling o callback): no pisa lo que el usuario esta escribiendo
    private static void event(String text) {
        synchronized (OUT) {
            System.out.println();
            System.out.println(text);
            if (promptVisible) {
                System.out.print(currentPrompt);
            }
            System.out.flush();
        }
    }

    // Lee una linea mostrando el prompt; devuelve null si se cierra la entrada
    private static String readLine(Scanner scanner, String prompt) {
        synchronized (OUT) {
            currentPrompt = prompt;
            System.out.print(prompt);
            System.out.flush();
            promptVisible = true;
        }

        String line = scanner.hasNextLine() ? scanner.nextLine() : null;

        synchronized (OUT) {
            promptVisible = false;
        }
        return line;
    }
}