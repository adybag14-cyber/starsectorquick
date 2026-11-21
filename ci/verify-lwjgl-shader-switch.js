'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { chromium } = require('playwright');

const renderer = fs.readFileSync('build/final/wasm-modules/lwjgl.js', 'utf8');
function section(first, next) {
  const begin = renderer.indexOf(first);
  const end = renderer.indexOf(next, begin);
  assert.ok(begin >= 0 && end > begin);
  return renderer.slice(begin, end);
}
const production = [
  section('var vertexShaderSrc =', '// LWJGL_VERTEX_ARRAY_COALESCING_V1_BEGIN'),
  section('function drawUniforms(', 'function getCompatEnableState('),
  section('function drawArraysImpl(', 'function pushDrawArraysInList(')
].join('\n');

async function main() {
  const browser = await chromium.launch({args: ['--enable-unsafe-swiftshader']});
  try {
    const page = await browser.newPage();
    const cases = await page.evaluate(production => {
      const fixture = new Function('glCtx', 'reference', production + `
        if (alphaDraw === plainDraw) throw Error('fast shader did not link');
        if (reference) plainDraw = alphaDraw;
        const assert = value => { if (!value) throw Error('draw invariant'); };
        const identity = () => new Float32Array([1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]);
        const modelViewMatrixStack=[identity()], projMatrixStack=[identity()], textureMatrixStack=[identity()];
        let modelViewMatrixUniformDirty=true, projMatrixUniformDirty=true, textureMatrixUniformDirty=true;
        const detailedDrawTelemetryEnabled=false;
        const positions = new Float32Array([-.5,-.3, .45,-.2, -.1,.45]);
        glCtx.bindBuffer(glCtx.ARRAY_BUFFER,vertexBuffer);
        glCtx.bufferData(glCtx.ARRAY_BUFFER,positions,glCtx.STATIC_DRAW);
        glCtx.enableVertexAttribArray(vertexPosition);
        glCtx.vertexAttribPointer(vertexPosition,2,glCtx.FLOAT,false,0,0);
        glCtx.vertexAttrib2f(texCoord,.375,.625);
        glCtx.vertexAttrib4f(colorLocation,.75,.5,.875,.625);
        glCtx.bindTexture(glCtx.TEXTURE_2D,glCtx.createTexture());
        glCtx.texImage2D(glCtx.TEXTURE_2D,0,glCtx.RGBA,2,2,0,glCtx.RGBA,glCtx.UNSIGNED_BYTE,
          new Uint8Array([255,30,10,0,20,255,60,40,60,25,255,180,120,70,80,255]));
        glCtx.texParameteri(glCtx.TEXTURE_2D,glCtx.TEXTURE_MIN_FILTER,glCtx.LINEAR);
        const images=[];
        for(let func=512;func<=519;func++) for(const textured of [false,true]) {
          setTexture2DEnabled(textured);
          // Change state in the currently bound program. The next few draws
          // switch programs without touching the same matrices or point size.
          pointSizeState = textured ? 5 : 9;
          glCtx.uniform1f(pointSizeLocation,pointSizeState);
          modelViewMatrixStack[0][12] = (func-515)/14;
          projMatrixStack[0][5] = textured ? .8 : 1;
          textureMatrixStack[0][12] = textured ? .125 : 0;
          modelViewMatrixUniformDirty=projMatrixUniformDirty=textureMatrixUniformDirty=true;
          for(const enabled of [false,true,false]) {
            alphaTestState.enabled=enabled; alphaTestState.func=func; alphaTestState.ref=.375;
            syncAlphaTestUniforms();
            glCtx.clearColor(.1,.2,.3,1); glCtx.clear(glCtx.COLOR_BUFFER_BIT);
            drawArraysImpl(glCtx.TRIANGLES,0,3);
            drawArraysImpl(glCtx.POINTS,0,3);
            if(!reference && activeDraw !== (enabled && func!==519 ? alphaDraw : plainDraw))
              throw Error('incorrect shader selected');
            const pixels=new Uint8Array(32*32*4);
            glCtx.readPixels(0,0,32,32,glCtx.RGBA,glCtx.UNSIGNED_BYTE,pixels);
            images.push(pixels);
          }
        }
        if(glCtx.getError()!==glCtx.NO_ERROR) throw Error('WebGL error');
        return images;`);
      function capture(reference) {
        const canvas=document.createElement('canvas');
        canvas.width=canvas.height=32;
        return fixture(canvas.getContext('webgl2',{antialias:false}),reference);
      }
      const baseline=capture(true), candidate=capture(false);
      for(let i=0;i<baseline.length;i++) {
        if(baseline[i].some((value,index)=>value!==candidate[i][index])) throw Error('pixel mismatch at draw '+i);
      }
      if(!candidate[0].some((value,index)=>value!==candidate[1][index])) throw Error('GL_NEVER did not reject fragments');
      return candidate.length;
    }, production);
    assert.equal(cases,48);
    console.log('Shader switching matched the general program in all 48 framebuffer comparisons.');
  } finally {
    await browser.close();
  }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
