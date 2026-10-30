import test from 'node:test';
import assert from 'node:assert/strict';
import { createGame,interact,hitEnemy,build,moveBuilding,startWave,launch,recall,step,chooseUpgrade,collide,clamp } from '../dist/core.js';

test('Il ferro è spendibile solo quando il carico trasformato raggiunge il deposito',()=>{
  const s=createGame();interact(s,'mine');interact(s,'mine');assert.equal(s.iron,6);assert.equal(s.ball.ore,2);
  assert.equal(interact(s,'depot'),false);s.forgeOpen=false;assert.equal(interact(s,'forge'),false);
  s.forgeOpen=true;interact(s,'forge');assert.equal(s.ball.ore,0);assert.equal(s.ball.metal,2);assert.equal(s.iron,6);
  interact(s,'depot');assert.equal(s.iron,8);assert.equal(s.delivered,2);assert.equal(s.ball.metal,0);
  assert.equal(interact(s,'depot'),false);assert.equal(s.iron,8);
});
test('Capienza, consumo del carico e danno sono coerenti',()=>{
  const s=createGame();for(let i=0;i<12;i++)interact(s,'mine');assert.equal(s.ball.ore,4);
  const e={hp:30,x:0,y:0};hitEnemy(s,e);assert.equal(e.hp,29);assert.equal(s.ball.ore,4);
  s.burn=true;hitEnemy(s,e);assert.equal(e.hp,26);assert.equal(s.ball.ore,3);
  interact(s,'forge');hitEnemy(s,e);assert.equal(e.hp,21);assert.equal(s.ball.metal,2);assert.equal(s.stats.spentCargo,2);
});
test('Costruzione e movimento rispettano risorse, spazi e fase',()=>{
  const s=createGame();assert.equal(build(s,'turret',500,500),false);assert.equal(build(s,'wall',500,500),true);assert.equal(s.iron,3);
  assert.equal(build(s,'wall',500,500),false);assert.equal(build(s,'wall',-100,400),false);assert.equal(build(s,'invalid',500,450),false);
  assert.equal(moveBuilding(s,'forge',460,290),true);assert.equal(moveBuilding(s,'forge',748,480),false);
  startWave(s);assert.equal(build(s,'wall',200,500),false);assert.equal(moveBuilding(s,'forge',200,400),false);
});
test('La sponda riflette la velocità senza intrappolare la sfera',()=>{
  const b={x:160,y:295,vx:0,vy:300,r:9};assert.equal(collide(b,{x:100,y:300,w:120,h:24}),true);assert.ok(b.vy<0);assert.ok(b.y<292);
});
test('Richiamare conserva il carico e rispetta il tempo di ricarica',()=>{
  const s=createGame();startWave(s);interact(s,'mine');launch(s);assert.equal(recall(s),true);assert.equal(s.ball.ore,1);assert.equal(s.ball.held,true);
  launch(s);assert.equal(recall(s),false);s.recall=0;assert.equal(recall(s),true);
});
test('Perdere la sfera perde il carico e costa un solo punto integrità',()=>{
  const s=createGame();startWave(s);interact(s,'mine');launch(s);s.ball.x=50;s.ball.y=745;s.ball.vx=0;s.ball.vy=400;step(s,.02);
  assert.equal(s.hp,5);assert.equal(s.ball.ore,0);assert.equal(s.stats.lostCargo,1);assert.equal(s.ball.held,true);step(s,.02);assert.equal(s.hp,5);
});
test('Le ondate consentono talenti, costruzione e conclusione della spedizione',()=>{
  const s=createGame();
  for(let i=1;i<=5;i++){
    assert.equal(startWave(s),true);assert.equal(s.wave,i);s.enemies=[];s.time=9;step(s,.01);
    if(i<5){assert.equal(s.phase,'upgrade');assert.equal(chooseUpgrade(s,'cargo'),true);assert.equal(chooseUpgrade(s,'cargo'),false);}
  }
  assert.equal(s.phase,'short');assert.equal(s.capacity,12);
  const winner=createGame();winner.wave=4;winner.delivered=14;startWave(winner);winner.enemies=[];winner.time=9;step(winner,.01);assert.equal(winner.phase,'won');
});
test('Il tempo scaduto fa pagare i nemici rimasti; zero integrità termina la partita',()=>{
  const s=createGame();startWave(s);s.time=42;s.hp=1;step(s,.01);assert.equal(s.phase,'lost');assert.equal(s.hp,0);
});

test('Una spedizione completa è vincibile usando soltanto tiri, richiami e racchetta',()=>{
  const s=createGame();let iterations=0;
  while(!['won','short','lost'].includes(s.phase)&&iterations++<100000){
    if(s.phase==='build')startWave(s);
    if(s.phase==='upgrade')chooseUpgrade(s,s.hp<4?'paddle':'damage');
    const b=s.ball;
    const target=b.metal?{x:s.buildings[1].x+68,y:s.buildings[1].y+32}:b.ore?{x:s.buildings[0].x+65,y:s.buildings[0].y+34}:{x:421,y:179};
    if(b.held)launch(s,Math.atan2(target.y-b.y,target.x-b.x));
    if(b.vy>0){
      const time=(s.paddle.y-b.r-b.y)/b.vy,landing=b.x+b.vx*Math.max(0,time);
      const angle=Math.atan2(target.y-s.paddle.y,target.x-landing),offset=clamp((angle+Math.PI/2)/1.12,-1,1);
      s.paddle.x=clamp(landing-offset*s.paddleWidth/2,s.paddleWidth/2+17,960-s.paddleWidth/2-17);
    }
    if(s.recall===0&&(b.ore||b.metal))recall(s);
    step(s,1/120);
  }
  assert.equal(s.phase,'won');assert.ok(s.delivered>=s.target);assert.ok(s.stats.mined>=s.delivered);assert.ok(s.hp>0);
});
