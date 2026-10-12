package chat.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.function.Consumer;

/**
 * Captura de microfono (javax.sound.sampled) en un hilo propio.
 * Entrega tramas PCM de exactamente FRAME_BYTES al sink. Mientras esta silenciado
 * sigue leyendo del microfono (para no acumular buffer) pero no entrega nada.
 */
public final class AudioCapture implements Runnable, AutoCloseable {

    private final TargetDataLine line;
    private final Consumer<byte[]> sink;
    private final Thread thread;
    private volatile boolean running = true;
    private volatile boolean muted;

    public AudioCapture(Consumer<byte[]> sink) throws LineUnavailableException {
        this.sink = sink;
        AudioFormat fmt = AudioConfig.format();
        try {
            line = AudioSystem.getTargetDataLine(fmt);
        } catch (IllegalArgumentException e) {
            throw new LineUnavailableException("no hay microfono compatible: " + e.getMessage());
        }
        line.open(fmt, AudioConfig.FRAME_BYTES * 8);
        thread = new Thread(this, "audio-capture");
        thread.setDaemon(true);
    }

    public void start() {
        line.start();
        thread.start();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    @Override
    public void run() {
        byte[] buf = new byte[AudioConfig.FRAME_BYTES];
        while (running) {
            int n;
            try {
                n = line.read(buf, 0, buf.length);
            } catch (RuntimeException e) {
                break; // la linea se cerro mientras leiamos
            }
            if (n <= 0) {
                if (!running || !line.isOpen()) {
                    break;
                }
                continue;
            }
            if (n == buf.length && !muted) {
                sink.accept(buf.clone());
            }
        }
    }

    @Override
    public void close() {
        running = false;
        line.stop();
        line.close();
    }
}