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
if (!path) throw new Error('usage: node ci/verify-lwjgl-texture-subimage.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const names = [
  'getTexelComponentCount',
  'buildTextureUploadArray',
  'isLegacyWebGl1TextureFormat',
  'expandLegacyTextureDataToRgba',
  'expandRgbTextureDataToRgba',
  'normalizeTextureUpload',
  'Java_org_lwjgl_opengl_GL11_nglTexImage2D',
  'Java_org_lwjgl_opengl_GL11_nglTexSubImage2D',
];
const code = names.map(name => extractFunction(source, name)).join('\n');

const calls = [];
const glCtx = {
  RED: 0x1903,
  RG: 0x8227,
  RGB: 0x1907,
  RGBA: 0x1908,
  TEXTURE_2D: 0x0DE1,
  UNSIGNED_BYTE: 0x1401,
  UNSIGNED_SHORT: 0x1403,
  SHORT: 0x1402,
  UNSIGNED_SHORT_5_6_5: 0x8363,
  UNSIGNED_SHORT_4_4_4_4: 0x8033,
  UNSIGNED_SHORT_5_5_5_1: 0x8034,
  FLOAT: 0x1406,
  UNSIGNED_INT: 0x1405,
  NO_ERROR: 0,
  texImage2D(...args) { calls.push(['image', ...args]); },
  texSubImage2D(...args) { calls.push(['sub', ...args]); },
  generateMipmap(...args) { calls.push(['mipmap', ...args]); },
  getError() { return 0; },
};
const bytes = new Uint8Array([0, 10, 20, 30, 40, 50, 60]);
const lib = { getJNIDataView: () => new DataView(bytes.buffer) };
const context = vm.createContext({
  glCtx,
  boundTexture2DId: 1,
  textureGenerateMipmap: [false, false, false],
  textureStorageUploadFormat: [null, null, null],
  compatDrawDiagnosticsEnabled: false,
  strictWebGLValidation: false,
  texImageWarnings: new Set(),
  warnOnce() {},
  checkNoList() {},
  curList: null,
  assert(value) { if (!value) throw new Error('assertion failed'); },
  Uint8Array,
  Uint16Array,
  Uint32Array,
  Float32Array,
  DataView,
  Math,
  Number,
  String,
});
vm.runInContext(code, context);

// Desktop GL permits RGB source pixels for RGBA storage. The bridge expands the
// base image to RGBA for WebGL2, and every later RGB sub-image must use that same
// tracked storage format instead of sending an invalid three-component update.
context.Java_org_lwjgl_opengl_GL11_nglTexImage2D(
  lib, glCtx.TEXTURE_2D, 0, glCtx.RGBA, 1, 1, 0, glCtx.RGB, glCtx.UNSIGNED_BYTE, 1, 0,
);
expect(context.textureStorageUploadFormat[1] === glCtx.RGBA,
  `base storage format was not normalized to RGBA: ${context.textureStorageUploadFormat[1]}`);

context.Java_org_lwjgl_opengl_GL11_nglTexSubImage2D(
  lib, glCtx.TEXTURE_2D, 0, 0, 0, 2, 1, glCtx.RGB, glCtx.UNSIGNED_BYTE, 1, 0,
);
const sub = calls.find(call => call[0] === 'sub');
expect(sub, 'texSubImage2D was not called');
expect(sub[7] === glCtx.RGBA, `sub-image format must be RGBA, got 0x${Number(sub[7]).toString(16)}`);
expect(sub[9] instanceof Uint8Array, 'sub-image data must remain an unsigned-byte upload');
expect(sub[9].length === 8, `two RGB pixels must expand to eight RGBA bytes, got ${sub[9].length}`);
expect(Array.from(sub[9]).join(',') === '10,20,30,255,40,50,60,255',
  `unexpected expanded sub-image bytes: ${Array.from(sub[9])}`);

// A deferred texture handle can be refreshed before it has received WebGL base
// storage. A complete level-zero sub-image then has enough information to create
// the texture instead of emitting INVALID_OPERATION for an undefined level.
context.boundTexture2DId = 2;
const imagesBeforePromotion = calls.filter(call => call[0] === 'image').length;
const subsBeforePromotion = calls.filter(call => call[0] === 'sub').length;
context.Java_org_lwjgl_opengl_GL11_nglTexSubImage2D(
  lib, glCtx.TEXTURE_2D, 0, 0, 0, 1, 1, glCtx.RGB, glCtx.UNSIGNED_BYTE, 1, 0,
);
expect(calls.filter(call => call[0] === 'image').length === imagesBeforePromotion + 1,
  'undefined level-zero storage was not promoted to texImage2D');
expect(calls.filter(call => call[0] === 'sub').length === subsBeforePromotion,
  'undefined level-zero storage still issued texSubImage2D');
expect(context.textureStorageUploadFormat[2] === glCtx.RGB,
  `promoted storage format was not tracked: ${context.textureStorageUploadFormat[2]}`);

console.log('verify-lwjgl-texture-subimage: OK RGB storage compatibility and undefined level-zero promotion');
