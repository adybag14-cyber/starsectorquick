'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
(async()=>{
 const source=fs.readFileSync('build/final/wasm-modules/lwjgl.js','utf8');
 const a=source.indexOf('// LWJGL_CAMPAIGN_BOUNDARY_EXPERIMENT_V1_BEGIN'),b=source.indexOf('// LWJGL_CAMPAIGN_BOUNDARY_EXPERIMENT_V1_END');
 assert.ok(a>=0&&b>a);let flushed=0,next=1;const raf=new Map(),timers=new Map();
 const context=vm.createContext({window:{},document:{visibilityState:'visible'},glCtx:{flush(){flushed++;}},
  requestAnimationFrame(fn){const id=next++;raf.set(id,fn);return id;},cancelAnimationFrame(id){raf.delete(id);},
  setTimeout(fn,ms){assert.equal(ms,100);const id=next++;timers.set(id,fn);return id;},clearTimeout(id){timers.delete(id);}});
 vm.runInContext(source.slice(a,b),context);
 const w=context.window;w.__LWJGL_CAMPAIGN_PACING__='raf';
 assert.equal(context.campaignFrameBoundary(),undefined);
 for(const state of ['waiting','title','campaign-initializing','campaign-loading','failed','fatal']){w.__STARSECTOR_RUNTIME_STATE__={state};assert.equal(context.campaignFrameBoundary(),undefined);}
 assert.equal(raf.size,0);assert.equal(timers.size,0);
 w.__STARSECTOR_RUNTIME_STATE__={state:'campaign'};w.__LWJGL_CAMPAIGN_PACING__='none';assert.equal(context.campaignFrameBoundary(),undefined);
 w.__LWJGL_CAMPAIGN_PACING__='flush';assert.equal(context.campaignFrameBoundary(),undefined);assert.equal(flushed,1);
 w.__LWJGL_CAMPAIGN_PACING__='raf';const p=context.campaignFrameBoundary();assert.ok(p&&typeof p.then==='function');assert.equal(raf.size,1);assert.equal(timers.size,1);
 const frame=[...raf.values()][0];raf.clear();frame();await p;assert.equal(timers.size,0);assert.equal(w.__lwjglCampaignBoundaryStats.rafCompleted,1);
 const p2=context.campaignFrameBoundary();const fallback=[...timers.values()][0];timers.clear();fallback();await p2;assert.equal(raf.size,0);assert.equal(w.__lwjglCampaignBoundaryStats.timerFallbacks,1);
 context.document.visibilityState='hidden';assert.equal(context.campaignFrameBoundary(),undefined);assert.equal(raf.size,0);
 context.document.visibilityState='visible';w.__LWJGL_CAMPAIGN_PACING__='invalid';assert.equal(context.campaignFrameBoundary(),undefined);
 assert.equal(w.__lwjglCampaignBoundaryStats.rafRequests,2);assert.equal(flushed,1);
 console.log('test-campaign-frame-boundary: OK startup remains synchronous; explicit modes, hidden tab and bounded pending-frame release');
})().catch(e=>{console.error(e);process.exitCode=1;});
