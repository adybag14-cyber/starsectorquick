'use strict';
const fs=require('fs');
function extract(src,name){
  const start=src.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=src.indexOf('{',start); let depth=0,q=null,esc=false;
  for(let i=brace;i<src.length;i++){
    const ch=src[i];
    if(q){ if(esc)esc=false; else if(ch==='\\')esc=true; else if(ch===q)q=null; continue; }
    if(ch==='"'||ch==="'"||ch==='`'){q=ch;continue;}
    if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return src.slice(start,i+1);
  }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2]; if(!path) throw new Error('usage: node ci/verify-lwjgl-detailed-draw-telemetry-static-dispatch.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
expect(src.includes('LWJGL_DETAILED_DRAW_TELEMETRY_STATIC_DISPATCH_V1'),'static telemetry dispatch marker missing');
expect(src.includes('detailedDrawTelemetryStaticDispatchActive: true'),'activation marker missing');
expect(src.includes('if(!detailedDrawTelemetryEnabled) drawArraysImpl = drawArraysImplProduction;'),'draw selector missing');
expect(src.includes('if(!detailedDrawTelemetryEnabled) uploadImmediateInterleaved = uploadImmediateInterleavedProduction;'),'upload selector missing');
const draw=extract(src,'drawArraysImpl');
const prod=extract(src,'drawArraysImplProduction');
const upload=extract(src,'uploadImmediateInterleaved');
const prodUpload=extract(src,'uploadImmediateInterleavedProduction');
expect(draw.includes('detailedDrawTelemetryEnabled'),'diagnostic draw implementation lost telemetry guard');
expect(draw.includes('presentationStats.quadDrawCallsSaved += quadCount - 1'),'diagnostic quad telemetry lost');
expect(upload.includes('presentationStats.immediateInterleavedDraws++'),'diagnostic upload telemetry lost');
for(const [name,body] of [['draw',prod],['upload',prodUpload]]){
  expect(!body.includes('detailedDrawTelemetryEnabled'),`${name} production clone still branches on telemetry flag`);
  expect(!body.includes('presentationStats.legacyDrawCalls'),`${name} production clone still counts legacy draws`);
  expect(!body.includes('presentationStats.webglDrawCalls'),`${name} production clone still counts WebGL draws`);
  expect(!body.includes('presentationStats.immediateInterleavedDraws'),`${name} production clone still counts immediate draws`);
  expect(!body.includes('presentationStats.immediateInterleavedUploads'),`${name} production clone still counts immediate uploads`);
}
// Prove the production clones retain the actual rendering/upload operations.
for(const token of ['glCtx.uniformMatrix4fv','glCtx.drawArrays','glCtx.drawElements','ensureQuadIndexCapacity'])
  expect(prod.includes(token),`draw production clone missing ${token}`);
for(const token of ['glCtx.bindBuffer','glCtx.bufferData','glCtx.vertexAttribPointer','glCtx.enableVertexAttribArray'])
  expect(prodUpload.includes(token),`upload production clone missing ${token}`);
console.log('verify-lwjgl-detailed-draw-telemetry-static-dispatch: OK production=branchless diagnostics=preserved');
