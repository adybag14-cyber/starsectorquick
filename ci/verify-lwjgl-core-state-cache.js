'use strict';
const fs=require('fs'), vm=require('vm');
function extract(source,name){const start=source.indexOf(`function ${name}(`);if(start<0)throw new Error(`missing ${name}`);const brace=source.indexOf('{',start);let depth=0,q=null,esc=false;for(let i=brace;i<source.length;i++){const ch=source[i];if(q){if(esc)esc=false;else if(ch==='\\\\')esc=true;else if(ch===q)q=null;continue;}if(ch==='"'||ch==="'"||ch==='`'){q=ch;continue;}if(ch==='{')depth++;else if(ch==='}'&&--depth===0)return source.slice(start,i+1);}throw new Error(`unterminated ${name}`);}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2];if(!path)throw new Error('usage: node ci/verify-lwjgl-core-state-cache.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('WEBGL_CORE_ENABLE_QUERY_CACHE_V1'),'missing enable-query cache marker');
expect(!source.includes('coreColorState'),'enable-only candidate must not shadow color state');
expect(!source.includes('coreDepthState'),'enable-only candidate must not shadow depth state');
const names=['hasCoreEnableState','noteCoreEnableStateCall','syncAlphaTestUniforms','setTexture2DEnabled','getCompatEnableState','setCoreEnableState','setCompatEnableState','snapshotAttribState','restoreAttribState'];
const code=names.map(n=>extract(source,n)).join('\n');
function make(cache){
 const calls=[],queries=[],uniforms=[];
 const gl={BLEND:3042,CULL_FACE:2884,DEPTH_TEST:2929,SCISSOR_TEST:3089,STENCIL_TEST:2960,TEXTURE_2D:3553,
   BLEND_SRC_RGB:32969,BLEND_DST_RGB:32968,BLEND_SRC_ALPHA:32971,BLEND_DST_ALPHA:32970,COLOR_WRITEMASK:3107,COLOR_CLEAR_VALUE:3106,
   DEPTH_WRITEMASK:2930,DEPTH_FUNC:2932,DEPTH_CLEAR_VALUE:2931};
 const state={en:{[gl.BLEND]:false,[gl.CULL_FACE]:false,[gl.DEPTH_TEST]:false,[gl.SCISSOR_TEST]:false,[gl.STENCIL_TEST]:false}};
 gl.enable=c=>{calls.push(['enable',c]);state.en[c]=true};gl.disable=c=>{calls.push(['disable',c]);state.en[c]=false};
 gl.isEnabled=c=>{queries.push(['isEnabled',c]);return !!state.en[c]};gl.uniform1f=(l,v)=>uniforms.push([l,v]);
 gl.getParameter=id=>{queries.push(['getParameter',id]);const map={[gl.BLEND_SRC_RGB]:1,[gl.BLEND_DST_RGB]:0,[gl.BLEND_SRC_ALPHA]:1,[gl.BLEND_DST_ALPHA]:0,[gl.COLOR_WRITEMASK]:[true,true,true,true],[gl.COLOR_CLEAR_VALUE]:[0,0,0,0],[gl.DEPTH_WRITEMASK]:true,[gl.DEPTH_FUNC]:513,[gl.DEPTH_CLEAR_VALUE]:1};return map[id]};
 gl.blendFuncSeparate=()=>{};gl.colorMask=()=>{};gl.clearColor=()=>{};gl.depthMask=()=>{};gl.depthFunc=()=>{};gl.clearDepth=()=>{};
 const coreEnableState=Object.create(null);for(const c of [gl.BLEND,gl.CULL_FACE,gl.DEPTH_TEST,gl.SCISSOR_TEST,gl.STENCIL_TEST])coreEnableState[c]=false;
 const stats={coreStateCalls:0,coreStateChanges:0,coreStateSkipped:0,coreStateSnapshotQueriesAvoided:0};
 const c=vm.createContext({glCtx:gl,coreEnableQueryCacheEnabled:cache,coreEnableState,presentationStats:stats,alphaTestState:{enabled:false,func:519,ref:0},texture2DEnabled:false,attribStateStack:[],Object,Array,Number,Math,alphaTestEnabledLocation:'ae',alphaFuncLocation:'af',alphaRefLocation:'ar',texMaskLocation:'tm'});
 vm.runInContext(code,c);return {c,gl,state,calls,queries,uniforms,stats};
}
function run(cache){const x=make(cache),c=x.c,g=x.gl;c.setCoreEnableState(g.BLEND,true);c.setCoreEnableState(g.BLEND,true);c.setCoreEnableState(g.DEPTH_TEST,true);c.setCoreEnableState(g.DEPTH_TEST,true);c.alphaTestState.enabled=true;c.setTexture2DEnabled(true);let q=x.queries.length;const saved=c.snapshotAttribState(0x2000);const enableQueries=x.queries.length-q;c.setCoreEnableState(g.BLEND,false);c.setCoreEnableState(g.DEPTH_TEST,false);c.alphaTestState.enabled=false;c.setTexture2DEnabled(false);c.restoreAttribState(saved);q=x.queries.length;c.snapshotAttribState(0x4100);const colorDepthQueries=x.queries.length-q;return {...x,saved,enableQueries,colorDepthQueries};}
const raw=run(false),fast=run(true);
expect(JSON.stringify(raw.saved)===JSON.stringify(fast.saved),'enable snapshot mismatch');
expect(raw.enableQueries===5,`raw GL_ENABLE_BIT must issue 5 isEnabled queries, got ${raw.enableQueries}`);
expect(fast.enableQueries===0,`enable cache must issue zero isEnabled queries, got ${fast.enableQueries}`);
expect(raw.colorDepthQueries===9&&fast.colorDepthQueries===9,`color/depth queries must remain raw 9/9, got ${raw.colorDepthQueries}/${fast.colorDepthQueries}`);
expect(fast.stats.coreStateSnapshotQueriesAvoided===5,`expected 5 avoided queries, got ${fast.stats.coreStateSnapshotQueriesAvoided}`);
expect(fast.calls.length===raw.calls.length,`enable-query cache changed GL write count ${fast.calls.length}/${raw.calls.length}`);
expect(fast.stats.coreStateSkipped===0,'enable-query cache must never skip a state write');
expect(fast.stats.coreStateCalls===fast.stats.coreStateChanges,'tracked writes must remain 1:1');
expect(fast.state.en[fast.gl.BLEND]===true&&fast.state.en[fast.gl.DEPTH_TEST]===true,'restore did not recover tracked enable state');
console.log(`verify-lwjgl-core-state-cache: OK enableOnly writes=${fast.stats.coreStateCalls} avoided=${fast.stats.coreStateSnapshotQueriesAvoided} rawColorDepthQueries=${fast.colorDepthQueries}`);
