'use strict';
const fs = require('fs');
const vm = require('vm');

function extractFunction(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0)throw new Error(`missing ${name}`);
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
function stable(v){return JSON.stringify(v,Object.keys(v||{}).sort());}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-core-state-cache.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('WEBGL_CORE_RENDER_STATE_CACHE_V1'),'missing core-state cache marker');
const names=['hasCoreEnableState','noteCoreStateCall','syncAlphaTestUniforms','setTexture2DEnabled','getCompatEnableState','setCoreEnableState','setCompatEnableState','setCoreBlendFuncSeparate','setCoreBlendFunc','setCoreColorMask','setCoreClearColor','setCoreDepthMask','setCoreDepthFunc','setCoreClearDepth','snapshotAttribState','restoreAttribState'];
const code=names.map(n=>extractFunction(source,n)).join('\n');

function makeContext(cacheEnabled){
  const calls=[];
  const queryCalls=[];
  const uniformCalls=[];
  const glCtx={
    BLEND:3042,CULL_FACE:2884,DEPTH_TEST:2929,SCISSOR_TEST:3089,STENCIL_TEST:2960,TEXTURE_2D:3553,
    ONE:1,ZERO:0,LESS:513,
    BLEND_SRC_RGB:32969,BLEND_DST_RGB:32968,BLEND_SRC_ALPHA:32971,BLEND_DST_ALPHA:32970,
    COLOR_WRITEMASK:3107,COLOR_CLEAR_VALUE:3106,DEPTH_WRITEMASK:2930,DEPTH_FUNC:2932,DEPTH_CLEAR_VALUE:2931,
    VIEWPORT:2978,DEPTH_RANGE:2928,STENCIL_FUNC:2962,STENCIL_REF:2967,STENCIL_VALUE_MASK:2963,STENCIL_WRITEMASK:2968,
    STENCIL_FAIL:2964,STENCIL_PASS_DEPTH_FAIL:2965,STENCIL_PASS_DEPTH_PASS:2966,STENCIL_CLEAR_VALUE:2961,
  };
  const state={
    enables:{[glCtx.BLEND]:false,[glCtx.CULL_FACE]:false,[glCtx.DEPTH_TEST]:false,[glCtx.SCISSOR_TEST]:false,[glCtx.STENCIL_TEST]:false},
    blend:[glCtx.ONE,glCtx.ZERO,glCtx.ONE,glCtx.ZERO], colorMask:[true,true,true,true], clearColor:[0,0,0,0],
    depthMask:true,depthFunc:glCtx.LESS,clearDepth:1,
    viewport:[0,0,1024,768],depthRange:[0,1],stencil:[519,0,0xffffffff,0xffffffff,7680,7680,7680,0]
  };
  glCtx.enable=cap=>{calls.push(['enable',cap]);state.enables[cap]=true;};
  glCtx.disable=cap=>{calls.push(['disable',cap]);state.enables[cap]=false;};
  glCtx.isEnabled=cap=>{queryCalls.push(['isEnabled',cap]);return !!state.enables[cap];};
  glCtx.blendFunc=(s,d)=>{calls.push(['blendFunc',s,d]);state.blend=[s,d,s,d];};
  glCtx.blendFuncSeparate=(sr,dr,sa,da)=>{calls.push(['blendFuncSeparate',sr,dr,sa,da]);state.blend=[sr,dr,sa,da];};
  glCtx.colorMask=(r,g,b,a)=>{calls.push(['colorMask',!!r,!!g,!!b,!!a]);state.colorMask=[!!r,!!g,!!b,!!a];};
  glCtx.clearColor=(r,g,b,a)=>{calls.push(['clearColor',r,g,b,a]);state.clearColor=[r,g,b,a];};
  glCtx.depthMask=v=>{calls.push(['depthMask',!!v]);state.depthMask=!!v;};
  glCtx.depthFunc=v=>{calls.push(['depthFunc',v]);state.depthFunc=v;};
  glCtx.clearDepth=v=>{calls.push(['clearDepth',v]);state.clearDepth=v;};
  glCtx.viewport=(...v)=>{calls.push(['viewport',...v]);state.viewport=v;};
  glCtx.depthRange=(...v)=>{calls.push(['depthRange',...v]);state.depthRange=v;};
  glCtx.stencilFunc=(a,b,c)=>{calls.push(['stencilFunc',a,b,c]);state.stencil[0]=a;state.stencil[1]=b;state.stencil[2]=c;};
  glCtx.stencilMask=v=>{calls.push(['stencilMask',v]);state.stencil[3]=v;};
  glCtx.stencilOp=(a,b,c)=>{calls.push(['stencilOp',a,b,c]);state.stencil[4]=a;state.stencil[5]=b;state.stencil[6]=c;};
  glCtx.clearStencil=v=>{calls.push(['clearStencil',v]);state.stencil[7]=v;};
  glCtx.uniform1f=(loc,val)=>uniformCalls.push([loc,val]);
  glCtx.getParameter=id=>{
    queryCalls.push(['getParameter',id]);
    const map={
      [glCtx.BLEND_SRC_RGB]:state.blend[0],[glCtx.BLEND_DST_RGB]:state.blend[1],[glCtx.BLEND_SRC_ALPHA]:state.blend[2],[glCtx.BLEND_DST_ALPHA]:state.blend[3],
      [glCtx.COLOR_WRITEMASK]:state.colorMask.slice(),[glCtx.COLOR_CLEAR_VALUE]:state.clearColor.slice(),
      [glCtx.DEPTH_WRITEMASK]:state.depthMask,[glCtx.DEPTH_FUNC]:state.depthFunc,[glCtx.DEPTH_CLEAR_VALUE]:state.clearDepth,
      [glCtx.VIEWPORT]:state.viewport.slice(),[glCtx.DEPTH_RANGE]:state.depthRange.slice(),
      [glCtx.STENCIL_FUNC]:state.stencil[0],[glCtx.STENCIL_REF]:state.stencil[1],[glCtx.STENCIL_VALUE_MASK]:state.stencil[2],[glCtx.STENCIL_WRITEMASK]:state.stencil[3],
      [glCtx.STENCIL_FAIL]:state.stencil[4],[glCtx.STENCIL_PASS_DEPTH_FAIL]:state.stencil[5],[glCtx.STENCIL_PASS_DEPTH_PASS]:state.stencil[6],[glCtx.STENCIL_CLEAR_VALUE]:state.stencil[7]
    }; return map[id];
  };
  const presentationStats={coreStateCalls:0,coreStateChanges:0,coreStateSkipped:0,coreStateSnapshotQueriesAvoided:0,alphaUniformUploads:0,alphaUniformUploadsSaved:0,textureMaskUniformUploads:0,textureMaskUniformUploadsSaved:0};
  const coreEnableState=Object.create(null); for(const cap of [glCtx.BLEND,glCtx.CULL_FACE,glCtx.DEPTH_TEST,glCtx.SCISSOR_TEST,glCtx.STENCIL_TEST])coreEnableState[cap]=false;
  const context=vm.createContext({
    glCtx,presentationStats,coreRenderStateCacheEnabled:cacheEnabled,coreEnableState,
    coreColorState:{blendSrcRgb:glCtx.ONE,blendDstRgb:glCtx.ZERO,blendSrcAlpha:glCtx.ONE,blendDstAlpha:glCtx.ZERO,colorMask:[true,true,true,true],clearColor:[0,0,0,0]},
    coreDepthState:{writeMask:true,func:glCtx.LESS,clearValue:1},
    alphaTestState:{enabled:false,func:519,ref:0},alphaUniformState:{enabled:null,func:null,ref:null},uploadedTexture2DEnabled:null,texture2DEnabled:false,
    alphaTestEnabledLocation:'alphaEnabled',alphaFuncLocation:'alphaFunc',alphaRefLocation:'alphaRef',texMaskLocation:'texMask',
    attribStateStack:[],Object,Array,Number,Math,presentationStatsPlaceholder:null
  });
  vm.runInContext(code,context);
  return {context,state,calls,queryCalls,uniformCalls,presentationStats};
}
function snapshotState(x){return JSON.parse(JSON.stringify(x.state));}
function runScenario(cacheEnabled){
  const x=makeContext(cacheEnabled), c=x.context, gl=x.context.glCtx;
  // Prime uniforms; repeating exact values should be cached only in fast mode.
  c.syncAlphaTestUniforms(); c.syncAlphaTestUniforms(); c.setTexture2DEnabled(false); c.setTexture2DEnabled(false);
  // Repeated state changes deliberately exercise duplicate elimination.
  c.setCoreEnableState(gl.BLEND,true); c.setCoreEnableState(gl.BLEND,true);
  c.setCoreEnableState(gl.DEPTH_TEST,true); c.setCoreEnableState(gl.DEPTH_TEST,true);
  c.setCoreBlendFunc(770,771); c.setCoreBlendFunc(770,771);
  c.setCoreColorMask(true,false,true,false); c.setCoreColorMask(true,false,true,false);
  c.setCoreClearColor(.1,.2,.3,.4); c.setCoreClearColor(.1,.2,.3,.4);
  c.setCoreDepthMask(false); c.setCoreDepthMask(false);
  c.setCoreDepthFunc(515); c.setCoreDepthFunc(515);
  c.setCoreClearDepth(.75); c.setCoreClearDepth(.75);
  c.alphaTestState.enabled=true; c.alphaTestState.func=516; c.alphaTestState.ref=.5; c.syncAlphaTestUniforms(); c.syncAlphaTestUniforms();
  c.setTexture2DEnabled(true); c.setTexture2DEnabled(true);
  const beforeQueries=x.queryCalls.length;
  const saved=c.snapshotAttribState(0x6100);
  const snapshotQueries=x.queryCalls.length-beforeQueries;
  // Mutate all tracked state, then restore from snapshot.
  c.setCoreEnableState(gl.BLEND,false); c.setCoreEnableState(gl.DEPTH_TEST,false);
  c.setCoreBlendFunc(1,0); c.setCoreColorMask(false,false,false,false); c.setCoreClearColor(.9,.8,.7,.6);
  c.setCoreDepthMask(true); c.setCoreDepthFunc(gl.LESS); c.setCoreClearDepth(1);
  c.alphaTestState.enabled=false; c.alphaTestState.func=519; c.alphaTestState.ref=0; c.syncAlphaTestUniforms();
  c.setTexture2DEnabled(false);
  c.restoreAttribState(saved);
  return {...x,saved,snapshotQueries,final:snapshotState(x)};
}
const legacy=runScenario(false), fast=runScenario(true);
expect(JSON.stringify(legacy.saved)===JSON.stringify(fast.saved),`snapshot mismatch\nlegacy=${JSON.stringify(legacy.saved)}\nfast=${JSON.stringify(fast.saved)}`);
expect(JSON.stringify(legacy.final)===JSON.stringify(fast.final),`final GL state mismatch\nlegacy=${JSON.stringify(legacy.final)}\nfast=${JSON.stringify(fast.final)}`);
expect(legacy.snapshotQueries===14,`legacy common snapshot should issue 14 queries, got ${legacy.snapshotQueries}`);
expect(fast.snapshotQueries===14,`write-skip split must preserve all 14 snapshot queries, got ${fast.snapshotQueries}`);
expect(fast.presentationStats.coreStateSnapshotQueriesAvoided===0,`write-skip split must not avoid snapshot queries, got ${fast.presentationStats.coreStateSnapshotQueriesAvoided}`);
expect(fast.calls.length<legacy.calls.length,`cache did not reduce GL state calls: fast=${fast.calls.length} legacy=${legacy.calls.length}`);
expect(fast.presentationStats.coreStateSkipped>0,'cache did not record skipped state calls');
expect(fast.uniformCalls.length===legacy.uniformCalls.length,`non-uniform split changed uniform upload count fast=${fast.uniformCalls.length} legacy=${legacy.uniformCalls.length}`);
expect(fast.presentationStats.alphaUniformUploadsSaved===0,'non-uniform split must not cache alpha uniforms');
expect(fast.presentationStats.textureMaskUniformUploadsSaved===0,'non-uniform split must not cache texture-mask uniforms');
// Kill switch must preserve repeated raw state calls and raw snapshot queries.
const raw=makeContext(false), rc=raw.context, rg=raw.context.glCtx;
rc.setCoreEnableState(rg.BLEND,true); rc.setCoreEnableState(rg.BLEND,true);
expect(raw.calls.filter(c=>c[0]==='enable').length===2,'kill switch skipped duplicate glEnable');
const rq=raw.queryCalls.length; rc.snapshotAttribState(0x6100);
expect(raw.queryCalls.length-rq===14,'kill switch did not preserve 14 raw snapshot queries');
console.log(`verify-lwjgl-core-state-cache: OK writeSkip fastCalls=${fast.calls.length} legacyCalls=${legacy.calls.length} skipped=${fast.presentationStats.coreStateSkipped} snapshotQueries=${fast.snapshotQueries} uniformCalls=${fast.uniformCalls.length}`);
