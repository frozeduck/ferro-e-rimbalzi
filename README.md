# Ferro e Rimbalzi (React + Vite · Fantasy Nanico)

Gioco d'azione, logica e difesa della rocca nanica riscritta come applicazione web moderna in **React + TypeScript + Vite + Tailwind CSS**, ottimizzata per gameplay verticale (portrait) e giocabile sia su mobile che su desktop.

Basato sul progetto originale nativo di [frozeduck/ferro-e-rimbalzi](https://github.com/frozeduck/ferro-e-rimbalzi).

---

## Caratteristiche Principali

1. **Roccaforte dei Nani (Hub Interattivo Completo)**:
   * **Grande Fucina dei Fabbri**: forgia miglioramenti permanenti validi per tutte le spedizioni (Tempra dello Scudo per cuori vitali, Stiva Runicata per la capienza della sfera, Incudine d'Acciaio per il danno base).
   * **Taverna della Botte Bollente**: bevi un boccale di Idromele della Roccia, Birra di Fungo Silvano o Distillato del Drago per ottenere una benedizione/buff temporaneo per la prossima spedizione.
   * **Tesoreria Reale & Annali del Clan**: consulta il bilancio del regno (Ferro, Legname e Gemme) e l'albo storico dei record (spedizioni vinte, minerali estratti, legname tagliato, gemme pure sfaccettate, mostri sconfitti).
   * **Portale delle Gallerie**: consulta la mappa tattica dei biomi, seleziona e sblocca la destinazione (Miniera di Ferro, Bosco dei Nani o Caverna dei Cristalli) e avvia la spedizione.
   * **Salvataggio Persistente**: tutte le scorte, i potenziamenti del clan, i buff e i territori sbloccati sono salvati automaticamente con `localStorage`.

2. **Spedizione & Difesa della Miniera (Breakout Fantasy)**:
   * **Fase Cantiere (Build Phase)**: erigi Mura in pietra, Barricate in legno o Mura in ossidiana (3 Oro), e Baliste o Torrette Laser (6-7 Oro) spostabili sulla griglia di scalpellatura nanica.
   * **Fase Ondata (Wave Phase)**: mira e lancia la sfera magica runica, difendi la palizzata nanica respingendo le ondate di goblin, ladri koboldi, ragni silvani, treant, golem e spettri di cristallo.
   * **Blocchi Risorsa Arkanoid con Rimbalzo Solido**: giacimenti e ceppi d'albero con smusso 3D a vista, deformazione elastica, hit-flash e rilascio progressivo di risorse nella stiva della sfera.
   * **Forgiatura & Consegna**: attraversa la Fucina aperta (o Segheria / Banco Tagliapietre) per fondere il grezzo in risorse raffinate, poi deposita al Forziere Reale.
   * **Prismi & Multiball**: risonanza cristallina che sdoppia la sfera in un clone ausiliario.
   * **Talenti tra le Ondate**: seleziona artefatti nanici (Bisaccia rinforzata, Scudo maggiorato, Incanto runico).

3. **Sintesi Sonora Procedurale Polifonica**:
   * Motore audio procedurale in tempo reale tramite **Web Audio API** senza campioni audio esterni (deflessione dello scudo accordata in frequenza, rimbalzo pareti, molla boing funghi, risonanza arpeggiata dei cristalli, dardo balista, raggio laser, segheria, fanfara di vittoria).
   * Feedback aptico tramite `navigator.vibrate` per dispositivi compatibili.
   * Controlli da tastiera (Spazio per lanciare, Freccia Sinistra / Destra o A / D per muovere lo scudo, P per mettere in pausa).

---

## Sviluppo ed Esecuzione

```bash
# Avvia il server di sviluppo su porta 3000
npm run dev

# Compila l'applicazione per la produzione
npm run build
```
