#!/usr/bin/env node
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { chromium } = require('playwright');

(async () => {
  const source = fs.readFileSync(process.argv[2] || 'build/final/wasm-modules/lwjgl.js', 'utf8');
  const section = marker => {
    const a = source.indexOf('// '+marker+'_BEGIN');
    const b = source.indexOf('// '+marker+'_END');
    assert.ok(a >= 0 && b > a, marker); return source.slice(a,b);
  };
  const fn = name => {
    const a = source.indexOf('function '+name+'('); assert.ok(a >= 0,name);
    const candidates = ['\nfunction ','\nvar '].map(s=>source.indexOf(s,a+10)).filter(i=>i>a);
    return source.slice(a,Math.min(...candidates));
  };
  const names = ['getCompatEnableState','setCompatEnableState','snapshotAttribState','restoreAttribState',
    'Java_org_lwjgl_opengl_GL11_nglViewport','Java_org_lwjgl_opengl_GL11_nglEnable','Java_org_lwjgl_opengl_GL11_nglDisable',
    'Java_org_lwjgl_opengl_GL11_nglClearDepth','Java_org_lwjgl_opengl_GL11_nglDepthMask','Java_org_lwjgl_opengl_GL11_nglDepthFunc',
    'Java_org_lwjgl_opengl_GL11_nglStencilFunc','Java_org_lwjgl_opengl_GL11_nglStencilOp'];
  const code = [section('LWJGL_COLOR_ATTRIB_SNAPSHOT_CACHE_V1'),section('LWJGL_EXTENDED_ATTRIB_SNAPSHOT_CACHE_V1'),...names.map(fn)].join('\n');
  const browser = await chromium.launch({headless:true,args:['--enable-unsafe-swiftshader']});
  try {
    const page=await browser.newPage();
    await page.setContent('<canvas id="gl" width="64" height="64"></canvas>');
    const report=await page.evaluate(code=>new Function('code',`
      const glCanvas=document.getElementById('gl');
      const glCtx=glCanvas.getContext('webgl2',{stencil:true,preserveDrawingBuffer:true});
      if(!glCtx)throw new Error('WebGL2 unavailable');
      const alphaTestState={enabled:false,func:519,ref:0}; let texture2DEnabled=false;
      const immediateModeData={currentColor:[1,1,1,1],currentTexCoord:[0,0]};
      const alphaTestWarnings=new Set(); const texCoordData={enabled:true};
      const texCoord=0; const textureObjects={}; let boundTexture2DId=0;
      const verboseLog=false; let curList=null;
      function syncAlphaTestUniforms(){} function setTexture2DEnabled(value){texture2DEnabled=!!value;}
      function applyCurrentColorAttrib(){} function warnOnce(){}
      function pushInList(list,args,callee){list.push({args:[...args],callee});}
      eval(code);
      let checks=0;
      const same=(a,b,label)=>{checks++;if(JSON.stringify(a)!==JSON.stringify(b))throw new Error(label+': '+JSON.stringify({actual:a,expected:b}));};
      const groups=['depth','viewport','stencil'];
      const compare=label=>{for(const group of groups)same(snapshotExtendedAttribState(group),readExtendedAttribState(group),label+'-'+group);};
      const caps=[glCtx.BLEND,glCtx.CULL_FACE,glCtx.DEPTH_TEST,glCtx.SCISSOR_TEST,glCtx.STENCIL_TEST];
      const compareEnables=label=>{for(const cap of caps)same(getCompatEnableState(cap),glCtx.isEnabled(cap),label+'-'+cap);};
      window.__LWJGL_EXTENDED_ATTRIB_CACHE__=true;
      compare('initial');compareEnables('initial');
      const compareFuncs=[512,513,514,515,516,517,518,519];
      const stencilOps=[glCtx.KEEP,glCtx.ZERO,glCtx.REPLACE,glCtx.INCR,glCtx.DECR,glCtx.INVERT,glCtx.INCR_WRAP,glCtx.DECR_WRAP];
      for(let i=0;i<160;i++){
        Java_org_lwjgl_opengl_GL11_nglClearDepth(null,(i-20)/100,0);
        Java_org_lwjgl_opengl_GL11_nglDepthMask(null,!!(i%2),0);
        Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,compareFuncs[i%8],0);
        Java_org_lwjgl_opengl_GL11_nglViewport(null,-i,i%3,i%64+1,i%48+1,0);
        Java_org_lwjgl_opengl_GL11_nglStencilFunc(null,compareFuncs[(i+1)%8],i-50,-1,0);
        Java_org_lwjgl_opengl_GL11_nglStencilOp(null,stencilOps[i%8],stencilOps[(i+1)%8],stencilOps[(i+2)%8],0);
        for(let j=0;j<caps.length;j++){
          if((i+j)%2)Java_org_lwjgl_opengl_GL11_nglEnable(null,caps[j],0);
          else Java_org_lwjgl_opengl_GL11_nglDisable(null,caps[j],0);
        }
        compare('mutated-'+i);compareEnables('mutated-'+i);
      }
      const mask=0x0100|0x0800|0x0400|0x2000;
      const outer=snapshotAttribState(mask);const saved=JSON.stringify(outer);
      Java_org_lwjgl_opengl_GL11_nglViewport(null,1,2,30,40,0);
      Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,glCtx.GREATER,0);
      const inner=snapshotAttribState(mask);
      Java_org_lwjgl_opengl_GL11_nglEnable(null,glCtx.STENCIL_TEST,0);
      Java_org_lwjgl_opengl_GL11_nglStencilOp(null,glCtx.ZERO,glCtx.ZERO,glCtx.ZERO,0);
      restoreAttribState(inner);compare('inner-restore');compareEnables('inner-restore');
      restoreAttribState(outer);compare('outer-restore');compareEnables('outer-restore');
      same(JSON.stringify(outer),saved,'independent-nested-snapshot');
      // The invalid operation still reaches GL and its error is not swallowed.
      Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,0xdead,0);same(glCtx.getError(),glCtx.INVALID_ENUM,'invalid-depth');compare('after-invalid-depth');
      Java_org_lwjgl_opengl_GL11_nglViewport(null,0,0,-1,32,0);same(glCtx.getError(),glCtx.INVALID_VALUE,'invalid-viewport');compare('after-invalid-viewport');
      Java_org_lwjgl_opengl_GL11_nglStencilFunc(null,0xdead,12,-1,0);same(glCtx.getError(),glCtx.INVALID_ENUM,'invalid-stencil');compare('after-invalid-stencil');
      Java_org_lwjgl_opengl_GL11_nglStencilOp(null,0xdead,glCtx.KEEP,glCtx.KEEP,0);same(glCtx.getError(),glCtx.INVALID_ENUM,'invalid-stencil-op');compare('after-invalid-stencil-op');
      Java_org_lwjgl_opengl_GL11_nglViewport(null,0,0,2147483647,2147483647,0);compare('viewport-driver-clamp');
      Java_org_lwjgl_opengl_GL11_nglClearDepth(null,Infinity,0);compare('infinite-depth');
      Java_org_lwjgl_opengl_GL11_nglClearDepth(null,NaN,0);compare('nan-depth');
      // List recording must not mutate shadows until its commands execute.
      const beforeList=snapshotAttribState(mask);curList=[];
      Java_org_lwjgl_opengl_GL11_nglViewport(null,0,0,44,22,0);
      Java_org_lwjgl_opengl_GL11_nglDisable(null,glCtx.BLEND,0);
      same(snapshotAttribState(mask),beforeList,'display-list-record');
      const recorded=curList;curList=null;for(const item of recorded)item.callee(...item.args);
      compare('display-list-playback');compareEnables('display-list-playback');
      window.__LWJGL_EXTENDED_ATTRIB_CACHE__=false;compare('disabled');compareEnables('disabled');
      Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,glCtx.LESS,0);
      window.__LWJGL_EXTENDED_ATTRIB_CACHE__=true;compare('reenabled');compareEnables('reenabled');
      glCtx.depthFunc(glCtx.ALWAYS);window.__lwjglInvalidateExtendedAttribState();compare('external-invalidation');
      glCanvas.dispatchEvent(new Event('webglcontextrestored'));compare('context-restore');compareEnables('context-restore');
      const before={...extendedAttribStats};for(let i=0;i<100;i++){for(const group of groups)snapshotExtendedAttribState(group);for(const cap of caps)getCompatEnableState(cap);}
      for(const key of ['depthReads','viewportReads','stencilReads','enableReads'])same(extendedAttribStats[key],before[key],'zero-readbacks-'+key);
      same(glCtx.getError(),glCtx.NO_ERROR,'no-unexpected-gl-errors');
      // Clear/readback output equality after nested depth/stencil/enable restore.
      const image=()=>{setCompatEnableState(glCtx.SCISSOR_TEST,false);glCtx.clearColor(.25,.5,.75,1);glCtx.clear(glCtx.COLOR_BUFFER_BIT);const data=new Uint8Array(64*64*4);glCtx.readPixels(0,0,64,64,glCtx.RGBA,glCtx.UNSIGNED_BYTE,data);return Array.from(data);};
      window.__LWJGL_EXTENDED_ATTRIB_CACHE__=false;const a=snapshotAttribState(mask);restoreAttribState(a);const pixelsA=image();
      window.__LWJGL_EXTENDED_ATTRIB_CACHE__=true;const b=snapshotAttribState(mask);restoreAttribState(b);same(image(),pixelsA,'framebuffer-byte-parity');
      return {checks,stats:extendedAttribStats,unchangedReadbackSnapshots:300,unchangedEnableReads:500};
    `)(code),code);
    console.log('verify-lwjgl-extended-attrib-cache: OK '+JSON.stringify(report));
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
