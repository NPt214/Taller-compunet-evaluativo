package chat.client;

import ChatApp.ChatException;
import ChatApp.ChatMessage;
import ChatApp.ChatRoomPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import java.util.Scanner;

public class ClientMain {

    private static volatile boolean running = true;
    private static volatile long lastReceivedId = 0;

    public static void main(String[] args) {

        try (Communicator communicator = Util.initialize(args)) {

            String proxyString = "ChatService:default -h 127.0.0.1 -p 10000";
            ObjectPrx base = communicator.stringToProxy(proxyString);
            ChatRoomPrx chatPrx = ChatRoomPrx.checkedCast(base);

            if (chatPrx == null) {
                System.err.println("[ERROR] No se pudo conectar con el servidor.");
                return;
            }

            Scanner scanner = new Scanner(System.in);
            String nickname;

            System.out.println("=== CHAT DISTRIBUIDO ZEROC ICE ===");

            while (true) {

                System.out.print("Ingrese su nickname: ");
                nickname = scanner.nextLine().trim();

                if (nickname.isEmpty()) {
                    continue;
                }

                try {
                    chatPrx.login(nickname);
                    System.out.println("Conectado correctamente como " + nickname);
                    break;
                } catch (ChatException e) {
                    System.out.println("[ERROR] " + e.reason);
                }
            }

            final String activeUser = nickname;

            Thread listenerThread = new Thread(() -> {

                while (running) {

                    try {

                        ChatMessage[] messages = chatPrx.getPendingMessages(activeUser, lastReceivedId);

                        for (ChatMessage msg : messages) {

                            if (msg.id > lastReceivedId) {
                                lastReceivedId = msg.id;
                            }

                            System.out.println();
                            System.out.println("[" + msg.timestamp + "] [" + msg.room + "] <" + msg.sender + "> " + msg.text);
                            System.out.print("> ");
                        }

                        Thread.sleep(500);

                    } catch (Exception e) {

                        if (running) {
                            System.err.println("\n[ERROR] Se perdio la conexion con el servidor.");
                        }

                        running = false;
                    }
                }
            });

            listenerThread.setDaemon(true);
            listenerThread.start();

            printHelp();

            while (running) {

                System.out.print("> ");
                String input = scanner.nextLine().trim();

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

                            System.out.println("Usuarios conectados:");

                            if (users.length == 0) {
                                System.out.println("No hay usuarios conectados.");
                            } else {
                                for (String user : users) {
                                    System.out.println(" - " + user);
                                }
                            }

                            break;
                        }

                        case "/rooms": {

                            String[] rooms = chatPrx.listRooms();

                            System.out.println("Salas disponibles:");

                            if (rooms.length == 0) {
                                System.out.println("No hay salas disponibles.");
                            } else {
                                for (String room : rooms) {
                                    System.out.println(" - " + room);
                                }
                            }

                            break;
                        }

                        case "/create": {

                            if (arguments.isEmpty()) {
                                System.out.println("Uso: /create <sala>");
                                break;
                            }

                            chatPrx.createRoom(activeUser, arguments);
                            System.out.println("Sala creada: " + arguments);

                            break;
                        }

                        case "/join": {

                            if (arguments.isEmpty()) {
                                System.out.println("Uso: /join <sala>");
                                break;
                            }

                            chatPrx.joinRoom(activeUser, arguments);
                            System.out.println("Te uniste a: " + arguments);

                            break;
                        }

                        case "/leave": {

                            if (arguments.isEmpty()) {
                                System.out.println("Uso: /leave <sala>");
                                break;
                            }

                            chatPrx.leaveRoom(activeUser, arguments);
                            System.out.println("Saliste de: " + arguments);

                            break;
                        }

                        case "/say": {

                            if (arguments.isEmpty()) {
                                System.out.println("Uso: /say <sala> <mensaje>");
                                break;
                            }

                            String[] sayParts = arguments.split("\\s+", 2);

                            if (sayParts.length < 2) {
                                System.out.println("Uso: /say <sala> <mensaje>");
                                break;
                            }

                            String room = sayParts[0];
                            String message = sayParts[1];

                            chatPrx.postMessage(activeUser, room, message);
                            System.out.println("[" + room + "] <" + activeUser + "> " + message);

                            break;
                        }

                        case "/exit": {

                            running = false;

                            try {
                                chatPrx.logout(activeUser);
                            } catch (Exception ignored) {
                            }

                            System.out.println("Sesion cerrada.");
                            break;
                        }

                        default: {

                            System.out.println("Comando desconocido.");
                            System.out.println("Escribe /help para ver los comandos.");
                            break;
                        }
                    }

                } catch (ChatException e) {

                    System.out.println("[ERROR] " + e.reason);

                } catch (Exception e) {

                    System.err.println("[ERROR DE RED] " + e.getMessage());
                }
            }

            scanner.close();

        } catch (Exception e) {

            System.err.println("[ERROR FATAL] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printHelp() {

        System.out.println();
        System.out.println("========== COMANDOS ==========");
        System.out.println("/users                Lista usuarios");
        System.out.println("/rooms                Lista salas");
        System.out.println("/create <sala>        Crea una sala");
        System.out.println("/join <sala>          Entra a una sala");
        System.out.println("/leave <sala>         Sale de una sala");
        System.out.println("/say <sala> <mensaje> Envia mensaje");
        System.out.println("/help                 Muestra ayuda");
        System.out.println("/exit                 Cierra sesion");
        System.out.println("==============================");
        System.out.println();
    }
}