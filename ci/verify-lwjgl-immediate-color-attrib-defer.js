'use strict';
const fs=require('fs'),vm=require('vm');
function extract(src,name){const a=src.indexOf(`function ${name}(`);if(a<0)throw Error(`missing ${name}`);const b=src.indexOf('{',a);let d=0,q=null,e=false;for(let i=b;i<src.length;i++){const c=src[i];if(q){if(e)e=false;else if(c==='\\\\')e=true;else if(c===q)q=null;continue;}if(c==='"'||c==="\'"||c==='`'){q=c;continue;}if(c==='{')d++;else if(c==='}'&&--d===0)return src.slice(a,i+1);}throw Error(`unterminated ${name}`);}
function expect(v,m){if(!v)throw Error(m);}
const path=process.argv[2];if(!path)throw Error('usage: node ci/verify-lwjgl-immediate-color-attrib-defer.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
expect(src.includes('LWJGL_IMMEDIATE_COLOR_ATTRIB_DEFER_V1'),'marker missing');
for(const t of ['immediateBeginActive = true','immediateBeginActive = false','immediateColorAttribDeferredObserved','LWJGL_IMMEDIATE_STATIC_DISPATCH_V1','Java_org_lwjgl_opengl_GL11_nglColor4f = Java_org_lwjgl_opengl_GL11_nglColor4fLegacy','Java_org_lwjgl_opengl_GL11_nglColor3f = Java_org_lwjgl_opengl_GL11_nglColor3fLegacy'])expect(src.includes(t),`missing ${t}`);
const names=['applyCurrentColorAttrib','Java_org_lwjgl_opengl_GL11_nglBegin','Java_org_lwjgl_opengl_GL11_nglColor4f','Java_org_lwjgl_opengl_GL11_nglColor4fLegacy','Java_org_lwjgl_opengl_GL11_nglColor3f','Java_org_lwjgl_opengl_GL11_nglColor3fLegacy'];
expect(!extract(src,'Java_org_lwjgl_opengl_GL11_nglColor4f').includes('immediateInterleavedEnabled'),'color4f still branches on static interleaved flag');
expect(!extract(src,'Java_org_lwjgl_opengl_GL11_nglColor3f').includes('immediateInterleavedEnabled'),'color3f still branches on static interleaved flag');
const code=names.map(n=>extract(src,n)).join('\n');
const calls=[];
const c=vm.createContext({glCtx:{disableVertexAttribArray:(...a)=>calls.push(['disable',...a]),vertexAttrib4f:(...a)=>calls.push(['color',...a])},colorLocation:4,curList:null,pushInList(){throw Error('list path');},immediateModeData:{mode:0,vertexPos:0,colorPos:0,texCoordPos:0,interleavedPos:0,currentColor:[1,1,1,1],currentTexCoord:[0,0]},immediateBeginActive:false,immediateInterleavedEnabled:true,presentationStats:{immediateColorAttribDeferredObserved:false},verboseLog:false,console});
vm.runInContext(code,c);
c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0);
c.Java_org_lwjgl_opengl_GL11_nglColor4f(null,.1,.2,.3,.4,0);
expect(calls.length===0,'in-begin color emitted generic WebGL attrib calls');
expect(c.presentationStats.immediateColorAttribDeferredObserved===true,'defer activation missing');
expect(c.immediateModeData.currentColor.join(',')==='0.1,0.2,0.3,0.4','current color not updated');
c.immediateBeginActive=false;
c.Java_org_lwjgl_opengl_GL11_nglColor3f(null,.5,.6,.7,0);
expect(calls.length===2&&calls[0][0]==='disable'&&calls[1][0]==='color','outside-begin generic color behavior changed');
expect(calls[1].slice(2).join(',')==='0.5,0.6,0.7,1','outside-begin generic color wrong');
c.immediateInterleavedEnabled=false;c.Java_org_lwjgl_opengl_GL11_nglBegin(null,7,0);calls.length=0;c.Java_org_lwjgl_opengl_GL11_nglColor4fLegacy(null,.9,.8,.7,.6,0);
expect(calls.length===2,'legacy immediate fallback must retain generic color updates');
console.log('verify-lwjgl-immediate-color-attrib-defer: OK staticColorDispatch=true deferred=true fallback=true');
