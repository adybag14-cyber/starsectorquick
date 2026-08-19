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
if (!path) throw new Error('usage: node ci/verify-lwjgl-attrib-shadow.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const names = [
  'syncAlphaTestUniforms', 'setTexture2DEnabled', 'hasTrackedEnableState',
  'getCompatEnableState', 'setCompatEnableState', 'setBlendFuncSeparateShadow',
  'setBlendFuncShadow', 'setColorMaskShadow', 'setClearColorShadow',
  'setDepthMaskShadow', 'setDepthFuncShadow', 'setClearDepthShadow',
  'setViewportShadow', 'setDepthRangeShadow', 'setStencilFuncShadow',
  'setStencilMaskShadow', 'setStencilOpShadow', 'setClearStencilShadow',
  'cloneAttribArray', 'attribSnapshotQueryCount', 'noteAttribSnapshot',
  'snapshotAttribStateLegacy', 'snapshotAttribState', 'restoreAttribState',
];
const functions = names.map(name => extractFunction(source, name)).join('\n');
expect(source.includes('LWJGL_ATTRIB_STATE_SHADOW_V1'), 'shadow-state marker missing');
const snapshotSource = extractFunction(source, 'snapshotAttribState');
expect(!snapshotSource.includes('getParameter'), 'snapshot still performs synchronous getParameter');
expect(!snapshotSource.includes('isEnabled'), 'snapshot still performs synchronous isEnabled');
for (const token of [
  'setClearColorShadow(r, g, b, a)',
  'setViewportShadow(x, y, width, height)',
  'setCompatEnableState(a, false)',
  'setCompatEnableState(a, true)',
  'setClearDepthShadow(a)',
  'setDepthFuncShadow(a)',
  'setDepthMaskShadow(a)',
  'setBlendFuncShadow(sfactor, dfactor)',
  'setColorMaskShadow(r, g, b, a)',
  'setStencilFuncShadow(func, ref, mask)',
  'setStencilOpShadow(sfail, dpfail, dppass)',
]) expect(source.includes(token), `state mutator is not shadowed: ${token}`);

