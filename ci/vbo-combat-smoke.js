'use strict';
const fs = require('fs');
const { chromium } = require('playwright');
const url = process.env.STARSECTOR_TEST_URL || 'http://127.0.0.1:8000/launch.html';
const timeoutMs = Math.max(60000, Number(process.env.STARSECTOR_VBO_COMBAT_TIMEOUT_MS || 300000));
const outDir = process.env.STARSECTOR_VBO_COMBAT_OUTPUT_DIR || 'test_output/vbo-combat-smoke';
fs.mkdirSync(outDir, { recursive: true });
const logs=[]; const errors=[];
let fatal=null;
function fatalLine(text){
  return /(?:uncaught|unhandled|fatal(?:\s+error)?|linkageerror|unsatisfiedlinkerror|exception in thread|cheerpj.*(?:abort|failed))/i.test(text)
    && !/(?:fallback|probe|expected|handled)/i.test(text);
}
(async()=>{
  const browser=await chromium.launch({headless:true,args:['--enable-unsafe-swiftshader']});
  try{
    const context=await browser.newContext({viewport:{width:1280,height:900},serviceWorkers:'allow'});
    const page=await context.newPage();
    page.setDefaultNavigationTimeout(60000);
    page.on('console',m=>{ const t=m.text(); logs.push(`[${m.type()}] ${t}`); if(!fatal && fatalLine(t)) fatal=t; });
    page.on('pageerror',e=>{ const t=String(e&&e.stack||e); errors.push(t); if(!fatal) fatal=t; });
    await page.addInitScript(config=>{ for(const [k,v] of Object.entries(config)) window[k]=v; },{
      __STARSECTOR_BOOT__:'combat',
      __STARSECTOR_AUTO_CAMPAIGN__:false,
      __STARSECTOR_DIRECT_LAUNCH__:false,
      __STARSECTOR_FORCE_CHEERPJ_STORAGE_RESET__:true,
      __STARSECTOR_RENDER_WIDTH__:1024,
      __STARSECTOR_RENDER_HEIGHT__:768,
      __LWJGL_FIRST_LOG_LIMIT__:512,
    });
    const started=Date.now();
    await page.goto(url,{waitUntil:'domcontentloaded'});
    let stats={}; let graphics=null;
    while(Date.now()-started<timeoutMs && !fatal){
      try{
        const state=await page.evaluate(()=>({
          stats:{...(window.__lwjglVboStats||{})},
          graphics:window.__lwjglGraphicsInfo||null,
          timing:window.__STARSECTOR_BOOT_TIMING__||null,
        }));
        stats=state.stats||{}; graphics=state.graphics;
        if(Number(stats.generated||0)>=1 && Number(stats.dataBytes||0)>0 && Number(stats.subDataCalls||0)>=1 && Number(stats.subDataBytes||0)>0 && Number(stats.vboDraws||0)>=1) break;
      } catch(e){ errors.push(`poll:${String(e)}`); }
      await page.waitForTimeout(500);
    }
    const active=Number(stats.generated||0)>=1 && Number(stats.dataBytes||0)>0 && Number(stats.subDataCalls||0)>=1 && Number(stats.subDataBytes||0)>0 && Number(stats.vboDraws||0)>=1;
    const result={ok:active&&!fatal,active,elapsedMs:Date.now()-started,stats,graphics,fatal,errors};
    fs.writeFileSync(`${outDir}/browser.log`,logs.join('\n')+'\n');
    fs.writeFileSync(`${outDir}/result.json`,JSON.stringify(result,null,2)+'\n');
    console.log(`[vbo-combat-smoke] active=${active} elapsedMs=${result.elapsedMs} generated=${Number(stats.generated||0)} dataBytes=${Number(stats.dataBytes||0)} subDataCalls=${Number(stats.subDataCalls||0)} subDataBytes=${Number(stats.subDataBytes||0)} vboDraws=${Number(stats.vboDraws||0)} fatal=${Boolean(fatal)}`);
    if(!result.ok){
      console.error(JSON.stringify(result,null,2));
      process.exitCode=1;
    }
  } finally { await browser.close(); }
})().catch(e=>{ console.error(e&&e.stack||e); process.exit(1); });
