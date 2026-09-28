'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),os=require('node:os'),path=require('node:path');
const {chromium}=require('playwright');
const {browserRendererProfile,readBrowserGpuIdentity}=require('./browser-renderer-profile');
(async()=>{
 assert.deepEqual(browserRendererProfile('default').args,['--enable-unsafe-swiftshader']);
 assert.throws(()=>browserRendererProfile('unknown'),/Unsupported/);
 assert.throws(()=>browserRendererProfile('--no-sandbox'),/Unsupported/);
 const records=[];
 for(const name of ['default','swiftshader-driver']){
  const profile=browserRendererProfile(name);const browser=await chromium.launch({headless:true,args:profile.args});
  try{
   const page=await browser.newPage({viewport:{width:200,height:200}});
   await page.setContent('<canvas width="64" height="64"></canvas>');
   const record=await page.evaluate(async()=>{
    const canvas=document.querySelector('canvas'),gl=canvas.getContext('webgl2',{antialias:false,preserveDrawingBuffer:true});
    if(!gl)throw new Error('No WebGL2');
    gl.clearColor(17/255,34/255,51/255,1);gl.clear(gl.COLOR_BUFFER_BIT);
    const pixel=new Uint8Array(4);gl.readPixels(20,20,1,1,gl.RGBA,gl.UNSIGNED_BYTE,pixel);
    const info=gl.getExtension('WEBGL_debug_renderer_info');
    let frames=0;for(let i=0;i<4;i++)await new Promise(resolve=>requestAnimationFrame(()=>{frames++;resolve();}));
    return {pixel:Array.from(pixel),frames,error:gl.getError(),renderer:info?gl.getParameter(info.UNMASKED_RENDERER_WEBGL):gl.getParameter(gl.RENDERER)};
   });
   assert.deepEqual(record.pixel,[17,34,51,255]);assert.equal(record.frames,4);assert.equal(record.error,0);
   if(name==='swiftshader-driver')assert.match(record.renderer,/SwiftShader/i);
   records.push({profile:await readBrowserGpuIdentity(browser,profile),...record});
  }finally{await browser.close();}
 }
 const destination=process.env.STARSECTOR_BROWSER_PROFILE_OUTPUT||'test_output/browser-profile-contract.json';
 fs.mkdirSync(path.dirname(destination),{recursive:true});fs.writeFileSync(destination,JSON.stringify(records,null,2));
 console.log('test-browser-renderer-profile: OK real pixels, RAF progress; requested driver mode remains explicit CPU SwiftShader');
 console.log(JSON.stringify(records,null,2));
})().catch(error=>{console.error(error);process.exitCode=1;});
