'use strict';
const fs=require('node:fs');const assert=require('node:assert/strict');const {chromium}=require('playwright');
(async()=>{
 const source=fs.readFileSync(process.argv[2]||'build/final/wasm-modules/lwjgl.js','utf8');
 const section=name=>{const a=source.indexOf('// '+name+'_BEGIN'),b=source.indexOf('// '+name+'_END');assert.ok(a>=0&&b>a,name);return source.slice(a,b);};
 const fn=name=>{const a=source.indexOf('function '+name+'(');assert.ok(a>=0,name);return source.slice(a,Math.min(...['\nfunction ','\nvar '].map(x=>source.indexOf(x,a+10)).filter(i=>i>a)));};
 const code=[section('LWJGL_COLOR_ATTRIB_SNAPSHOT_CACHE_V1'),section('LWJGL_EXTENDED_ATTRIB_SNAPSHOT_CACHE_V1'),
  ...['getCompatEnableState','setCompatEnableState','snapshotAttribState','restoreAttribState','Java_org_lwjgl_opengl_GL11_nglDepthMask','Java_org_lwjgl_opengl_GL11_nglDepthFunc','Java_org_lwjgl_opengl_GL11_nglClearDepth'].map(fn)].join('\n');
 const depthWrites=source.split('\n').filter(line=>/glCtx\.(?:depthMask|depthFunc|clearDepth)\(/.test(line));
 assert.equal(depthWrites.length,6,'New depth write requires mirror review');
 const browser=await chromium.launch({headless:true,args:['--enable-unsafe-swiftshader']});
 try{
  const page=await browser.newPage();await page.setContent('<canvas width="32" height="32"></canvas>');
  const result=await page.evaluate(code=>new Function('code',`
   const canvas=document.querySelector('canvas'),glCtx=canvas.getContext('webgl2',{depth:true,antialias:false,preserveDrawingBuffer:true});if(!glCtx)throw new Error('No WebGL2');
   const alphaTestState={enabled:false,func:519,ref:0};let texture2DEnabled=false;let curList=null;
   function syncAlphaTestUniforms(){}function setTexture2DEnabled(v){texture2DEnabled=!!v;}function pushInList(list,args,callee){list.push({args:[...args],callee});}
   eval(code);
   let checks=0;const same=(a,b,label)=>{checks++;if(JSON.stringify(a)!==JSON.stringify(b))throw new Error(label+' '+JSON.stringify({a,b}));};
   const compare=label=>{const actual=snapshotDepthState(),expected=readDepthStateFromDriver();same(actual,expected,label);checks++;if(!Object.is(actual.clearValue,expected.clearValue))throw new Error('clear depth bit semantics '+label);};
   window.__LWJGL_EXTENDED_ATTRIB_CACHE__=false;window.__LWJGL_DEPTH_STATE_CACHE__=true;compare('initial driver state');
   for(let i=0;i<320;i++){
    Java_org_lwjgl_opengl_GL11_nglDepthMask(null,!!(i%3),0);Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,512+i%8,0);
    Java_org_lwjgl_opengl_GL11_nglClearDepth(null,(i-55)/137,0);compare('mutation '+i);
   }
   const reads=depthStateStats.driverReads;
   for(let i=0;i<200;i++){
    Java_org_lwjgl_opengl_GL11_nglDepthMask(null,!!(i%2),0);Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,512+i%8,0);
    Java_org_lwjgl_opengl_GL11_nglClearDepth(null,i/200,0);snapshotDepthState();
   }
   same(depthStateStats.driverReads,reads,'writes do not force driver synchronization');
   const outer=snapshotAttribState(0x100),serialized=JSON.stringify(outer);
   Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,glCtx.NEVER,0);const inner=snapshotAttribState(0x100);
   Java_org_lwjgl_opengl_GL11_nglClearDepth(null,.125,0);restoreAttribState(inner);compare('nested restore');restoreAttribState(outer);compare('outer restore');same(JSON.stringify(outer),serialized,'snapshot ownership');
   for(const invalid of [511,520,0xdead,-1]){Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,invalid,0);same(glCtx.getError(),glCtx.INVALID_ENUM,'invalid func delivered');compare('authoritative after invalid');}
   for(const special of [-Infinity,Infinity,NaN,-0,0,0.123456789]){Java_org_lwjgl_opengl_GL11_nglClearDepth(null,special,0);compare('special clear '+special);}
   const original=snapshotDepthState();curList=[];Java_org_lwjgl_opengl_GL11_nglDepthMask(null,false,0);same(snapshotDepthState(),original,'display list does not mutate immediately');
   const list=curList;curList=null;for(const row of list)row.callee(...row.args);compare('display list playback');
   window.__LWJGL_DEPTH_STATE_CACHE__=false;compare('disabled path');Java_org_lwjgl_opengl_GL11_nglClearDepth(null,.75,0);window.__LWJGL_DEPTH_STATE_CACHE__=true;compare('re-enabled seed');
   glCtx.depthFunc(glCtx.NEVER);window.__lwjglInvalidateExtendedAttribState();compare('external invalidation');
   canvas.dispatchEvent(new Event('webglcontextrestored'));compare('context restore');
   // Draw real depth-tested fragments; cached and original state must produce
   // identical nonempty pixels through nested depth-group restoration.
   const shader=(type,text)=>{const s=glCtx.createShader(type);glCtx.shaderSource(s,text);glCtx.compileShader(s);if(!glCtx.getShaderParameter(s,glCtx.COMPILE_STATUS))throw new Error(glCtx.getShaderInfoLog(s));return s;};
   const program=glCtx.createProgram();glCtx.attachShader(program,shader(glCtx.VERTEX_SHADER,'#version 300 es\\nuniform float z;void main(){vec2 p=vec2(float((gl_VertexID<<1)&2),float(gl_VertexID&2));gl_Position=vec4(p*2.-1.,z,1);}'));
   glCtx.attachShader(program,shader(glCtx.FRAGMENT_SHADER,'#version 300 es\\nprecision highp float;uniform vec4 tint;out vec4 c;void main(){c=tint;}'));glCtx.linkProgram(program);if(!glCtx.getProgramParameter(program,glCtx.LINK_STATUS))throw new Error(glCtx.getProgramInfoLog(program));glCtx.useProgram(program);
   const z=glCtx.getUniformLocation(program,'z'),tint=glCtx.getUniformLocation(program,'tint');const draw=(r,g,b,depth)=>{glCtx.uniform1f(z,depth);glCtx.uniform4f(tint,r,g,b,1);glCtx.drawArrays(glCtx.TRIANGLES,0,3);};
   const fixture=(enabled,i)=>{
    window.__LWJGL_DEPTH_STATE_CACHE__=enabled;invalidateDepthState();setCompatEnableState(glCtx.DEPTH_TEST,true);setCompatEnableState(glCtx.BLEND,false);
    Java_org_lwjgl_opengl_GL11_nglDepthMask(null,true,0);Java_org_lwjgl_opengl_GL11_nglClearDepth(null,1,0);glCtx.clearColor(0,0,0,1);glCtx.clear(glCtx.DEPTH_BUFFER_BIT|glCtx.COLOR_BUFFER_BIT);
    Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,glCtx.LESS,0);draw(1,0,0,.2);const saved=snapshotAttribState(0x100);
    Java_org_lwjgl_opengl_GL11_nglDepthFunc(null,512+i%8,0);Java_org_lwjgl_opengl_GL11_nglDepthMask(null,i%2===0,0);draw(0,1,0,.7);
    restoreAttribState(saved);draw(0,0,1,.4);const bytes=new Uint8Array(4096);glCtx.readPixels(0,0,32,32,glCtx.RGBA,glCtx.UNSIGNED_BYTE,bytes);return Array.from(bytes);
   };
   let rasterBytes=0;const colors=new Set();
   for(let i=0;i<64;i++){const a=fixture(false,i),b=fixture(true,i);same(b,a,'depth pixel parity '+i);rasterBytes+=b.length;colors.add(b.slice(0,4).join(','));}
   if(colors.size<2)throw new Error('Trivial raster fixture');same(glCtx.getError(),glCtx.NO_ERROR,'no unexpected GL errors');
   return {checks,rasterBytes,stats:depthStateStats};
  `)(code),code);console.log('verify-lwjgl-depth-state: OK '+JSON.stringify(result));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
