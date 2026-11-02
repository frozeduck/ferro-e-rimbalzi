import {
  Ball,
  Building,
  Enemy,
  GameEventListener,
  GamePhase,
  Mine,
  Mushroom,
  Paddle,
  PointF,
  Prism,
  Shot,
  Stats,
  Effect,
  BiomeType,
} from '../types/game';

export class GameEngine {
  public static readonly W = 540;
  public H = 960;
  public static readonly COST_WALL = 3;
  public static readonly COST_TURRET = 7;

  // Hub & Clan Progression
  public screen: 'hub' | 'game' = 'hub';
  public selectedBiome: BiomeType = 'mine_iron';
  public clanIron = 0;
  public clanWood = 0;
  public clanGems = 0;
  public forestUnlocked = false;
  public crystalUnlocked = false;

  // Clan Upgrades
  public clanUpgradeHp = 0; // Liv 0..3: +1 Cuore iniziale
  public clanUpgradeCapacity = 0; // Liv 0..3: +1 Capienza iniziale
  public clanUpgradeDamage = 0; // Liv 0..2: +1 Danno base iniziale

  // Tavern Buff
  public clanBuff: '' | 'heart' | 'speed' | 'iron_start' | 'gem_glow' = '';

  // Historical Records
  public clanRunsWon = 0;
  public clanTotalMined = 0;
  public clanTotalWood = 0;
  public clanTotalGems = 0;
  public clanTotalKills = 0;

  // Expedition Runtime
  public phase: GamePhase = 'build';
  public wave = 0;
  public maxWaves = 5;
  public time = 0;
  public duration = 40;
  public gold = 8;
  public delivered = 0;
  public target = 14;
  public hp = 6;
  public maxHp = 6;
  public kills = 0;
  public capacity = 4;
  public damage = 1;
  public paddleWidth = 110;
  public xp = 0;
  public level = 1;
  public burn = false;
  public forgeOpen = true;
  public recall = 0;
  public aim = -1.57;
  public elapsed = 0;
  public nextId = 1;
  public topInset = 0;

  public ball: Ball = {
    x: 270,
    y: 768,
    vx: 0,
    vy: 0,
    r: 8,
    ore: 0,
    metal: 0,
    held: true,
    isClone: false,
    trail: [],
  };

  public extraBall: Ball | null = null;
  public multiballTimer = 0;
  public paddle: Paddle = { x: 270, y: 790 };
  public buildings: Building[] = [];
  public mines: Mine[] = [];
  public mushrooms: Mushroom[] = [];
  public prisms: Prism[] = [];
  public enemies: Enemy[] = [];
  public effects: Effect[] = [];
  public events: string[] = [];
  public hits: Map<string, number> = new Map();
  public shots: Shot[] = [];
  public stats: Stats = {
    mined: 0,
    smelted: 0,
    spentCargo: 0,
    lostCargo: 0,
    built: 0,
    recalls: 0,
  };

  public listener: GameEventListener | null = null;

  constructor() {
    this.initDefaultWorld();
    this.loadFromStorage();
  }

  public loadFromStorage() {
    try {
      const data = localStorage.getItem('ferro_clan_vault');
      if (data) {
        const parsed = JSON.parse(data);
        this.clanIron = Number(parsed.clanIron) || 0;
        this.clanWood = Number(parsed.clanWood) || 0;
        this.clanGems = Number(parsed.clanGems) || 0;
        this.forestUnlocked = !!parsed.forestUnlocked;
        this.crystalUnlocked = !!parsed.crystalUnlocked;
        this.clanUpgradeHp = Number(parsed.clanUpgradeHp) || 0;
        this.clanUpgradeCapacity = Number(parsed.clanUpgradeCapacity) || 0;
        this.clanUpgradeDamage = Number(parsed.clanUpgradeDamage) || 0;
        this.clanBuff = parsed.clanBuff || '';
        this.clanRunsWon = Number(parsed.clanRunsWon) || 0;
        this.clanTotalMined = Number(parsed.clanTotalMined) || 0;
        this.clanTotalWood = Number(parsed.clanTotalWood) || 0;
        this.clanTotalGems = Number(parsed.clanTotalGems) || 0;
        this.clanTotalKills = Number(parsed.clanTotalKills) || 0;
      }
    } catch {
      // Ignore localStorage errors
    }
  }

