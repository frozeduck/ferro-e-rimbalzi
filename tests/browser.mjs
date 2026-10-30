import assert from 'node:assert/strict';
import { mkdirSync } from 'node:fs';
const { chromium } = await import(process.env.PLAYWRIGHT_CORE || process.argv[2] || 'playwright-core');
mkdirSync(new URL('../artifacts/',import.meta.url),{recursive:true});
const browser=await chromium.launch({executablePath:process.env.CHROME || '/usr/bin/google-chrome',args:['--no-sandbox','--disable-dev-shm-usage']});
const errors=[];
try {
 for(const [width,height] of [[1360,900],[390,844],[844,390]]){
  const context=await browser.newContext({viewport:{width,height},hasTouch:width<900,isMobile:width<900});
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
  // Test-only access is injected into the served script, never into shipped assets.
  await page.route('**/app.js',async route=>{const response=await route.fetch();let body=await response.text();body=body.replace("const keys=new Set()","Object.defineProperty(window,'testGame',{get:()=>game});\nconst keys=new Set()");await route.fulfill({response,body});});
  await page.goto(process.env.BASE_URL || process.argv[3] || 'http://127.0.0.1:8143/');
  await page.locator('#enter').waitFor();
  await page.screenshot({path:new URL(`../artifacts/${width}-title.png`,import.meta.url).pathname});
  await page.locator('[data-help]').first().click();await page.locator('#controls[open]').waitFor();await page.locator('#close-controls').click();
  await page.locator('#enter').click();
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),true);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollHeight<=document.documentElement.clientHeight),true,'Il gioco non deve diventare una pagina da scorrere');
  await page.screenshot({path:new URL(`../artifacts/${width}-build.png`,import.meta.url).pathname,fullPage:true});
  const board=page.locator('#board');
  const worldPoint=async(x,y)=>{const box=await board.boundingBox(),scale=Math.min(box.width/960,box.height/720);return{x:box.x+(box.width-960*scale)/2+x*scale,y:box.y+(box.height-720*scale)/2+y*scale};};
  const clickAt=async(x,y)=>{const p=await worldPoint(x,y);await page.mouse.click(p.x,p.y);};
  await page.locator('[data-build="wall"]').click();await clickAt(500,520);
  assert.equal(await page.locator('#iron').textContent(),'3');
  const forge=await page.evaluate(()=>({...testGame.buildings[0]}));
  let p=await worldPoint(forge.x+65,forge.y+34);await page.mouse.move(p.x,p.y);await page.mouse.down();
  p=await worldPoint(465,310);await page.mouse.move(p.x,p.y);await page.mouse.up();
  assert.notEqual(await page.evaluate(()=>testGame.buildings[0].x),forge.x);
  await page.getByRole('button',{name:/Avvia l’ondata/}).click();
  await page.waitForTimeout(100);await clickAt(420,180);await page.waitForTimeout(300);
  assert.equal(await page.evaluate(()=>testGame.phase),'wave');assert.equal(await page.evaluate(()=>testGame.ball.held),false);
  assert.equal(await page.locator('#build-options').isVisible(),false,'Gli strumenti di costruzione devono lasciare spazio all’azione');
  await page.getByRole('button',{name:/Fonderia aperta/}).click();assert.equal(await page.evaluate(()=>testGame.forgeOpen),false);
  await page.getByRole('button',{name:/Conserva il carico/}).click();assert.equal(await page.evaluate(()=>testGame.burn),true);
  await page.locator('#pause').click();await page.locator('#pause-menu[open]').waitFor();const before=await page.evaluate(()=>testGame.time);await page.waitForTimeout(100);assert.equal(await page.evaluate(()=>testGame.time),before);
  await page.locator('#resume').click();
  await page.keyboard.press('p');await page.locator('#pause-menu[open]').waitFor();await page.keyboard.press('Escape');assert.equal(await page.locator('#pause-menu').isVisible(),false);
  await page.evaluate(()=>{const s=testGame;s.burn=false;s.forgeOpen=true;s.ball.ore=2;s.ball.metal=0;s.ball.held=false;s.ball.x=s.buildings[0].x+65;s.ball.y=s.buildings[0].y+34;s.ball.vx=0;s.ball.vy=-200;});
  await page.waitForFunction(()=>testGame.ball.metal===2);
  await page.evaluate(()=>{const s=testGame;s.ball.x=s.buildings[1].x+68;s.ball.y=s.buildings[1].y+32;s.ball.vx=0;s.ball.vy=-200;});
  await page.waitForFunction(()=>testGame.delivered===2);assert.equal(await page.locator('#iron').textContent(),'5');
  await page.screenshot({path:new URL(`../artifacts/${width}-wave.png`,import.meta.url).pathname,fullPage:true});
  await page.evaluate(()=>{testGame.enemies=[];testGame.time=10;});
  await page.locator('#upgrade[open]').waitFor();await page.locator('[data-upgrade="cargo"]').click();
  assert.equal(await page.evaluate(()=>testGame.capacity),6);assert.equal(await page.evaluate(()=>testGame.phase),'build');
  await page.getByRole('button',{name:/Avvia l’ondata/}).click();
  await page.evaluate(()=>{testGame.wave=5;testGame.delivered=14;testGame.enemies=[];testGame.time=10;});
  await page.locator('#result[open]').waitFor();assert.equal(await page.locator('#result-title').textContent(),'Il quartiere è salvo.');
  await page.locator('#again').click();assert.equal(await page.evaluate(()=>testGame.wave),0);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),true);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollHeight<=document.documentElement.clientHeight),true);
  await context.close();
 }
 assert.deepEqual(errors,[]);console.log('Browser OK desktop / telefono verticale e orizzontale: menu, zero scorrimento, costruzione, mira, carico, consegna, pausa, talento, vittoria e riavvio.');
}finally{await browser.close();}
