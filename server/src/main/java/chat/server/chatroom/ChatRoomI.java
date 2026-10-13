package chat.server.chatroom;

import ChatApp.ChatException;
import ChatApp.ChatMessage;
import ChatApp.ChatRoom;
import ChatApp.FileCallbackPrx;
import ChatApp.FileInfo;
import com.zeroc.Ice.Current;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public class ChatRoomI implements ChatRoom {

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final List<ChatMessage> messageHistory = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Set<String>> rooms = new ConcurrentHashMap<>();
    private final AtomicLong messageIdCounter = new AtomicLong(0);
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");


    private volatile BiConsumer<String, String> onMemberLeft;

    private final FileTransfer files = new FileTransfer(this);

    public void setOnMemberLeft(BiConsumer<String, String> listener) {
        this.onMemberLeft = listener;
    }


    public boolean isMember(String room, String nickname) {
        Set<String> members = rooms.get(room);
        return members != null && members.contains(nickname);
    }

    public Set<String> membersOf(String room) {
        Set<String> members = rooms.get(room);
        return members == null ? new java.util.HashSet<>() : new java.util.HashSet<>(members);
    }

    private void fireMemberLeft(String room, String nickname) {
        BiConsumer<String, String> l = onMemberLeft;
        if (l != null) {
            l.accept(room, nickname);
        }
    }

    @Override
    public synchronized void login(String nickname, Current current) throws ChatException {

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new ChatException("El nickname no puede ser nulo o vacio.");
        }

        String cleanNick = nickname.trim();

        if (onlineUsers.contains(cleanNick)) {
            throw new ChatException("El nickname '" + cleanNick + "' ya se encuentra conectado.");
        }

        onlineUsers.add(cleanNick);

        System.out.println("[ICE-SERVER] Usuario conectado con exito: " + cleanNick);
    }

    @Override
    public void createRoom(String nickname, String room, Current current) throws ChatException {

        if (!onlineUsers.contains(nickname)) {
            throw new ChatException("Acceso denegado: El usuario debe iniciar sesion primero.");
        }

        if (room == null || room.trim().isEmpty()) {
            throw new ChatException("El nombre de la sala no puede estar vacio.");
        }

        String cleanRoom = room.trim();
        Set<String> newRoom = ConcurrentHashMap.newKeySet();
        Set<String> previous = rooms.putIfAbsent(cleanRoom, newRoom);

        if (previous != null) {
            throw new ChatException("La sala '" + cleanRoom + "' ya existe.");
        }

        System.out.println("[ICE-SERVER] Sala creada: " + cleanRoom);
    }

    @Override
    public String[] listRooms(Current current) {

        return rooms.keySet().toArray(new String[0]);
    }

    @Override
    public void joinRoom(String nickname, String room, Current current) throws ChatException {

        if (!onlineUsers.contains(nickname)) {
            throw new ChatException("Acceso denegado: El usuario debe iniciar sesion primero.");
        }

        Set<String> members = rooms.get(room);

        if (members == null) {
            throw new ChatException("La sala '" + room + "' no existe.");
        }

        if (!members.add(nickname)) {
            throw new ChatException("El usuario ya pertenece a la sala '" + room + "'.");
        }

        System.out.println("[ICE-SERVER] " + nickname + " se unio a la sala " + room);
    }

    @Override
    public void leaveRoom(String nickname, String room, Current current) throws ChatException {

        if (!onlineUsers.contains(nickname)) {
            throw new ChatException("Acceso denegado: El usuario debe iniciar sesion primero.");
        }

        Set<String> members = rooms.get(room);

        if (members == null) {
            throw new ChatException("La sala '" + room + "' no existe.");
        }

        if (!members.remove(nickname)) {
            throw new ChatException("El usuario no pertenece a la sala '" + room + "'.");
        }

        System.out.println("[ICE-SERVER] " + nickname + " abandono la sala " + room);
        fireMemberLeft(room, nickname);
    }

    @Override
    public void postMessage(String nickname, String room, String message, Current current) throws ChatException {

        if (!onlineUsers.contains(nickname)) {
            throw new ChatException("Acceso denegado: El usuario debe iniciar sesion primero.");
        }

        Set<String> members = rooms.get(room);

        if (members == null) {
            throw new ChatException("La sala '" + room + "' no existe.");
        }

        if (!members.contains(nickname)) {
            throw new ChatException("El usuario no pertenece a la sala '" + room + "'.");
        }

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        long id = messageIdCounter.incrementAndGet();
        String now = LocalTime.now().format(timeFormat);
        ChatMessage msg = new ChatMessage(id, nickname, message.trim(), now, room);

        messageHistory.add(msg);

        System.out.println("[" + now + "] [" + room + "] <" + nickname + "> " + message.trim());
    }

    @Override
    public ChatMessage[] getPendingMessages(String nickname, long lastMessageId, Current current) {

        List<ChatMessage> pending = new ArrayList<>();

        for (ChatMessage msg : messageHistory) {

            if (msg.id <= lastMessageId) {
                continue;
            }

            if (msg.sender.equals(nickname)) {
                continue;
            }

            Set<String> members = rooms.get(msg.room);

            if (members != null && members.contains(nickname)) {
                pending.add(msg);
            }
        }

        return pending.toArray(new ChatMessage[0]);
    }

    @Override
    public String[] getOnlineUsers(Current current) {

        return onlineUsers.toArray(new String[0]);
    }

    @Override
    public synchronized void logout(String nickname, Current current) {

        if (onlineUsers.remove(nickname)) {

            for (java.util.Map.Entry<String, Set<String>> e : rooms.entrySet()) {
                if (e.getValue().remove(nickname)) {
                    fireMemberLeft(e.getKey(), nickname);
                }
            }

            files.onUserGone(nickname);

            System.out.println("[ICE-SERVER] Usuario desconectado: " + nickname);
        }
    }

    @Override
    public void registerFileCallback(String nickname, FileCallbackPrx callback, Current current)
            throws ChatException {
        if (nickname == null || !onlineUsers.contains(nickname.trim())) {
            throw new ChatException("Acceso denegado: El usuario debe iniciar sesion primero.");
        }
        files.registerCallback(nickname.trim(), callback);
    }

    @Override
    public String startUpload(FileInfo info, Current current) throws ChatException {
        if (info == null || !onlineUsers.contains(info.sender)) {
            throw new ChatException("Acceso denegado: El usuario debe iniciar sesion primero.");
        }
        return files.startUpload(info);
    }

    @Override
    public void writeChunk(String uploadId, int chunkIndex, byte[] data, Current current)
            throws ChatException {
        files.writeChunk(uploadId, chunkIndex, data);
    }

    @Override
    public void closeUpload(String uploadId, Current current) throws ChatException {
        files.closeUpload(uploadId);
    }

    @Override
    public byte[] readChunk(String nickname, String fileId, int chunkIndex, Current current)
            throws ChatException {
        return files.readChunk(nickname, fileId, chunkIndex);
    }

    @Override
    public void closeDownload(String nickname, String fileId, Current current) {
        files.closeDownload(nickname, fileId);
    }

    public void onConnectionLost(String nickname) {
        files.onUserGone(nickname);
    }
}
