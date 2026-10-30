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

    public static void playTone(final float hz, final float durationSec) {
        if (!soundEnabled) return;
        executor.execute(() -> {
            try {
                int numSamples = (int) (SAMPLE_RATE * durationSec);
                if (numSamples <= 0) return;
                short[] buffer = new short[numSamples];
                double angularFrequency = 2.0 * Math.PI * hz / SAMPLE_RATE;

                for (int i = 0; i < numSamples; i++) {
                    double decay = Math.exp(-3.5 * i / numSamples);
                    double sample = Math.sin(i * angularFrequency) * decay * 0.42;
                    buffer[i] = (short) (sample * 32767);
                }

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

                Thread.sleep((long) (durationSec * 1000) + 40);
                track.stop();
                track.release();
            } catch (Exception ignored) {
            }
        });
    }

    public static void playTone(float hz) {
        playTone(hz, 0.15f);
    }
}
