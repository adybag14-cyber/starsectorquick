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
const setEnabled = extractFunction(source, 'setVertexAttribArrayEnabledCached');
const setPointer = extractFunction(source, 'setVertexAttribPointerCached');
const calls = [];
const presentationStats = {
  vertexAttribPointerUpdates: 0,
  vertexAttribPointerUpdatesSaved: 0,
  vertexAttribEnableChanges: 0,
  vertexAttribEnableChangesSaved: 0,
};
const context = vm.createContext({
  glCtx: {
    enableVertexAttribArray(loc) { calls.push(['enable', loc]); },
    disableVertexAttribArray(loc) { calls.push(['disable', loc]); },
    vertexAttribPointer(loc, size, type, normalized, stride, offset) {
      calls.push(['pointer', loc, size, type, normalized, stride, offset]);
    },
  },
  vertexAttribStateCacheEnabled: true,
  vertexAttribEnabledState: Object.create(null),
  vertexAttribPointerState: Object.create(null),
  presentationStats,
});
vm.runInContext(`${setEnabled}\n${setPointer}`, context);
const vertexBuffer = { name: 'vertex' };
context.setVertexAttribArrayEnabledCached(1, true);
context.setVertexAttribArrayEnabledCached(1, true);
expect(calls.filter(c => c[0] === 'enable').length === 1, 'repeated enable should be elided');
expect(presentationStats.vertexAttribEnableChangesSaved === 1, 'enable saving telemetry missing');
context.setVertexAttribPointerCached(1, vertexBuffer, 3, 5126, false, 12, 0);
context.setVertexAttribPointerCached(1, vertexBuffer, 3, 5126, false, 12, 0);
expect(calls.filter(c => c[0] === 'pointer').length === 1, 'identical pointer should be elided');
expect(presentationStats.vertexAttribPointerUpdatesSaved === 1, 'pointer saving telemetry missing');
context.setVertexAttribPointerCached(1, vertexBuffer, 2, 5126, false, 8, 0);
expect(calls.filter(c => c[0] === 'pointer').length === 2, 'layout change must update pointer');
context.setVertexAttribArrayEnabledCached(1, false);
context.setVertexAttribArrayEnabledCached(1, false);
expect(calls.filter(c => c[0] === 'disable').length === 1, 'repeated disable should be elided');
context.vertexAttribStateCacheEnabled = false;
context.setVertexAttribArrayEnabledCached(1, false);
context.setVertexAttribPointerCached(1, vertexBuffer, 2, 5126, false, 8, 0);
expect(calls.filter(c => c[0] === 'disable').length === 2, 'disabled cache must preserve enable/disable call behavior');
expect(calls.filter(c => c[0] === 'pointer').length === 3, 'disabled cache must preserve pointer call behavior');
console.log(`verify-lwjgl-vertex-attrib-cache: OK pointerSaved=${presentationStats.vertexAttribPointerUpdatesSaved} enableSaved=${presentationStats.vertexAttribEnableChangesSaved}`);
