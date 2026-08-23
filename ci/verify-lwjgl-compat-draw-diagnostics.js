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
  'compatNormalizeTextureComponent',
  'compatSummarizeTextureUpload',
  'compatSnapshotTextureDebugInfo',
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
  TEXTURE_BINDING_2D: 0x8069,
  UNSIGNED_BYTE: 0x1401,
  BYTE: 0x1400,
  UNSIGNED_SHORT: 0x1403,
  SHORT: 0x1402,
  UNSIGNED_INT: 0x1405,
  FLOAT: 0x1406,
  UNSIGNED_SHORT_5_6_5: 0x8363,
  UNSIGNED_SHORT_4_4_4_4: 0x8033,
  UNSIGNED_SHORT_5_5_5_1: 0x8034,
  isEnabled: () => true,
  getParameter(name) {
    if (name === this.VIEWPORT) return [0, 0, 1024, 768];
    if (name === this.BLEND_SRC_RGB) return 0x0302;
    if (name === this.BLEND_DST_RGB) return 0x0303;
    if (name === this.TEXTURE_BINDING_2D) return textureObject;
    return null;
  },
};
const textureObject = { id: 7 };
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
  textureMatrixStack: [identity],
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
    texCoordBuf: new Float32Array(0),
  },
  texture2DEnabled: false,
  boundTexture2DId: 7,
  textureObjects: [null, null, null, null, null, null, null, textureObject],
  textureStorageUploadFormat: [null, null, null, null, null, null, null, 0x1908],
  textureDebugInfo: [null, null, null, null, null, null, null, {
    id: 7,
    fullUploadCount: 1,
    subUploadCount: 0,
    copyUploadCount: 0,
    lastFullUpload: { width: 2, height: 2, nearWhiteRatio: 0 },
  }],
  getTexelComponentCount: () => 4,
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
expect(records[0].actualBoundTextureMatches === true, 'actual WebGL binding did not match logical texture');
expect(records[0].textureDebugInfo && records[0].textureDebugInfo.lastFullUpload.width === 2,
  'texture upload metadata was not captured');
expect(records[0].firstColor.join(',') === '1,1,1,1', 'first vertex color was not captured');
expect(records[0].texCoordBounds.minS === 0 && records[0].texCoordBounds.maxS === 1
  && records[0].texCoordBounds.minT === 0 && records[0].texCoordBounds.maxT === 1,
  'texture-coordinate range was not captured');
expect(records[0].textureMatrix.join(',') === identity.join(','), 'texture matrix was not captured');

console.log('verify-lwjgl-compat-draw-diagnostics: OK full-screen draw state, UVs, binding, and upload metadata captured');
