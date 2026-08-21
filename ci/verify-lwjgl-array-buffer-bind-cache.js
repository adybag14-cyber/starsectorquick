'use strict';
const fs = require('fs');
const vm = require('vm');
function extract(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=source.indexOf('{',start); let depth=0,quote=null,esc=false;
  for(let i=brace;i<source.length;i++){
    const ch=source[i];
    if(quote){ if(esc)esc=false; else if(ch==='\\\\')esc=true; else if(ch===quote)quote=null; continue; }
    if(ch==='"'||ch==="\'"||ch==='`'){quote=ch;continue;}
    if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return source.slice(start,i+1);
  }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){ if(!v) throw new Error(m); }
const path=process.argv[2]; if(!path) throw new Error('usage: node ci/verify-lwjgl-array-buffer-bind-cache.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('LWJGL_ARRAY_BUFFER_BIND_CACHE_V1'),'missing cache marker');
const raw=(source.match(/glCtx\.bindBuffer\(glCtx\.ARRAY_BUFFER/g)||[]).length;
expect(raw===1,`raw ARRAY_BUFFER bind count=${raw}`);
expect(extract(source,'uploadDataImpl').includes('bindArrayBufferCached(buffer);'),'legacy upload bypasses cache');
expect(extract(source,'uploadImmediateInterleaved').includes('bindArrayBufferCached(vertexBuffer);'),'immediate upload bypasses cache');
const calls=[];
const context=vm.createContext({
  glCtx:{ARRAY_BUFFER:0x8892,bindBuffer:(target,obj)=>calls.push([target,obj])},
  presentationStats:{arrayBufferBindCacheHitObserved:false},
  arrayBufferBindCacheEnabled:true,
  currentArrayBufferBinding:null,
});
vm.runInContext(extract(source,'bindArrayBufferCached'),context);
const a={id:'a'}, b={id:'b'};
context.bindArrayBufferCached(a);
context.bindArrayBufferCached(a);
expect(calls.length===1,'repeated identical bind was not skipped');
expect(context.presentationStats.arrayBufferBindCacheHitObserved===true,'cache-hit activation flag missing');
context.bindArrayBufferCached(b);
expect(calls.length===2&&calls[1][1]===b,'changed buffer must bind');
context.bindArrayBufferCached(null);
context.bindArrayBufferCached(null);
expect(calls.length===3&&calls[2][1]===null,'null unbind/cache behavior mismatch');
context.arrayBufferBindCacheEnabled=false;
context.bindArrayBufferCached(null);
context.bindArrayBufferCached(null);
expect(calls.length===5,'disabled cache must preserve repeated physical binds');
console.log(`verify-lwjgl-array-buffer-bind-cache: OK physicalBinds=${calls.length} hit=${context.presentationStats.arrayBufferBindCacheHitObserved}`);
