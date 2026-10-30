# Ferro e Rimbalzi (Android APK)

Gioco d'azione e logica nativo per Android: Breakout, trasporto delle risorse e costruzioni fisiche nella stessa arena. Interfaccia in italiano, ottimizzata per touchscreen. Nessun account, nessuna connessione Internet richiesta (100% offline).

**[Scarica la Release v0.2.0 su GitHub](https://github.com/frozeduck/ferro-e-rimbalzi/releases/tag/v0.2.0)** · [Codice sorgente](https://github.com/frozeduck/ferro-e-rimbalzi)

## Caratteristiche del gioco

* **100% Nativo Android**: Sviluppato in Java con `SurfaceView` e rendering hardware su `Canvas`. Nessun browser, nessun WebView, nessun asset web.
* **Audio procedurale offline**: Effetti sonori sintetizzati in tempo reale con `AudioTrack` (nessun file audio pesante da scaricare).
* **Controlli touch reattivi**: Trascina la racchetta, tocca o rilascia per mirare e lanciare la sfera. Tra un'ondata e l'altra trascina e posiziona le strutture con aggancio a griglia.
* **Catena economica**:
  - **Giacimenti**: Raccogli minerale con la sfera.
  - **Fonderia**: Aperta trasforma il minerale in ferro; chiusa riflette la sfera come sponda.
  - **Deposito**: Incassa il ferro trasformato per acquistare difese e raggiungere l'obiettivo.
  - **Abilità**: Conserva o brucia il carico per infliggere danni devastanti ai nemici.
* **Obiettivo**: Sopravvivere a 5 ondate e consegnare almeno 14 unità di ferro al deposito. Tra le ondate si scelgono i talenti (Stiva, Rinforzo racchetta, Danni).

## Installazione e Download

Il pacchetto APK pronto per l'installazione si trova in:
* `downloads/ferro-e-rimbalzi-0.2.0-debug.apk`
* Checksum SHA256: `downloads/ferro-e-rimbalzi-0.2.0-debug.apk.sha256`

Per installarlo sul tuo dispositivo Android:
```bash
adb install downloads/ferro-e-rimbalzi-0.2.0-debug.apk
```

## Compilazione e Test

I comandi sono automatizzati tramite Gradle e script di verifica:

```bash
# Esegue tutti i 9 test unitari in Java, compila l'APK e ne verifica l'integrità
./scripts/build-apk.sh

# Oppure tramite npm
npm test
```

Lo script `./scripts/check-apk.py` valida automaticamente:
1. Integrità dello ZIP dell'APK.
2. Presenza di `classes.dex` (codice bytecode nativo compilato).
3. Assenza totale di asset web (`.html`, `.js`, `.css`).
4. Assenza del permesso `android.permission.INTERNET` (garanzia di funzionamento 100% offline).

## Struttura del progetto

```
android/
├── app/
│   ├── src/main/java/eu/sabotino/ferrorimbalzi/
│   │   ├── MainActivity.java    # Activity immersiva a schermo intero
│   │   ├── GameView.java        # SurfaceView con ciclo di rendering a ~60 FPS
│   │   ├── Game.java            # Modello di gioco, fisica e logica dell'arena
│   │   └── AudioHelper.java     # Sintesi audio procedurale con AudioTrack
│   ├── src/main/res/            # Icona vettoriale e risorse Android
│   └── src/main/AndroidManifest.xml
downloads/                       # APK compilato e checksum SHA256
scripts/
├── build-apk.sh                 # Script di build completo e test
└── check-apk.py                 # Validatore di integrità dell'APK
tests/
└── GameTest.java                # 9 test unitari sulle regole del gioco
```
