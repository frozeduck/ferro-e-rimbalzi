import React, { useEffect, useRef, useState, useCallback } from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { Ball, Building, Enemy, Mine, Mushroom, Prism } from '../types/game';

interface GameCanvasProps {
  game: GameEngine;
  paused: boolean;
  onOpenTutorial: () => void;
  onTogglePause: () => void;
  placingType: Building['type'] | null;
  setPlacingType: (type: Building['type'] | null) => void;
  onStateChange: () => void;
}

interface Particle {
  active: boolean;
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  type: number; // 0: sparks, 1: embers, 2: rock, 3: crystal, 4: spore, 5: wood
}

interface Sparkle {
  x: number;
  y: number;
  speed: number;
  size: number;
  alpha: number;
}

const SPRITE_URLS: Record<string, string> = {
  mine: '/assets/sprite_miniera.png',
  tree: '/assets/sprite_albero.png',
  crystal_mine: '/assets/sprite_cristallo.png',
  forge_open: '/assets/sprite_forgia_aperta.png',
  forge_closed: '/assets/sprite_forgia_chiusa.png',
  sawmill: '/assets/sprite_segheria.png',
  gemcutter: '/assets/sprite_tagliapietre.png',
  vault: '/assets/sprite_forziere.png',
  crystal_vault: '/assets/sprite_forziere_cristallo.png',
  wall: '/assets/sprite_muro.png',
  wood_wall: '/assets/sprite_muro_legno.png',
  crystal_wall: '/assets/sprite_muro_ossidiana.png',
  turret: '/assets/sprite_balista.png',
  laser_turret: '/assets/sprite_torretta_laser.png',
  shield: '/assets/sprite_scudo.png',
  orb: '/assets/sprite_sfera.png',
  goblin: '/assets/sprite_goblin.png',
  spider: '/assets/sprite_ragno.png',
  golem: '/assets/sprite_golem.png',
  spectre: '/assets/sprite_spettro.png',
  mushroom: '/assets/sprite_fungo.png',
  prism: '/assets/sprite_prisma.png',
  bg_cavern: '/assets/sfondo_caverna.png',
  bg_forest: '/assets/sfondo_foresta.png',
  bg_crystals: '/assets/sfondo_cristalli.png',
};

