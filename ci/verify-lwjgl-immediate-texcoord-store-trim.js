'use strict';
const fs = require('fs');
function expect(v,m){ if(!v) throw new Error(m); }
function extract(src,name){
  const start=src.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=src.indexOf('{',start); let depth=0,q=null,esc=false;
  for(let i=brace;i<src.length;i++){
    const ch=src[i];
    if(q){ if(esc) esc=false; else if(ch==='\\') esc=true; else if(ch===q) q=null; continue; }
    if(ch==='"'||ch==="'"||ch==='`'){ q=ch; continue; }
    if(ch==='{') depth++; else if(ch==='}' && --depth===0) return src.slice(start,i+1);
  }
  throw new Error(`unterminated ${name}`);
}
const path=process.argv[2]; if(!path) throw new Error('usage: node ci/verify-lwjgl-immediate-texcoord-store-trim.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
expect(src.includes('LWJGL_IMMEDIATE_IMPLICIT_TEXCOORD_STORE_TRIM_V1'),'marker missing');
expect(src.includes('immediateImplicitTexCoordStoreTrimActive: true'),'activation telemetry missing');
const append=extract(src,'appendImmediateVertex');
const fastStart=append.indexOf('if(immediateInterleavedEnabled)');
const fastEnd=append.indexOf('\t\treturn;',fastStart);
expect(fastStart>=0&&fastEnd>fastStart,'interleaved append block missing');
const fast=append.slice(fastStart,fastEnd);
expect(!fast.includes('currentTexCoord[0] = texS'),'interleaved append still rewrites texcoord S');
expect(!fast.includes('currentTexCoord[1] = texT'),'interleaved append still rewrites texcoord T');
const explicit=extract(src,'Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord');
for(const required of ['currentTexCoord[0] = texS','currentTexCoord[1] = texT','appendImmediateVertex(x, y, z, texS, texT)'])
  expect(explicit.includes(required),`explicit fused vertex lost ${required}`);
expect(explicit.indexOf('currentTexCoord[1] = texT') < explicit.indexOf('appendImmediateVertex('),'explicit current texcoord update must precede append');
const implicit=extract(src,'Java_org_lwjgl_opengl_GL11_nglVertex3f');
expect(implicit.includes('currentTexCoord[0]'),'implicit vertex no longer reads current texcoord S');
expect(implicit.includes('currentTexCoord[1]'),'implicit vertex no longer reads current texcoord T');
expect(!implicit.includes('currentTexCoord[0] ='),'implicit vertex unexpectedly writes current texcoord S');
expect(!implicit.includes('currentTexCoord[1] ='),'implicit vertex unexpectedly writes current texcoord T');
const setter=extract(src,'Java_org_lwjgl_opengl_GL11_nglTexCoord2f');
expect(setter.includes('currentTexCoord[0] = x')&&setter.includes('currentTexCoord[1] = y'),'glTexCoord2f lost current-state updates');
console.log('verify-lwjgl-immediate-texcoord-store-trim: OK implicitStores=0 explicitState=preserved');
