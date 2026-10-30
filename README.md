# Ferro e Rimbalzi (Android APK · Verticale · Fantasy Nanico)

Gioco d'azione, logica e difesa della rocca nanica nativo per **Android**, progettato da zero per essere giocato in **orientamento verticale (portrait)** su smartphone con una sola mano.

**[Scarica la Release v0.9.9 su GitHub](https://github.com/frozeduck/ferro-e-rimbalzi/releases/tag/v0.9.9)** · [Codice sorgente](https://github.com/frozeduck/ferro-e-rimbalzi)

---

## Panoramica del Gioco (Ambientazione Fantasy Nanico & Hub Interattivo)

*Ferro e Rimbalzi* unisce il dinamismo del Breakout / flipper con un'economia tattica di spedizioni, estrazione mineraria, segheria silvana e difesa dall'assedio di mostri:

### 1. Roccaforte dei Nani (Hub Interattivo Completo)
La piazza centrale dell'Avamposto offre 4 edifici interattivi visitabili toccandoli:
* **Grande Fucina dei Fabbri**: forgia miglioramenti permanenti validi per tutte le partite future (Tempra dello Scudo per cuori vitali, Stiva Runicata per la capienza della sfera, Incudine d'Acciaio per il danno base).
* **Taverna della Botte Bollente**: bevi un boccale di Idromele della Roccia, Birra di Fungo Silvano o Distillato del Drago per ottenere una benedizione/buff temporaneo per la prossima spedizione.
* **Camera del Tesoro & Annali Reali**: consulta il bilancio del regno (Ferro, Legname e Gemme) e l'albo storico dei record (spedizioni vinte, minerali estratti, legname tagliato, gemme pure sfaccettate, mostri sconfitti).
* **Portale delle Gallerie**: consulta la mappa tattica dei biomi, seleziona la destinazione (Miniera di Ferro, Bosco dei Nani o Caverna dei Cristalli) e avvia la spedizione sui binari.
* **Salvataggio Persistente**: tutte le scorte, i potenziamenti del clan, i buff e i territori sbloccati sono salvati automaticamente con `SharedPreferences`.

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

### 4. Bioma Caverna dei Cristalli (Fase 3)
* Sbloccabile con 18 Ferro + 14 Legname dalle riserve del Clan.
* **Giacimenti di Cristallo Puro**: estrai cristalli grezzi di quarzo e mithril.
* **Banco del Tagliapietre Runico**: rifinisce e sfaccetta i cristalli in gemme scintillanti.
* **Cassaforte delle Gemme**: forziere d'ossidiana rinforzato per custodire il tesoro runico.
* **Prismi di Risonanza Cristallina (Gimmick Multiball)**: colpendo i prismi la sfera si sdoppia! La seconda sfera clone aiuta a minare ed eliminare i nemici; se il clone cade si dissolve senza perdere cuori, e se la principale cade il clone la salva!
* **Barricate d'Ossidiana & Torrette Laser a Prisma**: difesa ad alta energia focalizzata da lenti prismatiche (320px di raggio, 2 danni).
* **Golem di Quarzo & Spettri di Cristallo**: nemici corazzati lenti ed entita eteree fluttuanti a traiettoria armonica.

---

## Caratteristiche Tecniche

* **100% Nativo Android**: Sviluppato in Java standard con `SurfaceView` e rendering hardware su `Canvas` a ~60 FPS. Nessun wrapper web, nessun browser, nessun framework pesante.
* **Layout Verticale Nativo (Portrait 9:16)**: Ottimizzato per display mobile con coordinate virtuali 540x960 e letterboxing adattivo ad alta precisione.
* **Safe Area & Display Cutout**: Compatibilita totale con fotocamere punch-hole e schermi ultra-allungati (Samsung Galaxy Z Flip 7, 22:9).
* **Grafica Pixel Art Fantasy Autentica (16-bit)**: 0% SVG vettoriale generico, palette 32 colori scalata con Nearest Neighbor pixel-per-pixel.
* **Audio Procedurale Sintetico**: Generazione sonora tramite `AudioTrack` con decadimento esponenziale sintetizzato al volo (zero file audio da scaricare).
* **100% Offline e Sicuro**: Nessun permesso di rete (`android.permission.INTERNET`) richiesto nel Manifest.

---

## Download e Installazione

Il pacchetto APK firmato pronto all'uso:
* **APK**: `downloads/ferro-e-rimbalzi-0.9.9-debug.apk`
* **Checksum**: `downloads/ferro-e-rimbalzi-0.9.9-debug.apk.sha256`

Per installarlo tramite computer o adb:
```bash
adb install downloads/ferro-e-rimbalzi-0.9.9-debug.apk
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

