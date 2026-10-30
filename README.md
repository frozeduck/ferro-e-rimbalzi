# Ferro e Rimbalzi (Android APK · Verticale)

Gioco d'azione, logica e difesa del nucleo nativo per **Android**, progettato da zero per essere giocato in **orientamento verticale (portrait)** su smartphone con una sola mano.

**[Scarica la Release v0.4.0 su GitHub](https://github.com/frozeduck/ferro-e-rimbalzi/releases/tag/v0.4.0)** · [Codice sorgente](https://github.com/frozeduck/ferro-e-rimbalzi)

---

## Panoramica del Gioco

*Ferro e Rimbalzi* unisce il dinamismo del Breakout / flipper con un'economia tattica di estrazione, fusione e difesa a ondate:

1. **Estrazione mineraria (Settore Superiore)**:
   * 3 giacimenti minerari attivi nella parte alta dell'arena rilasciano minerale grezzo quando colpiti dalla sfera.
2. **Fusione e Deposito (Settore Mediano)**:
   * **Fonderia**: con il comando dedicato puoi aprirla o chiuderla. Quando è aperta, la sfera che la attraversa trasforma istantaneamente tutto il minerale in **ferro raffinato**. Quando è chiusa, fa da sponda solida riflettente.
   * **Deposito**: quando la sfera carica di ferro entra nel deposito, incassa il ferro per acquistare difese e far progredire la missione.
3. **Difesa e Minaccia nemica**:
   * Ondate di insetti corazzati e ladri veloci scendono verso il fondo. Se un nemico vivo tocca la **linea di difesa**, fa breccia togliendo 1 cuore al nucleo.
   * **Carico Armato**: attiva questa modalità per bruciare un'unità di carico all'impatto con un nemico, triplicando i danni (+4 danno extra con ferro, +2 con minerale)!
   * **Richiamo di Emergenza**: richiama immediatamente la sfera alla racchetta per mettere in salvo il carico accumulato (tempo di ricarica: 12 secondi).
4. **Cantiere e Talenti fra le Ondate**:
   * Sposta liberamente gli edifici con il tocco per creare percorsi ottimali per la sfera.
   * Spendi il ferro incassato per erigere nuove **Sponde (3 Fe)** o **Torrette Sentinella (7 Fe)** che sparano laser automatici.
   * Scegli tra 3 talenti ad ogni ondata superata: **Stiva Rinforzata** (+2 capienza), **Racchetta Maggiorata** (+20px e +1 cuore), **Impatto Cinetico** (+1 danno base).
5. **Obiettivo**:
   * Resistere a 5 ondate e consegnare almeno **14 unità di ferro** al deposito.

---

## Caratteristiche Tecniche

* **100% Nativo Android**: Sviluppato in Java standard con `SurfaceView` e rendering hardware su `Canvas` a ~60 FPS. Nessun wrapper web, nessun browser, nessun framework pesante.
* **Layout Verticale Nativo (Portrait 9:16)**: Ottimizzato per display mobile con coordinate virtuali 540x960 e letterboxing adattivo ad alta precisione.
* **Audio Procedurale Sintetico**: Generazione sonora tramite `AudioTrack` con decadimento esponenziale sintetizzato al volo (zero file audio da scaricare).
* **100% Offline e Sicuro**: Nessun permesso di rete (`android.permission.INTERNET`) richiesto nel Manifest.

---

## Download e Installazione

Il pacchetto APK firmato pronto all'uso:
* 📦 **APK**: `downloads/ferro-e-rimbalzi-0.4.0-debug.apk`
* 🔒 **Checksum**: `downloads/ferro-e-rimbalzi-0.4.0-debug.apk.sha256`

Per installarlo tramite computer o adb:
```bash
adb install downloads/ferro-e-rimbalzi-0.4.0-debug.apk
```

---

## Compilazione e Test

Tutto il ciclo di vita è automatizzato da Gradle e da script locali:

```bash
# Esegue i 9 test unitari, compila l'APK e verifica l'integrità
./scripts/build-apk.sh

# Verifica di conformità (classes.dex, zero web, zero internet)
python3 scripts/check-apk.py
```

### Struttura del progetto

```
android/
├── app/
│   ├── src/main/java/eu/sabotino/ferrorimbalzi/
│   │   ├── MainActivity.java    # Activity immersiva verticale a schermo intero
│   │   ├── GameView.java        # SurfaceView a 60 FPS con comandi touch per smartphone
│   │   ├── Game.java            # Modello logico e fisico dell'arena verticale
│   │   └── AudioHelper.java     # Sintesi sonora procedurale con AudioTrack
│   ├── src/main/res/            # Risorse, colori e icona vettoriale
│   └── src/main/AndroidManifest.xml
downloads/                       # APK v0.3.0 compilato, sha256 e landing page
scripts/
├── build-apk.sh                 # Script di test e compilazione Gradle
└── check-apk.py                 # Validatore di integrità dell'APK
tests/
└── GameTest.java                # 9 test unitari sulle regole del gioco
.agents/skills/                  # Catalogo portable awesome-gamedev-agent-skills + router
```

