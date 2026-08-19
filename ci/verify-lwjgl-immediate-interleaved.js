'use strict';
const fs=require('fs'); const vm=require('vm');
function extract(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=source.indexOf('{',start); let depth=0,quote=null,esc=false;
  for(let i=brace;i<source.length;i++){ const ch=source[i]; if(quote){ if(esc)esc=false; else if(ch==='\\')esc=true; else if(ch===quote)quote=null; continue; }
    if(ch==='"'||ch==="'"||ch==='`'){quote=ch;continue;} if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return source.slice(start,i+1); }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
function close(a,b,eps=1e-6){return Math.abs(a-b)<=eps;}
function expectArray(actual,expected,label){expect(actual.length===expected.length,`${label} length ${actual.length} != ${expected.length}`); expected.forEach((v,i)=>expect(close(actual[i],v),`${label}[${i}] ${actual[i]} != ${v}`));}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-immediate-interleaved.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
for(const marker of ['WEBGL_IMMEDIATE_INTERLEAVED_V1','WEBGL_IMMEDIATE_COMPACT_COLOR_V1','__LWJGL_IMMEDIATE_COMPACT_COLOR__','immediateCompactColorBytesSaved']) expect(src.includes(marker),`missing ${marker}`);
const names=['ensureImmediateArrayCapacity','appendImmediateVertex','Java_org_lwjgl_opengl_GL11_nglBegin','Java_org_lwjgl_opengl_GL11_nglTexCoord2f','Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord','Java_org_lwjgl_opengl_GL11_nglVertex3f','uploadImmediateInterleavedFull','uploadImmediateInterleavedCompact','Java_org_lwjgl_opengl_GL11_nglEnd'];
const code=names.map(n=>extract(src,n)).join('\n');
function make(interleaved=true,compact=true){
  const calls=[],legacy=[],draws=[];
  const glCtx={ARRAY_BUFFER:0x8892,STATIC_DRAW:0x88E4,FLOAT:0x1406,NO_ERROR:0,
    bindBuffer:(...a)=>calls.push(['bindBuffer',...a]), bufferData:(t,d,u)=>calls.push(['bufferData',t,Array.from(d),u]),
    vertexAttribPointer:(...a)=>calls.push(['pointer',...a]), enableVertexAttribArray:(...a)=>calls.push(['enable',...a]),
    disableVertexAttribArray:(...a)=>calls.push(['disable',...a]), vertexAttrib4f:(...a)=>calls.push(['attrib4f',...a]),
    vertexAttrib2f:(...a)=>calls.push(['attrib2f',...a]), getError:()=>0};
  const c=vm.createContext({
    glCtx, immediateInterleavedEnabled:interleaved, immediateCompactColorEnabled:compact, curList:null, pushInList(){throw new Error('unexpected list');},
    immediateModeData:{mode:0,vertexBuf:new Float32Array(32),vertexPos:0,colorBuf:new Float32Array(32),colorPos:0,currentColor:[1,1,1,1],currentTexCoord:[0,0],texCoordBuf:new Float32Array(32),texCoordPos:0,interleavedBuf:new Float32Array(96),interleavedPos:0,compactBuf:new Float32Array(64),compactPos:0,compactBatchColor:[1,1,1,1],compactColorVaried:false,compactVertexCount:0},
    presentationStats:{immediateInterleavedDraws:0,immediateInterleavedUploads:0,immediateInterleavedUploadsSaved:0,immediateInterleavedBytes:0,immediateCompactColorDraws:0,immediateCompactColorVertices:0,immediateCompactColorBytesSaved:0,immediateCompactColorFallbackDraws:0,immediateCompactColorBackfillVertices:0},
    vertexBuffer:{id:'v'}, colorBuffer:{id:'c'}, texCoordBuffer:{id:'t'}, vertexPosition:3,colorLocation:4,texCoord:5,
    strictWebGLValidation:false,clientArrayWarnings:new Set(),warnOnce(){},
    uploadDataImpl:(buf,buffer,attr,size,type,stride)=>legacy.push({data:Array.from(buf),buffer,attr,size,type,stride}),
    drawArraysImpl:(mode,first,count)=>draws.push({mode,first,count}), Float32Array,Math,console
  });
  vm.runInContext(code,c); return {c,calls,legacy,draws};
}

// Common path: same color for all vertices -> 5 floats/vertex + constant RGBA.
let x=make(true,true), c=x.c;
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,4,0);
c.immediateModeData.currentColor=[.1,.2,.3,.4];
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,1,2,3,.25,.5,0);
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,4,5,6,.75,1,0);
// A color change after the last vertex is GL current state only and must not retroactively vary captured vertices.
c.immediateModeData.currentColor=[.9,.8,.7,.6];
c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
let uploads=x.calls.filter(a=>a[0]==='bufferData'); expect(uploads.length===1,`uniform uploads=${uploads.length}`);
expectArray(uploads[0][2],[1,2,3,.25,.5,4,5,6,.75,1],'uniform compact data');
let ptr=x.calls.filter(a=>a[0]==='pointer'); expect(ptr.length===2,`uniform pointer count=${ptr.length}`);
expect(ptr[0][2]===3&&ptr[0][5]===20&&ptr[0][6]===0,'uniform vertex layout');
expect(ptr[1][2]===2&&ptr[1][5]===20&&ptr[1][6]===12,'uniform tex layout');
const constant=x.calls.find(a=>a[0]==='attrib4f'); expect(constant,'uniform constant color missing'); expectArray(constant.slice(2),[.1,.2,.3,.4],'uniform constant color');
expect(x.calls.some(a=>a[0]==='disable'&&a[1]===4),'uniform color array not disabled');
expect(c.presentationStats.immediateCompactColorDraws===1,'uniform draw stat');
expect(c.presentationStats.immediateCompactColorVertices===2,'uniform vertex stat');
expect(c.presentationStats.immediateCompactColorBytesSaved===32,'uniform byte savings');
expect(c.presentationStats.immediateInterleavedBytes===40,'uniform upload bytes');
expect(c.presentationStats.immediateCompactColorFallbackDraws===0,'uniform fallback stat');
expect(c.presentationStats.immediateCompactColorBackfillVertices===0,'uniform backfill stat');
expect(x.draws.length===1&&x.draws[0].count===2,'uniform draw count');

