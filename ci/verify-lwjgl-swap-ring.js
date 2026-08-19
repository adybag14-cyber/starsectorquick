'use strict';
const fs=require('fs'),vm=require('vm');
function extract(src,name){const start=src.indexOf(`function ${name}(`);if(start<0)throw new Error(`missing ${name}`);const brace=src.indexOf('{',start);let d=0,q=null,e=false;for(let i=brace;i<src.length;i++){const c=src[i];if(q){if(e)e=false;else if(c==='\\')e=true;else if(c===q)q=null;continue;}if(c==='"'||c==="'"||c==='`'){q=c;continue;}if(c==='{')d++;else if(c==='}'&&--d===0)return src.slice(start,i+1);}throw new Error('unterminated');}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-swap-ring.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8'); expect(src.includes('WEBGL_RECENT_SWAP_RING_V1'),'marker missing'); expect(!src.includes('recentSwapTimes.shift()'),'Array.shift remains');
const fn=extract(src,'recordRecentSwapTime');
const stats={recentFps:0,recentFrameMs:0}; const ctx=vm.createContext({recentSwapTimes:new Float64Array(121),recentSwapTimeCount:0,recentSwapTimeNext:0,presentationStats:stats,Float64Array}); vm.runInContext(fn,ctx);
const old=[]; let t=0,seed=0x5a17c0de; function rand(){seed=(Math.imul(seed,1664525)+1013904223)>>>0;return seed/4294967296;}
for(let i=0;i<10000;i++){t+=5+rand()*220+(i%317===0?500:0); old.push(t); if(old.length>121)old.shift(); ctx.recordRecentSwapTime(t); if(old.length<2){expect(ctx.presentationStats.recentFps===0&&ctx.presentationStats.recentFrameMs===0,`warmup ${i}`);continue;} const dur=old[old.length-1]-old[0]; const fps=dur>0?(old.length-1)*1000/dur:0; const ms=dur>0?dur/(old.length-1):0; expect(Math.abs(ctx.presentationStats.recentFps-fps)<1e-12,`fps mismatch ${i}`); expect(Math.abs(ctx.presentationStats.recentFrameMs-ms)<1e-12,`ms mismatch ${i}`);}
expect(ctx.recentSwapTimeCount===121,'ring count'); expect(ctx.recentSwapTimeNext===10000%121,'ring cursor'); console.log(`verify-lwjgl-swap-ring: OK samples=10000 count=${ctx.recentSwapTimeCount} cursor=${ctx.recentSwapTimeNext}`);