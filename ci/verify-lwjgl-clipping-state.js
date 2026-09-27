#!/usr/bin/env node
'use strict';
// Real WebGL regression corpus. Uses the shipped bridge function bodies, not a
// parallel implementation; the expected mask ownership is defined explicitly.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');

(async () => {
  const sourcePath = process.argv[2] || 'build/final/wasm-modules/lwjgl.js';
  const source = fs.readFileSync(sourcePath, 'utf8');
  const section = marker => {
    const begin=source.indexOf('// '+marker+'_BEGIN'), end=source.indexOf('// '+marker+'_END');
    assert.ok(begin>=0 && end>begin,marker);return source.slice(begin,end);
  };
  const fn = name => {
    const begin=source.indexOf('function '+name+'(');assert.ok(begin>=0,name);
    const end=Math.min(...['\nfunction ','\nvar '].map(s=>source.indexOf(s,begin+10)).filter(i=>i>begin));
    return source.slice(begin,end);
  };
  const names=['getCompatEnableState','setCompatEnableState','snapshotAttribState','restoreAttribState',
    'Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers'];
  const optional=['presentMainFramebuffer'];
  const code=require('./renderer-test-helpers').vertexArraySection(source) + [section('LWJGL_COLOR_ATTRIB_SNAPSHOT_CACHE_V1'),section('LWJGL_EXTENDED_ATTRIB_SNAPSHOT_CACHE_V1'),
    ...names.map(fn),...optional.filter(name=>source.includes('function '+name+'(')).map(fn)].join('\n');
  const browser=await chromium.launch({headless:true,args:['--enable-unsafe-swiftshader']});
  let result;
  try {
    const page=await browser.newPage();
    await page.setContent('<canvas id="gl" width="32" height="32"></canvas>');
    result=await page.evaluate(code=>new Function('code',`
      const canvas=document.getElementById('gl');
      const glCtx=canvas.getContext('webgl2',{alpha:false,antialias:false,stencil:true,preserveDrawingBuffer:true});
      if(!glCtx)throw new Error('WebGL2 is required');
      const alphaTestState={enabled:false,func:519,ref:0};let texture2DEnabled=false;
      const immediateModeData={currentColor:[1,1,1,1],currentTexCoord:[0,0]};
      const texCoordData={enabled:true},textureObjects={};let boundTexture2DId=0;const texCoord=0;
      function syncAlphaTestUniforms(){} function setTexture2DEnabled(v){texture2DEnabled=!!v;}
      function applyCurrentColorAttrib(){}
      const verboseLog=false,presentationReadbackDiagnostics=false;
      const presentationStats={swapCount:0,scissorProtectedPresents:0,samples:[]};let frameCount=0,frameLimit=0;
      const fbWidth=32,fbHeight=32;
      const mainFb=glCtx.createFramebuffer();glCtx.bindFramebuffer(glCtx.FRAMEBUFFER,mainFb);
      const color=glCtx.createRenderbuffer();glCtx.bindRenderbuffer(glCtx.RENDERBUFFER,color);
      glCtx.renderbufferStorage(glCtx.RENDERBUFFER,glCtx.RGBA8,32,32);
      glCtx.framebufferRenderbuffer(glCtx.FRAMEBUFFER,glCtx.COLOR_ATTACHMENT0,glCtx.RENDERBUFFER,color);
      if(glCtx.checkFramebufferStatus(glCtx.FRAMEBUFFER)!==glCtx.FRAMEBUFFER_COMPLETE)throw new Error('Bad fixture framebuffer');
      function ensureFramebufferSize(){} function recordPresentationFrameInterval(){} function compatFinishFrame(){} function campaignFrameBoundary(){}
      eval(code);
      const checks=[];let assertions=0;
      const same=(a,b,label)=>{assertions++;if(JSON.stringify(a)!==JSON.stringify(b))throw new Error(label+' '+JSON.stringify({actual:a,expected:b}));};
      const check=(name,test)=>{try{test();checks.push({name,passed:true});}catch(error){checks.push({name,passed:false,error:error.message});}};
      const COLOR=0x4000,DEPTH=0x0100,STENCIL=0x0400,ENABLE=0x2000,SCISSOR=0x80000,VIEWPORT=0x0800;
      const caps=[glCtx.BLEND,glCtx.DEPTH_TEST,glCtx.STENCIL_TEST,glCtx.SCISSOR_TEST,0x0BC0];
      const reset=()=>{
        window.__LWJGL_EXTENDED_ATTRIB_CACHE__=false;invalidateExtendedAttribState();colorAttribShadow=null;
        glCtx.bindFramebuffer(glCtx.FRAMEBUFFER,mainFb);glCtx.viewport(0,0,32,32);
        glCtx.scissor(0,0,32,32);for(const cap of [...caps,glCtx.CULL_FACE])setCompatEnableState(cap,false);
        glCtx.colorMask(true,true,true,true);glCtx.clearColor(0,0,0,1);glCtx.clear(glCtx.COLOR_BUFFER_BIT);
      };
      const flags=()=>caps.map(cap=>getCompatEnableState(cap));
      const box=()=>Array.from(glCtx.getParameter(glCtx.SCISSOR_BOX));
      const pixels=target=>{glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER,target);const bytes=new Uint8Array(32*32*4);glCtx.readPixels(0,0,32,32,glCtx.RGBA,glCtx.UNSIGNED_BYTE,bytes);return Array.from(bytes);};
      // A shader-based full-screen draw proves this is valid drawing being
      // clipped, not merely equality of two empty framebuffer captures.
      const shader=(type,text)=>{const s=glCtx.createShader(type);glCtx.shaderSource(s,text);glCtx.compileShader(s);if(!glCtx.getShaderParameter(s,glCtx.COMPILE_STATUS))throw new Error(glCtx.getShaderInfoLog(s));return s;};
      const program=glCtx.createProgram();
      glCtx.attachShader(program,shader(glCtx.VERTEX_SHADER,'#version 300 es\\nvoid main(){vec2 p=vec2(float((gl_VertexID<<1)&2),float(gl_VertexID&2));gl_Position=vec4(p*2.0-1.0,0,1);}'));
      glCtx.attachShader(program,shader(glCtx.FRAGMENT_SHADER,'#version 300 es\\nprecision highp float; uniform vec4 tint; out vec4 c;void main(){c=tint;}'));
      glCtx.linkProgram(program);if(!glCtx.getProgramParameter(program,glCtx.LINK_STATUS))throw new Error(glCtx.getProgramInfoLog(program));
      glCtx.useProgram(program);const tint=glCtx.getUniformLocation(program,'tint');
      const draw=(r,g,b)=>{glCtx.uniform4f(tint,r,g,b,1);glCtx.drawArrays(glCtx.TRIANGLES,0,3);};
      check('scissor-box-and-enable-restore',()=>{
        reset();setCompatEnableState(glCtx.SCISSOR_TEST,true);const saved=snapshotAttribState(SCISSOR);
        glCtx.scissor(0,0,0,0);setCompatEnableState(glCtx.SCISSOR_TEST,false);restoreAttribState(saved);
        same(box(),[0,0,32,32],'box');same(getCompatEnableState(glCtx.SCISSOR_TEST),true,'enabled');
      });
      check('scissor-disabled-state-restores',()=>{
        reset();const saved=snapshotAttribState(SCISSOR);setCompatEnableState(glCtx.SCISSOR_TEST,true);restoreAttribState(saved);
        same(getCompatEnableState(glCtx.SCISSOR_TEST),false,'disabled');
      });
      check('valid-drawing-survives-zero-area-nested-clip',()=>{
        reset();setCompatEnableState(glCtx.SCISSOR_TEST,true);const saved=snapshotAttribState(SCISSOR);
        glCtx.scissor(0,0,0,0);draw(0,1,0);restoreAttribState(saved);draw(1,0,0);
        const data=pixels(mainFb);let red=0;for(let i=0;i<data.length;i+=4)if(data[i]===255&&data[i+1]===0&&data[i+2]===0)red++;
        same(red,1024,'all visible red pixels');
      });
      for(const [name,mask,cap] of [['blend',COLOR,glCtx.BLEND],['alpha',COLOR,0x0BC0],['depth',DEPTH,glCtx.DEPTH_TEST],['stencil',STENCIL,glCtx.STENCIL_TEST]]){
        check(name+'-group-owns-enable',()=>{reset();setCompatEnableState(cap,true);const s=snapshotAttribState(mask);setCompatEnableState(cap,false);restoreAttribState(s);same(getCompatEnableState(cap),true,name);});
      }
      check('enable-mask-does-not-restore-unsaved-box',()=>{reset();const s=snapshotAttribState(ENABLE);glCtx.scissor(2,3,4,5);setCompatEnableState(glCtx.SCISSOR_TEST,true);restoreAttribState(s);same(box(),[2,3,4,5],'unsaved box');same(getCompatEnableState(glCtx.SCISSOR_TEST),false,'enable');});
      // Every subset of the six groups, in both cache modes. Explicit ownership
      // prevents accidentally restoring all state for a partial mask.
      const groups=[COLOR,DEPTH,STENCIL,ENABLE,SCISSOR,VIEWPORT];
      for(const cache of [false,true])for(let bits=0;bits<64;bits++){
        const mask=groups.reduce((m,g,i)=>m|((bits&(1<<i))?g:0),0);
        check('mixed-mask-'+bits+'-cache-'+cache,()=>{
          reset();window.__LWJGL_EXTENDED_ATTRIB_CACHE__=cache;
          const saved=snapshotAttribState(mask);for(const cap of caps)setCompatEnableState(cap,true);glCtx.scissor(2,3,4,5);
          restoreAttribState(saved);
          const expected=[COLOR,DEPTH,STENCIL,SCISSOR,COLOR].map(owner=>!(mask&(owner|ENABLE)));
          same(flags(),expected,'saved enable ownership');same(box(),mask&SCISSOR?[0,0,32,32]:[2,3,4,5],'saved box ownership');
        });
      }
      check('nested-boxes-dont-alias',()=>{
        reset();const outer=snapshotAttribState(SCISSOR);glCtx.scissor(4,5,20,21);setCompatEnableState(glCtx.SCISSOR_TEST,true);
        const inner=snapshotAttribState(SCISSOR);glCtx.scissor(1,1,1,1);restoreAttribState(inner);same(box(),[4,5,20,21],'inner');
        restoreAttribState(outer);same(box(),[0,0,32,32],'outer');same(getCompatEnableState(glCtx.SCISSOR_TEST),false,'outer enable');
      });
      // Swap/presentation is a whole-frame copy, not another clipped game draw.
      // Preserve the game's enable/box after copying, including zero-area clips.
      for(const clip of [[0,0,0,0],[0,0,1,1],[8,8,16,16],[-5,-5,64,64]])for(const cache of [false,true]){
        check('full-frame-present-'+clip.join('-')+'-cache-'+cache,()=>{
          reset();window.__LWJGL_EXTENDED_ATTRIB_CACHE__=cache;draw(.25,.5,.75);const expected=pixels(mainFb);
          glCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER,null);glCtx.clearColor(0,0,0,1);glCtx.clear(glCtx.COLOR_BUFFER_BIT);
          glCtx.bindFramebuffer(glCtx.FRAMEBUFFER,mainFb);glCtx.scissor(...clip);setCompatEnableState(glCtx.SCISSOR_TEST,true);
          Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers();
          same(pixels(null),expected,'presentation pixel parity');same(box(),clip,'preserve clip box');same(getCompatEnableState(glCtx.SCISSOR_TEST),true,'preserve clip enable');
          glCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER,mainFb);
        });
      }
      check('invalid-scissor-does-not-replace-authoritative-box',()=>{
        reset();const saved=snapshotAttribState(SCISSOR);
        glCtx.scissor(1,2,-1,0);same(glCtx.getError(),glCtx.INVALID_VALUE,'invalid scissor');
        restoreAttribState(saved);same(box(),[0,0,32,32],'box after rejected write');
      });
      check('presentation-disabled-scissor-control',()=>{
        reset();draw(1,1,0);const expected=pixels(mainFb);
        Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers();same(pixels(null),expected,'unclipped control');
        same(getCompatEnableState(glCtx.SCISSOR_TEST),false,'disabled remains disabled');
      });
      check('presentation-error-restores-framebuffer-and-scissor',()=>{
        reset();setCompatEnableState(glCtx.SCISSOR_TEST,true);glCtx.scissor(2,3,4,5);
        const original=glCtx.blitFramebuffer;let rejected=false;
        glCtx.blitFramebuffer=()=>{throw new Error('owned fixture failure');};
        try{Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers();}
        catch(error){rejected=error.message==='owned fixture failure';}
        finally{glCtx.blitFramebuffer=original;}
        same(rejected,true,'test reached blit');same(getCompatEnableState(glCtx.SCISSOR_TEST),true,'enabled after throw');
        same(box(),[2,3,4,5],'clip after throw');
        same(glCtx.getParameter(glCtx.DRAW_FRAMEBUFFER_BINDING)===mainFb,true,'draw framebuffer restored');
        same(glCtx.getParameter(glCtx.READ_FRAMEBUFFER_BINDING)===mainFb,true,'read framebuffer restored');
      });
      check('unexpected-gl-errors',()=>same(glCtx.getError(),glCtx.NO_ERROR,'GL error'));
      return {checks,assertions,passed:checks.filter(c=>c.passed).length,failed:checks.filter(c=>!c.passed).length};
    `)(code),code);
  } finally {await browser.close();}
  const out=process.env.STARSECTOR_CLIPPING_OUTPUT || 'test_output/clipping-state/result.json';
  fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,JSON.stringify({sourcePath,...result},null,2));
  for(const c of result.checks)if(!c.passed)console.error('FAIL '+c.name+': '+c.error.slice(0,350));
  console.log('Clipping state: '+result.passed+' passed, '+result.failed+' failed; '+result.assertions+' assertions');
  process.exitCode=result.failed?1:0;
})().catch(error=>{console.error(error);process.exitCode=1;});
