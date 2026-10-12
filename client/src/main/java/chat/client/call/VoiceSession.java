package chat.client.call;

import chat.client.audio.AudioCapture;
import chat.client.audio.AudioMixer;
import chat.common.audio.AudioPacket;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public final class VoiceSession implements AutoCloseable {

    private final String nick;
    private final String room;
    private final DatagramSocket socket;
    private final Consumer<String> warn;
    private final AtomicInteger txSeq = new AtomicInteger();
    private final Map<String, Integer> lastSeq = new ConcurrentHashMap<>();

    private volatile InetSocketAddress target;
    private volatile boolean relayMode;
    private volatile String expectedPeer;
    private volatile boolean muted;
    private volatile boolean closed;

    private AudioCapture capture;
    private AudioMixer mixer;
    private ScheduledExecutorService keepAlive;

    private VoiceSession(String nick, String room, DatagramSocket socket, Consumer<String> warn) {
        this.nick = nick;
        this.room = room == null ? "" : room;
        this.socket = socket;
        this.warn = warn;
    }

    /** Crea el socket UDP en un puerto libre (puerto 0 = lo asigna el SO). */
    public static VoiceSession open(String nick, String room, Consumer<String> warn)
            throws SocketException {
        return new VoiceSession(nick, room, new DatagramSocket(0), warn);
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }
     public static String localAddressToward(String serverHost) {
        try (DatagramSocket probe = new DatagramSocket()) {
            probe.connect(InetAddress.getByName(serverHost), 9);
            InetAddress a = probe.getLocalAddress();
            if (a != null && !a.isAnyLocalAddress()) {
                return a.getHostAddress();
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return "127.0.0.1";
    }

    public static InetSocketAddress resolve(String host, int port) throws IOException {
        return new InetSocketAddress(InetAddress.getByName(host), port);
    }

     public synchronized void start(InetSocketAddress target, boolean relayMode, String expectedPeer)
            throws LineUnavailableException {

        this.target = target;
        this.relayMode = relayMode;
        this.expectedPeer = expectedPeer;

        mixer = new AudioMixer();
        mixer.start();

        Thread rx = new Thread(this::receiveLoop, "udp-audio-rx");
        rx.setDaemon(true);
        rx.start();

        try {
            capture = new AudioCapture(this::sendFrame);
            capture.setMuted(muted);
            capture.start();
        } catch (LineUnavailableException e) {
            warn.accept("[AVISO] Sin microfono (" + e.getMessage() + "): solo podras escuchar.");
        }

        if (relayMode) {
            sendHello();
            keepAlive = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "udp-audio-hello");
                t.setDaemon(true);
                return t;
            });
            keepAlive.scheduleAtFixedRate(this::sendHello, 1, 3, TimeUnit.SECONDS);
        }
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
        if (capture != null) {
            capture.setMuted(muted);
        }
    }

    public boolean isMuted() {
        return muted;
    }



    private void sendHello() {
        send(new AudioPacket(AudioPacket.HELLO, 0, room, nick, null));
    }

    private void sendFrame(byte[] pcm) {
        send(new AudioPacket(AudioPacket.AUDIO, txSeq.getAndIncrement(), room, nick, pcm));
    }

    private void send(AudioPacket p) {
        if (closed || target == null) {
            return;
        }
        byte[] d = p.encode();
        try {
            socket.send(new DatagramPacket(d, d.length, target));
        } catch (IOException e) {
            // UDP: una trama perdida no se reintenta
        }
    }



    private void receiveLoop() {
        byte[] buf = new byte[AudioPacket.MAX_DATAGRAM];
        DatagramPacket in = new DatagramPacket(buf, buf.length);

        while (!closed) {
            try {
                in.setLength(buf.length);
                socket.receive(in);
            } catch (SocketException e) {
                return; // socket cerrado
            } catch (IOException e) {
                continue;
            }

            AudioPacket p = AudioPacket.decode(in.getData(), in.getOffset(), in.getLength());
            if (p == null || p.type != AudioPacket.AUDIO || p.sender.equals(nick)) {
                continue;
            }
            if (expectedPeer != null && !p.sender.equals(expectedPeer)) {
                continue;
            }
            if (relayMode && !p.room.equals(room)) {
                continue;
            }
            if (!acceptSeq(p.sender, p.seq)) {
                continue;
            }
            mixer.push(p.sender, p.payload);
        }
    }


    private boolean acceptSeq(String sender, int seq) {
        Integer last = lastSeq.get(sender);
        if (last != null) {
            int d = seq - last;
            if (d <= 0 && d > -50) {
                return false;
            }
        }
        lastSeq.put(sender, seq);
        return true;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (keepAlive != null) {
            keepAlive.shutdownNow();
        }
        if (capture != null) {
            capture.close();
        }
        socket.close();
        if (mixer != null) {
            mixer.close();
        }
    }
}
