import { W,H,COST,clamp,createGame,cargo,speed,startWave,launch,recall,step,build,moveBuilding,canPlace,chooseUpgrade } from './core.js';
const $=id=>document.getElementById(id),canvas=$('board'),ctx=canvas.getContext('2d');
let game=createGame(),paused=false,placing=null,drag=null,pointer={x:350,y:100},last=performance.now(),toastUntil=0,lastEvent=0,uiCache='',sound=false,audio;
const keys=new Set(),reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
const colors={ore:'#a87d60',metal:'#efc46e',forge:'#e9ae64',depot:'#9fbec1',wall:'#8b9baf',turret:'#b9c594'};
const view={width:960,height:720,scale:1,x:0,y:0,dpr:1};
function fit(){const r=canvas.parentElement.getBoundingClientRect();view.width=r.width;view.height=r.height;view.dpr=Math.min(devicePixelRatio||1,2);view.scale=Math.min(r.width/W,r.height/H);view.x=(r.width-W*view.scale)/2;view.y=(r.height-H*view.scale)/2;canvas.width=Math.round(r.width*view.dpr);canvas.height=Math.round(r.height*view.dpr);}
new ResizeObserver(fit).observe(canvas.parentElement);fit();
function toast(text){$('toast').textContent=text;$('toast').classList.add('visible');toastUntil=performance.now()+2600;}
function tone(hz=440){if(!sound)return;try{audio??=new(window.AudioContext||window.webkitAudioContext)();audio.resume();const o=audio.createOscillator(),g=audio.createGain();o.type='sine';o.frequency.setValueAtTime(hz,audio.currentTime);o.frequency.exponentialRampToValueAtTime(hz*.65,audio.currentTime+.1);g.gain.setValueAtTime(.055,audio.currentTime);g.gain.exponentialRampToValueAtTime(.001,audio.currentTime+.16);o.connect(g);g.connect(audio.destination);o.start();o.stop(audio.currentTime+.17);}catch{sound=false;}}
function newGame(){game=createGame();paused=false;placing=null;drag=null;keys.clear();lastEvent=0;uiCache='';document.querySelectorAll('dialog[open]').forEach(d=>d.close());updateUI();draw();}
function act(){
  if(game.phase==='build'){placing=null;startWave(game);toast('Mira nell’arena e clicca per lanciare.');}
  else if(game.phase==='wave'){if(paused)togglePause();else if(game.ball.held)launch(game);else recall(game);}
  else if(game.phase==='upgrade')$('upgrade').showModal();
  else newGame();
  updateUI();canvas.focus({preventScroll:true});
}
function toggleForge(){game.forgeOpen=!game.forgeOpen;toast(game.forgeOpen?'Fonderia aperta: il minerale diventa ferro.':'Fonderia chiusa: il tetto diventa una sponda.');updateUI();}
function toggleBurn(){game.burn=!game.burn;toast(game.burn?'Carico armato: ogni colpo consuma 1 risorsa.':'Consegna: il carico resta al sicuro negli attacchi.');updateUI();}
function closePause(){paused=false;keys.clear();$('pause-menu').close();updateUI();canvas.focus({preventScroll:true});}
function togglePause(){if($('pause-menu').open){closePause();return;}paused=true;keys.clear();updateUI();$('pause-menu').showModal();}
function point(e){const r=canvas.getBoundingClientRect();return{x:(e.clientX-r.left-view.x)/view.scale,y:(e.clientY-r.top-view.y)/view.scale};}
function aimed(p){game.aim=clamp(Math.atan2(p.y-(game.paddle.y-22),p.x-game.paddle.x),-Math.PI+.18,-.18);}
canvas.addEventListener('pointerdown',e=>{
  if(document.querySelector('dialog[open]'))return;
  e.preventDefault();canvas.focus({preventScroll:true});pointer=point(e);canvas.setPointerCapture(e.pointerId);
  if(game.phase==='build'){
    if(placing){if(build(game,placing,pointer.x,pointer.y)){tone(330);toast('Costruzione completata. Puoi trascinarla.');placing=null;}else toast('Serve spazio libero fra le linee tratteggiate.');}
    else {const b=[...game.buildings].reverse().find(b=>pointer.x>=b.x-10&&pointer.x<=b.x+b.w+10&&pointer.y>=b.y-10&&pointer.y<=b.y+b.h+10);if(b)drag={id:b.id,x:pointer.x,y:pointer.y};}
  }else if(game.phase==='wave'&&!paused){if(game.ball.held){aimed(pointer);launch(game);tone(520);}else game.paddle.x=clamp(pointer.x,game.paddleWidth/2+17,W-game.paddleWidth/2-17);}
  updateUI();
});
canvas.addEventListener('pointermove',e=>{pointer=point(e);if(drag){drag.x=pointer.x;drag.y=pointer.y;}
  if(game.phase==='wave'&&!paused){if(game.ball.held)aimed(pointer);else game.paddle.x=clamp(pointer.x,game.paddleWidth/2+17,W-game.paddleWidth/2-17);}
});
function releaseDrag(){if(drag){if(!moveBuilding(game,drag.id,drag.x,drag.y))toast('Posizione occupata. Lascia libero un po’ di spazio.');drag=null;updateUI();}}
canvas.addEventListener('pointerup',releaseDrag);canvas.addEventListener('pointercancel',()=>{drag=null;});
window.addEventListener('keydown',e=>{
  if($('pause-menu').open&&['KeyP','Escape'].includes(e.code)){e.preventDefault();closePause();return;}
  if(document.querySelector('dialog[open]')||/INPUT|TEXTAREA|SELECT/.test(e.target.tagName))return;
  if(e.code==='Space'&&e.target.tagName==='BUTTON')return;
  if(['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Space'].includes(e.code))e.preventDefault();
  keys.add(e.code);if(e.repeat)return;
  if(e.code==='Space'){if(game.phase==='build')act();else if(game.phase==='wave'&&!paused)launch(game);}
  if(e.code==='KeyF')toggleForge();if(e.code==='KeyC')toggleBurn();if(e.code==='KeyR'&&!paused)recall(game);
  if(e.code==='KeyP')togglePause();if(e.code==='Escape'){placing=null;drag=null;if(game.phase==='wave')togglePause();}
  if(game.phase==='build'&&['Digit1','Digit2'].includes(e.code)){const button=document.querySelector(`[data-build="${e.code==='Digit1'?'wall':'turret'}"]`);if(!button.disabled)button.click();}
});
window.addEventListener('keyup',e=>keys.delete(e.code));window.addEventListener('blur',()=>keys.clear());
document.addEventListener('visibilitychange',()=>{if(document.hidden&&game.phase==='wave'&&!paused&&!document.querySelector('dialog[open]'))togglePause();});
$('main-action').onclick=act;$('forge').onclick=toggleForge;$('burn').onclick=toggleBurn;$('recall').onclick=()=>{recall(game);updateUI();};$('pause').onclick=togglePause;
$('sound').onclick=()=>{sound=!sound;$('sound').setAttribute('aria-pressed',String(sound));$('sound').textContent=sound?'Audio acceso':'Audio spento';tone();};
$('enter').onclick=()=>{$('welcome').close();document.body.classList.remove('title-open');canvas.focus({preventScroll:true});};
$('resume').onclick=closePause;
document.querySelectorAll('[data-help]').forEach(b=>b.onclick=()=>$('controls').showModal());
$('close-controls').onclick=()=>$('controls').close();
$('pause-menu').addEventListener('cancel',e=>{e.preventDefault();closePause();});
$('welcome').addEventListener('cancel',e=>e.preventDefault());
$('fullscreen').hidden=!document.fullscreenEnabled;
$('fullscreen').onclick=async()=>{try{if(document.fullscreenElement)await document.exitFullscreen();else await document.documentElement.requestFullscreen();}catch{toast('Schermo intero non disponibile su questo dispositivo.');}};
$('again').onclick=newGame;$('restart').onclick=()=>{if(game.wave>0&&!window.confirm('Ricominciare questa spedizione?'))return;newGame();};
document.querySelectorAll('[data-build]').forEach(b=>b.onclick=()=>{placing=placing===b.dataset.build?null:b.dataset.build;toast(placing?'Clicca nell’arena per posizionare.':'Costruzione annullata.');updateUI();});
document.querySelectorAll('[data-upgrade]').forEach(b=>b.onclick=()=>{chooseUpgrade(game,b.dataset.upgrade);$('upgrade').close();updateUI();});
for(const id of ['upgrade','result'])$(id).addEventListener('cancel',e=>e.preventDefault());
function updateUI(){
  const s=game,b=s.ball,key=JSON.stringify([s.phase,s.wave,s.hp,s.iron,s.delivered,b.ore,b.metal,b.held,s.capacity,s.level,s.forgeOpen,s.burn,placing,paused,Math.ceil(s.recall)]);
  if(key===uiCache)return;uiCache=key;
  const active=s.phase==='wave';
  document.body.dataset.phase=s.phase;
  $('phase').textContent=paused?'PAUSA':({build:'COSTRUZIONE',wave:'ASSEDIO',upgrade:'TALENTO',won:'VITTORIA',short:'SPEDIZIONE FINITA',lost:'SCONFITTA'})[s.phase];
  $('phase-dot').style.background=active?'#bb8356':'#738765';document.body.classList.toggle('paused',paused);
  $('wave').textContent=`Ondata ${s.phase==='build'?s.wave+1:s.wave} / ${s.maxWaves}`;
  $('hp').textContent=`${s.hp} / ${s.maxHp}`;$('iron').textContent=s.iron;$('delivered').textContent=`${s.delivered} / ${s.target}`;
  $('health-pips').replaceChildren(...Array.from({length:s.maxHp},(_,i)=>{const pip=document.createElement('i');if(i>=s.hp)pip.className='empty';return pip;}));
  $('ore').textContent=b.ore;$('metal').textContent=b.metal;$('capacity').textContent=`${cargo(s)} / ${s.capacity}`;
  $('cargo-slots').replaceChildren(...Array.from({length:s.capacity},(_,i)=>{const d=document.createElement('span');d.className='cargo-slot'+(i<b.metal?' metal':i<cargo(s)?' ore':'');return d;}));
  $('forge').querySelector('strong').textContent=s.forgeOpen?'Fonderia aperta':'Fonderia chiusa';$('forge-desc').textContent=s.forgeOpen?'Trasforma il minerale':'Il tetto riflette la sfera';$('forge-toggle').className='ability-light'+(s.forgeOpen?' on':'');$('forge').setAttribute('aria-pressed',String(s.forgeOpen));
  $('burn-label').textContent=s.burn?'Carico armato':'Conserva il carico';$('burn-desc').textContent=s.burn?'+2 danni minerale / +4 ferro':'Consegna al deposito';$('burn-toggle').className='ability-light'+(s.burn?' on danger':'');$('burn').setAttribute('aria-pressed',String(s.burn));
  $('recall').disabled=!active||b.held||s.recall>0||paused;$('recall-label').textContent=s.recall>0?`Richiamo · ${Math.ceil(s.recall)}s`:'Richiama';
  document.querySelectorAll('[data-build]').forEach(b=>{b.disabled=s.phase!=='build'||s.iron<COST[b.dataset.build]||s.buildings.length>=10;b.classList.toggle('selected',placing===b.dataset.build);});
  $('build-hint').textContent=active?'Costruisci fra un’ondata e la successiva.':placing?'Clicca per posizionare. Esc annulla.':'Trascina gli edifici. Seleziona una costruzione per aggiungerla.';
  $('board-tip').textContent=active?(b.held?'Mira e clicca per lanciare':s.burn?'Carico armato: ogni colpo consuma una risorsa.':'Fonderia → deposito · R richiama la sfera'):'Trascina gli edifici · 1 Sponda · 2 Sentinella · Spazio Avvia';
  $('hero').textContent=`APPRENDISTA · LIV. ${s.level}`;
  $('main-action').innerHTML=(s.phase==='build'?'Avvia l’ondata':s.phase==='wave'?(paused?'Riprendi':b.held?'Lancia la sfera':'Richiama la sfera'):s.phase==='upgrade'?'Scegli il talento':'Nuova spedizione')+' <span>↗</span>';
  $('main-action').disabled=active&&!paused&&!b.held&&s.recall>0;
  $('main-action').hidden=active&&!b.held;
  if(s.phase==='upgrade'&&!$('upgrade').open)$('upgrade').showModal();
  if(['won','short','lost'].includes(s.phase)&&!$('result').open){
    const won=s.phase==='won',short=s.phase==='short';
    $('result-eyebrow').textContent=won?'UNA CITTÀ CHE SA RIMBALZARE':short?'LA CITTÀ RESISTE. LA FONDERIA ASPETTA.':'DAL CRATERE SI IMPARA';
    $('result-title').textContent=won?'Il quartiere è salvo.':short?'Serviva un altro viaggio.':'Si riparte dalle fondamenta.';
    $('result-desc').textContent=won?'Hai difeso la città e consegnato il ferro necessario. Il tuo progetto funziona.':short?`Hai superato le cinque ondate, ma servivano ${s.target} ferro consegnati. Prova ad avvicinare fonderia e deposito.`:'La città ha perso tutta l’integrità. Prova una racchetta più larga, una sentinella o richiami più frequenti.';
    $('result-stats').innerHTML=[['Ferro consegnato',`${s.delivered} / ${s.target}`],['Minerale raccolto',s.stats.mined],['Nemici frantumati',s.kills],['Risorse usate in attacco',s.stats.spentCargo],['Carico perso',s.stats.lostCargo]].map(([a,b])=>`<div><span>${a}</span><b>${b}</b></div>`).join('');$('result').showModal();
  }
}
function rect(x,y,w,h,fill,stroke){ctx.fillStyle=fill;ctx.fillRect(x,y,w,h);if(stroke){ctx.strokeStyle=stroke;ctx.lineWidth=1;ctx.strokeRect(x+.5,y+.5,w-1,h-1);}}
function line(x,y,tx,ty,color,width=1){ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(tx,ty);ctx.strokeStyle=color;ctx.lineWidth=width;ctx.stroke();}
function text(t,x,y,size=12,color='#9ab1a0',align='left',font='monospace'){ctx.fillStyle=color;ctx.font=`${size}px ${font}`;ctx.textAlign=align;ctx.fillText(t,x,y);}
function diamond(x,y,r,color){ctx.beginPath();ctx.moveTo(x,y-r);ctx.lineTo(x+r,y);ctx.lineTo(x,y+r);ctx.lineTo(x-r,y);ctx.closePath();ctx.fillStyle=color;ctx.fill();}
function backdrop(){
  ctx.setTransform(view.dpr,0,0,view.dpr,0,0);
  const w=view.width,h=view.height,t=reduced?0:performance.now()/1000;
  rect(0,0,w,h,'#0a111d');
  const haze=ctx.createRadialGradient(w*.7,h*.42,0,w*.65,h*.45,Math.max(w,h)*.7);haze.addColorStop(0,'#3a393450');haze.addColorStop(.5,'#18243680');haze.addColorStop(1,'#080d16');ctx.fillStyle=haze;ctx.fillRect(0,0,w,h);
  for(let i=0;i<70;i++){const x=(Math.sin(i*133.7)*.5+.5)*w,y=((Math.cos(i*87.3)*.5+.5)*h+t*(i%3)*.35)%h;ctx.globalAlpha=.15+(i%5)*.07;rect(x,y,i%9===0?2:1,i%9===0?2:1,'#d3d4bf');}ctx.globalAlpha=1;
  for(let i=0;i<16;i++){
    const side=i%2,x=side?w-(Math.sin(i*5.1)*.5+.5)*w*.08:(Math.cos(i*4.1)*.5+.5)*w*.08,y=i/16*h;
    ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x+(side?-1:1)*(30+i%3*18),y+25);ctx.lineTo(x+(side?-1:1)*(20+i%2*25),y+65);ctx.lineTo(x,y+90);ctx.lineTo(side?w:0,y+65);ctx.closePath();ctx.fillStyle=i%3?'#1e2833':'#293039';ctx.fill();ctx.strokeStyle='#41443950';ctx.lineWidth=1;ctx.stroke();
  }
  if($('welcome').open){
    const x=w*.76,y=h*.43,r=Math.min(w*.18,h*.3);
    for(let i=0;i<4;i++){ctx.beginPath();ctx.ellipse(x,y,r*(1.3+i*.23),r*(.7+i*.17),-.65,0,Math.PI*2);ctx.strokeStyle=i%2?'#a47b3e28':'#8b9aab22';ctx.lineWidth=i===0?2:1;ctx.stroke();}
    const planet=ctx.createRadialGradient(x-r*.35,y-r*.4,r*.06,x,y,r);planet.addColorStop(0,'#e3bf7e');planet.addColorStop(.2,'#a8844f');planet.addColorStop(.58,'#454b4c');planet.addColorStop(1,'#151d2a');ctx.shadowColor='#e3ae6245';ctx.shadowBlur=55;ctx.beginPath();ctx.arc(x,y,r,0,Math.PI*2);ctx.fillStyle=planet;ctx.fill();ctx.shadowBlur=0;
    ctx.save();ctx.beginPath();ctx.arc(x,y,r,0,Math.PI*2);ctx.clip();
    for(let i=0;i<11;i++){const yy=y-r+i*r*.2;line(x-r,yy,x+r,yy-r*.35,'#0c152357',2);}
    ctx.beginPath();ctx.ellipse(x,y,r*.42,r*1.1,-.45,0,Math.PI*2);ctx.strokeStyle='#d7bc7a66';ctx.lineWidth=4;ctx.stroke();
    for(let i=0;i<15;i++){const px=x+Math.sin(i*3.7)*r*.9,py=y+Math.cos(i*2.9)*r*.9;line(px,py,px+r*.1,py+r*.08,'#ffc57370',1.5);}
    ctx.restore();
    for(let i=0;i<7;i++){const a=i/7*Math.PI*2+t*.03,px=x+Math.cos(a)*r*1.5,py=y+Math.sin(a)*r*.85;diamond(px,py,4+i%3*3,i%2?'#94764d':'#8d9fa5');}
    return false;
  }
  ctx.translate(view.x,view.y);ctx.scale(view.scale,view.scale);return true;
}
function building(b,ghost=false){
  const s=game,c=colors[b.type],open=b.type==='forge'&&s.forgeOpen;
  ctx.save();if(ghost)ctx.globalAlpha=.5;
  rect(b.x+5,b.y+7,b.w,b.h,'#08181588');
  if(b.type==='wall'){
    rect(b.x,b.y,b.w,b.h,'#415d50','#8aa18a');rect(b.x+4,b.y+4,b.w-8,4,'#8ca388');
    for(let i=20;i<b.w;i+=24)line(b.x+i,b.y+8,b.x+i,b.y+b.h-3,'#263e34',2);
    text('SPONDA',b.x+b.w/2,b.y+b.h+16,9,'#7d9786','center');
  }else if(b.type==='forge'){
    rect(b.x,b.y,b.w,b.h,open?'#39382b':'#624b35',c);
    rect(b.x+8,b.y+8,24,b.h-16,'#242b25','#9b7850');
    for(let i=0;i<3;i++)rect(b.x+12,b.y+13+i*11,16,5,open?'#d49c50':'#6e6752');
    if(open){ctx.setLineDash([5,5]);line(b.x+39,b.y+b.h/2,b.x+b.w-9,b.y+b.h/2,'#d5a65a88',2);ctx.setLineDash([]);text('→',b.x+b.w-18,b.y+42,22,'#d3a560','center');}
    else{for(let x=40;x<b.w;x+=18)line(b.x+x,b.y+8,b.x+x-17,b.y+b.h-8,'#ad8d5c',3);}
    text(open?'FONDERIA APERTA':'TETTO CHIUSO',b.x+b.w/2,b.y+b.h+17,10,c,'center');
    rect(b.x+12,b.y-17,19,17,'#62634e','#a6a079');
    if(open&&!reduced){for(let i=0;i<3;i++){const t=(s.elapsed*.22+i*.32)%1;ctx.globalAlpha=(1-t)*.25;rect(b.x+15+Math.sin(s.elapsed+i)*7,b.y-22-t*28,8+t*10,6+t*9,'#bec6a3');}ctx.globalAlpha=ghost?.5:1;}
  }else if(b.type==='depot'){
    rect(b.x,b.y,b.w,b.h,'#244637',c);rect(b.x+8,b.y+8,b.w-16,b.h-16,'#1a352a','#5b7d5e');
    for(let i=0;i<3;i++){const x=b.x+20+i*31;rect(x,b.y+23,23,19,'#71896a','#afbc8c');line(x+1,b.y+26,x+21,b.y+39,'#405b41');line(x+21,b.y+26,x+1,b.y+39,'#405b41');}
    text('DEPOSITO',b.x+b.w/2,b.y+b.h+17,10,c,'center');text('↓',b.x+b.w/2,b.y-9,19,c,'center');
  }else{
    rect(b.x,b.y,b.w,b.h,'#314b35',c);rect(b.x+7,b.y+7,b.w-14,b.h-14,'#4b6546','#7d9671');
    ctx.beginPath();ctx.arc(b.x+27,b.y+27,11,0,Math.PI*2);ctx.strokeStyle='#b6c397';ctx.lineWidth=3;ctx.stroke();line(b.x+27,b.y+29,b.x+27,b.y+7,'#e0d4a5',7);
    text('SENTINELLA',b.x+b.w/2,b.y+b.h+17,9,c,'center');
  }
  if(s.phase==='build'){ctx.setLineDash([3,5]);ctx.strokeStyle='#a4b9a64d';ctx.strokeRect(b.x-5,b.y-5,b.w+10,b.h+10);ctx.setLineDash([]);}
  ctx.restore();
}
function draw(){
  if(!backdrop())return;
  const s=game,b=s.ball;
  const glow=ctx.createRadialGradient(430,290,20,430,290,600);glow.addColorStop(0,'#29384688');glow.addColorStop(1,'#0c132244');ctx.fillStyle=glow;ctx.fillRect(0,0,W,H);
  if(s.phase==='build'){
    for(let x=32;x<W;x+=32)for(let y=48;y<H;y+=32){ctx.fillStyle='#b3bdc629';ctx.fillRect(x,y,1.5,1.5);}
    ctx.save();ctx.strokeStyle='#8194a45a';ctx.setLineDash([3,8]);ctx.strokeRect(24,240,912,350);ctx.restore();
    text('ZONE EDIFICABILI',36,577,9,'#839699');
  }
  line(20,614,940,614,'#b4815a70');text('LIMITE DEL QUARTIERE',480,610,9,'#aa916e','center');
  rect(12,28,936,8,'#3d4855','#71808b');rect(12,28,8,619,'#3d4855','#71808b');rect(940,28,8,619,'#3d4855','#71808b');
  for(let x=40;x<940;x+=48){rect(x,30,17,4,'#8e855e');}
  text('DAL CRATERE',480,58,10,'#a2a795','center');
  for(let i=0;i<s.mines.length;i++){
    const m=s.mines[i],flash=s.elapsed-(s.hits.get('mine'+i)??-10)<.16;
    rect(m.x+4,m.y+5,m.w,m.h,'#0b1e19aa');
    ctx.beginPath();ctx.moveTo(m.x,m.y+15);ctx.lineTo(m.x+14,m.y);ctx.lineTo(m.x+m.w-12,m.y);ctx.lineTo(m.x+m.w,m.y+18);ctx.lineTo(m.x+m.w-6,m.y+m.h);ctx.lineTo(m.x+4,m.y+m.h);ctx.closePath();ctx.fillStyle=flash?'#c39d6b':'#4b5040';ctx.fill();ctx.strokeStyle='#a88c5c';ctx.stroke();
    diamond(m.x+25,m.y+21,8,'#ad895a');diamond(m.x+53,m.y+17,10,'#c4a66c');diamond(m.x+83,m.y+27,7,'#9c7c50');
    line(m.x+36,m.y+7,m.x+42,m.y+35,'#222f26',2);text('GIACIMENTO',m.x+m.w/2,m.y+m.h+17,9,'#ac9670','center');
  }
  if(s.phase==='build'){
    ctx.save();ctx.setLineDash([5,9]);ctx.beginPath();ctx.moveTo(421,211);ctx.lineTo(s.buildings[0].x+65,s.buildings[0].y+34);ctx.lineTo(s.buildings[1].x+68,s.buildings[1].y+32);ctx.strokeStyle='#c2b16b35';ctx.stroke();ctx.restore();
  }
  for(const entity of s.buildings)building(entity);
  for(const e of s.enemies){
    const thief=e.type==='thief',c=thief?'#a6b9a0':'#d29c83';
    rect(e.x+3,e.y+5,e.w,e.h,'#081b1788');
    if(thief){ctx.beginPath();ctx.moveTo(e.x+24,e.y-6);ctx.lineTo(e.x+e.w,e.y+e.h);ctx.lineTo(e.x,e.y+e.h);ctx.closePath();ctx.fillStyle='#456258';ctx.fill();ctx.strokeStyle=c;ctx.stroke();}
    else{rect(e.x,e.y,e.w,e.h,'#5c493b',c);for(let i=0;i<3;i++){line(e.x-6,e.y+8+i*10,e.x+1,e.y+10+i*8,c,2);line(e.x+e.w-1,e.y+10+i*8,e.x+e.w+6,e.y+8+i*10,c,2);}}
    ctx.shadowColor='#f7b979';ctx.shadowBlur=7;rect(e.x+13,e.y+12,5,4,'#f5d8a0');rect(e.x+30,e.y+12,5,4,'#f5d8a0');ctx.shadowBlur=0;
    for(let i=0;i<e.maxHp;i++)rect(e.x+4+i*10,e.y+e.h+5,7,3,i<e.hp?c:'#415247');
  }
  for(const shot of s.shots){ctx.globalAlpha=shot.life/.2;line(shot.x,shot.y,shot.tx,shot.ty,'#dfcb86',2);ctx.globalAlpha=1;}
  if(drag||placing){const entity=drag?s.buildings.find(x=>x.id===drag.id):{type:placing,w:placing==='wall'?112:54,h:placing==='wall'?24:54};const p=drag||pointer;const ghost={...entity,x:Math.round((p.x-entity.w/2)/16)*16,y:Math.round((p.y-entity.h/2)/16)*16};building(ghost,true);ctx.strokeStyle=canPlace(s,ghost,drag?.id)?'#abc59a':'#db9a7b';ctx.lineWidth=2;ctx.strokeRect(ghost.x-4,ghost.y-4,ghost.w+8,ghost.h+8);}
  const paddle=s.paddle,pw=s.paddleWidth;
  rect(paddle.x-pw/2+3,paddle.y+6,pw,12,'#071a15');rect(paddle.x-pw/2,paddle.y,pw,12,'#b6c8a8','#e2e7cc');rect(paddle.x-pw/2+4,paddle.y+4,14,8,'#5e896b');rect(paddle.x+pw/2-18,paddle.y+4,14,8,'#5e896b');rect(paddle.x-14,paddle.y+3,28,6,'#e8c37f');
  if(b.held){b.x=paddle.x;b.y=paddle.y-22;if(s.phase==='wave'&&!paused){
    ctx.save();ctx.setLineDash([4,9]);line(b.x,b.y,b.x+Math.cos(s.aim)*175,b.y+Math.sin(s.aim)*175,'#e8d8a977');ctx.restore();
    text('MIRA E CLICCA',paddle.x,paddle.y-54,10,'#c8c5a0','center');
  }}
  if(!reduced)for(let i=b.trail.length-1;i>=0;i--){const t=b.trail[i];ctx.beginPath();ctx.arc(t.x,t.y,b.r*(1-i/(b.trail.length+1)),0,Math.PI*2);ctx.fillStyle=`rgba(${s.burn?'233,157,102':'209,215,160'},${.28*(1-i/b.trail.length)})`;ctx.fill();}
  ctx.shadowColor=s.burn?'#ebad65':'#d1dab2';ctx.shadowBlur=15;ctx.beginPath();ctx.arc(b.x,b.y,b.r,0,Math.PI*2);ctx.fillStyle=s.burn?'#eabd7b':'#e5e8c4';ctx.fill();ctx.shadowBlur=0;ctx.beginPath();ctx.arc(b.x-2,b.y-2,3,0,Math.PI*2);ctx.fillStyle='#fffbea';ctx.fill();
  if(cargo(s)>0){for(let i=0;i<cargo(s);i++){const angle=i/cargo(s)*Math.PI*2+(reduced?0:s.elapsed);diamond(b.x+Math.cos(angle)*17,b.y+Math.sin(angle)*17,3,i<b.metal?colors.metal:colors.ore);}text(`${b.ore} M · ${b.metal} Fe`,b.x,b.y-25,10,'#d9c595','center');}
  for(const effect of s.effects){ctx.globalAlpha=clamp(effect.life,0,1);if(!reduced){ctx.beginPath();ctx.arc(effect.x,effect.y+20,Math.max(1,(1.6-effect.life)*23),0,Math.PI*2);ctx.strokeStyle=effect.color+'55';ctx.lineWidth=1;ctx.stroke();}text(effect.text,effect.x,effect.y,13,effect.color,'center');ctx.globalAlpha=1;}
  if(s.phase==='build'){text('COSTRUZIONE',480,88,18,'#dec28a','center');text('Prepara il percorso del ferro',480,111,12,'#8e9da8','center');}
  $('clock').textContent=`00:${String(Math.ceil(Math.max(0,s.duration-s.time))).padStart(2,'0')}`;
}
function frame(now){
  const dt=Math.min((now-last)/1000,.05);last=now;
  if(!paused&&!document.querySelector('dialog[open]')){
    if(game.phase==='wave'){
      const direction=(keys.has('ArrowRight')||keys.has('KeyD')?1:0)-(keys.has('ArrowLeft')||keys.has('KeyA')?1:0);
      game.paddle.x=clamp(game.paddle.x+direction*650*dt,game.paddleWidth/2+17,W-game.paddleWidth/2-17);
      if(game.ball.held){if(keys.has('ArrowUp'))game.aim-=dt;if(keys.has('ArrowDown'))game.aim+=dt;game.aim=clamp(game.aim,-Math.PI+.18,-.18);}
    }
    step(game,dt);
  }
  if(game.stats.mined+game.stats.smelted+game.delivered+game.kills>lastEvent){lastEvent=game.stats.mined+game.stats.smelted+game.delivered+game.kills;tone(game.ball.metal?660:360);}
  if(now>toastUntil)$('toast').classList.remove('visible');
  updateUI();draw();requestAnimationFrame(frame);
}
updateUI();document.body.classList.add('title-open');$('welcome').showModal();draw();requestAnimationFrame(frame);
