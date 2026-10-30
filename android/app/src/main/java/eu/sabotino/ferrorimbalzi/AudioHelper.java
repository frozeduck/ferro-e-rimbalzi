package eu.sabotino.ferrorimbalzi;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioHelper {
    private static final int SAMPLE_RATE = 22050;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static volatile boolean soundEnabled = true;

    public static void toggleSound() {
        soundEnabled = !soundEnabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    private static void playBuffer(final short[] buffer) {
        if (!soundEnabled || buffer == null || buffer.length == 0) return;
        executor.execute(() -> {
            try {
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(buffer.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                track.write(buffer, 0, buffer.length);
                track.play();

                long durMs = (long) ((float) buffer.length / SAMPLE_RATE * 1000f) + 20;
                Thread.sleep(Math.min(durMs, 350));
                track.stop();
                track.release();
            } catch (Exception ignored) {
            }
        });
    }

    // Colpo metallico su incudine nanica (Anvil Clang)
    public static void playAnvil() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.16f);
        short[] buf = new short[n];
        double p1 = 0, p2 = 0, p3 = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p1 += 2.0 * Math.PI * 880.0 / SAMPLE_RATE;
            p2 += 2.0 * Math.PI * 1760.0 / SAMPLE_RATE;
            p3 += 2.0 * Math.PI * 2640.0 / SAMPLE_RATE;
            float decay = (float) Math.exp(-6.0 * t);
            double s = (Math.sin(p1) * 0.5 + Math.sin(p2) * 0.35 + Math.sin(p3) * 0.15) * decay * 0.5;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Scatto della balista nanica (Ballista Bowstring Twang)
    public static void playBallista() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.14f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float freq = 320f * (1f - t * 0.4f);
            p += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float decay = (float) Math.exp(-4.5 * t);
            double noise = (Math.random() * 2.0 - 1.0) * 0.2 * (1f - t);
            double s = (Math.sin(p) * 0.7 + noise) * decay * 0.55;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Fonderia: fuoco e mantice (Forge Fire Roar)
    public static void playForgeFire() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.20f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p += 2.0 * Math.PI * 180.0 / SAMPLE_RATE;
            double noise = (Math.random() * 2.0 - 1.0) * 0.5;
            float decay = (float) Math.exp(-2.2 * t);
            double s = (Math.sin(p) * 0.4 + noise * 0.6) * decay * 0.5;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Incasso forziere (Coins & Iron Clink)
    public static void playCoins() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.24f);
        short[] buf = new short[n];
        float[] freqs = { 659f, 880f, 1046f, 1318f };
        int subLen = n / 4;
        for (int seg = 0; seg < 4; seg++) {
            float f = freqs[seg];
            double p = 0;
            for (int i = 0; i < subLen; i++) {
                int idx = seg * subLen + i;
                p += 2.0 * Math.PI * f / SAMPLE_RATE;
                float segT = (float) i / subLen;
                float decay = (1f - segT * 0.45f);
                buf[idx] = (short) (Math.sin(p) * decay * 0.38f * 32767);
            }
        }
        playBuffer(buf);
    }

    // Grido / abbattimento goblin (Goblin Defeat)
    public static void playGoblinHit() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.16f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float f = 280f * (1f - t * 0.6f);
            p += 2.0 * Math.PI * f / SAMPLE_RATE;
            float decay = (float) Math.exp(-3.8 * t);
            double s = Math.sin(p) * decay * 0.5;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Corno di guerra nanico / Allarme breccia (Dwarven Warhorn)
    public static void playWarhorn() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.28f);
        short[] buf = new short[n];
        double p1 = 0, p2 = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p1 += 2.0 * Math.PI * 146.8 / SAMPLE_RATE; // D3
            p2 += 2.0 * Math.PI * 220.0 / SAMPLE_RATE; // A3
            float env = t < 0.2f ? (t / 0.2f) : (float) Math.exp(-2.0 * (t - 0.2f));
            double s = (Math.sin(p1) * 0.65 + Math.sin(p2) * 0.35) * env * 0.6;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Suono generico fallback
    public static void playTone(final float hz, final float durationSec) {
        if (!soundEnabled) return;
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        if (numSamples <= 0) return;
        short[] buffer = new short[numSamples];
        double angularFrequency = 2.0 * Math.PI * hz / SAMPLE_RATE;

        for (int i = 0; i < numSamples; i++) {
            double decay = Math.exp(-3.8 * i / numSamples);
            double sample = Math.sin(i * angularFrequency) * decay * 0.45;
            buffer[i] = (short) (sample * 32767);
        }
        playBuffer(buffer);
    }

    public static void playTone(float hz) {
        playTone(hz, 0.12f);
    }
}
