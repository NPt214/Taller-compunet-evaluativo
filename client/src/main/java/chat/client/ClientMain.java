package chat.client;

import CallApp.CallCallbackPrx;
import CallApp.CallException;
import CallApp.CallServicePrx;
import ChatApp.ChatException;
import ChatApp.ChatMessage;
import ChatApp.ChatRoomPrx;
import ChatApp.FileCallbackPrx;
import ChatApp.FileInfo;
import PrivateChatApp.DeliveryReceipt;
import PrivateChatApp.PrivateCallback;
import PrivateChatApp.PrivateCallbackPrx;
import PrivateChatApp.PrivateChatException;
import PrivateChatApp.PrivateChatPrx;
import PrivateChatApp.PrivateMessage;
import chat.client.call.CallCallbackI;
import chat.client.call.CallManager;
import chat.client.file.FileCallbackI;
import com.zeroc.Ice.ACMClose;
import com.zeroc.Ice.ACMHeartbeat;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientMain {

    private static final int SESSION_TIMEOUT_SECONDS = 10;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile boolean running = true;
    private static volatile long lastReceivedId = 0;

    private static final AtomicBoolean loggedOut = new AtomicBoolean(false);

    private static final int CHUNK_SIZE = 64 * 1024;
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;
    private static final AtomicBoolean sendingFile = new AtomicBoolean(false);

    public static void main(String[] args) {

        List<String> rest = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, rest)) {

            String host = rest.size() > 0 ? rest.get(0) : "127.0.0.1";
            String port = rest.size() > 1 ? rest.get(1) : "10000";

            ChatRoomPrx chatPrx = ChatRoomPrx.checkedCast(communicator.stringToProxy("ChatService:default -h " + host + " -p " + port));
            PrivateChatPrx privatePrx = PrivateChatPrx.checkedCast(communicator.stringToProxy("PrivateChatService:default -h " + host + " -p " + port));

            CallServicePrx callPrx = CallServicePrx.checkedCast(communicator.stringToProxy("CallService:default -h " + host + " -p " + port));

            if (chatPrx == null || privatePrx == null || callPrx == null) {
                System.err.println("Error: No se pudo conectar con el servidor.");
                return;
            }

            String endpoints = communicator.getProperties().getPropertyWithDefault("CallbackAdapter.Endpoints", "default");
            ObjectAdapter callbackAdapter = communicator.createObjectAdapterWithEndpoints("CallbackAdapter", endpoints);
            PrivateCallbackPrx callbackPrx = PrivateCallbackPrx.uncheckedCast(callbackAdapter.add(new PrivateCallbackI(), new Identity(UUID.randomUUID().toString(), "client")));

            CallManager callManager = new CallManager(callPrx, host, System.out::println, System.out::println);
            CallCallbackPrx callCallbackPrx = CallCallbackPrx.uncheckedCast(callbackAdapter.add(new CallCallbackI(callManager), new Identity(UUID.randomUUID().toString(), "client")));

            callbackAdapter.activate();

            Scanner scanner = new Scanner(System.in);
            String nickname;

            System.out.println("=== CHAT DISTRIBUIDO ZEROC ICE (chatroom + 1 a 1) ===");

            while (true) {

                System.out.print("Ingrese su nickname: ");

                if (!scanner.hasNextLine()) {
                    return;
                }

                nickname = scanner.nextLine().trim();

                if (nickname.isEmpty()) {
                    continue;
                }

                try {
                    chatPrx.login(nickname);

                    try {
                        privatePrx.login(nickname, callbackPrx);
                    } catch (PrivateChatException e) {
                        try {
                            chatPrx.logout(nickname);
                        } catch (Exception ignored) {
                        }
                        throw e;
                    }

                    try {
                        callPrx.login(nickname, callCallbackPrx);
                    } catch (CallException e) {
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

                    final String me = nickname;
                    FileCallbackPrx fileCallbackPrx = FileCallbackPrx.uncheckedCast(callbackAdapter.add(new FileCallbackI(info -> startFileDownload(chatPrx, me, info)), new Identity(UUID.randomUUID().toString(), "client")));

                    try {
                        chatPrx.registerFileCallback(nickname, fileCallbackPrx);
                    } catch (ChatException e) {
                        callbackAdapter.remove(fileCallbackPrx.ice_getIdentity());
                        try {
                            callPrx.logout(nickname);
                        } catch (Exception ignored) {
                        }
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
                    System.out.println("Conectado correctamente como " + nickname);
                    break;

                } catch (ChatException e) {
                    System.out.println("Error: " + e.reason);
                } catch (PrivateChatException e) {
                    System.out.println("Error: " + e.reason);
                } catch (CallException e) {
                    System.out.println("Error: " + e.reason);
                }
            }

            final String activeUser = nickname;

            keepConnectionAlive(privatePrx);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> logoutQuietly(chatPrx, privatePrx, callPrx, callManager, activeUser)));

            Thread listenerThread = new Thread(() -> {

                while (running) {

                    try {

                        ChatMessage[] messages = chatPrx.getPendingMessages(activeUser, lastReceivedId);

                        for (ChatMessage msg : messages) {

                            if (msg.id > lastReceivedId) {
                                lastReceivedId = msg.id;
                            }

                            System.out.println(msg.timestamp + " " + msg.room + " - " + msg.sender + ": " + msg.text);
                        }

                        Thread.sleep(500);

                    } catch (Exception e) {

                        if (running) {
                            System.out.println("Error: Se perdio la conexion con el servidor.");
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

                if (!scanner.hasNextLine()) {
                    break;
                }

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
                                    System.out.println(" - " + user + (user.equals(activeUser) ? " (tu)" : ""));
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
                            System.out.println(room + " - " + activeUser + ": " + message);

                            break;
                        }

                        case "/msg": {

                            String[] msgParts = arguments.split("\\s+", 2);

                            if (arguments.isEmpty() || msgParts.length < 2) {
                                System.out.println("Uso: /msg <nickname> <mensaje>");
                                break;
                            }

                            String to = msgParts[0];
                            String text = msgParts[1];

                            DeliveryReceipt receipt = privatePrx.sendPrivateMessage(activeUser, to, text);
                            System.out.println(receipt.timestamp + " Privado para " + receipt.recipient + ": " + text + "  (entregado al servidor, #" + receipt.messageId + ")");

                            break;
                        }

                        case "/call": {

                            if (arguments.isEmpty()) {
                                System.out.println("Uso: /call <nickname>");
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
                                System.out.println("Uso: /gcall <sala>");
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

                        case "/sendfile": {

                            String[] fileParts = arguments.split("\\s+", 2);

                            if (arguments.isEmpty() || fileParts.length < 2) {
                                System.out.println("Uso: /sendfile <nick> <ruta>   o   /sendfile #<sala> <ruta>");
                                break;
                            }

                            startFileSend(chatPrx, activeUser, fileParts[0], fileParts[1]);
                            break;
                        }

                        case "/exit":
                        case "/quit": {

                            running = false;

                            logoutQuietly(chatPrx, privatePrx, callPrx, callManager, activeUser);

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

                    System.out.println("Error: " + e.reason);

                } catch (PrivateChatException e) {

                    System.out.println("Error: " + e.reason);

                } catch (CallException e) {

                    System.out.println("Error: " + e.reason);

                } catch (Exception e) {

                    System.err.println("Error de red: " + e.getMessage());
                }
            }

            logoutQuietly(chatPrx, privatePrx, callPrx, callManager, activeUser);
            scanner.close();

        } catch (Exception e) {

            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class PrivateCallbackI implements PrivateCallback {

        @Override
        public void onPrivateMessage(PrivateMessage message, Current current) {
            System.out.println(message.timestamp + " Privado de " + message.sender + ": " + message.text);
        }

        @Override
        public void onPrivateMessageFailed(long messageId, String recipient, String reason, Current current) {
            System.out.println(now() + " Error: el mensaje #" + messageId + " para '" + recipient + "' no se entrego: " + reason + ".");
        }
    }

    private static void printHelp() {

        System.out.println();
        System.out.println("           COMANDOS                 ");
        System.out.println("--- Chatroom ---");
        System.out.println("/rooms                Lista salas");
        System.out.println("/create <sala>        Crea una sala");
        System.out.println("/join <sala>          Entra a una sala");
        System.out.println("/leave <sala>         Sale de una sala");
        System.out.println("/say <sala> <mensaje> Envia mensaje a una sala");
        System.out.println("--- 1 a 1 ---");
        System.out.println("/msg <nick> <mensaje> Envia un mensaje privado");
        System.out.println("--- Voz (UDP) ---");
        System.out.println("/call <nick>         Llama a un usuario");
        System.out.println("/accept              Contesta la llamada entrante");
        System.out.println("/reject              Rechaza la llamada entrante");
        System.out.println("/hangup              Cuelga la llamada 1 a 1");
        System.out.println("/gcall <sala>        Inicia o se une a la llamada de una sala");
        System.out.println("/gleave              Sale de la llamada grupal");
        System.out.println("/mute                Silencia / activa el microfono");
        System.out.println("--- Archivos ---");
        System.out.println("/sendfile <nick> <ruta>    Envia un archivo a un usuario");
        System.out.println("/sendfile #<sala> <ruta>   Envia un archivo a una sala");
        System.out.println("--- General ---");
        System.out.println("/users                Lista usuarios");
        System.out.println("/help                 Muestra ayuda");
        System.out.println("/exit                 Cierra sesion");
        System.out.println();
    }

    private static void keepConnectionAlive(PrivateChatPrx privatePrx) {
        try {
            privatePrx.ice_getConnection().setACM(OptionalInt.of(SESSION_TIMEOUT_SECONDS), Optional.of(ACMClose.CloseOff), Optional.of(ACMHeartbeat.HeartbeatAlways));
        } catch (com.zeroc.Ice.LocalException e) {
            System.out.println("Aviso: No se pudo configurar el heartbeat: " + e);
        }
    }

    private static void logoutQuietly(ChatRoomPrx chatPrx, PrivateChatPrx privatePrx, CallServicePrx callPrx, CallManager callManager, String nick) {

        if (!loggedOut.compareAndSet(false, true)) {
            return;
        }

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

    private static void startFileSend(ChatRoomPrx chatPrx, String sender, String rawTarget, String rawPath) {

        boolean toRoom = rawTarget.startsWith("#");
        String target = toRoom ? rawTarget.substring(1) : rawTarget;

        if (target.isEmpty()) {
            System.out.println("Error: Debes indicar el usuario o la sala de destino.");
            return;
        }
        if (!toRoom && target.equals(sender)) {
            System.out.println("Error: No puedes enviarte un archivo a ti mismo.");
            return;
        }

        String cleanPath = rawPath.trim();
        if (cleanPath.length() >= 2 && cleanPath.startsWith("\"") && cleanPath.endsWith("\"")) {
            cleanPath = cleanPath.substring(1, cleanPath.length() - 1);
        }

        Path path;
        long size;
        try {
            path = Paths.get(cleanPath);
            if (!Files.exists(path)) {
                System.out.println("Error: El archivo '" + cleanPath + "' no existe.");
                return;
            }
            if (Files.isDirectory(path)) {
                System.out.println("Error: '" + cleanPath + "' es una carpeta, no un archivo.");
                return;
            }
            if (!Files.isReadable(path)) {
                System.out.println("Error: No hay permisos para leer '" + cleanPath + "'.");
                return;
            }
            size = Files.size(path);
        } catch (java.nio.file.InvalidPathException | IOException e) {
            System.out.println("Error: Ruta invalida: " + cleanPath);
            return;
        }

        if (size > MAX_FILE_SIZE) {
            System.out.println("Error: El archivo supera el limite de " + (MAX_FILE_SIZE / (1024 * 1024)) + " MB.");
            return;
        }
        if (!sendingFile.compareAndSet(false, true)) {
            System.out.println("Error: Ya hay un archivo enviandose, espera a que termine.");
            return;
        }

        final Path file = path;
        final long fileSize = size;
        String destino = toRoom ? "la sala " + target : target;
        System.out.println("Enviando '" + file.getFileName() + "' (" + fileSize + " bytes) a " + destino + "...");

        Thread worker = new Thread(() -> {
            try {
                sendFile(chatPrx, sender, target, toRoom, file, fileSize);
                System.out.println("'" + file.getFileName() + "' subido al servidor y enviado a " + destino + ".");
            } catch (ChatException e) {
                System.out.println("Error: Envio cancelado: " + e.reason);
            } catch (IOException e) {
                System.out.println("Error: No se pudo leer el archivo: " + e.getMessage());
            } catch (com.zeroc.Ice.LocalException e) {
                System.out.println("Error de red: Se interrumpio el envio: " + e.getClass().getSimpleName());
            } catch (Exception e) {
                System.out.println("Error: Envio fallido: " + e.getMessage());
            } finally {
                sendingFile.set(false);
            }
        }, "file-sender");
        worker.setDaemon(true);
        worker.start();
    }

    private static void sendFile(ChatRoomPrx chatPrx, String sender, String target, boolean toRoom, Path file, long size) throws Exception {

        int totalChunks = (int) Math.max(1, (size + CHUNK_SIZE - 1) / CHUNK_SIZE);
        FileInfo info = new FileInfo("", file.getFileName().toString(), sender, target, toRoom, size, CHUNK_SIZE, totalChunks, sha256(file));

        String uploadId = chatPrx.startUpload(info);

        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            for (int i = 0; i < totalChunks; i++) {
                int read = in.readNBytes(buffer, 0, CHUNK_SIZE);
                chatPrx.writeChunk(uploadId, i, Arrays.copyOf(buffer, read));
            }
        }

        chatPrx.closeUpload(uploadId);
    }

    private static void startFileDownload(ChatRoomPrx chatPrx, String me, FileInfo info) {

        String origen = info.sender + (info.toRoom ? " en la sala " + info.target : "");
        System.out.println("Recibiendo '" + info.fileName + "' (" + info.totalSize + " bytes) de " + origen + "...");

        Thread worker = new Thread(() -> {
            try {
                Path saved = downloadFile(chatPrx, me, info);
                System.out.println("'" + info.fileName + "' de " + origen + " guardado en " + saved.toAbsolutePath());
            } catch (ChatException e) {
                System.out.println("Error: No se pudo descargar '" + info.fileName + "': " + e.reason);
            } catch (IOException e) {
                System.out.println("Error: No se pudo guardar '" + info.fileName + "': " + e.getMessage());
            } catch (com.zeroc.Ice.LocalException e) {
                System.out.println("Error de red: Se interrumpio la descarga de '" + info.fileName + "': " + e.getClass().getSimpleName());
            } catch (Exception e) {
                System.out.println("Error: Descarga fallida de '" + info.fileName + "': " + e.getMessage());
            } finally {
                try {
                    chatPrx.closeDownload(me, info.fileId);
                } catch (Exception ignored) {
                }
            }
        }, "file-download");
        worker.setDaemon(true);
        worker.start();
    }

    private static Path downloadFile(ChatRoomPrx chatPrx, String me, FileInfo info) throws Exception {

        Path dir = Paths.get("descargas", me);
        Files.createDirectories(dir);
        Path partial = dir.resolve(info.fileId + ".part");

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long written = 0;
        try (OutputStream out = Files.newOutputStream(partial)) {
            for (int i = 0; i < info.totalChunks; i++) {
                byte[] data = chatPrx.readChunk(me, info.fileId, i);
                out.write(data);
                sha.update(data);
                written += data.length;
            }
        } catch (Exception e) {
            Files.deleteIfExists(partial);
            throw e;
        }

        String hash = HexFormat.of().formatHex(sha.digest());
        if (written != info.totalSize || !hash.equalsIgnoreCase(info.sha256)) {
            Files.deleteIfExists(partial);
            throw new IOException("el archivo llego corrupto o incompleto (se descarto)");
        }

        Path target = freeName(dir, safeName(info.fileName));
        return Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String safeName(String name) {
        Path p = Paths.get(name == null ? "" : name).getFileName();
        String clean = p == null ? "" : p.toString().trim();
        return clean.isEmpty() ? "archivo" : clean;
    }

    private static Path freeName(Path dir, String name) {
        Path candidate = dir.resolve(name);
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; Files.exists(candidate); i++) {
            candidate = dir.resolve(base + " (" + i + ")" + ext);
        }
        return candidate;
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int n;
            while ((n = in.read(buffer)) > 0) {
                sha.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(sha.digest());
    }

    private static String now() {
        return LocalTime.now().format(TIME);
    }
}
