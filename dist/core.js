export const W = 960, H = 720;
export const COST = { wall: 3, turret: 7 };
export const clamp = (n, a, b) => Math.max(a, Math.min(b, n));
export function createGame() {
  return {
    phase: 'build', wave: 0, maxWaves: 5, time: 0, duration: 42,
    iron: 6, delivered: 0, target: 14, hp: 6, maxHp: 6, kills: 0,
    capacity: 4, damage: 1, paddleWidth: 142, xp: 0, level: 1,
    burn: false, forgeOpen: true, recall: 0, selected: null,
    ball: { x: 480, y: 650, vx: 0, vy: 0, r: 9, ore: 0, metal: 0, held: true, trail: [] },
    paddle: { x: 480, y: 674 }, aim: -1.99,
    buildings: [
      { id: 'forge', type: 'forge', x: 330, y: 350, w: 130, h: 68 },
      { id: 'depot', type: 'depot', x: 680, y: 455, w: 136, h: 64 },
      { id: 'wall0', type: 'wall', x: 756, y: 275, w: 112, h: 24 },
    ],
    mines: [{ x: 145, y: 180, w: 112, h: 46 }, { x: 365, y: 156, w: 112, h: 46 }, { x: 585, y: 180, w: 112, h: 46 }],
    enemies: [], effects: [], events: [], hits: new Map(), shots: [], elapsed: 0, nextId: 1,
    stats: { mined: 0, smelted: 0, spentCargo: 0, lostCargo: 0, built: 0, recalls: 0 },
  };
}
export function event(s, text, x = 480, y = 400, color = '#e6ba76') {
  s.events.push(text); if (s.events.length > 5) s.events.shift();
  s.effects.push({ text, x, y, color, life: 1.6 });
}
export function cargo(s) { return s.ball.ore + s.ball.metal; }
export function speed(s) { return 425 - cargo(s) * 23; }
export function normalize(s) {
  const b = s.ball, m = Math.hypot(b.vx, b.vy) || 1, v = speed(s);
  b.vx = b.vx / m * v; b.vy = b.vy / m * v;
  if (Math.abs(b.vy) < 70) { b.vy = (b.vy < 0 ? -1 : 1) * 100; const n = Math.hypot(b.vx,b.vy); b.vx *= v/n; b.vy *= v/n; }
}
export function launch(s, angle = s.aim) {
  if (s.phase !== 'wave' || !s.ball.held) return false;
  s.aim = clamp(angle, -Math.PI + .18, -.18);
  s.ball.held = false; s.ball.vx = Math.cos(s.aim) * speed(s); s.ball.vy = Math.sin(s.aim) * speed(s);
  return true;
}
export function recall(s) {
  if (s.phase !== 'wave' || s.ball.held || s.recall > 0) return false;
  s.ball.held = true; s.ball.trail = []; s.recall = 12; s.stats.recalls++;
  event(s, 'Sfera richiamata · carico al sicuro', s.paddle.x, 615); return true;
}
export function startWave(s) {
  if (s.phase !== 'build') return false;
  s.wave++; s.phase = 'wave'; s.time = 0; s.recall = 0; s.ball.held = true;
  s.enemies = Array.from({length: 3 + s.wave * 2}, (_,i) => ({
    id: 'enemy' + i, type: i % 3 === 2 ? 'thief' : 'beetle',
    x: 92 + (i % 8) * 105, y: 64 - Math.floor(i / 8) * 62,
    w: 48, h: 34, hp: 2 + Math.floor(s.wave / 2), maxHp: 2 + Math.floor(s.wave / 2),
  }));
  event(s, `Ondata ${s.wave} · difendi il nucleo`, 480, 110, '#ef9a8e'); return true;
}
function settle(s) {
  // Delivering to the depot is the only way cargo becomes spendable iron.
  s.ball.held = true; s.ball.trail = []; s.shots = [];
  if (s.wave === s.maxWaves) {
    s.phase = s.delivered >= s.target ? 'won' : 'short'; return;
  }
  s.phase = 'upgrade'; s.iron += 2; s.xp += 2;
  event(s, 'Quartiere salvo · +2 ferro di recupero');
}
export function chooseUpgrade(s, type) {
  if (s.phase !== 'upgrade' || !['cargo','paddle','damage'].includes(type)) return false;
  if(type === 'cargo') s.capacity += 2;
  if(type === 'paddle') { s.paddleWidth += 24; s.hp = Math.min(s.maxHp,s.hp+1); }
  if(type === 'damage') s.damage++;
  s.level++; s.phase = 'build'; return true;
}
export function canPlace(s, rect, exclude) {
  if (![rect.x, rect.y, rect.w, rect.h].every(Number.isFinite)) return false;
  if(rect.x < 24 || rect.x+rect.w > W-24 || rect.y < 240 || rect.y+rect.h > 590) return false;
  return ![...s.buildings, ...s.mines].some(b => b.id !== exclude && rect.x < b.x+b.w+12 && rect.x+rect.w+12 > b.x && rect.y < b.y+b.h+12 && rect.y+rect.h+12 > b.y);
}
export function build(s, type, x, y) {
  if(s.phase !== 'build' || !Object.hasOwn(COST,type) || s.iron < COST[type] || s.buildings.length >= 10) return false;
  const rect = { x: Math.round((x-(type==='wall'?56:27))/16)*16, y: Math.round((y-12)/16)*16, w: type==='wall'?112:54, h: type==='wall'?24:54 };
  if(!canPlace(s,rect)) return false;
  s.iron -= COST[type]; s.buildings.push({...rect, type, id:'built'+s.nextId++, cooldown:0}); s.stats.built++;
  return true;
}
export function moveBuilding(s, id, x, y) {
  if(s.phase !== 'build') return false;
  const b=s.buildings.find(b=>b.id===id); if(!b) return false;
  const rect={...b,x:Math.round((x-b.w/2)/16)*16,y:Math.round((y-b.h/2)/16)*16};
  if(!canPlace(s,rect,id)) return false;
  b.x=rect.x;b.y=rect.y;return true;
}
export function interact(s,type) {
  const b=s.ball;
  if(type==='mine' && cargo(s)<s.capacity) {
    b.ore++;s.stats.mined++;event(s,'+1 minerale',b.x,b.y-20,'#d8a677');normalize(s);return true;
  }
  if(type==='forge' && s.forgeOpen && b.ore>0) {
    const n=b.ore;b.metal+=n;b.ore=0;s.stats.smelted+=n;
    event(s,`${n} minerale → ferro`,b.x,b.y-20,'#f8c467');return true;
  }
  if(type==='depot' && b.metal>0) {
    const n=b.metal;s.iron+=n;s.delivered+=n;b.metal=0;
    event(s,`Consegnati ${n} ferro`,b.x,b.y-25,'#92d4bd');normalize(s);return true;
  }
  return false;
}
export function hitEnemy(s,e) {
  let dmg=s.damage;
  if(s.burn && cargo(s)>0) {
    if(s.ball.metal>0){s.ball.metal--;dmg+=4;}else{s.ball.ore--;dmg+=2;}
    s.stats.spentCargo++;event(s,'Carico consumato!',e.x,e.y,'#ffc177');normalize(s);
  }
  e.hp-=dmg;
  if(e.hp<=0){s.kills++;s.xp++;event(s,'Frantumato',e.x,e.y,'#93cbb6');}
  else event(s,`−${dmg}`,e.x,e.y,'#f8e5c7');
}
export function collide(ball,rect,solid=true) {
  const cx=clamp(ball.x,rect.x,rect.x+rect.w),cy=clamp(ball.y,rect.y,rect.y+rect.h);
  let dx=ball.x-cx,dy=ball.y-cy,d=Math.hypot(dx,dy);
  if(d>=ball.r)return false;
  if(!solid)return true;
  if(d===0){
    const ds=[ball.x-rect.x,rect.x+rect.w-ball.x,ball.y-rect.y,rect.y+rect.h-ball.y],i=ds.indexOf(Math.min(...ds));
    dx=i===0?-1:i===1?1:0;dy=i===2?-1:i===3?1:0;d=1;
    if(i<2)ball.x=i===0?rect.x-ball.r:rect.x+rect.w+ball.r;else ball.y=i===2?rect.y-ball.r:rect.y+rect.h+ball.r;
  }else{ball.x+=dx/d*(ball.r-d+.1);ball.y+=dy/d*(ball.r-d+.1);}
  const nx=dx/d,ny=dy/d,dot=ball.vx*nx+ball.vy*ny;
  if(dot<0){ball.vx-=2*dot*nx;ball.vy-=2*dot*ny;}return true;
}
export function step(s,dt) {
  dt=clamp(dt,0,.05);s.elapsed+=dt;
  s.effects.forEach(e=>{e.life-=dt;e.y-=dt*18;});s.effects=s.effects.filter(e=>e.life>0);
  if(s.phase!=='wave')return;
  s.time+=dt;s.recall=Math.max(0,s.recall-dt);
  const b=s.ball;
  if(b.held){b.x=s.paddle.x;b.y=s.paddle.y-22;}
  for(const e of s.enemies){
    if(e.hp<=0)continue;
    e.y+=dt*(e.type==='thief'?8+s.wave:4+s.wave);
    if(e.y+e.h>=610){e.hp=0;s.hp--;event(s,'Breccia · −1 integrità',e.x,600,'#e9978b');}
  }
  for(const turret of s.buildings.filter(x=>x.type==='turret')){
    turret.cooldown=Math.max(0,(turret.cooldown||0)-dt);
    const enemy=s.enemies.find(e=>e.hp>0 && Math.hypot(e.x-turret.x,e.y-turret.y)<350);
    if(enemy && turret.cooldown===0){
      enemy.hp-=1;if(enemy.hp<=0){s.kills++;s.xp++;}
      s.shots.push({x:turret.x+27,y:turret.y+27,tx:enemy.x+24,ty:enemy.y+17,life:.2});turret.cooldown=1.8;
    }
  }
  s.shots.forEach(x=>x.life-=dt);s.shots=s.shots.filter(x=>x.life>0);
  if(!b.held){
    b.trail.unshift({x:b.x,y:b.y});if(b.trail.length>22)b.trail.pop();
    // Small fixed substeps keep a fast ball from passing through thin walls.
    const n=Math.max(1,Math.ceil(speed(s)*dt/(b.r*.65))),h=dt/n;
    for(let k=0;k<n;k++){
      b.x+=b.vx*h;b.y+=b.vy*h;
      if(b.x<b.r+16){b.x=b.r+16;b.vx=Math.abs(b.vx);}if(b.x>W-16-b.r){b.x=W-16-b.r;b.vx=-Math.abs(b.vx);}
      if(b.y<b.r+32){b.y=b.r+32;b.vy=Math.abs(b.vy);}
      if(b.vy>0 && b.y+b.r>=s.paddle.y && b.y-b.r<s.paddle.y+14 && Math.abs(b.x-s.paddle.x)<s.paddleWidth/2+b.r){
        const off=clamp((b.x-s.paddle.x)/(s.paddleWidth/2),-1,1),a=-Math.PI/2+off*1.12;
        b.vx=Math.cos(a)*speed(s);b.vy=Math.sin(a)*speed(s);b.y=s.paddle.y-b.r-.2;
      }
      for(const [i,mine] of s.mines.entries())if(collide(b,mine)&&s.elapsed-(s.hits.get('mine'+i)??-10)>.35){s.hits.set('mine'+i,s.elapsed);interact(s,'mine');}
      for(const facility of s.buildings){
        const solid=facility.type==='wall'||facility.type==='turret'||(facility.type==='forge'&&!s.forgeOpen);
        if(collide(b,facility,solid)&&s.elapsed-(s.hits.get(facility.id)??-10)>.3){s.hits.set(facility.id,s.elapsed);interact(s,facility.type);}
      }
      for(const e of s.enemies)if(e.hp>0&&collide(b,e)&&s.elapsed-(s.hits.get(e.id)??-10)>.22){s.hits.set(e.id,s.elapsed);hitEnemy(s,e);}
      if(b.y>H+20){
        s.hp--;s.stats.lostCargo+=cargo(s);b.ore=0;b.metal=0;b.held=true;b.trail=[];
        event(s,'Sfera persa · −1 integrità',480,600,'#ef9a8e');break;
      }
    }
  }
  s.enemies=s.enemies.filter(e=>e.hp>0);
  if(s.hp<=0){s.hp=0;s.phase='lost';return;}
  if(s.time>=s.duration){
    const escaped=s.enemies.length;
    if(escaped){s.hp=Math.max(0,s.hp-Math.ceil(escaped/3));event(s,`Assedio: −${Math.ceil(escaped/3)} integrità`,480,600,'#ef9a8e');}
    s.enemies=[];
    if(s.hp<=0)s.phase='lost';else settle(s);
  }else if(s.time>8&&s.enemies.length===0)settle(s);
}
