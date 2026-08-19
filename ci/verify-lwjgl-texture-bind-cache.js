'use strict';
const fs = require('fs');
const vm = require('vm');
function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}(`);
  if (start < 0) throw new Error(`missing function ${name}`);
  const brace = source.indexOf('{', start);
  let depth=0, quote=null, escaped=false;
  for(let i=brace;i<source.length;i++) {
    const ch=source[i];
    if(quote){ if(escaped) escaped=false; else if(ch==='\\') escaped=true; else if(ch===quote) quote=null; continue; }
    if(ch==='"'||ch==="'"||ch==='`'){quote=ch;continue;}
    if(ch==='{') depth++; else if(ch==='}' && --depth===0) return source.slice(start,i+1);
  }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){ if(!v) throw new Error(m); }
const path=process.argv[2]; if(!path) throw new Error('usage: node ci/verify-lwjgl-texture-bind-cache.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('WEBGL_TEXTURE_BIND_CACHE_V1'),'missing texture bind cache marker');
const invalidate=extractFunction(source,'invalidateBoundTexture2D');
const bind=extractFunction(source,'Java_org_lwjgl_opengl_GL11_nglBindTexture');
const resize=extractFunction(source,'ensureFramebufferSize');
expect(resize.includes('invalidateBoundTexture2D();'),'framebuffer resize does not invalidate game texture cache');
const calls=[];
const textureObjects=[null,{id:1},{id:2}];
const presentationStats={textureBindCalls:0,textureBindChanges:0,textureBindSkipped:0,textureBindInvalidations:0};
const context=vm.createContext({
  glCtx:{TEXTURE_2D:3553,bindTexture(target,obj){calls.push([target,obj&&obj.id||0]);}},
  textureObjects,presentationStats,textureBindCacheEnabled:true,boundTexture2DId:-1,
  curList:null,pushInList(){throw new Error('unexpected display-list recording');},
  assert(v){if(!v)throw new Error('assert');}
});
vm.runInContext(`${invalidate}\n${bind}`,context);
context.Java_org_lwjgl_opengl_GL11_nglBindTexture(null,3553,1,0);
expect(calls.length===1 && calls[0][1]===1,'first texture bind must reach WebGL');
context.Java_org_lwjgl_opengl_GL11_nglBindTexture(null,3553,1,0);
expect(calls.length===1,'duplicate texture bind was not skipped');
expect(presentationStats.textureBindSkipped===1,'duplicate skip counter wrong');
context.Java_org_lwjgl_opengl_GL11_nglBindTexture(null,3553,2,0);
expect(calls.length===2 && calls[1][1]===2,'different texture must bind');
context.invalidateBoundTexture2D();
context.Java_org_lwjgl_opengl_GL11_nglBindTexture(null,3553,2,0);
expect(calls.length===3 && calls[2][1]===2,'invalidated same-id bind must reach WebGL');
expect(presentationStats.textureBindInvalidations===1,'invalidation counter wrong');
context.textureBindCacheEnabled=false;
context.Java_org_lwjgl_opengl_GL11_nglBindTexture(null,3553,2,0);
context.Java_org_lwjgl_opengl_GL11_nglBindTexture(null,3553,2,0);
expect(calls.length===5,'kill switch must restore every legacy bind');
expect(presentationStats.textureBindCalls===6 && presentationStats.textureBindChanges===5 && presentationStats.textureBindSkipped===1,
  `counter mismatch ${JSON.stringify(presentationStats)}`);
console.log(`verify-lwjgl-texture-bind-cache: OK calls=${presentationStats.textureBindCalls} changes=${presentationStats.textureBindChanges} skipped=${presentationStats.textureBindSkipped} invalidations=${presentationStats.textureBindInvalidations}`);