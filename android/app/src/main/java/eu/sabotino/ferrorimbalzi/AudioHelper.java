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
                Thread.sleep(Math.min(durMs, 300));
                track.stop();
                track.release();
            } catch (Exception ignored) {
            }
        });
    }

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

    public static void playLaser() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.09f);
        short[] buf = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float freq = 1200f * (1f - t * 0.75f);
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float decay = (1f - t);
            buf[i] = (short) (Math.sin(phase) * decay * 0.4f * 32767);
        }
        playBuffer(buf);
    }

    public static void playSmelt() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.18f);
        short[] buf = new short[n];
        double p1 = 0, p2 = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p1 += 2.0 * Math.PI * 520.0 / SAMPLE_RATE;
            p2 += 2.0 * Math.PI * 650.0 / SAMPLE_RATE;
            float decay = (float) Math.exp(-2.5 * t);
            double s = (Math.sin(p1) * 0.5 + Math.sin(p2) * 0.5) * decay * 0.45;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    public static void playDeliver() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.24f);
        short[] buf = new short[n];
        float[] freqs = { 440f, 554f, 659f, 880f };
        int subLen = n / 4;
        for (int seg = 0; seg < 4; seg++) {
            float f = freqs[seg];
            double p = 0;
            for (int i = 0; i < subLen; i++) {
                int idx = seg * subLen + i;
                p += 2.0 * Math.PI * f / SAMPLE_RATE;
                float segT = (float) i / subLen;
                float decay = (1f - segT * 0.35f);
                buf[idx] = (short) (Math.sin(p) * decay * 0.42f * 32767);
            }
        }
        playBuffer(buf);
    }

    public static void playExplosion() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.18f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float f = 140f * (1f - t * 0.6f);
            p += 2.0 * Math.PI * f / SAMPLE_RATE;
            double noise = (Math.random() * 2.0 - 1.0) * 0.4;
            float decay = (float) Math.exp(-3.5 * t);
            double s = (Math.sin(p) * 0.6 + noise) * decay * 0.55;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    public static void playBreach() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.22f);
        short[] buf = new short[n];
        double p1 = 0, p2 = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p1 += 2.0 * Math.PI * 300.0 / SAMPLE_RATE;
            p2 += 2.0 * Math.PI * 350.0 / SAMPLE_RATE;
            float decay = (float) Math.exp(-2.0 * t);
            double s = (Math.sin(p1) > 0 ? 0.4 : -0.4) * decay;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }
}
