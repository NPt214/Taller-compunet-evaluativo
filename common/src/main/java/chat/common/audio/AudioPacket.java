package chat.common.audio;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public final class AudioPacket {

    public static final byte HELLO = 1;
    public static final byte AUDIO = 2;

    public static final int MAX_DATAGRAM = 2048;

    public final byte type;
    public final int seq;
    public final String room;
    public final String sender;
    public final byte[] payload;

    public AudioPacket(byte type, int seq, String room, String sender, byte[] payload) {
        this.type = type;
        this.seq = seq;
        this.room = room == null ? "" : room;
        this.sender = sender == null ? "" : sender;
        this.payload = payload == null ? new byte[0] : payload;
    }

    public byte[] encode() {
        byte[] r = room.getBytes(StandardCharsets.UTF_8);
        byte[] s = sender.getBytes(StandardCharsets.UTF_8);
        if (r.length > 255 || s.length > 255) {
            throw new IllegalArgumentException("room/sender exceden 255 bytes");
        }
        ByteBuffer b = ByteBuffer.allocate(1 + 4 + 1 + r.length + 1 + s.length + payload.length);
        b.put(type);
        b.putInt(seq);
        b.put((byte) r.length).put(r);
        b.put((byte) s.length).put(s);
        b.put(payload);
        return b.array();
    }


    public static AudioPacket decode(byte[] data, int off, int len) {
        try {
            ByteBuffer b = ByteBuffer.wrap(data, off, len);
            byte type = b.get();
            if (type != HELLO && type != AUDIO) {
                return null;
            }
            int seq = b.getInt();
            String room = readString(b);
            String sender = readString(b);
            byte[] payload = new byte[b.remaining()];
            b.get(payload);
            return new AudioPacket(type, seq, room, sender, payload);
        } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static String readString(ByteBuffer b) {
        int n = b.get() & 0xFF;
        byte[] x = new byte[n];
        b.get(x);
        return new String(x, StandardCharsets.UTF_8);
    }
}