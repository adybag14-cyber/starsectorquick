#!/usr/bin/env node
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { chromium } = require('playwright');

(async () => {
  const source = fs.readFileSync(process.argv[2] || 'build/final/wasm-modules/lwjgl.js','utf8');
  const begin = source.indexOf('// LWJGL_COLOR_ATTRIB_SNAPSHOT_CACHE_V1_BEGIN');
  const end = source.indexOf('// LWJGL_COLOR_ATTRIB_SNAPSHOT_CACHE_V1_END');
  assert.ok(begin >= 0 && end > begin);
  const functionSource = name => {
    const start = source.indexOf('function '+name+'(');
    assert.ok(start >= 0, name);
    let next = source.indexOf('\nfunction ',start+10);
    const nextVariable = source.indexOf('\nvar ',start+10);
    if (nextVariable >= 0 && (next < 0 || nextVariable < next)) next = nextVariable;
    assert.ok(next > start, name);
    return source.slice(start,next);
  };
  const extraBegin = source.indexOf('// LWJGL_EXTENDED_ATTRIB_SNAPSHOT_CACHE_V1_BEGIN');
  const extraEnd = source.indexOf('// LWJGL_EXTENDED_ATTRIB_SNAPSHOT_CACHE_V1_END');
  const extra = extraBegin < 0 ? '' : source.slice(extraBegin, extraEnd);
  const code = source.slice(begin,end) + '\n' + extra + '\n' + [
    'snapshotAttribState','restoreAttribState','Java_org_lwjgl_opengl_GL11_nglClearColor',
    'Java_org_lwjgl_opengl_GL11_nglBlendFunc','Java_org_lwjgl_opengl_GL11_nglColorMask',
  ].map(functionSource).join('\n');
  const browser = await chromium.launch({headless:true,args:['--enable-unsafe-swiftshader']});
  try {
    const page=await browser.newPage();
    await page.setContent('<canvas id="gl" width="32" height="32"></canvas>');
    const report=await page.evaluate(code => {
      return new Function('code', `
        const glCanvas=document.getElementById('gl');
        const glCtx=glCanvas.getContext('webgl2');
        if(!glCtx)throw new Error('WebGL2 unavailable');
        const alphaTestState={func:519,ref:0}; const curList=null;
        function checkNoList(){} function syncAlphaTestUniforms(){}
        eval(code);
        const checks=[];
        const same=(actual,expected,label)=>{
          if(JSON.stringify(actual)!==JSON.stringify(expected))throw new Error(label+': '+JSON.stringify({actual,expected}));
          checks.push(label);
        };
        const snapshot=()=>snapshotAttribState(0x4000).color;
        const compare=label=>same(snapshot(),readColorAttribState(),label);
        window.__LWJGL_COLOR_ATTRIB_CACHE__=true;
        compare('initial-state');
        const factors=[glCtx.ZERO,glCtx.ONE,glCtx.SRC_COLOR,glCtx.ONE_MINUS_SRC_COLOR,
          glCtx.SRC_ALPHA,glCtx.ONE_MINUS_SRC_ALPHA,glCtx.DST_ALPHA,glCtx.ONE_MINUS_DST_ALPHA,
          glCtx.DST_COLOR,glCtx.ONE_MINUS_DST_COLOR];
        for(let i=0;i<120;i++){
          Java_org_lwjgl_opengl_GL11_nglBlendFunc(null,factors[i%factors.length],factors[(i*3+1)%factors.length],0);
          Java_org_lwjgl_opengl_GL11_nglClearColor(null,(i-30)/100,1.7-(i/50),0.123456789,0.75,0);
          Java_org_lwjgl_opengl_GL11_nglColorMask(null,i%2,1,0,i%3,0);
          compare('mutation-'+i);
        }
        const outer=snapshotAttribState(0x4000);
        const saved=JSON.stringify(outer);
        Java_org_lwjgl_opengl_GL11_nglClearColor(null,0.99,0.88,0.77,1,0);
        const inner=snapshotAttribState(0x4000);
        Java_org_lwjgl_opengl_GL11_nglBlendFunc(null,glCtx.ONE,glCtx.ZERO,0);
        restoreAttribState(inner); compare('inner-restored');
        restoreAttribState(outer); compare('outer-restored');
        same(JSON.stringify(outer),saved,'snapshot-arrays-do-not-alias');
        // Valid uncommon factors retain authoritative fallback rather than
        // duplicating the full WebGL enum/error semantics in the fast path.
        Java_org_lwjgl_opengl_GL11_nglBlendFunc(null,glCtx.CONSTANT_COLOR,glCtx.ZERO,0);
        compare('uncommon-enum-fallback');
        Java_org_lwjgl_opengl_GL11_nglBlendFunc(null,0xdead,glCtx.ZERO,0);
        if(glCtx.getError()!==glCtx.INVALID_ENUM)throw new Error('expected invalid enum');
        compare('invalid-enum-preserves-real-state');
        const errors=glCtx.getError(); if(errors!==glCtx.NO_ERROR)throw new Error('unexpected WebGL error '+errors);
        window.__LWJGL_COLOR_ATTRIB_CACHE__=false; compare('uncached-mode');
        Java_org_lwjgl_opengl_GL11_nglClearColor(null,0.25,0.5,0.75,1,0);
        window.__LWJGL_COLOR_ATTRIB_CACHE__=true; compare('re-enabled-mode-reseeds');
        glCanvas.dispatchEvent(new Event('webglcontextrestored')); compare('context-restore-reseeds');
        const before={...colorAttribCacheStats}; for(let i=0;i<100;i++)snapshot();
        if(colorAttribCacheStats.readbacks!==before.readbacks)throw new Error('cached snapshots still query GL');
        if(colorAttribCacheStats.hits-before.hits!==100)throw new Error('cached branch was not exercised');
        return {checks:checks.length,cacheHits:colorAttribCacheStats.hits,zeroReadbackCachedSnapshots:100};
      `)(code);
    },code);
    console.log('verify-lwjgl-color-attrib-cache: OK '+JSON.stringify(report));
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
