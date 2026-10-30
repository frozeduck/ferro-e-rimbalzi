package eu.sabotino.ferrorimbalzi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.List;

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

    // Paints
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tempRect = new RectF();
    private final Path tempPath = new Path();

    // Button touch regions in virtual coordinates
    private final RectF btnForge = new RectF(20f, 630f, 180f, 705f);
    private final RectF btnBurn = new RectF(190f, 630f, 350f, 705f);
    private final RectF btnRecall = new RectF(360f, 630f, 520f, 705f);
    private final RectF btnPause = new RectF(900f, 8f, 950f, 48f);

    // Build mode button regions
    private final RectF btnWall = new RectF(540f, 630f, 670f, 705f);
    private final RectF btnTurret = new RectF(680f, 630f, 810f, 705f);
    private final RectF btnStartWave = new RectF(820f, 630f, 945f, 705f);

    // Upgrade cards
    private final RectF cardUpgrade1 = new RectF(140f, 260f, 360f, 500f);
    private final RectF cardUpgrade2 = new RectF(380f, 260f, 580f, 500f);
    private final RectF cardUpgrade3 = new RectF(600f, 260f, 820f, 500f);

    // End/Pause screen buttons
    private final RectF btnRestart = new RectF(380f, 520f, 580f, 580f);
    private final RectF btnResume = new RectF(380f, 440f, 580f, 500f);

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        game = new Game();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        game = new Game();
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
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTimeNs) / 1_000_000_000f;
            lastTimeNs = now;
            if (dt > 0.05f) dt = 0.05f;

            if (!paused) {
                game.step(dt);

                int statsCount = game.stats.mined + game.stats.smelted + game.delivered + game.kills;
                if (statsCount > lastStatsCount) {
                    lastStatsCount = statsCount;
                    AudioHelper.playTone(game.ball.metal > 0 ? 660f : 360f);
                }
            }

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        drawGame(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            // Target roughly ~60 FPS
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
        // Pause button check
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
                AudioHelper.playTone(520f);
            } else if (cardUpgrade2.contains(tx, ty)) {
                game.chooseUpgrade("paddle");
                AudioHelper.playTone(520f);
            } else if (cardUpgrade3.contains(tx, ty)) {
                game.chooseUpgrade("damage");
                AudioHelper.playTone(520f);
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
                    placingType = null;
                }
                return;
            }

            // Check if dragging an existing building
            for (int i = game.buildings.size() - 1; i >= 0; i--) {
                Game.Building b = game.buildings.get(i);
                if (tx >= b.x - 10f && tx <= b.x + b.w + 10f && ty >= b.y - 10f && ty <= b.y + b.h + 10f) {
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
            if (game.ball.held && ty < 620f) {
                updateAim(tx, ty);
                if (game.launch()) {
                    AudioHelper.playTone(520f);
                }
            }
        }
    }

    private void updateAim(float tx, float ty) {
        float angle = (float) Math.atan2(ty - (game.paddle.y - 22f), tx - game.paddle.x);
        game.aim = Game.clamp(angle, (float) -Math.PI + 0.18f, -0.18f);
    }

    private void updatePaddle(float tx) {
        game.paddle.x = Game.clamp(tx, game.paddleWidth / 2f + 17f, Game.W - game.paddleWidth / 2f - 17f);
    }

    // ==========================================
    // RENDERING
    // ==========================================

    private void drawGame(Canvas canvas) {
        canvas.drawColor(0xFF080D16);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(viewScale, viewScale);

        // Arena backdrop glow
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0C1322);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        // Grid in build phase
        if ("build".equals(game.phase)) {
            paintFill.setColor(0x29B3BDC6);
            for (float x = 32f; x < Game.W; x += 32f) {
                for (float y = 48f; y < Game.H; y += 32f) {
                    canvas.drawRect(x, y, x + 2f, y + 2f, paintFill);
                }
            }
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0x5A8194A4);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(24f, 240f, 936f, 590f, paintStroke);

            paintText.setTextSize(14f);
            paintText.setColor(0xFF839699);
            paintText.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("ZONA EDIFICABILE", 36f, 580f, paintText);
        }

        // Limit lines and boundaries
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x70B4815A);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(20f, 614f, 940f, 614f, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF3D4855);
        canvas.drawRect(12f, 28f, 948f, 36f, paintFill);
        canvas.drawRect(12f, 28f, 20f, 647f, paintFill);
        canvas.drawRect(940f, 28f, 948f, 647f, paintFill);

        // Mines (giacimenti)
        for (int i = 0; i < game.mines.size(); i++) {
            Game.Mine m = game.mines.get(i);
            boolean flash = (game.elapsed - game.hits.getOrDefault("mine" + i, -10f)) < 0.16f;
            paintFill.setColor(flash ? 0xFFC39D6B : 0xFF4B5040);
            canvas.drawRect(m.x, m.y, m.x + m.w, m.y + m.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFA88C5C);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(m.x, m.y, m.x + m.w, m.y + m.h, paintStroke);

            paintText.setTextSize(12f);
            paintText.setColor(0xFFAC9670);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GIACIMENTO", m.x + m.w / 2f, m.y + m.h + 16f, paintText);
        }

        // Buildings (forge, depot, wall, turret)
        for (Game.Building b : game.buildings) {
            drawBuilding(canvas, b);
        }

        // Turret shots
        for (Game.Shot shot : game.shots) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2f);
            int alpha = (int) (255 * (shot.life / 0.2f));
            paintStroke.setColor(Color.argb(alpha, 223, 203, 134));
            canvas.drawLine(shot.x, shot.y, shot.tx, shot.ty, paintStroke);
        }

        // Enemies
        for (Game.Enemy e : game.enemies) {
            if (e.hp <= 0) continue;
            boolean thief = "thief".equals(e.type);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(thief ? 0xFF456258 : 0xFF5C493B);
            canvas.drawRect(e.x, e.y, e.x + e.w, e.y + e.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(thief ? 0xFFA6B9A0 : 0xFFD29C83);
            paintStroke.setStrokeWidth(1.5f);
            canvas.drawRect(e.x, e.y, e.x + e.w, e.y + e.h, paintStroke);

            // HP pips
            for (int k = 0; k < e.maxHp; k++) {
                paintFill.setColor(k < e.hp ? (thief ? 0xFFA6B9A0 : 0xFFD29C83) : 0xFF415247);
                canvas.drawRect(e.x + 4f + k * 10f, e.y + e.h + 4f, e.x + 11f + k * 10f, e.y + e.h + 7f, paintFill);
            }
        }

        // Paddle
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFB6C8A8);
        canvas.drawRect(game.paddle.x - game.paddleWidth / 2f, game.paddle.y,
                game.paddle.x + game.paddleWidth / 2f, game.paddle.y + 12f, paintFill);
        paintFill.setColor(0xFFE8C37F);
        canvas.drawRect(game.paddle.x - 14f, game.paddle.y + 3f, game.paddle.x + 14f, game.paddle.y + 9f, paintFill);

        // Aim line
        if (game.ball.held && "wave".equals(game.phase) && !paused) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2f);
            paintStroke.setColor(0x99E8D8A9);
            float endX = game.ball.x + (float) Math.cos(game.aim) * 160f;
            float endY = game.ball.y + (float) Math.sin(game.aim) * 160f;
            canvas.drawLine(game.ball.x, game.ball.y, endX, endY, paintStroke);

            paintText.setTextSize(13f);
            paintText.setColor(0xFFC8C5A0);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("TOCCA / RILASCIA PER LANCIARE", game.paddle.x, game.paddle.y - 40f, paintText);
        }

        // Ball Trail
        for (int i = game.ball.trail.size() - 1; i >= 0; i--) {
            Game.PointF t = game.ball.trail.get(i);
            float ratio = 1f - (float) i / (game.ball.trail.size() + 1);
            int alpha = (int) (70 * ratio);
            paintFill.setColor(game.burn ? Color.argb(alpha, 233, 157, 102) : Color.argb(alpha, 209, 215, 160));
            canvas.drawCircle(t.x, t.y, game.ball.r * ratio, paintFill);
        }

        // Ball
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(game.burn ? 0xFFEABD7B : 0xFFE5E8C4);
        canvas.drawCircle(game.ball.x, game.ball.y, game.ball.r, paintFill);

        // Ball Cargo payload indicators
        if (game.cargo() > 0) {
            int total = game.cargo();
            for (int i = 0; i < total; i++) {
                double a = i * 2.0 * Math.PI / total + game.elapsed;
                float px = game.ball.x + (float) Math.cos(a) * 16f;
                float py = game.ball.y + (float) Math.sin(a) * 16f;
                paintFill.setColor(i < game.ball.metal ? 0xFFEFC46E : 0xFFA87D60);
                canvas.drawCircle(px, py, 3f, paintFill);
            }
            paintText.setTextSize(12f);
            paintText.setColor(0xFFD9C595);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(game.ball.ore + " Min · " + game.ball.metal + " Fe", game.ball.x, game.ball.y - 20f, paintText);
        }

        // Floating texts / effects
        for (Game.Effect eff : game.effects) {
            int alpha = (int) (255 * Game.clamp(eff.life, 0f, 1f));
            paintText.setColor((eff.color & 0x00FFFFFF) | (alpha << 24));
            paintText.setTextSize(14f);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(eff.text, eff.x, eff.y, paintText);
        }

        // HUD and Buttons
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

    private void drawBuilding(Canvas canvas, Game.Building b) {
        paintFill.setStyle(Paint.Style.FILL);
        if ("wall".equals(b.type)) {
            paintFill.setColor(0xFF415D50);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFF8AA18A);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintStroke);
            paintText.setTextSize(11f);
            paintText.setColor(0xFF7D9786);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SPONDA", b.x + b.w / 2f, b.y + b.h + 15f, paintText);
        } else if ("forge".equals(b.type)) {
            paintFill.setColor(game.forgeOpen ? 0xFF39382B : 0xFF624B35);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFE9AE64);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintStroke);

            paintText.setTextSize(12f);
            paintText.setColor(0xFFE9AE64);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(game.forgeOpen ? "FONDERIA APERTA →" : "TETTO CHIUSO (SPONDA)", b.x + b.w / 2f, b.y + b.h + 16f, paintText);
        } else if ("depot".equals(b.type)) {
            paintFill.setColor(0xFF244637);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFF9FBEC1);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintStroke);

            paintText.setTextSize(12f);
            paintText.setColor(0xFF9FBEC1);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("DEPOSITO (INCASSA)", b.x + b.w / 2f, b.y + b.h + 16f, paintText);
        } else if ("turret".equals(b.type)) {
            paintFill.setColor(0xFF314B35);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFB9C594);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintStroke);

            paintFill.setColor(0xFFB6C397);
            canvas.drawCircle(b.x + 27f, b.y + 27f, 10f, paintFill);

            paintText.setTextSize(11f);
            paintText.setColor(0xFFB9C594);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SENTINELLA", b.x + b.w / 2f, b.y + b.h + 15f, paintText);
        }
    }

    private void drawHUD(Canvas canvas) {
        // Top HUD bar
        paintFill.setColor(0xAA101A27);
        canvas.drawRect(20f, 5f, 940f, 50f, paintFill);

        paintText.setTextSize(16f);
        paintText.setColor(0xFFE6BA76);
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Ondata " + (game.wave) + "/5", 30f, 32f, paintText);

        // HP hearts
        paintText.setColor(0xFFEF9A8E);
        StringBuilder hpStr = new StringBuilder();
        for (int i = 0; i < game.hp; i++) hpStr.append("♥ ");
        canvas.drawText("Cuore: " + hpStr.toString(), 160f, 32f, paintText);

        // Iron & Target
        paintText.setColor(0xFFEFC46E);
        canvas.drawText("Ferro: " + game.iron + "  |  Consegnati: " + game.delivered + "/" + game.target, 380f, 32f, paintText);

        // Timer
        int seconds = (int) Math.max(0, game.duration - game.time);
        paintText.setColor(0xFFC0CAD0);
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Tempo: " + seconds + "s", 880f, 32f, paintText);

        // Pause Button
        paintFill.setColor(0xFF2B3A4C);
        canvas.drawRoundRect(btnPause, 6f, 6f, paintFill);
        paintText.setColor(0xFFFFFFFF);
        paintText.setTextSize(14f);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("❚❚", btnPause.centerX(), btnPause.centerY() + 5f, paintText);

        // Bottom Controls
        if ("wave".equals(game.phase)) {
            // Forge Button
            paintFill.setColor(game.forgeOpen ? 0xFF584A2C : 0xFF2B3139);
            canvas.drawRoundRect(btnForge, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.forgeOpen ? 0xFFE9AE64 : 0xFF8E959E);
            canvas.drawText(game.forgeOpen ? "Fonderia: APERTA" : "Fonderia: CHIUSA", btnForge.centerX(), btnForge.centerY() - 4f, paintText);
            paintText.setTextSize(10f);
            canvas.drawText(game.forgeOpen ? "Trasforma minerale" : "Riflette la sfera", btnForge.centerX(), btnForge.centerY() + 14f, paintText);

            // Burn Button
            paintFill.setColor(game.burn ? 0xFF5A2A20 : 0xFF243B33);
            canvas.drawRoundRect(btnBurn, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.burn ? 0xFFFF9A8E : 0xFF92D4BD);
            canvas.drawText(game.burn ? "Carico: ARMATO" : "Carico: CONSERVA", btnBurn.centerX(), btnBurn.centerY() - 4f, paintText);
            paintText.setTextSize(10f);
            canvas.drawText(game.burn ? "+Danni (consuma)" : "Per consegna deposito", btnBurn.centerX(), btnBurn.centerY() + 14f, paintText);

            // Recall Button
            boolean recallReady = game.recall <= 0f && !game.ball.held;
            paintFill.setColor(recallReady ? 0xFF35485E : 0xFF1C2530);
            canvas.drawRoundRect(btnRecall, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(recallReady ? 0xFFE2E7CC : 0xFF657280);
            String recallText = game.recall > 0f ? "Richiamo (" + (int) Math.ceil(game.recall) + "s)" : "RICHIAMA SFERA";
            canvas.drawText(recallText, btnRecall.centerX(), btnRecall.centerY() + 5f, paintText);
        } else if ("build".equals(game.phase)) {
            // Wall Button
            boolean wallSelected = "wall".equals(placingType);
            paintFill.setColor(wallSelected ? 0xFF557766 : (game.iron >= Game.COST_WALL ? 0xFF2A4036 : 0xFF1C2521));
            canvas.drawRoundRect(btnWall, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.iron >= Game.COST_WALL ? 0xFFB6C8A8 : 0xFF606D66);
            canvas.drawText("Sponda (3 Fe)", btnWall.centerX(), btnWall.centerY() + 5f, paintText);

            // Turret Button
            boolean turretSelected = "turret".equals(placingType);
            paintFill.setColor(turretSelected ? 0xFF6A7744 : (game.iron >= Game.COST_TURRET ? 0xFF3A4426 : 0xFF1C2216));
            canvas.drawRoundRect(btnTurret, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.iron >= Game.COST_TURRET ? 0xFFE0D4A5 : 0xFF656950);
            canvas.drawText("Sentinella (7 Fe)", btnTurret.centerX(), btnTurret.centerY() + 5f, paintText);

            // Start Wave Button
            paintFill.setColor(0xFF9E6534);
            canvas.drawRoundRect(btnStartWave, 8f, 8f, paintFill);
            paintText.setTextSize(14f);
            paintText.setColor(0xFFFFFFFF);
            canvas.drawText("AVVIA ONDATA ➔", btnStartWave.centerX(), btnStartWave.centerY() + 5f, paintText);
        }
    }

    private void drawUpgradeOverlay(Canvas canvas) {
        paintFill.setColor(0xDD0A111D);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        paintText.setTextSize(26f);
        paintText.setColor(0xFFE6BA76);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SCEGLI UN TALENTO", 480f, 180f, paintText);

        paintText.setTextSize(14f);
        paintText.setColor(0xFF9AB1A0);
        canvas.drawText("Migliora la tua attrezzatura per la prossima ondata", 480f, 215f, paintText);

        // Card 1: Stiva
        paintFill.setColor(0xFF1B2936);
        canvas.drawRoundRect(cardUpgrade1, 12f, 12f, paintFill);
        paintStroke.setColor(0xFF4C6A85);
        canvas.drawRoundRect(cardUpgrade1, 12f, 12f, paintStroke);
        paintText.setColor(0xFFEFC46E);
        paintText.setTextSize(18f);
        canvas.drawText("STIVA ESPANSA", cardUpgrade1.centerX(), cardUpgrade1.top + 60f, paintText);
        paintText.setColor(0xFFD0D7DE);
        paintText.setTextSize(13f);
        canvas.drawText("+2 Capienza Carico", cardUpgrade1.centerX(), cardUpgrade1.top + 110f, paintText);
        canvas.drawText("Trasporta più minerale", cardUpgrade1.centerX(), cardUpgrade1.top + 140f, paintText);

        // Card 2: Racchetta
        paintFill.setColor(0xFF1B2936);
        canvas.drawRoundRect(cardUpgrade2, 12f, 12f, paintFill);
        paintStroke.setColor(0xFF4C6A85);
        canvas.drawRoundRect(cardUpgrade2, 12f, 12f, paintStroke);
        paintText.setColor(0xFF92D4BD);
        paintText.setTextSize(18f);
        canvas.drawText("RINFORZO BARRA", cardUpgrade2.centerX(), cardUpgrade2.top + 60f, paintText);
        paintText.setColor(0xFFD0D7DE);
        paintText.setTextSize(13f);
        canvas.drawText("+24px Lunghezza", cardUpgrade2.centerX(), cardUpgrade2.top + 110f, paintText);
        canvas.drawText("+1 Integrità Cuore", cardUpgrade2.centerX(), cardUpgrade2.top + 140f, paintText);

        // Card 3: Danno
        paintFill.setColor(0xFF1B2936);
        canvas.drawRoundRect(cardUpgrade3, 12f, 12f, paintFill);
        paintStroke.setColor(0xFF4C6A85);
        canvas.drawRoundRect(cardUpgrade3, 12f, 12f, paintStroke);
        paintText.setColor(0xFFEF9A8E);
        paintText.setTextSize(18f);
        canvas.drawText("IMPATTO PESANTE", cardUpgrade3.centerX(), cardUpgrade3.top + 60f, paintText);
        paintText.setColor(0xFFD0D7DE);
        paintText.setTextSize(13f);
        canvas.drawText("+1 Danno Base", cardUpgrade3.centerX(), cardUpgrade3.top + 110f, paintText);
        canvas.drawText("Sfera più letale", cardUpgrade3.centerX(), cardUpgrade3.top + 140f, paintText);
    }

    private void drawResultOverlay(Canvas canvas) {
        paintFill.setColor(0xEE0A111D);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        boolean won = "won".equals(game.phase);
        boolean shortIron = "short".equals(game.phase);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(32f);
        paintText.setColor(won ? 0xFF92D4BD : shortIron ? 0xFFE6BA76 : 0xFFEF9A8E);
        canvas.drawText(won ? "VITTORIA!" : shortIron ? "SPEDIZIONE CONCLUSA" : "SCONFITTA", 480f, 210f, paintText);

        paintText.setTextSize(16f);
        paintText.setColor(0xFFE2E7CC);
        String desc = won ? "Hai difeso il quartiere e consegnato " + game.delivered + "/" + game.target + " ferro!"
                : shortIron ? "Ondate superate, ma servivano " + game.target + " ferro (consegnati: " + game.delivered + ")."
                : "La città ha perso tutta l'integrità.";
        canvas.drawText(desc, 480f, 260f, paintText);

        // Stats summary
        paintText.setTextSize(14f);
        paintText.setColor(0xFF9FBEC1);
        canvas.drawText("Minerale estratto: " + game.stats.mined + "  ·  Nemici frantumati: " + game.kills, 480f, 320f, paintText);
        canvas.drawText("Ferro forgiato: " + game.stats.smelted + "  ·  Strutture erette: " + game.stats.built, 480f, 350f, paintText);

        // Restart button
        paintFill.setColor(0xFF385547);
        canvas.drawRoundRect(btnRestart, 10f, 10f, paintFill);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("NUOVA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }

    private void drawPauseOverlay(Canvas canvas) {
        paintFill.setColor(0xCC080D16);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(30f);
        paintText.setColor(0xFFE6BA76);
        canvas.drawText("IN PAUSA", 480f, 340f, paintText);

        paintFill.setColor(0xFF2C4358);
        canvas.drawRoundRect(btnResume, 8f, 8f, paintFill);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("RIPRENDI", btnResume.centerX(), btnResume.centerY() + 6f, paintText);

        paintFill.setColor(0xFF58332C);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintText.setTextSize(16f);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText("RICOMINCIA", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }
}

