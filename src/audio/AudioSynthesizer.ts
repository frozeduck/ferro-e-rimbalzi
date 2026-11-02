// Procedural Polyphonic Audio Synthesizer for Ferro e Rimbalzi
// Faithfully matches the exact mathematical PCM synthesis from AudioHelper.java

class SoundManager {
  private ctx: AudioContext | null = null;
  public enabled: boolean = true;
  private readonly SAMPLE_RATE = 22050;

  private getContext(): AudioContext | null {
    if (!this.enabled) return null;
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (AudioCtx) {
        this.ctx = new AudioCtx({ sampleRate: this.SAMPLE_RATE });
      }
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume().catch(() => {});
    }
    return this.ctx;
  }

  private playBuffer(generator: (buf: Float32Array, length: number) => void, length: number) {
    if (!this.enabled) return;
    try {
      const ctx = this.getContext();
      if (!ctx) return;

      const audioBuffer = ctx.createBuffer(1, length, this.SAMPLE_RATE);
      const channelData = audioBuffer.getChannelData(0);
      generator(channelData, length);

      const source = ctx.createBufferSource();
      source.buffer = audioBuffer;
      source.connect(ctx.destination);
      source.start();
    } catch {
      // Audio might be blocked before first user interaction
    }
  }

  public toggleSound(): boolean {
    this.enabled = !this.enabled;
    return this.enabled;
  }

  // Rimbalzo sullo scudo nanico (Paddle Deflect)
  public playShieldDeflect(hitOffsetRatio: number = 0) {
    const n = Math.floor(this.SAMPLE_RATE * 0.11);
    const baseFreq = 380 + Math.abs(hitOffsetRatio) * 160;
    this.playBuffer((buf) => {
      let p1 = 0, p2 = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        p1 += (2.0 * Math.PI * baseFreq) / this.SAMPLE_RATE;
        p2 += (2.0 * Math.PI * (baseFreq * 2.15)) / this.SAMPLE_RATE;
        const decay = Math.exp(-6.5 * t);
        const noise = (Math.random() * 2.0 - 1.0) * 0.18 * (1.0 - t);
        buf[i] = (Math.sin(p1) * 0.65 + Math.sin(p2) * 0.25 + noise) * decay * 0.6;
      }
    }, n);
  }

  // Rimbalzo secco sulle pareti di roccia (Wall Clack)
  public playWallClack() {
    const n = Math.floor(this.SAMPLE_RATE * 0.045);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        const freq = 340 * (1 - t * 0.5);
        p += (2.0 * Math.PI * freq) / this.SAMPLE_RATE;
        const decay = Math.exp(-12.0 * t);
        const noise = (Math.random() * 2.0 - 1.0) * 0.25 * (1 - t);
        buf[i] = (Math.sin(p) * 0.65 + noise) * decay * 0.45;
      }
    }, n);
  }

  // Rimbalzo elastico sui Funghi Bouncer del Bosco (Springy Boing)
  public playMushroomBounce() {
    const n = Math.floor(this.SAMPLE_RATE * 0.18);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        const freq = 220 + 360 * Math.sin(t * Math.PI * 0.9);
        p += (2.0 * Math.PI * freq) / this.SAMPLE_RATE;
        const decay = Math.exp(-3.5 * t);
        buf[i] = Math.sin(p) * decay * 0.55;
      }
    }, n);
  }

  // Risonanza cristallina dei prismi e gemme pure (Crystal Chime / Multiball)
  public playCrystalChime() {
    const n = Math.floor(this.SAMPLE_RATE * 0.26);
    const freqs = [1318, 1760, 2637];
    const subLen = Math.floor(n / 3);
    this.playBuffer((buf) => {
      for (let seg = 0; seg < 3; seg++) {
        const f = freqs[seg];
        let p1 = 0, p2 = 0;
        for (let i = 0; i < subLen; i++) {
          const idx = seg * subLen + i;
          const segT = i / subLen;
          p1 += (2.0 * Math.PI * f) / this.SAMPLE_RATE;
          p2 += (2.0 * Math.PI * (f * 1.5)) / this.SAMPLE_RATE;
          const decay = Math.exp(-4.2 * segT);
          buf[idx] = (Math.sin(p1) * 0.75 + Math.sin(p2) * 0.25) * decay * 0.48;
        }
      }
    }, n);
  }

  // Raggio laser focale ad alta energia (Laser Turret Discharge)
  public playLaserBeam() {
    const n = Math.floor(this.SAMPLE_RATE * 0.12);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        const freq = 1600 * (1 - t * 0.78);
        p += (2.0 * Math.PI * freq) / this.SAMPLE_RATE;
        const decay = Math.exp(-3.8 * t);
        const squareHarmonic = Math.sin(p) > 0 ? 0.35 : -0.35;
        buf[i] = (Math.sin(p) * 0.55 + squareHarmonic * 0.3) * decay * 0.52;
      }
    }, n);
  }

  // Segheria meccanica runica: lama dentata rotante sul legname (Sawmill Buzz)
  public playSawmillBuzz() {
    const n = Math.floor(this.SAMPLE_RATE * 0.16);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        const freq = 140 + Math.sin(t * 50) * 25;
        p += (2.0 * Math.PI * freq) / this.SAMPLE_RATE;
        const decay = Math.exp(-2.2 * t);
        const noise = (Math.random() * 2.0 - 1.0) * 0.45;
        buf[i] = (Math.sin(p) * 0.45 + noise * 0.55) * decay * 0.5;
      }
    }, n);
  }

  // Scalpellata d'estrazione minerali, ceppi o quarzo (Mining Strike)
  public playMiningStrike(isCrystal: boolean, isForest: boolean) {
    const n = Math.floor(this.SAMPLE_RATE * 0.1);
    const baseF = isCrystal ? 1480 : isForest ? 260 : 780;
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        p += (2.0 * Math.PI * baseF) / this.SAMPLE_RATE;
        const decay = Math.exp(-6.8 * t);
        const noise = (Math.random() * 2.0 - 1.0) * (isForest ? 0.4 : 0.2);
        buf[i] = (Math.sin(p) * 0.7 + noise) * decay * 0.45;
      }
    }, n);
  }

  // Clic tattile delicato in legno per i pulsanti UI (Button Click)
  public playButtonClick() {
    const n = Math.floor(this.SAMPLE_RATE * 0.035);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        p += (2.0 * Math.PI * 620) / this.SAMPLE_RATE;
        const decay = Math.exp(-14.0 * t);
        buf[i] = Math.sin(p) * decay * 0.35;
      }
    }, n);
  }

  // Fanfara nanica solenne di vittoria (Victory Fanfare)
  public playVictoryFanfare() {
    const n = Math.floor(this.SAMPLE_RATE * 0.44);
    const chord = [293.7, 370.0, 440.0, 587.3];
    const subLen = Math.floor(n / 4);
    this.playBuffer((buf) => {
      for (let seg = 0; seg < 4; seg++) {
        const f = chord[seg];
        let p = 0;
        for (let i = 0; i < subLen; i++) {
          const idx = seg * subLen + i;
          p += (2.0 * Math.PI * f) / this.SAMPLE_RATE;
          const segT = i / subLen;
          const env = segT < 0.1 ? segT / 0.1 : Math.exp(-2.2 * (segT - 0.1));
          buf[idx] = Math.sin(p) * env * 0.45;
        }
      }
    }, n);
  }

  // Colpo metallico su incudine nanica (Anvil Clang)
  public playAnvil() {
    const n = Math.floor(this.SAMPLE_RATE * 0.16);
    this.playBuffer((buf) => {
      let p1 = 0, p2 = 0, p3 = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        p1 += (2.0 * Math.PI * 880.0) / this.SAMPLE_RATE;
        p2 += (2.0 * Math.PI * 1760.0) / this.SAMPLE_RATE;
        p3 += (2.0 * Math.PI * 2640.0) / this.SAMPLE_RATE;
        const decay = Math.exp(-6.0 * t);
        buf[i] = (Math.sin(p1) * 0.5 + Math.sin(p2) * 0.35 + Math.sin(p3) * 0.15) * decay * 0.5;
      }
    }, n);
  }

  // Scatto della balista nanica (Ballista Bowstring Twang)
  public playBallista() {
    const n = Math.floor(this.SAMPLE_RATE * 0.14);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        const freq = 320 * (1 - t * 0.4);
        p += (2.0 * Math.PI * freq) / this.SAMPLE_RATE;
        const decay = Math.exp(-4.5 * t);
        const noise = (Math.random() * 2.0 - 1.0) * 0.2 * (1 - t);
        buf[i] = (Math.sin(p) * 0.7 + noise) * decay * 0.55;
      }
    }, n);
  }

  // Fonderia: fuoco e mantice (Forge Fire Roar)
  public playForgeFire() {
    const n = Math.floor(this.SAMPLE_RATE * 0.2);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        p += (2.0 * Math.PI * 180.0) / this.SAMPLE_RATE;
        const noise = (Math.random() * 2.0 - 1.0) * 0.5;
        const decay = Math.exp(-2.2 * t);
        buf[i] = (Math.sin(p) * 0.4 + noise * 0.6) * decay * 0.5;
      }
    }, n);
  }

  // Incasso forziere (Coins & Iron Clink)
  public playCoins() {
    const n = Math.floor(this.SAMPLE_RATE * 0.24);
    const freqs = [659, 880, 1046, 1318];
    const subLen = Math.floor(n / 4);
    this.playBuffer((buf) => {
      for (let seg = 0; seg < 4; seg++) {
        const f = freqs[seg];
        let p = 0;
        for (let i = 0; i < subLen; i++) {
          const idx = seg * subLen + i;
          p += (2.0 * Math.PI * f) / this.SAMPLE_RATE;
          const segT = i / subLen;
          const decay = 1.0 - segT * 0.45;
          buf[idx] = Math.sin(p) * decay * 0.38;
        }
      }
    }, n);
  }

  // Grido / abbattimento goblin (Goblin Defeat)
  public playGoblinHit() {
    const n = Math.floor(this.SAMPLE_RATE * 0.16);
    this.playBuffer((buf) => {
      let p = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        const f = 280 * (1 - t * 0.6);
        p += (2.0 * Math.PI * f) / this.SAMPLE_RATE;
        const decay = Math.exp(-3.8 * t);
        buf[i] = Math.sin(p) * decay * 0.5;
      }
    }, n);
  }

  // Corno di guerra nanico / Allarme breccia (Dwarven Warhorn)
  public playWarhorn() {
    const n = Math.floor(this.SAMPLE_RATE * 0.28);
    this.playBuffer((buf) => {
      let p1 = 0, p2 = 0;
      for (let i = 0; i < n; i++) {
        const t = i / n;
        p1 += (2.0 * Math.PI * 146.8) / this.SAMPLE_RATE; // D3
        p2 += (2.0 * Math.PI * 220.0) / this.SAMPLE_RATE; // A3
        const env = t < 0.2 ? t / 0.2 : Math.exp(-2.0 * (t - 0.2));
        buf[i] = (Math.sin(p1) * 0.65 + Math.sin(p2) * 0.35) * env * 0.6;
      }
    }, n);
  }
}

export const AudioHelper = new SoundManager();
