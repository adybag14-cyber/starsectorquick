#!/usr/bin/env node
'use strict';
const fs=require('node:fs');const assert=require('node:assert/strict');
const {chromium}=require('playwright');
const {browserRendererProfile}=require('./browser-renderer-profile');
(async()=>{
 const source=fs.readFileSync(process.argv[2]||'build/final/wasm-modules/lwjgl.js','utf8');
 const marker='LWJGL_VERTEX_ARRAY_COALESCING_V1';
 const a=source.indexOf('// '+marker+'_BEGIN'),b=source.indexOf('// '+marker+'_END');
 assert.ok(a>=0&&b>a);const code=source.slice(a,b);
 assert.doesNotMatch(source,/\.bindVertexArray(?:OES)?\(/,'Cache ownership must be extended before introducing a second VAO');
 assert.equal((source.match(/glCtx\.enableVertexAttribArray\(/g)||[]).length,1,'No untracked bridge enable calls');
 assert.equal((source.match(/glCtx\.disableVertexAttribArray\(/g)||[]).length,1,'No untracked bridge disable calls');
 const browser=await chromium.launch({headless:true,args:browserRendererProfile().args});
 try{
  const page=await browser.newPage();await page.setContent('<canvas id="gl" width="32" height="32"></canvas>');
  const result=await page.evaluate(code=>new Function('code',`
   const canvas=document.getElementById('gl'),glCtx=canvas.getContext('webgl2',{antialias:false,preserveDrawingBuffer:true});
   if(!glCtx)throw new Error('WebGL2 required');
   const vertexPosition=0,colorLocation=1,texCoord=2;
   eval(code);
   let checks=0,rasterBytes=0;const same=(a,b,m)=>{checks++;if(JSON.stringify(a)!==JSON.stringify(b))throw new Error(m+JSON.stringify({actual:a,expected:b}));};
   const shader=(type,text)=>{const s=glCtx.createShader(type);glCtx.shaderSource(s,text);glCtx.compileShader(s);if(!glCtx.getShaderParameter(s,glCtx.COMPILE_STATUS))throw new Error(glCtx.getShaderInfoLog(s));return s;};
   const program=glCtx.createProgram();
   glCtx.attachShader(program,shader(glCtx.VERTEX_SHADER,'#version 300 es\\nlayout(location=0) in vec2 pos;layout(location=1) in vec4 col;layout(location=2) in vec2 uv;out vec4 c;void main(){gl_Position=vec4(pos,0,1);c=col*vec4(uv.x,uv.y,1,1);}'));
   glCtx.attachShader(program,shader(glCtx.FRAGMENT_SHADER,'#version 300 es\\nprecision highp float;in vec4 c;out vec4 o;void main(){o=c;}'));
   glCtx.linkProgram(program);if(!glCtx.getProgramParameter(program,glCtx.LINK_STATUS))throw new Error(glCtx.getProgramInfoLog(program));glCtx.useProgram(program);
   const fill=(index,size,data)=>{const buf=glCtx.createBuffer();glCtx.bindBuffer(glCtx.ARRAY_BUFFER,buf);glCtx.bufferData(glCtx.ARRAY_BUFFER,new Float32Array(data),glCtx.STATIC_DRAW);glCtx.vertexAttribPointer(index,size,glCtx.FLOAT,false,0,0);};
   fill(0,2,[-1,-1,3,-1,-1,3]);fill(1,4,[1,0,0,1,0,1,0,1,0,0,1,1]);fill(2,2,[1,1,1,.5,.5,1]);
   const realEnabled=i=>glCtx.getVertexAttrib(i,glCtx.VERTEX_ATTRIB_ARRAY_ENABLED);
   for(const enabled of [false,true]){
    window.__LWJGL_VERTEX_ARRAY_CACHE__=enabled;invalidateVertexArrayState();
    for(let k=0;k<600;k++){
     const index=k%3,value=!!((k*13+(k>>3))&1);
     setBridgeVertexArrayEnabled(index,value);same(realEnabled(index),value,'state '+k);
     setBridgeVertexArrayEnabled(index,value);same(realEnabled(index),value,'repeat '+k);
    }
   }
   const render=(cache,mask,seed)=>{
    window.__LWJGL_VERTEX_ARRAY_CACHE__=cache;invalidateVertexArrayState();
    for(let i=0;i<3;i++){
     const flag=!!(mask&(1<<i));setBridgeVertexArrayEnabled(i,flag);setBridgeVertexArrayEnabled(i,flag);
    }
    glCtx.vertexAttrib2f(0,0,0);glCtx.vertexAttrib4f(1,.1+(seed%7)/10,.6,.8,1);glCtx.vertexAttrib2f(2,.2+(seed%5)/10,.9);
    glCtx.clearColor(0,0,0,1);glCtx.clear(glCtx.COLOR_BUFFER_BIT);glCtx.drawArrays(glCtx.TRIANGLES,0,3);
    const raw=new Uint8Array(4096);glCtx.readPixels(0,0,32,32,glCtx.RGBA,glCtx.UNSIGNED_BYTE,raw);return Array.from(raw);
   };
   const diversity=new Set();
   for(let mask=0;mask<8;mask++)for(let seed=0;seed<16;seed++){
    const expected=render(false,mask,seed),actual=render(true,mask,seed);same(actual,expected,'raster '+mask+'/'+seed);rasterBytes+=actual.length;diversity.add(JSON.stringify(actual.slice(0,64)));
   }
   if(diversity.size<4)throw new Error('Insufficient nonempty drawing witnesses');
   window.__LWJGL_VERTEX_ARRAY_CACHE__=true;invalidateVertexArrayState();
   for(let i=0;i<3;i++)setBridgeVertexArrayEnabled(i,true);
   const before={...vertexArrayStats};for(let i=0;i<1000;i++)setBridgeVertexArrayEnabled(i%3,true);
   same(vertexArrayStats.driverWrites-before.driverWrites,0,'redundant writes removed');same(vertexArrayStats.coalesced-before.coalesced,1000,'branch counted');
   // Out-of-domain indices still exercise the driver on every call.
   const invalid=glCtx.getParameter(glCtx.MAX_VERTEX_ATTRIBS);
   for(const index of [-1,invalid])for(let n=0;n<2;n++){
    setBridgeVertexArrayEnabled(index,true);same(glCtx.getError(),glCtx.INVALID_VALUE,'invalid index forwarded');
   }
   setBridgeVertexArrayEnabled(3,true);same(realEnabled(3),true,'unowned valid index');setBridgeVertexArrayEnabled(3,false);
   window.__LWJGL_VERTEX_ARRAY_CACHE__=false;setBridgeVertexArrayEnabled(1,false);
   window.__LWJGL_VERTEX_ARRAY_CACHE__=true;setBridgeVertexArrayEnabled(1,true);same(realEnabled(1),true,'reenable seeds state');
   // Simulated restoration changes state behind the bridge, then dispatches the
   // same invalidation event. Actual context rebuilding is outside this cache.
   glCtx.disableVertexAttribArray(1);canvas.dispatchEvent(new Event('webglcontextrestored'));setBridgeVertexArrayEnabled(1,true);same(realEnabled(1),true,'context restoration invalidates');
   const orig=glCtx.enableVertexAttribArray;invalidateVertexArrayState();
   glCtx.enableVertexAttribArray=()=>{throw new Error('owned injection');};
   let failed=false;try{setBridgeVertexArrayEnabled(0,true);}catch(e){failed=e.message==='owned injection';}finally{glCtx.enableVertexAttribArray=orig;}
   same(failed,true,'failure propagates');setBridgeVertexArrayEnabled(0,true);same(realEnabled(0),true,'retry after known local failure');
   same(glCtx.getError(),glCtx.NO_ERROR,'no unexpected GL error');
   return {checks,rasterBytes,distinctPixelWitnesses:diversity.size,redundantWritesAvoided:1000,stats:{...vertexArrayStats}};
  `)(code),code);
  console.log('verify-lwjgl-vertex-array-cache: OK '+JSON.stringify(result));
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
