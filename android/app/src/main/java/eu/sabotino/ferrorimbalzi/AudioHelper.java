package eu.sabotino.ferrorimbalzi;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioHelper {
    private static final int SAMPLE_RATE = 22050;
    // Pool di 3 thread per consentire sovrapposizione polifonica naturale degli effetti sonori
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
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

                long durMs = (long) ((float) buffer.length / SAMPLE_RATE * 1000f) + 15;
                Thread.sleep(Math.min(durMs, 400));
                track.stop();
                track.release();
            } catch (Exception ignored) {
            }
        });
    }

    // Rimbalzo sullo scudo nanico (Paddle Deflect) - Frequenza accordata alla posizione d'impatto
    public static void playShieldDeflect(float hitOffsetRatio) {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.11f);
        short[] buf = new short[n];
        // Centro = tono più profondo e roccioso, bordo = squillo metallico più acuto
        float baseFreq = 380f + Math.abs(hitOffsetRatio) * 160f;
        double p1 = 0, p2 = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p1 += 2.0 * Math.PI * baseFreq / SAMPLE_RATE;
            p2 += 2.0 * Math.PI * (baseFreq * 2.15f) / SAMPLE_RATE;
            float decay = (float) Math.exp(-6.5 * t);
            double noise = (Math.random() * 2.0 - 1.0) * 0.18 * (1f - t);
            double s = (Math.sin(p1) * 0.65 + Math.sin(p2) * 0.25 + noise) * decay * 0.6;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Rimbalzo secco sulle pareti di roccia e travi perimetrali (Wall Clack)
    public static void playWallClack() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.045f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float freq = 340f * (1f - t * 0.5f);
            p += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float decay = (float) Math.exp(-12.0 * t);
            double noise = (Math.random() * 2.0 - 1.0) * 0.25 * (1f - t);
            double s = (Math.sin(p) * 0.65 + noise) * decay * 0.45;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Rimbalzo elastico sui Funghi Bouncer del Bosco (Springy Boing)
    public static void playMushroomBounce() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.18f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            // Sweep di frequenza ascendente caratteristico da rimbalzo molla
            float freq = 220f + 360f * (float) Math.sin(t * Math.PI * 0.9);
            p += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float decay = (float) Math.exp(-3.5 * t);
            double s = Math.sin(p) * decay * 0.55;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Risonanza cristallina dei prismi e gemme pure (Crystal Chime / Multiball)
    public static void playCrystalChime() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.26f);
        short[] buf = new short[n];
        // Arpeggio cristallino su tre toni armonici scintillanti (Mi6, La6, Mi7)
        float[] freqs = { 1318f, 1760f, 2637f };
        int subLen = n / 3;
        for (int seg = 0; seg < 3; seg++) {
            float f = freqs[seg];
            double p1 = 0, p2 = 0;
            for (int i = 0; i < subLen; i++) {
                int idx = seg * subLen + i;
                float segT = (float) i / subLen;
                p1 += 2.0 * Math.PI * f / SAMPLE_RATE;
                p2 += 2.0 * Math.PI * (f * 1.5f) / SAMPLE_RATE;
                float decay = (float) Math.exp(-4.2 * segT);
                double s = (Math.sin(p1) * 0.75 + Math.sin(p2) * 0.25) * decay * 0.48;
                buf[idx] = (short) (s * 32767);
            }
        }
        playBuffer(buf);
    }

    // Raggio laser focale ad alta energia (Laser Turret Discharge)
    public static void playLaserBeam() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.12f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            // Rapida discesa di frequenza sci-fi focalizzata
            float freq = 1600f * (1f - t * 0.78f);
            p += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float decay = (float) Math.exp(-3.8 * t);
            double squareHarmonic = Math.sin(p) > 0 ? 0.35 : -0.35;
            double s = (Math.sin(p) * 0.55 + squareHarmonic * 0.3) * decay * 0.52;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Segheria meccanica runica: lama dentata rotante sul legname (Sawmill Buzz)
    public static void playSawmillBuzz() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.16f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float freq = 140f + (float) Math.sin(t * 50f) * 25f;
            p += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float decay = (float) Math.exp(-2.2 * t);
            double noise = (Math.random() * 2.0 - 1.0) * 0.45;
            double s = (Math.sin(p) * 0.45 + noise * 0.55) * decay * 0.5;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Scalpellata d'estrazione minerali, ceppi o quarzo (Mining Strike)
    public static void playMiningStrike(boolean isCrystal, boolean isForest) {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.10f);
        short[] buf = new short[n];
        float baseF = isCrystal ? 1480f : (isForest ? 260f : 780f);
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p += 2.0 * Math.PI * baseF / SAMPLE_RATE;
            float decay = (float) Math.exp(-6.8 * t);
            double noise = (Math.random() * 2.0 - 1.0) * (isForest ? 0.4 : 0.2);
            double s = (Math.sin(p) * 0.7 + noise) * decay * 0.45;
            buf[i] = (short) (s * 32767);
        }
        playBuffer(buf);
    }

    // Clic tattile delicato in legno per i pulsanti UI (Button Click)
    public static void playButtonClick() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.035f);
        short[] buf = new short[n];
        double p = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            p += 2.0 * Math.PI * 620f / SAMPLE_RATE;
            float decay = (float) Math.exp(-14.0 * t);
            buf[i] = (short) (Math.sin(p) * decay * 0.35 * 32767);
        }
        playBuffer(buf);
    }

    // Fanfara nanica solenne di vittoria (Victory Fanfare)
    public static void playVictoryFanfare() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.44f);
        short[] buf = new short[n];
        // Accordo maestoso D4 (293.7), F#4 (370.0), A4 (440.0), D5 (587.3)
        float[] chord = { 293.7f, 370.0f, 440.0f, 587.3f };
        int subLen = n / 4;
        for (int seg = 0; seg < 4; seg++) {
            float f = chord[seg];
            double p = 0;
            for (int i = 0; i < subLen; i++) {
                int idx = seg * subLen + i;
                p += 2.0 * Math.PI * f / SAMPLE_RATE;
                float segT = (float) i / subLen;
                float env = segT < 0.1f ? (segT / 0.1f) : (float) Math.exp(-2.2 * (segT - 0.1f));
                buf[idx] = (short) (Math.sin(p) * env * 0.45 * 32767);
            }
        }
        playBuffer(buf);
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
