'use strict';
const fs = require('fs');
const vm = require('vm');
function extractFunction(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=source.indexOf('{',start); let depth=0,quote=null,escaped=false;
  for(let i=brace;i<source.length;i++){
    const ch=source[i];
    if(quote){ if(escaped) escaped=false; else if(ch==='\\') escaped=true; else if(ch===quote) quote=null; continue; }
    if(ch==='"'||ch==="'"||ch==='`'){ quote=ch; continue; }
    if(ch==='{') depth++; else if(ch==='}' && --depth===0) return source.slice(start,i+1);
  }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){ if(!v) throw new Error(m); }
const path=process.argv[2]; if(!path) throw new Error('usage: node ci/verify-lwjgl-arb-vbo-runtime.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
for(const marker of ['LWJGL_ARB_VBO_COMPAT_V1','data.vbo > 0','window.__lwjglVboStats']) expect(source.includes(marker),`missing ${marker}`);
const names=['arbBufferTarget','arbBoundBufferId','bindArrayBufferPhysical','arbBindPhysical',
 'Java_org_lwjgl_opengl_ARBBufferObject_nglGenBuffersARB','Java_org_lwjgl_opengl_ARBBufferObject_nglDeleteBuffersARB',
 'Java_org_lwjgl_opengl_ARBBufferObject_nglBindBufferARB','Java_org_lwjgl_opengl_ARBBufferObject_nglBufferDataARB',
 'Java_org_lwjgl_opengl_ARBBufferObject_nglBufferSubDataARB','normalizeLegacyClientArrayLayout','clientArrayComponentBytes',
 'clientArrayEffectiveStride','clientArrayByteLength','isWebGLClientArrayType','isIntegerClientArrayType','uploadData'];
const code=names.map(n=>extractFunction(source,n)).join('\n');
let next=0; const calls=[];
const glCtx={
 ARRAY_BUFFER:0x8892,ELEMENT_ARRAY_BUFFER:0x8893,FLOAT:0x1406,UNSIGNED_BYTE:0x1401,BYTE:0x1400,SHORT:0x1402,UNSIGNED_SHORT:0x1403,HALF_FLOAT:0x140B,
 createBuffer(){ const o={id:++next}; calls.push(['create',o.id]); return o; },
 deleteBuffer(o){ calls.push(['delete',o&&o.id]); },
 bindBuffer(t,o){ calls.push(['bind',t,o&&o.id||0]); },
 bufferData(t,d,u){ calls.push(['data',t,typeof d==='number'?d:Array.from(d),u]); },
 bufferSubData(t,off,d){ calls.push(['sub',t,off,Array.from(d)]); },
 vertexAttribPointer(loc,size,type,norm,stride,off){ calls.push(['ptr',loc,size,type,norm,stride,off]); },
 enableVertexAttribArray(loc){ calls.push(['enable',loc]); },
 disableVertexAttribArray(loc){ calls.push(['disable',loc]); },
 vertexAttrib4f(){}, vertexAttrib2f(){},
};
const mem=new ArrayBuffer(512); const lib={getJNIDataView(){return new DataView(mem);}};
const ctx=vm.createContext({glCtx,arbBufferObjects:[null],arbBoundArrayBufferId:0,arbBoundElementArrayBufferId:0,
 arbVboStats:{generated:0,deleted:0,dataBytes:0,subDataBytes:0,subDataCalls:0,vboDraws:0,physicalBindCalls:0,physicalBindChanges:0,physicalBindSkipped:0},
 physicalArrayBufferBinding:null,
 clientArrayWarnings:new Set(),warnOnce(){},assert(v){if(!v)throw new Error('assert');},
 colorLocation:1,texCoord:2,Math,Number,Uint8Array,Uint32Array,DataView,Set,console});
vm.runInContext(code,ctx);
// Generate one logical VBO id into Java memory.
ctx.Java_org_lwjgl_opengl_ARBBufferObject_nglGenBuffersARB(lib,1,16,0);
const id=new Uint32Array(mem,16,1)[0]; expect(id===1,`generated id ${id}`); expect(ctx.arbVboStats.generated===1,'generated stat');
// Dynamic allocation uses the same ARB/WebGL enum and no CPU source copy.
ctx.Java_org_lwjgl_opengl_ARBBufferObject_nglBindBufferARB(lib,0x8892,id,0);
ctx.Java_org_lwjgl_opengl_ARBBufferObject_nglBufferDataARB(lib,0x8892,128,0,0x88E8,0);
expect(calls.some(c=>c[0]==='data'&&c[2]===128&&c[3]===0x88E8),'size-only bufferData mismatch');
expect(ctx.arbVboStats.dataBytes===128,'data byte stat');
// Starsector uploads FloatBuffer regions by byte offset. Verify exact bytes/offset.
const src=new Uint8Array(mem,96,16); for(let i=0;i<src.length;i++) src[i]=i+1;
ctx.Java_org_lwjgl_opengl_ARBBufferObject_nglBufferSubDataARB(lib,0x8892,40,16,96,0);
const sub=calls.filter(c=>c[0]==='sub').at(-1); expect(sub&&sub[2]===40,'subdata byte offset mismatch'); expect(sub[3].join(',')===Array.from(src).join(','),'subdata bytes mismatch');
expect(ctx.arbVboStats.subDataCalls===1&&ctx.arbVboStats.subDataBytes===16,'subdata stats');
// VBO-backed client pointers are GPU byte offsets; no Java client-array upload is allowed.
const data={enabled:true,size:2,type:glCtx.FLOAT,stride:0,pointer:64,vbo:id,buf:null};
const used=ctx.uploadData(null,data,{temporary:true},7,4); expect(used===true,'VBO uploadData did not report resident path');
const ptr=calls.filter(c=>c[0]==='ptr').at(-1); expect(ptr&&ptr[1]===7&&ptr[2]===2&&ptr[3]===glCtx.FLOAT&&ptr[4]===false&&ptr[5]===0&&ptr[6]===64,`pointer mismatch ${JSON.stringify(ptr)}`);
expect(!calls.some(c=>c[0]==='data'&&Array.isArray(c[2])),'VBO pointer path copied client data');
expect(ctx.arbVboStats.physicalBindChanges===1,`expected one real resident bind, got ${ctx.arbVboStats.physicalBindChanges}`);
expect(ctx.arbVboStats.physicalBindSkipped>=3,`expected repeated resident binds to be skipped, got ${ctx.arbVboStats.physicalBindSkipped}`);
// Deleting a bound id clears logical binding; WebGL deleteBuffer handles physical detachment.
new Uint32Array(mem,32,1)[0]=id;
ctx.Java_org_lwjgl_opengl_ARBBufferObject_nglDeleteBuffersARB(lib,1,32,0);
expect(ctx.arbBufferObjects[id]===null,'delete did not clear object'); expect(ctx.arbBoundArrayBufferId===0,'delete did not clear logical binding'); expect(ctx.arbVboStats.deleted===1,'delete stat');
ctx.Java_org_lwjgl_opengl_ARBBufferObject_nglBindBufferARB(lib,0x8892,0,0);
expect(ctx.physicalArrayBufferBinding===null,'delete/unbind physical tracker mismatch');
const rawArrayBinds=(source.match(/glCtx\.bindBuffer\(glCtx\.ARRAY_BUFFER/g)||[]).length; expect(rawArrayBinds===1,`ARRAY_BUFFER bind bypass count=${rawArrayBinds}`);
console.log(`verify-lwjgl-arb-vbo-runtime: OK id=${id} dataBytes=${ctx.arbVboStats.dataBytes} subDataBytes=${ctx.arbVboStats.subDataBytes} ptrOffset=${ptr[6]} bindChanges=${ctx.arbVboStats.physicalBindChanges} bindSkipped=${ctx.arbVboStats.physicalBindSkipped}`);
