'use strict';

const fs = require('fs');
const vm = require('vm');

function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}(`);
  if (start < 0) throw new Error(`missing function ${name}`);
  const brace = source.indexOf('{', start);
  let depth = 0;
  let quote = null;
  let escaped = false;
  for (let i = brace; i < source.length; i++) {
    const ch = source[i];
    if (quote) {
      if (escaped) escaped = false;
      else if (ch === '\\') escaped = true;
      else if (ch === quote) quote = null;
      continue;
    }
    if (ch === '"' || ch === "'" || ch === '`') {
      quote = ch;
      continue;
    }
    if (ch === '{') depth++;
    else if (ch === '}' && --depth === 0) return source.slice(start, i + 1);
  }
  throw new Error(`unterminated function ${name}`);
}

function expect(condition, message) {
  if (!condition) throw new Error(message);
}

const path = process.argv[2];
if (!path) throw new Error('usage: node ci/verify-lwjgl-compat-draw-diagnostics.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const code = [
  'compatProjectedBounds',
  'compatRecordLargeDraw',
  'compatRecordImmediateDraw',
].map(name => extractFunction(source, name)).join('\n');

const identity = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1];
const glCtx = {
  BLEND: 0x0BE2,
  BLEND_SRC_RGB: 0x80C9,
  BLEND_DST_RGB: 0x80C8,
  VIEWPORT: 0x0BA2,
  isEnabled: () => true,
  getParameter(name) {
    if (name === this.VIEWPORT) return [0, 0, 1024, 768];
    if (name === this.BLEND_SRC_RGB) return 0x0302;
    if (name === this.BLEND_DST_RGB) return 0x0303;
    return null;
  },
};
const context = vm.createContext({
  compatDrawDiagnosticsEnabled: true,
  compatDrawDiagnostics: { enabled: true, frames: [], currentLargeDraws: [], currentClears: [] },
  glCtx,
  glMatrix: {
    vec4: {
      create: () => new Float32Array(4),
      transformMat4(out, value, matrix) {
        for (let row = 0; row < 4; row++) {
          out[row] = value[0] * matrix[row]
            + value[1] * matrix[row + 4]
            + value[2] * matrix[row + 8]
            + value[3] * matrix[row + 12];
        }
        return out;
      },
    },
  },
  modelViewMatrixStack: [identity],
  projMatrixStack: [identity],
  immediateInterleavedEnabled: true,
  immediateModeData: {
    currentColor: [1, 1, 1, 1],
    interleavedBuf: new Float32Array([
      -1, -1, 0, 1, 1, 1, 1, 0, 0,
       1, -1, 0, 1, 1, 1, 1, 1, 0,
       1,  1, 0, 1, 1, 1, 1, 1, 1,
      -1,  1, 0, 1, 1, 1, 1, 0, 1,
    ]),
    vertexBuf: new Float32Array(0),
    colorBuf: new Float32Array(0),
  },
  texture2DEnabled: false,
  boundTexture2DId: 7,
  textureObjects: [null, null, null, null, null, null, null, { id: 7 }],
  textureStorageUploadFormat: [null, null, null, null, null, null, null, 0x1908],
  alphaTestState: { enabled: false },
  Float32Array,
  Array,
  Number,
  Math,
  Infinity,
});
vm.runInContext(code, context);
context.compatRecordImmediateDraw(7, 4);

const records = context.compatDrawDiagnostics.currentLargeDraws;
expect(records.length === 1, `expected one screen-covering draw, got ${records.length}`);
expect(Math.abs(records[0].bounds.coverage - 1) < 1e-6,
  `expected full-screen coverage, got ${records[0].bounds.coverage}`);
expect(records[0].texture2DEnabled === false, 'texture enable state was not captured');
expect(records[0].boundTexture2DId === 7 && records[0].boundTextureExists === true,
  'bound texture identity was not captured');
expect(records[0].firstColor.join(',') === '1,1,1,1', 'first vertex color was not captured');

console.log('verify-lwjgl-compat-draw-diagnostics: OK full-screen untextured white quad captured');
