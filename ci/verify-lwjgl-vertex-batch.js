'use strict';
// Compare the actual direct-memory bulk receiver with the established scalar
// vertex path, including exact Float32 uploads and unchanged primitive draws.
const fs = require('fs');
const vm = require('vm');
const assert = require('assert/strict');
const file = process.argv[2] || 'build/final/wasm-modules/lwjgl.js';
const source = fs.readFileSync(file, 'utf8');
let checks = 0, submittedVertices = 0, cachedViewBatches = 0, sameBufferResizes = 0;
function eq(a,b,what) { assert.deepEqual(a,b,what); checks++; }
function ok(a,what) { assert.ok(a,what); checks++; }
function extract(name) {
  const start = source.indexOf('function ' + name + '(');
  if(start < 0) throw new Error('missing '+name);
  const brace = source.indexOf('{',start);
  let depth=0, quote=null, escape=false;
  for(let i=brace;i<source.length;i++) {
    const c=source[i];
    if(quote) {
      if(escape) escape=false;
      else if(c==='\\') escape=true;
      else if(c===quote) quote=null;
      continue;
    }
    if(c==='"' || c==="'" || c==='`') { quote=c; continue; }
    if(c==='{') depth++;
    else if(c==='}' && --depth===0) return source.slice(start,i+1);
  }
  throw new Error('unterminated '+name);
}
const marker='LWJGL_VERTEX_BATCH_V1';
const begin=source.indexOf('// '+marker+'_BEGIN');
const end=source.indexOf('// '+marker+'_END');
ok(begin>=0 && end>begin,'complete production bulk receiver section');
const code = source.slice(begin,end) + '\n' + [
  'ensureImmediateArrayCapacity','appendImmediateVertex',
  'Java_org_lwjgl_opengl_GL11_nglBegin','Java_org_lwjgl_opengl_GL11_nglEnd',
  'Java_org_lwjgl_opengl_GL11_nglColor3f','Java_org_lwjgl_opengl_GL11_nglColor4f',
  'Java_org_lwjgl_opengl_GL11_nglTexCoord2f',
  'Java_org_lwjgl_opengl_GL11_nglVertex3f',
  'Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord'
].map(extract).join('\n');
function make(interleaved, flag, float32Constructor = Float32Array) {
  const uploads=[],draws=[],genericColor=[];
  const window={};
  if(flag!==undefined)window.__LWJGL_VERTEX_BATCH__=flag;
  const c=vm.createContext({
    window, Float32Array: float32Constructor, Number, Math, console,
    curList:null, immediateBeginActive:false,
    immediateInterleavedEnabled:interleaved, verboseLog:false,
    immediateModeData:{
      mode:0,vertexBuf:new Float32Array(4),vertexPos:0,
      colorBuf:new Float32Array(4),colorPos:0,currentColor:[1,1,1,1],
      texCoordBuf:new Float32Array(4),texCoordPos:0,currentTexCoord:[0,0],
      interleavedBuf:new Float32Array(4),interleavedPos:0
    },
    presentationStats:{}, vertexBuffer:'v',colorBuffer:'c',texCoordBuffer:'t',
    vertexPosition:0,colorLocation:1,texCoord:2,
    glCtx:{FLOAT:0x1406,vertexAttrib2f:(...args)=>genericColor.push(args)},
    applyCurrentColorAttrib:()=>genericColor.push(Array.from(c.immediateModeData.currentColor)),
    setBridgeVertexArrayEnabled(){},
    uploadImmediateInterleaved:count=>uploads.push({kind:'interleaved',data:Array.from(c.immediateModeData.interleavedBuf.subarray(0,count*9))}),
    uploadDataImpl:(data,buffer)=>uploads.push({kind:buffer,data:Array.from(data)}),
    compatRecordImmediateDraw(){},
    drawArraysImpl:(mode,first,count)=>draws.push({mode,first,count}),
    pushInList:(list,args,fn)=>list.push({fn,args:Array.from(args)})
  });
  vm.runInContext(code,c);
  return {c,uploads,draws,genericColor};
}
function call(x, name, ...args) {
  return x.c['Java_org_lwjgl_opengl_GL11_'+name](null,...args,0);
}
function rawBatch(x, rows) {
  const address=64;
  const memory=new ArrayBuffer(address+1024*5*4);
  const data=new Float32Array(memory,address,rows.length*5);
  for(let i=0;i<rows.length;i++)data.set(rows[i],i*5);
  x.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexBatch(
    {getJNIDataView:()=>new DataView(memory)},BigInt(address),rows.length,0);
  data.fill(123456); // JNI transport storage may be reused immediately.
}
function row(i) {
  let a=[Math.fround(Math.sin(i)*314.3),Math.fround(Math.cos(i)*9.7),Math.fround(i*.03),
    Math.fround(i*.017),Math.fround(i*-.031)];
  if(i===1)a=[-0,+0,NaN,Infinity,-Infinity];
  if(i===2)a=[Infinity,-Infinity,-0,NaN,-0];
  return a;
}
function run(interleaved, batched, count, mode) {
  const x=make(interleaved,batched);
  eq(call(x,'nglBegin',mode),batched,'begin handshake');
  let pending=[];
  function flush() { if(pending.length) {rawBatch(x,pending);pending=[];} }
  for(let i=0;i<count;i++) {
    if(i%127===0) {
      flush();
      if(i%254===0)call(x,'nglColor4f',Math.fround(.1),Math.fround(i*.01),Math.fround(.7),Math.fround(.4));
      else call(x,'nglColor3f',Math.fround(.2),Math.fround(.6),Math.fround(.8));
    }
    const a=row(i);
    if(batched) {pending.push(a);if(pending.length===1024)flush();}
    else call(x,'nglVertex3fTexCoord',...a);
  }
  flush();
  // A legal scalar native vertex may be mixed with batch messages. This also
  // proves current-texcoord and legacy-array fallback behavior remain exact.
  call(x,'nglTexCoord2f',Math.fround(.25),Math.fround(.75));
  call(x,'nglVertex3f',Math.fround(.13),-0,Math.fround(.19));
  call(x,'nglEnd');
  const s=x.c.vertexBatchStats;
  eq(s.drawVertices,s.batchVertices+s.legacyVertices,'complete primitive conservation');
  eq(s.batchVertices,batched?count:0,'exact completed batch vertices');
  eq(s.legacyVertices,batched?1:count+1,'exact completed scalar vertices');
  eq(s.drawVertices,count+1,'full incomplete primitive retained');
  eq(s.draws,1,'one original draw per Begin/End');
  submittedVertices+=count+1;
  return x;
}
for(const interleaved of [false,true]) {
  for(const mode of [0,1,2,3,4,5,6,7,8,9]) {
    for(const count of [0,1,2,3,4,1023,1024,1025,2049,4097]) {
      const scalar=run(interleaved,false,count,mode);
      const batch=run(interleaved,true,count,mode);
      eq(batch.uploads,scalar.uploads,'exact Float32 upload payloads');
      eq(batch.draws,scalar.draws,'mode and draw topology');
      eq(batch.genericColor,scalar.genericColor,'generic color state');
      eq(Array.from(batch.c.immediateModeData.currentTexCoord),
         Array.from(scalar.c.immediateModeData.currentTexCoord),'current texcoord state');
    }
  }
}
// Capacity-sized uninterrupted runs exercise the actual 1024-vertex receiver
// limit independently of the color-flushed cases above.
for(const count of [1023,1024,1025,2048,2501]) {
  const x=make(true,true),y=make(true,false);
  call(x,'nglBegin',7);call(y,'nglBegin',7);
  for(let start=0;start<count;start+=1024) {
    const rows=Array.from({length:Math.min(1024,count-start)},(_,j)=>row(start+j));
    rawBatch(x,rows);
    for(const a of rows)call(y,'nglVertex3fTexCoord',...a);
  }
  call(x,'nglEnd');call(y,'nglEnd');
  eq(x.uploads,y.uploads,'capacity chunks retain complete payload');
  eq(x.draws,y.draws,'capacity chunks never split primitive');
  eq(x.c.vertexBatchStats.batchCalls,Math.ceil(count/1024),'bounded message count');
}
// Count production Float32Array buffer-view allocations, excluding test-side
// transport writes and numeric-sized output buffers. Fresh DataView wrappers,
// changing addresses/counts, and rewritten transport bytes must reuse one view.
for(const interleaved of [false,true]) {
  let allocations=0, jniReads=0, submissions=0;
  const countedFloat32 = new Proxy(Float32Array, {
    construct(target,args) {
      if(args[0] instanceof ArrayBuffer) allocations++;
      return Reflect.construct(target,args);
    }
  });
  const batch=make(interleaved,true,countedFloat32);
  const scalar=make(interleaved,false);
  let memory=new ArrayBuffer(4099); // valid messages need not fill a 4-byte-sized heap
  const lib={getJNIDataView:()=>{jniReads++;return new DataView(memory);}};
  call(batch,'nglBegin',7);call(scalar,'nglBegin',7);
  function submit(offset,count,seed) {
    const data=new Float32Array(memory,offset,count*5);
    for(let i=0;i<count;i++)data.set(row(seed+i),i*5);
    batch.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexBatch(lib,BigInt(offset),count,0);
    for(let i=0;i<count;i++)call(scalar,'nglVertex3fTexCoord',...row(seed+i));
    data.fill(123456); // accepted geometry must already be copied out of JNI memory
    submissions++;cachedViewBatches++;
  }
  for(let i=0;i<512;i++)submit([4,64,480][i%3],[1,2,3,7,31][i%5],i);
  eq(allocations,1,'512 same-memory batches allocate exactly one JNI Float32Array view');
  eq(batch.c.vertexBatchStats.memoryViewCreations,1,'live counter reports one reused heap view');
  eq(jniReads,submissions,'same-memory batches still request the current JNI view');

  const previous=memory;
  memory=new ArrayBuffer(4099); // equal length, different identity
  new Float32Array(previous,0,Math.floor(previous.byteLength/4)).fill(-654321);
  submit(12,5,1);
  eq(allocations,2,'equal-length memory replacement refreshes the view');
  memory=new ArrayBuffer(16387); // replacement growth and an odd trailing byte count
  submit(Math.floor(memory.byteLength/4)*4-5*20,5,2);
  eq(allocations,3,'replacement growth reads the new heap tail');

  const beforeRejected=batch.c.immediateModeData.vertexPos;
  memory=new ArrayBuffer(83);
  assert.throws(()=>batch.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexBatch(lib,64,1,0),
    /exceeds JNI memory/);checks++;
  eq(batch.c.immediateModeData.vertexPos,beforeRejected,'fresh smaller heap rejects stale cached range');
  eq(allocations,3,'invalid range does not allocate a heap view');
  submit(4,1,2);
  eq(allocations,4,'valid smaller heap refreshes the view');

  const resizable=new ArrayBuffer(4099,{maxByteLength:16387});
  if(typeof resizable.resize==='function' && resizable.resizable) {
    memory=resizable;
    submit(64,3,0);
    eq(allocations,5,'resizable heap starts with one whole-memory view');
    memory.resize(8195);
    submit(Math.floor(memory.byteLength/4)*4-3*20,3,1);
    eq(allocations,6,'same-identity heap growth refreshes view length');
    memory.resize(1027);
    submit(Math.floor(memory.byteLength/4)*4-2*20,2,2);
    eq(allocations,7,'same-identity heap shrink refreshes view length');
    sameBufferResizes+=2;
  }
  call(batch,'nglEnd');call(scalar,'nglEnd');
  eq(jniReads,submissions+1,'every valid batch and buffer-range check reads fresh JNI memory');
  eq(batch.uploads,scalar.uploads,'cached view preserves exact Float32 data across heap/offset/count changes');
  eq(batch.draws,scalar.draws,'cached transport preserves original draw topology');
  eq(Array.from(batch.c.immediateModeData.currentTexCoord),
     Array.from(scalar.c.immediateModeData.currentTexCoord),'cached transport preserves final texcoords');
  eq(batch.c.vertexBatchStats.batchCalls,submissions,'invalid batch excluded from normal stats');
  eq(batch.c.vertexBatchStats.memoryViewCreations,allocations,'live counter matches actual view allocations');
}
// Absent flag defaults OFF; reads occur only at Begin.
{
  const x=make(true);
  eq(call(x,'nglBegin',7),false,'default off');
  x.c.window.__LWJGL_VERTEX_BATCH__=true;
  call(x,'nglVertex3fTexCoord',1,2,3,0,0);call(x,'nglEnd');
  eq(x.c.vertexBatchStats.enabledBegins,0,'midprimitive flag leaves handshake unchanged');
  eq(call(x,'nglBegin',7),true,'next Begin sees opt-in');
  rawBatch(x,[[4,5,6,0,0]]);call(x,'nglEnd');
  eq(x.c.vertexBatchStats.batchVertices,1,'toggle batch count');
  eq(x.c.vertexBatchStats.legacyVertices,1,'toggle scalar count');
}
// Compilation records scalar commands; replay retains ordering and geometry.
{
  const x=make(true,true),list=[];
  x.c.curList=list;
  eq(call(x,'nglBegin',7),false,'list compilation must return a Boolean false');
  call(x,'nglColor4f',.1,.2,.3,.4);
  call(x,'nglVertex3fTexCoord',1,2,3,.25,.75);
  call(x,'nglEnd');
  eq(x.draws.length,0,'no compilation draw');
  eq(x.c.vertexBatchStats.enabledBegins,0,'compilation is not an executed Begin');
  x.c.curList=null;
  for(const command of list)command.fn.apply(null,command.args);
  eq(x.draws,[{mode:7,first:0,count:1}],'list replay geometry');
  eq(x.c.vertexBatchStats.batchCalls,0,'list replay uses original scalar path');
  eq(x.c.vertexBatchStats.legacyVertices,1,'list replay scalar accounting');
}
function rejected(address,count,active=true,list=null) {
  const x=make(true,true);
  if(active)call(x,'nglBegin',7);
  x.c.curList=list;
  const memory=new ArrayBuffer(64+1024*20);
  assert.throws(()=>x.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexBatch(
    {getJNIDataView:()=>new DataView(memory)},address,count,0));checks++;
  eq(x.c.immediateModeData.vertexPos,0,'invalid batch must not mutate geometry');
  eq(x.c.vertexBatchStats.batchCalls,0,'invalid batch must not be counted');
}
for(const address of [0,-4,66,NaN,Infinity,Number.MAX_SAFE_INTEGER+1])rejected(address,1);
for(const count of [0,-1,1025,1.5,NaN])rejected(64,count);
rejected(64+1024*20-4,1);
rejected(64,1,false);
rejected(64,1,true,[]);
ok(/\bJava_org_lwjgl_opengl_GL11_nglAppendVertexBatch\s*[,}:]/.test(
   source.slice(source.indexOf('export default {'))), 'native batch export exists');
console.log('verify-lwjgl-vertex-batch: OK '+JSON.stringify({
  checks,submittedVertices,primitiveModes:10,cachedViewBatches,sameBufferResizes,
  float32PayloadParity:true,capacity:1024,reusesTransportImmediately:true,
  listCompileFallback:true,defaultOff:true
}));
