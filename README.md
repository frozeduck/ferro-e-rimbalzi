# Ferro e Rimbalzi (Android APK · Verticale · Fantasy Nanico)

Gioco d'azione, logica e difesa della rocca nanica nativo per **Android**, progettato da zero per essere giocato in **orientamento verticale (portrait)** su smartphone con una sola mano.

**[Scarica la Release v0.9.1 su GitHub](https://github.com/frozeduck/ferro-e-rimbalzi/releases/tag/v0.9.1)** · [Codice sorgente](https://github.com/frozeduck/ferro-e-rimbalzi)

---

## Panoramica del Gioco (Ambientazione Fantasy Nanico)

*Ferro e Rimbalzi* unisce il dinamismo del Breakout / flipper con un'economia tattica di estrazione mineraria, forgiatura e difesa dall'assedio goblin:

1. **Miniera di Ferro e Roccia (Settore Superiore)**:
   * 3 giacimenti montani nella roccia viva rilasciano minerale grezzo e schegge quando percossi dalla sfera runica.
2. **Forgia e Forziere Reale (Settore Mediano)**:
   * **Grande Fucina Nanica**: puoi aprirla o chiuderla. Aperta, accoglie la sfera nel crogiolo e fonde istantaneamente il minerale in **ferro raffinato**. Chiusa, il portone borchiato funge da sponda riflettente.
   * **Forziere del Regno**: quando la sfera carica di ferro entra nel forziere, incassa il ferro per erigere difese e finanziare la spedizione.
3. **Assedio dei Goblin e Minaccia alle Barricate**:
   * Orde di goblin invasori con elmi cornuti e ladri koboldi furtivi scendono lungo la galleria. Se toccano la **palizzata**, sottraggono integrità alla rocca nanica.
   * **Carico Armato**: infiamma la sfera con il fuoco del drago, sacrificando ferro o minerale per triplicare i danni all'impatto con i mostri!
   * **Richiamo dello Scudo**: richiama tempestivamente la sfera sullo scudo torre per mettere in salvo il carico (ricarica: 12 secondi).
4. **Cantiere dei Fabbri e Artefatti Runici**:
   * Trascina le strutture per creare percorsi di rimbalzo micidiali.
   * Spendi il ferro incassato per erigere **Mura in Pietra (3 Fe)** o **Baliste a Ripetizione (7 Fe)** che scagliano dardi infuocati automatici.
   * Scegli tra 3 artefatti runici ad ogni ondata respinta: **Bisaccia Rinforzata** (+2 capienza), **Scudo Torre Maggiorato** (+20px e +1 cuore), **Incanto Runico** (+1 danno).
5. **Obiettivo**:
   * Resistere a 5 ondate di assedio e consegnare almeno **14 lingotti di ferro** al forziere reale.

---

## Caratteristiche Tecniche

* **100% Nativo Android**: Sviluppato in Java standard con `SurfaceView` e rendering hardware su `Canvas` a ~60 FPS. Nessun wrapper web, nessun browser, nessun framework pesante.
* **Layout Verticale Nativo (Portrait 9:16)**: Ottimizzato per display mobile con coordinate virtuali 540x960 e letterboxing adattivo ad alta precisione.
* **Audio Procedurale Sintetico**: Generazione sonora tramite `AudioTrack` con decadimento esponenziale sintetizzato al volo (zero file audio da scaricare).
* **100% Offline e Sicuro**: Nessun permesso di rete (`android.permission.INTERNET`) richiesto nel Manifest.

---

## Download e Installazione

Il pacchetto APK firmato pronto all'uso:
* **APK**: `downloads/ferro-e-rimbalzi-0.9.1-debug.apk`
* **Checksum**: `downloads/ferro-e-rimbalzi-0.9.1-debug.apk.sha256`

Per installarlo tramite computer o adb:
```bash
adb install downloads/ferro-e-rimbalzi-0.9.1-debug.apk
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

