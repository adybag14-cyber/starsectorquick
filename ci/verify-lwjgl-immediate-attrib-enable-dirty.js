'use strict';
const fs=require('fs'),vm=require('vm');
function extract(src,name){
  const start=src.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=src.indexOf('{',start); let depth=0,q=null,esc=false;
  for(let i=brace;i<src.length;i++){const ch=src[i]; if(q){if(esc)esc=false; else if(ch==='\\\\')esc=true; else if(ch===q)q=null; continue;} if(ch==='"'||ch==="\'"||ch==='`'){q=ch;continue;} if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return src.slice(start,i+1);} throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-immediate-attrib-enable-dirty.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
for(const needle of ['LWJGL_IMMEDIATE_ATTRIB_ENABLE_DIRTY_V1','var immediateAttribEnableDirty = true;','immediateAttribEnableDirty = false;','immediateAttribEnableRefreshes++']) expect(src.includes(needle),`missing ${needle}`);
// All three bridge-side attribute disables must invalidate the fixed immediate enable layout.
const disables=[...src.matchAll(/glCtx\.disableVertexAttribArray\(([^)]+)\);/g)];
expect(disables.length===3,`unexpected disable site count ${disables.length}`);
for(const m of disables){ const tail=src.slice(m.index,m.index+180); expect(tail.includes('immediateAttribEnableDirty = true;'),`disable site does not dirty enable layout: ${m[0]}`); }
const names=['applyCurrentColorAttrib','uploadImmediateInterleaved'];
const code=names.map(n=>extract(src,n)).join('\n');
const calls=[];
const glCtx={ARRAY_BUFFER:0x8892,STATIC_DRAW:0x88E4,FLOAT:0x1406,NO_ERROR:0,
 bindBuffer:(...a)=>calls.push(['bind',...a]), bufferData:(...a)=>calls.push(['data',...a]),
 vertexAttribPointer:(...a)=>calls.push(['ptr',...a]), enableVertexAttribArray:(...a)=>calls.push(['enable',...a]),
 disableVertexAttribArray:(...a)=>calls.push(['disable',...a]), vertexAttrib4f:(...a)=>calls.push(['attrib4f',...a]), getError:()=>0};
const c=vm.createContext({glCtx,Float32Array,Set,console,strictWebGLValidation:false,clientArrayWarnings:new Set(),warnOnce(){},
 immediateModeData:{interleavedBuf:new Float32Array(96),currentColor:[.2,.4,.6,.8]},vertexBuffer:{id:'v'},vertexPosition:3,colorLocation:4,texCoord:5,
 immediatePointerLayoutDirty:true,immediateAttribEnableDirty:true,
 presentationStats:{immediateInterleavedDraws:0,immediateInterleavedUploads:0,immediateInterleavedUploadsSaved:0,immediateInterleavedBytes:0,immediatePointerLayoutRefreshes:0,immediateAttribEnableRefreshes:0}});
vm.runInContext(code,c);
c.uploadImmediateInterleaved(2);
expect(calls.filter(x=>x[0]==='enable').length===3,'first immediate upload must enable three attributes');
expect(c.presentationStats.immediateAttribEnableRefreshes===1,'first enable refresh telemetry');
expect(c.immediateAttribEnableDirty===false,'enable layout should be clean after first upload');
c.uploadImmediateInterleaved(2);
expect(calls.filter(x=>x[0]==='enable').length===3,'clean immediate upload repeated enable calls');
expect(c.presentationStats.immediateAttribEnableRefreshes===1,'clean upload refreshed enables');
c.applyCurrentColorAttrib();
expect(calls.filter(x=>x[0]==='disable').length===1,'color constant path must disable color array');
expect(c.immediateAttribEnableDirty===true,'color disable did not dirty enable layout');
c.uploadImmediateInterleaved(2);
expect(calls.filter(x=>x[0]==='enable').length===6,'dirty immediate upload did not restore three enables');
expect(c.presentationStats.immediateAttribEnableRefreshes===2,'dirty enable refresh telemetry');
expect(c.immediateAttribEnableDirty===false,'restored enable layout should be clean');
console.log(`verify-lwjgl-immediate-attrib-enable-dirty: OK refreshes=${c.presentationStats.immediateAttribEnableRefreshes} enableCalls=${calls.filter(x=>x[0]==='enable').length}`);
