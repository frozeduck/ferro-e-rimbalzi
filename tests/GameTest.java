package eu.sabotino.ferrorimbalzi;

public class GameTest {
    public static void main(String[] args) {
        System.out.println("Esecuzione test nativi Ferro e Rimbalzi (Layout Verticale)...");

        testIlFerroESpendibileSoloQuandoRaggiungeDeposito();
        testCapienzaConsumoDelCaricoEDanno();
        testCostruzioneEMovimentoRispettanoRisorseSpaziEFase();
        testLaSpondaRifletteLaVelocitaSenzaIntrappolareLaSfera();
        testRichiamareConservaIlCaricoERispettaTempoRicarica();
        testPerdereLaSferaPerdeIlCaricoECostaUnSoloPuntoIntegrita();
        testLeOndateConsentonoTalentiCostruzioneEConclusione();
        testIlTempoScadutoFaPagareINemiciZeroIntegritaTermina();
        testSpedizioneCompletaEVincibileUsandoSoltantoTiriRichiamiERacchetta();

        System.out.println("TUTTI I 9 TEST PASSATI CON SUCCESSO SUL NUOVO MODELLO VERTICALE! (9/9)");
    }

    private static void assertEquals(Object actual, Object expected, String message) {
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError(message + " - Atteso: " + expected + ", ma trovato: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void testIlFerroESpendibileSoloQuandoRaggiungeDeposito() {
        Game s = new Game();
        s.interact("mine");
        s.interact("mine");
        assertEquals(s.iron, 6, "Ferro iniziale deve rimanere 6");
        assertEquals(s.ball.ore, 2, "Minerale raccolto nella sfera deve essere 2");
        assertTrue(!s.interact("depot"), "Deposito non accetta minerale grezzo non fuso");

        s.forgeOpen = false;
        assertTrue(!s.interact("forge"), "Fonderia chiusa non trasforma il minerale");

        s.forgeOpen = true;
        s.interact("forge");
        assertEquals(s.ball.ore, 0, "Minerale trasformato deve azzerarsi");
        assertEquals(s.ball.metal, 2, "Ferro nella sfera deve essere 2");
        assertEquals(s.iron, 6, "Ferro spendibile non ancora incassato al deposito");

        s.interact("depot");
        assertEquals(s.iron, 8, "Ferro spendibile aumentato a 8");
        assertEquals(s.delivered, 2, "Ferro consegnato aumentato a 2");
        assertEquals(s.ball.metal, 0, "Sfera svuotata al deposito");
        assertTrue(!s.interact("depot"), "Deposito non interagisce senza ferro");
        System.out.println("✔ Il ferro è spendibile solo quando il carico trasformato raggiunge il deposito");
    }

    public static void testCapienzaConsumoDelCaricoEDanno() {
        Game s = new Game();
        for (int i = 0; i < 10; i++) s.interact("mine");
        assertEquals(s.ball.ore, 4, "Capienza massima iniziale rispettata (4)");

        Game.Enemy e = new Game.Enemy();
        e.hp = 30;
        e.x = 100;
        e.y = 100;
        s.hitEnemy(e);
        assertEquals(e.hp, 29, "Colpo normale toglie 1 danno base");
        assertEquals(s.ball.ore, 4, "Carico conservato se burn è disattivato");

        s.burn = true;
        s.hitEnemy(e);
        assertEquals(e.hp, 26, "Colpo con burn consuma minerale e toglie 3 danni");
        assertEquals(s.ball.ore, 3, "1 minerale consumato in attacco");

        s.interact("forge");
        s.hitEnemy(e);
        assertEquals(e.hp, 21, "Colpo con ferro consuma metallo e toglie 5 danni (+4 danno extra)");
        assertEquals(s.ball.metal, 2, "1 ferro consumato");
        assertEquals(s.stats.spentCargo, 2, "Statistica carico speso aggiornata");
        System.out.println("✔ Capienza, consumo del carico e danno sono coerenti");
    }

    public static void testCostruzioneEMovimentoRispettanoRisorseSpaziEFase() {
        Game s = new Game();
        assertTrue(!s.build("turret", 270, 400), "Sentinella costa 7 ferro (iniziale 6, deve fallire)");
        assertTrue(s.build("wall", 270, 600), "Sponda costa 3 ferro (deve riuscire)");
        assertEquals(s.iron, 3, "Ferro rimasto dopo acquisto sponda");
        assertTrue(!s.build("wall", 270, 600), "Non si può sovrapporre un edificio");
        assertTrue(!s.build("wall", -50, 400), "Fuori mappa vietato");
        assertTrue(!s.build("wall", 270, 100), "Costruzione fuori dalla zona mediana vietata");
        assertTrue(!s.build("invalid", 270, 400), "Tipo edificio sconosciuto vietato");
        assertTrue(s.moveBuilding("forge", 150, 500), "Spostamento in spazio valido consentito");

        s.startWave();
        assertTrue(!s.build("wall", 270, 600), "Costruzione vietata durante l'ondata");
        assertTrue(!s.moveBuilding("forge", 150, 500), "Spostamento vietato durante l'ondata");
        System.out.println("✔ Costruzione e movimento rispettano risorse, spazi e fase");
    }

    public static void testLaSpondaRifletteLaVelocitaSenzaIntrappolareLaSfera() {
        Game.Ball b = new Game.Ball();
        b.x = 200;
        b.y = 315;
        b.vx = 0;
        b.vy = 280;
        b.r = 8;
        assertTrue(Game.collide(b, 150, 320, 140, 22, true), "Collisione con sponda rilevata");
        assertTrue(b.vy < 0, "Velocità verticale invertita verso l'alto");
        assertTrue(b.y < 315, "Sfera separata dal corpo solido");
        System.out.println("✔ La sponda riflette la velocità senza intrappolare la sfera");
    }

    public static void testRichiamareConservaIlCaricoERispettaTempoRicarica() {
        Game s = new Game();
        s.startWave();
        s.interact("mine");
        s.launch();
        assertTrue(s.recall(), "Richiamo consentito durante l'ondata");
        assertEquals(s.ball.ore, 1, "Minerale conservato nel richiamo");
        assertTrue(s.ball.held, "Sfera trattenuta sulla racchetta");

        s.launch();
        assertTrue(!s.recall(), "Richiamo in cooldown non consentito");
        s.recall = 0;
        assertTrue(s.recall(), "Richiamo pronto al termine del cooldown");
        System.out.println("✔ Richiamare conserva il carico e rispetta il tempo di ricarica");
    }

    public static void testPerdereLaSferaPerdeIlCaricoECostaUnSoloPuntoIntegrita() {
        Game s = new Game();
        s.startWave();
        s.interact("mine");
        s.launch();
        s.ball.x = 200;
        s.ball.y = 975;
        s.ball.vx = 0;
        s.ball.vy = 400;
        s.step(0.02f);

        assertEquals(s.hp, 5, "Perdita sfera toglie 1 integrità al nucleo");
        assertEquals(s.ball.ore, 0, "Carico perso con la sfera");
        assertEquals(s.stats.lostCargo, 1, "Statistiche carico perso aggiornate");
        assertTrue(s.ball.held, "Sfera riposizionata sulla racchetta");

        s.step(0.02f);
        assertEquals(s.hp, 5, "Nessun danno extra nel frame successivo");
        System.out.println("✔ Perdere la sfera perde il carico e costa un solo punto integrità");
    }

    public static void testLeOndateConsentonoTalentiCostruzioneEConclusione() {
        Game s = new Game();
        for (int i = 1; i <= 5; i++) {
            assertTrue(s.startWave(), "Avvio ondata " + i);
            assertEquals(s.wave, i, "Numero ondata coerente");
            s.enemies.clear();
            s.time = 8;
            s.step(0.01f);
            if (i < 5) {
                assertEquals(s.phase, "upgrade", "Transizione a upgrade a fine ondata");
                assertTrue(s.chooseUpgrade("cargo"), "Scelta talento stiva");
                assertTrue(!s.chooseUpgrade("cargo"), "Talento già scelto non rieseguibile");
            }
        }
        assertEquals(s.phase, "short", "Consegne insufficienti portano a spedizione finita (short)");
        assertEquals(s.capacity, 12, "Capienza aumentata con i talenti scelti");

        Game winner = new Game();
        winner.wave = 4;
        winner.delivered = 14;
        winner.startWave();
        winner.enemies.clear();
        winner.time = 8;
        winner.step(0.01f);
        assertEquals(winner.phase, "won", "Raggiunto target 14 porta a vittoria");
        System.out.println("✔ Le ondate consentono talenti, costruzione e conclusione della spedizione");
    }

    public static void testIlTempoScadutoFaPagareINemiciZeroIntegritaTermina() {
        Game s = new Game();
        s.startWave();
        s.time = 40;
        s.hp = 1;
        s.step(0.01f);
        assertEquals(s.phase, "lost", "Integrità esaurita porta a sconfitta");
        assertEquals(s.hp, 0, "Integrità azzerata");
        System.out.println("✔ Il tempo scaduto fa pagare i nemici rimasti; zero integrità termina la partita");
    }

    public static void testSpedizioneCompletaEVincibileUsandoSoltantoTiriRichiamiERacchetta() {
        Game s = new Game();
        int iterations = 0;
        while (!java.util.List.of("won", "short", "lost").contains(s.phase) && iterations++ < 100000) {
            if ("build".equals(s.phase)) s.startWave();
            if ("upgrade".equals(s.phase)) s.chooseUpgrade(s.hp < 4 ? "paddle" : "damage");
            Game.Ball b = s.ball;
            float tx, ty;
            if (b.metal > 0) {
                tx = s.buildings.get(1).x + s.buildings.get(1).w / 2f;
                ty = s.buildings.get(1).y + s.buildings.get(1).h / 2f;
            } else if (b.ore > 0) {
                tx = s.buildings.get(0).x + s.buildings.get(0).w / 2f;
                ty = s.buildings.get(0).y + s.buildings.get(0).h / 2f;
            } else {
                tx = 270f;
                ty = 100f;
            }
            if (b.held) s.launch((float) Math.atan2(ty - b.y, tx - b.x));
            if (b.vy > 0) {
                float time = (s.paddle.y - b.r - b.y) / b.vy;
                float landing = b.x + b.vx * Math.max(0, time);
                float angle = (float) Math.atan2(ty - s.paddle.y, tx - landing);
                float offset = Game.clamp((float) ((angle + Math.PI / 2.0) / 1.15), -1f, 1f);
                s.paddle.x = Game.clamp(landing - offset * s.paddleWidth / 2f, s.paddleWidth / 2f + 14f, Game.W - s.paddleWidth / 2f - 14f);
            }
            if (s.recall <= 0f && (b.ore > 0 || b.metal > 0)) s.recall();
            s.step(1f / 120f);
        }
        assertEquals(s.phase, "won", "Spedizione completa vinta");
        assertTrue(s.delivered >= s.target, "Target 14 ferro raggiunto");
        assertTrue(s.stats.mined >= s.delivered, "Minerale estratto coerente con consegne");
        assertTrue(s.hp > 0, "Nucleo della città ancora integro");
        System.out.println("✔ Una spedizione completa è vincibile usando soltanto tiri, richiami e racchetta");
    }
}
