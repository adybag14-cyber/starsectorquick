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
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-immediate-interleaved.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
for(const marker of ['WEBGL_IMMEDIATE_INTERLEAVED_V1','__LWJGL_IMMEDIATE_INTERLEAVED__','immediateInterleavedUploadsSaved','LWJGL_DETAILED_DRAW_TELEMETRY_OPTIN_V1','detailedDrawTelemetryActive','LWJGL_IMMEDIATE_STATIC_DISPATCH_V1']) expect(src.includes(marker),`missing ${marker}`);
const pointerDirtyPresent=src.includes('LWJGL_IMMEDIATE_POINTER_DIRTY_V1');
const names=['ensureImmediateArrayCapacity','appendImmediateVertex','appendImmediateVertexLegacy','Java_org_lwjgl_opengl_GL11_nglBegin','Java_org_lwjgl_opengl_GL11_nglTexCoord2f','Java_org_lwjgl_opengl_GL11_nglTexCoord2fLegacy','Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord','Java_org_lwjgl_opengl_GL11_nglVertex3f','Java_org_lwjgl_opengl_GL11_nglVertex3fLegacy','uploadImmediateInterleaved','Java_org_lwjgl_opengl_GL11_nglEnd','Java_org_lwjgl_opengl_GL11_nglEndLegacy'];
const dispatchBegin='// LWJGL_IMMEDIATE_STATIC_DISPATCH_BEGIN';
const dispatchEnd='// LWJGL_IMMEDIATE_STATIC_DISPATCH_END';
const db=src.indexOf(dispatchBegin), de=src.indexOf(dispatchEnd);
expect(db>=0&&de>db,'static dispatch block missing');
const dispatchCode=src.slice(db+dispatchBegin.length,de);
for(const hot of ['appendImmediateVertex','Java_org_lwjgl_opengl_GL11_nglTexCoord2f','Java_org_lwjgl_opengl_GL11_nglVertex3f','Java_org_lwjgl_opengl_GL11_nglEnd'])
  expect(!extract(src,hot).includes('immediateInterleavedEnabled'),`${hot} still branches on static interleaved flag`);
const code=names.map(n=>extract(src,n)).join('\n')+'\n'+dispatchCode;
function make(enabled){
  const calls=[]; const legacy=[]; const draws=[];
  const glCtx={ARRAY_BUFFER:0x8892,STATIC_DRAW:0x88E4,FLOAT:0x1406,NO_ERROR:0,
    bindBuffer:(...a)=>calls.push(['bindBuffer',...a]), bufferData:(t,d,u)=>calls.push(['bufferData',t,Array.from(d),u]),
    vertexAttribPointer:(...a)=>calls.push(['pointer',...a]), enableVertexAttribArray:(...a)=>calls.push(['enable',...a]),
    disableVertexAttribArray:(...a)=>calls.push(['disable',...a]), vertexAttrib2f:(...a)=>calls.push(['attrib2f',...a]), getError:()=>0};
  const c=vm.createContext({
  detailedDrawTelemetryEnabled: true,
    glCtx, immediateInterleavedEnabled:enabled, curList:null, pushInList(){throw new Error('unexpected list');},
    immediateModeData:{mode:0,vertexBuf:new Float32Array(32),vertexPos:0,colorBuf:new Float32Array(32),colorPos:0,currentColor:[1,1,1,1],currentTexCoord:[0,0],texCoordBuf:new Float32Array(32),texCoordPos:0,interleavedBuf:new Float32Array(96),interleavedPos:0},
    presentationStats:{immediateInterleavedDraws:0,immediateInterleavedUploads:0,immediateInterleavedUploadsSaved:0,immediateInterleavedBytes:0,immediatePointerLayoutRefreshes:0},
    immediatePointerLayoutDirty:true,
    vertexBuffer:{id:'v'}, colorBuffer:{id:'c'}, texCoordBuffer:{id:'t'}, vertexPosition:3,colorLocation:4,texCoord:5,
    strictWebGLValidation:false,clientArrayWarnings:new Set(),warnOnce(){},
    uploadDataImpl:(buf,buffer,attr,size,type,stride)=>legacy.push({data:Array.from(buf),buffer,attr,size,type,stride}),
    drawArraysImpl:(mode,first,count)=>draws.push({mode,first,count}), Float32Array,Math,console
  });
  vm.runInContext(code,c); return {c,calls,legacy,draws};
}
let x=make(true); const c=x.c;
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0);
c.immediateModeData.currentColor=[0.1,0.2,0.3,0.4];
c.Java_org_lwjgl_opengl_GL11_nglTexCoord2f(null,0.25,0.5,0);
c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,1,2,3,0);
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,4,5,6,0.75,1,0);
c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
let uploads=x.calls.filter(a=>a[0]==='bufferData'); expect(uploads.length===1,`enabled uploads=${uploads.length}`);
const data=uploads[0][2], expected=[1,2,3,.1,.2,.3,.4,.25,.5,4,5,6,.1,.2,.3,.4,.75,1];
expect(data.length===expected.length,`float length ${data.length}`); expected.forEach((v,i)=>expect(Math.abs(data[i]-v)<1e-6,`float ${i}: ${data[i]} != ${v}`));
const ptr=x.calls.filter(a=>a[0]==='pointer'); expect(ptr.length===3,`pointer count ${ptr.length}`);
expect(ptr[0][2]===3&&ptr[0][5]===36&&ptr[0][6]===0,'vertex layout');
expect(ptr[1][2]===4&&ptr[1][5]===36&&ptr[1][6]===12,'color layout');
expect(ptr[2][2]===2&&ptr[2][5]===36&&ptr[2][6]===28,'tex layout');
expect(x.legacy.length===0,'enabled path used legacy uploader'); expect(x.draws.length===1&&x.draws[0].count===2,'enabled draw count');
expect(c.presentationStats.immediateInterleavedDraws===1&&c.presentationStats.immediateInterleavedUploads===1,'enabled stats');
expect(c.presentationStats.immediateInterleavedUploadsSaved===2,'saved upload stats'); expect(c.presentationStats.immediateInterleavedBytes===72,'byte stats');
// Existing bridge semantics reset current texcoord at each begin/end block.
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,4,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,9,8,7,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
uploads=x.calls.filter(a=>a[0]==='bufferData'); const last=uploads[uploads.length-1][2]; expect(last[7]===0&&last[8]===0,'begin-local texcoord reset');
if(pointerDirtyPresent){
  expect(x.calls.filter(a=>a[0]==='pointer').length===3,'second immediate draw should reuse fixed pointer layout');
  expect(c.presentationStats.immediatePointerLayoutRefreshes===1,'pointer layout refreshed more than once without legacy dirtiness');
}
// Disabled mode must retain the exact three independent attribute uploads.
x=make(false); x.c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0); x.c.immediateModeData.currentColor=[.2,.4,.6,.8]; x.c.Java_org_lwjgl_opengl_GL11_nglTexCoord2f(null,.3,.7,0); x.c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,2,4,6,0); x.c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
expect(x.legacy.length===3,`fallback uploads=${x.legacy.length}`); expect(x.calls.filter(a=>a[0]==='bufferData').length===0,'fallback direct interleaved upload');
expect(x.legacy[0].size===3&&x.legacy[1].size===4&&x.legacy[2].size===2,'fallback attribute sizes'); expect(x.draws.length===1&&x.draws[0].count===1,'fallback draw');
console.log('verify-lwjgl-immediate-interleaved: OK staticDispatch=true enabledUploads=1 fallbackUploads=3 stride=36 offsets=0/12/28');