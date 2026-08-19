'use strict';

const fs = require('fs');
const vm = require('vm');

function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}(`);
  if (start < 0) throw new Error(`missing function ${name}`);
  const brace = source.indexOf('{', start);
  if (brace < 0) throw new Error(`missing body for ${name}`);
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
    else if (ch === '}') {
      depth--;
      if (depth === 0) return source.slice(start, i + 1);
    }
  }
  throw new Error(`unterminated function ${name}`);
}

function expect(condition, message) {
  if (!condition) throw new Error(message);
}

const path = process.argv[2];
if (!path) throw new Error('usage: node ci/verify-lwjgl-matrix-uniform-cache.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const ensureStack = extractFunction(source, 'ensureCurMatrixStack');
const uploadMatrices = extractFunction(source, 'uploadCurrentMatrices');
const markDirty = extractFunction(source, 'markCurrentMatrixDirty');
const setTop = extractFunction(source, 'setCurMatrixTop');

const uploads = [];
const model = [{ name: 'model-0' }];
const projection = [{ name: 'projection-0' }];
const texture = [{ name: 'texture-0' }];
const presentationStats = { matrixUniformUploads: 0, matrixUniformUploadsSaved: 0 };
const context = vm.createContext({
  glCtx: { uniformMatrix4fv(loc, transpose, value) { uploads.push([loc, transpose, value.name]); } },
  Array,
  Error,
  Set,
  matrixStackWarnings: new Set(),
  warnOnce(set, key) { set.add(key); },
  glMatrix: { mat4: { create() { return { name: 'recovered-identity' }; } } },
  mvLocation: 'mv',
  projLocation: 'proj',
  modelViewMatrixStack: model,
  projMatrixStack: projection,
  textureMatrixStack: texture,
  curMatrixStack: model,
  matrixUniformCacheEnabled: true,
  modelViewMatrixGeneration: 1,
  projMatrixGeneration: 1,
  textureMatrixGeneration: 1,
  uploadedModelViewMatrixGeneration: 0,
  uploadedProjMatrixGeneration: 0,
  presentationStats,
});
vm.runInContext(`${ensureStack}\n${markDirty}\n${setTop}\n${uploadMatrices}`, context);

context.curMatrixStack = [];
const recovered = context.ensureCurMatrixStack();
expect(recovered.length === 1 && recovered[0].name === 'recovered-identity', 'matrix-stack guard recovery mismatch');
expect(context.matrixStackWarnings.has('matrix-stack-empty-recovery'), 'matrix-stack recovery warning missing');
context.curMatrixStack = model;

context.uploadCurrentMatrices();
expect(uploads.length === 2, `first draw must upload both matrices, got ${uploads.length}`);
context.uploadCurrentMatrices();
expect(uploads.length === 2, 'unchanged second draw should not upload matrices');
expect(presentationStats.matrixUniformUploadsSaved === 2, `expected two saved uploads, got ${presentationStats.matrixUniformUploadsSaved}`);

context.curMatrixStack = model;
context.setCurMatrixTop({ name: 'model-1' });
context.uploadCurrentMatrices();
expect(uploads.length === 3 && uploads[2][0] === 'mv' && uploads[2][2] === 'model-1',
  `model mutation should upload model only: ${JSON.stringify(uploads)}`);

context.curMatrixStack = projection;
context.setCurMatrixTop({ name: 'projection-1' });
context.uploadCurrentMatrices();
expect(uploads.length === 4 && uploads[3][0] === 'proj' && uploads[3][2] === 'projection-1',
  `projection mutation should upload projection only: ${JSON.stringify(uploads)}`);

context.curMatrixStack = texture;
context.setCurMatrixTop({ name: 'texture-1' });
context.uploadCurrentMatrices();
expect(uploads.length === 4, 'texture matrix changes must not dirty model/projection uniforms');

context.matrixUniformCacheEnabled = false;
context.uploadCurrentMatrices();
expect(uploads.length === 6, 'disabled cache must preserve legacy two-uniform-per-draw behavior');
console.log(`verify-lwjgl-matrix-uniform-cache: OK uploads=${presentationStats.matrixUniformUploads} saved=${presentationStats.matrixUniformUploadsSaved}`);
