'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),os=require('node:os'),path=require('node:path');
const {browserRendererProfile,readBrowserGpuIdentity}=require('./browser-renderer-profile');
(async()=>{
 assert.deepEqual(browserRendererProfile('default').args,['--enable-unsafe-swiftshader']);
 assert.throws(()=>browserRendererProfile('unknown'),/Unsupported/);
 assert.throws(()=>browserRendererProfile('--no-sandbox'),/Unsupported/);
 assert.deepEqual(browserRendererProfile('default'),{name:'default',args:['--enable-unsafe-swiftshader']});
 assert.deepEqual(browserRendererProfile('swiftshader-driver'),{name:'swiftshader-driver',
  args:['--enable-unsafe-swiftshader','--enable-gpu','--use-gl=angle','--use-angle=swiftshader']});
 const hardware=browserRendererProfile('d3d11-hardware');
 assert.deepEqual(hardware,{name:'d3d11-hardware',args:['--enable-gpu','--use-gl=angle','--use-angle=d3d11']});
 const hardwareGpu={
  featureStatus:{webgl:'enabled',gpu_compositing:'enabled'},
  // Presence of an unused software adapter must not reject the active GPU.
  devices:[{vendorId:4318,deviceString:'NVIDIA GeForce RTX 4090'},
   {vendorId:5140,deviceString:'Microsoft Basic Render Driver'}],
  auxAttributes:{glVendor:'Google Inc. (NVIDIA)',
   glRenderer:'ANGLE (NVIDIA, NVIDIA GeForce RTX 4090 Direct3D11 vs_5_0 ps_5_0, D3D11)',
   displayType:'ANGLE_D3D11'}
 };
 async function readFixture(gpu,profile=hardware){
  let detached=false;
  const browser={version:()=> 'fixture-chromium',newBrowserCDPSession:async()=>({
   send:async command=>{assert.equal(command,'SystemInfo.getInfo');return {gpu};},
   detach:async()=>{detached=true;}
  })};
  try{return await readBrowserGpuIdentity(browser,profile);}
  finally{assert.equal(detached,true,'identity validation must detach its CDP session');}
 }
 const identity=await readFixture(hardwareGpu);
 for(const [profileName,driver] of [['mesa-llvmpipe','llvmpipe'],['swiftshader-driver','SwiftShader']]){
  const profile=browserRendererProfile(profileName);
  assert.equal((await readFixture({...hardwareGpu,auxAttributes:{glRenderer:driver}},profile)).requested,profileName);
  await assert.rejects(readFixture(hardwareGpu,profile),/renderer is not active/);
 }
 assert.equal(identity.requested,'d3d11-hardware');
 assert.deepEqual(identity.args,hardware.args,'metadata records exact launch arguments');
 assert.equal(identity.gl.glRenderer,hardwareGpu.auxAttributes.glRenderer);
 assert.equal(identity.devices.length,2);
 for(const renderer of [
  'ANGLE (Google, Vulkan SwiftShader Device, SwiftShader driver)',
  'ANGLE (Microsoft, Microsoft Basic Render Driver Direct3D11, D3D11)',
  'ANGLE (Microsoft, WARP Direct3D11, D3D11)',
  'llvmpipe (LLVM 19.1.7, 256 bits)',
  'ANGLE (NVIDIA, OpenGL 4.6)',
  ''
 ]){
  await assert.rejects(readFixture({...hardwareGpu,
   auxAttributes:{...hardwareGpu.auxAttributes,glRenderer:renderer}}),/Direct3D11 hardware renderer is not active/);
 }
 await assert.rejects(readFixture({...hardwareGpu,
  featureStatus:{webgl:'unavailable_software',gpu_compositing:'enabled'}}),/Direct3D11 hardware renderer is not active/);
 const softwareGpu={...hardwareGpu,auxAttributes:{glRenderer:'ANGLE (Google, SwiftShader)'}};
 for(const name of ['default','swiftshader-driver']){
  const profile=browserRendererProfile(name);
  assert.equal((await readFixture(softwareGpu,profile)).requested,name,'existing software profiles remain unchanged');
 }
 if(process.argv.includes('--unit')){
  console.log('test-browser-renderer-profile: OK profile arguments, metadata, hardware identity and fallback rejection (no browser launched)');
  return;
 }
 const {chromium}=require('playwright');
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
