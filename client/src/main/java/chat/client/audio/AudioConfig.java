package chat.client.audio;

import javax.sound.sampled.AudioFormat;

/** Parametros de audio compartidos por captura, red y reproduccion. */
public final class AudioConfig {

    public static final float SAMPLE_RATE = 16000f;   // 16 kHz: voz clara con poco ancho de banda
    public static final int SAMPLE_BITS = 16;
    public static final int CHANNELS = 1;             // mono
    public static final int FRAME_MS = 20;            // una trama cada 20 ms (50 datagramas/s)

    /** 16000 * 0.020 = 320 muestras * 2 bytes = 640 bytes de PCM por trama. */
    public static final int FRAME_SAMPLES = (int) (SAMPLE_RATE * FRAME_MS / 1000);
    public static final int FRAME_BYTES = FRAME_SAMPLES * (SAMPLE_BITS / 8);

    private AudioConfig() {
    }

    /** PCM firmado, little-endian. */
    public static AudioFormat format() {
        return new AudioFormat(SAMPLE_RATE, SAMPLE_BITS, CHANNELS, true, false);
    }
}