const calls = [];
let syncQueryCalls = 0;
const glCtx = {
  BLEND: 0x0BE2, CULL_FACE: 0x0B44, DEPTH_TEST: 0x0B71,
  SCISSOR_TEST: 0x0C11, STENCIL_TEST: 0x0B90, TEXTURE_2D: 0x0DE1,
  ONE: 1, ZERO: 0, LESS: 0x0201, ALWAYS: 0x0207, KEEP: 0x1E00,
  BLEND_SRC_RGB: 100, BLEND_DST_RGB: 101, BLEND_SRC_ALPHA: 102, BLEND_DST_ALPHA: 103,
  COLOR_WRITEMASK: 104, COLOR_CLEAR_VALUE: 105, DEPTH_WRITEMASK: 106, DEPTH_FUNC: 107,
  DEPTH_CLEAR_VALUE: 108, VIEWPORT: 109, DEPTH_RANGE: 110, STENCIL_FUNC: 111, STENCIL_REF: 112,
  STENCIL_VALUE_MASK: 113, STENCIL_WRITEMASK: 114, STENCIL_FAIL: 115, STENCIL_PASS_DEPTH_FAIL: 116,
  STENCIL_PASS_DEPTH_PASS: 117, STENCIL_CLEAR_VALUE: 118,
  LEQUAL: 0x0203, NOTEQUAL: 0x0205, REPLACE: 0x1E01, INCR: 0x1E02, DECR: 0x1E03,
  drawingBufferWidth: 1280, drawingBufferHeight: 720,
  uniform1f(...args) { calls.push(['uniform1f', ...args]); },
  enable(cap) { calls.push(['enable', cap]); },
  disable(cap) { calls.push(['disable', cap]); },
  isEnabled(cap) { syncQueryCalls++; return Boolean(attribEnableShadow[cap]); },
  getParameter(pname) {
    syncQueryCalls++;
    const color = attribStateShadow.color;
    const depth = attribStateShadow.depth;
    const viewport = attribStateShadow.viewport;
    const stencil = attribStateShadow.stencil;
    const values = {
      [this.BLEND_SRC_RGB]: color.blendSrcRgb, [this.BLEND_DST_RGB]: color.blendDstRgb,
      [this.BLEND_SRC_ALPHA]: color.blendSrcAlpha, [this.BLEND_DST_ALPHA]: color.blendDstAlpha,
      [this.COLOR_WRITEMASK]: color.colorMask, [this.COLOR_CLEAR_VALUE]: color.clearColor,
      [this.DEPTH_WRITEMASK]: depth.writeMask, [this.DEPTH_FUNC]: depth.func,
      [this.DEPTH_CLEAR_VALUE]: depth.clearValue, [this.VIEWPORT]: viewport.box,
      [this.DEPTH_RANGE]: viewport.depthRange, [this.STENCIL_FUNC]: stencil.func,
      [this.STENCIL_REF]: stencil.ref, [this.STENCIL_VALUE_MASK]: stencil.valueMask,
      [this.STENCIL_WRITEMASK]: stencil.writeMask, [this.STENCIL_FAIL]: stencil.fail,
      [this.STENCIL_PASS_DEPTH_FAIL]: stencil.depthFail, [this.STENCIL_PASS_DEPTH_PASS]: stencil.depthPass,
      [this.STENCIL_CLEAR_VALUE]: stencil.clearValue,
    };
    if (!(pname in values)) throw new Error(`unexpected getParameter ${pname}`);
    return values[pname];
  },
  blendFunc(...args) { calls.push(['blendFunc', ...args]); },
  blendFuncSeparate(...args) { calls.push(['blendFuncSeparate', ...args]); },
  colorMask(...args) { calls.push(['colorMask', ...args]); },
  clearColor(...args) { calls.push(['clearColor', ...args]); },
  depthMask(...args) { calls.push(['depthMask', ...args]); },
  depthFunc(...args) { calls.push(['depthFunc', ...args]); },
  clearDepth(...args) { calls.push(['clearDepth', ...args]); },
  viewport(...args) { calls.push(['viewport', ...args]); },
  depthRange(...args) { calls.push(['depthRange', ...args]); },
  stencilFunc(...args) { calls.push(['stencilFunc', ...args]); },
  stencilMask(...args) { calls.push(['stencilMask', ...args]); },
  stencilOp(...args) { calls.push(['stencilOp', ...args]); },
  clearStencil(...args) { calls.push(['clearStencil', ...args]); },
};
const attribEnableShadow = Object.create(null);
for (const cap of [glCtx.BLEND, glCtx.CULL_FACE, glCtx.DEPTH_TEST, glCtx.SCISSOR_TEST, glCtx.STENCIL_TEST]) attribEnableShadow[cap] = false;
const attribStateShadow = {
  color: { blendSrcRgb: glCtx.ONE, blendDstRgb: glCtx.ZERO, blendSrcAlpha: glCtx.ONE, blendDstAlpha: glCtx.ZERO,
    colorMask: [true,true,true,true], clearColor: [0,0,0,0] },
  depth: { writeMask: true, func: glCtx.LESS, clearValue: 1 },
  viewport: { box: [0,0,1280,720], depthRange: [0,1] },
  stencil: { func: glCtx.ALWAYS, ref: 0, valueMask: 0xffffffff, writeMask: 0xffffffff,
    fail: glCtx.KEEP, depthFail: glCtx.KEEP, depthPass: glCtx.KEEP, clearValue: 0 },
};
const alphaTestState = { enabled: false, func: glCtx.ALWAYS, ref: 0 };
let texture2DEnabled = false;
const presentationStats = { attribPushCount: 0, attribPopCount: 0, attribSnapshotQueriesAvoided: 0, attribSnapshotQueriesExecuted: 0 };
const context = vm.createContext({
  glCtx, attribEnableShadow, attribStateShadow, alphaTestState,
  texture2DEnabled, attribStateShadowEnabled: true, presentationStats, Object, Number, Array,
  alphaTestEnabledLocation: 'alpha-enabled', alphaFuncLocation: 'alpha-func',
  alphaRefLocation: 'alpha-ref', texMaskLocation: 'tex-mask',
});
vm.runInContext(functions, context);

context.setCompatEnableState(glCtx.BLEND, true);
context.setCompatEnableState(glCtx.DEPTH_TEST, true);
context.setCompatEnableState(0x0BC0, true);
context.setTexture2DEnabled(true);
context.setBlendFuncShadow(770, 771);
context.setColorMaskShadow(false, true, false, true);
context.setClearColorShadow(0.1, 0.2, 0.3, 0.4);
context.setDepthMaskShadow(false);
context.setDepthFuncShadow(glCtx.LEQUAL);
context.setClearDepthShadow(0.5);
context.setViewportShadow(1, 2, 300, 400);
context.setStencilFuncShadow(glCtx.NOTEQUAL, 3, 0xff);
context.setStencilOpShadow(glCtx.REPLACE, glCtx.INCR, glCtx.DECR);
const snapshot = context.snapshotAttribState(0x6D00);
expect(presentationStats.attribPushCount === 1, 'attrib push telemetry missing');
expect(syncQueryCalls === 0, `shadow snapshot performed ${syncQueryCalls} synchronous queries`);
expect(presentationStats.attribSnapshotQueriesAvoided === 24,
  `expected 24 avoided synchronous state queries, got ${presentationStats.attribSnapshotQueriesAvoided}`);
