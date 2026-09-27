'use strict';
const assert=require('node:assert/strict');const fs=require('node:fs');const os=require('node:os');const path=require('node:path');
const {chromium}=require('playwright');const {collectStallAttribution}=require('./cheerpj-stall-attribution');
(async()=>{
 const dir=fs.mkdtempSync(path.join(os.tmpdir(),'cheerpj-attribution-'));const browser=await chromium.launch({headless:true,args:['--enable-unsafe-swiftshader']});
 try{
  const context=await browser.newContext();const page=await context.newPage();
  await page.setContent('<canvas id="lwjglCanvas" width="64" height="64"></canvas>');
  await page.evaluate(()=>{
   const c=document.querySelector('canvas'),gl=c.getContext('webgl2',{antialias:false});if(!gl)throw new Error('No WebGL2');
   window.__lwjglPresentationStats={swapCount:0};window.__saved=Object.fromEntries(['getParameter','clear','isEnabled','flush'].map(k=>[k,gl[k]]));
   window.__stop=false;const frame=()=>{if(window.__stop)return;gl.clearColor(.2,.4,.6,1);gl.clear(gl.COLOR_BUFFER_BIT);gl.getParameter(gl.VIEWPORT);gl.isEnabled(gl.BLEND);window.__lwjglPresentationStats.swapCount++;requestAnimationFrame(frame);};frame();
  });
  const report=await collectStallAttribution(page,context,dir,500);
  assert.equal(report.ok,true);assert.ok(report.webgl.totals.clear.calls>0);assert.ok(report.trace.bytes>0);assert.ok(report.webgl.frames.length>0);assert.equal(report.webgl.restored,report.webgl.expected);
  const restored=await page.evaluate(()=>{const gl=document.querySelector('canvas').getContext('webgl2');window.__stop=true;return !window.__starsectorStallAttribution&&Object.entries(window.__saved).every(([k,v])=>gl[k]===v&&!Object.hasOwn(gl,k));});
  assert.equal(restored,true);assert.ok(Array.isArray(JSON.parse(fs.readFileSync(path.join(dir,'campaign-stalls.trace.json'),'utf8')).traceEvents));
  await assert.rejects(collectStallAttribution(page,context,dir,99),/Invalid attribution duration/);
  console.log('test-stall-attribution: OK actual GL calls and bounded trace; exact descriptors restored');
 }finally{await browser.close();fs.rmSync(dir,{recursive:true,force:true});}
})().catch(error=>{console.error(error);process.exitCode=1;});