  public saveToStorage() {
    try {
      const payload = {
        clanIron: this.clanIron,
        clanWood: this.clanWood,
        clanGems: this.clanGems,
        forestUnlocked: this.forestUnlocked,
        crystalUnlocked: this.crystalUnlocked,
        clanUpgradeHp: this.clanUpgradeHp,
        clanUpgradeCapacity: this.clanUpgradeCapacity,
        clanUpgradeDamage: this.clanUpgradeDamage,
        clanBuff: this.clanBuff,
        clanRunsWon: this.clanRunsWon,
        clanTotalMined: this.clanTotalMined,
        clanTotalWood: this.clanTotalWood,
        clanTotalGems: this.clanTotalGems,
        clanTotalKills: this.clanTotalKills,
      };
      localStorage.setItem('ferro_clan_vault', JSON.stringify(payload));
    } catch {
      // Ignore
    }
  }

  public initDefaultWorld() {
    this.buildings = [];
    this.mines = [];
    this.mushrooms = [];
    this.prisms = [];

    const blockW = 114;
    const blockH = 44;
    const blockY = this.topInset + 86;
    const blockXs = [21, 149, 277, 405];

    if (this.selectedBiome === 'crystal_cave') {
      this.buildings.push({ id: 'gemcutter', type: 'gemcutter', x: 35, y: 400, w: 120, h: 80, cooldown: 0 });
      this.buildings.push({ id: 'crystal_vault', type: 'crystal_vault', x: 385, y: 400, w: 120, h: 80, cooldown: 0 });
      this.buildings.push({ id: 'wall0', type: 'crystal_wall', x: 395, y: 260, w: 110, h: 24, cooldown: 0 });

      for (const bx of blockXs) {
        this.mines.push({ x: bx, y: blockY, w: blockW, h: blockH, type: 'crystal_cluster', wobbleTime: 0, hits: 0 });
      }

      this.prisms.push({ x: 115, y: 260, w: 46, h: 46, pulseTime: 0 });
      this.prisms.push({ x: 385, y: 260, w: 46, h: 46, pulseTime: 0 });
    } else if (this.selectedBiome === 'forest_wood') {
      this.buildings.push({ id: 'sawmill', type: 'sawmill', x: 35, y: 400, w: 120, h: 80, cooldown: 0 });
      this.buildings.push({ id: 'depot', type: 'depot', x: 385, y: 400, w: 120, h: 80, cooldown: 0 });
      this.buildings.push({ id: 'wall0', type: 'wood_wall', x: 395, y: 260, w: 110, h: 24, cooldown: 0 });

      for (const bx of blockXs) {
        this.mines.push({ x: bx, y: blockY, w: blockW, h: blockH, type: 'ancient_tree', wobbleTime: 0, hits: 0 });
      }

      this.mushrooms.push({ x: 115, y: 260, w: 44, h: 44, bounceTime: 0 });
      this.mushrooms.push({ x: 385, y: 260, w: 44, h: 44, bounceTime: 0 });
    } else {
      this.buildings.push({ id: 'forge', type: 'forge', x: 35, y: 400, w: 120, h: 80, cooldown: 0 });
      this.buildings.push({ id: 'depot', type: 'depot', x: 385, y: 400, w: 120, h: 80, cooldown: 0 });
      this.buildings.push({ id: 'wall0', type: 'wall', x: 395, y: 260, w: 110, h: 24, cooldown: 0 });

      for (const bx of blockXs) {
        this.mines.push({ x: bx, y: blockY, w: blockW, h: blockH, type: 'iron_vein', wobbleTime: 0, hits: 0 });
      }
    }
  }

  public startExpedition(biome: BiomeType) {
    this.selectedBiome = biome;
    this.screen = 'game';
    this.wave = 0;
    this.time = 0;
    this.phase = 'build';
    this.delivered = 0;
    this.kills = 0;

    // Apply permanent clan upgrades
    this.maxHp = 6 + this.clanUpgradeHp;
    this.hp = this.maxHp;
    this.capacity = 4 + this.clanUpgradeCapacity;
    this.damage = 1 + this.clanUpgradeDamage;
    this.paddleWidth = 110;
    this.gold = 8;

    // Apply Tavern Buff
    if (this.clanBuff === 'heart') {
      this.maxHp++;
      this.hp = this.maxHp;
    } else if (this.clanBuff === 'speed') {
      this.paddleWidth += 25;
    } else if (this.clanBuff === 'iron_start') {
      this.gold += 4;
    } else if (this.clanBuff === 'gem_glow') {
      this.gold += 2;
    }
    this.clanBuff = ''; // Consumed
    this.saveToStorage();

    this.extraBall = null;
    this.multiballTimer = 0;
    this.ball = {
      x: this.paddle.x,
      y: this.paddle.y - 20,
      vx: 0,
      vy: 0,
      r: 8,
      ore: 0,
      metal: 0,
      held: true,
      isClone: false,
      trail: [],
    };
    this.enemies = [];
    this.shots = [];
    this.effects = [];
    this.stats = {
      mined: 0,
      smelted: 0,
      spentCargo: 0,
      lostCargo: 0,
      built: 0,
      recalls: 0,
    };
    this.initDefaultWorld();
  }

