'use strict';
const fs = require('fs');
const vm = require('vm');
function extractFunction(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0)throw new Error(`missing ${name}`);
  const brace=source.indexOf('{',start); let depth=0,quote=null,escaped=false;
  for(let i=brace;i<source.length;i++){
    const ch=source[i];
    if(quote){if(escaped)escaped=false;else if(ch==='\\\\')escaped=true;else if(ch===quote)quote=null;continue;}
    if(ch==='"'||ch==="'"||ch==='`'){quote=ch;continue;}
    if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return source.slice(start,i+1);
  }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-matrix-generation-cache.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('WEBGL_MATRIX_GENERATION_CACHE_V2'),'missing lean matrix generation cache marker');
expect(!source.includes('matrixUniformUploadsSaved'),'cache-hit telemetry must stay off the hot path');
const ensureStack=source.includes('LWJGL_MATRIX_STACK_GUARD_V1') ? extractFunction(source,'ensureCurMatrixStack') : '';
const draw=extractFunction(source,'drawArraysImpl');
const mark=extractFunction(source,'markCurrentDrawMatrixDirty');
const setTop=extractFunction(source,'setCurMatrixTop');
const loadIdentity=extractFunction(source,'Java_org_lwjgl_opengl_GL11_nglLoadIdentity');
const popMatrix=extractFunction(source,'Java_org_lwjgl_opengl_GL11_nglPopMatrix');
expect(draw.includes('uploadedModelViewMatrixGeneration !== modelViewMatrixGeneration'),'model generation check missing');
expect(draw.includes('uploadedProjMatrixGeneration !== projMatrixGeneration'),'projection generation check missing');
expect((draw.match(/matrixUniformUploads\+\+/g)||[]).length===2,'only actual model/projection uploads should increment telemetry');
expect(loadIdentity.includes('markCurrentDrawMatrixDirty();'),'glLoadIdentity must dirty active draw matrix');
expect(popMatrix.includes('markCurrentDrawMatrixDirty();'),'glPopMatrix must dirty restored active draw matrix');
const uploads=[];
const model=[{name:'m0'}], projection=[{name:'p0'}], texture=[{name:'t0'}];
const presentationStats={matrixUniformUploads:0,legacyDrawCalls:0,webglDrawCalls:0,quadBatches:0,quadQuads:0,quadDrawCallsSaved:0};
const glCtx={
  POINTS:0,LINES:1,LINE_LOOP:2,LINE_STRIP:3,TRIANGLES:4,TRIANGLE_STRIP:5,TRIANGLE_FAN:6,UNSIGNED_INT:5125,
  uniformMatrix4fv(loc,transpose,value){uploads.push([loc,transpose,value.name]);},
  drawArrays(){},drawElements(){},bindBuffer(){},ELEMENT_ARRAY_BUFFER:34963
};
const context=vm.createContext({
  glCtx,mvLocation:'mv',projLocation:'proj',modelViewMatrixStack:model,projMatrixStack:projection,textureMatrixStack:texture,
  curMatrixStack:model,modelViewMatrixGeneration:1,projMatrixGeneration:1,uploadedModelViewMatrixGeneration:0,uploadedProjMatrixGeneration:0,
  presentationStats,assert(v){if(!v)throw new Error('assert');},ensureQuadIndexCapacity(){},quadIndexBuffer:{},Object,Number,Math,Array,Set,
  matrixStackWarnings:new Set(),warnOnce(){},glMatrix:{mat4:{create(){return {name:'identity'};}}}
});
vm.runInContext(`${ensureStack}\n${mark}\n${setTop}\n${draw}`,context);
context.drawArraysImpl(glCtx.POINTS,0,1);
expect(uploads.length===2&&presentationStats.matrixUniformUploads===2,'first draw must upload both matrices');
context.drawArraysImpl(glCtx.POINTS,0,1);
expect(uploads.length===2&&presentationStats.matrixUniformUploads===2,'unchanged draw must do zero matrix upload work');
context.curMatrixStack=model; context.setCurMatrixTop({name:'m1'}); context.drawArraysImpl(glCtx.POINTS,0,1);
expect(uploads.length===3&&uploads[2][0]==='mv'&&uploads[2][2]==='m1','model mutation should upload model only');
context.curMatrixStack=projection; context.setCurMatrixTop({name:'p1'}); context.drawArraysImpl(glCtx.POINTS,0,1);
expect(uploads.length===4&&uploads[3][0]==='proj'&&uploads[3][2]==='p1','projection mutation should upload projection only');
const beforeM=context.modelViewMatrixGeneration,beforeP=context.projMatrixGeneration;
context.curMatrixStack=texture; context.setCurMatrixTop({name:'t1'});
expect(context.modelViewMatrixGeneration===beforeM&&context.projMatrixGeneration===beforeP,'texture matrix must not dirty draw uniforms');
context.drawArraysImpl(glCtx.POINTS,0,1);
expect(uploads.length===4,'texture-only mutation must not upload model/projection matrices');
console.log(`verify-lwjgl-matrix-generation-cache: OK uploads=${presentationStats.matrixUniformUploads} draws=${presentationStats.webglDrawCalls} modelGen=${context.modelViewMatrixGeneration} projGen=${context.projMatrixGeneration}`);
