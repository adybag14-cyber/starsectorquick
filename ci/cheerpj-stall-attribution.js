'use strict';
const fs=require('node:fs');
const path=require('node:path');

// Observer-heavy attribution is deliberately AFTER all normal timing windows.
// Restores exact method descriptors; this is not a production optimization.
async function collectStallAttribution(page,context,out,durationMs=12000){
  if(!Number.isInteger(durationMs)||durationMs<100||durationMs>15000)throw new Error("Invalid attribution duration");
  const report={durationMs,scope:'instrumented attribution, not an FPS benchmark',trace:null};
  const session=await context.newCDPSession(page);
  let tracing=false,installed=false;
  try{
    await session.send('Performance.enable');
    const before=(await session.send('Performance.getMetrics')).metrics;
    report.before=Object.fromEntries(before.map(x=>[x.name,x.value]));
    const ready=await page.evaluate(()=>{
      const canvas=document.getElementById('lwjglCanvas');
      if(!canvas)throw new Error('No real game canvas');
      if(window.__starsectorStallAttribution)throw new Error('Attribution already installed');
      const gl=canvas.getContext('webgl2');
      const methods=['getParameter','isEnabled','bufferData','bufferSubData','drawArrays','drawElements','blitFramebuffer',
        'bindFramebuffer','bindBuffer','bindTexture','enable','disable','uniformMatrix4fv','uniform1f','vertexAttribPointer',
        'enableVertexAttribArray','disableVertexAttribArray','clear','flush','finish','getError','readPixels'];
      const saved=[];const totals=Object.create(null);const frames=[];
      let lastSwap=window.__lwjglPresentationStats.swapCount;
      let current={swap:lastSwap,start:performance.now(),calls:0,glMs:0,slowestMs:0,slowest:null};
      let overflow=false;
      for(const name of methods){
        const original=gl[name];if(typeof original!=='function')continue;
        const own=Object.getOwnPropertyDescriptor(gl,name);
        const replacement=function(...args){
          const now=performance.now(),swap=window.__lwjglPresentationStats.swapCount;
          if(swap!==lastSwap){
            current.end=now;current.elapsedMs=now-current.start;
            if(frames.length<2048)frames.push(current);else overflow=true;
            lastSwap=swap;current={swap,start:now,calls:0,glMs:0,slowestMs:0,slowest:null};
          }
          const key=name==='getParameter'||name==='isEnabled'?name+':'+Number(args[0]).toString(16):name;
          let value;
          try{return value=Reflect.apply(original,this,args);}
          finally{
            const dt=performance.now()-now;
            let row=totals[key];if(!row)row=totals[key]={calls:0,ms:0,maxMs:0};
            row.calls++;row.ms+=dt;row.maxMs=Math.max(row.maxMs,dt);
            current.calls++;current.glMs+=dt;
            if(dt>current.slowestMs){current.slowestMs=dt;current.slowest=key;}
          }
        };
        Object.defineProperty(gl,name,{value:replacement,writable:true,configurable:true});
        saved.push({name,own,replacement});
      }
      const start=performance.now();
      window.__starsectorStallAttribution={finish:()=>{
        let restored=0;
        for(const row of saved){
          if(gl[row.name]!==row.replacement)throw new Error('GL method changed during attribution: '+row.name);
          if(row.own)Object.defineProperty(gl,row.name,row.own);else delete gl[row.name];
          restored++;
        }
        const result={start,end:performance.now(),totals,frames,overflow,restored,expected:saved.length,
          swaps:window.__lwjglPresentationStats.swapCount,canvas:[canvas.width,canvas.height]};
        delete window.__starsectorStallAttribution;
        return result;
      }};
      return {start,swap:lastSwap,canvas:[canvas.width,canvas.height]};
    });
    installed=true;report.start=ready;
    const completion=new Promise(resolve=>session.once('Tracing.tracingComplete',resolve));
    await session.send('Tracing.start',{categories:'devtools.timeline,v8,gpu,blink.user_timing,disabled-by-default-devtools.timeline.frame',
      options:'record-as-much-as-possible',transferMode:'ReturnAsStream'});
    tracing=true;
    await page.waitForTimeout(report.durationMs);
    report.webgl=await page.evaluate(()=>window.__starsectorStallAttribution.finish());
    installed=false;
    const after=(await session.send('Performance.getMetrics')).metrics;
    report.after=Object.fromEntries(after.map(x=>[x.name,x.value]));
    report.metricDelta=Object.fromEntries(['TaskDuration','ScriptDuration','LayoutDuration','RecalcStyleDuration','LayoutCount','RecalcStyleCount']
      .map(k=>[k,report.after[k]-report.before[k]]));
    await session.send('Tracing.end');tracing=false;
    const {stream}=await completion;
    if(!stream)throw new Error('No trace stream');
    let bytes=0;const chunks=[];
    try{
      while(true){
        const row=await session.send('IO.read',{handle:stream,size:262144});
        const chunk=Buffer.from(row.data,row.base64Encoded?'base64':'utf8');bytes+=chunk.length;
        if(bytes>64*1024*1024)throw new Error('Trace exceeded 64MiB bound');
        chunks.push(chunk);if(row.eof)break;
      }
    }finally{await session.send('IO.close',{handle:stream});}
    fs.writeFileSync(path.join(out,'campaign-stalls.trace.json'),Buffer.concat(chunks));
    report.trace={bytes,file:'campaign-stalls.trace.json'};
    report.ok=!report.webgl.overflow&&report.webgl.restored===report.webgl.expected;
    if(!report.ok)throw new Error('Incomplete WebGL attribution');
  }catch(error){report.ok=false;report.error=error.stack||String(error);throw error;}
  finally{
    try{
      if(installed)report.cleanup=await page.evaluate(()=>window.__starsectorStallAttribution?.finish());
      if(tracing)await session.send('Tracing.end');
    }finally{
      await session.detach().catch(()=>{});
      fs.writeFileSync(path.join(out,'stall-attribution.json'),JSON.stringify(report,null,2));
    }
  }
  return report;
}
module.exports={collectStallAttribution};