  public returnToHub() {
    if (this.phase === 'won') {
      this.clanRunsWon++;
    }
    this.clanTotalMined += this.stats.mined;
    this.clanTotalKills += this.kills;

    if (this.selectedBiome === 'crystal_cave') {
      this.clanGems += this.delivered;
      this.clanTotalGems += this.delivered;
    } else if (this.selectedBiome === 'forest_wood') {
      this.clanWood += this.delivered;
      this.clanTotalWood += this.delivered;
    } else {
      this.clanIron += this.delivered;
    }
    this.extraBall = null;
    this.screen = 'hub';
    this.phase = 'build';
    this.ball.held = true;
    this.saveToStorage();
  }

  public buyClanUpgrade(type: 'hp' | 'capacity' | 'damage'): boolean {
    if (type === 'hp') {
      if (this.clanUpgradeHp >= 3) return false;
      const cost = 10 + this.clanUpgradeHp * 8;
      if (this.clanIron < cost) return false;
      this.clanIron -= cost;
      this.clanUpgradeHp++;
      this.saveToStorage();
      return true;
    }
    if (type === 'capacity') {
      if (this.clanUpgradeCapacity >= 3) return false;
      const costFe = 8 + this.clanUpgradeCapacity * 6;
      const costWood = 6 + this.clanUpgradeCapacity * 6;
      if (this.clanIron < costFe || this.clanWood < costWood) return false;
      this.clanIron -= costFe;
      this.clanWood -= costWood;
      this.clanUpgradeCapacity++;
      this.saveToStorage();
      return true;
    }
    if (type === 'damage') {
      if (this.clanUpgradeDamage >= 2) return false;
      const costFe = 15 + this.clanUpgradeDamage * 10;
      const costWood = 10 + this.clanUpgradeDamage * 10;
      if (this.clanIron < costFe || this.clanWood < costWood) return false;
      this.clanIron -= costFe;
      this.clanWood -= costWood;
      this.clanUpgradeDamage++;
      this.saveToStorage();
      return true;
    }
    return false;
  }

  public buyTavernBuff(type: 'heart' | 'speed' | 'iron_start' | 'gem_glow'): boolean {
    if (type === 'heart') {
      if (this.clanIron < 3) return false;
      this.clanIron -= 3;
      this.clanBuff = 'heart';
      this.saveToStorage();
      return true;
    }
    if (type === 'speed') {
      if (this.clanWood < 3) return false;
      this.clanWood -= 3;
      this.clanBuff = 'speed';
      this.saveToStorage();
      return true;
    }
    if (type === 'iron_start') {
      if (this.clanIron < 4) return false;
      this.clanIron -= 4;
      this.clanBuff = 'iron_start';
      this.saveToStorage();
      return true;
    }
    if (type === 'gem_glow') {
      if (this.clanGems < 2) return false;
      this.clanGems -= 2;
      this.clanBuff = 'gem_glow';
      this.saveToStorage();
      return true;
    }
    return false;
  }

  public unlockForest(): boolean {
    if (this.clanIron >= 14 && !this.forestUnlocked) {
      this.clanIron -= 14;
      this.forestUnlocked = true;
      this.saveToStorage();
      return true;
    }
    return false;
  }

  public unlockCrystal(): boolean {
    if (this.clanIron >= 18 && this.clanWood >= 14 && !this.crystalUnlocked) {
      this.clanIron -= 18;
      this.clanWood -= 14;
      this.crystalUnlocked = true;
      this.saveToStorage();
      return true;
    }
    return false;
  }

  public getDefenseY(): number {
    return this.H - 145;
  }

  public setTopInset(inset: number) {
    if (inset < 0) inset = 0;
    this.topInset = inset;
    this.initDefaultWorld();
  }

  public setVirtualHeight(newH: number) {
    if (newH < 800) newH = 800;
    this.H = newH;
    this.paddle.y = this.H - 170;
    if (this.ball.held) {
      this.ball.y = this.paddle.y - 20;
    }
  }

  public static clamp(n: number, a: number, b: number): number {
    return Math.max(a, Math.min(b, n));
  }

  public event(text: string, x = 270, y = 500, color = '#E6BA76') {
    this.events.push(text);
    if (this.events.length > 5) this.events.shift();
    this.effects.push({ text, x, y, color, life: 1.5, maxLife: 1.5 });
  }

  public cargo(targetBall: Ball = this.ball): number {
    return targetBall.ore + targetBall.metal;
  }