export const GameCanvas: React.FC<GameCanvasProps> = ({
  game,
  paused,
  onOpenTutorial,
  onTogglePause,
  placingType,
  setPlacingType,
  onStateChange,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const imagesRef = useRef<Record<string, HTMLImageElement>>({});
  const [imagesLoaded, setImagesLoaded] = useState(false);

  // Juice & Game Feel
  const traumaRef = useRef(0);
  const hitStopTimerRef = useRef(0);
  const paddleRecoilYRef = useRef(0);
  const damageFlashRef = useRef(0);
  const ballSquashXRef = useRef(1);
  const ballSquashYRef = useRef(1);

  // Particles & Sparkles
  const particlesRef = useRef<Particle[]>([]);
  const sparklesRef = useRef<Sparkle[]>([]);

  // Dragging in build phase
  const draggingBuildingRef = useRef<Building | null>(null);
  const dragOffsetRef = useRef({ x: 0, y: 0 });

  const addTrauma = useCallback((t: number) => {
    traumaRef.current = Math.min(1.0, traumaRef.current + t);
  }, []);

  const triggerHitStop = useCallback((durationSec: number) => {
    hitStopTimerRef.current = Math.max(hitStopTimerRef.current, durationSec);
  }, []);

  const vibrate = useCallback((millis: number) => {
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate(millis);
      } catch {
        // Ignore
      }
    }
  }, []);

  const spawnParticles = useCallback((x: number, y: number, count: number, color: string, type: number) => {
    const list = particlesRef.current;
    for (let i = 0; i < count; i++) {
      const angle = Math.random() * Math.PI * 2.0;
      let speed: number;
      let vx: number;
      let vy: number;

      if (type === 1) {
        // Embers rising
        speed = 15 + Math.random() * 40;
        vx = Math.cos(angle) * speed * 0.4;
        vy = -Math.abs(Math.sin(angle) * speed) - 25;
      } else if (type === 4) {
        // Mushroom spores
        speed = 20 + Math.random() * 45;
        vx = Math.cos(angle) * speed * 0.8;
        vy = -Math.abs(Math.sin(angle) * speed * 0.5) - 10;
      } else if (type === 3) {
        // Crystal shards
        speed = 50 + Math.random() * 180;
        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed;
      } else if (type === 5) {
        // Wood shavings
        speed = 30 + Math.random() * 70;
        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed * 0.6 - 15;
      } else {
        speed = 35 + Math.random() * 150;
        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed;
      }

      const life = 0.22 + Math.random() * 0.42;
      const size = type === 2 || type === 5 ? 3.5 + Math.random() * 3 : 2 + Math.random() * 2.5;

      list.push({
        active: true,
        x,
        y,
        vx,
        vy,
        life,
        maxLife: life,
        size,
        color,
        type,
      });
    }

    if (list.length > 200) {
      particlesRef.current = list.slice(list.length - 150);
    }
  }, []);

  // Preload sprites
  useEffect(() => {
    let loadedCount = 0;
    const total = Object.keys(SPRITE_URLS).length;
    const imgs: Record<string, HTMLImageElement> = {};

    for (const [key, url] of Object.entries(SPRITE_URLS)) {
      const img = new Image();
      img.src = url;
      img.onload = () => {
        loadedCount++;
        if (loadedCount >= total) {
          imagesRef.current = imgs;
          setImagesLoaded(true);
        }
      };
      img.onerror = () => {
        loadedCount++;
        if (loadedCount >= total) {
          imagesRef.current = imgs;
          setImagesLoaded(true);
        }
      };
      imgs[key] = img;
    }

    // Init sparkles
    const sparkles: Sparkle[] = [];
    for (let i = 0; i < 45; i++) {
      sparkles.push({
        x: Math.random() * GameEngine.W,
        y: Math.random() * 960,
        speed: 10 + Math.random() * 26,
        size: 1 + Math.random() * 2.2,
        alpha: 0.2 + Math.random() * 0.6,
      });
    }
    sparklesRef.current = sparkles;
  }, []);

  // Wire up audio & haptic listeners
  useEffect(() => {
    game.listener = {
      onPaddleHit: (ball, off) => {
        AudioHelper.playShieldDeflect(off);
        vibrate(15);
        ballSquashXRef.current = 1.35;
        ballSquashYRef.current = 0.65;
        paddleRecoilYRef.current = 4.5;
        spawnParticles(ball.x, game.paddle.y, 10, '#F59E0B', 0);
        onStateChange();
      },
      onWallHit: (ball) => {
        AudioHelper.playWallClack();
        ballSquashXRef.current = 0.78;
        ballSquashYRef.current = 1.25;
        spawnParticles(ball.x, ball.y, 5, '#827B99', 2);
        onStateChange();
      },
      onMushroomHit: (m) => {
        AudioHelper.playMushroomBounce();
        vibrate(28);
        addTrauma(0.18);
        ballSquashXRef.current = 1.45;
        ballSquashYRef.current = 0.6;
        spawnParticles(m.x + m.w / 2, m.y + m.h / 2, 14, '#F472B6', 4);
        onStateChange();
      },
      onPrismHit: (p) => {
        AudioHelper.playCrystalChime();
        vibrate(28);
        addTrauma(0.24);
        ballSquashXRef.current = 1.25;
        ballSquashYRef.current = 1.25;
        spawnParticles(p.x + p.w / 2, p.y + p.h / 2, 18, '#67E8F9', 3);
        onStateChange();
      },
      onEnemyHit: (e, _dmg, killed) => {
        if (killed) {
          AudioHelper.playGoblinHit();
          AudioHelper.playCoins();
          vibrate(45);
          addTrauma(0.35);
          triggerHitStop(0.045);
          const bloodColor =
            e.type === 'spectre'
              ? '#A855F7'
              : e.type === 'golem'
              ? '#67E8F9'
              : e.type === 'spider'
              ? '#84CC16'
              : '#22C55E';
          spawnParticles(e.x + e.w / 2, e.y + e.h / 2, 22, bloodColor, 0);
          spawnParticles(e.x + e.w / 2, e.y + e.h / 2, 10, '#FBBF24', 0);
        } else {
          vibrate(15);
          addTrauma(0.12);
          spawnParticles(e.x + e.w / 2, e.y + e.h / 2, 6, '#E2E8F0', 0);
        }
        onStateChange();
      },
      onHeartLost: () => {
        AudioHelper.playWarhorn();
        vibrate(60);
        addTrauma(0.65);
        triggerHitStop(0.07);
        damageFlashRef.current = 0.75;
        onStateChange();
      },
      onBuildingHit: (b) => {
        if (b.type === 'forge') {
          AudioHelper.playForgeFire();
          spawnParticles(b.x + b.w / 2, b.y + 12, 12, '#FF8A00', 1);
        } else if (b.type === 'sawmill') {
          AudioHelper.playSawmillBuzz();
          spawnParticles(b.x + b.w / 2, b.y + 12, 14, '#FCD34D', 5);
        } else if (b.type === 'gemcutter') {
          AudioHelper.playCrystalChime();
          spawnParticles(b.x + b.w / 2, b.y + 12, 14, '#67E8F9', 3);
        } else if (b.type === 'depot' || b.type === 'crystal_vault') {
          AudioHelper.playCoins();
          spawnParticles(b.x + b.w / 2, b.y + 12, 16, '#FBBF24', 0);
        } else {
          AudioHelper.playWallClack();
          spawnParticles(b.x + b.w / 2, b.y + b.h / 2, 8, '#827B99', 2);
        }
        onStateChange();
      },
      onMineHit: (m, _index) => {
        const isCrystal = game.selectedBiome === 'crystal_cave';
        const isForest = game.selectedBiome === 'forest_wood';
        AudioHelper.playMiningStrike(isCrystal, isForest);
        vibrate(15);
        const partColor = isCrystal ? '#67E8F9' : isForest ? '#86EFAC' : '#D8A677';
        const partType = isCrystal ? 3 : isForest ? 5 : 2;
        spawnParticles(m.x + m.w / 2, m.y + m.h / 2, 10, partColor, partType);
        onStateChange();
      },
      onLaserFired: (_sx, _sy, _tx, _ty, isLaser) => {
        if (isLaser) {
          AudioHelper.playLaserBeam();
          addTrauma(0.12);
        } else {
          AudioHelper.playBallista();
        }
        onStateChange();
      },
    };
  }, [game, vibrate, addTrauma, triggerHitStop, spawnParticles, onStateChange]);

  // Main Render Loop
  useEffect(() => {
    let animationFrameId: number;
    let lastTime = performance.now();

    const render = (now: number) => {
      let dt = (now - lastTime) / 1000.0;
      lastTime = now;
      if (dt > 0.05) dt = 0.05;

      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) {
          // Responsive dimensions
          const dpr = window.devicePixelRatio || 1;
          const rect = canvas.getBoundingClientRect();
          if (canvas.width !== Math.floor(rect.width * dpr) || canvas.height !== Math.floor(rect.height * dpr)) {
            canvas.width = Math.floor(rect.width * dpr);
            canvas.height = Math.floor(rect.height * dpr);
          }

          const scale = rect.width / GameEngine.W;
          const virtualH = rect.height / scale;
          game.setVirtualHeight(virtualH);

          if (!paused && game.screen === 'game') {
            // Update sparkles
            for (const s of sparklesRef.current) {
              s.y -= s.speed * dt;
              if (s.y < 65) {
                s.y = virtualH;
                s.x = Math.random() * GameEngine.W;
              }
            }

            // Update particles
            const pList = particlesRef.current;
            for (let i = pList.length - 1; i >= 0; i--) {
              const p = pList[i];
              p.life -= dt;
              if (p.life <= 0) {
                pList.splice(i, 1);
                continue;
              }
              p.x += p.vx * dt;
              p.y += p.vy * dt;
              if (p.type === 1 || p.type === 4) {
                p.vy -= 18 * dt;
                p.vx *= 0.96;
              } else if (p.type === 2 || p.type === 5) {
                p.vy += 80 * dt;
                p.vx *= 0.94;
              } else {
                p.vx *= 0.93;
                p.vy *= 0.93;
              }
            }

            // Continuous forge sparks
            if (game.forgeOpen && game.phase === 'wave') {
              for (const b of game.buildings) {
                if (b.type === 'forge' && Math.random() < 0.35) {
                  spawnParticles(b.x + Math.random() * b.w, b.y + 6, 1, '#FF8A00', 1);
                }
              }
            }

            // Hit stop or step game
            if (hitStopTimerRef.current > 0) {
              hitStopTimerRef.current = Math.max(0, hitStopTimerRef.current - dt);
            } else {
              game.step(dt);
            }

            // Recover ball shape & paddle recoil
            ballSquashXRef.current += (1.0 - ballSquashXRef.current) * 16.0 * dt;
            ballSquashYRef.current += (1.0 - ballSquashYRef.current) * 16.0 * dt;
            paddleRecoilYRef.current += (0.0 - paddleRecoilYRef.current) * 18.0 * dt;
            damageFlashRef.current = Math.max(0, damageFlashRef.current - 1.5 * dt);
          }

          // DRAW
          ctx.save();
          ctx.scale(dpr, dpr);
          ctx.imageSmoothingEnabled = false;

          // Background fill
          ctx.fillStyle = '#140C09';
          ctx.fillRect(0, 0, rect.width, rect.height);

          // Apply screen shake
          const shake = traumaRef.current * traumaRef.current;
          const shakeX = Math.sin(game.elapsed * 42.0) * 12.0 * shake;
          const shakeY = Math.cos(game.elapsed * 38.0) * 9.0 * shake;
          traumaRef.current = Math.max(0, traumaRef.current - 1.4 * dt);

          ctx.save();
          ctx.translate(shakeX * scale, shakeY * scale);
          ctx.scale(scale, scale);

          // Draw tiled background
          const imgs = imagesRef.current;
          const isCrystal = game.selectedBiome === 'crystal_cave';
          const isForest = game.selectedBiome === 'forest_wood';
          const bgImg = isCrystal ? imgs.bg_crystals : isForest ? imgs.bg_forest : imgs.bg_cavern;

          if (bgImg && bgImg.complete && bgImg.naturalWidth > 0) {
            for (let y = 0; y < virtualH; y += 960) {
              ctx.drawImage(bgImg, 0, y, GameEngine.W, 960);
            }
          }

          // Ambient radial glows
          const glowColor = isCrystal ? 'rgba(6, 182, 212, 0.25)' : isForest ? 'rgba(16, 185, 129, 0.25)' : 'rgba(255, 138, 0, 0.25)';
          const drawGlow = (gx: number, gy: number) => {
            const rad = ctx.createRadialGradient(gx, gy, 0, gx, gy, 200);
            rad.addColorStop(0, glowColor);
            rad.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.fillStyle = rad;
            ctx.beginPath();
            ctx.arc(gx, gy, 200, 0, Math.PI * 2);
            ctx.fill();
          };
          drawGlow(40, 100);
          drawGlow(GameEngine.W - 40, 100);

          // Ambient sparkles
          for (const s of sparklesRef.current) {
            ctx.fillStyle = isCrystal
              ? `rgba(103, 232, 249, ${s.alpha})`
              : isForest
              ? `rgba(110, 240, 180, ${s.alpha})`
              : `rgba(255, 170, 60, ${s.alpha})`;
            ctx.beginPath();
            ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2);
            ctx.fill();
          }

          // Masonry grid during build phase
          if (game.phase === 'build') {
            ctx.strokeStyle = 'rgba(217, 119, 6, 0.15)';
            ctx.lineWidth = 1;
            const maxGridY = virtualH - 230;
            for (let gx = 20; gx < GameEngine.W; gx += 32) {
              ctx.beginPath();
              ctx.moveTo(gx, 220);
              ctx.lineTo(gx, maxGridY);
              ctx.stroke();
            }
            for (let gy = 220; gy < maxGridY; gy += 32) {
              ctx.beginPath();
              ctx.moveTo(16, gy);
              ctx.lineTo(GameEngine.W - 16, gy);
              ctx.stroke();
            }
            ctx.strokeStyle = 'rgba(217, 119, 6, 0.4)';
            ctx.strokeRect(16, 220, GameEngine.W - 32, maxGridY - 220);
          }

          // Wooden perimeter beams
          const wallBottom = virtualH - 115;
          const wallTop = game.topInset + 65;

          // Top beam
          ctx.fillStyle = '#382214';
          ctx.fillRect(0, wallTop, GameEngine.W, 8);

          // Side beams
          ctx.fillStyle = '#2E1C12';
          ctx.fillRect(0, wallTop, 14, wallBottom - wallTop);
          ctx.fillRect(GameEngine.W - 14, wallTop, 14, wallBottom - wallTop);

          // Iron rivets
          ctx.fillStyle = '#78716C';
          for (let y = wallTop + 25; y < wallBottom - 10; y += 50) {
            ctx.beginPath();
            ctx.arc(7, y, 2.5, 0, Math.PI * 2);
            ctx.fill();
            ctx.beginPath();
            ctx.arc(GameEngine.W - 7, y, 2.5, 0, Math.PI * 2);
            ctx.fill();
          }

          // Defense trench line
          const defY = game.getDefenseY();
          ctx.strokeStyle = '#DC2626';
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.moveTo(14, defY);
          ctx.lineTo(GameEngine.W - 14, defY);
          ctx.stroke();

          // Spikes
          ctx.fillStyle = '#991B1B';
          for (let sx = 20; sx < GameEngine.W - 20; sx += 20) {
            ctx.beginPath();
            ctx.moveTo(sx - 6, defY + 6);
            ctx.lineTo(sx, defY - 9);
            ctx.lineTo(sx + 6, defY + 6);
            ctx.closePath();
            ctx.fill();
          }

          // Defense warning text
          ctx.fillStyle = 'rgba(252, 165, 165, 0.75)';
          ctx.font = '10px MedievalSharp, serif';
          ctx.textAlign = 'center';
          ctx.fillText('▲ DIFESA ROCCA: SE NEMICI O SFERA OLTREPASSANO QUESTA LINEA PERDI 1 CUORE ▲', GameEngine.W / 2, defY + 16);

          // Mines (Solid Arkanoid blocks)
          for (let i = 0; i < game.mines.length; i++) {
            const m = game.mines[i];
            const hitDt = game.elapsed - (game.hits.get(`mine${i}`) || -10);
            const isHit = hitDt >= 0 && hitDt < 0.22;
            const wobbleProg = m.wobbleTime > 0 ? m.wobbleTime / 0.24 : isHit ? 1 - hitDt / 0.22 : 0;

            ctx.save();
            const cx = m.x + m.w / 2;
            const cy = m.y + m.h / 2;
            ctx.translate(cx, cy);
            if (wobbleProg > 0) {
              const sqX = 1 + Math.sin(wobbleProg * Math.PI * 3.0) * 0.08 * wobbleProg;
              const sqY = 1 - Math.sin(wobbleProg * Math.PI * 3.0) * 0.12 * wobbleProg;
              ctx.scale(sqX, sqY);
            }
            ctx.translate(-cx, -cy);

            // Outer Arkanoid chassis
            ctx.fillStyle = isCrystal ? '#0B192C' : isForest ? '#2A150A' : '#1C1917';
            ctx.beginPath();
            ctx.roundRect(m.x, m.y, m.w, m.h, 6);
            ctx.fill();

            // Inset bed
            const inset = 3.5;
            ctx.fillStyle = isCrystal ? '#1E1B4B' : isForest ? '#451A03' : '#292524';
            ctx.beginPath();
            ctx.roundRect(m.x + inset, m.y + inset, m.w - inset * 2, m.h - inset * 2, 4);
            ctx.fill();

            // Bevel highlights
            ctx.lineWidth = 1.6;
            ctx.strokeStyle = isCrystal ? 'rgba(165, 243, 252, 0.6)' : isForest ? 'rgba(253, 230, 138, 0.5)' : 'rgba(226, 232, 240, 0.5)';
            ctx.beginPath();
            ctx.moveTo(m.x + 4, m.y + 2);
            ctx.lineTo(m.x + m.w - 4, m.y + 2);
            ctx.moveTo(m.x + 2, m.y + 4);
            ctx.lineTo(m.x + 2, m.y + m.h - 4);
            ctx.stroke();

            ctx.strokeStyle = 'rgba(0, 0, 0, 0.6)';
            ctx.beginPath();
            ctx.moveTo(m.x + 4, m.y + m.h - 2);
            ctx.lineTo(m.x + m.w - 4, m.y + m.h - 2);
            ctx.moveTo(m.x + m.w - 2, m.y + 4);
            ctx.lineTo(m.x + m.w - 2, m.y + m.h - 4);
            ctx.stroke();

            // Veins / textures
            if (isCrystal) {
              ctx.strokeStyle = '#06B6D4';
              ctx.lineWidth = 2;
              ctx.beginPath();
              ctx.moveTo(m.x + 10, m.y + m.h - 8);
              ctx.lineTo(m.x + 28, m.y + 8);
              ctx.moveTo(m.x + m.w - 10, m.y + m.h - 8);
              ctx.lineTo(m.x + m.w - 28, m.y + 8);
              ctx.stroke();
            } else if (isForest) {
              ctx.strokeStyle = '#78350F';
              ctx.lineWidth = 1.8;
              ctx.beginPath();
              ctx.moveTo(m.x + 8, m.y + 13);
              ctx.lineTo(m.x + m.w - 8, m.y + 13);
              ctx.moveTo(m.x + 8, m.y + 29);
              ctx.lineTo(m.x + m.w - 8, m.y + 29);
              ctx.stroke();
            } else {
              ctx.strokeStyle = '#D97706';
              ctx.lineWidth = 2;
              ctx.beginPath();
              ctx.moveTo(m.x + 8, m.y + 16);
              ctx.lineTo(m.x + 24, m.y + 11);
              ctx.lineTo(m.x + 40, m.y + 24);
              ctx.moveTo(m.x + m.w - 40, m.y + 22);
              ctx.lineTo(m.x + m.w - 24, m.y + 11);
              ctx.lineTo(m.x + m.w - 8, m.y + 18);
              ctx.stroke();
            }

            // Sprite inside
            const mBmp = isCrystal ? imgs.crystal_mine : isForest ? imgs.tree : imgs.mine;
            if (mBmp && mBmp.complete && mBmp.naturalWidth > 0) {
              const iconH = m.h - 10;
              const iconW = iconH * (mBmp.width / mBmp.height);
              ctx.drawImage(mBmp, cx - iconW / 2, cy - iconH / 2, iconW, iconH);
            }

            // Hit flash
            if (isHit || wobbleProg > 0.05) {
              ctx.lineWidth = 3.5;
              ctx.strokeStyle = isCrystal ? '#67E8F9' : isForest ? '#FEF08A' : '#FFFFFF';
              ctx.beginPath();
              ctx.roundRect(m.x, m.y, m.w, m.h, 6);
              ctx.stroke();
            }

            ctx.restore();
          }

          // Buildings
          for (const b of game.buildings) {
            const isWall = b.type === 'wall' || b.type === 'wood_wall' || b.type === 'crystal_wall';
            if (isWall) {
              const wBmp = b.type === 'crystal_wall' ? imgs.crystal_wall : b.type === 'wood_wall' ? imgs.wood_wall : imgs.wall;
              if (wBmp && wBmp.complete && wBmp.naturalWidth > 0) {
                ctx.drawImage(wBmp, b.x, b.y, b.w, b.h);
              } else {
                ctx.fillStyle = b.type === 'crystal_wall' ? '#1E1430' : b.type === 'wood_wall' ? '#78350F' : '#383532';
                ctx.beginPath();
                ctx.roundRect(b.x, b.y, b.w, b.h, 3);
                ctx.fill();
              }
            } else {
              let bmp: HTMLImageElement | undefined;
              if (b.type === 'forge') bmp = game.forgeOpen ? imgs.forge_open : imgs.forge_closed;
              else if (b.type === 'sawmill') bmp = imgs.sawmill;
              else if (b.type === 'gemcutter') bmp = imgs.gemcutter;
              else if (b.type === 'crystal_vault') bmp = imgs.crystal_vault;
              else if (b.type === 'depot') bmp = imgs.vault;
              else if (b.type === 'laser_turret') bmp = imgs.laser_turret;
              else if (b.type === 'turret') bmp = imgs.turret;

              if (bmp && bmp.complete && bmp.naturalWidth > 0) {
                const bmpRatio = bmp.width / bmp.height;
                const boxRatio = b.w / b.h;
                let drawW = b.w;
                let drawH = b.h;
                if (bmpRatio > boxRatio) {
                  drawH = b.w / bmpRatio;
                } else {
                  drawW = b.h * bmpRatio;
                }
                const bcx = b.x + b.w / 2;
                const bcy = b.y + b.h / 2;
                ctx.drawImage(bmp, bcx - drawW / 2, bcy - drawH / 2, drawW, drawH);
              }

              // Animated details
              if (b.type === 'sawmill' && game.phase === 'wave') {
                const bladeCx = b.x + b.w * 0.4;
                const bladeCy = b.y + b.h * 0.48;
                const spin = game.elapsed * 16.0;
                ctx.strokeStyle = '#FDE047';
                ctx.lineWidth = 2;
                const gx = Math.cos(spin) * 12;
                const gy = Math.sin(spin) * 12;
                ctx.beginPath();
                ctx.moveTo(bladeCx - gx, bladeCy - gy);
                ctx.lineTo(bladeCx + gx, bladeCy + gy);
                ctx.stroke();
              } else if (b.type === 'forge' && game.forgeOpen) {
                const hearthPulse = 0.7 + 0.3 * Math.sin(game.elapsed * 5.0);
                const hearthGrad = ctx.createRadialGradient(
                  b.x + b.w * 0.5,
                  b.y + b.h * 0.56,
                  0,
                  b.x + b.w * 0.5,
                  b.y + b.h * 0.56,
                  22 * hearthPulse
                );
                hearthGrad.addColorStop(0, 'rgba(255, 138, 0, 0.4)');
                hearthGrad.addColorStop(1, 'rgba(255, 138, 0, 0)');
                ctx.fillStyle = hearthGrad;
                ctx.beginPath();
                ctx.arc(b.x + b.w * 0.5, b.y + b.h * 0.56, 22 * hearthPulse, 0, Math.PI * 2);
                ctx.fill();
              }
            }

            // Selection outline in build mode
            if (game.phase === 'build' && draggingBuildingRef.current === b) {
              ctx.lineWidth = 3;
              ctx.strokeStyle = '#FBBF24';
              ctx.strokeRect(b.x, b.y, b.w, b.h);
            }
          }

          // Mushrooms
          for (const m of game.mushrooms) {
            const mBmp = imgs.mushroom;
            if (mBmp && mBmp.complete && mBmp.naturalWidth > 0) {
              ctx.save();
              const cx = m.x + m.w / 2;
              const cy = m.y + m.h / 2;
              ctx.translate(cx, cy);
              if (m.bounceTime > 0) {
                const t = m.bounceTime / 0.35;
                ctx.scale(1 + 0.25 * Math.sin(t * Math.PI), 1 - 0.2 * Math.sin(t * Math.PI));
              }
              ctx.translate(-cx, -cy);
              ctx.drawImage(mBmp, m.x, m.y, m.w, m.h);
              ctx.restore();
            }
          }

          // Prisms
          for (const p of game.prisms) {
            const pBmp = imgs.prism;
            if (pBmp && pBmp.complete && pBmp.naturalWidth > 0) {
              ctx.drawImage(pBmp, p.x, p.y, p.w, p.h);
            }
            if (p.pulseTime > 0) {
              const alpha = p.pulseTime / 0.5;
              ctx.strokeStyle = `rgba(103, 232, 249, ${alpha})`;
              ctx.lineWidth = 3;
              ctx.beginPath();
              ctx.arc(p.x + p.w / 2, p.y + p.h / 2, p.w / 2 + (1 - alpha) * 20, 0, Math.PI * 2);
              ctx.stroke();
            }
          }

          // Turret Shots
          for (const shot of game.shots) {
            const isLaser = shot.life > 0.22;
            const alpha = shot.life / (isLaser ? 0.3 : 0.2);
            ctx.lineWidth = isLaser ? 4.5 : 3.5;
            ctx.strokeStyle = isLaser ? `rgba(6, 182, 212, ${alpha})` : `rgba(245, 158, 11, ${alpha})`;
            ctx.beginPath();
            ctx.moveTo(shot.x, shot.y);
            ctx.lineTo(shot.tx, shot.ty);
            ctx.stroke();

            ctx.lineWidth = isLaser ? 2.5 : 1.5;
            ctx.strokeStyle = `rgba(255, 255, 255, ${alpha})`;
            ctx.beginPath();
            ctx.moveTo(shot.x, shot.y);
            ctx.lineTo(shot.tx, shot.ty);
            ctx.stroke();
          }

          // Enemies
          for (const e of game.enemies) {
            if (e.hp <= 0) continue;
            let eBmp: HTMLImageElement | undefined = imgs.goblin;
            if (e.type === 'golem') eBmp = imgs.golem;
            else if (e.type === 'spectre') eBmp = imgs.spectre;
            else if (e.type === 'spider') eBmp = imgs.spider;

            ctx.save();
            const ecx = e.x + e.w / 2;
            const ecy = e.y + e.h / 2;
            ctx.translate(ecx, ecy);
            if (e.hitFlashTime > 0) {
              const wobble = Math.sin(e.hitFlashTime * 35.0) * 0.12;
              ctx.scale(1 + wobble, 1 - wobble);
            }
            ctx.translate(-ecx, -ecy);

            if (eBmp && eBmp.complete && eBmp.naturalWidth > 0) {
              ctx.drawImage(eBmp, e.x, e.y, e.w, e.h);
            } else {
              ctx.fillStyle = e.hitFlashTime > 0 ? '#FFFFFF' : '#16A34A';
              ctx.fillRect(e.x, e.y, e.w, e.h);
            }
            ctx.restore();

            // Enemy health bar if damaged
            if (e.hp < e.maxHp) {
              const barW = e.w * 0.9;
              const barH = 4;
              const bx = e.x + (e.w - barW) / 2;
              const by = e.y - 7;
              ctx.fillStyle = '#7F1D1D';
              ctx.fillRect(bx, by, barW, barH);
              ctx.fillStyle = e.type === 'golem' ? '#06B6D4' : e.type === 'spectre' ? '#A855F7' : '#22C55E';
              ctx.fillRect(bx, by, barW * (e.hp / e.maxHp), barH);
            }
          }

          // Dwarven Shield (Paddle)
          const px = game.paddle.x;
          const py = game.paddle.y + paddleRecoilYRef.current;
          const pw = game.paddleWidth;
          const ph = 24;

          const shieldBmp = imgs.shield;
          if (shieldBmp && shieldBmp.complete && shieldBmp.naturalWidth > 0) {
            ctx.drawImage(shieldBmp, px - pw / 2, py - 4, pw, ph + 8);
          } else {
            ctx.fillStyle = '#78350F';
            ctx.beginPath();
            ctx.roundRect(px - pw / 2, py, pw, ph, 6);
            ctx.fill();
          }

          if (paddleRecoilYRef.current > 0.8) {
            ctx.strokeStyle = `rgba(251, 191, 36, ${Math.min(1.0, paddleRecoilYRef.current / 4.0)})`;
            ctx.lineWidth = 2.5;
            ctx.beginPath();
            ctx.roundRect(px - pw / 2, py, pw, ph, 6);
            ctx.stroke();
          }

          // Runic Trajectory Aim
          if (game.ball.held && game.phase === 'wave') {
            const x0 = game.ball.x;
            const y0 = game.ball.y;
            const aimAngle = game.aim;
            const dx = Math.cos(aimAngle);
            let dy = Math.sin(aimAngle);
            if (dy >= 0) dy = -0.01;

            const minX = 14 + game.ball.r;
            const maxX = GameEngine.W - 14 - game.ball.r;
            const minY = game.topInset + 80 + game.ball.r;

            const tX = dx < 0 ? (minX - x0) / dx : dx > 0 ? (maxX - x0) / dx : 9999;
            const tY = (minY - y0) / dy;
            let t1 = Math.min(tX, tY);
            if (t1 < 0) t1 = 160;

            const x1 = x0 + dx * t1;
            const y1 = y0 + dy * t1;

            // Primary ray
            ctx.strokeStyle = 'rgba(245, 158, 11, 0.9)';
            ctx.lineWidth = 2.5;
            ctx.beginPath();
            ctx.moveTo(x0, y0);
            ctx.lineTo(x1, y1);
            ctx.stroke();

            // Runes on ray
            ctx.fillStyle = '#FDE68A';
            for (let f = 0.2; f < 0.95; f += 0.2) {
              ctx.beginPath();
              ctx.arc(x0 + dx * t1 * f, y0 + dy * t1 * f, 2.5, 0, Math.PI * 2);
              ctx.fill();
            }

            // Impact node
            ctx.fillStyle = '#F59E0B';
            ctx.beginPath();
            ctx.arc(x1, y1, 5, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = '#FFFFFF';
            ctx.lineWidth = 1.5;
            ctx.stroke();

            // Secondary ray reflection
            const rdx = t1 === tX ? -dx : dx;
            const rdy = t1 === tY ? -dy : dy;
            const x2 = x1 + rdx * 140;
            const y2 = y1 + rdy * 140;

            ctx.strokeStyle = 'rgba(245, 158, 11, 0.5)';
            ctx.lineWidth = 1.8;
            ctx.beginPath();
            ctx.moveTo(x1, y1);
            ctx.lineTo(x2, y2);
            ctx.stroke();

            ctx.font = '12px MedievalSharp, serif';
            ctx.fillStyle = '#FEF3C7';
            ctx.textAlign = 'center';
            ctx.fillText('MIRA E RILASCIA PER LANCIARE LA SFERA RUNICA', GameEngine.W / 2, game.paddle.y - 36);
          }

          // Ball Trails
          const drawBallTrail = (b: Ball) => {
            const tLen = b.trail.length;
            for (let ti = tLen - 1; ti >= 0; ti--) {
              const pt = b.trail[ti];
              const ratio = 1.0 - ti / (tLen + 1);
              ctx.fillStyle = b.isClone
                ? `rgba(6, 182, 212, ${0.5 * ratio})`
                : game.burn
                ? `rgba(249, 115, 22, ${0.55 * ratio})`
                : `rgba(245, 158, 11, ${0.5 * ratio})`;
              ctx.beginPath();
              ctx.arc(pt.x, pt.y, b.r * ratio * 0.95, 0, Math.PI * 2);
              ctx.fill();
            }
          };
          drawBallTrail(game.ball);
          if (game.extraBall) drawBallTrail(game.extraBall);

          // Balls
          const drawBall = (b: Ball) => {
            ctx.save();
            ctx.translate(b.x, b.y);
            ctx.scale(ballSquashXRef.current, ballSquashYRef.current);

            // Ball glow
            const glowR = Math.max(5, b.r * 1.8);
            const ballGlow = ctx.createRadialGradient(0, 0, 0, 0, 0, glowR);
            if (b.isClone) {
              ballGlow.addColorStop(0, 'rgba(6, 182, 212, 0.9)');
              ballGlow.addColorStop(1, 'rgba(8, 145, 178, 0)');
            } else if (game.burn) {
              ballGlow.addColorStop(0, 'rgba(255, 85, 0, 0.9)');
              ballGlow.addColorStop(1, 'rgba(255, 0, 0, 0)');
            } else {
              ballGlow.addColorStop(0, 'rgba(245, 158, 11, 0.85)');
              ballGlow.addColorStop(1, 'rgba(217, 119, 6, 0)');
            }
            ctx.fillStyle = ballGlow;
            ctx.beginPath();
            ctx.arc(0, 0, glowR, 0, Math.PI * 2);
            ctx.fill();

            // Ball sprite
            const orbBmp = imgs.orb;
            if (orbBmp && orbBmp.complete && orbBmp.naturalWidth > 0) {
              ctx.drawImage(orbBmp, -b.r * 1.25, -b.r * 1.25, b.r * 2.5, b.r * 2.5);
              if (b.isClone) {
                ctx.strokeStyle = '#67E8F9';
                ctx.lineWidth = 2;
                ctx.beginPath();
                ctx.arc(0, 0, b.r + 1, 0, Math.PI * 2);
                ctx.stroke();
              }
            } else {
              ctx.fillStyle = b.isClone ? '#06B6D4' : game.burn ? '#FFD700' : '#E7E5E4';
              ctx.beginPath();
              ctx.arc(0, 0, b.r, 0, Math.PI * 2);
              ctx.fill();
            }
            ctx.restore();

            // Orbiting cargo
            const totalCargo = b.ore + b.metal;
            if (totalCargo > 0) {
              for (let ci = 0; ci < totalCargo; ci++) {
                const angle = (ci * 2.0 * Math.PI) / totalCargo + game.elapsed * 2.2;
                const cpx = b.x + Math.cos(angle) * 18;
                const cpy = b.y + Math.sin(angle) * 11;
                if (ci < b.metal) {
                  ctx.fillStyle = isCrystal ? '#06B6D4' : isForest ? '#10B981' : '#FBBF24';
                  ctx.fillRect(cpx - 4, cpy - 3, 8, 6);
                  ctx.strokeStyle = '#FFFBEB';
                  ctx.lineWidth = 1;
                  ctx.strokeRect(cpx - 4, cpy - 3, 8, 6);
                } else {
                  ctx.fillStyle = isCrystal ? '#0284C7' : isForest ? '#14532D' : '#78350F';
                  ctx.beginPath();
                  ctx.arc(cpx, cpy, 3.5, 0, Math.PI * 2);
                  ctx.fill();
                  ctx.strokeStyle = '#FDE68A';
                  ctx.lineWidth = 1;
                  ctx.stroke();
                }
              }

              ctx.font = '11px MedievalSharp, serif';
              ctx.fillStyle = '#FEF3C7';
              ctx.textAlign = 'center';
              const cargoLabel = isCrystal
                ? `${b.ore} Cristalli · ${b.metal} Gemme`
                : isForest
                ? `${b.ore} Tronchi · ${b.metal} Assi`
                : `${b.ore} Min · ${b.metal} Ferro`;
              ctx.fillText(cargoLabel, b.x, b.y - 20);
            }
          };
          drawBall(game.ball);
          if (game.extraBall) drawBall(game.extraBall);

          // Particles
          for (const p of particlesRef.current) {
            const alpha = p.life / p.maxLife;
            ctx.fillStyle = p.color;
            ctx.globalAlpha = Math.max(0, Math.min(1, alpha));
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size * alpha, 0, Math.PI * 2);
            ctx.fill();
            ctx.globalAlpha = 1.0;
          }

          // Floating combat text / notifications
          for (const eff of game.effects) {
            const prog = eff.maxLife > 0 ? 1 - eff.life / eff.maxLife : 0;
            const alpha = GameEngine.clamp(eff.life, 0, 1);
            const effScale = prog < 0.2 ? 1.0 + 0.4 * (1 - prog / 0.2) : 1.0;

            ctx.save();
            ctx.translate(eff.x, eff.y);
            ctx.scale(effScale, effScale);
            ctx.font = '14px MedievalSharp, serif';
            ctx.textAlign = 'center';

            ctx.fillStyle = `rgba(0, 0, 0, ${alpha * 0.5})`;
            ctx.fillText(eff.text, 1.5, 1.5);

            ctx.fillStyle = eff.color;
            ctx.globalAlpha = alpha;
            ctx.fillText(eff.text, 0, 0);
            ctx.restore();
          }

          // Damage flash vignette
          if (damageFlashRef.current > 0) {
            ctx.fillStyle = `rgba(220, 38, 38, ${damageFlashRef.current * 0.55})`;
            ctx.fillRect(0, 0, GameEngine.W, virtualH);
          }

          ctx.restore(); // restore shake/scale
          ctx.restore(); // restore dpr
        }
      }

      animationFrameId = requestAnimationFrame(render);
    };

    animationFrameId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animationFrameId);
  }, [game, paused, spawnParticles]);

  // Touch / Mouse coordinate conversion
  const getCanvasCoords = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0]?.clientX ?? e.changedTouches[0]?.clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0]?.clientY ?? e.changedTouches[0]?.clientY : e.clientY;
    const scale = rect.width / GameEngine.W;
    return {
      x: (clientX - rect.left) / scale,
      y: (clientY - rect.top) / scale,
    };
  }, []);

  const handlePointerDown = (e: React.MouseEvent | React.TouchEvent) => {
    if (paused) return;
    const { x, y } = getCanvasCoords(e);

    // Build mode placement or dragging
    if (game.phase === 'build') {
      if (placingType) {
        if (game.build(placingType, x, y)) {
          AudioHelper.playAnvil();
          vibrate(28);
          spawnParticles(x, y, 16, '#EAB308', 0);
          setPlacingType(null);
          onStateChange();
        }
        return;
      }

      for (let i = game.buildings.length - 1; i >= 0; i--) {
        const b = game.buildings[i];
        if (x >= b.x - 8 && x <= b.x + b.w + 8 && y >= b.y - 8 && y <= b.y + b.h + 8) {
          draggingBuildingRef.current = b;
          dragOffsetRef.current = { x: x - b.x, y: y - b.y };
          return;
        }
      }
    }

    // Wave mode aiming or paddle moving
    if (game.phase === 'wave') {
      if (game.ball.held) {
        const angle = Math.atan2(y - (game.paddle.y - 20), x - game.paddle.x);
        game.aim = GameEngine.clamp(angle, -Math.PI + 0.22, -0.22);
      } else {
        game.paddle.x = GameEngine.clamp(x, game.paddleWidth / 2 + 14, GameEngine.W - game.paddleWidth / 2 - 14);
      }
    }
  };

  const handlePointerMove = (e: React.MouseEvent | React.TouchEvent) => {
    if (paused) return;
    const { x, y } = getCanvasCoords(e);

    if (game.phase === 'build' && draggingBuildingRef.current) {
      const b = draggingBuildingRef.current;
      const targetX = x - dragOffsetRef.current.x + b.w / 2;
      const targetY = y - dragOffsetRef.current.y + b.h / 2;
      game.moveBuilding(b.id, targetX, targetY);
      return;
    }

    if (game.phase === 'wave') {
      if (game.ball.held) {
        const angle = Math.atan2(y - (game.paddle.y - 20), x - game.paddle.x);
        game.aim = GameEngine.clamp(angle, -Math.PI + 0.22, -0.22);
      } else {
        game.paddle.x = GameEngine.clamp(x, game.paddleWidth / 2 + 14, GameEngine.W - game.paddleWidth / 2 - 14);
      }
    }
  };

  const handlePointerUp = (e: React.MouseEvent | React.TouchEvent) => {
    if (game.phase === 'build') {
      draggingBuildingRef.current = null;
      return;
    }

    if (game.phase === 'wave' && game.ball.held) {
      const { x, y } = getCanvasCoords(e);
      if (y < game.H - 110) {
        const angle = Math.atan2(y - (game.paddle.y - 20), x - game.paddle.x);
        game.aim = GameEngine.clamp(angle, -Math.PI + 0.22, -0.22);
        if (game.launch()) {
          AudioHelper.playAnvil();
          ballSquashXRef.current = 0.72;
          ballSquashYRef.current = 1.3;
          spawnParticles(game.paddle.x, game.paddle.y, 16, '#F59E0B', 0);
          onStateChange();
        }
      }
    }
  };

  return (
    <div ref={containerRef} className="relative w-full h-full flex justify-center items-center overflow-hidden bg-black select-none">
      <canvas
        ref={canvasRef}
        onMouseDown={handlePointerDown}
        onMouseMove={handlePointerMove}
        onMouseUp={handlePointerUp}
        onTouchStart={handlePointerDown}
        onTouchMove={handlePointerMove}
        onTouchEnd={handlePointerUp}
        className="w-full h-full max-w-[540px] max-h-[960px] aspect-[9/16] cursor-crosshair touch-none shadow-2xl border-x border-[#382214]"
      />
    </div>
  );
};
