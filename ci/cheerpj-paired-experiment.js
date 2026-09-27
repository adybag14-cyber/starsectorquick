'use strict';
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

function percentile(sorted, fraction) {
  if (!sorted.length) throw new Error('Empty interval sample');
  return sorted[Math.max(0, Math.min(sorted.length - 1, Math.ceil(sorted.length * fraction) - 1))];
}
function intervalStats(values) {
  if (!Array.isArray(values) || !values.length || values.some(x=>!Number.isFinite(x)||x<0))
    throw new Error('Invalid raw frame intervals');
  const sorted=[...values].sort((a,b)=>a-b);
  const mean=values.reduce((a,b)=>a+b,0)/values.length;
  const middle=percentile(sorted,.5);
  return {samples:values.length,fps:mean>0?1000/mean:0,meanMs:mean,p50Ms:middle,
    p95Ms:percentile(sorted,.95),p99Ms:percentile(sorted,.99),maxMs:sorted.at(-1),
    jitterP95Ms:percentile(values.map(x=>Math.abs(x-middle)).sort((a,b)=>a-b),.95)};
}
function summarizeBlocks(blocks) {
  if (blocks.length !== 8 || blocks.some((b,i)=>b.enabled!==[false,true,true,false,true,false,false,true][i]))
    throw new Error('Expected the complete prespecified ABBABAAB experiment');
  const median=values=>{const v=[...values].sort((a,b)=>a-b);return(v[(v.length-1)>>1]+v[v.length>>1])/2;};
  const result={baseline:{},candidate:{},changePercent:{},pairedChanges:[]};
  for(const metric of ['fps','meanMs','p95Ms','p99Ms','jitterP95Ms']) {
    const a=median(blocks.filter(b=>!b.enabled).map(b=>b.statistics[metric]));
    const b=median(blocks.filter(b=>b.enabled).map(b=>b.statistics[metric]));
    result.baseline[metric]=a;result.candidate[metric]=b;result.changePercent[metric]=a?(b-a)*100/a:null;
  }
  for(let i=0;i<8;i+=2) {
    const a=blocks.slice(i,i+2).find(b=>!b.enabled).statistics;
    const b=blocks.slice(i,i+2).find(b=>b.enabled).statistics;
    result.pairedChanges.push({pair:i/2,fpsPercent:(b.fps-a.fps)*100/a.fps,
      p95Ms:b.p95Ms-a.p95Ms,p99Ms:b.p99Ms-a.p99Ms});
  }
  result.pooled={baseline:intervalStats(blocks.filter(b=>!b.enabled).flatMap(b=>b.intervalsMs)),
    candidate:intervalStats(blocks.filter(b=>b.enabled).flatMap(b=>b.intervalsMs))};
  return result;
}

async function runPairedExperiment(page, out) {
  const report={design:'ABBABAAB',baseline:'PR183 color cache only',candidate:'extended attribute snapshot cache',
    framesPerWindow:240,warmupFrames:24,measurement:'last 240 consecutive presentation intervals at prespecified polling stop',
    note:'Application presentation, not GPU completion or monitor scan-out; profiler and screenshots are outside all timed windows.',blocks:[]};
  const file=path.join(out,'extended-attrib-ab.json');
  try {
    const order=[false,true,true,false,true,false,false,true];
    for(let index=0;index<order.length;index++) {
      const enabled=order[index];
      const warm=await page.evaluate(enabled=>{
        if(!window.__lwjglExtendedAttribStats || typeof window.__lwjglSnapshotPresentationIntervals!=='function')
          throw new Error('Extended renderer experiment is not loaded');
        window.__LWJGL_COLOR_ATTRIB_CACHE__=true;
        window.__LWJGL_EXTENDED_ATTRIB_CACHE__=enabled;
        window.__lwjglInvalidateExtendedAttribState();
        return window.__lwjglPresentationStats.swapCount;
      },enabled);
      await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+24,warm,{timeout:60000,polling:250});
      const before=await page.evaluate(()=>{
        window.__lwjglResetPresentationTimingWindow();
        return {at:performance.now(),swaps:window.__lwjglPresentationStats.swapCount,
          counters:{...window.__lwjglExtendedAttribStats},
          canvas:[document.getElementById('lwjglCanvas').width,document.getElementById('lwjglCanvas').height]};
      });
      await page.waitForFunction(start=>window.__lwjglPresentationStats.swapCount>=start+241,before.swaps,{timeout:120000,polling:250});
      const after=await page.evaluate(()=>{
        window.__lwjglRefreshPresentationStats();
        return {at:performance.now(),state:window.__STARSECTOR_RUNTIME_STATE__?.state,
          enabled:window.__LWJGL_EXTENDED_ATTRIB_CACHE__,colorCache:window.__LWJGL_COLOR_ATTRIB_CACHE__,
          counters:{...window.__lwjglExtendedAttribStats},raw:window.__lwjglSnapshotPresentationIntervals(),
          reported:{...window.__lwjglPresentationStats},
          canvas:[document.getElementById('lwjglCanvas').width,document.getElementById('lwjglCanvas').height]};
      });
      assert.equal(after.state,'campaign');assert.equal(after.enabled,enabled);assert.equal(after.colorCache,true);
      assert.deepEqual(after.canvas,before.canvas);assert.equal(after.raw.count,240);
      const statistics=intervalStats(after.raw.intervalsMs);
      assert.ok(Math.abs(statistics.meanMs-after.reported.recentFrameMs)<1e-6,'raw mean disagrees with runtime');
      assert.equal(statistics.p95Ms,after.reported.frameP95Ms);assert.equal(statistics.p99Ms,after.reported.frameP99Ms);
      const delta=Object.fromEntries(Object.keys(before.counters).map(key=>[key,after.counters[key]-before.counters[key]]));
      const hits=delta.depthHits+delta.viewportHits+delta.stencilHits+delta.enableHits;
      const reads=delta.depthReads+delta.viewportReads+delta.stencilReads+delta.enableReads;
      assert.ok(enabled?hits>0:reads>0,'selected experiment branch was not exercised');
      if(!enabled)assert.equal(hits,0);
      const block={index,enabled,canvas:after.canvas,wallMs:after.at-before.at,
        swapDelta:after.raw.swapCount-before.swaps,counters:delta,statistics,intervalsMs:after.raw.intervalsMs};
      report.blocks.push(block);
      // Preserve partial negative results if a later gate/browser operation fails.
      fs.writeFileSync(file,JSON.stringify(report,null,2));
      console.log('[extended-attrib-ab]',JSON.stringify({...block,intervalsMs:undefined}));
    }
    report.summary=summarizeBlocks(report.blocks);report.ok=true;
  } catch(error) {report.ok=false;report.error=error.stack||String(error);throw error;}
  finally {
    await page.evaluate(()=>{
      window.__LWJGL_COLOR_ATTRIB_CACHE__=true;window.__LWJGL_EXTENDED_ATTRIB_CACHE__=true;
      window.__lwjglInvalidateExtendedAttribState?.();
    }).catch(()=>{});
    fs.writeFileSync(file,JSON.stringify(report,null,2));
  }
  return report;
}
module.exports={percentile,intervalStats,summarizeBlocks,runPairedExperiment};
