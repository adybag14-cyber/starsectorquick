'use strict';
const fs=require('fs'),vm=require('vm');
function extractFunction(source,name){const start=source.indexOf(`function ${name}(`);if(start<0)throw new Error(`missing ${name}`);const brace=source.indexOf('{',start);let d=0,q=null,e=false;for(let i=brace;i<source.length;i++){const ch=source[i];if(q){if(e)e=false;else if(ch==='\\')e=true;else if(ch===q)q=null;continue;}if(ch==='"'||ch==="'"||ch==='`'){q=ch;continue;}if(ch==='{')d++;else if(ch==='}'&&--d===0)return source.slice(start,i+1);}throw new Error(`unterminated ${name}`);}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2];if(!path)throw new Error('usage: node ci/verify-lwjgl-color-attrib-cache.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');expect(source.includes('WEBGL_COLOR_ATTRIB_CACHE_V1'),'missing marker');
const setFn=extractFunction(source,'setColorAttribArrayEnabled'),applyFn=extractFunction(source,'applyCurrentColorAttrib'),uploadFn=extractFunction(source,'uploadDataImpl');
expect(uploadFn.includes('if(attributeLocation == colorLocation) setColorAttribArrayEnabled(true);'),'color upload not routed through cache');
function run(cache){
 const calls=[]; const state={enabled:false,color:[0,0,0,1]};
 const glCtx={enableVertexAttribArray(loc){calls.push(['enable',loc]);state.enabled=true;},disableVertexAttribArray(loc){calls.push(['disable',loc]);state.enabled=false;},vertexAttrib4f(loc,r,g,b,a){calls.push(['color',loc,r,g,b,a]);state.color=[r,g,b,a];}};
 const presentationStats={colorAttribArrayChanges:0,colorAttribArrayCallsSaved:0,colorAttribConstantUploads:0,colorAttribConstantUploadsSaved:0};
 const immediateModeData={currentColor:[1,1,1,1]};
 const context=vm.createContext({glCtx,presentationStats,immediateModeData,colorLocation:3,colorAttribCacheEnabled:cache,colorAttribArrayEnabled:false,uploadedConstantColor:[null,null,null,null]});
 vm.runInContext(`${setFn}\n${applyFn}`,context);
 context.applyCurrentColorAttrib();
 context.applyCurrentColorAttrib();
 context.setColorAttribArrayEnabled(true);
 context.applyCurrentColorAttrib();
 immediateModeData.currentColor[0]=.25; immediateModeData.currentColor[1]=.5; immediateModeData.currentColor[2]=.75; immediateModeData.currentColor[3]=.8;
 context.applyCurrentColorAttrib(); context.applyCurrentColorAttrib();
 return {calls,state,presentationStats,context};
}
const legacy=run(false),fast=run(true);
expect(JSON.stringify(legacy.state)===JSON.stringify(fast.state),`final state mismatch legacy=${JSON.stringify(legacy.state)} fast=${JSON.stringify(fast.state)}`);
expect(fast.calls.length<legacy.calls.length,`cache did not reduce calls fast=${fast.calls.length} legacy=${legacy.calls.length}`);
expect(fast.presentationStats.colorAttribArrayCallsSaved>0,'no color-array calls saved');
expect(fast.presentationStats.colorAttribConstantUploadsSaved>0,'no constant-color uploads saved');
// The fast sequence should be: white constant, array enable, array disable, changed constant.
expect(JSON.stringify(fast.calls)===JSON.stringify([['color',3,1,1,1,1],['enable',3],['disable',3],['color',3,.25,.5,.75,.8]]),`unexpected fast call sequence ${JSON.stringify(fast.calls)}`);
// Kill switch reproduces two calls for each plain apply, including redundant disable/color.
const rawCalls=[];const rawState={enabled:false,color:[0,0,0,1]};const rawCtx=vm.createContext({glCtx:{enableVertexAttribArray(l){rawCalls.push(['enable',l]);rawState.enabled=true;},disableVertexAttribArray(l){rawCalls.push(['disable',l]);rawState.enabled=false;},vertexAttrib4f(l,r,g,b,a){rawCalls.push(['color',l,r,g,b,a]);rawState.color=[r,g,b,a];}},presentationStats:{colorAttribArrayChanges:0,colorAttribArrayCallsSaved:0,colorAttribConstantUploads:0,colorAttribConstantUploadsSaved:0},immediateModeData:{currentColor:[1,1,1,1]},colorLocation:3,colorAttribCacheEnabled:false,colorAttribArrayEnabled:false,uploadedConstantColor:[null,null,null,null]});vm.runInContext(`${setFn}\n${applyFn}`,rawCtx);rawCtx.applyCurrentColorAttrib();rawCtx.applyCurrentColorAttrib();expect(rawCalls.length===4&&rawCalls[0][0]==='disable'&&rawCalls[1][0]==='color'&&rawCalls[2][0]==='disable'&&rawCalls[3][0]==='color',`kill switch altered legacy apply calls ${JSON.stringify(rawCalls)}`);
console.log(`verify-lwjgl-color-attrib-cache: OK fastCalls=${fast.calls.length} legacyCalls=${legacy.calls.length} arraySaved=${fast.presentationStats.colorAttribArrayCallsSaved} colorSaved=${fast.presentationStats.colorAttribConstantUploadsSaved}`);
