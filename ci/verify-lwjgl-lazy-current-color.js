'use strict';
const fs=require('fs'),vm=require('vm');
function extract(src,name){const a=src.indexOf(`function ${name}(`);if(a<0)throw Error(`missing ${name}`);const b=src.indexOf('{',a);let d=0,q=null,e=false;for(let i=b;i<src.length;i++){const c=src[i];if(q){if(e)e=false;else if(c==='\\')e=true;else if(c===q)q=null;continue;}if(c==='"'||c==="'"||c==='`'){q=c;continue;}if(c==='{')d++;else if(c==='}'&&--d===0)return src.slice(a,i+1);}throw Error(`unterminated ${name}`);}
function expect(v,m){if(!v)throw Error(m);}
const path=process.argv[2];if(!path)throw Error('usage: node ci/verify-lwjgl-lazy-current-color.js <lwjgl.js>');const src=fs.readFileSync(path,'utf8');
expect(src.includes('LWJGL_LAZY_CURRENT_COLOR_ATTRIB_V1'),'marker missing');
const names=['applyCurrentColorAttrib','Java_org_lwjgl_opengl_GL11_nglColor4f','Java_org_lwjgl_opengl_GL11_nglColor3f'];const code=names.map(n=>extract(src,n)).join('\n');
const calls=[];const c=vm.createContext({glCtx:{disableVertexAttribArray:(...a)=>calls.push(['disable',...a]),vertexAttrib4f:(...a)=>calls.push(['color',...a])},colorLocation:4,curList:null,pushInList(){throw Error('list');},immediateModeData:{currentColor:[1,1,1,1]},presentationStats:{lazyCurrentColorObserved:false},immediateAttribEnableDirty:false,verboseLog:false,console});vm.runInContext(code,c);
c.Java_org_lwjgl_opengl_GL11_nglColor4f(null,.1,.2,.3,.4,0);expect(calls.length===0,'glColor4f eagerly wrote WebGL generic color');expect(c.immediateAttribEnableDirty===false,'lazy glColor dirtied enable layout');expect(c.immediateModeData.currentColor.join(',')==='0.1,0.2,0.3,0.4','logical current color wrong');expect(c.presentationStats.lazyCurrentColorObserved===true,'activation missing');
c.Java_org_lwjgl_opengl_GL11_nglColor3f(null,.5,.6,.7,0);expect(calls.length===0,'glColor3f eagerly wrote WebGL generic color');expect(c.immediateModeData.currentColor.join(',')==='0.5,0.6,0.7,1','logical color3 wrong');
c.applyCurrentColorAttrib();expect(calls.length===2&&calls[0][0]==='disable'&&calls[1][0]==='color','lazy materialization behavior changed');expect(c.immediateAttribEnableDirty===true,'materialization did not dirty enable layout');expect(calls[1].slice(2).join(',')==='0.5,0.6,0.7,1','lazy materialized color wrong');
// Structural proof: the disabled color-array path still materializes current color at draw time.
const upload=extract(src,'uploadData');expect(upload.includes('if(attributeLocation == colorLocation)')&&upload.includes('applyCurrentColorAttrib();'),'client color fallback no longer materializes current color');
console.log('verify-lwjgl-lazy-current-color: OK eagerCalls=0 lazyMaterialization=2');