  public speed(targetBall: Ball = this.ball): number {
    return 480 - this.cargo(targetBall) * 22;
  }

  public normalizeBall(targetBall: Ball | null) {
    if (!targetBall) return;
    let m = Math.hypot(targetBall.vx, targetBall.vy);
    if (m === 0) m = 1;
    const v = this.speed(targetBall);
    targetBall.vx = (targetBall.vx / m) * v;
    targetBall.vy = (targetBall.vy / m) * v;
    if (Math.abs(targetBall.vy) < 80) {
      targetBall.vy = (targetBall.vy < 0 ? -1 : 1) * 110;
      const n = Math.hypot(targetBall.vx, targetBall.vy);
      targetBall.vx *= v / n;
      targetBall.vy *= v / n;
    }
  }

  public normalize() {
    this.normalizeBall(this.ball);
  }

  public launch(angle?: number): boolean {
    if (this.phase !== 'wave' || !this.ball.held) return false;
    const launchAngle = angle !== undefined ? angle : this.aim;
    this.aim = GameEngine.clamp(launchAngle, -Math.PI + 0.22, -0.22);
    this.ball.held = false;
    const spd = this.speed(this.ball);
    this.ball.vx = Math.cos(this.aim) * spd;
    this.ball.vy = Math.sin(this.aim) * spd;
    return true;
  }

  public recallBall(): boolean {
    if (this.phase !== 'wave' || this.ball.held || this.recall > 0) return false;
    this.ball.held = true;
    this.ball.trail = [];
    this.recall = 12;
    this.stats.recalls++;
    this.event('Sfera richiamata · carico al sicuro', this.paddle.x, 740, '#E6BA76');
    return true;
  }

  public startWave(): boolean {
    if (this.phase !== 'build') return false;
    this.wave++;
    this.phase = 'wave';
    this.time = 0;
    this.recall = 0;
    this.ball.held = true;
    this.enemies = [];
    const isForest = this.selectedBiome === 'forest_wood';
    const isCrystal = this.selectedBiome === 'crystal_cave';
    const count = 3 + this.wave * 2;

    for (let i = 0; i < count; i++) {
      let type: Enemy['type'];
      let w: number;
      let h: number;
      let hp: number;

      if (isCrystal) {
        type = i % 3 === 0 ? 'golem' : 'spectre';
        w = i % 3 === 0 ? 48 : 34;
        h = i % 3 === 0 ? 44 : 30;
        hp = i % 3 === 0 ? 5 + this.wave : 2 + Math.floor(this.wave / 2);
      } else if (isForest) {
        type = i % 3 === 0 ? 'treant' : 'spider';
        w = i % 3 === 0 ? 46 : 36;
        h = i % 3 === 0 ? 42 : 32;
        hp = i % 3 === 0 ? 4 + this.wave : 2 + Math.floor(this.wave / 2);
      } else {
        type = i % 3 === 2 ? 'thief' : 'beetle';
        w = 42;
        h = 32;
        hp = 2 + Math.floor(this.wave / 2);
      }

      this.enemies.push({
        id: `enemy${i}`,
        type,
        w,
        h,
        hp,
        maxHp: hp,
        hitFlashTime: 0,
        x: 45 + (i % 5) * 94,
        y: this.topInset + 50 - Math.floor(i / 5) * 55,
      });
    }

    this.event(`Ondata ${this.wave} · difendi il nucleo`, 270, this.topInset + 160, '#EF9A8E');
    return true;
  }

  private settle() {
    this.ball.held = true;
    this.ball.trail = [];
    this.extraBall = null;
    this.shots = [];
    if (this.wave === this.maxWaves) {
      this.phase = this.delivered >= this.target ? 'won' : 'short';
      return;
    }
    this.phase = 'upgrade';
    this.gold += 4;
    this.xp += 2;
    this.event('Ondata respinta! +4 Oro di ricompensa', 270, 480, '#FBBF24');
  }

  public chooseUpgrade(type: 'cargo' | 'paddle' | 'damage'): boolean {
    if (this.phase !== 'upgrade') return false;
    if (type === 'cargo') {
      this.capacity += 2;
    } else if (type === 'paddle') {
      this.paddleWidth += 20;
      this.hp = Math.min(this.maxHp, this.hp + 1);
    } else if (type === 'damage') {
      this.damage++;
    } else {
      return false;
    }
    this.level++;
    this.phase = 'build';
    return true;
  }

