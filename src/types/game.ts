export interface PointF {
  x: number;
  y: number;
}

export interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  r: number;
  ore: number;
  metal: number;
  held: boolean;
  isClone: boolean;
  trail: PointF[];
}

export interface Paddle {
  x: number;
  y: number;
}

export interface Building {
  id: string;
  type: 'forge' | 'sawmill' | 'gemcutter' | 'depot' | 'crystal_vault' | 'wall' | 'wood_wall' | 'crystal_wall' | 'turret' | 'laser_turret';
  x: number;
  y: number;
  w: number;
  h: number;
  cooldown: number;
}

export interface Mine {
  x: number;
  y: number;
  w: number;
  h: number;
  type: 'iron_vein' | 'ancient_tree' | 'crystal_cluster';
  wobbleTime: number;
  hits: number;
}

export interface Mushroom {
  x: number;
  y: number;
  w: number;
  h: number;
  bounceTime: number;
}

export interface Prism {
  x: number;
  y: number;
  w: number;
  h: number;
  pulseTime: number;
}

export interface Enemy {
  id: string;
  type: 'beetle' | 'thief' | 'spider' | 'treant' | 'golem' | 'spectre';
  x: number;
  y: number;
  w: number;
  h: number;
  hp: number;
  maxHp: number;
  hitFlashTime: number;
}

export interface Effect {
  text: string;
  x: number;
  y: number;
  color: string;
  life: number;
  maxLife: number;
}

export interface Shot {
  x: number;
  y: number;
  tx: number;
  ty: number;
  life: number;
}

export interface Stats {
  mined: number;
  smelted: number;
  spentCargo: number;
  lostCargo: number;
  built: number;
  recalls: number;
}

export type BiomeType = 'mine_iron' | 'forest_wood' | 'crystal_cave';
export type GamePhase = 'build' | 'wave' | 'upgrade' | 'won' | 'short' | 'lost';
export type HubModal = 'plaza' | 'forge' | 'tavern' | 'bank' | 'gate';

export interface ClanProgress {
  clanIron: number;
  clanWood: number;
  clanGems: number;
  forestUnlocked: boolean;
  crystalUnlocked: boolean;
  clanUpgradeHp: number;
  clanUpgradeCapacity: number;
  clanUpgradeDamage: number;
  clanBuff: '' | 'heart' | 'speed' | 'iron_start' | 'gem_glow';
  clanRunsWon: number;
  clanTotalMined: number;
  clanTotalWood: number;
  clanTotalGems: number;
  clanTotalKills: number;
}

export interface GameEventListener {
  onPaddleHit?: (ball: Ball, hitOffsetRatio: number) => void;
  onWallHit?: (ball: Ball) => void;
  onMushroomHit?: (m: Mushroom) => void;
  onPrismHit?: (p: Prism) => void;
  onEnemyHit?: (e: Enemy, dmg: number, killed: boolean) => void;
  onHeartLost?: () => void;
  onBuildingHit?: (b: Building) => void;
  onMineHit?: (m: Mine, index: number) => void;
  onLaserFired?: (sx: number, sy: number, tx: number, ty: number, isLaser: boolean) => void;
}
