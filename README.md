# Ferro e Rimbalzi

Prototipo browser senza dipendenze: Breakout, trasporto delle risorse e costruzioni fisiche nella stessa arena. Interfaccia in italiano, mouse/tastiera/touch. Nessun account nel gioco e nessun salvataggio persistente.

**[Gioca su GitHub Pages](https://frozeduck.github.io/ferro-e-rimbalzi/)** · [Codice sorgente](https://github.com/frozeduck/ferro-e-rimbalzi)

Il gioco occupa la finestra senza scorrimento: menu iniziale, arena con HUD, barra delle abilità e menu di pausa. Gli strumenti di costruzione compaiono soltanto fra le ondate. Il campo mantiene le proporzioni quando si ridimensiona la finestra; il puntamento tiene conto delle bande laterali. Nessun font o asset esterno da scaricare.

## Prova

`npm start` serve `dist/` su http://127.0.0.1:8143. I file statici funzionano con qualsiasi server HTTP.

Prima dell'ondata trascina gli edifici o compra sponde (3 Fe) e torri (7 Fe). Avvia, mira e clicca per lanciare. Muovi la racchetta con mouse, tocco o frecce. Spazio lancia, F apre/chiude la fonderia, C conserva/consuma il carico, R richiama la sfera ogni 12 secondi, P mette in pausa. I giacimenti sono inesauribili per questo esperimento. La fonderia aperta trasforma il minerale al passaggio; il deposito incassa il ferro. La fonderia chiusa riflette la sfera. Ogni carico rallenta la sfera. In attacco il minerale aggiunge 2 danni, il ferro 4, consumando una risorsa per impatto.

Obiettivo: sopravvivere a 5 ondate e consegnare 14 ferro. Fra ondate si sceglie un talento e si ricevono 2 ferro di recupero. Questo bonus è distinto dalle consegne e non contribuisce all'obiettivo. Le risorse in viaggio passano all'ondata successiva; una sfera persa perde il carico. A fine ondata i nemici rimasti danneggiano il nucleo.

## Verifica

`npm test` verifica catena economica, consumo del carico, collisioni, limiti delle costruzioni, richiamo, perdita del carico e transizioni/vittoria/sconfitta.

La verifica opzionale nel browser richiede Playwright Core e Google Chrome disponibili nell'ambiente: `PLAYWRIGHT_CORE=/percorso/playwright-core/index.mjs CHROME=/percorso/chrome node tests/browser.mjs`. Il server locale deve essere avviato con `npm start`. Si possono anche passare il percorso di Playwright e l'URL del gioco come primo e secondo argomento del comando. Il controllo prova desktop, telefono verticale e orizzontale, assenza di scorrimento, menu, puntamento, costruzione, consegna, pausa e fine partita. Non serve per giocare né per pubblicare il gioco.

## Pubblicazione

Ogni push su `main` esegue i test e pubblica soltanto `dist/` su GitHub Pages tramite il workflow `.github/workflows/pages.yml`. Non servono dipendenze, build o chiavi aggiuntive. Il workflow si può avviare anche manualmente dalla scheda Actions.

## Cosa osservare giocando

1. Il percorso giacimento → fonderia → deposito si legge e si riesce a eseguire?
2. Spostare un edificio cambia in modo utile le traiettorie?
3. Ci sono momenti in cui conviene bruciare il carico e altri in cui conviene consegnarlo?

La novità della combinazione è un'ipotesi di design, non una certificazione di esclusività. Riferimenti della ricerca: BALL x PIT, Wizorb, Ballionaire, Breaktory, Pinly, Rotoball e Beast Breaker. Prima versione volutamente limitata: una sola arena, cinque ondate, due nemici e tre talenti; bilanciamento da verificare con partite umane.