// Begin-local texcoord semantics remain (0,0) without an explicit texcoord.
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,4,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,9,8,7,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
uploads=x.calls.filter(a=>a[0]==='bufferData'); expectArray(uploads[uploads.length-1][2],[9,8,7,0,0],'begin-local texcoord');

// Rare path: color actually changes before a vertex. Prior compact vertices must be backfilled exactly.
x=make(true,true); c=x.c;
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,4,0);
c.immediateModeData.currentColor=[.1,.2,.3,.4];
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,1,2,3,.1,.2,0);
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,4,5,6,.3,.4,0);
c.immediateModeData.currentColor=[.6,.7,.8,.9];
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,7,8,9,.5,.6,0);
c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
uploads=x.calls.filter(a=>a[0]==='bufferData'); expect(uploads.length===1,`varying uploads=${uploads.length}`);
expectArray(uploads[0][2],[1,2,3,.1,.2,.3,.4,.1,.2,4,5,6,.1,.2,.3,.4,.3,.4,7,8,9,.6,.7,.8,.9,.5,.6],'varying full data');
ptr=x.calls.filter(a=>a[0]==='pointer'); expect(ptr.length===3,`varying pointer count=${ptr.length}`);
expect(ptr[0][5]===36&&ptr[0][6]===0,'varying vertex layout'); expect(ptr[1][5]===36&&ptr[1][6]===12,'varying color layout'); expect(ptr[2][5]===36&&ptr[2][6]===28,'varying tex layout');
expect(x.calls.some(a=>a[0]==='enable'&&a[1]===4),'varying color array not enabled');
expect(c.presentationStats.immediateCompactColorFallbackDraws===1,'varying fallback stat');
expect(c.presentationStats.immediateCompactColorBackfillVertices===2,'varying backfill count');
expect(c.presentationStats.immediateCompactColorDraws===0,'varying compact draw stat');
expect(c.presentationStats.immediateInterleavedBytes===108,'varying bytes');

// Compact kill switch must retain the validated 9-float interleaved path exactly.
x=make(true,false); c=x.c; c.Java_org_lwjgl_opengl_GL11_nglBegin(null,4,0); c.immediateModeData.currentColor=[.2,.4,.6,.8]; c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,2,4,6,.3,.7,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
uploads=x.calls.filter(a=>a[0]==='bufferData'); expect(uploads.length===1,'compact-disabled upload count'); expectArray(uploads[0][2],[2,4,6,.2,.4,.6,.8,.3,.7],'compact-disabled full data'); expect(x.calls.filter(a=>a[0]==='pointer').length===3,'compact-disabled pointer count'); expect(c.presentationStats.immediateCompactColorDraws===0&&c.presentationStats.immediateCompactColorFallbackDraws===0,'compact-disabled stats');

