package eu.sabotino.ferrorimbalzi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
    // Risoluzione virtuale nativa verticale (Portrait 9:16)
    public static final float W = 540f;
    public float H = 960f;

    public static final int COST_WALL = 3;
    public static final int COST_TURRET = 7;

    public static class PointF {
        public float x, y;
        public PointF(float x, float y) { this.x = x; this.y = y; }
    }

    public static class Ball {
        public float x = 270f, y = 768f, vx = 0f, vy = 0f, r = 8f;
        public int ore = 0, metal = 0;
        public boolean held = true;
        public List<PointF> trail = new ArrayList<>();
    }

    public static class Paddle {
        public float x = 270f, y = 790f;
    }

    public static class Building {
        public String id;
        public String type; // forge, depot, wall, turret
        public float x, y, w, h;
        public float cooldown = 0f;

        public Building() {}
        public Building(String id, String type, float x, float y, float w, float h) {
            this.id = id; this.type = type; this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    public static class Mine {
        public float x, y, w, h;
        public Mine(float x, float y, float w, float h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    public static class Enemy {
        public String id;
        public String type; // beetle, thief
        public float x, y, w, h;
        public int hp, maxHp;
    }

    public static class Effect {
        public String text;
        public float x, y;
        public int color;
        public float life;
        public Effect(String text, float x, float y, int color, float life) {
            this.text = text; this.x = x; this.y = y; this.color = color; this.life = life;
        }
    }

    public static class Shot {
        public float x, y, tx, ty, life;
        public Shot(float x, float y, float tx, float ty, float life) {
            this.x = x; this.y = y; this.tx = tx; this.ty = ty; this.life = life;
        }
    }

    public static class Stats {
        public int mined = 0;
        public int smelted = 0;
        public int spentCargo = 0;
        public int lostCargo = 0;
        public int built = 0;
        public int recalls = 0;
    }

    public String phase = "build"; // build, wave, upgrade, won, short, lost
    public int wave = 0;
    public int maxWaves = 5;
    public float time = 0f;
    public float duration = 40f;
    public int iron = 6;
    public int delivered = 0;
    public int target = 14;
    public int hp = 6;
    public int maxHp = 6;
    public int kills = 0;
    public int capacity = 4;
    public int damage = 1;
    public float paddleWidth = 110f;
    public int xp = 0;
    public int level = 1;
    public boolean burn = false;
    public boolean forgeOpen = true;
    public float recall = 0f;
    public float aim = -1.57f;
    public float elapsed = 0f;
    public int nextId = 1;

    public Ball ball = new Ball();
    public Paddle paddle = new Paddle();
    public List<Building> buildings = new ArrayList<>();
    public List<Mine> mines = new ArrayList<>();
    public List<Enemy> enemies = new ArrayList<>();
    public List<Effect> effects = new ArrayList<>();
    public List<String> events = new ArrayList<>();
    public Map<String, Float> hits = new HashMap<>();
    public List<Shot> shots = new ArrayList<>();
    public Stats stats = new Stats();

    public Game() {
        initDefaultWorld();
    }

    public void initDefaultWorld() {
        buildings.clear();
        // Fonderia a sinistra, Deposito a destra, sponda protettiva sul lato destro
        buildings.add(new Building("forge", "forge", 35f, 430f, 140f, 60f));
        buildings.add(new Building("depot", "depot", 365f, 430f, 140f, 60f));
        buildings.add(new Building("wall0", "wall", 395f, 260f, 110f, 22f));

        mines.clear();
        // 3 Giacimenti nel settore minerario superiore
        mines.add(new Mine(35f, 95f, 130f, 44f));
        mines.add(new Mine(205f, 80f, 130f, 44f));
        mines.add(new Mine(375f, 95f, 130f, 44f));
    }

    public void setVirtualHeight(float newH) {
        if (newH < 800f) newH = 800f;
        this.H = newH;
        this.paddle.y = H - 170f;
        if (this.ball.held) {
            this.ball.y = this.paddle.y - 20f;
        }
    }

    public static float clamp(float n, float a, float b) {
        return Math.max(a, Math.min(b, n));
    }

    public void event(String text, float x, float y, int color) {
        events.add(text);
        if (events.size() > 5) events.remove(0);
        effects.add(new Effect(text, x, y, color, 1.5f));
    }

    public void event(String text) {
        event(text, 270f, 500f, 0xFFE6BA76);
    }

    public int cargo() {
        return ball.ore + ball.metal;
    }

    public float speed() {
        return 480f - cargo() * 22f;
    }

    public void normalize() {
        float m = (float) Math.hypot(ball.vx, ball.vy);
        if (m == 0f) m = 1f;
        float v = speed();
        ball.vx = ball.vx / m * v;
        ball.vy = ball.vy / m * v;
        if (Math.abs(ball.vy) < 80f) {
            ball.vy = (ball.vy < 0f ? -1f : 1f) * 110f;
            float n = (float) Math.hypot(ball.vx, ball.vy);
            ball.vx *= v / n;
            ball.vy *= v / n;
        }
    }

    public boolean launch(float angle) {
        if (!"wave".equals(phase) || !ball.held) return false;
        aim = clamp(angle, (float) -Math.PI + 0.22f, -0.22f);
        ball.held = false;
        ball.vx = (float) Math.cos(aim) * speed();
        ball.vy = (float) Math.sin(aim) * speed();
        return true;
    }

    public boolean launch() {
        return launch(aim);
    }

    public boolean recall() {
        if (!"wave".equals(phase) || ball.held || recall > 0f) return false;
        ball.held = true;
        ball.trail.clear();
        recall = 12f;
        stats.recalls++;
        event("Sfera richiamata · carico al sicuro", paddle.x, 740f, 0xFFE6BA76);
        return true;
    }

    public boolean startWave() {
        if (!"build".equals(phase)) return false;
        wave++;
        phase = "wave";
        time = 0f;
        recall = 0f;
        ball.held = true;
        enemies.clear();
        int count = 3 + wave * 2;
        for (int i = 0; i < count; i++) {
            Enemy e = new Enemy();
            e.id = "enemy" + i;
            e.type = (i % 3 == 2) ? "thief" : "beetle";
            e.x = 45f + (i % 5) * 94f;
            e.y = 50f - (i / 5) * 55f;
            e.w = 42f;
            e.h = 32f;
            e.hp = 2 + wave / 2;
            e.maxHp = 2 + wave / 2;
            enemies.add(e);
        }
        event("Ondata " + wave + " · difendi il nucleo", 270f, 160f, 0xFFEF9A8E);
        return true;
    }

    private void settle() {
        ball.held = true;
        ball.trail.clear();
        shots.clear();
        if (wave == maxWaves) {
            phase = delivered >= target ? "won" : "short";
            return;
        }
        phase = "upgrade";
        iron += 2;
        xp += 2;
        event("Ondata respinta! +2 ferro di recupero", 270f, 480f, 0xFFE6BA76);
    }

    public boolean chooseUpgrade(String type) {
        if (!"upgrade".equals(phase)) return false;
        if ("cargo".equals(type)) {
            capacity += 2;
        } else if ("paddle".equals(type)) {
            paddleWidth += 20f;
            hp = Math.min(maxHp, hp + 1);
        } else if ("damage".equals(type)) {
            damage++;
        } else {
            return false;
        }
        level++;
        phase = "build";
        return true;
    }

    public boolean canPlace(float rx, float ry, float rw, float rh, String exclude) {
        if (Float.isNaN(rx) || Float.isNaN(ry) || Float.isNaN(rw) || Float.isNaN(rh)) return false;
        // Zona edificabile mediana verticale
        if (rx < 16f || rx + rw > W - 16f || ry < 220f || ry + rh > H - 230f) return false;
        for (Building b : buildings) {
            if (b.id.equals(exclude)) continue;
            if (rx < b.x + b.w + 10f && rx + rw + 10f > b.x && ry < b.y + b.h + 10f && ry + rh + 10f > b.y) {
                return false;
            }
        }
        for (Mine m : mines) {
            if (rx < m.x + m.w + 10f && rx + rw + 10f > m.x && ry < m.y + m.h + 10f && ry + rh + 10f > m.y) {
                return false;
            }
        }
        return true;
    }

    public boolean build(String type, float x, float y) {
        if (!"build".equals(phase)) return false;
        int cost = "wall".equals(type) ? COST_WALL : "turret".equals(type) ? COST_TURRET : -1;
        if (cost < 0 || iron < cost || buildings.size() >= 12) return false;

        float rw = "wall".equals(type) ? 120f : 50f;
        float rh = "wall".equals(type) ? 22f : 50f;
        float rx = Math.round((x - rw / 2f) / 16f) * 16f;
        float ry = Math.round((y - rh / 2f) / 16f) * 16f;

        if (!canPlace(rx, ry, rw, rh, null)) return false;
        iron -= cost;
        Building b = new Building();
        b.id = "built" + (nextId++);
        b.type = type;
        b.x = rx;
        b.y = ry;
        b.w = rw;
        b.h = rh;
        b.cooldown = 0f;
        buildings.add(b);
        stats.built++;
        return true;
    }

    public boolean moveBuilding(String id, float x, float y) {
        if (!"build".equals(phase)) return false;
        Building b = null;
        for (Building item : buildings) {
            if (item.id.equals(id)) { b = item; break; }
        }
        if (b == null) return false;
        float rx = Math.round((x - b.w / 2f) / 16f) * 16f;
        float ry = Math.round((y - b.h / 2f) / 16f) * 16f;
        if (!canPlace(rx, ry, b.w, b.h, id)) return false;
        b.x = rx;
        b.y = ry;
        return true;
    }

    public boolean interact(String type) {
        if ("mine".equals(type) && cargo() < capacity) {
            ball.ore++;
            stats.mined++;
            event("+1 minerale", ball.x, ball.y - 18f, 0xFFD8A677);
            normalize();
            return true;
        }
        if ("forge".equals(type) && forgeOpen && ball.ore > 0) {
            int n = ball.ore;
            ball.metal += n;
            ball.ore = 0;
            stats.smelted += n;
            event(n + " minerale → ferro!", ball.x, ball.y - 18f, 0xFFF8C467);
            return true;
        }
        if ("depot".equals(type) && ball.metal > 0) {
            int n = ball.metal;
            iron += n;
            delivered += n;
            ball.metal = 0;
            event("Consegnati +" + n + " Fe", ball.x, ball.y - 22f, 0xFF92D4BD);
            normalize();
            return true;
        }
        return false;
    }

    public void hitEnemy(Enemy e) {
        int dmg = damage;
        if (burn && cargo() > 0) {
            if (ball.metal > 0) {
                ball.metal--;
                dmg += 4;
            } else {
                ball.ore--;
                dmg += 2;
            }
            stats.spentCargo++;
            event("Carico bruciato!", e.x, e.y, 0xFFFFC177);
            normalize();
        }
        e.hp -= dmg;
        if (e.hp <= 0) {
            kills++;
            xp++;
            event("Distrutto", e.x, e.y, 0xFF93CBB6);
        } else {
            event("−" + dmg, e.x, e.y, 0xFFF8E5C7);
        }
    }

    public static boolean collide(Ball ball, float rx, float ry, float rw, float rh, boolean solid) {
        float cx = clamp(ball.x, rx, rx + rw);
        float cy = clamp(ball.y, ry, ry + rh);
        float dx = ball.x - cx;
        float dy = ball.y - cy;
        float d = (float) Math.hypot(dx, dy);
        if (d >= ball.r) return false;
        if (!solid) return true;
        if (d == 0f) {
            float l = ball.x - rx;
            float r = rx + rw - ball.x;
            float t = ball.y - ry;
            float bot = ry + rh - ball.y;
            float min = Math.min(Math.min(l, r), Math.min(t, bot));
            int i = (min == l) ? 0 : (min == r) ? 1 : (min == t) ? 2 : 3;
            dx = (i == 0) ? -1f : (i == 1) ? 1f : 0f;
            dy = (i == 2) ? -1f : (i == 3) ? 1f : 0f;
            d = 1f;
            if (i < 2) ball.x = (i == 0) ? rx - ball.r : rx + rw + ball.r;
            else ball.y = (i == 2) ? ry - ball.r : ry + rh + ball.r;
        } else {
            ball.x += dx / d * (ball.r - d + 0.1f);
            ball.y += dy / d * (ball.r - d + 0.1f);
        }
        float nx = dx / d, ny = dy / d;
        float dot = ball.vx * nx + ball.vy * ny;
        if (dot < 0f) {
            ball.vx -= 2f * dot * nx;
            ball.vy -= 2f * dot * ny;
        }
        return true;
    }

    public static boolean collide(Ball ball, Mine m) {
        return collide(ball, m.x, m.y, m.w, m.h, false);
    }

    public static boolean collide(Ball ball, Building b, boolean solid) {
        return collide(ball, b.x, b.y, b.w, b.h, solid);
    }

    public static boolean collide(Ball ball, Enemy e) {
        return collide(ball, e.x, e.y, e.w, e.h, false);
    }

    public void step(float dt) {
        dt = clamp(dt, 0f, 0.05f);
        elapsed += dt;

        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect eff = effects.get(i);
            eff.life -= dt;
            eff.y -= dt * 20f;
            if (eff.life <= 0f) effects.remove(i);
        }

        if (!"wave".equals(phase)) return;

        time += dt;
        recall = Math.max(0f, recall - dt);

        if (ball.held) {
            ball.x = paddle.x;
            ball.y = paddle.y - 20f;
        }

        for (Enemy e : enemies) {
            if (e.hp <= 0) continue;
            e.y += dt * (e.type.equals("thief") ? 9f + wave : 5f + wave);
            if (e.y + e.h >= H - 200f) {
                e.hp = 0;
                hp--;
                event("Breccia! −1 Cuore", e.x, H - 210f, 0xFFE9978B);
            }
        }

        for (Building turret : buildings) {
            if (!turret.type.equals("turret")) continue;
            turret.cooldown = Math.max(0f, turret.cooldown - dt);
            Enemy targetEnemy = null;
            for (Enemy e : enemies) {
                if (e.hp > 0 && Math.hypot(e.x - turret.x, e.y - turret.y) < 260.0) {
                    targetEnemy = e;
                    break;
                }
            }
            if (targetEnemy != null && turret.cooldown == 0f) {
                targetEnemy.hp -= 1;
                if (targetEnemy.hp <= 0) {
                    kills++;
                    xp++;
                }
                shots.add(new Shot(turret.x + 25f, turret.y + 25f, targetEnemy.x + 21f, targetEnemy.y + 16f, 0.2f));
                turret.cooldown = 1.6f;
            }
        }

        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot s = shots.get(i);
            s.life -= dt;
            if (s.life <= 0f) shots.remove(i);
        }

        if (!ball.held) {
            ball.trail.add(0, new PointF(ball.x, ball.y));
            if (ball.trail.size() > 20) ball.trail.remove(ball.trail.size() - 1);

            int n = Math.max(1, (int) Math.ceil(speed() * dt / (ball.r * 0.65f)));
            float h = dt / n;

            for (int k = 0; k < n; k++) {
                ball.x += ball.vx * h;
                ball.y += ball.vy * h;

                // Pareti laterali e superiore
                if (ball.x < ball.r + 14f) { ball.x = ball.r + 14f; ball.vx = Math.abs(ball.vx); }
                if (ball.x > W - 14f - ball.r) { ball.x = W - 14f - ball.r; ball.vx = -Math.abs(ball.vx); }
                if (ball.y < ball.r + 80f) { ball.y = ball.r + 80f; ball.vy = Math.abs(ball.vy); }

                // Rimbalzo sulla racchetta
                if (ball.vy > 0f && ball.y + ball.r >= paddle.y && ball.y - ball.r < paddle.y + 16f
                        && Math.abs(ball.x - paddle.x) < paddleWidth / 2f + ball.r) {
                    float off = clamp((ball.x - paddle.x) / (paddleWidth / 2f), -1f, 1f);
                    float a = (float) (-Math.PI / 2.0) + off * 1.15f;
                    ball.vx = (float) Math.cos(a) * speed();
                    ball.vy = (float) Math.sin(a) * speed();
                    ball.y = paddle.y - ball.r - 0.2f;
                }

                // Miniere
                for (int i = 0; i < mines.size(); i++) {
                    Mine m = mines.get(i);
                    if (collide(ball, m) && elapsed - hits.getOrDefault("mine" + i, -10f) > 0.32f) {
                        hits.put("mine" + i, elapsed);
                        interact("mine");
                    }
                }

                // Edifici
                for (Building facility : buildings) {
                    boolean solid = facility.type.equals("wall") || facility.type.equals("turret")
                            || (facility.type.equals("forge") && !forgeOpen);
                    if (collide(ball, facility, solid) && elapsed - hits.getOrDefault(facility.id, -10f) > 0.28f) {
                        hits.put(facility.id, elapsed);
                        interact(facility.type);
                    }
                }

                // Nemici
                for (Enemy e : enemies) {
                    if (e.hp > 0 && collide(ball, e) && elapsed - hits.getOrDefault(e.id, -10f) > 0.20f) {
                        hits.put(e.id, elapsed);
                        hitEnemy(e);
                    }
                }

                // Caduta sotto la linea
                if (ball.y > H + 15f) {
                    hp--;
                    stats.lostCargo += cargo();
                    ball.ore = 0;
                    ball.metal = 0;
                    ball.held = true;
                    ball.trail.clear();
                    event("Sfera persa! −1 Cuore", 270f, 750f, 0xFFEF9A8E);
                    break;
                }
            }
        }

        enemies.removeIf(e -> e.hp <= 0);

        if (hp <= 0) {
            hp = 0;
            phase = "lost";
            return;
        }

        if (time >= duration) {
            int escaped = enemies.size();
            if (escaped > 0) {
                int penalty = (int) Math.ceil(escaped / 3.0);
                hp = Math.max(0, hp - penalty);
                event("Assedio nemico: −" + penalty + " Cuori", 270f, 750f, 0xFFEF9A8E);
            }
            enemies.clear();
            if (hp <= 0) {
                phase = "lost";
            } else {
                settle();
            }
        } else if (time > 6f && enemies.isEmpty()) {
            settle();
        }
    }
}
