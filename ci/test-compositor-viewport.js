'use strict';
const fs=require('node:fs');const assert=require('node:assert/strict');
const {chromium}=require('playwright');const {PNG}=require('pngjs');
const {captureCompositorViewport}=require('./capture-compositor-viewport');
const {cropGameCanvasFromViewportScreenshot}=require('./canvas-screenshot-fallback');
(async()=>{
  let detached=0;
  const fake=send=>({context:()=>({newCDPSession:async()=>({send,detach:async()=>{detached++;}})})});
  await assert.rejects(captureCompositorViewport(fake(async()=>({data:'notPNG'}))),/invalid PNG/);
  await assert.rejects(captureCompositorViewport(fake(()=>new Promise(()=>{})),5),/timed out/);
  await assert.rejects(captureCompositorViewport(fake(async()=>{throw new Error('closed');})),/closed/);
  assert.equal(detached,3);
  const source=fs.readFileSync('ci/campaign-render-test.js','utf8');
  assert.match(source,/captureCompositorViewport\(page, screenshotTimeoutMs\)/);
  const browser=await chromium.launch({headless:true,args:['--enable-unsafe-swiftshader']});
  try{
    const page=await browser.newPage({viewport:{width:1000,height:850}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
    await page.setContent('<style>body{margin:0;background:#050505}#game{width:640px;height:480px;border:2px solid #00d9ff;margin:60px auto}canvas{display:block;width:640px;height:480px}</style><div id="game"><canvas id="c" width="640" height="480"></canvas></div>');
    await page.evaluate(()=>{
      const c=document.getElementById('c'),ctx=c.getContext('2d');window.framesSeen=0;window.stopTest=false;
      function draw(){if(window.stopTest)return;window.framesSeen++;ctx.fillStyle='rgb(190,30,40)';ctx.fillRect(0,0,640,240);ctx.fillStyle='rgb(30,60,180)';ctx.fillRect(0,240,640,240);ctx.fillStyle='rgb(50,'+(window.framesSeen%200+30)+',60)';ctx.fillRect(300,200,40,80);const start=performance.now();while(performance.now()-start<8){}requestAnimationFrame(draw);}
      draw();
    });
    let before=await page.evaluate(()=>window.framesSeen);let pixels=0;
    for(let i=0;i<3;i++){
      await page.waitForFunction(start=>window.framesSeen>=start+3,before);
      const bytes=await captureCompositorViewport(page,10000);
      const frame=PNG.sync.read(bytes);assert.equal(frame.width,1000);assert.equal(frame.height,850);
      const crop=cropGameCanvasFromViewportScreenshot(bytes,640,480);assert.ok(crop);
      const png=PNG.sync.read(crop.buffer);assert.ok(Math.abs(png.width-640)<=1&&Math.abs(png.height-480)<=1);
      const at=(x,y)=>Array.from(png.data.subarray((y*png.width+x)*4,(y*png.width+x)*4+4));
      assert.deepEqual(at(100,100),[190,30,40,255]);assert.deepEqual(at(100,350),[30,60,180,255]);
      pixels+=png.width*png.height;before=await page.evaluate(()=>window.framesSeen);
    }
    await page.evaluate(()=>{window.stopTest=true;});assert.deepEqual(errors,[]);
    console.log('test-compositor-viewport: OK real animated canvas, three PNG crops, '+pixels+' pixels; failure/timeout cleanup passed');
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
