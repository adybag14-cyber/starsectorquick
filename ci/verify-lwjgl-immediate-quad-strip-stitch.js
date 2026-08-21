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

function expectFunctionContains(source, name, needle) {
  const body = extractFunction(source, name);
  expect(body.includes(needle), `${name} missing ${needle}`);
}

const path = process.argv[2];
if (!path) throw new Error('usage: node ci/verify-lwjgl-immediate-quad-strip-stitch.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
expect(source.includes('WEBGL_IMMEDIATE_QUAD_STRIP_STITCH_V1'), 'quad-strip stitch marker missing');
expect(source.includes('__LWJGL_IMMEDIATE_QUAD_STRIP_BATCH__ !== false'), 'runtime disable switch missing');

// Execute the production bridge builder itself. Each test vertex is encoded as
// nine copies of its integer id, so boundary duplication is easy to inspect.
const ensureArray = extractFunction(source, 'ensureImmediateArrayCapacity');
const ensureBatch = extractFunction(source, 'ensureImmediateQuadStripBatchCapacity');
const appendBatch = extractFunction(source, 'appendImmediateQuadStripBatch');
const batchContext = vm.createContext({
  Float32Array,
  immediateQuadStripBatch: {
    data: new Float32Array(128),
    floatPos: 4 * 9,
    vertexCount: 4,
    stripCount: 1,
  },
  immediateModeData: { interleavedBuf: new Float32Array(4 * 9) },
});
for (let vertex = 0; vertex < 4; vertex++) {
  for (let k = 0; k < 9; k++) batchContext.immediateQuadStripBatch.data[vertex * 9 + k] = vertex;
}
for (let vertex = 0; vertex < 4; vertex++) {
  for (let k = 0; k < 9; k++) batchContext.immediateModeData.interleavedBuf[vertex * 9 + k] = 10 + vertex;
}
vm.runInContext(`${ensureArray}\n${ensureBatch}\n${appendBatch}`, batchContext);
batchContext.appendImmediateQuadStripBatch(4);
expect(batchContext.immediateQuadStripBatch.vertexCount === 10,
  `two four-vertex strips should stitch to 10 vertices, got ${batchContext.immediateQuadStripBatch.vertexCount}`);
expect(batchContext.immediateQuadStripBatch.floatPos === 90,
  `stitched float count should be 90, got ${batchContext.immediateQuadStripBatch.floatPos}`);
expect(batchContext.immediateQuadStripBatch.stripCount === 2, 'strip count did not advance');
const ids = [];
for (let vertex = 0; vertex < 10; vertex++) ids.push(batchContext.immediateQuadStripBatch.data[vertex * 9]);
expect(JSON.stringify(ids) === JSON.stringify([0, 1, 2, 3, 3, 10, 10, 11, 12, 13]),
  `unexpected stitched vertex ids: ${ids.join(',')}`);

// Verify every connector triangle is degenerate and the first non-degenerate B
// triangle starts with B0/B1/B2 at an even strip index, preserving winding.
function triangleAt(vertices, i) {
  if ((i & 1) === 0) return [vertices[i], vertices[i + 1], vertices[i + 2]];
  return [vertices[i + 1], vertices[i], vertices[i + 2]];
}
function degenerate(t) { return t[0] === t[1] || t[1] === t[2] || t[0] === t[2]; }
for (let i = 2; i <= 5; i++) expect(degenerate(triangleAt(ids, i)), `bridge triangle ${i} is not degenerate`);
expect(JSON.stringify(triangleAt(ids, 6)) === JSON.stringify([10, 11, 12]),
  `second strip winding/parity changed: ${triangleAt(ids, 6).join(',')}`);

// Execute the production flush function with a two-strip pending batch and
// prove that logical draw accounting remains two while WebGL submits one draw.
const flushFn = extractFunction(source, 'flushImmediateQuadStripBatch');
const calls = [];
const stats = {
  legacyDrawCalls: 0,
  webglDrawCalls: 0,
  immediateQuadStripDeferredRuns: 0,
  immediateQuadStripBatchedRuns: 0,
  immediateQuadStripBatchedStrips: 0,
  immediateQuadStripDrawCallsSaved: 0,
  immediateQuadStripBridgeVertices: 0,
  immediateQuadStripUploadsSaved: 0,
};
const flushContext = vm.createContext({
  immediateQuadStripBatch: {
    active: true,
    data: new Float32Array(90),
    floatPos: 90,
    vertexCount: 10,
    stripCount: 2,
    modelView: new Float32Array(16),
    projection: new Float32Array(16),
    renderSignature: 'x',
  },
  presentationStats: stats,
  glCtx: {
    TRIANGLE_STRIP: 5,
    uniformMatrix4fv(location, transpose, value) { calls.push(['uniform', location, transpose, value.length]); },
    drawArrays(mode, first, count) { calls.push(['drawArrays', mode, first, count]); },
  },
  mvLocation: 'mv',
  projLocation: 'proj',
  uploadImmediateInterleavedData(data, vertexCount, logicalDraws) {
    calls.push(['upload', data.length, vertexCount, logicalDraws]);
  },
  applyImmediateQuadStripDeferredState() { calls.push(['applyDeferred']); },
});
vm.runInContext(flushFn, flushContext);
flushContext.flushImmediateQuadStripBatch();
expect(calls.filter(c => c[0] === 'drawArrays').length === 1, 'two strips must submit exactly one WebGL draw');
expect(calls.some(c => c[0] === 'drawArrays' && c[1] === 5 && c[2] === 0 && c[3] === 10), 'stitched TRIANGLE_STRIP draw missing');
expect(calls.some(c => c[0] === 'upload' && c[1] === 90 && c[2] === 10 && c[3] === 2), 'combined upload missing');
expect(stats.legacyDrawCalls === 2, `logical draw count changed: ${stats.legacyDrawCalls}`);
expect(stats.webglDrawCalls === 1, `WebGL draw count should be 1, got ${stats.webglDrawCalls}`);
expect(stats.immediateQuadStripDrawCallsSaved === 1, 'draw-call saving telemetry incorrect');
expect(stats.immediateQuadStripBridgeVertices === 2, 'bridge vertex telemetry incorrect');
expect(stats.immediateQuadStripUploadsSaved === 1, 'upload saving telemetry incorrect');
expect(calls[calls.length - 1][0] === 'applyDeferred', 'deferred physical state must be applied after pending draw');

// Hard/observable operations must flush before they can reorder the pending draw.
for (const name of [
  'Java_org_lwjgl_opengl_GL11_nglClear',
  'Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers',
  'Java_org_lwjgl_opengl_GL11_nglDrawArrays',
  'Java_org_lwjgl_opengl_GL11_nglTexImage2D',
  'Java_org_lwjgl_opengl_GL11_nglTexSubImage2D',
  'Java_org_lwjgl_opengl_GL11_nglCopyTexImage2D',
  'Java_org_lwjgl_opengl_GL11_nglCopyTexSubImage2D',
  'Java_org_lwjgl_opengl_GL11_nglCallList',
  'Java_org_lwjgl_opengl_GL11_nglCallLists',
  'Java_org_lwjgl_opengl_GL11_nglPushAttrib',
  'Java_org_lwjgl_opengl_GL11_nglPopAttrib',
  'Java_org_lwjgl_opengl_GL11_nglFlush',
]) expectFunctionContains(source, name, 'flushImmediateQuadStripBatch()');

// Reversible state churn is intentionally deferred rather than becoming a
// mandatory barrier; matching final state can therefore stitch the next strip.
for (const [name, needle] of [
  ['Java_org_lwjgl_opengl_GL11_nglBindTexture', 'deferImmediateQuadStripState("textureBind")'],
  ['Java_org_lwjgl_opengl_GL11_nglViewport', 'deferImmediateQuadStripState("viewport")'],
  ['Java_org_lwjgl_opengl_GL11_nglDepthFunc', 'deferImmediateQuadStripState("depthFunc")'],
  ['Java_org_lwjgl_opengl_GL11_nglCullFace', 'deferImmediateQuadStripState("cullFace")'],
  ['Java_org_lwjgl_opengl_GL11_nglDepthMask', 'deferImmediateQuadStripState("depthMask")'],
  ['Java_org_lwjgl_opengl_GL11_nglBlendFunc', 'deferImmediateQuadStripState("blendFunc")'],
  ['Java_org_lwjgl_opengl_GL11_nglColorMask', 'deferImmediateQuadStripState("colorMask")'],
  ['Java_org_lwjgl_opengl_GL11_nglScissor', 'deferImmediateQuadStripState("scissor")'],
  ['Java_org_lwjgl_opengl_GL11_nglStencilFunc', 'deferImmediateQuadStripState("stencilFunc")'],
  ['Java_org_lwjgl_opengl_GL11_nglStencilOp', 'deferImmediateQuadStripState("stencilOp")'],
]) expectFunctionContains(source, name, needle);

const endBody = extractFunction(source, 'Java_org_lwjgl_opengl_GL11_nglEnd');
expect(endBody.includes('immediateModeData.mode == 8/*QUAD_STRIP*/'), 'nglEnd quad-strip gate missing');
expect(endBody.includes('immediateQuadStripStateMatchesPending()'), 'nglEnd final-state match missing');
expect(endBody.includes('appendImmediateQuadStripBatch(vertexCount)'), 'nglEnd stitch path missing');
expect(endBody.includes('flushImmediateQuadStripBatch()'), 'nglEnd fallback flush missing');

console.log(`verify-lwjgl-immediate-quad-strip-stitch: OK ids=${ids.join(',')} saved=${stats.immediateQuadStripDrawCallsSaved}`);
