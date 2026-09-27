'use strict';
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const {intervalStats,summarizeBlocks}=require('./cheerpj-paired-experiment');
const {captureExperimentFlags,restoreExperimentFlags}=require('./renderer-experiment-state');

async function runRafPairedExperiment(page,out){
  const report={design:'ABBABAAB',baseline:'default color cache, extended cache OFF, synchronous campaign boundary',
    candidate:'same caches and scene, campaign RAF boundary only',framesPerWindow:240,warmupFrames:24,
    note:'Same-campaign presentation intervals; not monitor scan-out, GPU completion or interaction latency.',blocks:[]};
  const file=path.join(out,'campaign-raf-paired.json');
  const originalFlags=await captureExperimentFlags(page);
  try{
    const order=[false,true,true,false,true,false,false,true];
    for(let index=0;index<order.length;index++){
      const enabled=order[index];
      const warm=await page.evaluate(enabled=>{
        if(!window.__lwjglCampaignBoundaryStats)throw new Error('Campaign boundary experiment missing');
        window.__LWJGL_COLOR_ATTRIB_CACHE__=true;
        window.__LWJGL_EXTENDED_ATTRIB_CACHE__=false;
        window.__LWJGL_CAMPAIGN_PACING__=enabled?'raf':'none';
        window.__lwjglInvalidateExtendedAttribState();
        return window.__lwjglPresentationStats.swapCount;
      },enabled);
      await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+24,warm,{timeout:60000,polling:250});
      const before=await page.evaluate(()=>{
        window.__lwjglResetPresentationTimingWindow();
        return {at:performance.now(),swaps:window.__lwjglPresentationStats.swapCount,
          boundary:{...window.__lwjglCampaignBoundaryStats},canvas:[document.getElementById('lwjglCanvas').width,document.getElementById('lwjglCanvas').height]};
      });
      await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+241,before.swaps,{timeout:120000,polling:250});
      const after=await page.evaluate(()=>{
        window.__lwjglRefreshPresentationStats();
        return {at:performance.now(),state:window.__STARSECTOR_RUNTIME_STATE__?.state,visibility:document.visibilityState,
          pacing:window.__LWJGL_CAMPAIGN_PACING__,extended:window.__LWJGL_EXTENDED_ATTRIB_CACHE__,color:window.__LWJGL_COLOR_ATTRIB_CACHE__,
          boundary:{...window.__lwjglCampaignBoundaryStats},raw:window.__lwjglSnapshotPresentationIntervals(),
          reported:{...window.__lwjglPresentationStats},canvas:[document.getElementById('lwjglCanvas').width,document.getElementById('lwjglCanvas').height]};
      });
      assert.equal(after.state,'campaign');assert.equal(after.visibility,'visible');assert.equal(after.pacing,enabled?'raf':'none');
      assert.equal(after.extended,false);assert.equal(after.color,true);assert.equal(after.raw.count,240);assert.deepEqual(after.canvas,before.canvas);
      const counters=Object.fromEntries(Object.keys(before.boundary).map(k=>[k,after.boundary[k]-before.boundary[k]]));
      if(enabled){assert.ok(counters.rafRequests>=241);assert.ok(counters.rafCompleted>=counters.rafRequests*.9,'RAF path mostly timed out');}
      else{assert.equal(counters.rafRequests,0);assert.equal(counters.flushes,0);}
      const statistics=intervalStats(after.raw.intervalsMs);
      assert.ok(Math.abs(statistics.meanMs-after.reported.recentFrameMs)<1e-6);
      const row={index,enabled,statistics,counters,wallMs:after.at-before.at,swapDelta:after.raw.swapCount-before.swaps,
        canvas:after.canvas,intervalsMs:after.raw.intervalsMs};
      report.blocks.push(row);fs.writeFileSync(file,JSON.stringify(report,null,2));
      console.log('[campaign-raf-paired]',JSON.stringify({...row,intervalsMs:undefined}));
    }
    report.summary=summarizeBlocks(report.blocks);report.ok=true;
  }catch(error){report.ok=false;report.error=error.stack||String(error);throw error;}
  finally{
    try{await restoreExperimentFlags(page,originalFlags);report.flagsRestored=true;}
    catch(error){report.ok=false;report.flagsRestored=false;report.cleanupError=String(error);throw error;}
    finally{fs.writeFileSync(file,JSON.stringify(report,null,2));}
  }
  return report;
}
module.exports={runRafPairedExperiment};
