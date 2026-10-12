package chat.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reproduccion con mezcla. Cada emisor tiene su propia cola (mini jitter buffer) y
 * un unico hilo suma las tramas de todos los emisores activos y las escribe en UN
 * solo SourceDataLine. Escribir cada flujo directo al parlante mezcla bytes de forma
 * incoherente (audio ininteligible) en cuanto hay mas de un hablante.
 *
 * El write() bloqueante del SourceDataLine actua de reloj: el hilo escribe una trama
 * (o silencio) cada 20 ms de forma natural.
 */
public final class AudioMixer implements Runnable, AutoCloseable {

    private static final int PREBUFFER_FRAMES = 2;     // 40 ms antes de empezar a reproducir un emisor
    private static final int MAX_QUEUE_FRAMES = 10;    // 200 ms: si se acumula mas, se descarta lo viejo
    private static final long SOURCE_IDLE_MS = 3000;   // emisor sin datos por 3 s: se elimina

    private static final class Source {
        private final ArrayDeque<byte[]> queue = new ArrayDeque<>();
        private boolean playing;
        private long lastPush = System.currentTimeMillis();

        synchronized void push(byte[] frame) {
            queue.addLast(frame);
            while (queue.size() > MAX_QUEUE_FRAMES) {
                queue.pollFirst();
            }
            lastPush = System.currentTimeMillis();
        }

        synchronized byte[] poll() {
            if (!playing) {
                if (queue.size() < PREBUFFER_FRAMES) {
                    return null;
                }
                playing = true;
            }
            byte[] f = queue.pollFirst();
            if (f == null) {
                playing = false; // se vacio: volver a acumular colchon
            }
            return f;
        }

        synchronized boolean idle(long now) {
            return now - lastPush > SOURCE_IDLE_MS;
        }
    }

    private final Map<String, Source> sources = new ConcurrentHashMap<>();
    private final SourceDataLine line;
    private final Thread thread;
    private volatile boolean running = true;

    public AudioMixer() throws LineUnavailableException {
        AudioFormat fmt = AudioConfig.format();
        try {
            line = AudioSystem.getSourceDataLine(fmt);
        } catch (IllegalArgumentException e) {
            throw new LineUnavailableException("no hay altavoces compatibles: " + e.getMessage());
        }
        line.open(fmt, AudioConfig.FRAME_BYTES * 5); // 100 ms de buffer de salida
        thread = new Thread(this, "audio-mixer");
        thread.setDaemon(true);
    }

    public void start() {
        line.start();
        thread.start();
    }

    /** Encola una trama recibida de un emisor. */
    public void push(String sender, byte[] pcm) {
        if (pcm.length != AudioConfig.FRAME_BYTES) {
            return;
        }
        sources.computeIfAbsent(sender, k -> new Source()).push(pcm);
    }

    @Override
    public void run() {
        int[] acc = new int[AudioConfig.FRAME_SAMPLES];
        byte[] out = new byte[AudioConfig.FRAME_BYTES];

        while (running) {
            Arrays.fill(acc, 0);
            long now = System.currentTimeMillis();

            for (Map.Entry<String, Source> e : sources.entrySet()) {
                Source s = e.getValue();
                byte[] f = s.poll();
                if (f != null) {
                    for (int i = 0; i < acc.length; i++) {
                        acc[i] += (short) ((f[2 * i] & 0xFF) | (f[2 * i + 1] << 8));
                    }
                } else if (s.idle(now)) {
                    sources.remove(e.getKey(), s);
                }
            }

            for (int i = 0; i < acc.length; i++) {
                int v = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, acc[i])); // evita clipping
                out[2 * i] = (byte) v;
                out[2 * i + 1] = (byte) (v >> 8);
            }

            try {
                line.write(out, 0, out.length); // bloquea hasta que hay espacio: marca el ritmo
            } catch (RuntimeException e) {
                break; // la linea se cerro mientras escribiamos
            }
        }
    }

    @Override
    public void close() {
        running = false;
        line.stop();
        line.flush();
        line.close();
    }
}