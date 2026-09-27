'use strict';
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
const {intervalStats}=require('./cheerpj-paired-experiment');
async function runPacingScreen(page,out){
  const definitions={baseline:{cache:false,pacing:'none'},cache:{cache:true,pacing:'none'},flush:{cache:true,pacing:'flush'},raf:{cache:true,pacing:'raf'}};
  const order=['baseline','cache','flush','raf','raf','flush','cache','baseline'];
  const report={design:order,framesPerWindow:240,warmupFrames:24,
    note:'Exploratory screen, two reverse-order windows per configuration; not a confirmatory throughput claim.',blocks:[]};
  const file=path.join(out,'campaign-pacing-screen.json');
  try{
    for(let index=0;index<order.length;index++){
      const name=order[index],config=definitions[name];
      const warm=await page.evaluate(config=>{
        if(!window.__lwjglCampaignBoundaryStats)throw new Error('Campaign pacing experiment missing');
        window.__LWJGL_COLOR_ATTRIB_CACHE__=true;
        window.__LWJGL_EXTENDED_ATTRIB_CACHE__=config.cache;
        window.__LWJGL_CAMPAIGN_PACING__=config.pacing;
        window.__lwjglInvalidateExtendedAttribState();
        return window.__lwjglPresentationStats.swapCount;
      },config);
      await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+24,warm,{timeout:60000,polling:250});
      const before=await page.evaluate(()=>{
        window.__lwjglResetPresentationTimingWindow();
        return {at:performance.now(),swaps:window.__lwjglPresentationStats.swapCount,counters:{...window.__lwjglCampaignBoundaryStats}};
      });
      await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+241,before.swaps,{timeout:120000,polling:250});
      const after=await page.evaluate(()=>({at:performance.now(),state:window.__STARSECTOR_RUNTIME_STATE__?.state,
        raw:window.__lwjglSnapshotPresentationIntervals(),counters:{...window.__lwjglCampaignBoundaryStats},
        cache:window.__LWJGL_EXTENDED_ATTRIB_CACHE__,pacing:window.__LWJGL_CAMPAIGN_PACING__,
        canvas:[document.getElementById('lwjglCanvas').width,document.getElementById('lwjglCanvas').height]}));
      assert.equal(after.state,'campaign');assert.equal(after.cache,config.cache);assert.equal(after.pacing,config.pacing);assert.equal(after.raw.count,240);
      const counters=Object.fromEntries(Object.keys(before.counters).map(k=>[k,after.counters[k]-before.counters[k]]));
      if(name==='flush')assert.ok(counters.flushes>=241);
      if(name==='raf'){assert.ok(counters.rafRequests>=241);assert.ok(counters.rafCompleted>0);}
      const row={index,name,config,statistics:intervalStats(after.raw.intervalsMs),counters,
        wallMs:after.at-before.at,swapDelta:after.raw.swapCount-before.swaps,canvas:after.canvas,intervalsMs:after.raw.intervalsMs};
      report.blocks.push(row);fs.writeFileSync(file,JSON.stringify(report,null,2));
      console.log('[campaign-pacing-screen]',JSON.stringify({...row,intervalsMs:undefined}));
    }
    report.means={};
    for(const name of Object.keys(definitions)){
      const blocks=report.blocks.filter(row=>row.name===name);report.means[name]={};
      for(const metric of ['fps','meanMs','p95Ms','p99Ms','jitterP95Ms'])report.means[name][metric]=blocks.reduce((n,b)=>n+b.statistics[metric],0)/blocks.length;
      report.means[name].pooled=intervalStats(blocks.flatMap(b=>b.intervalsMs));
    }
    report.ok=true;
  }catch(e){report.ok=false;report.error=e.stack||String(e);throw e;}
  finally{
    await page.evaluate(()=>{window.__LWJGL_EXTENDED_ATTRIB_CACHE__=false;window.__LWJGL_CAMPAIGN_PACING__='none';window.__lwjglInvalidateExtendedAttribState?.();}).catch(()=>{});
    fs.writeFileSync(file,JSON.stringify(report,null,2));
  }
  return report;
}
module.exports={runPacingScreen};
