package eu.sabotino.ferrorimbalzi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread gameThread;
    private volatile boolean running = false;
    private volatile boolean surfaceReady = false;
    private final SurfaceHolder holder;

    public Game game;
    public boolean paused = false;
    public boolean showTutorial = true; // Mostra il tutorial al primo avvio
    private String placingType = null;
    private Game.Building draggingBuilding = null;
    private float dragOffsetX = 0f, dragOffsetY = 0f;

    // Viewport reattivo 100% Fullscreen per qualsiasi proporzione di schermo (16:9, 19.5:9, 20:9, ecc.)
    private float viewScale = 1f;
    private float virtualH = 960f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    private long lastTimeNs = 0;
    private int lastStatsCount = 0;
    private int lastHp = 6;
    private int lastKills = 0;

    // Game Feel: Camera Trauma (pattern da game-feel skill)
    private float trauma = 0f;
    private float ballSquashX = 1f;
    private float ballSquashY = 1f;

    // Texture e Sprite Grafici Fantasy Nanici
    private Bitmap bmpMine;
    private Bitmap bmpForgeOpen;
    private Bitmap bmpForgeClosed;
    private Bitmap bmpVault;
    private Bitmap bmpWall;
    private Bitmap bmpTurret;
    private Bitmap bmpShield;
    private Bitmap bmpOrb;
    private Bitmap bmpGoblin;
    private Bitmap bmpBg;
    private final Paint paintPixel = new Paint();

    // Atmosfera sotterranea: Faville della forgia e pulviscolo di miniera
    private static class Sparkle {
        float x, y, speed, size;
        int alpha;
    }
    private final Sparkle[] sparkles = new Sparkle[50];

    // Particelle per impatti su roccia, fiamme e metallo fuso (Zero GC allocations)
    private static class Particle {
        boolean active = false;
        float x, y, vx, vy;
        float life, maxLife, size;
        int color;
        int type; // 0: scintille incudine, 1: braci forgia, 2: schegge roccia, 3: fuoco runico
    }
    private final Particle[] particles = new Particle[150];
    private int nextParticleIdx = 0;

    // Paints
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Reusable Paths & Rects per annullare Garbage Collection
    private final Path rockPath = new Path();
    private final Path shieldPath = new Path();
    private final Path goblinPath = new Path();
    private final RectF tempRect = new RectF();

    // Regioni touch dinamiche in coordinate virtuali (adattate all'altezza dello schermo)
    private final RectF btnHelp = new RectF(440f, 10f, 482f, 70f);
    private final RectF btnPause = new RectF(488f, 10f, 530f, 70f);
    private final RectF btnTutorialClose = new RectF(80f, 750f, 460f, 810f);

    // Pulsanti inferiori ergonomici (fase Ondata)
    private final RectF btnForge = new RectF(15f, 850f, 175f, 935f);
    private final RectF btnBurn = new RectF(185f, 850f, 345f, 935f);
    private final RectF btnRecall = new RectF(355f, 850f, 525f, 935f);

    // Pulsanti inferiori (fase Cantiere)
    private final RectF btnWall = new RectF(15f, 850f, 165f, 935f);
    private final RectF btnTurret = new RectF(175f, 850f, 325f, 935f);
    private final RectF btnStartWave = new RectF(335f, 850f, 525f, 935f);

    // Schede pergamena talenti
    private final RectF cardUpgrade1 = new RectF(30f, 210f, 510f, 360f);
    private final RectF cardUpgrade2 = new RectF(30f, 380f, 510f, 530f);
    private final RectF cardUpgrade3 = new RectF(30f, 550f, 510f, 700f);

    // Hub dell'Avamposto Nanico e Selezione Spedizioni
    private final RectF cardMineIron = new RectF(25f, 190f, 515f, 340f);
    private final RectF cardForestWood = new RectF(25f, 360f, 515f, 510f);
    private final RectF btnStartExpedition = new RectF(50f, 550f, 490f, 630f);
    private final RectF btnReturnHub = new RectF(150f, 650f, 390f, 715f);

    // Pulsanti finestra modale
    private final RectF btnResume = new RectF(150f, 480f, 390f, 545f);
    private final RectF btnRestart = new RectF(150f, 730f, 390f, 795f);
    private final RectF btnPauseRestart = new RectF(150f, 565f, 390f, 630f);

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        init(context);
    }

    private void init(Context context) {
        paintPixel.setFilterBitmap(false);
        paintPixel.setAntiAlias(false);
        paintPixel.setDither(false);

        game = new Game();
        lastHp = game.hp;
        loadBitmaps(context);

        // Inizializza faville e pulviscolo di caverna
        for (int i = 0; i < sparkles.length; i++) {
            sparkles[i] = new Sparkle();
            sparkles[i].x = (float) (Math.random() * Game.W);
            sparkles[i].y = (float) (Math.random() * virtualH);
            sparkles[i].speed = 10f + (float) (Math.random() * 26f);
            sparkles[i].size = 1f + (float) (Math.random() * 2.2f);
            sparkles[i].alpha = 50 + (int) (Math.random() * 140);
        }

        // Inizializza pool particelle
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
    }

    private void loadBitmaps(Context context) {
        try {
            android.content.res.Resources res = context.getResources();
            bmpMine = BitmapFactory.decodeResource(res, R.drawable.sprite_miniera);
            bmpForgeOpen = BitmapFactory.decodeResource(res, R.drawable.sprite_forgia_aperta);
            bmpForgeClosed = BitmapFactory.decodeResource(res, R.drawable.sprite_forgia_chiusa);
            bmpVault = BitmapFactory.decodeResource(res, R.drawable.sprite_forziere);
            bmpWall = BitmapFactory.decodeResource(res, R.drawable.sprite_muro);
            bmpTurret = BitmapFactory.decodeResource(res, R.drawable.sprite_balista);
            bmpShield = BitmapFactory.decodeResource(res, R.drawable.sprite_scudo);
            bmpOrb = BitmapFactory.decodeResource(res, R.drawable.sprite_sfera);
            bmpGoblin = BitmapFactory.decodeResource(res, R.drawable.sprite_goblin);
            bmpBg = BitmapFactory.decodeResource(res, R.drawable.sfondo_caverna);
        } catch (Throwable t) {
            android.util.Log.e("GameView", "Failed to load bitmaps", t);
        }
    }

    public void addTrauma(float t) {
        trauma = Math.min(1f, trauma + t);
    }

    public void spawnParticles(float x, float y, int count, int color, int type) {
        for (int i = 0; i < count; i++) {
            Particle p = particles[nextParticleIdx];
            nextParticleIdx = (nextParticleIdx + 1) % particles.length;
            p.active = true;
            p.x = x;
            p.y = y;
            float angle = (float) (Math.random() * Math.PI * 2.0);
            float speed = (type == 1) ? 15f + (float) (Math.random() * 40f) : 35f + (float) (Math.random() * 150f);
            if (type == 1) { // Braci della fornace che salgono
                p.vx = (float) (Math.cos(angle) * speed * 0.4f);
                p.vy = -Math.abs((float) (Math.sin(angle) * speed)) - 20f;
            } else {
                p.vx = (float) (Math.cos(angle) * speed);
                p.vy = (float) (Math.sin(angle) * speed);
            }
            p.life = 0.2f + (float) (Math.random() * 0.4f);
            p.maxLife = p.life;
            p.size = (type == 2) ? 3.5f + (float) (Math.random() * 3f) : 2f + (float) (Math.random() * 2.5f);
            p.color = color;
            p.type = type;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        pause();
    }

    private void updateViewport(int w, int h) {
        if (w <= 0 || h <= 0) return;
        viewScale = (float) w / Game.W;
        virtualH = (float) h / viewScale;
        offsetX = 0f;
        offsetY = 0f;

        game.setVirtualHeight(virtualH);
        updateDynamicLayout(virtualH);
    }

    private void updateDynamicLayout(float vH) {
        float btnTop = vH - 105f;
        float btnBottom = vH - 25f;

        btnForge.set(15f, btnTop, 175f, btnBottom);
        btnBurn.set(185f, btnTop, 345f, btnBottom);
        btnRecall.set(355f, btnTop, 525f, btnBottom);

        btnWall.set(15f, btnTop, 165f, btnBottom);
        btnTurret.set(175f, btnTop, 325f, btnBottom);
        btnStartWave.set(335f, btnTop, 525f, btnBottom);

        float midY = vH / 2f;
        cardUpgrade1.set(30f, midY - 265f, 510f, midY - 115f);
        cardUpgrade2.set(30f, midY - 95f, 510f, midY + 55f);
        cardUpgrade3.set(30f, midY + 75f, 510f, midY + 225f);

        btnResume.set(150f, midY + 10f, 390f, midY + 75f);
        btnRestart.set(150f, midY + 95f, 390f, midY + 160f);

        btnTutorialClose.set(80f, midY + 315f, Game.W - 80f, midY + 365f);
    }

    public void resume() {
        if (!running) {
            running = true;
            lastTimeNs = System.nanoTime();
            gameThread = new Thread(this, "GameLoop");
            gameThread.start();
        }
    }

    public void pause() {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException ignored) {}
            gameThread = null;
        }
    }

    public void togglePause() {
        paused = !paused;
    }

    public void resetGame() {
        game = new Game();
        game.setVirtualHeight(virtualH);
        paused = false;
        showTutorial = false;
        placingType = null;
        draggingBuilding = null;
        lastStatsCount = 0;
        lastHp = game.hp;
        lastKills = 0;
        trauma = 0f;
    }

    @Override
    public void run() {
        while (running) {
            if (!surfaceReady) {
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ignored) {}
                continue;
            }

            try {
                long now = System.nanoTime();
                float dt = (now - lastTimeNs) / 1_000_000_000f;
                lastTimeNs = now;
                if (dt > 0.05f) dt = 0.05f;

                if (!paused && !showTutorial) {
                    // Aggiorna pulviscolo e faville ambientali
                    for (Sparkle s : sparkles) {
                        s.y -= s.speed * dt;
                        if (s.y < 65f) {
                            s.y = virtualH;
                            s.x = (float) (Math.random() * Game.W);
                        }
                    }

                    // Aggiorna particelle
                    for (Particle p : particles) {
                        if (!p.active) continue;
                        p.life -= dt;
                        if (p.life <= 0f) {
                            p.active = false;
                            continue;
                        }
                        p.x += p.vx * dt;
                        p.y += p.vy * dt;
                        p.vx *= 0.95f;
                        p.vy *= 0.95f;
                    }

                    // Faville continue dalla forgia attiva
                    if (game.forgeOpen && "wave".equals(game.phase)) {
                        for (Game.Building b : game.buildings) {
                            if ("forge".equals(b.type) && Math.random() < 0.35) {
                                spawnParticles(b.x + (float) Math.random() * b.w, b.y + 6f, 1, 0xFFFF8A00, 1);
                            }
                        }
                    }

                    // Riacquisto forma della sfera
                    ballSquashX += (1f - ballSquashX) * 12f * dt;
                    ballSquashY += (1f - ballSquashY) * 12f * dt;

                    // Simulazione di gioco
                    game.step(dt);

                    // Feedback audio ed effetti
                    if (game.hp < lastHp) {
                        addTrauma(0.55f);
                        AudioHelper.playWarhorn();
                        lastHp = game.hp;
                    } else if (game.hp > lastHp) {
                        lastHp = game.hp;
                    }

                    if (game.kills > lastKills) {
                        addTrauma(0.22f);
                        AudioHelper.playGoblinHit();
                        lastKills = game.kills;
                    }

                    int statsCount = game.stats.mined + game.stats.smelted + game.delivered;
                    if (statsCount > lastStatsCount) {
                        if (game.stats.smelted > 0 && statsCount == lastStatsCount + 1 && game.ball.metal > 0) {
                            AudioHelper.playForgeFire();
                            addTrauma(0.12f);
                        } else if (game.delivered > 0 && statsCount == lastStatsCount + 1 && game.ball.metal == 0) {
                            AudioHelper.playCoins();
                            addTrauma(0.18f);
                        } else {
                            AudioHelper.playAnvil();
                        }
                        lastStatsCount = statsCount;
                    }

                    // Colpi di balista sparati
                    if (!game.shots.isEmpty() && Math.random() < 0.35) {
                        AudioHelper.playBallista();
                    }
                }

                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) {
                            drawGame(canvas, dt);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas);
                        } catch (Exception ignored) {}
                    }
                }

                long elapsedMs = (System.nanoTime() - now) / 1_000_000L;
                long sleepMs = 16L - elapsedMs;
                if (sleepMs > 0) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ignored) {}
                }
            } catch (Throwable t) {
                android.util.Log.e("GameView", "Game loop error", t);
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX() / viewScale;
        float touchY = event.getY() / viewScale;
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(touchX, touchY);
                return true;

            case MotionEvent.ACTION_MOVE:
                handleTouchMove(touchX, touchY);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleTouchUp(touchX, touchY);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleTouchDown(float tx, float ty) {
        if ("hub".equals(game.screen)) {
            if (cardMineIron.contains(tx, ty)) {
                game.selectedBiome = "mine_iron";
                AudioHelper.playAnvil();
            } else if (cardForestWood.contains(tx, ty)) {
                if (game.forestUnlocked) {
                    game.selectedBiome = "forest_wood";
                    AudioHelper.playAnvil();
                } else if (game.clanIron >= 14) {
                    game.unlockForest();
                    game.selectedBiome = "forest_wood";
                    AudioHelper.playWarhorn();
                } else {
                    AudioHelper.playAnvil();
                }
            } else if (btnStartExpedition.contains(tx, ty)) {
                game.startExpedition(game.selectedBiome);
                AudioHelper.playWarhorn();
            }
            return;
        }

        if (showTutorial) {
            if (btnTutorialClose.contains(tx, ty)) {
                showTutorial = false;
                AudioHelper.playAnvil();
            }
            return;
        }

        if (btnHelp.contains(tx, ty)) {
            showTutorial = true;
            AudioHelper.playAnvil();
            return;
        }

        if (btnPause.contains(tx, ty)) {
            togglePause();
            AudioHelper.playAnvil();
            return;
        }

        if (paused) {
            if (btnResume.contains(tx, ty)) {
                paused = false;
                AudioHelper.playAnvil();
            } else if (btnPauseRestart.contains(tx, ty)) {
                resetGame();
                AudioHelper.playAnvil();
            }
            return;
        }

        if ("upgrade".equals(game.phase)) {
            if (cardUpgrade1.contains(tx, ty)) {
                game.chooseUpgrade("cargo");
                AudioHelper.playAnvil();
                addTrauma(0.15f);
            } else if (cardUpgrade2.contains(tx, ty)) {
                game.chooseUpgrade("paddle");
                AudioHelper.playAnvil();
                addTrauma(0.15f);
            } else if (cardUpgrade3.contains(tx, ty)) {
                game.chooseUpgrade("damage");
                AudioHelper.playAnvil();
                addTrauma(0.15f);
            }
            return;
        }

        if ("won".equals(game.phase) || "short".equals(game.phase) || "lost".equals(game.phase)) {
            if (btnReturnHub.contains(tx, ty)) {
                game.returnToHub();
                AudioHelper.playAnvil();
            } else if (btnRestart.contains(tx, ty)) {
                resetGame();
                AudioHelper.playAnvil();
            }
            return;
        }

        if ("build".equals(game.phase)) {
            if (btnStartWave.contains(tx, ty)) {
                placingType = null;
                game.startWave();
                AudioHelper.playWarhorn();
                addTrauma(0.2f);
                return;
            }
            if (btnWall.contains(tx, ty)) {
                placingType = "wall".equals(placingType) ? null : "wall";
                AudioHelper.playAnvil();
                return;
            }
            if (btnTurret.contains(tx, ty)) {
                placingType = "turret".equals(placingType) ? null : "turret";
                AudioHelper.playAnvil();
                return;
            }

            if (placingType != null) {
                if (game.build(placingType, tx, ty)) {
                    AudioHelper.playAnvil();
                    spawnParticles(tx, ty, 16, 0xFFEAB308, 0);
                    placingType = null;
                }
                return;
            }

            for (int i = game.buildings.size() - 1; i >= 0; i--) {
                Game.Building b = game.buildings.get(i);
                if (tx >= b.x - 8f && tx <= b.x + b.w + 8f && ty >= b.y - 8f && ty <= b.y + b.h + 8f) {
                    draggingBuilding = b;
                    dragOffsetX = tx - b.x;
                    dragOffsetY = ty - b.y;
                    return;
                }
            }
        }

        if ("wave".equals(game.phase)) {
            if (btnForge.contains(tx, ty)) {
                game.forgeOpen = !game.forgeOpen;
                if (game.forgeOpen) AudioHelper.playForgeFire();
                else AudioHelper.playAnvil();
                return;
            }
            if (btnBurn.contains(tx, ty)) {
                game.burn = !game.burn;
                AudioHelper.playAnvil();
                return;
            }
            if (btnRecall.contains(tx, ty)) {
                if (game.recall()) {
                    AudioHelper.playWarhorn();
                    addTrauma(0.25f);
                    spawnParticles(game.ball.x, game.ball.y, 25, 0xFFF59E0B, 0);
                }
                return;
            }

            if (game.ball.held) {
                updateAim(tx, ty);
            } else {
                updatePaddle(tx);
            }
        }
    }

    private void handleTouchMove(float tx, float ty) {
        if (paused || showTutorial) return;

        if ("build".equals(game.phase) && draggingBuilding != null) {
            float targetX = tx - dragOffsetX + draggingBuilding.w / 2f;
            float targetY = ty - dragOffsetY + draggingBuilding.h / 2f;
            game.moveBuilding(draggingBuilding.id, targetX, targetY);
            return;
        }

        if ("wave".equals(game.phase)) {
            if (game.ball.held) {
                updateAim(tx, ty);
            } else {
                updatePaddle(tx);
            }
        }
    }

    private void handleTouchUp(float tx, float ty) {
        if (showTutorial) return;

        if ("build".equals(game.phase)) {
            draggingBuilding = null;
            return;
        }

        if ("wave".equals(game.phase)) {
            if (game.ball.held && ty < virtualH - 110f) {
                updateAim(tx, ty);
                if (game.launch()) {
                    AudioHelper.playAnvil();
                    ballSquashX = 0.72f;
                    ballSquashY = 1.3f;
                    spawnParticles(game.paddle.x, game.paddle.y, 16, 0xFFF59E0B, 0);
                }
            }
        }
    }

    private void updateAim(float tx, float ty) {
        float angle = (float) Math.atan2(ty - (game.paddle.y - 20f), tx - game.paddle.x);
        game.aim = Game.clamp(angle, (float) -Math.PI + 0.22f, -0.22f);
    }

    private void updatePaddle(float tx) {
        game.paddle.x = Game.clamp(tx, game.paddleWidth / 2f + 14f, Game.W - game.paddleWidth / 2f - 14f);
    }

    // ==========================================
    // RENDERING FANTASY NANICO 100% VISUALE
    // ==========================================

    private void drawGame(Canvas canvas, float dt) {
        float screenW = getWidth() > 0 ? (float) getWidth() : Game.W;
        float screenH = getHeight() > 0 ? (float) getHeight() : virtualH;

        // Base di fondo uniforme per evitare sfarfallii ai bordi estremi dello schermo
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF140C09);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        canvas.save();

        // Screen Shake nanico (game-feel)
        float shake = trauma * trauma;
        float shakeX = (float) (Math.sin(game.elapsed * 42f) * 12f * shake);
        float shakeY = (float) (Math.cos(game.elapsed * 38f) * 9f * shake);
        trauma = Math.max(0f, trauma - 1.4f * dt);

        canvas.translate(shakeX * viewScale, shakeY * viewScale);
        canvas.scale(viewScale, viewScale);

        // Rendering pixel art sfondo caverna SENZA stiramenti:
        // Ripetiamo la texture di mattoni verticalmente lungo virtualH mantenendo
        // rigorosamente il rapporto 1:1 dei pixel quadrati su qualsiasi schermo (Flip 7, 21:9, 16:9)
        if (bmpBg != null) {
            for (float y = 0f; y < virtualH; y += 960f) {
                tempRect.set(0f, y, Game.W, y + 960f);
                canvas.drawBitmap(bmpBg, null, tempRect, paintPixel);
            }
        }

        // Luce calda delle torce agli angoli superiori
        paintGlow.setStyle(Paint.Style.FILL);
        paintGlow.setShader(new RadialGradient(40f, 100f, 200f, 0x44FF8A00, 0x00FF8A00, Shader.TileMode.CLAMP));
        canvas.drawCircle(40f, 100f, 200f, paintGlow);
        paintGlow.setShader(new RadialGradient(Game.W - 40f, 100f, 200f, 0x44FF8A00, 0x00FF8A00, Shader.TileMode.CLAMP));
        canvas.drawCircle(Game.W - 40f, 100f, 200f, paintGlow);
        paintGlow.setShader(null);

        // Faville e pulviscolo dorato delle fucine
        for (Sparkle s : sparkles) {
            paintFill.setColor(Color.argb(s.alpha, 255, 170, 60));
            canvas.drawCircle(s.x, s.y, s.size, paintFill);
        }

        if ("hub".equals(game.screen)) {
            drawHub(canvas);
            canvas.restore();
            return;
        }

        // Griglia di costruzione: Cantiere di scalpellatura nanica
        if ("build".equals(game.phase)) {
            drawMasonryGrid(canvas);
        }

        // Pareti di roccia e travi di quercia borchiate (fino in fondo allo schermo)
        drawMinePerimeter(canvas);

        // Trincea difensiva dei Nani (Palizzata)
        drawDwarvenTrench(canvas);

        // Giacimenti minerari: Vene di roccia, cristalli e picconi incrociati
        for (int i = 0; i < game.mines.size(); i++) {
            drawOreVein(canvas, game.mines.get(i), i);
        }

        // Edifici: Forgia con incudine, Forziere ricolmo di tesori, Mura e Baliste
        for (Game.Building b : game.buildings) {
            drawBuilding(canvas, b);
        }

        // Dardi infuocati scagliati dalle Baliste
        for (Game.Shot shot : game.shots) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(3.5f);
            int alpha = (int) (255 * (shot.life / 0.2f));
            paintStroke.setColor(Color.argb(alpha, 245, 158, 11));
            canvas.drawLine(shot.x, shot.y, shot.tx, shot.ty, paintStroke);
            paintStroke.setStrokeWidth(1.5f);
            paintStroke.setColor(Color.argb(alpha, 255, 255, 230));
            canvas.drawLine(shot.x, shot.y, shot.tx, shot.ty, paintStroke);
        }

        // Invasori: Goblin guerrieri e Ladri Koboldi
        for (Game.Enemy e : game.enemies) {
            if (e.hp <= 0) continue;
            drawMonster(canvas, e);
        }

        // Lo Scudo Torre dei Nani (Racchetta)
        drawDwarvenShield(canvas);

        // Linea di mira runica
        if (game.ball.held && "wave".equals(game.phase) && !paused && !showTutorial) {
            drawRunicAim(canvas);
        }

        // Scia di fuoco runico della sfera
        drawRunicTrail(canvas);

        // La Sfera Magica Runica
        drawRunicOrb(canvas);

        // Particelle (Scintille di forgia, schegge rocciose)
        drawParticles(canvas);

        // Testo fluttuante (transitorio da impatto: danno, estrazione, fusione)
        drawEffects(canvas);

        // HUD fantasy: Stendardo di ferro e legno, stemmi scudo, pergamena
        drawHUD(canvas);

        // Finestre modali stile pergamena
        if (showTutorial) {
            drawTutorialOverlay(canvas);
        } else if (paused) {
            drawPauseOverlay(canvas);
        } else if ("upgrade".equals(game.phase)) {
            drawUpgradeOverlay(canvas);
        } else if ("won".equals(game.phase) || "short".equals(game.phase) || "lost".equals(game.phase)) {
            drawResultOverlay(canvas);
        }

        canvas.restore();
    }

    private void drawMasonryGrid(Canvas canvas) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x22D97706);
        paintStroke.setStrokeWidth(1f);
        float maxGridY = virtualH - 230f;
        for (float x = 20f; x < Game.W; x += 32f) {
            canvas.drawLine(x, 220f, x, maxGridY, paintStroke);
        }
        for (float y = 220f; y < maxGridY; y += 32f) {
            canvas.drawLine(16f, y, Game.W - 16f, y, paintStroke);
        }

        paintStroke.setColor(0x66D97706);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRect(16f, 220f, Game.W - 16f, maxGridY, paintStroke);
    }

    private void drawMinePerimeter(Canvas canvas) {
        float wallBottom = virtualH - 115f;

        // Trave portante superiore in legno di quercia
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF382214);
        canvas.drawRect(0f, 65f, Game.W, 73f, paintFill);

        // Travi laterali borchiate con ferro battuto fino al pannello inferiore
        paintFill.setColor(0xFF2E1C12);
        canvas.drawRect(0f, 65f, 14f, wallBottom, paintFill);
        canvas.drawRect(Game.W - 14f, 65f, Game.W, wallBottom, paintFill);

        // Borchie di ferro lungo tutta l'altezza dello schermo
        paintFill.setColor(0xFF78716C);
        for (float y = 90f; y < wallBottom - 10f; y += 50f) {
            canvas.drawCircle(7f, y, 2.5f, paintFill);
            canvas.drawCircle(Game.W - 7f, y, 2.5f, paintFill);
        }

        // Bordo in pietra lavorata
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF573E30);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(14f, 80f, 14f, wallBottom, paintStroke);
        canvas.drawLine(Game.W - 14f, 80f, Game.W - 14f, wallBottom, paintStroke);
    }

    private void drawDwarvenTrench(Canvas canvas) {
        float defenseY = virtualH - 200f;

        // Palizzata e trincea difensiva dei Nani
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2.5f);
        paintStroke.setColor(0xFFB91C1C);
        canvas.drawLine(14f, defenseY, Game.W - 14f, defenseY, paintStroke);

        // Punte di palizzata in legno
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF78350F);
        for (float x = 20f; x < Game.W - 20f; x += 22f) {
            rockPath.reset();
            rockPath.moveTo(x - 5f, defenseY + 4f);
            rockPath.lineTo(x, defenseY - 7f);
            rockPath.lineTo(x + 5f, defenseY + 4f);
            rockPath.close();
            canvas.drawPath(rockPath, paintFill);
        }
    }

    private void drawPreservingAspect(Canvas canvas, Bitmap bmp, float x, float y, float w, float h) {
        if (bmp == null) return;
        float bmpRatio = (float) bmp.getWidth() / bmp.getHeight();
        float boxRatio = w / h;
        float drawW = w;
        float drawH = h;
        if (bmpRatio > boxRatio) {
            drawH = w / bmpRatio;
        } else {
            drawW = h * bmpRatio;
        }
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        tempRect.set(cx - drawW / 2f, cy - drawH / 2f, cx + drawW / 2f, cy + drawH / 2f);
        canvas.drawBitmap(bmp, null, tempRect, paintPixel);
    }

    private void drawOreVein(Canvas canvas, Game.Mine m, int index) {
        if (bmpMine != null) {
            drawPreservingAspect(canvas, bmpMine, m.x, m.y, m.w, m.h);
        } else {
            tempRect.set(m.x, m.y, m.x + m.w, m.y + m.h);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF2D241E);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        }

        boolean flash = (game.elapsed - game.hits.getOrDefault("mine" + index, -10f)) < 0.18f;
        if (flash) {
            tempRect.set(m.x, m.y, m.x + m.w, m.y + m.h);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(3f);
            paintStroke.setColor(0xFFFEF3C7);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);
        }
    }

    private void drawBuilding(Canvas canvas, Game.Building b) {
        Bitmap bmp = null;
        if ("wall".equals(b.type)) {
            bmp = bmpWall;
        } else if ("forge".equals(b.type)) {
            bmp = game.forgeOpen ? bmpForgeOpen : bmpForgeClosed;
        } else if ("vault".equals(b.type) || "depot".equals(b.type)) {
            bmp = bmpVault;
        } else if ("turret".equals(b.type)) {
            bmp = bmpTurret;
        }

        if (bmp != null) {
            drawPreservingAspect(canvas, bmp, b.x, b.y, b.w, b.h);
        } else {
            tempRect.set(b.x, b.y, b.x + b.w, b.y + b.h);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor("forge".equals(b.type) ? 0xFF1C1917 : "wall".equals(b.type) ? 0xFF383532 : 0xFF3B1A0A);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        }

        // Bordo di selezione in cantiere
        if ("build".equals(game.phase) && draggingBuilding == b) {
            tempRect.set(b.x, b.y, b.x + b.w, b.y + b.h);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(3f);
            paintStroke.setColor(0xFFFBBF24);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);
        }
    }

    private void drawMonster(Canvas canvas, Game.Enemy e) {
        if (bmpGoblin != null) {
            drawPreservingAspect(canvas, bmpGoblin, e.x, e.y, e.w, e.h);
        } else {
            tempRect.set(e.x, e.y, e.x + e.w, e.y + e.h);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF16A34A);
            canvas.drawRoundRect(tempRect, 4f, 4f, paintFill);
        }

        // Barra vita goblin se ferito
        if (e.hp < e.maxHp) {
            float barW = e.w * 0.9f;
            float barH = 4f;
            float bx = e.x + (e.w - barW) / 2f;
            float by = e.y - 7f;
            paintFill.setColor(0xFF7F1D1D);
            canvas.drawRect(bx, by, bx + barW, by + barH, paintFill);
            paintFill.setColor(0xFF22C55E);
            float fillW = barW * ((float) e.hp / e.maxHp);
            canvas.drawRect(bx, by, bx + fillW, by + barH, paintFill);
        }
    }

    private void drawDwarvenShield(Canvas canvas) {
        float px = game.paddle.x;
        float py = game.paddle.y;
        float pw = game.paddleWidth;
        float ph = 24f;

        tempRect.set(px - pw / 2f, py - 4f, px + pw / 2f, py + ph + 4f);
        if (bmpShield != null) {
            canvas.drawBitmap(bmpShield, null, tempRect, paintPixel);
        } else {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF78350F);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        }
    }

    private void drawRunicAim(Canvas canvas) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setColor(0xCCF59E0B);

        float endX = game.ball.x + (float) Math.cos(game.aim) * 160f;
        float endY = game.ball.y + (float) Math.sin(game.aim) * 160f;
        canvas.drawLine(game.ball.x, game.ball.y, endX, endY, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFF59E0B);
        canvas.drawCircle(endX, endY, 4f, paintFill);

        paintText.setTextSize(12f);
        paintText.setColor(0xFFFEF3C7);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MIRA E RILASCIA PER LANCIARE LA SFERA RUNICA", Game.W / 2f, game.paddle.y - 36f, paintText);
    }

    private void drawRunicTrail(Canvas canvas) {
        int tSize = game.ball.trail.size();
        for (int i = tSize - 1; i >= 0; i--) {
            Game.PointF t = game.ball.trail.get(i);
            float ratio = 1f - (float) i / (tSize + 1);
            int alpha = (int) (140 * ratio);
            paintFill.setStyle(Paint.Style.FILL);
            if (game.burn) {
                paintFill.setColor(Color.argb(alpha, 249, 115, 22));
            } else {
                paintFill.setColor(Color.argb(alpha, 245, 158, 11));
            }
            canvas.drawCircle(t.x, t.y, game.ball.r * ratio * 0.95f, paintFill);
        }
    }

    private void drawRunicOrb(Canvas canvas) {
        canvas.save();
        canvas.translate(game.ball.x, game.ball.y);
        canvas.scale(ballSquashX, ballSquashY);

        float glowR = Math.max(5f, game.ball.r * 1.8f);
        paintGlow.setStyle(Paint.Style.FILL);
        if (game.burn) {
            paintGlow.setShader(new RadialGradient(0f, 0f, glowR,
                    0xFFFF5500, 0x00FF0000, Shader.TileMode.CLAMP));
        } else {
            paintGlow.setShader(new RadialGradient(0f, 0f, glowR,
                    0xFFF59E0B, 0x00D97706, Shader.TileMode.CLAMP));
        }
        canvas.drawCircle(0f, 0f, glowR, paintGlow);
        paintGlow.setShader(null);

        tempRect.set(-game.ball.r * 1.25f, -game.ball.r * 1.25f, game.ball.r * 1.25f, game.ball.r * 1.25f);
        if (bmpOrb != null) {
            canvas.drawBitmap(bmpOrb, null, tempRect, paintPixel);
        } else {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(game.burn ? 0xFFFFD700 : 0xFFE7E5E4);
            canvas.drawCircle(0f, 0f, game.ball.r, paintFill);
        }

        canvas.restore();

        if (game.cargo() > 0) {
            int total = game.cargo();
            for (int i = 0; i < total; i++) {
                double a = i * 2.0 * Math.PI / total + game.elapsed * 2.2;
                float px = game.ball.x + (float) Math.cos(a) * 18f;
                float py = game.ball.y + (float) Math.sin(a) * 11f;

                paintFill.setStyle(Paint.Style.FILL);
                if (i < game.ball.metal) {
                    paintFill.setColor(0xFFFBBF24);
                    canvas.drawRect(px - 4f, py - 3f, px + 4f, py + 3f, paintFill);
                    paintStroke.setStyle(Paint.Style.STROKE);
                    paintStroke.setStrokeWidth(1f);
                    paintStroke.setColor(0xFFFFFBEB);
                    canvas.drawRect(px - 4f, py - 3f, px + 4f, py + 3f, paintStroke);
                } else {
                    paintFill.setColor(0xFF78350F);
                    canvas.drawCircle(px, py, 3.5f, paintFill);
                    paintStroke.setStyle(Paint.Style.STROKE);
                    paintStroke.setStrokeWidth(1f);
                    paintStroke.setColor(0xFFFDE68A);
                    canvas.drawCircle(px, py, 3.5f, paintStroke);
                }
            }

            paintText.setTextSize(11f);
            paintText.setColor(0xFFFEF3C7);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(game.ball.ore + " Min · " + game.ball.metal + " Ferro", game.ball.x, game.ball.y - 20f, paintText);
        }
    }

    private void drawParticles(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        for (Particle p : particles) {
            if (!p.active) continue;
            float lifeRatio = p.life / p.maxLife;
            int alpha = (int) (255 * lifeRatio);
            paintFill.setColor((p.color & 0x00FFFFFF) | (alpha << 24));
            canvas.drawCircle(p.x, p.y, p.size * lifeRatio, paintFill);
        }
    }

    private void drawEffects(Canvas canvas) {
        for (Game.Effect eff : game.effects) {
            int alpha = (int) (255 * Game.clamp(eff.life, 0f, 1f));
            paintText.setColor(Color.argb(alpha / 2, 0, 0, 0));
            paintText.setTextSize(14f);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(eff.text, eff.x + 1.5f, eff.y + 1.5f, paintText);

            paintText.setColor((eff.color & 0x00FFFFFF) | (alpha << 24));
            canvas.drawText(eff.text, eff.x, eff.y, paintText);
        }
    }

    private void drawHUD(Canvas canvas) {
        float topBarH = 80f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0f, 0f, 0f, topBarH, 0xFF24150F, 0xFF140C09, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, Game.W, topBarH, paintFill);
        paintFill.setShader(null);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF78350F);
        paintStroke.setStrokeWidth(2f);
        canvas.drawLine(0f, topBarH, Game.W, topBarH, paintStroke);

        // 1. Scheda ONDATE (x=10f..105f)
        tempRect.set(10f, 10f, 105f, 70f);
        paintFill.setColor(0xFF2E190E);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        paintStroke.setColor(0xFFB45309);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(11f);
        paintText.setColor(0xFFD6D3D1);
        canvas.drawText("ASSEDIO", tempRect.centerX(), 28f, paintText);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText(game.wave + " / 5", tempRect.centerX(), 52f, paintText);

        // 2. Scheda INTEGRITA ROCCA (x=112f..240f)
        tempRect.set(112f, 10f, 240f, 70f);
        paintFill.setColor(0xFF26120D);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        paintStroke.setColor(0xFF991B1B);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(11f);
        paintText.setColor(0xFFFCA5A5);
        canvas.drawText("ROCCA: " + game.hp + "/" + game.maxHp, tempRect.centerX(), 28f, paintText);

        // Orbi vitali piu grandi, nitidi e contrastati
        float heartStartX = 126f;
        for (int i = 0; i < game.maxHp; i++) {
            float cx = heartStartX + i * 22f;
            boolean active = i < game.hp;
            paintFill.setColor(active ? 0xFFDC2626 : 0xFF3F3F46);
            canvas.drawCircle(cx, 48f, 7f, paintFill);
            if (active) {
                paintFill.setColor(0xFFFECACA);
                canvas.drawCircle(cx - 2f, 46f, 2.5f, paintFill);
            }
        }

        // 3. Scheda RISORSE & SPEDIZIONE (x=247f..375f)
        tempRect.set(247f, 10f, 375f, 70f);
        paintFill.setColor(0xFF2E190E);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        paintStroke.setColor(0xFFB45309);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(13f);
        paintText.setColor(0xFFF59E0B);
        canvas.drawText("Ferro: " + game.iron + " Fe", 257f, 32f, paintText);
        paintText.setColor(0xFF34D399);
        canvas.drawText("In Cassa: " + game.delivered + "/" + game.target, 257f, 54f, paintText);

        // 4. Scheda TEMPO (x=382f..432f)
        int sec = (int) Math.max(0, game.duration - game.time);
        tempRect.set(382f, 10f, 432f, 70f);
        paintFill.setColor(0xFF26120D);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        paintStroke.setColor(sec <= 5 ? 0xFFEF4444 : 0xFFB45309);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(9f);
        paintText.setColor(0xFFD6D3D1);
        canvas.drawText("TEMPO", tempRect.centerX(), 28f, paintText);
        paintText.setTextSize(16f);
        paintText.setColor(sec <= 5 ? 0xFFEF4444 : 0xFFFDE68A);
        canvas.drawText(sec + "s", tempRect.centerX(), 52f, paintText);

        // 5. Pulsante Aiuto / Tutorial (?) (x=440f..482f)
        paintFill.setColor(0xFF382214);
        canvas.drawRoundRect(btnHelp, 6f, 6f, paintFill);
        paintStroke.setColor(0xFFD97706);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnHelp, 6f, 6f, paintStroke);
        paintText.setColor(0xFFFEF3C7);
        paintText.setTextSize(18f);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("?", btnHelp.centerX(), btnHelp.centerY() + 6f, paintText);

        // 6. Pulsante Pausa (❚❚) (x=488f..530f)
        paintFill.setColor(0xFF382214);
        canvas.drawRoundRect(btnPause, 6f, 6f, paintFill);
        paintStroke.setColor(0xFFD97706);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnPause, 6f, 6f, paintStroke);
        paintText.setColor(0xFFFEF3C7);
        paintText.setTextSize(15f);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("❚❚", btnPause.centerX(), btnPause.centerY() + 5f, paintText);

        // ==========================================
        // BARRA INFERIORE A FILO SCHERMO INTERO
        // ==========================================
        float barH = 120f;
        float barTop = Math.max(0f, virtualH - barH);
        float barBottom = Math.max(barTop + 1f, virtualH);
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0f, barTop, 0f, barBottom, 0xEE1E110A, 0xFF120A05, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, barTop, Game.W, virtualH, paintFill);
        paintFill.setShader(null);

        paintStroke.setColor(0xFF573E30);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(0f, barTop, Game.W, barTop, paintStroke);

        if ("wave".equals(game.phase)) {
            drawFantasyButton(canvas, btnForge,
                    game.forgeOpen ? "FUCINA: APERTA" : "FUCINA: CHIUSA",
                    game.forgeOpen ? "Fonde il Minerale" : "Portone Riflettente",
                    game.forgeOpen ? 0xFFC2410C : 0xFF44403C,
                    game.forgeOpen ? 0xFFFFEDD5 : 0xFFD6D3D1);

            drawFantasyButton(canvas, btnBurn,
                    game.burn ? "CARICO: ARMATO" : "CARICO: SALVO",
                    game.burn ? "Danno x3 (Consuma)" : "Per il Forziere",
                    game.burn ? 0xFFB91C1C : 0xFF047857,
                    game.burn ? 0xFFFEE2E2 : 0xFFD1FAE5);

            boolean ready = game.recall <= 0f && !game.ball.held;
            String recallLbl = game.recall > 0f ? "RICHIAMO (" + (int) Math.ceil(game.recall) + "s)" : "RICHIAMA SFERA";
            drawFantasyButton(canvas, btnRecall,
                    recallLbl,
                    "Salva sullo Scudo",
                    ready ? 0xFFD97706 : 0xFF292524,
                    ready ? 0xFFFFFBEB : 0xFF78716C);

        } else if ("build".equals(game.phase)) {
            boolean selWall = "wall".equals(placingType);
            boolean canWall = game.iron >= Game.COST_WALL;
            drawFantasyButton(canvas, btnWall,
                    "+ MURA (3 Fe)",
                    selWall ? "▶ POSIZIONA..." : "Mura in pietra",
                    selWall ? 0xFF047857 : (canWall ? 0xFF065F46 : 0xFF292524),
                    canWall ? 0xFFD1FAE5 : 0xFF78716C);

            boolean selTurret = "turret".equals(placingType);
            boolean canTurret = game.iron >= Game.COST_TURRET;
            drawFantasyButton(canvas, btnTurret,
                    "+ BALISTA (7 Fe)",
                    selTurret ? "▶ POSIZIONA..." : "Dardi automatici",
                    selTurret ? 0xFFD97706 : (canTurret ? 0xFF92400E : 0xFF292524),
                    canTurret ? 0xFFFEF3C7 : 0xFF78716C);

            drawFantasyButton(canvas, btnStartWave,
                    "SUONA L'ALLARME ➔",
                    "Affronta l'ondata",
                    0xFFB91C1C,
                    0xFFFFFBEB);
        }
    }

    private void drawFantasyButton(Canvas canvas, RectF r, String title, String sub, int bgClr, int textClr) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(bgClr);
        canvas.drawRoundRect(r, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(1.5f);
        paintStroke.setColor(0x40FFFFFF);
        canvas.drawLine(r.left + 8f, r.top + 1f, r.right - 8f, r.top + 1f, paintStroke);

        paintStroke.setColor(textClr);
        canvas.drawRoundRect(r, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(13f);
        paintText.setColor(textClr);
        canvas.drawText(title, r.centerX(), r.centerY() - 2f, paintText);

        paintText.setTextSize(10f);
        paintText.setColor(Color.argb(190, Color.red(textClr), Color.green(textClr), Color.blue(textClr)));
        canvas.drawText(sub, r.centerX(), r.centerY() + 15f, paintText);
    }

    // ==========================================
    // TUTORIAL ILLUSTRATO INTERATTIVO
    // ==========================================

    private void drawTutorialOverlay(Canvas canvas) {
        paintFill.setColor(0xF6120B07);
        canvas.drawRect(0f, 0f, Game.W, virtualH, paintFill);

        float midY = virtualH / 2f;
        float cardTop = midY - 370f;
        float cardBottom = midY + 380f;
        tempRect.set(15f, cardTop, Game.W - 15f, cardBottom);

        // Pergamena del tutorial
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF26180F);
        canvas.drawRoundRect(tempRect, 10f, 10f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2.5f);
        paintStroke.setColor(0xFFD97706);
        canvas.drawRoundRect(tempRect, 10f, 10f, paintStroke);

        // Bordo interno dorato
        paintStroke.setStrokeWidth(1f);
        paintStroke.setColor(0x80FDE047);
        tempRect.inset(4f, 4f);
        canvas.drawRoundRect(tempRect, 8f, 8f, paintStroke);

        // Titolo
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(20f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("GUIDA ALLA ROCCA DEI NANI", Game.W / 2f, cardTop + 36f, paintText);

        paintText.setTextSize(12f);
        paintText.setColor(0xFFD6D3D1);
        canvas.drawText("Come riconoscere le strutture e difendere la miniera", Game.W / 2f, cardTop + 56f, paintText);

        // 4 Righe illustrate con icone grafiche
        float stepY = cardTop + 75f;

        drawTutorialCard(canvas, 25f, stepY, 0,
                "1. MINIERA DI FERRO",
                "Colpisci la roccia viva con la sfera runica per",
                "estrarre il minerale grezzo (massimo 4 pezzi).");

        stepY += 140f;
        drawTutorialCard(canvas, 25f, stepY, 1,
                "2. FORGIA DEI FABBRI",
                "Fai attraversare la sfera quando la fornace è ACCESA",
                "per fondere il minerale in ferro. Chiusa fa da sponda.");

        stepY += 140f;
        drawTutorialCard(canvas, 25f, stepY, 2,
                "3. FORZIERE REALE",
                "Fai entrare la sfera nel forziere per depositare il",
                "ferro: compri difese e vinci consegnando 14 Fe!");

        stepY += 140f;
        drawTutorialCard(canvas, 25f, stepY, 3,
                "4. DIFESA DELLA ROCCA",
                "Elimina i goblin prima che varchino la palizzata!",
                "'Carico Armato' triplica i danni; 'Richiama' salva la sfera.");

        // Pulsante "HO CAPITO ➔ ALLE ARMI!"
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFB91C1C);
        canvas.drawRoundRect(btnTutorialClose, 8f, 8f, paintFill);

        paintStroke.setColor(0xFFFCA5A5);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnTutorialClose, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("HO CAPITO ➔ ALLE ARMI!", btnTutorialClose.centerX(), btnTutorialClose.centerY() + 6f, paintText);
    }

    private void drawTutorialCard(Canvas canvas, float x, float y, int type, String title, String l1, String l2) {
        tempRect.set(x, y, Game.W - x, y + 125f);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF1B110B);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(1.2f);
        paintStroke.setColor(0xFF573E30);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

        // Icona grafica a sinistra
        float iconBoxW = 90f;
        float iconCx = x + iconBoxW / 2f;
        float iconCy = y + 62f;

        Bitmap cardBmp = null;
        if (type == 0) cardBmp = bmpMine;
        else if (type == 1) cardBmp = bmpForgeOpen;
        else if (type == 2) cardBmp = bmpVault;
        else if (type == 3) cardBmp = bmpTurret != null ? bmpTurret : bmpShield;

        if (cardBmp != null) {
            tempRect.set(iconCx - 36f, iconCy - 36f, iconCx + 36f, iconCy + 36f);
            canvas.drawBitmap(cardBmp, null, tempRect, paintPixel);
        }

        // Testi a destra
        float textX = x + iconBoxW + 8f;
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(13f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText(title, textX, y + 30f, paintText);

        paintText.setTextSize(11f);
        paintText.setColor(0xFFE7E5E4);
        canvas.drawText(l1, textX, y + 58f, paintText);
        canvas.drawText(l2, textX, y + 80f, paintText);
    }

    private void drawUpgradeOverlay(Canvas canvas) {
        paintFill.setColor(0xF0140C08);
        canvas.drawRect(0f, 0f, Game.W, virtualH, paintFill);

        float midY = virtualH / 2f;
        paintText.setTextSize(24f);
        paintText.setColor(0xFFFBBF24);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ONDATA RESPINTA!", Game.W / 2f, midY - 310f, paintText);

        paintText.setTextSize(14f);
        paintText.setColor(0xFFD6D3D1);
        canvas.drawText("I mastri fabbri nanici offrono un'artefatto:", Game.W / 2f, midY - 280f, paintText);

        drawParchmentCard(canvas, cardUpgrade1, "BISACCIA DA MINIERA RINFORZATA",
                "+2 Capienza Carico Sfera",
                "Permette di trasportare e fondere più minerale grezzo.",
                0xFFF59E0B);

        drawParchmentCard(canvas, cardUpgrade2, "SCUDO TORRE MAGGIORATO",
                "+20px Larghezza Scudo & +1 Cuore",
                "Allarga la difesa della rocca e ripara i danni subiti.",
                0xFF10B981);

        drawParchmentCard(canvas, cardUpgrade3, "INCANTO RUNICO PESANTE",
                "+1 Danno Base Sfera",
                "La sfera runica distrugge più rapidamente i goblin invasori.",
                0xFFEF4444);
    }

    private void drawParchmentCard(Canvas canvas, RectF r, String title, String bonus, String desc, int accentClr) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF2E1C12);
        canvas.drawRoundRect(r, 10f, 10f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setColor(accentClr);
        canvas.drawRoundRect(r, 10f, 10f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(15f);
        paintText.setColor(accentClr);
        canvas.drawText(title, r.centerX(), r.top + 45f, paintText);

        paintText.setTextSize(14f);
        paintText.setColor(0xFFFEF3C7);
        canvas.drawText(bonus, r.centerX(), r.top + 80f, paintText);

        paintText.setTextSize(12f);
        paintText.setColor(0xFFD6D3D1);
        canvas.drawText(desc, r.centerX(), r.top + 115f, paintText);
    }

    private void drawResultOverlay(Canvas canvas) {
        paintFill.setColor(0xF0140C08);
        canvas.drawRect(0f, 0f, Game.W, virtualH, paintFill);

        boolean won = "won".equals(game.phase);
        boolean shortIron = "short".equals(game.phase);
        float midY = virtualH / 2f;

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(32f);
        paintText.setColor(won ? 0xFF10B981 : shortIron ? 0xFFF59E0B : 0xFFEF4444);
        canvas.drawText(won ? "ROCCA SALVATA!" : shortIron ? "FERRO INSUFFICIENTE" : "LA ROCCA È CADUTA", Game.W / 2f, midY - 200f, paintText);

        paintText.setTextSize(15f);
        paintText.setColor(0xFFFEF3C7);
        String desc = won ? "Hai difeso le miniere e consegnato " + game.delivered + "/" + game.target + " Fe al forziere!"
                : shortIron ? "Sopravvissuto alle 5 ondate, ma servivano " + game.target + " Fe (raccolti: " + game.delivered + ")."
                : "I goblin hanno sfondato la palizzata nanica.";
        canvas.drawText(desc, Game.W / 2f, midY - 160f, paintText);

        tempRect.set(60f, midY - 120f, Game.W - 60f, midY + 60f);
        paintFill.setColor(0xFF2E1C12);
        canvas.drawRoundRect(tempRect, 8f, 8f, paintFill);
        paintStroke.setColor(0xFF78350F);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(tempRect, 8f, 8f, paintStroke);

        paintText.setTextSize(14f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("RESOCONTO DELLA BATTAGLIA", Game.W / 2f, midY - 85f, paintText);

        paintText.setColor(0xFFE7E5E4);
        canvas.drawText("Minerale grezzo estratto: " + game.stats.mined, Game.W / 2f, midY - 50f, paintText);
        canvas.drawText("Ferro forgiato nella fucina: " + game.stats.smelted, Game.W / 2f, midY - 25f, paintText);
        canvas.drawText("Goblin e mostri abbattuti: " + game.kills, Game.W / 2f, midY, paintText);
        canvas.drawText("Mura e baliste edificate: " + game.stats.built, Game.W / 2f, midY + 25f, paintText);

        btnReturnHub.set(100f, midY + 80f, Game.W - 100f, midY + 140f);
        btnRestart.set(100f, midY + 155f, Game.W - 100f, midY + 215f);

        paintFill.setColor(0xFF047857);
        canvas.drawRoundRect(btnReturnHub, 10f, 10f, paintFill);
        paintStroke.setColor(0xFF6EE7B7);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnReturnHub, 10f, 10f, paintStroke);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("TORNA ALL'AVAMPOSTO", btnReturnHub.centerX(), btnReturnHub.centerY() + 6f, paintText);

        paintFill.setColor(0xFFB91C1C);
        canvas.drawRoundRect(btnRestart, 10f, 10f, paintFill);
        paintStroke.setColor(0xFFFCA5A5);
        canvas.drawRoundRect(btnRestart, 10f, 10f, paintStroke);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("RIGIOCA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }

    private void drawPauseOverlay(Canvas canvas) {
        paintFill.setColor(0xD0140C08);
        canvas.drawRect(0f, 0f, Game.W, virtualH, paintFill);

        float midY = virtualH / 2f;
        btnResume.set(150f, midY - 10f, 390f, midY + 45f);
        btnPauseRestart.set(150f, midY + 60f, 390f, midY + 115f);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(28f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("BATTAGLIA SOSPESA", Game.W / 2f, midY - 70f, paintText);

        paintFill.setColor(0xFF047857);
        canvas.drawRoundRect(btnResume, 8f, 8f, paintFill);
        paintStroke.setColor(0xFF6EE7B7);
        canvas.drawRoundRect(btnResume, 8f, 8f, paintStroke);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("RIPRENDI", btnResume.centerX(), btnResume.centerY() + 6f, paintText);

        paintFill.setColor(0xFFB91C1C);
        canvas.drawRoundRect(btnPauseRestart, 8f, 8f, paintFill);
        paintStroke.setColor(0xFFFCA5A5);
        canvas.drawRoundRect(btnPauseRestart, 8f, 8f, paintStroke);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("RICOMINCIA", btnPauseRestart.centerX(), btnPauseRestart.centerY() + 6f, paintText);
    }

    private void drawHub(Canvas canvas) {
        // Testata Avamposto
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(22f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("AVAMPOSTO DEI NANI", Game.W / 2f, 75f, paintText);

        paintText.setTextSize(13f);
        paintText.setColor(0xFFD6D3D1);
        canvas.drawText("SALA DELLE SPEDIZIONI & RISERVE", Game.W / 2f, 102f, paintText);

        // Magazzino Riserve Centrali (Chip Oro/Ferro)
        tempRect.set(40f, 120f, Game.W - 40f, 175f);
        paintFill.setColor(0xFF26150F);
        canvas.drawRoundRect(tempRect, 8f, 8f, paintFill);
        paintStroke.setColor(0xFFB45309);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(tempRect, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(14f);
        paintText.setColor(0xFFF59E0B);
        canvas.drawText("SCORTE ROCCA: " + game.clanIron + " Ferro  |  " + game.clanWood + " Legname", tempRect.centerX(), 154f, paintText);

        // 1. Scheda Spedizione: MINIERA DI FERRO
        boolean selIron = "mine_iron".equals(game.selectedBiome);
        tempRect.set(cardMineIron);
        paintFill.setColor(selIron ? 0xFF351C12 : 0xFF20130C);
        canvas.drawRoundRect(tempRect, 10f, 10f, paintFill);
        paintStroke.setColor(selIron ? 0xFFF59E0B : 0xFF78350F);
        paintStroke.setStrokeWidth(selIron ? 3f : 1.5f);
        canvas.drawRoundRect(tempRect, 10f, 10f, paintStroke);

        // Icona Miniera a sinistra senza distorsioni
        drawPreservingAspect(canvas, bmpMine, cardMineIron.left + 16f, cardMineIron.top + 20f, 96f, 72f);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(17f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("MINIERA DI FERRO", cardMineIron.left + 125f, cardMineIron.top + 38f, paintText);

        paintText.setTextSize(12f);
        paintText.setColor(0xFFFEF3C7);
        canvas.drawText("Gimmick: Fucina fusa, sfera pesante", cardMineIron.left + 125f, cardMineIron.top + 64f, paintText);

        paintText.setColor(0xFFD6D3D1);
        canvas.drawText("Spacca la roccia e forgia lingotti.", cardMineIron.left + 125f, cardMineIron.top + 88f, paintText);

        paintText.setTextSize(13f);
        paintText.setColor(selIron ? 0xFF34D399 : 0xFF94A3B8);
        canvas.drawText(selIron ? "● SELEZIONATA" : "Tocca per selezionare", cardMineIron.left + 125f, cardMineIron.top + 120f, paintText);

        // 2. Scheda Spedizione: BOSCO DEI NANI
        boolean selForest = "forest_wood".equals(game.selectedBiome);
        tempRect.set(cardForestWood);
        paintFill.setColor(selForest ? 0xFF14291D : 0xFF171717);
        canvas.drawRoundRect(tempRect, 10f, 10f, paintFill);
        paintStroke.setColor(!game.forestUnlocked ? 0xFF52525B : selForest ? 0xFF10B981 : 0xFF047857);
        paintStroke.setStrokeWidth(selForest ? 3f : 1.5f);
        canvas.drawRoundRect(tempRect, 10f, 10f, paintStroke);

        // Icona Legno / Scudo a sinistra
        drawPreservingAspect(canvas, bmpShield, cardForestWood.left + 16f, cardForestWood.top + 20f, 96f, 72f);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(17f);
        paintText.setColor(game.forestUnlocked ? 0xFF34D399 : 0xFFA1A1AA);
        canvas.drawText("BOSCO DEI NANI", cardForestWood.left + 125f, cardForestWood.top + 38f, paintText);

        paintText.setTextSize(12f);
        paintText.setColor(0xFFE2E8F0);
        canvas.drawText("Gimmick: Ceppi elastici & segheria", cardForestWood.left + 125f, cardForestWood.top + 64f, paintText);

        if (game.forestUnlocked) {
            paintText.setColor(0xFFD6D3D1);
            canvas.drawText("Raccogli legname pregiato per baliste.", cardForestWood.left + 125f, cardForestWood.top + 88f, paintText);
            paintText.setTextSize(13f);
            paintText.setColor(selForest ? 0xFF34D399 : 0xFF94A3B8);
            canvas.drawText(selForest ? "● SELEZIONATA" : "Tocca per selezionare", cardForestWood.left + 125f, cardForestWood.top + 120f, paintText);
        } else {
            paintText.setColor(game.clanIron >= 14 ? 0xFFFBBF24 : 0xFFEF4444);
            canvas.drawText("Richiede 14 Ferro (Hai: " + game.clanIron + ")", cardForestWood.left + 125f, cardForestWood.top + 88f, paintText);
            paintText.setTextSize(13f);
            paintText.setColor(game.clanIron >= 14 ? 0xFFFBBF24 : 0xFF71717A);
            canvas.drawText(game.clanIron >= 14 ? "TOCCA PER SBLOCCARE (14 Fe)" : "BLOCCATA", cardForestWood.left + 125f, cardForestWood.top + 120f, paintText);
        }

        // Tasto centrale: AVVIA SPEDIZIONE
        paintFill.setColor(0xFF047857);
        canvas.drawRoundRect(btnStartExpedition, 10f, 10f, paintFill);
        paintStroke.setColor(0xFF6EE7B7);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawRoundRect(btnStartExpedition, 10f, 10f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(18f);
        paintText.setColor(0xFFFFFFFF);
        String startLabel = "mine_iron".equals(game.selectedBiome) ? "ENTRA NELLA MINIERA" : "ESPLORA IL BOSCO";
        canvas.drawText(startLabel, btnStartExpedition.centerX(), btnStartExpedition.centerY() + 7f, paintText);
    }
}