  public canPlace(rx: number, ry: number, rw: number, rh: number, excludeId: string | null): boolean {
    if (isNaN(rx) || isNaN(ry) || isNaN(rw) || isNaN(rh)) return false;
    if (rx < 16 || rx + rw > GameEngine.W - 16 || ry < this.topInset + 220 || ry + rh > this.H - 230) return false;
    for (const b of this.buildings) {
      if (b.id === excludeId) continue;
      if (rx < b.x + b.w + 10 && rx + rw + 10 > b.x && ry < b.y + b.h + 10 && ry + rh + 10 > b.y) {
        return false;
      }
    }
    for (const m of this.mines) {
      if (rx < m.x + m.w + 10 && rx + rw + 10 > m.x && ry < m.y + m.h + 10 && ry + rh + 10 > m.y) {
        return false;
      }
    }
    return true;
  }

  public build(type: Building['type'], x: number, y: number): boolean {
    if (this.phase !== 'build') return false;
    const isWall = type === 'wall' || type === 'wood_wall' || type === 'crystal_wall';
    const isForest = this.selectedBiome === 'forest_wood';
    const isCrystal = this.selectedBiome === 'crystal_cave';
    const cost = isWall ? GameEngine.COST_WALL : type === 'turret' || type === 'laser_turret' ? (isCrystal ? 7 : isForest ? 6 : GameEngine.COST_TURRET) : -1;

    if (cost < 0 || this.gold < cost || this.buildings.length >= 12) return false;

    const rw = isWall ? 120 : 50;
    const rh = isWall ? 22 : 50;
    const rx = Math.round((x - rw / 2) / 16) * 16;
    const ry = Math.round((y - rh / 2) / 16) * 16;

    if (!this.canPlace(rx, ry, rw, rh, null)) return false;
    this.gold -= cost;
    this.buildings.push({
      id: `built${this.nextId++}`,
      type,
      x: rx,
      y: ry,
      w: rw,
      h: rh,
      cooldown: 0,
    });
    this.stats.built++;
    return true;
  }

  public moveBuilding(id: string, x: number, y: number): boolean {
    if (this.phase !== 'build') return false;
    const b = this.buildings.find((item) => item.id === id);
    if (!b) return false;
    const rx = Math.round((x - b.w / 2) / 16) * 16;
    const ry = Math.round((y - b.h / 2) / 16) * 16;
    if (!this.canPlace(rx, ry, b.w, b.h, id)) return false;
    b.x = rx;
    b.y = ry;
    return true;
  }

  public interact(type: string): boolean {
    const isForest = this.selectedBiome === 'forest_wood';
    const isCrystal = this.selectedBiome === 'crystal_cave';

    if (type === 'mine' && this.cargo() < this.capacity) {
      this.ball.ore++;
      this.stats.mined++;
      if (isCrystal) {
        this.event('+1 Cristallo grezzo', this.ball.x, this.ball.y - 18, '#67E8F9');
      } else if (isForest) {
        this.event('+1 Tronco', this.ball.x, this.ball.y - 18, '#86EFAC');
      } else {
        this.event('+1 minerale', this.ball.x, this.ball.y - 18, '#D8A677');
      }
      this.normalize();
      return true;
    }

    if ((type === 'forge' || type === 'sawmill' || type === 'gemcutter') && this.forgeOpen && this.ball.ore > 0) {
      const n = this.ball.ore;
      this.ball.metal += n;
      this.ball.ore = 0;
      this.stats.smelted += n;
      if (isCrystal) {
        this.event(`${n} grezzi → gemme pure!`, this.ball.x, this.ball.y - 18, '#67E8F9');
      } else if (isForest) {
        this.event(`${n} tronchi → assi!`, this.ball.x, this.ball.y - 18, '#FCD34D');
      } else {
        this.event(`${n} minerale → ferro!`, this.ball.x, this.ball.y - 18, '#F8C467');
      }
      return true;
    }

    if ((type === 'depot' || type === 'crystal_vault' || type === 'vault') && this.ball.metal > 0) {
      const n = this.ball.metal;
      this.delivered += n;
      this.ball.metal = 0;
      if (isCrystal) {
        this.event(`Consegnate +${n} Gemme`, this.ball.x, this.ball.y - 22, '#67E8F9');
      } else if (isForest) {
        this.event(`Consegnati +${n} Legno`, this.ball.x, this.ball.y - 22, '#6EE7B7');
      } else {
        this.event(`Consegnati +${n} Fe`, this.ball.x, this.ball.y - 22, '#92D4BD');
      }
      this.normalize();
      return true;
    }
    return false;
  }

  public static getGoldDrop(type: Enemy['type']): number {
    if (type === 'golem' || type === 'treant') return 3;
    if (type === 'thief' || type === 'spectre') return 2;
    return 1;
  }