expect(snapshot.enable[glCtx.BLEND] === true && snapshot.enable[glCtx.DEPTH_TEST] === true, 'enable snapshot mismatch');
expect(snapshot.enable[0x0BC0] === true && snapshot.enable[glCtx.TEXTURE_2D] === true, 'compat enable snapshot mismatch');
expect(snapshot.color.blendSrcRgb === 770 && snapshot.color.blendDstRgb === 771, 'blend snapshot mismatch');
expect(JSON.stringify(snapshot.color.colorMask) === JSON.stringify([false,true,false,true]), 'color mask snapshot mismatch');
expect(snapshot.depth.writeMask === false && snapshot.depth.func === glCtx.LEQUAL && snapshot.depth.clearValue === 0.5,
  'depth snapshot mismatch');
expect(JSON.stringify(snapshot.viewport.box) === JSON.stringify([1,2,300,400]), 'viewport snapshot mismatch');
expect(snapshot.stencil.func === glCtx.NOTEQUAL && snapshot.stencil.ref === 3 && snapshot.stencil.valueMask === 0xff,
  'stencil snapshot mismatch');

// Prove the snapshot is independent and restore rewinds every state group.
context.setCompatEnableState(glCtx.BLEND, false);
context.setCompatEnableState(glCtx.DEPTH_TEST, false);
context.setCompatEnableState(0x0BC0, false);
context.setTexture2DEnabled(false);
context.setBlendFuncShadow(1, 0);
context.setColorMaskShadow(true, true, true, true);
context.setClearColorShadow(0, 0, 0, 0);
context.setDepthMaskShadow(true);
context.setDepthFuncShadow(glCtx.LESS);
context.setClearDepthShadow(1);
context.setViewportShadow(0, 0, 640, 480);
context.setStencilFuncShadow(glCtx.ALWAYS, 0, 0xffffffff);
context.setStencilOpShadow(glCtx.KEEP, glCtx.KEEP, glCtx.KEEP);
context.restoreAttribState(snapshot);
expect(presentationStats.attribPopCount === 1, 'attrib pop telemetry missing');
expect(attribEnableShadow[glCtx.BLEND] === true && attribEnableShadow[glCtx.DEPTH_TEST] === true, 'enable restore mismatch');
expect(alphaTestState.enabled === true, 'alpha enable restore mismatch');
expect(context.texture2DEnabled === true, 'texture enable restore mismatch');
expect(attribStateShadow.color.blendSrcRgb === 770 && attribStateShadow.color.blendDstRgb === 771, 'blend restore mismatch');
expect(JSON.stringify(attribStateShadow.color.colorMask) === JSON.stringify([false,true,false,true]), 'color restore mismatch');
expect(attribStateShadow.depth.writeMask === false && attribStateShadow.depth.func === glCtx.LEQUAL, 'depth restore mismatch');
expect(JSON.stringify(attribStateShadow.viewport.box) === JSON.stringify([1,2,300,400]), 'viewport restore mismatch');
expect(attribStateShadow.stencil.func === glCtx.NOTEQUAL && attribStateShadow.stencil.ref === 3, 'stencil restore mismatch');

// The kill-switch path must reproduce the exact snapshot through real WebGL-style
// queries, providing an on-device differential fallback if a driver exposes a surprise.
context.attribStateShadowEnabled = false;
syncQueryCalls = 0;
const legacySnapshot = context.snapshotAttribState(0x6D00);
expect(JSON.stringify(legacySnapshot) === JSON.stringify(snapshot),
  `legacy/shadow differential mismatch\nshadow=${JSON.stringify(snapshot)}\nlegacy=${JSON.stringify(legacySnapshot)}`);
expect(syncQueryCalls === 24, `legacy path should execute 24 synchronous queries, got ${syncQueryCalls}`);
expect(presentationStats.attribSnapshotQueriesExecuted === 24, 'legacy query telemetry mismatch');
expect(presentationStats.attribPushCount === 2, 'push telemetry should include differential snapshot');
console.log(`verify-lwjgl-attrib-shadow: OK avoided=${presentationStats.attribSnapshotQueriesAvoided} executed=${presentationStats.attribSnapshotQueriesExecuted} pushes=${presentationStats.attribPushCount}`);
