package chat.server.call;

import chat.common.audio.AudioPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class UdpAudioRelay implements AutoCloseable {

    private static final class Member {
        volatile SocketAddress addr;
    }

    private final int port;
    private final Map<String, Map<String, Member>> rooms = new ConcurrentHashMap<>();
    private volatile boolean running;
    private DatagramSocket socket;
    private Thread thread;

    public UdpAudioRelay(int port) {
        this.port = port;
    }

    public void start() throws SocketException {
        socket = new DatagramSocket(port);
        running = true;
        thread = new Thread(this::loop, "udp-audio-relay");
        thread.setDaemon(true);
        thread.start();
    }

    public int getPort() {
        return socket != null ? socket.getLocalPort() : port;
    }


    public void allow(String room, String nick) {
        rooms.compute(room, (k, m) -> {
            if (m == null) {
                m = new ConcurrentHashMap<>();
            }
            m.put(nick, new Member());
            return m;
        });
    }

    public void remove(String room, String nick) {
        rooms.computeIfPresent(room, (k, m) -> {
            m.remove(nick);
            return m.isEmpty() ? null : m;
        });
    }

    private void loop() {
        byte[] buf = new byte[AudioPacket.MAX_DATAGRAM];
        DatagramPacket in = new DatagramPacket(buf, buf.length);
        while (running) {
            try {
                in.setLength(buf.length);
                socket.receive(in);
                handle(in);
            } catch (SocketException e) {
                if (running) {
                    System.err.println("[UDP-RELAY] socket error: " + e.getMessage());
                }
                return;
            } catch (IOException e) {
                System.err.println("[UDP-RELAY] I/O error: " + e.getMessage());
            }
        }
    }

    private void handle(DatagramPacket in) {
        AudioPacket pkt = AudioPacket.decode(in.getData(), in.getOffset(), in.getLength());
        if (pkt == null) {
            return;
        }
        Map<String, Member> members = rooms.get(pkt.room);
        if (members == null) {
            return;
        }
        Member me = members.get(pkt.sender);
        if (me == null) {
            return; // no autorizado por Ice
        }

        SocketAddress from = in.getSocketAddress();
        if (pkt.type == AudioPacket.HELLO) {
            me.addr = from; // registro o re-registro
            return;
        }
        if (me.addr == null) {
            me.addr = from;
        } else if (!me.addr.equals(from)) {
            return; // origen distinto al registrado: descartar (anti-suplantacion basica)
        }

        for (Map.Entry<String, Member> e : members.entrySet()) {
            SocketAddress dest = e.getValue().addr;
            if (dest == null || e.getKey().equals(pkt.sender)) {
                continue;
            }
            try {
                socket.send(new DatagramPacket(in.getData(), in.getOffset(), in.getLength(), dest));
            } catch (IOException ex) {
                // un destino caido no debe afectar a los demas
            }
        }
    }

    @Override
    public void close() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}
