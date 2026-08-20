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
if (!path) throw new Error('usage: node ci/verify-lwjgl-vertex-attrib-cache.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
expect(source.includes('LWJGL_VERTEX_ATTRIB_POINTER_CACHE_V1'), 'missing pointer-cache marker');
expect(!source.includes('setVertexAttribArrayEnabledCached'), 'pointer-only candidate must not cache enable state');
const setPointer = extractFunction(source, 'setVertexAttribPointerCached');
const calls = [];
const presentationStats = { vertexAttribPointerCacheHitObserved: false };
const context = vm.createContext({
  glCtx: {
    vertexAttribPointer(loc, size, type, normalized, stride, offset) {
      calls.push(['pointer', loc, size, type, normalized, stride, offset]);
    },
  },
  vertexAttribPointerCacheEnabled: true,
  vertexAttribPointerState: Object.create(null),
  presentationStats,
});
vm.runInContext(setPointer, context);
const vertexBuffer = { name: 'vertex' };
context.setVertexAttribPointerCached(1, vertexBuffer, 3, 5126, false, 12, 0);
context.setVertexAttribPointerCached(1, vertexBuffer, 3, 5126, false, 12, 0);
expect(calls.length === 1, 'identical pointer should be elided');
expect(presentationStats.vertexAttribPointerCacheHitObserved === true, 'pointer cache-hit proof missing');
context.setVertexAttribPointerCached(1, vertexBuffer, 2, 5126, false, 8, 0);
expect(calls.length === 2, 'layout change must update pointer');
context.vertexAttribPointerCacheEnabled = false;
context.setVertexAttribPointerCached(1, vertexBuffer, 2, 5126, false, 8, 0);
expect(calls.length === 3, 'disabled cache must preserve pointer call behavior');
console.log(`verify-lwjgl-vertex-attrib-cache: OK pointerOnly=true pointerHit=${presentationStats.vertexAttribPointerCacheHitObserved}`);
