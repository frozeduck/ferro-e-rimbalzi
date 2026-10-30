package eu.sabotino.ferrorimbalzi;

import android.content.Context;
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
    private final SurfaceHolder holder;

    public Game game;
    public boolean paused = false;
    private String placingType = null;
    private Game.Building draggingBuilding = null;
    private float dragOffsetX = 0f, dragOffsetY = 0f;

    private float viewScale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private long lastTimeNs = 0;
    private int lastStatsCount = 0;
    private int lastHp = 6;
    private int lastKills = 0;

    // Game Feel & Camera Trauma (Pattern 1 from game-feel skill)
    private float trauma = 0f;
    private float ballSquashX = 1f;
    private float ballSquashY = 1f;

    // Pre-allocated Starfield & Cosmic Dust
    private static class Star {
        float x, y, speed, size;
        int alpha;
    }
    private final Star[] stars = new Star[45];

    // Pre-allocated Particle Pool (Zero GC allocation)
    private static class Particle {
        boolean active = false;
        float x, y, vx, vy;
        float life, maxLife, size;
        int color;
        int type; // 0: spark, 1: ember, 2: debris, 3: plasma
    }
    private final Particle[] particles = new Particle[140];
    private int nextParticleIdx = 0;

    // Paints
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Reusable Paths & Rects to eliminate runtime GC allocations
    private final Path tempPath = new Path();
    private final Path asteroidPath = new Path();
    private final Path shipPath = new Path();
    private final Path enemyPath = new Path();
    private final RectF tempRect = new RectF();

    // Touch regions in virtual coordinates (Portrait W=540, H=960)
    private final RectF btnPause = new RectF(475f, 12f, 525f, 54f);

    // Wave Mode Bottom Buttons (Thumb Friendly)
    private final RectF btnForge = new RectF(15f, 850f, 175f, 935f);
    private final RectF btnBurn = new RectF(185f, 850f, 345f, 935f);
    private final RectF btnRecall = new RectF(355f, 850f, 525f, 935f);

    // Build Mode Bottom Buttons
    private final RectF btnWall = new RectF(15f, 850f, 165f, 935f);
    private final RectF btnTurret = new RectF(175f, 850f, 325f, 935f);
    private final RectF btnStartWave = new RectF(335f, 850f, 525f, 935f);

    // Upgrade Cards (Vertical stack for mobile portrait)
    private final RectF cardUpgrade1 = new RectF(30f, 210f, 510f, 360f);
    private final RectF cardUpgrade2 = new RectF(30f, 380f, 510f, 530f);
    private final RectF cardUpgrade3 = new RectF(30f, 550f, 510f, 700f);

    // Pause/Result Overlay Buttons
    private final RectF btnResume = new RectF(150f, 480f, 390f, 545f);
    private final RectF btnRestart = new RectF(150f, 565f, 390f, 630f);

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        init();
    }

    private void init() {
        game = new Game();
        lastHp = game.hp;

        // Initialize Starfield
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star();
            stars[i].x = (float) (Math.random() * Game.W);
            stars[i].y = (float) (Math.random() * Game.H);
            stars[i].speed = 12f + (float) (Math.random() * 28f);
            stars[i].size = 1f + (float) (Math.random() * 2.2f);
            stars[i].alpha = 60 + (int) (Math.random() * 150);
        }

        // Initialize Particle Pool
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
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
            float speed = (type == 1) ? 20f + (float) (Math.random() * 45f) : 40f + (float) (Math.random() * 160f);
            if (type == 1) { // Embers float upward
                p.vx = (float) (Math.cos(angle) * speed * 0.5f);
                p.vy = -Math.abs((float) (Math.sin(angle) * speed)) - 25f;
            } else if (type == 3) { // Thruster plasma flies down
                p.vx = ((float) Math.random() - 0.5f) * 35f;
                p.vy = 60f + (float) (Math.random() * 90f);
            } else {
                p.vx = (float) (Math.cos(angle) * speed);
                p.vy = (float) (Math.sin(angle) * speed);
            }
            p.life = 0.2f + (float) (Math.random() * 0.45f);
            p.maxLife = p.life;
            p.size = (type == 2) ? 3.5f + (float) (Math.random() * 3f) : 2f + (float) (Math.random() * 2.5f);
            p.color = color;
            p.type = type;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    private void updateViewport(int w, int h) {
        viewScale = Math.min((float) w / Game.W, (float) h / Game.H);
        offsetX = (w - Game.W * viewScale) / 2f;
        offsetY = (h - Game.H * viewScale) / 2f;
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
        paused = false;
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
            long now = System.nanoTime();
            float dt = (now - lastTimeNs) / 1_000_000_000f;
            lastTimeNs = now;
            if (dt > 0.05f) dt = 0.05f;

            if (!paused) {
                // Update Starfield
                for (Star s : stars) {
                    s.y += s.speed * dt;
                    if (s.y > Game.H) {
                        s.y = 0f;
                        s.x = (float) (Math.random() * Game.W);
                    }
                }

                // Update Particles
                for (Particle p : particles) {
                    if (!p.active) continue;
                    p.life -= dt;
                    if (p.life <= 0f) {
                        p.active = false;
                        continue;
                    }
                    p.x += p.vx * dt;
                    p.y += p.vy * dt;
                    if (p.type == 0 || p.type == 2) {
                        p.vx *= 0.94f;
                        p.vy *= 0.94f;
                    }
                }

                // Paddle thruster particles
                if ("wave".equals(game.phase) && !paused) {
                    spawnParticles(game.paddle.x - 22f, game.paddle.y + 14f, 1, 0xFF4FD1C5, 3);
                    spawnParticles(game.paddle.x + 22f, game.paddle.y + 14f, 1, 0xFF4FD1C5, 3);
                }

                // Forge flame embers when open
                if (game.forgeOpen && "wave".equals(game.phase)) {
                    for (Game.Building b : game.buildings) {
                        if ("forge".equals(b.type) && Math.random() < 0.28) {
                            spawnParticles(b.x + (float) Math.random() * b.w, b.y + 8f, 1, 0xFFFF8A00, 1);
                        }
                    }
                }

                // Ball squash recovery
                ballSquashX += (1f - ballSquashX) * 12f * dt;
                ballSquashY += (1f - ballSquashY) * 12f * dt;

                // Step simulation
                game.step(dt);

                // Detect game events for Audio & Visual Juice
                if (game.hp < lastHp) {
                    addTrauma(0.55f);
                    AudioHelper.playBreach();
                    lastHp = game.hp;
                } else if (game.hp > lastHp) {
                    lastHp = game.hp;
                }

                if (game.kills > lastKills) {
                    addTrauma(0.22f);
                    AudioHelper.playExplosion();
                    lastKills = game.kills;
                }

                int statsCount = game.stats.mined + game.stats.smelted + game.delivered;
                if (statsCount > lastStatsCount) {
                    if (game.stats.smelted > 0 && statsCount == lastStatsCount + 1 && game.ball.metal > 0) {
                        AudioHelper.playSmelt();
                        addTrauma(0.12f);
                    } else if (game.delivered > 0 && statsCount == lastStatsCount + 1 && game.ball.metal == 0) {
                        AudioHelper.playDeliver();
                        addTrauma(0.18f);
                    } else {
                        AudioHelper.playTone(480f, 0.08f);
                    }
                    lastStatsCount = statsCount;
                }

                // Shots fired
                if (!game.shots.isEmpty() && Math.random() < 0.3) {
                    AudioHelper.playLaser();
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
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = (event.getX() - offsetX) / viewScale;
        float touchY = (event.getY() - offsetY) / viewScale;
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
        if (btnPause.contains(tx, ty)) {
            togglePause();
            AudioHelper.playTone(440f);
            return;
        }

        if (paused) {
            if (btnResume.contains(tx, ty)) {
                paused = false;
                AudioHelper.playTone(480f);
            } else if (btnRestart.contains(tx, ty)) {
                resetGame();
                AudioHelper.playTone(440f);
            }
            return;
        }

        if ("upgrade".equals(game.phase)) {
            if (cardUpgrade1.contains(tx, ty)) {
                game.chooseUpgrade("cargo");
                AudioHelper.playTone(560f);
                addTrauma(0.15f);
            } else if (cardUpgrade2.contains(tx, ty)) {
                game.chooseUpgrade("paddle");
                AudioHelper.playTone(560f);
                addTrauma(0.15f);
            } else if (cardUpgrade3.contains(tx, ty)) {
                game.chooseUpgrade("damage");
                AudioHelper.playTone(560f);
                addTrauma(0.15f);
            }
            return;
        }

        if ("won".equals(game.phase) || "short".equals(game.phase) || "lost".equals(game.phase)) {
            if (btnRestart.contains(tx, ty)) {
                resetGame();
                AudioHelper.playTone(440f);
            }
            return;
        }

        if ("build".equals(game.phase)) {
            if (btnStartWave.contains(tx, ty)) {
                placingType = null;
                game.startWave();
                AudioHelper.playTone(520f);
                addTrauma(0.2f);
                return;
            }
            if (btnWall.contains(tx, ty)) {
                placingType = "wall".equals(placingType) ? null : "wall";
                AudioHelper.playTone(380f);
                return;
            }
            if (btnTurret.contains(tx, ty)) {
                placingType = "turret".equals(placingType) ? null : "turret";
                AudioHelper.playTone(380f);
                return;
            }

            if (placingType != null) {
                if (game.build(placingType, tx, ty)) {
                    AudioHelper.playTone(330f);
                    spawnParticles(tx, ty, 16, 0xFF4FD1C5, 0);
                    placingType = null;
                }
                return;
            }

            // Drag existing buildings
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
                AudioHelper.playTone(game.forgeOpen ? 460f : 320f);
                return;
            }
            if (btnBurn.contains(tx, ty)) {
                game.burn = !game.burn;
                AudioHelper.playTone(game.burn ? 560f : 360f);
                return;
            }
            if (btnRecall.contains(tx, ty)) {
                if (game.recall()) {
                    AudioHelper.playTone(400f);
                    addTrauma(0.25f);
                    spawnParticles(game.ball.x, game.ball.y, 25, 0xFFE6BA76, 0);
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
        if (paused) return;

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
        if ("build".equals(game.phase)) {
            draggingBuilding = null;
            return;
        }

        if ("wave".equals(game.phase)) {
            if (game.ball.held && ty < 840f) {
                updateAim(tx, ty);
                if (game.launch()) {
                    AudioHelper.playTone(520f);
                    ballSquashX = 0.7f;
                    ballSquashY = 1.35f;
                    spawnParticles(game.paddle.x, game.paddle.y, 18, 0xFF4FD1C5, 0);
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
    // RENDERING VERTICALE DIESELPUNK VOID-MINER
    // ==========================================

    private void drawGame(Canvas canvas, float dt) {
        // Deep Space Background Gradient
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0f, 0f, 0f, Game.H, 0xFF050812, 0xFF0B1424, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paintFill);
        paintFill.setShader(null);

        canvas.save();

        // Screen Shake calculation via decaying trauma (game-feel pattern)
        float shake = trauma * trauma;
        float shakeX = (float) (Math.sin(game.elapsed * 42f) * 12f * shake);
        float shakeY = (float) (Math.cos(game.elapsed * 38f) * 9f * shake);
        trauma = Math.max(0f, trauma - 1.4f * dt);

        canvas.translate(offsetX + shakeX * viewScale, offsetY + shakeY * viewScale);
        canvas.scale(viewScale, viewScale);

        // Draw Ambient Starfield & Space Dust
        for (Star s : stars) {
            paintFill.setColor(Color.argb(s.alpha, 160, 210, 255));
            canvas.drawCircle(s.x, s.y, s.size, paintFill);
        }

        // Draw Building Zone Grid Blueprint during Build Phase
        if ("build".equals(game.phase)) {
            drawBlueprintGrid(canvas);
        }

        // Sector Arena Perimeter & Hazard Bulkheads
        drawArenaPerimeter(canvas);

        // Core Defense Tripwire
        drawDefensePerimeter(canvas);

        // Mines (Jagged Crystalline Asteroids)
        for (int i = 0; i < game.mines.size(); i++) {
            drawMine(canvas, game.mines.get(i), i);
        }

        // Buildings (Smelter, Depot, Sponde, Turrets)
        for (Game.Building b : game.buildings) {
            drawBuilding(canvas, b);
        }

        // Turret Railgun Laser Beams
        for (Game.Shot shot : game.shots) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(3.5f);
            int alpha = (int) (255 * (shot.life / 0.2f));
            paintStroke.setColor(Color.argb(alpha, 100, 240, 255));
            canvas.drawLine(shot.x, shot.y, shot.tx, shot.ty, paintStroke);
            // Inner core beam
            paintStroke.setStrokeWidth(1.5f);
            paintStroke.setColor(Color.argb(alpha, 255, 255, 255));
            canvas.drawLine(shot.x, shot.y, shot.tx, shot.ty, paintStroke);
        }

        // Enemies (Scarab Drones & Void Harvesters)
        for (Game.Enemy e : game.enemies) {
            if (e.hp <= 0) continue;
            drawEnemy(canvas, e);
        }

        // Player Skiff (Paddle)
        drawPlayerPaddle(canvas);

        // Aiming trajectory when ball is held
        if (game.ball.held && "wave".equals(game.phase) && !paused) {
            drawAimLine(canvas);
        }

        // Ball Trail (Plasma / Fire Tail)
        drawBallTrail(canvas);

        // The Ball (Core Extractor Orb)
        drawBall(canvas);

        // Particles System (Sparks, Embers, Debris)
        drawParticles(canvas);

        // Floating Damage / Event Text
        drawEffects(canvas);

        // HUD Dashboard & Thumb Buttons
        drawHUD(canvas);

        // Overlays
        if (paused) {
            drawPauseOverlay(canvas);
        } else if ("upgrade".equals(game.phase)) {
            drawUpgradeOverlay(canvas);
        } else if ("won".equals(game.phase) || "short".equals(game.phase) || "lost".equals(game.phase)) {
            drawResultOverlay(canvas);
        }

        canvas.restore();
    }

    private void drawBlueprintGrid(Canvas canvas) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x184FD1C5);
        paintStroke.setStrokeWidth(1f);
        for (float x = 20f; x < Game.W; x += 32f) {
            canvas.drawLine(x, 220f, x, 730f, paintStroke);
        }
        for (float y = 220f; y < 730f; y += 32f) {
            canvas.drawLine(16f, y, Game.W - 16f, y, paintStroke);
        }

        // Construction border
        paintStroke.setColor(0x554FD1C5);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRect(16f, 220f, Game.W - 16f, 730f, paintStroke);

        paintText.setTextSize(11f);
        paintText.setColor(0xFF4FD1C5);
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("▶ SETTORE EDIFICABILE (Posiziona o riposiziona strutture)", 24f, 238f, paintText);
    }

    private void drawArenaPerimeter(Canvas canvas) {
        // Metallic upper border under HUD
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF1E293B);
        canvas.drawRect(0f, 65f, Game.W, 72f, paintFill);

        // Left wall with hazard chevron insets
        paintFill.setColor(0xFF162032);
        canvas.drawRect(0f, 65f, 14f, 825f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF334155);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(14f, 65f, 14f, 825f, paintStroke);

        // Right wall with hazard chevron insets
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF162032);
        canvas.drawRect(Game.W - 14f, 65f, Game.W, 825f, paintFill);
        canvas.drawLine(Game.W - 14f, 65f, Game.W - 14f, 825f, paintStroke);

        // Subtle neon boundary runners
        paintStroke.setColor(0x3338BDF8);
        paintStroke.setStrokeWidth(1f);
        canvas.drawLine(15f, 72f, 15f, 820f, paintStroke);
        canvas.drawLine(Game.W - 15f, 72f, Game.W - 15f, 820f, paintStroke);
    }

    private void drawDefensePerimeter(Canvas canvas) {
        // Pulsing glowing laser tripwire
        float pulse = (float) (Math.sin(game.elapsed * 6f) * 0.5 + 0.5);
        int alpha = 100 + (int) (120 * pulse);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2.5f);
        paintStroke.setColor(Color.argb(alpha, 239, 68, 68));
        canvas.drawLine(14f, 760f, Game.W - 14f, 760f, paintStroke);

        // Emitter nodes
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFDC2626);
        canvas.drawCircle(14f, 760f, 4f, paintFill);
        canvas.drawCircle(Game.W - 14f, 760f, 4f, paintFill);

        paintText.setTextSize(9f);
        paintText.setColor(0xCCEF4444);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PERIMETRO DIFESA NUCLEO", Game.W / 2f, 755f, paintText);
    }

    private void drawMine(Canvas canvas, Game.Mine m, int index) {
        boolean flash = (game.elapsed - game.hits.getOrDefault("mine" + index, -10f)) < 0.18f;

        // Draw Jagged Asteroid Body
        asteroidPath.reset();
        asteroidPath.moveTo(m.x + 8f, m.y);
        asteroidPath.lineTo(m.x + m.w - 12f, m.y + 4f);
        asteroidPath.lineTo(m.x + m.w, m.y + 14f);
        asteroidPath.lineTo(m.x + m.w - 6f, m.y + m.h - 6f);
        asteroidPath.lineTo(m.x + m.w - 24f, m.y + m.h);
        asteroidPath.lineTo(m.x + 12f, m.y + m.h - 2f);
        asteroidPath.lineTo(m.x, m.y + m.h - 14f);
        asteroidPath.lineTo(m.x + 4f, m.y + 12f);
        asteroidPath.close();

        // Shaded Asteroid Texture
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(flash ? 0xFFE2B770 : 0xFF283430);
        canvas.drawPath(asteroidPath, paintFill);

        // Metallic Rocky Outline
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setColor(flash ? 0xFFFFF1C2 : 0xFF475B54);
        canvas.drawPath(asteroidPath, paintStroke);

        // Glowing Crystalline Mineral Veins (Pulsing)
        float veinPulse = (float) (Math.sin(game.elapsed * 4f + index) * 0.3 + 0.7);
        int veinAlpha = (int) (220 * veinPulse);
        paintStroke.setColor(flash ? 0xFFFFFFFF : Color.argb(veinAlpha, 245, 180, 50));
        paintStroke.setStrokeWidth(2.5f);

        canvas.drawLine(m.x + 24f, m.y + 12f, m.x + 55f, m.y + 24f, paintStroke);
        canvas.drawLine(m.x + 55f, m.y + 24f, m.x + 78f, m.y + 16f, paintStroke);
        canvas.drawLine(m.x + 78f, m.y + 16f, m.x + 105f, m.y + 30f, paintStroke);

        // Mineral Crystals
        paintFill.setColor(Color.argb(veinAlpha, 255, 210, 80));
        canvas.drawCircle(m.x + 35f, m.y + 16f, 3f, paintFill);
        canvas.drawCircle(m.x + 70f, m.y + 22f, 3.5f, paintFill);
        canvas.drawCircle(m.x + 95f, m.y + 26f, 3f, paintFill);

        // Label
        paintText.setTextSize(10f);
        paintText.setColor(0xFFF1E6C8);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GIACIMENTO", m.x + m.w / 2f, m.y + m.h / 2f + 4f, paintText);
    }

    private void drawBuilding(Canvas canvas, Game.Building b) {
        tempRect.set(b.x, b.y, b.x + b.w, b.y + b.h);

        if ("wall".equals(b.type)) {
            // Reinforced Energy Barrier
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF1E2E2A);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2f);
            paintStroke.setColor(0xFF10B981);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

            // Center neon conduit
            paintStroke.setStrokeWidth(1.5f);
            paintStroke.setColor(0x8034D399);
            canvas.drawLine(b.x + 10f, b.y + b.h / 2f, b.x + b.w - 10f, b.y + b.h / 2f, paintStroke);

            paintText.setTextSize(10f);
            paintText.setColor(0xFF6EE7B7);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SPONDA DEFENSIVA", b.x + b.w / 2f, b.y + b.h / 2f + 3.5f, paintText);

        } else if ("forge".equals(b.type)) {
            // Blast Furnace Smelter
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF1F1D2B);
            canvas.drawRoundRect(tempRect, 8f, 8f, paintFill);

            // Molten metal vat interior
            float moltenTop = b.y + (game.forgeOpen ? 12f : 18f);
            tempRect.set(b.x + 8f, moltenTop, b.x + b.w - 8f, b.y + b.h - 8f);
            if (game.forgeOpen) {
                paintFill.setShader(new LinearGradient(b.x, b.y, b.x, b.y + b.h, 0xFFFF7A00, 0xFFFFD000, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(tempRect, 4f, 4f, paintFill);
                paintFill.setShader(null);
            } else {
                paintFill.setColor(0xFF4A3425);
                canvas.drawRoundRect(tempRect, 4f, 4f, paintFill);
            }

            // Chassis border
            tempRect.set(b.x, b.y, b.x + b.w, b.y + b.h);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2f);
            paintStroke.setColor(game.forgeOpen ? 0xFFFF9900 : 0xFF78716C);
            canvas.drawRoundRect(tempRect, 8f, 8f, paintStroke);

            // Corner rivets
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFFCBD5E1);
            canvas.drawCircle(b.x + 5f, b.y + 5f, 2f, paintFill);
            canvas.drawCircle(b.x + b.w - 5f, b.y + 5f, 2f, paintFill);
            canvas.drawCircle(b.x + 5f, b.y + b.h - 5f, 2f, paintFill);
            canvas.drawCircle(b.x + b.w - 5f, b.y + b.h - 5f, 2f, paintFill);

            paintText.setTextSize(11f);
            paintText.setTextAlign(Paint.Align.CENTER);
            if (game.forgeOpen) {
                paintText.setColor(0xFFFFFFFF);
                canvas.drawText("⚡ FONDERIA ATTIVA ➔ [FUSIONE]", b.x + b.w / 2f, b.y + b.h / 2f + 4f, paintText);
            } else {
                paintText.setColor(0xFFE2E8F0);
                canvas.drawText("TETTO CHIUSO (SPONDA SOLIDA)", b.x + b.w / 2f, b.y + b.h / 2f + 4f, paintText);
            }

        } else if ("depot".equals(b.type)) {
            // Orbital Cargo Dock
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF0F262E);
            canvas.drawRoundRect(tempRect, 8f, 8f, paintFill);

            // Cyan magnetic tractor field
            tempRect.set(b.x + 8f, b.y + 8f, b.x + b.w - 8f, b.y + b.h - 8f);
            paintFill.setColor(0xFF0E3A47);
            canvas.drawRoundRect(tempRect, 5f, 5f, paintFill);

            tempRect.set(b.x, b.y, b.x + b.w, b.y + b.h);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2f);
            paintStroke.setColor(0xFF06B6D4);
            canvas.drawRoundRect(tempRect, 8f, 8f, paintStroke);

            // Animated intake force rings
            float flow = (game.elapsed * 60f) % (b.w - 24f);
            paintStroke.setStrokeWidth(1.5f);
            paintStroke.setColor(0x8038BDF8);
            canvas.drawLine(b.x + 12f + flow, b.y + 12f, b.x + 12f + flow, b.y + b.h - 12f, paintStroke);

            paintText.setTextSize(11f);
            paintText.setColor(0xFF67E8F9);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("DEPOSITO CARGO (INCASSA FE)", b.x + b.w / 2f, b.y + b.h / 2f + 4f, paintText);

        } else if ("turret".equals(b.type)) {
            // Railgun Defense Turret
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF1E293B);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2f);
            paintStroke.setColor(0xFF38BDF8);
            canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

            float cx = b.x + b.w / 2f;
            float cy = b.y + b.h / 2f;

            // Turret rotating head aimed at nearest enemy
            Game.Enemy target = null;
            double minDist = 300.0;
            for (Game.Enemy e : game.enemies) {
                if (e.hp > 0) {
                    double d = Math.hypot(e.x - cx, e.y - cy);
                    if (d < minDist) {
                        minDist = d;
                        target = e;
                    }
                }
            }
            float angle = target != null ? (float) Math.atan2(target.y - cy, target.x - cx) : (float) (-Math.PI / 2.0);

            // Laser sight targeting line
            if (target != null) {
                paintStroke.setStrokeWidth(1f);
                paintStroke.setColor(0x4038BDF8);
                canvas.drawLine(cx, cy, target.x + target.w / 2f, target.y + target.h / 2f, paintStroke);
            }

            // Dual barrels
            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate((float) Math.toDegrees(angle));

            paintFill.setColor(0xFF64748B);
            canvas.drawRect(0f, -4f, 18f, -1.5f, paintFill);
            canvas.drawRect(0f, 1.5f, 18f, 4f, paintFill);

            // Turret Core Ball
            paintFill.setColor(0xFF38BDF8);
            canvas.drawCircle(0f, 0f, 7f, paintFill);
            paintFill.setColor(0xFFFFFFFF);
            canvas.drawCircle(0f, 0f, 3f, paintFill);

            canvas.restore();

            paintText.setTextSize(9f);
            paintText.setColor(0xFF94A3B8);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SENTINELLA", cx, b.y + b.h + 12f, paintText);
        }
    }

    private void drawEnemy(Canvas canvas, Game.Enemy e) {
        boolean thief = "thief".equals(e.type);

        if (thief) {
            // Void Harvester: Sleek Delta Stealth Interceptor
            enemyPath.reset();
            enemyPath.moveTo(e.x + e.w / 2f, e.y + e.h); // Nose facing down towards defense line
            enemyPath.lineTo(e.x + e.w, e.y + 4f);
            enemyPath.lineTo(e.x + e.w - 8f, e.y);
            enemyPath.lineTo(e.x + e.w / 2f, e.y + 8f);
            enemyPath.lineTo(e.x + 8f, e.y);
            enemyPath.lineTo(e.x, e.y + 4f);
            enemyPath.close();

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF2E1065); // Dark Purple
            canvas.drawPath(enemyPath, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(1.5f);
            paintStroke.setColor(0xFFA855F7); // Violet neon
            canvas.drawPath(enemyPath, paintStroke);

            // Violet ion thruster flare
            paintFill.setColor(0xFFC084FC);
            canvas.drawCircle(e.x + e.w / 2f, e.y + 3f, 3f, paintFill);

        } else {
            // Scarab Drone: Armored Heavy Beetle
            enemyPath.reset();
            enemyPath.moveTo(e.x + 6f, e.y);
            enemyPath.lineTo(e.x + e.w - 6f, e.y);
            enemyPath.lineTo(e.x + e.w, e.y + 10f);
            enemyPath.lineTo(e.x + e.w - 4f, e.y + e.h);
            enemyPath.lineTo(e.x + 4f, e.y + e.h);
            enemyPath.lineTo(e.x, e.y + 10f);
            enemyPath.close();

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF3F1D14); // Armored Rust Bronze
            canvas.drawPath(enemyPath, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(1.5f);
            paintStroke.setColor(0xFFF97316); // Molten Orange
            canvas.drawPath(enemyPath, paintStroke);

            // Mandibles facing down
            paintStroke.setColor(0xFFFDBA74);
            canvas.drawLine(e.x + 10f, e.y + e.h, e.x + 7f, e.y + e.h + 5f, paintStroke);
            canvas.drawLine(e.x + e.w - 10f, e.y + e.h, e.x + e.w - 7f, e.y + e.h + 5f, paintStroke);

            // Glowing red optical sensor visor
            paintFill.setColor(0xFFEF4444);
            canvas.drawRect(e.x + 12f, e.y + e.h - 7f, e.x + e.w - 12f, e.y + e.h - 4f, paintFill);
        }

        // Segmented Armor HP Cells above enemy
        float cellW = (e.w - 4f) / e.maxHp;
        for (int k = 0; k < e.maxHp; k++) {
            float cx = e.x + 2f + k * cellW;
            paintFill.setColor(k < e.hp ? (thief ? 0xFFC084FC : 0xFFF97316) : 0xFF334155);
            canvas.drawRect(cx, e.y - 7f, cx + cellW - 2f, e.y - 3f, paintFill);
        }
    }

    private void drawPlayerPaddle(Canvas canvas) {
        float px = game.paddle.x;
        float py = game.paddle.y;
        float pw = game.paddleWidth;

        // Heavy Alloy Hover-Skiff Chassis
        shipPath.reset();
        shipPath.moveTo(px - pw / 2f, py + 12f);
        shipPath.lineTo(px - pw / 2f + 10f, py);
        shipPath.lineTo(px + pw / 2f - 10f, py);
        shipPath.lineTo(px + pw / 2f, py + 12f);
        shipPath.lineTo(px + pw / 2f - 8f, py + 16f);
        shipPath.lineTo(px - pw / 2f + 8f, py + 16f);
        shipPath.close();

        // Hull Metallic Gradient
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(px, py, px, py + 16f, 0xFF334155, 0xFF0F172A, Shader.TileMode.CLAMP));
        canvas.drawPath(shipPath, paintFill);
        paintFill.setShader(null);

        // Alloy Border & Specular Ridge
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(1.8f);
        paintStroke.setColor(0xFF38BDF8); // Cyan energy trim
        canvas.drawPath(shipPath, paintStroke);

        // Center Magnetic Receptor Cup
        paintFill.setColor(0xFF0284C7);
        canvas.drawRect(px - 14f, py + 2f, px + 14f, py + 8f, paintFill);
        paintFill.setColor(0xFFBAE6FD);
        canvas.drawCircle(px, py + 5f, 3f, paintFill);

        // Hazard chevron stripes on wings
        paintStroke.setColor(0xFFF59E0B);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(px - pw / 2f + 12f, py + 3f, px - pw / 2f + 18f, py + 12f, paintStroke);
        canvas.drawLine(px + pw / 2f - 18f, py + 3f, px + pw / 2f - 12f, py + 12f, paintStroke);
    }

    private void drawAimLine(Canvas canvas) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setColor(0xCC38BDF8);

        float endX = game.ball.x + (float) Math.cos(game.aim) * 160f;
        float endY = game.ball.y + (float) Math.sin(game.aim) * 160f;
        canvas.drawLine(game.ball.x, game.ball.y, endX, endY, paintStroke);

        // Aim Reticle Dot
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF38BDF8);
        canvas.drawCircle(endX, endY, 4f, paintFill);

        paintText.setTextSize(12f);
        paintText.setColor(0xFFE2E8F0);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MIRA E RILASCIA PER LANCIARE LA SFERA", Game.W / 2f, game.paddle.y - 38f, paintText);
    }

    private void drawBallTrail(Canvas canvas) {
        int tSize = game.ball.trail.size();
        for (int i = tSize - 1; i >= 0; i--) {
            Game.PointF t = game.ball.trail.get(i);
            float ratio = 1f - (float) i / (tSize + 1);
            int alpha = (int) (130 * ratio);
            paintFill.setStyle(Paint.Style.FILL);
            if (game.burn) {
                paintFill.setColor(Color.argb(alpha, 255, 120, 20));
            } else {
                paintFill.setColor(Color.argb(alpha, 56, 189, 248));
            }
            canvas.drawCircle(t.x, t.y, game.ball.r * ratio * 0.95f, paintFill);
        }
    }

    private void drawBall(Canvas canvas) {
        canvas.save();
        canvas.translate(game.ball.x, game.ball.y);
        canvas.scale(ballSquashX, ballSquashY);

        // Outer Glow Corona
        paintGlow.setStyle(Paint.Style.FILL);
        if (game.burn) {
            paintGlow.setShader(new RadialGradient(0f, 0f, game.ball.r * 1.8f,
                    0xFFFFAA00, 0x00FF4400, Shader.TileMode.CLAMP));
        } else {
            paintGlow.setShader(new RadialGradient(0f, 0f, game.ball.r * 1.8f,
                    0xFF38BDF8, 0x000284C7, Shader.TileMode.CLAMP));
        }
        canvas.drawCircle(0f, 0f, game.ball.r * 1.8f, paintGlow);
        paintGlow.setShader(null);

        // Core Fusion Sphere
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(game.burn ? 0xFFFFD700 : 0xFFFFFFFF);
        canvas.drawCircle(0f, 0f, game.ball.r, paintFill);

        // Inner Specular Glint
        paintFill.setColor(0xFFFFFFFF);
        canvas.drawCircle(-game.ball.r * 0.3f, -game.ball.r * 0.3f, game.ball.r * 0.35f, paintFill);

        canvas.restore();

        // 3D Revolving Cargo Capsules Orbiting The Ball
        if (game.cargo() > 0) {
            int total = game.cargo();
            for (int i = 0; i < total; i++) {
                double a = i * 2.0 * Math.PI / total + game.elapsed * 2.2;
                float px = game.ball.x + (float) Math.cos(a) * 18f;
                float py = game.ball.y + (float) Math.sin(a) * 11f; // Elliptical 3D tilt

                paintFill.setStyle(Paint.Style.FILL);
                if (i < game.ball.metal) {
                    // Refined Iron Ingot: Gleaming Golden Bar
                    paintFill.setColor(0xFFFBBF24);
                    canvas.drawRect(px - 4f, py - 3f, px + 4f, py + 3f, paintFill);
                    paintStroke.setStyle(Paint.Style.STROKE);
                    paintStroke.setStrokeWidth(1f);
                    paintStroke.setColor(0xFFFFFBEB);
                    canvas.drawRect(px - 4f, py - 3f, px + 4f, py + 3f, paintStroke);
                } else {
                    // Raw Ore Chunk: Bronze Rock Crystal
                    paintFill.setColor(0xFFB45309);
                    canvas.drawCircle(px, py, 3.5f, paintFill);
                    paintStroke.setStyle(Paint.Style.STROKE);
                    paintStroke.setStrokeWidth(1f);
                    paintStroke.setColor(0xFFFCD34D);
                    canvas.drawCircle(px, py, 3.5f, paintStroke);
                }
            }

            // Cargo Readout
            paintText.setTextSize(11f);
            paintText.setColor(0xFFFDE68A);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(game.ball.ore + " Min · " + game.ball.metal + " Fe", game.ball.x, game.ball.y - 20f, paintText);
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
            // Shadow
            paintText.setColor(Color.argb(alpha / 2, 0, 0, 0));
            paintText.setTextSize(14f);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(eff.text, eff.x + 1.5f, eff.y + 1.5f, paintText);
            // Main Text
            paintText.setColor((eff.color & 0x00FFFFFF) | (alpha << 24));
            canvas.drawText(eff.text, eff.x, eff.y, paintText);
        }
    }

    private void drawHUD(Canvas canvas) {
        // Top Aerospace Visor Dashboard
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0f, 0f, 0f, 65f, 0xFA0B1324, 0xEE111C33, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, Game.W, 65f, paintFill);
        paintFill.setShader(null);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF1E293B);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(0f, 65f, Game.W, 65f, paintStroke);

        // Wave Badge
        tempRect.set(14f, 12f, 115f, 54f);
        paintFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintFill);
        paintStroke.setColor(0xFF475569);
        canvas.drawRoundRect(tempRect, 6f, 6f, paintStroke);

        paintText.setTextSize(11f);
        paintText.setColor(0xFF94A3B8);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MISSIONE", tempRect.centerX(), 28f, paintText);
        paintText.setTextSize(14f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("Ondata " + game.wave + "/5", tempRect.centerX(), 46f, paintText);

        // Shield Energy Cells
        float startX = 130f;
        for (int i = 0; i < game.maxHp; i++) {
            float cx = startX + i * 16f;
            boolean active = i < game.hp;
            paintFill.setColor(active ? (game.hp <= 2 ? 0xFFEF4444 : 0xFF10B981) : 0xFF334155);
            canvas.drawRoundRect(cx, 32f, cx + 12f, 52f, 2f, 2f, paintFill);
            if (active) {
                paintStroke.setColor(0xFFD1FAE5);
                paintStroke.setStrokeWidth(1f);
                canvas.drawRoundRect(cx, 32f, cx + 12f, 52f, 2f, 2f, paintStroke);
            }
        }
        paintText.setTextSize(10f);
        paintText.setColor(0xFF6EE7B7);
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("INTEGRITÀ SCUDI", startX, 26f, paintText);

        // Resources Counters
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(13f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("Fe Disp: " + game.iron, 245f, 28f, paintText);
        paintText.setColor(0xFF38BDF8);
        canvas.drawText("Consegnati: " + game.delivered + "/" + game.target, 245f, 48f, paintText);

        // Digital Countdown Timer
        int sec = (int) Math.max(0, game.duration - game.time);
        paintText.setTextSize(17f);
        paintText.setColor(sec <= 5 ? 0xFFEF4444 : 0xFFE2E8F0);
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(sec + "s", 460f, 38f, paintText);

        // Pause Button
        paintFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(btnPause, 6f, 6f, paintFill);
        paintStroke.setColor(0xFF475569);
        canvas.drawRoundRect(btnPause, 6f, 6f, paintStroke);
        paintText.setColor(0xFFF8FAFC);
        paintText.setTextSize(14f);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("❚❚", btnPause.centerX(), btnPause.centerY() + 5f, paintText);

        // ==========================================
        // BARRA INFERIORE COMANDI AD ALTA ERGONOMIA
        // ==========================================
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0f, 835f, 0f, 960f, 0xEE0B1324, 0xFF050812, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 835f, Game.W, 960f, paintFill);
        paintFill.setShader(null);

        paintStroke.setColor(0xFF1E293B);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(0f, 835f, Game.W, 835f, paintStroke);

        if ("wave".equals(game.phase)) {
            // Button Fonderia
            drawTactileButton(canvas, btnForge,
                    game.forgeOpen ? "FONDERIA: ON" : "FONDERIA: OFF",
                    game.forgeOpen ? "Trasforma Minerale" : "Muro Riflettente",
                    game.forgeOpen ? 0xFFEA580C : 0xFF334155,
                    game.forgeOpen ? 0xFFFFD7A0 : 0xFF94A3B8);

            // Button Carico
            drawTactileButton(canvas, btnBurn,
                    game.burn ? "CARICO: ARMATO" : "CARICO: SALVO",
                    game.burn ? "Danno x3 (Consuma)" : "Per il Deposito",
                    game.burn ? 0xFFDC2626 : 0xFF0D9488,
                    game.burn ? 0xFFFECACA : 0xFFCCFBF1);

            // Button Richiama Sfera
            boolean ready = game.recall <= 0f && !game.ball.held;
            String recallLbl = game.recall > 0f ? "RICHIAMO (" + (int) Math.ceil(game.recall) + "s)" : "RICHIAMA SFERA";
            drawTactileButton(canvas, btnRecall,
                    recallLbl,
                    "Salva & Centra Sfera",
                    ready ? 0xFF0284C7 : 0xFF1E293B,
                    ready ? 0xFFE0F2FE : 0xFF64748B);

        } else if ("build".equals(game.phase)) {
            // Button +Sponda
            boolean selWall = "wall".equals(placingType);
            boolean canWall = game.iron >= Game.COST_WALL;
            drawTactileButton(canvas, btnWall,
                    "+ SPONDA (3 Fe)",
                    selWall ? "▶ POSIZIONA..." : "Muro difensivo",
                    selWall ? 0xFF10B981 : (canWall ? 0xFF065F46 : 0xFF1E293B),
                    canWall ? 0xFFD1FAE5 : 0xFF64748B);

            // Button +Sentinella
            boolean selTurret = "turret".equals(placingType);
            boolean canTurret = game.iron >= Game.COST_TURRET;
            drawTactileButton(canvas, btnTurret,
                    "+ SENTINELLA (7 Fe)",
                    selTurret ? "▶ POSIZIONA..." : "Torretta laser auto",
                    selTurret ? 0xFF38BDF8 : (canTurret ? 0xFF075985 : 0xFF1E293B),
                    canTurret ? 0xFFE0F2FE : 0xFF64748B);

            // Button Avvia Ondata
            drawTactileButton(canvas, btnStartWave,
                    "AVVIA ONDATA ➔",
                    "Difendi il settore",
                    0xFFD97706,
                    0xFFFFFBEB);
        }
    }

    private void drawTactileButton(Canvas canvas, RectF r, String title, String sub, int bgClr, int textClr) {
        // Button Body
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(bgClr);
        canvas.drawRoundRect(r, 8f, 8f, paintFill);

        // Highlight top rim
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(1.5f);
        paintStroke.setColor(0x40FFFFFF);
        canvas.drawLine(r.left + 8f, r.top + 1f, r.right - 8f, r.top + 1f, paintStroke);

        // Border
        paintStroke.setColor(textClr);
        canvas.drawRoundRect(r, 8f, 8f, paintStroke);

        // Text
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(13f);
        paintText.setColor(textClr);
        canvas.drawText(title, r.centerX(), r.centerY() - 2f, paintText);

        paintText.setTextSize(10f);
        paintText.setColor(Color.argb(190, Color.red(textClr), Color.green(textClr), Color.blue(textClr)));
        canvas.drawText(sub, r.centerX(), r.centerY() + 15f, paintText);
    }

    private void drawUpgradeOverlay(Canvas canvas) {
        paintFill.setColor(0xEE050B14);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        paintText.setTextSize(24f);
        paintText.setColor(0xFFFBBF24);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ONDATA COMPLETATA", Game.W / 2f, 130f, paintText);

        paintText.setTextSize(14f);
        paintText.setColor(0xFF94A3B8);
        canvas.drawText("Scegli un modulo tecnologico per la spedizione:", Game.W / 2f, 165f, paintText);

        // Card 1
        drawUpgradeCard(canvas, cardUpgrade1, "📦 STIVA MAGNETICA RINFORZATA",
                "+2 Capienza Totale Carico",
                "Permette di raccogliere e fondere più minerale prima del deposito.",
                0xFFF59E0B);

        // Card 2
        drawUpgradeCard(canvas, cardUpgrade2, "🛡️ SCAFO RACCHETTA MAGGIORATO",
                "+20px Lunghezza & +1 Integrità Scudo",
                "Aumenta la copertura della linea difensiva e ripara i danni subiti.",
                0xFF10B981);

        // Card 3
        drawUpgradeCard(canvas, cardUpgrade3, "⚡ NUCLEO CINETICO AVANZATO",
                "+1 Danno Base Sfera",
                "Ogni impatto contro i droni nemici infligge danni distruttivi maggiori.",
                0xFF38BDF8);
    }

    private void drawUpgradeCard(Canvas canvas, RectF r, String title, String bonus, String desc, int accentClr) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(r, 10f, 10f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setColor(accentClr);
        canvas.drawRoundRect(r, 10f, 10f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(16f);
        paintText.setColor(accentClr);
        canvas.drawText(title, r.centerX(), r.top + 45f, paintText);

        paintText.setTextSize(14f);
        paintText.setColor(0xFFF8FAFC);
        canvas.drawText(bonus, r.centerX(), r.top + 80f, paintText);

        paintText.setTextSize(12f);
        paintText.setColor(0xFF94A3B8);
        canvas.drawText(desc, r.centerX(), r.top + 115f, paintText);
    }

    private void drawResultOverlay(Canvas canvas) {
        paintFill.setColor(0xF0050812);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        boolean won = "won".equals(game.phase);
        boolean shortIron = "short".equals(game.phase);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(32f);
        paintText.setColor(won ? 0xFF10B981 : shortIron ? 0xFFF59E0B : 0xFFEF4444);
        canvas.drawText(won ? "SETTORE DIFESO!" : shortIron ? "CARGO INSUFFICIENTE" : "NUCLEO DISTRUTTO", Game.W / 2f, 250f, paintText);

        paintText.setTextSize(15f);
        paintText.setColor(0xFFE2E8F0);
        String desc = won ? "Hai difeso il settore e consegnato " + game.delivered + "/" + game.target + " Fe!"
                : shortIron ? "Sopravvissuto alle 5 ondate, ma servivano " + game.target + " Fe (consegnati: " + game.delivered + ")."
                : "I droni nemici hanno abbattuto tutte le difese del settore.";
        canvas.drawText(desc, Game.W / 2f, 295f, paintText);

        // Mission Stats Panel
        tempRect.set(60f, 340f, Game.W - 60f, 520f);
        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(tempRect, 8f, 8f, paintFill);
        paintStroke.setColor(0xFF334155);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(tempRect, 8f, 8f, paintStroke);

        paintText.setTextSize(14f);
        paintText.setColor(0xFF38BDF8);
        canvas.drawText("RAPPORTO MISSIONE SPEDIZIONE", Game.W / 2f, 375f, paintText);

        paintText.setColor(0xFFCBD5E1);
        canvas.drawText("Minerale grezzo estratto: " + game.stats.mined, Game.W / 2f, 410f, paintText);
        canvas.drawText("Ferro raffinato nella fonderia: " + game.stats.smelted, Game.W / 2f, 435f, paintText);
        canvas.drawText("Droni ostili abbattuti: " + game.kills, Game.W / 2f, 460f, paintText);
        canvas.drawText("Strutture difensive edificate: " + game.stats.built, Game.W / 2f, 485f, paintText);

        // Restart Button
        paintFill.setColor(0xFF0284C7);
        canvas.drawRoundRect(btnRestart, 10f, 10f, paintFill);
        paintStroke.setColor(0xFF38BDF8);
        canvas.drawRoundRect(btnRestart, 10f, 10f, paintStroke);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("NUOVA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }

    private void drawPauseOverlay(Canvas canvas) {
        paintFill.setColor(0xD0050812);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(28f);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText("SIMULAZIONE IN PAUSA", Game.W / 2f, 380f, paintText);

        paintFill.setColor(0xFF0284C7);
        canvas.drawRoundRect(btnResume, 8f, 8f, paintFill);
        paintStroke.setColor(0xFF38BDF8);
        canvas.drawRoundRect(btnResume, 8f, 8f, paintStroke);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("RIPRENDI", btnResume.centerX(), btnResume.centerY() + 6f, paintText);

        paintFill.setColor(0xFFDC2626);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintStroke.setColor(0xFFF87171);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintStroke);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("RICOMINCIA", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }
}
