'use strict';
const fs=require('fs'); const vm=require('vm');
function extract(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0)throw new Error(`missing ${name}`);
  const brace=source.indexOf('{',start); let depth=0,quote=null,esc=false;
  for(let i=brace;i<source.length;i++){ const ch=source[i]; if(quote){if(esc)esc=false;else if(ch==='\\\\')esc=true;else if(ch===quote)quote=null;continue;} if(ch==='"'||ch==="'"||ch==='`'){quote=ch;continue;} if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return source.slice(start,i+1); }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
function near(a,b){return Math.abs(a-b)<1e-6;}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-compact-color-setter.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
for(const marker of ['WEBGL_IMMEDIATE_COMPACT_COLOR_SETTER_V2','__LWJGL_IMMEDIATE_COMPACT_COLOR_SETTER__','immediateCompactColorHitObserved']) expect(src.includes(marker),`missing ${marker}`);
const appendSrc=extract(src,'appendImmediateVertex');
expect(!appendSrc.includes('batch[0] ===')&&!appendSrc.includes('batch[0] !=='),'per-vertex color comparison reintroduced');
const markSrc=extract(src,'markImmediateCompactColorChange');
expect(markSrc.includes('batch[0] === r'),'setter-driven equality check missing');
expect(extract(src,'Java_org_lwjgl_opengl_GL11_nglColor4f').includes('markImmediateCompactColorChange(r, g, b, a)'),'glColor4f tracking missing');
expect(extract(src,'Java_org_lwjgl_opengl_GL11_nglColor3f').includes('markImmediateCompactColorChange(r, g, b, 1)'),'glColor3f tracking missing');
const names=['ensureImmediateArrayCapacity','markImmediateCompactColorChange','Java_org_lwjgl_opengl_GL11_nglColor4f','Java_org_lwjgl_opengl_GL11_nglColor3f','Java_org_lwjgl_opengl_GL11_nglBegin','Java_org_lwjgl_opengl_GL11_nglTexCoord2f','appendImmediateVertex','Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord','Java_org_lwjgl_opengl_GL11_nglVertex3f','uploadImmediateInterleaved','uploadImmediateCompactColor','Java_org_lwjgl_opengl_GL11_nglEnd'];
const code=names.map(n=>extract(src,n)).join('\n');
function make(compact=true){
  const calls=[],draws=[];
  const glCtx={ARRAY_BUFFER:0x8892,STATIC_DRAW:0x88E4,FLOAT:0x1406,NO_ERROR:0,
    bindBuffer:(...a)=>calls.push(['bind',...a]), bufferData:(t,d,u)=>calls.push(['data',t,Array.from(d),u]),
    vertexAttribPointer:(...a)=>calls.push(['ptr',...a]), enableVertexAttribArray:(...a)=>calls.push(['enable',...a]), disableVertexAttribArray:(...a)=>calls.push(['disable',...a]),
    vertexAttrib4f:(...a)=>calls.push(['attrib4f',...a]), vertexAttrib2f:(...a)=>calls.push(['attrib2f',...a]), getError:()=>0};
  const c=vm.createContext({glCtx, immediateInterleavedEnabled:true, immediateCompactColorSetterEnabled:compact, curList:null, pushInList(){throw new Error('unexpected list');}, verboseLog:false,
    immediateModeData:{mode:0,vertexBuf:new Float32Array(32),vertexPos:0,colorBuf:new Float32Array(32),colorPos:0,currentColor:[1,1,1,1],currentTexCoord:[0,0],texCoordBuf:new Float32Array(32),texCoordPos:0,interleavedBuf:new Float32Array(96),interleavedPos:0,compactColorBuf:new Float32Array(64),compactColorPos:0,compactBatchColor:[1,1,1,1],compactColorVaried:false,compactVertexCount:0,immediateBeginActive:false},
    presentationStats:{immediateInterleavedDraws:0,immediateInterleavedUploads:0,immediateInterleavedUploadsSaved:0,immediateInterleavedBytes:0,immediateCompactColorHitObserved:false,immediateCompactColorFallbackObserved:false},
    vertexBuffer:{id:'v'},colorBuffer:{id:'c'},texCoordBuffer:{id:'t'},vertexPosition:3,colorLocation:4,texCoord:5, strictWebGLValidation:false,clientArrayWarnings:new Set(),warnOnce(){},
    applyCurrentColorAttrib(){}, uploadDataImpl(){throw new Error('unexpected legacy upload');}, drawArraysImpl:(mode,first,count)=>draws.push({mode,first,count}), Float32Array,Math,console});
  vm.runInContext(code,c); return {c,calls,draws};
}
// Uniform color, including a repeated identical glColor call after a vertex: compact stays active.
let x=make(true), c=x.c;
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,8,0);
c.Java_org_lwjgl_opengl_GL11_nglColor4f(null,.1,.2,.3,.4,0);
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,1,2,3,.25,.5,0);
c.Java_org_lwjgl_opengl_GL11_nglColor4f(null,.1,.2,.3,.4,0);
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,4,5,6,.75,1,0);
c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
let data=x.calls.filter(a=>a[0]==='data'); expect(data.length===1,'uniform upload count');
let vals=data[0][2], exp=[1,2,3,.25,.5,4,5,6,.75,1]; expect(vals.length===10,`uniform float count ${vals.length}`); exp.forEach((v,i)=>expect(near(vals[i],v),`uniform float ${i}`));
let ptr=x.calls.filter(a=>a[0]==='ptr'); expect(ptr.length===2,'compact pointer count'); expect(ptr[0][2]===3&&ptr[0][5]===20&&ptr[0][6]===0,'compact position layout'); expect(ptr[1][2]===2&&ptr[1][5]===20&&ptr[1][6]===12,'compact tex layout');
expect(x.calls.some(a=>a[0]==='disable'&&a[1]===4),'compact color array not disabled');
let colorCall=x.calls.filter(a=>a[0]==='attrib4f').at(-1); expect(colorCall&&near(colorCall[2],.1)&&near(colorCall[3],.2)&&near(colorCall[4],.3)&&near(colorCall[5],.4),'compact constant color mismatch');
expect(c.presentationStats.immediateCompactColorHitObserved===true,'compact hit flag'); expect(c.presentationStats.immediateCompactColorFallbackObserved===false,'unexpected uniform fallback'); expect(c.presentationStats.immediateInterleavedBytes===40,'compact byte accounting'); expect(c.immediateModeData.compactColorVaried===false,'uniform draw marked varying');
// A genuine mid-draw color change backfills prior compact vertices exactly once and uses stock full layout.
x=make(true); c=x.c; c.Java_org_lwjgl_opengl_GL11_nglBegin(null,8,0); c.Java_org_lwjgl_opengl_GL11_nglColor4f(null,1,0,0,1,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,10,11,12,.1,.2,0); c.Java_org_lwjgl_opengl_GL11_nglColor4f(null,0,1,0,.5,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,20,21,22,.3,.4,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
data=x.calls.filter(a=>a[0]==='data'); vals=data[0][2]; expect(vals.length===18,`varying float count ${vals.length}`);
exp=[10,11,12,1,0,0,1,.1,.2,20,21,22,0,1,0,.5,.3,.4]; exp.forEach((v,i)=>expect(near(vals[i],v),`varying float ${i}: ${vals[i]} != ${v}`));
expect(c.presentationStats.immediateCompactColorFallbackObserved===true,'varying fallback flag'); expect(c.presentationStats.immediateCompactColorHitObserved===false,'varying draw incorrectly marked compact'); expect(c.presentationStats.immediateInterleavedBytes===72,'varying byte accounting');
ptr=x.calls.filter(a=>a[0]==='ptr'); expect(ptr.length===3&&ptr[1][1]===4,'varying full color pointer missing');
// glColor3f must trigger the same fallback and force alpha=1 for subsequent vertices.
x=make(true); c=x.c; c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,1,1,1,0,0,0); c.Java_org_lwjgl_opengl_GL11_nglColor3f(null,.2,.3,.4,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,2,2,2,1,1,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0); vals=x.calls.filter(a=>a[0]==='data')[0][2]; expect(near(vals[9+3],.2)&&near(vals[9+4],.3)&&near(vals[9+5],.4)&&near(vals[9+6],1),'glColor3f fallback alpha/color');
// Opt-out must retain the exact production nine-float interleaved upload.
x=make(false); c=x.c; c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0); c.Java_org_lwjgl_opengl_GL11_nglColor4f(null,.2,.4,.6,.8,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,3,4,5,.6,.7,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0); vals=x.calls.filter(a=>a[0]==='data')[0][2]; expect(vals.length===9,'opt-out float count'); exp=[3,4,5,.2,.4,.6,.8,.6,.7]; exp.forEach((v,i)=>expect(near(vals[i],v),`opt-out float ${i}`)); expect(c.presentationStats.immediateCompactColorHitObserved===false,'opt-out compact hit');
console.log('verify-lwjgl-compact-color-setter: OK uniformFloats=10 varyingFloats=18 optOutFloats=9');
