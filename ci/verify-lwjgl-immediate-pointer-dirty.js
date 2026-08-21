'use strict';
const fs=require('fs'),vm=require('vm');
function extract(src,name){
  const start=src.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=src.indexOf('{',start); let depth=0,q=null,esc=false;
  for(let i=brace;i<src.length;i++){const ch=src[i]; if(q){if(esc)esc=false; else if(ch==='\\\\')esc=true; else if(ch===q)q=null; continue;} if(ch==='"'||ch==="\'"||ch==='`'){q=ch;continue;} if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return src.slice(start,i+1);} throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-immediate-pointer-dirty.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
expect(src.includes('LWJGL_IMMEDIATE_POINTER_DIRTY_V1'),'missing dirty marker');
for(const needle of ['var immediatePointerLayoutDirty = true;','immediatePointerLayoutDirty = true;','immediatePointerLayoutDirty = false;','immediatePointerLayoutRefreshes++']) expect(src.includes(needle),`missing ${needle}`);
const arrayBufferCachePresent=src.includes('LWJGL_ARRAY_BUFFER_BIND_CACHE_V1');
const names=['clientArrayComponentBytes','normalizeLegacyClientArrayLayout','clientArrayEffectiveStride','isWebGLClientArrayType','isIntegerClientArrayType','convertDesktopClientArrayToFloat','uploadDataImpl','uploadImmediateInterleaved'];
if(arrayBufferCachePresent) names.unshift('bindArrayBufferCached');
const code=names.map(n=>extract(src,n)).join('\n');
const calls=[];
const glCtx={ARRAY_BUFFER:0x8892,STATIC_DRAW:0x88E4,FLOAT:0x1406,BYTE:0x1400,UNSIGNED_BYTE:0x1401,SHORT:0x1402,UNSIGNED_SHORT:0x1403,NO_ERROR:0,
 bindBuffer:(...a)=>calls.push(['bind',...a]), bufferData:(...a)=>calls.push(['data',...a]), vertexAttribPointer:(...a)=>calls.push(['ptr',...a]), enableVertexAttribArray:(...a)=>calls.push(['enable',...a]), getError:()=>0};
const c=vm.createContext({glCtx,DataView,Float32Array,Math,Number,Set,console,clientArrayWarnings:new Set(),warnOnce(){},strictWebGLValidation:false,
 immediateModeData:{interleavedBuf:new Float32Array(96)},vertexBuffer:{id:'v'},vertexPosition:3,colorLocation:4,texCoord:5,
 presentationStats:{immediateInterleavedDraws:0,immediateInterleavedUploads:0,immediateInterleavedUploadsSaved:0,immediateInterleavedBytes:0,immediatePointerLayoutRefreshes:0,arrayBufferBindCacheHitObserved:false},immediatePointerLayoutDirty:true,arrayBufferBindCacheEnabled:true,currentArrayBufferBinding:null});
vm.runInContext(code,c);
// First immediate upload installs the fixed layout.
c.uploadImmediateInterleaved(2);
expect(calls.filter(x=>x[0]==='ptr').length===3,'first immediate upload must install three pointers');
expect(c.presentationStats.immediatePointerLayoutRefreshes===1,'first refresh telemetry');
expect(c.immediatePointerLayoutDirty===false,'fixed layout should be clean');
// Second upload reuses the exact layout with no pointer calls.
c.uploadImmediateInterleaved(2);
expect(calls.filter(x=>x[0]==='ptr').length===3,'clean immediate upload repeated pointer calls');
expect(c.presentationStats.immediatePointerLayoutRefreshes===1,'clean upload refreshed layout');
// A legacy client-array pointer write dirties the fixed immediate layout.
const legacy=new Float32Array([0,1,2,3,4,5]);
expect(c.uploadDataImpl(legacy,{id:'legacy'},3,3,glCtx.FLOAT,0,2)===true,'legacy upload failed');
expect(c.immediatePointerLayoutDirty===true,'legacy pointer write did not dirty immediate layout');
expect(calls.filter(x=>x[0]==='ptr').length===4,'legacy pointer write missing');
// Next immediate upload restores all three fixed pointers exactly once.
c.uploadImmediateInterleaved(2);
expect(calls.filter(x=>x[0]==='ptr').length===7,'dirty immediate layout did not restore three pointers');
expect(c.presentationStats.immediatePointerLayoutRefreshes===2,'dirty refresh telemetry');
expect(c.immediatePointerLayoutDirty===false,'restored immediate layout should be clean');
console.log(`verify-lwjgl-immediate-pointer-dirty: OK refreshes=${c.presentationStats.immediatePointerLayoutRefreshes} pointerCalls=${calls.filter(x=>x[0]==='ptr').length}`);
