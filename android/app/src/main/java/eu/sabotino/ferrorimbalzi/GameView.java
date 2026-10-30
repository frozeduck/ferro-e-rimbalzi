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

    // Touch regions in virtual coordinates (Portrait W=540, H=960)
    private final RectF btnPause = new RectF(480f, 10f, 530f, 55f);

    // Wave Mode Bottom Buttons
    private final RectF btnForge = new RectF(15f, 850f, 175f, 935f);
    private final RectF btnBurn = new RectF(185f, 850f, 345f, 935f);
    private final RectF btnRecall = new RectF(355f, 850f, 525f, 935f);

    // Build Mode Bottom Buttons
    private final RectF btnWall = new RectF(15f, 850f, 165f, 935f);
    private final RectF btnTurret = new RectF(175f, 850f, 325f, 935f);
    private final RectF btnStartWave = new RectF(335f, 850f, 525f, 935f);

    // Upgrade Cards (Vertical stack for mobile portrait)
    private final RectF cardUpgrade1 = new RectF(30f, 220f, 510f, 370f);
    private final RectF cardUpgrade2 = new RectF(30f, 390f, 510f, 540f);
    private final RectF cardUpgrade3 = new RectF(30f, 560f, 510f, 710f);

    // Pause/Result Overlay Buttons
    private final RectF btnResume = new RectF(150f, 480f, 390f, 545f);
    private final RectF btnRestart = new RectF(150f, 565f, 390f, 630f);

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

            // Trascinamento strutture esistenti
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
    // RENDERING VERTICALE NATIVO
    // ==========================================

    private void drawGame(Canvas canvas) {
        canvas.drawColor(0xFF080D16);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(viewScale, viewScale);

        // Sfondo arena
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0B1422);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        // Griglia nell'area edificabile durante la fase di costruzione
        if ("build".equals(game.phase)) {
            paintFill.setColor(0x20B3BDC6);
            for (float x = 20f; x < Game.W; x += 32f) {
                for (float y = 220f; y < 730f; y += 32f) {
                    canvas.drawRect(x, y, x + 2f, y + 2f, paintFill);
                }
            }
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0x408194A4);
            paintStroke.setStrokeWidth(1.5f);
            canvas.drawRect(16f, 220f, Game.W - 16f, 730f, paintStroke);

            paintText.setTextSize(12f);
            paintText.setColor(0xFF839699);
            paintText.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("ZONA EDIFICABILE (Trascina o costruisci)", 24f, 240f, paintText);
        }

        // Pareti e perimetro dell'arena
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF334050);
        canvas.drawRect(0f, 65f, Game.W, 71f, paintFill);      // bordo superiore sotto HUD
        canvas.drawRect(0f, 65f, 10f, 820f, paintFill);        // bordo sinistro
        canvas.drawRect(Game.W - 10f, 65f, Game.W, 820f, paintFill); // bordo destro

        // Linea di difesa del quartiere
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x70B4815A);
        paintStroke.setStrokeWidth(2f);
        canvas.drawLine(10f, 760f, Game.W - 10f, 760f, paintStroke);
        paintText.setTextSize(10f);
        paintText.setColor(0xFFAA916E);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LINEA DI DIFESA", Game.W / 2f, 755f, paintText);

        // Giacimenti minerari
        for (int i = 0; i < game.mines.size(); i++) {
            Game.Mine m = game.mines.get(i);
            boolean flash = (game.elapsed - game.hits.getOrDefault("mine" + i, -10f)) < 0.16f;
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(flash ? 0xFFC39D6B : 0xFF3D4B3E);
            canvas.drawRect(m.x, m.y, m.x + m.w, m.y + m.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFA88C5C);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(m.x, m.y, m.x + m.w, m.y + m.h, paintStroke);

            paintText.setTextSize(11f);
            paintText.setColor(0xFFD6C6A0);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GIACIMENTO", m.x + m.w / 2f, m.y + m.h / 2f + 4f, paintText);
        }

        // Edifici (Fonderia, Deposito, Sponde, Torrette)
        for (Game.Building b : game.buildings) {
            drawBuilding(canvas, b);
        }

        // Colpi laser delle sentinelle
        for (Game.Shot shot : game.shots) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2.5f);
            int alpha = (int) (255 * (shot.life / 0.2f));
            paintStroke.setColor(Color.argb(alpha, 240, 215, 140));
            canvas.drawLine(shot.x, shot.y, shot.tx, shot.ty, paintStroke);
        }

        // Nemici
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

            // Punti vita nemico
            for (int k = 0; k < e.maxHp; k++) {
                paintFill.setColor(k < e.hp ? (thief ? 0xFFA6B9A0 : 0xFFD29C83) : 0xFF35443B);
                canvas.drawRect(e.x + 3f + k * 8f, e.y + e.h + 3f, e.x + 9f + k * 8f, e.y + e.h + 6f, paintFill);
            }
        }

        // Racchetta del giocatore
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFB6C8A8);
        canvas.drawRect(game.paddle.x - game.paddleWidth / 2f, game.paddle.y,
                game.paddle.x + game.paddleWidth / 2f, game.paddle.y + 14f, paintFill);
        paintFill.setColor(0xFFE8C37F);
        canvas.drawRect(game.paddle.x - 12f, game.paddle.y + 3f, game.paddle.x + 12f, game.paddle.y + 11f, paintFill);

        // Linea di mira quando la sfera è trattenuta
        if (game.ball.held && "wave".equals(game.phase) && !paused) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setStrokeWidth(2f);
            paintStroke.setColor(0xAAE8D8A9);
            float endX = game.ball.x + (float) Math.cos(game.aim) * 150f;
            float endY = game.ball.y + (float) Math.sin(game.aim) * 150f;
            canvas.drawLine(game.ball.x, game.ball.y, endX, endY, paintStroke);

            paintText.setTextSize(13f);
            paintText.setColor(0xFFE0DCBA);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("MIRA E RILASCIA PER LANCIARE", Game.W / 2f, game.paddle.y - 36f, paintText);
        }

        // Scia della sfera
        for (int i = game.ball.trail.size() - 1; i >= 0; i--) {
            Game.PointF t = game.ball.trail.get(i);
            float ratio = 1f - (float) i / (game.ball.trail.size() + 1);
            int alpha = (int) (80 * ratio);
            paintFill.setColor(game.burn ? Color.argb(alpha, 233, 157, 102) : Color.argb(alpha, 209, 215, 160));
            canvas.drawCircle(t.x, t.y, game.ball.r * ratio, paintFill);
        }

        // Sfera
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(game.burn ? 0xFFEABD7B : 0xFFE5E8C4);
        canvas.drawCircle(game.ball.x, game.ball.y, game.ball.r, paintFill);

        // Icone carico attorno alla sfera
        if (game.cargo() > 0) {
            int total = game.cargo();
            for (int i = 0; i < total; i++) {
                double a = i * 2.0 * Math.PI / total + game.elapsed * 1.5;
                float px = game.ball.x + (float) Math.cos(a) * 15f;
                float py = game.ball.y + (float) Math.sin(a) * 15f;
                paintFill.setColor(i < game.ball.metal ? 0xFFEFC46E : 0xFFA87D60);
                canvas.drawCircle(px, py, 3f, paintFill);
            }
            paintText.setTextSize(11f);
            paintText.setColor(0xFFE2CCA4);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(game.ball.ore + " Min · " + game.ball.metal + " Fe", game.ball.x, game.ball.y - 16f, paintText);
        }

        // Messaggi fluttuanti ed effetti visivi
        for (Game.Effect eff : game.effects) {
            int alpha = (int) (255 * Game.clamp(eff.life, 0f, 1f));
            paintText.setColor((eff.color & 0x00FFFFFF) | (alpha << 24));
            paintText.setTextSize(14f);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(eff.text, eff.x, eff.y, paintText);
        }

        // HUD Superiore e Barra Comandi Inferiore
        drawHUD(canvas);

        // Overlay Modali
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
            paintText.setTextSize(10f);
            paintText.setColor(0xFF8AA18A);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SPONDA", b.x + b.w / 2f, b.y + b.h / 2f + 3f, paintText);
        } else if ("forge".equals(b.type)) {
            paintFill.setColor(game.forgeOpen ? 0xFF3D3B2E : 0xFF604934);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFE9AE64);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintStroke);

            paintText.setTextSize(12f);
            paintText.setColor(0xFFE9AE64);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(game.forgeOpen ? "FONDERIA APERTA →" : "TETTO CHIUSO (SPONDA)", b.x + b.w / 2f, b.y + b.h / 2f + 4f, paintText);
        } else if ("depot".equals(b.type)) {
            paintFill.setColor(0xFF234437);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFF9FBEC1);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintStroke);

            paintText.setTextSize(12f);
            paintText.setColor(0xFF9FBEC1);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("DEPOSITO (INCASSA)", b.x + b.w / 2f, b.y + b.h / 2f + 4f, paintText);
        } else if ("turret".equals(b.type)) {
            paintFill.setColor(0xFF314B35);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFB9C594);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(b.x, b.y, b.x + b.w, b.y + b.h, paintStroke);

            paintFill.setColor(0xFFB6C397);
            canvas.drawCircle(b.x + b.w / 2f, b.y + b.h / 2f, 9f, paintFill);

            paintText.setTextSize(10f);
            paintText.setColor(0xFFB9C594);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SENTINELLA", b.x + b.w / 2f, b.y + b.h + 12f, paintText);
        }
    }

    private void drawHUD(Canvas canvas) {
        // Barra superiore dell'HUD
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE0D1622);
        canvas.drawRect(0f, 0f, Game.W, 65f, paintFill);

        // Ondata
        paintText.setTextSize(15f);
        paintText.setColor(0xFFE6BA76);
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Ondata " + game.wave + "/5", 16f, 28f, paintText);

        // Cuori / Integrità
        paintText.setColor(0xFFEF9A8E);
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < game.hp; i++) hearts.append("♥ ");
        canvas.drawText(hearts.toString(), 16f, 52f, paintText);

        // Risorse ferro & Consegne
        paintText.setColor(0xFFEFC46E);
        canvas.drawText("Fe: " + game.iron, 160f, 28f, paintText);
        paintText.setColor(0xFF92D4BD);
        canvas.drawText("Consegnati: " + game.delivered + "/" + game.target, 160f, 52f, paintText);

        // Timer rimanente
        int sec = (int) Math.max(0, game.duration - game.time);
        paintText.setColor(0xFFC0CAD0);
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(sec + "s", 465f, 38f, paintText);

        // Tasto Pausa
        paintFill.setColor(0xFF263544);
        canvas.drawRoundRect(btnPause, 6f, 6f, paintFill);
        paintText.setColor(0xFFFFFFFF);
        paintText.setTextSize(15f);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("❚❚", btnPause.centerX(), btnPause.centerY() + 5f, paintText);

        // ==========================================
        // BARRA INFERIORE COMANDI (ADATTA AL POLLICE)
        // ==========================================
        paintFill.setColor(0xEE0B1422);
        canvas.drawRect(0f, 835f, Game.W, 955f, paintFill);

        if ("wave".equals(game.phase)) {
            // Tasto Fonderia
            paintFill.setColor(game.forgeOpen ? 0xFF584A2C : 0xFF282E37);
            canvas.drawRoundRect(btnForge, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.forgeOpen ? 0xFFE9AE64 : 0xFF8E959E);
            canvas.drawText(game.forgeOpen ? "Fonderia: ON" : "Fonderia: OFF", btnForge.centerX(), btnForge.centerY() - 2f, paintText);
            paintText.setTextSize(10f);
            canvas.drawText(game.forgeOpen ? "Trasforma minerale" : "Muro riflettente", btnForge.centerX(), btnForge.centerY() + 15f, paintText);

            // Tasto Carico
            paintFill.setColor(game.burn ? 0xFF582822 : 0xFF243B33);
            canvas.drawRoundRect(btnBurn, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.burn ? 0xFFFF9A8E : 0xFF92D4BD);
            canvas.drawText(game.burn ? "Carico: ARMATO" : "Carico: SALVO", btnBurn.centerX(), btnBurn.centerY() - 2f, paintText);
            paintText.setTextSize(10f);
            canvas.drawText(game.burn ? "Danno x3 (consuma)" : "Per consegna deposito", btnBurn.centerX(), btnBurn.centerY() + 15f, paintText);

            // Tasto Richiama
            boolean ready = game.recall <= 0f && !game.ball.held;
            paintFill.setColor(ready ? 0xFF35485E : 0xFF1C2530);
            canvas.drawRoundRect(btnRecall, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(ready ? 0xFFE2E7CC : 0xFF657280);
            String label = game.recall > 0f ? "Richiamo (" + (int) Math.ceil(game.recall) + "s)" : "RICHIAMA SFERA";
            canvas.drawText(label, btnRecall.centerX(), btnRecall.centerY() + 5f, paintText);

        } else if ("build".equals(game.phase)) {
            // Tasto Sponda
            boolean selWall = "wall".equals(placingType);
            paintFill.setColor(selWall ? 0xFF4E7762 : (game.iron >= Game.COST_WALL ? 0xFF273E34 : 0xFF1B2320));
            canvas.drawRoundRect(btnWall, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.iron >= Game.COST_WALL ? 0xFFB6C8A8 : 0xFF5D6660);
            canvas.drawText("+ Sponda", btnWall.centerX(), btnWall.centerY() - 3f, paintText);
            paintText.setTextSize(10f);
            canvas.drawText("(3 Fe)", btnWall.centerX(), btnWall.centerY() + 13f, paintText);

            // Tasto Sentinella
            boolean selTurret = "turret".equals(placingType);
            paintFill.setColor(selTurret ? 0xFF657540 : (game.iron >= Game.COST_TURRET ? 0xFF364124 : 0xFF1C2216));
            canvas.drawRoundRect(btnTurret, 8f, 8f, paintFill);
            paintText.setTextSize(13f);
            paintText.setColor(game.iron >= Game.COST_TURRET ? 0xFFE0D4A5 : 0xFF656950);
            canvas.drawText("+ Sentinella", btnTurret.centerX(), btnTurret.centerY() - 3f, paintText);
            paintText.setTextSize(10f);
            canvas.drawText("(7 Fe)", btnTurret.centerX(), btnTurret.centerY() + 13f, paintText);

            // Tasto Avvia Ondata
            paintFill.setColor(0xFF9E6534);
            canvas.drawRoundRect(btnStartWave, 8f, 8f, paintFill);
            paintText.setTextSize(15f);
            paintText.setColor(0xFFFFFFFF);
            canvas.drawText("AVVIA ONDATA ➔", btnStartWave.centerX(), btnStartWave.centerY() + 5f, paintText);
        }
    }

    private void drawUpgradeOverlay(Canvas canvas) {
        paintFill.setColor(0xDD0A111D);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        paintText.setTextSize(24f);
        paintText.setColor(0xFFE6BA76);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ONDATA COMPLETATA", Game.W / 2f, 130f, paintText);

        paintText.setTextSize(14f);
        paintText.setColor(0xFF9AB1A0);
        canvas.drawText("Scegli un potenziamento per la spedizione:", Game.W / 2f, 170f, paintText);

        // Scheda 1: Stiva
        paintFill.setColor(0xFF1B2936);
        canvas.drawRoundRect(cardUpgrade1, 10f, 10f, paintFill);
        paintStroke.setColor(0xFF4C6A85);
        canvas.drawRoundRect(cardUpgrade1, 10f, 10f, paintStroke);
        paintText.setColor(0xFFEFC46E);
        paintText.setTextSize(18f);
        canvas.drawText("📦 STIVA RINFORZATA", cardUpgrade1.centerX(), cardUpgrade1.top + 50f, paintText);
        paintText.setColor(0xFFD0D7DE);
        paintText.setTextSize(14f);
        canvas.drawText("+2 Capienza di Carico", cardUpgrade1.centerX(), cardUpgrade1.top + 85f, paintText);
        canvas.drawText("Permette di trasportare più minerale e ferro", cardUpgrade1.centerX(), cardUpgrade1.top + 115f, paintText);

        // Scheda 2: Racchetta
        paintFill.setColor(0xFF1B2936);
        canvas.drawRoundRect(cardUpgrade2, 10f, 10f, paintFill);
        paintStroke.setColor(0xFF4C6A85);
        canvas.drawRoundRect(cardUpgrade2, 10f, 10f, paintStroke);
        paintText.setColor(0xFF92D4BD);
        paintText.setTextSize(18f);
        canvas.drawText("🛡️ RACCHETTA MAGGIORATA", cardUpgrade2.centerX(), cardUpgrade2.top + 50f, paintText);
        paintText.setColor(0xFFD0D7DE);
        paintText.setTextSize(14f);
        canvas.drawText("+20px Lunghezza & +1 Cuore", cardUpgrade2.centerX(), cardUpgrade2.top + 85f, paintText);
        canvas.drawText("Migliora la difesa e recupera integrità al nucleo", cardUpgrade2.centerX(), cardUpgrade2.top + 115f, paintText);

        // Scheda 3: Danno
        paintFill.setColor(0xFF1B2936);
        canvas.drawRoundRect(cardUpgrade3, 10f, 10f, paintFill);
        paintStroke.setColor(0xFF4C6A85);
        canvas.drawRoundRect(cardUpgrade3, 10f, 10f, paintStroke);
        paintText.setColor(0xFFEF9A8E);
        paintText.setTextSize(18f);
        canvas.drawText("⚡ IMPATTO CINETICO", cardUpgrade3.centerX(), cardUpgrade3.top + 50f, paintText);
        paintText.setColor(0xFFD0D7DE);
        paintText.setTextSize(14f);
        canvas.drawText("+1 Danno Base Sfera", cardUpgrade3.centerX(), cardUpgrade3.top + 85f, paintText);
        canvas.drawText("Ogni rimbalzo sui nemici infligge più danni", cardUpgrade3.centerX(), cardUpgrade3.top + 115f, paintText);
    }

    private void drawResultOverlay(Canvas canvas) {
        paintFill.setColor(0xEE0A111D);
        canvas.drawRect(0f, 0f, Game.W, Game.H, paintFill);

        boolean won = "won".equals(game.phase);
        boolean shortIron = "short".equals(game.phase);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(30f);
        paintText.setColor(won ? 0xFF92D4BD : shortIron ? 0xFFE6BA76 : 0xFFEF9A8E);
        canvas.drawText(won ? "VITTORIA!" : shortIron ? "SPEDIZIONE CONCLUSA" : "NUCLEO CADUTO", Game.W / 2f, 260f, paintText);

        paintText.setTextSize(15f);
        paintText.setColor(0xFFE2E7CC);
        String desc = won ? "Hai difeso il quartiere e consegnato " + game.delivered + "/" + game.target + " Fe!"
                : shortIron ? "Ondate superate, ma servivano " + game.target + " Fe (consegnati: " + game.delivered + ")."
                : "La città ha perso tutta l'integrità.";
        canvas.drawText(desc, Game.W / 2f, 310f, paintText);

        // Statistiche finali
        paintText.setTextSize(14f);
        paintText.setColor(0xFF9FBEC1);
        canvas.drawText("Minerale estratto: " + game.stats.mined, Game.W / 2f, 380f, paintText);
        canvas.drawText("Ferro forgiato: " + game.stats.smelted, Game.W / 2f, 410f, paintText);
        canvas.drawText("Nemici abbattuti: " + game.kills, Game.W / 2f, 440f, paintText);
        canvas.drawText("Strutture edificate: " + game.stats.built, Game.W / 2f, 470f, paintText);

        // Pulsante Ricomincia
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
        paintText.setTextSize(28f);
        paintText.setColor(0xFFE6BA76);
        canvas.drawText("GIOCO IN PAUSA", Game.W / 2f, 390f, paintText);

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
