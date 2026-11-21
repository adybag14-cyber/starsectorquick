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
  'pushInList','packDisplayListVertices','replayDisplayListVertices','callList',
  'Java_org_lwjgl_opengl_GL11_nglEndList',
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
    curList:null, cmdLists:[], immediateBeginActive:false,
    immediateInterleavedEnabled:interleaved, verboseLog:false,
    immediateModeData:{
      mode:0,vertexBuf:new Float32Array(4),vertexPos:0,
      colorBuf:new Float32Array(4),colorPos:0,currentColor:[1,1,1,1],
      texCoordBuf:new Float32Array(4),texCoordPos:0,currentTexCoord:[0,0],
      interleavedBuf:new Float32Array(4),interleavedPos:0
    },
    presentationStats:{packedListVertices:0}, vertexBuffer:'v',colorBuffer:'c',texCoordBuffer:'t',
    vertexPosition:0,colorLocation:1,texCoord:2,
    glCtx:{FLOAT:0x1406,vertexAttrib2f:(...args)=>genericColor.push(args)},
    applyCurrentColorAttrib:()=>genericColor.push(Array.from(c.immediateModeData.currentColor)),
    setBridgeVertexArrayEnabled(){},
    uploadImmediateInterleaved:count=>uploads.push({kind:'interleaved',data:Array.from(c.immediateModeData.interleavedBuf.subarray(0,count*9))}),
    uploadDataImpl:(data,buffer)=>uploads.push({kind:buffer,data:Array.from(data)}),
    compatRecordImmediateDraw(){},
    drawArraysImpl:(mode,first,count)=>draws.push({mode,first,count})
  });
  vm.runInContext(code,c);
  return {c,uploads,draws,genericColor};
}
function vertexCursor(x) {
  const data = x.c.immediateModeData;
  return x.c.immediateInterleavedEnabled ? data.interleavedPos : data.vertexPos;
}
function call(x, name, ...args) {
  return x.c['Java_org_lwjgl_opengl_GL11_'+name](null,...args,0);
}
function rawBatch(x, rows, forceGeneric=false) {
  if(rows.length===1 && !forceGeneric) {
    x.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexSingle(
      {getJNIDataView(){throw new Error('single transport must not read JNI memory');}},
      ...rows[0],0);
    return;
  }
  if(rows.length===2 && !forceGeneric) {
    x.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexPair(
      {getJNIDataView(){throw new Error('pair transport must not read JNI memory');}},
      ...rows.flat(),0);
    return;
  }
  if(rows.length===4 && !forceGeneric) {
    x.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexQuad(
      {getJNIDataView(){throw new Error('quad transport must not read JNI memory');}},
      ...rows.flat(),0);
    return;
  }
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
  if(i===3)a=[Math.fround(2**-149),Math.fround(-(2**-149)),Math.fround(2**-126),
    Math.fround(-(2**-126)),Math.fround(1+2**-23)];
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
for(const count of [1023,1024,1025,1026,1027,1028,2048,2501]) {
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
// Direct four-vertex messages must retain the generic receiver's exact float
// and state semantics, for every primitive mode and both output layouts.
for(const interleaved of [false,true])for(const mode of [0,1,2,3,4,5,6,7,8,9]) {
  const fast=make(interleaved,true),generic=make(interleaved,true),scalar=make(interleaved,false);
  for(const x of [fast,generic,scalar])call(x,'nglBegin',mode);
  for(const [length,seed] of [[4,0],[3,4],[4,7]]) {
    for(const x of [fast,generic,scalar])call(x,'nglColor4f',Math.fround(seed*.1),.5,.75,1);
    const rows=Array.from({length},(_,i)=>row(seed+i));
    rawBatch(fast,rows);rawBatch(generic,rows,true);
    for(const a of rows)call(scalar,'nglVertex3fTexCoord',...a);
  }
  for(const x of [fast,generic,scalar])call(x,'nglEnd');
  eq(fast.uploads,generic.uploads,'quad primitive parameters match generic Float32 transport');
  eq(fast.uploads,scalar.uploads,'quad primitive parameters match scalar Float32 transport');
  eq(fast.draws,scalar.draws,'quad transport never changes primitive topology');
  eq(fast.genericColor,scalar.genericColor,'quad transport preserves current color');
  eq(Array.from(fast.c.immediateModeData.currentTexCoord),
     Array.from(scalar.c.immediateModeData.currentTexCoord),'quad transport preserves final texcoords');
  eq(fast.c.vertexBatchStats.quadCalls,2,'only exact four-vertex messages use fast native');
  eq(fast.c.vertexBatchStats.batchCalls,3,'quad calls remain part of normal batch total');
  eq(fast.c.vertexBatchStats.batchVertices,11,'mixed transport completed vertex conservation');
  eq(fast.c.vertexBatchStats.legacyVertices,0,'mixed transport has no lost batch vertices');
  eq(generic.c.vertexBatchStats.quadCalls,0,'generic receiver still accepts four-vertex messages');
}
for(const interleaved of [false,true]) {
  const x=make(interleaved,true);
  call(x,'nglBegin',7);rawBatch(x,[row(0),row(1),row(2),row(3)]);call(x,'nglEnd');
  eq(x.c.vertexBatchStats.quadCalls,1,'quad receiver executed');
  eq(x.c.vertexBatchStats.memoryViewCreations,0,'quad-only transport allocates no JNI memory view');
}
for(const compiling of [false,true]) {
  const x=make(true,true);
  if(compiling){call(x,'nglBegin',7);x.c.curList=[];}
  assert.throws(()=>rawBatch(x,[row(0),row(1),row(2),row(3)]),
    /outside an immediate primitive/);checks++;
  eq(vertexCursor(x),0,'invalid quad state must not mutate geometry');
  eq(x.c.vertexBatchStats.batchCalls,0,'invalid quad state must not increment batch count');
  eq(x.c.vertexBatchStats.quadCalls,0,'invalid quad state must not increment quad count');
}
ok(source.slice(source.indexOf('export default {')).includes('Java_org_lwjgl_opengl_GL11_nglAppendVertexQuad,'),
   'native quad export exists');

// Pair, quad and generic messages can continue one original primitive. Compare
// against scalar and forced old-memory transport for every mode/layout.
for(const interleaved of [false,true])for(const mode of [0,1,2,3,4,5,6,7,8,9]) {
  const fast=make(interleaved,true),generic=make(interleaved,true),scalar=make(interleaved,false);
  for(const x of [fast,generic,scalar])call(x,'nglBegin',mode);
  for(const [length,seed] of [[2,0],[1,2],[2,2],[4,4],[3,8]]) {
    for(const x of [fast,generic,scalar])call(x,'nglColor4f',Math.fround(seed*.1),.5,.75,1);
    const rows=Array.from({length},(_,i)=>row(seed+i));
    rawBatch(fast,rows);rawBatch(generic,rows,true);
    for(const a of rows)call(scalar,'nglVertex3fTexCoord',...a);
  }
  for(const x of [fast,generic,scalar])call(x,'nglEnd');
  eq(fast.uploads,generic.uploads,'pair primitive parameters match generic Float32 transport');
  eq(fast.uploads,scalar.uploads,'pair primitive parameters match scalar Float32 transport');
  eq(fast.draws,scalar.draws,'pair transport does not change primitive topology');
  eq(fast.genericColor,scalar.genericColor,'pair transport preserves renderer color');
  eq(Array.from(fast.c.immediateModeData.currentTexCoord),
     Array.from(scalar.c.immediateModeData.currentTexCoord),'pair transport preserves final texcoords');
  eq(fast.c.vertexBatchStats.singleCalls,1,'one-vertex packet uses single native');
  eq(fast.c.vertexBatchStats.pairCalls,2,'only exact two-vertex messages use pair native');
  eq(fast.c.vertexBatchStats.quadCalls,1,'four-vertex specialization retained');
  eq(fast.c.vertexBatchStats.batchCalls,5,'single/pair/quad/generic accepted call total');
  eq(fast.c.vertexBatchStats.batchVertices,12,'mixed short transports complete exact vertices');
  eq(fast.c.vertexBatchStats.legacyVertices,0,'mixed short transport has no scalar leakage');
  eq(generic.c.vertexBatchStats.pairCalls,0,'generic receiver still accepts pairs');
  eq(generic.c.vertexBatchStats.quadCalls,0,'forced generic receiver accepts quads');
}
for(const interleaved of [false,true]) {
  const x=make(interleaved,true);
  call(x,'nglBegin',7);rawBatch(x,[row(1),row(3)]);call(x,'nglEnd');
  eq(x.c.vertexBatchStats.pairCalls,1,'pair receiver executed');
  eq(x.c.vertexBatchStats.batchVertices,2,'pair completed vertex count');
  eq(x.c.vertexBatchStats.memoryViewCreations,0,'pair-only transport does not create a JNI view');
}
for(const compiling of [false,true]) {
  const x=make(true,true);
  if(compiling){call(x,'nglBegin',7);x.c.curList=[];}
  assert.throws(()=>rawBatch(x,[row(0),row(1)]),/outside an immediate primitive/);checks++;
  eq(vertexCursor(x),0,'invalid pair state must not mutate geometry');
  eq(x.c.vertexBatchCurrentVertices,0,'invalid pair state must not mutate pending vertices');
  eq(x.c.vertexBatchStats.batchCalls,0,'invalid pair state must not increment batch count');
  eq(x.c.vertexBatchStats.pairCalls,0,'invalid pair state must not increment pair count');
  eq(Array.from(x.c.immediateModeData.currentTexCoord),[0,0],'invalid pair state preserves texcoords');
}
ok(source.slice(source.indexOf('export default {')).includes('Java_org_lwjgl_opengl_GL11_nglAppendVertexPair,'),
   'native pair export exists');

// Single packets keep their own batch accounting and use no JNI memory view.
for(const interleaved of [false,true])for(const mode of [0,1,2,3,4,5,6,7,8,9]) {
  const fast=make(interleaved,true),generic=make(interleaved,true),scalar=make(interleaved,false);
  for(const x of [fast,generic,scalar])call(x,'nglBegin',mode);
  for(const seed of [1,3,2]) {
    for(const x of [fast,generic,scalar])call(x,'nglColor4f',Math.fround(seed*.1),.5,.75,1);
    const rows=[row(seed)];
    rawBatch(fast,rows);rawBatch(generic,rows,true);
    call(scalar,'nglVertex3fTexCoord',...rows[0]);
  }
  for(const x of [fast,generic,scalar])call(x,'nglEnd');
  eq(fast.uploads,generic.uploads,'single parameters match generic Float32 transport');
  eq(fast.uploads,scalar.uploads,'single parameters match scalar Float32 transport');
  eq(fast.draws,scalar.draws,'single transport preserves primitive topology');
  eq(fast.genericColor,scalar.genericColor,'single transport preserves renderer color');
  eq(Array.from(fast.c.immediateModeData.currentTexCoord),
     Array.from(scalar.c.immediateModeData.currentTexCoord),'single transport preserves final texcoords');
  eq(fast.c.vertexBatchStats.singleCalls,3,'single receiver executed for every packet');
  eq(fast.c.vertexBatchStats.batchCalls,3,'single calls remain part of batch total');
  eq(fast.c.vertexBatchStats.batchVertices,3,'single completed batch vertices');
  eq(fast.c.vertexBatchStats.legacyVertices,0,'single vertices retain batch classification');
  eq(fast.c.vertexBatchStats.memoryViewCreations,0,'single transport creates no JNI memory view');
  eq(generic.c.vertexBatchStats.singleCalls,0,'old generic receiver still accepts single packets');
}
for(const compiling of [false,true]) {
  const x=make(true,true);
  if(compiling){call(x,'nglBegin',7);x.c.curList=[];}
  assert.throws(()=>rawBatch(x,[row(1)]),/outside an immediate primitive/);checks++;
  eq(vertexCursor(x),0,'invalid single state must not mutate geometry');
  eq(x.c.vertexBatchCurrentVertices,0,'invalid single state must not mutate pending vertices');
  eq(x.c.vertexBatchStats.batchCalls,0,'invalid single state must not increment batch count');
  eq(x.c.vertexBatchStats.singleCalls,0,'invalid single state must not increment single count');
  eq(Array.from(x.c.immediateModeData.currentTexCoord),[0,0],'invalid single state preserves texcoords');
}
ok(source.slice(source.indexOf('export default {')).includes('Java_org_lwjgl_opengl_GL11_nglAppendVertexSingle,'),
   'native single export exists');

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

  const beforeRejected=vertexCursor(batch);
  memory=new ArrayBuffer(83);
  assert.throws(()=>batch.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexBatch(lib,64,1,0),
    /exceeds JNI memory/);checks++;
  eq(vertexCursor(batch),beforeRejected,'fresh smaller heap rejects stale cached range');
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
  for(const command of list)command.f.apply(null,command.a);
  eq(x.draws,[{mode:7,first:0,count:1}],'list replay geometry');
  eq(x.c.vertexBatchStats.batchCalls,0,'list replay uses original scalar path');
  eq(x.c.vertexBatchStats.legacyVertices,1,'list replay scalar accounting');
}
// Real list compilation/replay versus the same recorded scalar commands.
// Include caller-owned color changes, short runs, and a trailing implicit UV.
for(const interleaved of [false,true]) {
  const scalar=make(interleaved,true), packed=make(interleaved,true);
  for(const target of [scalar,packed]) {
    const list=target.c.cmdLists[1]=[];
    target.c.curList=list;
    call(target,'nglBegin',7);
    for(const count of [8,7,31]) {
      for(let i=0;i<count;i++) call(target,'nglVertex3fTexCoord',...row(i).map(Math.fround));
      call(target,'nglColor4f',count/32,.25,.125,1);
    }
    call(target,'nglVertex3f',1,2,3); call(target,'nglEnd');
    if(target===packed) call(target,'nglEndList'); else target.c.curList=null;
  }
  ok(packed.c.cmdLists[1].length<scalar.c.cmdLists[1].length,'eligible runs were packed');
  for(const color of [[1,0,0,1],[1,0,0,1],[0,1,0,.5]]) {
    for(const target of [scalar,packed]) {
      call(target,'nglColor4f',...color);
      target.c.callList(1);
    }
    eq(packed.uploads,scalar.uploads,'packed list Float32 payload and inherited color');
    eq(packed.draws,scalar.draws,'packed list draw order and counts');
    eq(Array.from(packed.c.immediateModeData.currentTexCoord),Array.from(scalar.c.immediateModeData.currentTexCoord),'list final UV');
    eq(Array.from(packed.c.immediateModeData.currentColor),Array.from(scalar.c.immediateModeData.currentColor),'list final color');
  }
  eq(packed.c.presentationStats.packedListVertices,interleaved?117:0,'packed replay coverage');
  for(const target of [scalar,packed]) {
    target.c.curList=target.c.cmdLists[2]=[];
    target.c.callList(1);
    call(target,'nglEndList');
    target.c.cmdLists[1].length=0;
    target.c.callList(2);
  }
  eq(packed.uploads,scalar.uploads,'recording a replay preserves the nested list');
}
function rejected(address,count,active=true,list=null) {
  const x=make(true,true);
  if(active)call(x,'nglBegin',7);
  x.c.curList=list;
  const memory=new ArrayBuffer(64+1024*20);
  assert.throws(()=>x.c.Java_org_lwjgl_opengl_GL11_nglAppendVertexBatch(
    {getJNIDataView:()=>new DataView(memory)},address,count,0));checks++;
  eq(vertexCursor(x),0,'invalid batch must not mutate geometry');
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
  listCompileFallback:true,defaultOff:true,directQuadParity:true,directPairParity:true,directSingleParity:true
}));