  public hitEnemy(e: Enemy) {
    let dmg = this.damage;
    if (this.burn && this.cargo() > 0) {
      if (this.ball.metal > 0) {
        this.ball.metal--;
        dmg += 4;
      } else {
        this.ball.ore--;
        dmg += 2;
      }
      this.stats.spentCargo++;
      this.event('Carico bruciato!', e.x, e.y, '#FFC177');
      this.normalize();
    }
    e.hp -= dmg;
    e.hitFlashTime = 0.12;
    const killed = e.hp <= 0;
    if (killed) {
      this.kills++;
      this.xp++;
      const drop = GameEngine.getGoldDrop(e.type);
      this.gold += drop;
      this.event(`+${drop} Oro`, e.x, e.y, '#FBBF24');
    } else {
      this.event(`−${dmg}`, e.x, e.y, '#F8E5C7');
    }
    this.listener?.onEnemyHit?.(e, dmg, killed);
  }

  public static collide(ball: Ball, rx: number, ry: number, rw: number, rh: number, solid: boolean): boolean {
    const cx = GameEngine.clamp(ball.x, rx, rx + rw);
    const cy = GameEngine.clamp(ball.y, ry, ry + rh);
    let dx = ball.x - cx;
    let dy = ball.y - cy;
    let d = Math.hypot(dx, dy);
    if (d >= ball.r) return false;
    if (!solid) return true;

    if (d === 0) {
      const l = ball.x - rx;
      const r = rx + rw - ball.x;
      const t = ball.y - ry;
      const bot = ry + rh - ball.y;
      const min = Math.min(Math.min(l, r), Math.min(t, bot));
      const i = min === l ? 0 : min === r ? 1 : min === t ? 2 : 3;
      dx = i === 0 ? -1 : i === 1 ? 1 : 0;
      dy = i === 2 ? -1 : i === 3 ? 1 : 0;
      d = 1;
      if (i < 2) ball.x = i === 0 ? rx - ball.r : rx + rw + ball.r;
      else ball.y = i === 2 ? ry - ball.r : ry + rh + ball.r;
    } else {
      ball.x += (dx / d) * (ball.r - d + 0.1);
      ball.y += (dy / d) * (ball.r - d + 0.1);
    }
    const nx = dx / d;
    const ny = dy / d;
    const dot = ball.vx * nx + ball.vy * ny;
    if (dot < 0) {
      ball.vx -= 2 * dot * nx;
      ball.vy -= 2 * dot * ny;
    }
    return true;
  }

