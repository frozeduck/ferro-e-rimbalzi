# Ferro e Rimbalzi (Android APK · Verticale · Fantasy Nanico)

Gioco d'azione, logica e difesa della rocca nanica nativo per **Android**, progettato da zero per essere giocato in **orientamento verticale (portrait)** su smartphone con una sola mano.

**[Scarica la Release v0.9.5 su GitHub](https://github.com/frozeduck/ferro-e-rimbalzi/releases/tag/v0.9.5)** · [Codice sorgente](https://github.com/frozeduck/ferro-e-rimbalzi)

---

## Panoramica del Gioco (Ambientazione Fantasy Nanico & Biomi Multipli)

*Ferro e Rimbalzi* unisce il dinamismo del Breakout / flipper con un'economia tattica di spedizioni, estrazione mineraria, segheria silvana e difesa dall'assedio di mostri:

### 1. Avamposto dei Nani (Hub & Progressione Basi)
* **Sala delle Spedizioni**: seleziona la missione tra i biomi sbloccati.
* **Scorte della Rocca**: accumula Ferro (`Fe`) e Legname per sbloccare nuovi territori nanici e potenziare le difese.

### 2. Bioma Miniera di Ferro (Miniera Superiore)
* 3 giacimenti di roccia viva per estrarre minerale di ferro.
* **Grande Fucina Nanica**: fonde il minerale in ferro raffinato spendibile.
* **Forziere del Regno**: deposita i lingotti per finanziare la rocca.
* Invasori: Goblin guerrieri e Ladri Koboldi.

### 3. Bioma Bosco dei Nani (Fase 2)
* Sbloccabile con 14 Ferro dalle riserve del Clan.
* **Ceppi di Alberi Antichi**: abbatti la corteccia per raccogliere tronchi di legname pregiato.
* **Segheria Meccanica Runica**: lama dentata d'acciaio rotante che taglia i tronchi in assi rinforzate.
* **Funghi Elastici (Bouncers)**: respingenti naturali fluorescenti che imprimono una vigorosa spinta elastica (+10% velocita) e rilasciano spore luminescenti.
* **Barricate di Tronchi & Baliste Silvane**: difese veloci da erigere per 3 Legno.
* **Ragni delle Radici & Bestie Silvane**: nemici agili con movimento serpeggiante sinusoidale.

---

## Caratteristiche Tecniche

* **100% Nativo Android**: Sviluppato in Java standard con `SurfaceView` e rendering hardware su `Canvas` a ~60 FPS. Nessun wrapper web, nessun browser, nessun framework pesante.
* **Layout Verticale Nativo (Portrait 9:16)**: Ottimizzato per display mobile con coordinate virtuali 540x960 e letterboxing adattivo ad alta precisione.
* **Audio Procedurale Sintetico**: Generazione sonora tramite `AudioTrack` con decadimento esponenziale sintetizzato al volo (zero file audio da scaricare).
* **100% Offline e Sicuro**: Nessun permesso di rete (`android.permission.INTERNET`) richiesto nel Manifest.

---

## Download e Installazione

Il pacchetto APK firmato pronto all'uso:
* **APK**: `downloads/ferro-e-rimbalzi-0.9.4-debug.apk`
* **Checksum**: `downloads/ferro-e-rimbalzi-0.9.4-debug.apk.sha256`

Per installarlo tramite computer o adb:
```bash
adb install downloads/ferro-e-rimbalzi-0.9.4-debug.apk
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

