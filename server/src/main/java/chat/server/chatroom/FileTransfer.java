package chat.server.chatroom;

import ChatApp.ChatException;
import ChatApp.FileCallbackPrx;
import ChatApp.FileInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransfer {

    private static final int CALLBACK_TIMEOUT_MS = 5000;
    private static final int MAX_CHUNK_SIZE = 64 * 1024;
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;

    private final ChatRoomI chat;
    private final Path storage;

    private final Map<String, FileCallbackPrx> sessions = new ConcurrentHashMap<>();
    private final Map<String, Upload> uploads = new ConcurrentHashMap<>();
    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    public FileTransfer(ChatRoomI chat) {
        this.chat = chat;
        try {
            this.storage = Files.createTempDirectory("chat-files");
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta temporal de archivos", e);
        }
        this.storage.toFile().deleteOnExit();
    }

    public void registerCallback(String nickname, FileCallbackPrx callback) throws ChatException {
        if (nickname == null || nickname.trim().isEmpty() || callback == null) {
            throw new ChatException("Nickname o callback de archivos invalido.");
        }
        String nick = nickname.trim();
        if (sessions.putIfAbsent(nick, callback.ice_invocationTimeout(CALLBACK_TIMEOUT_MS)) != null) {
            throw new ChatException("El nickname '" + nick + "' ya esta registrado en el servicio de archivos.");
        }
        log("Sesion de archivos iniciada: " + nick);
    }

    public void onUserGone(String nickname) {
        if (sessions.remove(nickname) == null) {
            return;
        }
        uploads.values().removeIf(u -> {
            if (u.info.sender.equals(nickname)) {
                u.discard();
                return true;
            }
            return false;
        });
        for (String fileId : files.keySet()) {
            finishDownload(nickname, fileId);
        }
        log("Sesion de archivos terminada: " + nickname);
    }

    public String startUpload(FileInfo info) throws ChatException {
        if (info == null || info.sender == null || !sessions.containsKey(info.sender)) {
            throw new ChatException("Debe iniciar sesion antes de enviar archivos.");
        }
        if (info.target == null || info.target.isEmpty()) {
            throw new ChatException("Debe indicar el destino del archivo.");
        }
        if (info.totalSize < 0 || info.totalSize > MAX_FILE_SIZE
                || info.chunkSize <= 0 || info.chunkSize > MAX_CHUNK_SIZE
                || info.totalChunks != Math.max(1, (info.totalSize + info.chunkSize - 1) / info.chunkSize)) {
            throw new ChatException("Los metadatos del archivo no son validos.");
        }

        if (info.toRoom) {
            if (!chat.isMember(info.target, info.sender)) {
                throw new ChatException("No perteneces a la sala '" + info.target + "'.");
            }
        } else {
            if (info.target.equals(info.sender)) {
                throw new ChatException("No puedes enviarte un archivo a ti mismo.");
            }
            if (!sessions.containsKey(info.target)) {
                throw new ChatException("El usuario '" + info.target + "' no esta conectado.");
            }
        }

        String id = UUID.randomUUID().toString();
        info.fileId = id;
        try {
            uploads.put(id, new Upload(info, storage.resolve(id)));
        } catch (Exception e) {
            throw new ChatException("El servidor no pudo preparar la subida.");
        }
        return id;
    }

    public void writeChunk(String uploadId, int chunkIndex, byte[] data) throws ChatException {
        Upload u = uploads.get(uploadId);
        if (u == null) {
            throw new ChatException("La subida no existe o fue cancelada.");
        }
        try {
            u.write(chunkIndex, data);
        } catch (Exception e) {
            uploads.remove(uploadId);
            u.discard();
            throw new ChatException("Fragmento invalido, se cancelo la subida: " + e.getMessage());
        }
    }

    public void closeUpload(String uploadId) throws ChatException {
        Upload u = uploads.remove(uploadId);
        if (u == null) {
            throw new ChatException("La subida no existe o fue cancelada.");
        }
        FileInfo info = u.info;

        try {
            u.finish();
        } catch (Exception e) {
            u.discard();
            throw new ChatException("El archivo llego incompleto o corrupto: " + e.getMessage());
        }

        Set<String> recipients = ConcurrentHashMap.newKeySet();
        if (info.toRoom) {
            for (String m : chat.membersOf(info.target)) {
                if (!m.equals(info.sender) && sessions.containsKey(m)) {
                    recipients.add(m);
                }
            }
        } else if (sessions.containsKey(info.target)) {
            recipients.add(info.target);
        }

        if (recipients.isEmpty()) {
            u.discard();
            if (!info.toRoom) {
                throw new ChatException("El usuario '" + info.target + "' se desconecto.");
            }
            log("Archivo '" + info.fileName + "' de " + info.sender + ": la sala " + info.target
                    + " no tiene otros miembros conectados.");
            return;
        }

        files.put(info.fileId, new StoredFile(info, u.path, recipients));
        log("Archivo '" + info.fileName + "' (" + info.totalSize + " bytes, " + info.totalChunks
                + " fragmentos) " + info.sender + " -> " + (info.toRoom ? "sala " : "") + info.target);

        for (String nick : recipients) {
            FileCallbackPrx cb = sessions.get(nick);
            if (cb == null) {
                finishDownload(nick, info.fileId);
                continue;
            }
            cb.onFileAvailableAsync(info).whenComplete((r, ex) -> {
                if (ex != null) {
                    log("No se pudo avisar a " + nick + ": " + ex.getClass().getSimpleName());
                    finishDownload(nick, info.fileId);
                }
            });
        }
    }

    public byte[] readChunk(String nickname, String fileId, int chunkIndex) throws ChatException {
        StoredFile f = files.get(fileId);
        if (f == null || !f.pending.contains(nickname)) {
            throw new ChatException("El archivo no existe o no esta disponible para ti.");
        }
        if (chunkIndex < 0 || chunkIndex >= f.info.totalChunks) {
            throw new ChatException("Fragmento fuera de rango.");
        }
        long offset = (long) chunkIndex * f.info.chunkSize;
        int length = (int) Math.min(f.info.chunkSize, f.info.totalSize - offset);
        byte[] data = new byte[Math.max(0, length)];
        try (RandomAccessFile raf = new RandomAccessFile(f.path.toFile(), "r")) {
            raf.seek(offset);
            raf.readFully(data);
        } catch (IOException e) {
            throw new ChatException("El servidor no pudo leer el archivo.");
        }
        return data;
    }

    public void closeDownload(String nickname, String fileId) {
        finishDownload(nickname, fileId);
    }

    private void finishDownload(String nickname, String fileId) {
        StoredFile f = files.get(fileId);
        if (f == null) {
            return;
        }
        f.pending.remove(nickname);
        if (f.pending.isEmpty() && files.remove(fileId, f)) {
            try {
                Files.deleteIfExists(f.path);
            } catch (IOException ignored) {
            }
        }
    }

    private void log(String text) {
        System.out.println("[" + LocalTime.now().format(timeFormat) + "] [ICE-SERVER] [ARCHIVOS] " + text);
    }

    private static final class Upload {
        final FileInfo info;
        final Path path;
        private final OutputStream out;
        private final MessageDigest sha;
        private int nextIndex = 0;
        private long written = 0;

        Upload(FileInfo info, Path path) throws Exception {
            this.info = info;
            this.path = path;
            this.out = Files.newOutputStream(path);
            this.sha = MessageDigest.getInstance("SHA-256");
        }

        synchronized void write(int index, byte[] data) throws IOException {
            if (index != nextIndex) {
                throw new IOException("se esperaba el fragmento " + nextIndex + " y llego el " + index);
            }
            if (data.length > info.chunkSize || written + data.length > info.totalSize) {
                throw new IOException("el fragmento es mas grande de lo anunciado");
            }
            out.write(data);
            sha.update(data);
            written += data.length;
            nextIndex++;
        }

        synchronized void finish() throws IOException {
            out.close();
            String hash = HexFormat.of().formatHex(sha.digest());
            if (nextIndex != info.totalChunks || written != info.totalSize
                    || !hash.equalsIgnoreCase(info.sha256)) {
                throw new IOException("no coincide el tamano o el SHA-256");
            }
        }

        synchronized void discard() {
            try {
                out.close();
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }

    private static final class StoredFile {
        final FileInfo info;
        final Path path;
        final Set<String> pending;

        StoredFile(FileInfo info, Path path, Set<String> pending) {
            this.info = info;
            this.path = path;
            this.pending = pending;
        }
    }
}
