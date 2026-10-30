import assert from 'node:assert/strict';
import { mkdirSync } from 'node:fs';
const { chromium } = await import(process.env.PLAYWRIGHT_CORE || 'playwright-core');
mkdirSync(new URL('../artifacts/',import.meta.url),{recursive:true});
const browser=await chromium.launch({executablePath:process.env.CHROME || '/usr/bin/google-chrome',args:['--no-sandbox','--disable-dev-shm-usage']});
const errors=[];
try {
 for(const width of [1360,390]){
  const context=await browser.newContext({viewport:{width,height:width>500?1050:844},hasTouch:width<500,isMobile:width<500});
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
  // Test-only access is injected into the served script, never into shipped assets.
  await page.route('**/app.js',async route=>{const response=await route.fetch();let body=await response.text();body=body.replace("const keys=new Set()","Object.defineProperty(window,'testGame',{get:()=>game});\nconst keys=new Set()");await route.fulfill({response,body});});
  await page.goto('http://127.0.0.1:8143/');
  await page.getByRole('button',{name:/Entra in officina/}).click();
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),true);
  await page.screenshot({path:new URL(`../artifacts/${width}-build.png`,import.meta.url).pathname,fullPage:true});
  const board=page.locator('#board'),box=await board.boundingBox();
  const clickAt=(x,y)=>page.mouse.click(box.x+x/960*box.width,box.y+y/720*box.height);
  await page.locator('[data-build="wall"]').click();await clickAt(500,520);
  assert.equal(await page.locator('#iron').textContent(),'3');
  const forge=await page.evaluate(()=>({...testGame.buildings[0]}));
  await page.mouse.move(box.x+(forge.x+65)/960*box.width,box.y+(forge.y+34)/720*box.height);await page.mouse.down();
  await page.mouse.move(box.x+465/960*box.width,box.y+310/720*box.height);await page.mouse.up();
  assert.notEqual(await page.evaluate(()=>testGame.buildings[0].x),forge.x);
  await page.getByRole('button',{name:/Avvia l’ondata/}).click();
  await clickAt(420,180);await page.waitForTimeout(300);
  assert.equal(await page.evaluate(()=>testGame.phase),'wave');assert.equal(await page.evaluate(()=>testGame.ball.held),false);
  await page.getByRole('button',{name:/Fonderia aperta/}).click();assert.equal(await page.evaluate(()=>testGame.forgeOpen),false);
  await page.getByRole('button',{name:/Conserva il carico/}).click();assert.equal(await page.evaluate(()=>testGame.burn),true);
  await page.getByRole('button',{name:/Pausa/}).click();const before=await page.evaluate(()=>testGame.time);await page.waitForTimeout(100);assert.equal(await page.evaluate(()=>testGame.time),before);
  await page.getByRole('button',{name:/Riprendi/}).first().click();
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
  await page.getByRole('button',{name:/Un altro progetto/}).click();assert.equal(await page.evaluate(()=>testGame.wave),0);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),true);
  await context.close();
 }
 assert.deepEqual(errors,[]);console.log('Browser OK 1360px / 390px: costruzione, spostamento, lancio, fonderia, carico, consegna, pausa, talento, vittoria e riavvio.');
}finally{await browser.close();}