// Interleaved kill switch must retain the exact three independent legacy uploads.
x=make(false,true); c=x.c; c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0); c.immediateModeData.currentColor=[.2,.4,.6,.8]; c.Java_org_lwjgl_opengl_GL11_nglTexCoord2f(null,.3,.7,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,2,4,6,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
expect(x.legacy.length===3,`legacy uploads=${x.legacy.length}`); expect(x.legacy[0].size===3&&x.legacy[1].size===4&&x.legacy[2].size===2,'legacy attribute sizes'); expect(x.calls.filter(a=>a[0]==='bufferData').length===0,'legacy used compact uploader');

// Seeded randomized semantic differential: reconstruct every uploaded vertex.
let seed=0x2077cafe;
function rand(){ seed=(Math.imul(seed,1664525)+1013904223)>>>0; return seed/4294967296; }
function rv(){ return Math.fround((rand()*2000)-1000); }
function rc(){ return Math.fround(rand()); }
let fuzzUniform=0,fuzzVarying=0,fuzzVertices=0;
x=make(true,true); c=x.c;
for(let draw=0;draw<2000;draw++){
  const count=1+Math.floor(rand()*32); const ref=[]; const forceVary=rand()<0.35;
  let color=[rc(),rc(),rc(),rc()];
  c.Java_org_lwjgl_opengl_GL11_nglBegin(null,4,0);
  const beforeCalls=x.calls.length;
  for(let i=0;i<count;i++){
    if(forceVary && i>0 && rand()<0.28) color=[rc(),rc(),rc(),rc()];
    if(rand()<0.05){ const saved=color; c.immediateModeData.currentColor=[rc(),rc(),rc(),rc()]; c.immediateModeData.currentColor=saved; }
    c.immediateModeData.currentColor=color;
    const px=rv(),py=rv(),pz=rv(),u=rc(),v=rc();
    ref.push([px,py,pz,color[0],color[1],color[2],color[3],u,v]);
    c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,px,py,pz,u,v,0);
  }
  if(rand()<0.3) c.immediateModeData.currentColor=[rc(),rc(),rc(),rc()];
  c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
  const newCalls=x.calls.slice(beforeCalls); const bufs=newCalls.filter(a=>a[0]==='bufferData'); expect(bufs.length===1,`fuzz ${draw} uploads=${bufs.length}`);
  const pointers=newCalls.filter(a=>a[0]==='pointer'); const attr4=newCalls.find(a=>a[0]==='attrib4f');
  const uniform=ref.every(r=>r[3]===ref[0][3]&&r[4]===ref[0][4]&&r[5]===ref[0][5]&&r[6]===ref[0][6]);
  const data=bufs[0][2];
  if(uniform){
    fuzzUniform++; expect(data.length===count*5,`fuzz ${draw} uniform float count`); expect(pointers.length===2,`fuzz ${draw} uniform pointers`); expect(attr4,`fuzz ${draw} uniform color attr`);
    for(let i=0;i<count;i++){ const dp=i*5,r=ref[i]; expectArray([data[dp],data[dp+1],data[dp+2],attr4[2],attr4[3],attr4[4],attr4[5],data[dp+3],data[dp+4]],r,`fuzz uniform ${draw}/${i}`); }
  } else {
    fuzzVarying++; expect(data.length===count*9,`fuzz ${draw} varying float count`); expect(pointers.length===3,`fuzz ${draw} varying pointers`);
    for(let i=0;i<count;i++){ const dp=i*9; expectArray(data.slice(dp,dp+9),ref[i],`fuzz varying ${draw}/${i}`); }
  }
  fuzzVertices+=count;
}
expect(fuzzUniform>500&&fuzzVarying>100,'fuzz distribution insufficient');
console.log(`verify-lwjgl-immediate-compact-color-fuzz: OK draws=2000 uniform=${fuzzUniform} varying=${fuzzVarying} vertices=${fuzzVertices}`);

console.log(`verify-lwjgl-immediate-compact-color: OK uniformFloats=5 varyingFloats=9 savedBytes=${32} backfill=${2}`);