  private stepSingleBall(b: Ball, dt: number): boolean {
    if (b.held) return false;
    b.trail.unshift({ x: b.x, y: b.y });
    if (b.trail.length > 20) b.trail.pop();

    const n = Math.max(1, Math.ceil((this.speed(b) * dt) / (b.r * 0.65)));
    const h = dt / n;

    for (let k = 0; k < n; k++) {
      b.x += b.vx * h;
      b.y += b.vy * h;

      // Left, right, top bounds
      if (b.x < b.r + 14) {
        b.x = b.r + 14;
        b.vx = Math.abs(b.vx);
        this.listener?.onWallHit?.(b);
      }
      if (b.x > GameEngine.W - 14 - b.r) {
        b.x = GameEngine.W - 14 - b.r;
        b.vx = -Math.abs(b.vx);
        this.listener?.onWallHit?.(b);
      }
      if (b.y < b.r + this.topInset + 80) {
        b.y = b.r + this.topInset + 80;
        b.vy = Math.abs(b.vy);
        this.listener?.onWallHit?.(b);
      }

      // Paddle deflection
      if (
        b.vy > 0 &&
        b.y + b.r >= this.paddle.y &&
        b.y - b.r < this.paddle.y + 16 &&
        Math.abs(b.x - this.paddle.x) < this.paddleWidth / 2 + b.r
      ) {
        const off = GameEngine.clamp((b.x - this.paddle.x) / (this.paddleWidth / 2), -1, 1);
        const a = -Math.PI / 2 + off * 1.15;
        b.vx = Math.cos(a) * this.speed(b);
        b.vy = Math.sin(a) * this.speed(b);
        b.y = this.paddle.y - b.r - 0.2;
        this.listener?.onPaddleHit?.(b, off);
      }

      // Mushrooms (Bouncers)
      for (const m of this.mushrooms) {
        if (
          GameEngine.collide(b, m.x, m.y, m.w, m.h, true) &&
          this.elapsed - (this.hits.get(`mush${m.x}_${m.y}`) || -10) > 0.18
        ) {
          this.hits.set(`mush${m.x}_${m.y}`, this.elapsed);
          m.bounceTime = 0.35;
          b.vx *= 1.1;
          b.vy *= 1.1;
          this.normalizeBall(b);
          this.event('+RIMBALZO+', m.x + m.w / 2, m.y - 10, '#F472B6');
          this.listener?.onMushroomHit?.(m);
        }
      }

      // Prisms (Multiball)
      for (const p of this.prisms) {
        if (
          GameEngine.collide(b, p.x, p.y, p.w, p.h, true) &&
          this.elapsed - (this.hits.get(`prism${p.x}_${p.y}`) || -10) > 0.35
        ) {
          this.hits.set(`prism${p.x}_${p.y}`, this.elapsed);
          p.pulseTime = 0.5;
          this.event('Risonanza Cristallina!', p.x + p.w / 2, p.y - 10, '#67E8F9');
          if (this.extraBall === null) {
            this.extraBall = {
              x: p.x + p.w / 2,
              y: p.y + p.h + 12,
              vx: -b.vx,
              vy: Math.abs(b.vy) > 80 ? Math.abs(b.vy) : 260,
              r: 8,
              ore: 0,
              metal: 0,
              held: false,
              isClone: true,
              trail: [],
            };
            this.event('MULTIBALL! Sdoppiamento Sfera!', p.x + p.w / 2, p.y + 20, '#67E8F9');
          }
          this.listener?.onPrismHit?.(p);
        }
      }

      // Mines (Solid Arkanoid blocks)
      for (let i = 0; i < this.mines.length; i++) {
        const m = this.mines[i];
        if (GameEngine.collide(b, m.x, m.y, m.w, m.h, true)) {
          if (this.elapsed - (this.hits.get(`mine${i}`) || -10) > 0.18) {
            this.hits.set(`mine${i}`, this.elapsed);
            m.wobbleTime = 0.24;
            m.hits++;
            if (this.cargo(b) < this.capacity) {
              this.interact('mine');
            } else {
              this.event('Stiva Piena! Forziere', m.x + m.w / 2, m.y + m.h + 10, '#FCD34D');
            }
            this.listener?.onMineHit?.(m, i);
          }
        }
      }

      // Buildings
      for (const facility of this.buildings) {
        const solid =
          facility.type === 'wall' ||
          facility.type === 'wood_wall' ||
          facility.type === 'crystal_wall' ||
          facility.type === 'turret' ||
          facility.type === 'laser_turret' ||
          ((facility.type === 'forge' || facility.type === 'sawmill' || facility.type === 'gemcutter') && !this.forgeOpen);

        if (
          GameEngine.collide(b, facility.x, facility.y, facility.w, facility.h, solid) &&
          this.elapsed - (this.hits.get(facility.id) || -10) > 0.28
        ) {
          this.hits.set(facility.id, this.elapsed);
          this.interact(facility.type);
          this.listener?.onBuildingHit?.(facility);
        }
      }

      // Enemies
      for (const e of this.enemies) {
        if (e.hp > 0 && GameEngine.collide(b, e.x, e.y, e.w, e.h, false) && this.elapsed - (this.hits.get(e.id) || -10) > 0.2) {
          this.hits.set(e.id, this.elapsed);
          this.hitEnemy(e);
        }
      }

      // Sphere fell past defense trench line
      if (b.y > this.getDefenseY() + 15) {
        return true;
      }
    }
    return false;
  }

