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
if (!path) throw new Error('usage: node ci/verify-lwjgl-quad-batching.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const ensureQuad = extractFunction(source, 'ensureQuadIndexCapacity');
const drawArrays = extractFunction(source, 'drawArraysImpl');

const calls = [];
let uploadedIndices = null;
const glCtx = {
  ELEMENT_ARRAY_BUFFER: 0x8893,
  STATIC_DRAW: 0x88E4,
  TRIANGLES: 0x0004,
  TRIANGLE_STRIP: 0x0005,
  TRIANGLE_FAN: 0x0006,
  UNSIGNED_INT: 0x1405,
  POINTS: 0x0000,
  LINES: 0x0001,
  LINE_LOOP: 0x0002,
  LINE_STRIP: 0x0003,
  uniformMatrix4fv() {},
  bindBuffer(target, buffer) { calls.push(['bindBuffer', target, buffer]); },
  bufferData(target, data, usage) {
    uploadedIndices = Array.from(data);
    calls.push(['bufferData', target, data.length, usage]);
  },
  drawElements(mode, count, type, offset) { calls.push(['drawElements', mode, count, type, offset]); },
  drawArrays(mode, first, count) { calls.push(['drawArrays', mode, first, count]); },
};

const context = vm.createContext({
  detailedDrawTelemetryEnabled: true,
  projectionUniformDirty: true,
  glCtx,
  quadIndexBuffer: { kind: 'quad-index-buffer' },
  quadIndexVertexCapacity: 0,
  presentationStats: {
    legacyDrawCalls: 0,
    webglDrawCalls: 0,
    quadBatches: 0,
    quadQuads: 0,
    quadDrawCallsSaved: 0,
    quadIndexBufferUploads: 0,
  },
  mvLocation: {},
  projLocation: {},
  modelViewMatrixStack: [[]],
  projMatrixStack: [[]],
  unsupportedDrawModes: new Set(),
  warnOnce() {},
  assert(value) { if (!value) throw new Error('assertion failed'); },
  console,
  Math,
  Uint32Array,
});
vm.runInContext(`${ensureQuad}\n${drawArrays}`, context);

context.drawArraysImpl(7, 0, 8);
let draws = calls.filter(call => call[0] === 'drawElements');
expect(draws.length === 1, `two quads should use one indexed draw, got ${draws.length}`);
expect(draws[0][2] === 12, `two quads should emit 12 indices, got ${draws[0][2]}`);
const expected = [0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7];
expect(uploadedIndices && expected.every((value, index) => uploadedIndices[index] === value),
  `unexpected quad index prefix: ${uploadedIndices && uploadedIndices.slice(0, 12)}`);
expect(context.presentationStats.quadBatches === 1, 'quad batch telemetry missing');
expect(context.presentationStats.quadQuads === 2, 'quad count telemetry missing');
expect(context.presentationStats.quadDrawCallsSaved === 1, 'two quads should save one draw call');
expect(context.presentationStats.webglDrawCalls === 1, 'two quads should count as one WebGL draw');
expect(context.presentationStats.quadIndexBufferUploads === 1, 'first batch should upload one cached index buffer');

const uploadsAfterFirst = calls.filter(call => call[0] === 'bufferData').length;
context.drawArraysImpl(7, 0, 12);
expect(calls.filter(call => call[0] === 'drawElements').length === 2, 'three quads should add one indexed draw');
expect(calls.filter(call => call[0] === 'bufferData').length === uploadsAfterFirst,
  'smaller follow-up batch should reuse geometric index capacity');
expect(context.presentationStats.quadDrawCallsSaved === 3,
  `expected cumulative three saved calls, got ${context.presentationStats.quadDrawCallsSaved}`);

const indexedBeforeSingle = calls.filter(call => call[0] === 'drawElements').length;
context.drawArraysImpl(7, 0, 4);
expect(calls.filter(call => call[0] === 'drawElements').length === indexedBeforeSingle,
  'single quad should not pay indexed-draw overhead');
expect(calls.some(call => call[0] === 'drawArrays' && call[1] === glCtx.TRIANGLE_FAN && call[2] === 0 && call[3] === 4),
  'single quad direct TRIANGLE_FAN path missing');

context.drawArraysImpl(glCtx.TRIANGLES, 0, 6);
expect(calls.some(call => call[0] === 'drawArrays' && call[1] === glCtx.TRIANGLES && call[3] === 6),
  'non-quad drawArrays behavior changed');

let nonzeroRejected = false;
try { context.drawArraysImpl(7, 4, 4); } catch (_) { nonzeroRejected = true; }
expect(nonzeroRejected, 'nonzero first must retain the existing client-array assertion');

console.log(`verify-lwjgl-quad-batching: OK batches=${context.presentationStats.quadBatches} quads=${context.presentationStats.quadQuads} saved=${context.presentationStats.quadDrawCallsSaved}`);
