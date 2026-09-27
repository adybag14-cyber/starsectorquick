'use strict';
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
const {intervalStats,summarizeBlocks}=require('./cheerpj-paired-experiment');
const {captureExperimentFlags,restoreExperimentFlags}=require('./renderer-experiment-state');
async function runDepthPairedExperiment(page,out){
 const report={design:'ABBABAAB',baseline:'authoritative depth queries',candidate:'write-through depth state; same scene and all GL writes',
  framesPerWindow:240,warmupFrames:24,note:'Unprofiled application presentation, not GPU completion. Only depth-state reads differ.',blocks:[]};
 const file=path.join(out,'depth-state-paired.json');const original=await captureExperimentFlags(page);
 try{
  const order=[false,true,true,false,true,false,false,true];
  for(let index=0;index<order.length;index++){
   const enabled=order[index];
   const warm=await page.evaluate(enabled=>{
    if(!window.__lwjglDepthStateStats)throw new Error('Depth state candidate not loaded');
    window.__LWJGL_DEPTH_STATE_CACHE__=enabled;window.__LWJGL_COLOR_ATTRIB_CACHE__=true;
    window.__LWJGL_EXTENDED_ATTRIB_CACHE__=false;window.__LWJGL_CAMPAIGN_PACING__='none';
    window.__lwjglInvalidateExtendedAttribState();return window.__lwjglPresentationStats.swapCount;
   },enabled);
   await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+24,warm,{timeout:60000,polling:250});
   const before=await page.evaluate(()=>{window.__lwjglResetPresentationTimingWindow();return {at:performance.now(),swap:window.__lwjglPresentationStats.swapCount,counters:{...window.__lwjglDepthStateStats},canvas:[document.getElementById('lwjglCanvas').width,document.getElementById('lwjglCanvas').height]};});
   await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+241,before.swap,{timeout:120000,polling:250});
   const after=await page.evaluate(()=>{
    window.__lwjglRefreshPresentationStats();return {at:performance.now(),state:window.__STARSECTOR_RUNTIME_STATE__?.state,
     depth:window.__LWJGL_DEPTH_STATE_CACHE__,extended:window.__LWJGL_EXTENDED_ATTRIB_CACHE__,pacing:window.__LWJGL_CAMPAIGN_PACING__,
     counters:{...window.__lwjglDepthStateStats},raw:window.__lwjglSnapshotPresentationIntervals(),reported:{...window.__lwjglPresentationStats},canvas:[document.getElementById('lwjglCanvas').width,document.getElementById('lwjglCanvas').height]};
   });
   assert.equal(after.state,'campaign');assert.equal(after.depth,enabled);assert.equal(after.extended,false);assert.equal(after.pacing,'none');assert.equal(after.raw.count,240);assert.deepEqual(after.canvas,before.canvas);
   const counters=Object.fromEntries(Object.keys(before.counters).map(k=>[k,after.counters[k]-before.counters[k]]));
   if(enabled){assert.ok(counters.hits>0);assert.equal(counters.driverReads,0,'hot depth snapshots unexpectedly synchronize');}
   else{assert.ok(counters.driverReads>0);assert.equal(counters.hits,0);}
   const statistics=intervalStats(after.raw.intervalsMs);assert.ok(Math.abs(statistics.meanMs-after.reported.recentFrameMs)<1e-6);
   const row={index,enabled,statistics,counters,canvas:after.canvas,wallMs:after.at-before.at,swapDelta:after.raw.swapCount-before.swap,intervalsMs:after.raw.intervalsMs};
   report.blocks.push(row);fs.writeFileSync(file,JSON.stringify(report,null,2));console.log('[depth-state-paired]',JSON.stringify({...row,intervalsMs:undefined}));
  }
  report.summary=summarizeBlocks(report.blocks);report.ok=true;
 }catch(error){report.ok=false;report.error=error.stack||String(error);throw error;}
 finally{
  try{await restoreExperimentFlags(page,original);report.flagsRestored=true;}
  catch(error){report.ok=false;report.flagsRestored=false;report.cleanupError=String(error);throw error;}
  finally{fs.writeFileSync(file,JSON.stringify(report,null,2));}
 }
 return report;
}
module.exports={runDepthPairedExperiment};
