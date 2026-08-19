'use strict';
const fs = require('fs');
const vm = require('vm');
const glMatrix = require('../build/final/wasm-modules/gl-matrix-umd.js');

function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}(`);
  if (start < 0) throw new Error(`missing function ${name}`);
  const brace = source.indexOf('{', start);
  let depth=0, quote=null, escaped=false;
  for(let i=brace;i<source.length;i++) {
    const ch=source[i];
    if(quote){ if(escaped) escaped=false; else if(ch==='\\') escaped=true; else if(ch===quote) quote=null; continue; }
    if(ch==='"'||ch==="'"||ch==='`'){ quote=ch; continue; }
    if(ch==='{') depth++;
    else if(ch==='}' && --depth===0) return source.slice(start,i+1);
  }
  throw new Error(`unterminated function ${name}`);
}
function expect(v,m){ if(!v) throw new Error(m); }
function clone(m){ return m == null ? null : glMatrix.mat4.clone(m); }
function maxDiff(a,b){
  if(a===null || b===null) return a===b ? 0 : Infinity;
  let max=0;
  for(let i=0;i<16;i++) max=Math.max(max,Math.abs(a[i]-b[i]));
  return max;
}
function rng(seed){ let x=seed>>>0; return ()=>{ x=(Math.imul(x,1664525)+1013904223)>>>0; return x/4294967296; }; }

const path=process.argv[2]; if(!path) throw new Error('usage: node ci/verify-lwjgl-matrix-inplace.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('LWJGL_MATRIX_IN_PLACE_V1'),'missing in-place marker');
for(const token of ['if(!matrixInPlaceEnabled)','matrixTransformScratch','matrixVectorScratch','noteMatrixInPlace']) expect(source.includes(token),`missing ${token}`);
const names=['setMatrixVectorScratch','noteMatrixInPlace','getCurMatrixTop','setCurMatrixTop','Java_org_lwjgl_opengl_GL11_nglOrtho','Java_org_lwjgl_opengl_GL11_nglTranslatef','Java_org_lwjgl_opengl_GL11_nglMultMatrixf','Java_org_lwjgl_opengl_GL11_nglRotatef','Java_org_lwjgl_opengl_GL11_nglScalef'];
const code=names.map(n=>extractFunction(source,n)).join('\n');
const stats={matrixInPlaceOps:0,matrixTempAllocationsAvoided:0,matrixLegacyOps:0};
const context=vm.createContext({
  glMatrix,
  presentationStats:stats,
  matrixInPlaceEnabled:true,
  matrixTransformScratch:glMatrix.mat4.create(),
  matrixVectorScratch:glMatrix.vec3.create(),
  modelViewMatrixStack:[glMatrix.mat4.create()],
  curMatrixStack:null,
  curList:null,
  pushInList(){throw new Error('unexpected display-list path');},
  Math,
  Float32Array,
  Number,
});
context.curMatrixStack=context.modelViewMatrixStack;
vm.runInContext(code,context);

function setTop(m){ context.modelViewMatrixStack[0]=clone(m); context.curMatrixStack=context.modelViewMatrixStack; }
function runPair(start, invoke) {
  setTop(start); context.matrixInPlaceEnabled=false; invoke(); const legacy=clone(context.curMatrixStack[0]);
  setTop(start); context.matrixInPlaceEnabled=true; invoke(); const fast=clone(context.curMatrixStack[0]);
  const diff=maxDiff(legacy,fast);
  if(!(diff<=1e-6)) throw new Error(`matrix mismatch diff=${diff}\nlegacy=${legacy}\nfast=${fast}`);
  return diff;
}

const random=rng(0x5eed1234);
let worst=0;
for(let i=0;i<5000;i++) {
  const start=glMatrix.mat4.create();
  glMatrix.mat4.translate(start,start,[random()*40-20,random()*40-20,random()*10-5]);
  glMatrix.mat4.rotateZ(start,start,(random()*2-1)*Math.PI);
  glMatrix.mat4.scale(start,start,[0.2+random()*3,0.2+random()*3,0.2+random()*3]);
  const kind=i%5;
  let diff;
  if(kind===0){ const x=random()*200-100,y=random()*200-100,z=random()*50-25; diff=runPair(start,()=>context.Java_org_lwjgl_opengl_GL11_nglTranslatef(null,x,y,z,0)); }
  else if(kind===1){ const a=random()*720-360; let x=random()*2-1,y=random()*2-1,z=random()*2-1; if((i%997)===0){x=y=z=0;} diff=runPair(start,()=>context.Java_org_lwjgl_opengl_GL11_nglRotatef(null,a,x,y,z,0)); }
  else if(kind===2){ const x=random()*4-2,y=random()*4-2,z=random()*4-2; diff=runPair(start,()=>context.Java_org_lwjgl_opengl_GL11_nglScalef(null,x,y,z,0)); }
  else if(kind===3){ let l=random()*-200-1,r=random()*200+1,b=random()*-200-1,t=random()*200+1,n=random()*-10-0.1,f=random()*100+0.1; diff=runPair(start,()=>context.Java_org_lwjgl_opengl_GL11_nglOrtho(null,l,r,b,t,n,f,0)); }
  else {
    const other=glMatrix.mat4.create(); glMatrix.mat4.rotateX(other,other,(random()*2-1)*Math.PI); glMatrix.mat4.translate(other,other,[random()*5,random()*5,random()*5]);
    const memory=new ArrayBuffer(64); new Float32Array(memory).set(other); const lib={getJNIDataView(){return new DataView(memory);}};
    diff=runPair(start,()=>context.Java_org_lwjgl_opengl_GL11_nglMultMatrixf(lib,0,0));
  }
  worst=Math.max(worst,diff);
}
expect(stats.matrixInPlaceOps===5000,`expected 5000 fast ops, got ${stats.matrixInPlaceOps}`);
expect(stats.matrixLegacyOps===5000,`expected 5000 legacy ops, got ${stats.matrixLegacyOps}`);
expect(stats.matrixTempAllocationsAvoided===9000,`expected 9000 avoided temporaries, got ${stats.matrixTempAllocationsAvoided}`);
console.log(`verify-lwjgl-matrix-inplace: OK cases=5000 worstDiff=${worst} avoided=${stats.matrixTempAllocationsAvoided}`);