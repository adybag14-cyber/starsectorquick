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
for(const marker of ['WEBGL_IMMEDIATE_INTERLEAVED_V1','__LWJGL_IMMEDIATE_INTERLEAVED__','immediateInterleavedSubDataViewAvoided','immediateInterleavedBufferFloatCapacity']) expect(src.includes(marker),`missing ${marker}`);
expect(!extract(src,'uploadImmediateInterleaved').includes('.subarray('),'persistent upload reintroduced per-draw subarray');
const names=['ensureImmediateArrayCapacity','appendImmediateVertex','Java_org_lwjgl_opengl_GL11_nglBegin','Java_org_lwjgl_opengl_GL11_nglTexCoord2f','Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord','Java_org_lwjgl_opengl_GL11_nglVertex3f','ensureImmediateInterleavedBufferCapacity','uploadImmediateInterleaved','Java_org_lwjgl_opengl_GL11_nglEnd'];
const code=names.map(n=>extract(src,n)).join('\n');
function make(enabled){
  const calls=[]; const legacy=[]; const draws=[];
  const glCtx={ARRAY_BUFFER:0x8892,STATIC_DRAW:0x88E4,DYNAMIC_DRAW:0x88E8,FLOAT:0x1406,NO_ERROR:0,
    bindBuffer:(...a)=>calls.push(['bindBuffer',...a]),
    bufferData:(t,d,u)=>calls.push(['bufferData',t,d,u]),
    bufferSubData:(t,dst,src,off,len)=>calls.push(['bufferSubData',t,dst,Array.from(src.slice(off,off+len)),off,len]),
    vertexAttribPointer:(...a)=>calls.push(['pointer',...a]), enableVertexAttribArray:(...a)=>calls.push(['enable',...a]),
    disableVertexAttribArray:(...a)=>calls.push(['disable',...a]), vertexAttrib2f:(...a)=>calls.push(['attrib2f',...a]), getError:()=>0};
  const c=vm.createContext({
    glCtx, immediateInterleavedEnabled:enabled, curList:null, pushInList(){throw new Error('unexpected list');},
    immediateModeData:{mode:0,vertexBuf:new Float32Array(32),vertexPos:0,colorBuf:new Float32Array(32),colorPos:0,currentColor:[1,1,1,1],currentTexCoord:[0,0],texCoordBuf:new Float32Array(32),texCoordPos:0,interleavedBuf:new Float32Array(96),interleavedPos:0},
    presentationStats:{immediateInterleavedDraws:0,immediateInterleavedUploads:0,immediateInterleavedUploadsSaved:0,immediateInterleavedBytes:0,immediateInterleavedBufferGrowths:0,immediateInterleavedSubDataCalls:0,immediateInterleavedSubDataViewAvoided:0},
    immediateInterleavedBuffer:{id:'immediate'}, immediateInterleavedBufferFloatCapacity:0,
    vertexBuffer:{id:'v'}, colorBuffer:{id:'c'}, texCoordBuffer:{id:'t'}, vertexPosition:3,colorLocation:4,texCoord:5,
    strictWebGLValidation:false,clientArrayWarnings:new Set(),warnOnce(){},
    uploadDataImpl:(buf,buffer,attr,size,type,stride)=>legacy.push({data:Array.from(buf),buffer,attr,size,type,stride}),
    drawArraysImpl:(mode,first,count)=>draws.push({mode,first,count}), Float32Array,Math,console
  });
  vm.runInContext(code,c); return {c,calls,legacy,draws};
}
let x=make(true); const c=x.c;
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0); c.immediateModeData.currentColor=[0.1,0.2,0.3,0.4];
c.Java_org_lwjgl_opengl_GL11_nglTexCoord2f(null,0.25,0.5,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,1,2,3,0);
c.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord(null,4,5,6,0.75,1,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
let allocs=x.calls.filter(a=>a[0]==='bufferData'), subs=x.calls.filter(a=>a[0]==='bufferSubData');
expect(allocs.length===1,`growth allocations=${allocs.length}`); expect(typeof allocs[0][2]==='number'&&allocs[0][2]>=72,'geometric byte allocation missing');
expect(allocs[0][3]===c.glCtx?.DYNAMIC_DRAW || allocs[0][3]===0x88E8,'dynamic usage missing'); expect(subs.length===1,`subdata calls=${subs.length}`);
const data=subs[0][3], expected=[1,2,3,.1,.2,.3,.4,.25,.5,4,5,6,.1,.2,.3,.4,.75,1]; expect(data.length===expected.length,`float length ${data.length}`); expected.forEach((v,i)=>expect(Math.abs(data[i]-v)<1e-6,`float ${i}: ${data[i]} != ${v}`));
const ptr=x.calls.filter(a=>a[0]==='pointer'); expect(ptr.length===3,'pointer count'); expect(ptr[0][2]===3&&ptr[0][5]===36&&ptr[0][6]===0,'vertex layout'); expect(ptr[1][2]===4&&ptr[1][5]===36&&ptr[1][6]===12,'color layout'); expect(ptr[2][2]===2&&ptr[2][5]===36&&ptr[2][6]===28,'tex layout');
expect(x.legacy.length===0,'enabled path used legacy uploader'); expect(x.draws.length===1&&x.draws[0].count===2,'enabled draw'); expect(c.presentationStats.immediateInterleavedBufferGrowths===1,'growth stat'); expect(c.presentationStats.immediateInterleavedSubDataCalls===1&&c.presentationStats.immediateInterleavedSubDataViewAvoided===1,'subdata stats');
// A smaller second draw must reuse capacity: no second bufferData allocation.
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,4,0); c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,9,8,7,0); c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
allocs=x.calls.filter(a=>a[0]==='bufferData'); subs=x.calls.filter(a=>a[0]==='bufferSubData'); expect(allocs.length===1,'capacity was not reused'); expect(subs.length===2,'second subdata missing'); const last=subs[1][3]; expect(last[7]===0&&last[8]===0,'begin-local texcoord reset');
// Disabled mode retains the exact three independent attribute uploads.
x=make(false); x.c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0); x.c.immediateModeData.currentColor=[.2,.4,.6,.8]; x.c.Java_org_lwjgl_opengl_GL11_nglTexCoord2f(null,.3,.7,0); x.c.Java_org_lwjgl_opengl_GL11_nglVertex3f(null,2,4,6,0); x.c.Java_org_lwjgl_opengl_GL11_nglEnd(null,0);
expect(x.legacy.length===3,`fallback uploads=${x.legacy.length}`); expect(x.calls.filter(a=>a[0]==='bufferData'||a[0]==='bufferSubData').length===0,'fallback touched persistent buffer'); expect(x.draws.length===1&&x.draws[0].count===1,'fallback draw');
console.log('verify-lwjgl-immediate-interleaved-persistent: OK growths=1 subData=2 fallbackUploads=3 stride=36 offsets=0/12/28');