  public step(dt: number) {
    dt = GameEngine.clamp(dt, 0, 0.05);
    this.elapsed += dt;

    for (let i = this.effects.length - 1; i >= 0; i--) {
      const eff = this.effects[i];
      eff.life -= dt;
      eff.y -= dt * 20;
      if (eff.life <= 0) this.effects.splice(i, 1);
    }

    if (this.phase !== 'wave') return;

    this.time += dt;
    this.recall = Math.max(0, this.recall - dt);

    if (this.ball.held) {
      this.ball.x = this.paddle.x;
      this.ball.y = this.paddle.y - 20;
    }

    for (const m of this.mushrooms) {
      m.bounceTime = Math.max(0, m.bounceTime - dt);
    }
    for (const p of this.prisms) {
      p.pulseTime = Math.max(0, p.pulseTime - dt);
    }
    for (const m of this.mines) {
      m.wobbleTime = Math.max(0, m.wobbleTime - dt);
    }

    // Enemies advance
    for (const e of this.enemies) {
      e.hitFlashTime = Math.max(0, e.hitFlashTime - dt);
      if (e.hp <= 0) continue;
      let speedY: number;
      if (e.type === 'spectre') {
        speedY = 19 + this.wave * 1.5;
        e.x += Math.cos(this.elapsed * 4.5 + (e.id.charCodeAt(0) % 10)) * 45 * dt;
        e.x = GameEngine.clamp(e.x, 20, GameEngine.W - 20 - e.w);
      } else if (e.type === 'golem') {
        speedY = 9 + this.wave * 0.7;
      } else if (e.type === 'spider') {
        speedY = 18 + this.wave * 1.5;
        e.x += Math.sin(this.elapsed * 5 + (e.id.charCodeAt(0) % 10)) * 45 * dt;
        e.x = GameEngine.clamp(e.x, 20, GameEngine.W - 20 - e.w);
      } else if (e.type === 'treant') {
        speedY = 10 + this.wave * 0.8;
      } else if (e.type === 'thief') {
        speedY = 16 + this.wave * 1.2;
      } else {
        speedY = 13 + this.wave * 1.0;
      }
      e.y += dt * speedY;

      // Breach defense line
      if (e.y + e.h >= this.getDefenseY()) {
        e.hp = 0;
        this.hp--;
        this.event('Breccia Nemica! −1 Cuore', e.x, this.getDefenseY() - 15, '#EF4444');
        this.listener?.onHeartLost?.();
      }
    }

    // Turrets & Laser Turrets firing
    for (const turret of this.buildings) {
      const isLaser = turret.type === 'laser_turret';
      const isNormal = turret.type === 'turret';
      if (!isLaser && !isNormal) continue;
      turret.cooldown = Math.max(0, turret.cooldown - dt);
      let targetEnemy: Enemy | null = null;
      const maxRange = isLaser ? 320.0 : 260.0;
      for (const e of this.enemies) {
        if (e.hp > 0 && Math.hypot(e.x - turret.x, e.y - turret.y) < maxRange) {
          targetEnemy = e;
          break;
        }
      }

      if (targetEnemy && turret.cooldown === 0) {
        const dmg = isLaser ? 2 : 1;
        targetEnemy.hp -= dmg;
        targetEnemy.hitFlashTime = 0.12;
        const killed = targetEnemy.hp <= 0;
        if (killed) {
          this.kills++;
          this.xp++;
          const drop = GameEngine.getGoldDrop(targetEnemy.type);
          this.gold += drop;
          this.event(`+${drop} Oro`, targetEnemy.x, targetEnemy.y, '#FBBF24');
        }
        this.shots.push({
          x: turret.x + 25,
          y: turret.y + 25,
          tx: targetEnemy.x + 21,
          ty: targetEnemy.y + 16,
          life: isLaser ? 0.3 : 0.2,
        });
        turret.cooldown = isLaser ? 1.3 : 1.6;
        this.listener?.onLaserFired?.(turret.x + 25, turret.y + 25, targetEnemy.x + 21, targetEnemy.y + 16, isLaser);
        this.listener?.onEnemyHit?.(targetEnemy, dmg, killed);
      }
    }

    // Shot lifetime
    for (let i = this.shots.length - 1; i >= 0; i--) {
      this.shots[i].life -= dt;
      if (this.shots[i].life <= 0) this.shots.splice(i, 1);
    }

    // Extra ball step
    if (this.extraBall) {
      const extraDied = this.stepSingleBall(this.extraBall, dt);
      if (extraDied) {
        this.extraBall = null;
        this.event('Frammento dissipato', this.paddle.x, this.getDefenseY() - 15, '#67E8F9');
      }
    }

    // Main ball step
    if (!this.ball.held) {
      const ballDied = this.stepSingleBall(this.ball, dt);
      if (ballDied) {
        if (this.extraBall) {
          this.ball = this.extraBall;
          this.ball.isClone = false;
          this.extraBall = null;
          this.event('Sfera principale salvata dal clone!', this.paddle.x, this.getDefenseY() - 15, '#67E8F9');
        } else {
          this.hp--;
          this.stats.lostCargo += this.cargo();
          this.ball.ore = 0;
          this.ball.metal = 0;
          this.ball.held = true;
          this.ball.trail = [];
          this.event('Sfera Caduta! −1 Cuore', this.paddle.x, this.getDefenseY() - 15, '#F97316');
          this.listener?.onHeartLost?.();
        }
      }
    }

    this.enemies = this.enemies.filter((e) => e.hp > 0);

    if (this.hp <= 0) {
      this.hp = 0;
      this.phase = 'lost';
      return;
    }

    if (this.time >= this.duration) {
      const escaped = this.enemies.length;
      if (escaped > 0) {
        const penalty = Math.ceil(escaped / 3.0);
        this.hp = Math.max(0, this.hp - penalty);
        this.event(`Assalto nemico: Breccia! −${penalty} Cuori`, 270, this.getDefenseY() - 15, '#EF4444');
        this.listener?.onHeartLost?.();
      }
      this.enemies = [];
      if (this.hp <= 0) {
        this.phase = 'lost';
      } else {
        this.settle();
      }
    } else if (this.time > 6 && this.enemies.length === 0) {
      this.settle();
    }
  }
}
