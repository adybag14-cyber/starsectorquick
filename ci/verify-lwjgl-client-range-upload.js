'use strict';
const fs = require('fs');
const vm = require('vm');
function extractFunction(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=source.indexOf('{',start); let depth=0,quote=null,escaped=false;
  for(let i=brace;i<source.length;i++){
    const ch=source[i];
    if(quote){if(escaped)escaped=false;else if(ch==='\\')escaped=true;else if(ch===quote)quote=null;continue;}
    if(ch==='"'||ch==="'"||ch==='`'){quote=ch;continue;}
    if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return source.slice(start,i+1);
  }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
function values(view){return Array.from(view);}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-client-range-upload.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('WEBGL_CLIENT_RANGE_UPLOAD_V1'),'missing range upload marker');
const getView=extractFunction(source,'getClientMemoryByteView');
const upload=extractFunction(source,'uploadBufferDataRange');
const uploadData=extractFunction(source,'uploadData');
const end=extractFunction(source,'Java_org_lwjgl_opengl_GL11_nglEnd');
expect(uploadData.includes('getClientMemoryByteView(v.buffer)'),'direct client arrays do not reuse full memory view');
expect(uploadData.includes('data.pointer, byteLength'),'direct client range missing byte offset/length');
expect(!end.includes('.subarray('),'immediate glEnd still creates subarray views');
expect(end.includes('immediateModeData.vertexBuf, vertexBuffer'),'immediate vertex range not routed from full buffer');
expect(end.includes('0, immediateModeData.vertexPos'),'immediate vertex range length missing');

const calls=[];
const stats={clientRangeUploadCalls:0,clientRangeUploadBytes:0,clientRangeViewsAvoided:0,clientRangeFallbackViews:0,clientMemoryViewRefreshes:0};
const context=vm.createContext({
  glCtx:{ARRAY_BUFFER:34962,STATIC_DRAW:35044,bufferData(...args){calls.push(args);}},
  presentationStats:stats,
  clientRangeUploadEnabled:true,
  clientMemoryByteView:null,
  Number,
  Uint8Array,
});
vm.runInContext(`${getView}\n${upload}`,context);

const floats=new Float32Array([10,20,30,40,50,60,70,80]);
context.uploadBufferDataRange(floats,2,4);
expect(calls.length===1&&calls[0].length===5,'enabled range must use five-argument WebGL2 bufferData');
expect(calls[0][1]===floats&&calls[0][3]===2&&calls[0][4]===4,'enabled range source/offset/length changed');
expect(stats.clientRangeUploadCalls===1&&stats.clientRangeViewsAvoided===1,'enabled range counters wrong');
expect(stats.clientRangeUploadBytes===16,'Float32 range byte count wrong');

calls.length=0; context.clientRangeUploadEnabled=false;
context.uploadBufferDataRange(floats,2,4);
expect(calls.length===1&&calls[0].length===3,'disabled range must use legacy three-argument bufferData');
expect(calls[0][1] instanceof Float32Array,'legacy range must materialize Float32Array subarray');
expect(values(calls[0][1]).join(',')==='30,40,50,60','legacy subarray values differ');
expect(stats.clientRangeFallbackViews===1,'legacy fallback view counter wrong');

calls.length=0; context.clientRangeUploadEnabled=true;
const mem=new ArrayBuffer(512); const bytes=new Uint8Array(mem); for(let i=0;i<bytes.length;i++)bytes[i]=i&255;
const view1=context.getClientMemoryByteView(mem); const view2=context.getClientMemoryByteView(mem);
expect(view1===view2&&view1.byteLength===512,'same memory buffer did not reuse byte view');
expect(stats.clientMemoryViewRefreshes===1,'memory view refreshed more than once for same buffer');
context.uploadBufferDataRange(view1,123,37);
expect(calls[0].length===5&&calls[0][1]===view1&&calls[0][3]===123&&calls[0][4]===37,'Uint8 range offset/length changed');
expect(stats.clientRangeUploadBytes===53,'combined range byte count wrong');
const mem2=new ArrayBuffer(1024); const view3=context.getClientMemoryByteView(mem2);
expect(view3!==view1&&view3.buffer===mem2,'new memory buffer did not refresh byte view');
expect(stats.clientMemoryViewRefreshes===2,'memory refresh counter wrong after buffer replacement');

console.log(`verify-lwjgl-client-range-upload: OK rangeCalls=${stats.clientRangeUploadCalls} bytes=${stats.clientRangeUploadBytes} avoided=${stats.clientRangeViewsAvoided} fallback=${stats.clientRangeFallbackViews} memoryRefresh=${stats.clientMemoryViewRefreshes}`);