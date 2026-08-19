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
    if (ch === '"' || ch === "'" || ch === '`') { quote = ch; continue; }
    if (ch === '{') depth++;
    else if (ch === '}') {
      depth--;
      if (depth === 0) return source.slice(start, i + 1);
    }
  }
  throw new Error(`unterminated function ${name}`);
}

function expect(value, message) {
  if (!value) throw new Error(message);
}

const path = process.argv[2];
if (!path) throw new Error('usage: node ci/verify-lwjgl-client-buffer-streaming.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
expect(source.includes('WEBGL_CLIENT_BUFFER_STREAMING_V1'), 'missing streaming marker');
const helper = extractFunction(source, 'uploadClientArrayBuffer');
const uploadImpl = extractFunction(source, 'uploadDataImpl');
expect(uploadImpl.includes('uploadClientArrayBuffer(buffer, uploadBuf);'), 'uploadDataImpl is not routed through streaming helper');
expect(!uploadImpl.includes('glCtx.bufferData(glCtx.ARRAY_BUFFER, uploadBuf'), 'uploadDataImpl still replaces storage directly');

const calls = [];
const glCtx = {
  ARRAY_BUFFER: 0x8892,
  STATIC_DRAW: 0x88E4,
  STREAM_DRAW: 0x88E0,
  bindBuffer(target, buffer) { calls.push(['bind', target, buffer]); },
  bufferData(target, dataOrSize, usage) { calls.push(['data', target, dataOrSize, usage]); },
  bufferSubData(target, offset, data) { calls.push(['sub', target, offset, data]); },
};
const stats = {
  clientArrayBufferAllocations: 0,
  clientArrayBufferSubDataCalls: 0,
  clientArrayBufferReuses: 0,
  clientArrayLegacyBufferDataCalls: 0,
  clientArrayUploadBytes: 0,
  clientArrayBufferCapacityBytes: 0,
};
const context = vm.createContext({
  glCtx,
  presentationStats: stats,
  clientBufferStreamingEnabled: true,
  clientArrayBufferCapacities: new Map(),
  Number,
  Math,
});
vm.runInContext(helper, context);

const bufferA = { id: 'a' };
const bufferB = { id: 'b' };
const first = new Uint8Array(100);
for (let i = 0; i < first.length; i++) first[i] = i & 255;
context.uploadClientArrayBuffer(bufferA, first);
expect(calls.length === 3, `first upload should bind+allocate+subData, got ${JSON.stringify(calls)}`);
expect(calls[1][0] === 'data' && calls[1][2] === 256 && calls[1][3] === glCtx.STREAM_DRAW, 'first allocation must be 256-byte STREAM_DRAW storage');
expect(calls[2][0] === 'sub' && calls[2][2] === 0 && calls[2][3] === first, 'first subData must receive the exact BufferSource view');
expect(stats.clientArrayBufferAllocations === 1 && stats.clientArrayBufferSubDataCalls === 1, 'first upload counters wrong');
expect(stats.clientArrayUploadBytes === 100 && stats.clientArrayBufferCapacityBytes === 256, 'first upload byte/capacity counters wrong');

calls.length = 0;
const second = new Uint8Array(80);
context.uploadClientArrayBuffer(bufferA, second);
expect(calls.length === 2 && calls[0][0] === 'bind' && calls[1][0] === 'sub', 'reuse must not call bufferData');
expect(calls[1][2] === 0 && calls[1][3] === second, 'reuse must upload exact second BufferSource');
expect(stats.clientArrayBufferReuses === 1 && stats.clientArrayBufferAllocations === 1, 'reuse counters wrong');
expect(stats.clientArrayUploadBytes === 180 && stats.clientArrayBufferCapacityBytes === 256, 'reuse byte/capacity counters wrong');

calls.length = 0;
const third = new Uint8Array(300);
context.uploadClientArrayBuffer(bufferA, third);
expect(calls.length === 3 && calls[1][0] === 'data' && calls[1][2] === 512, 'growth must double to 512 bytes');
expect(stats.clientArrayBufferAllocations === 2 && stats.clientArrayBufferCapacityBytes === 512, 'growth counters wrong');
expect(context.clientArrayBufferCapacities.get(bufferA) === 512, 'buffer A capacity not tracked');

calls.length = 0;
const other = new Uint8Array(20);
context.uploadClientArrayBuffer(bufferB, other);
expect(calls[1][0] === 'data' && calls[1][2] === 256, 'independent buffer must allocate its own capacity');
expect(context.clientArrayBufferCapacities.get(bufferB) === 256, 'buffer B capacity not independent');
expect(stats.clientArrayBufferCapacityBytes === 768, 'total live capacity counter should equal A+B capacities');

calls.length = 0;
context.clientBufferStreamingEnabled = false;
const legacy = new Uint8Array([9, 8, 7, 6]);
context.uploadClientArrayBuffer(bufferA, legacy);
expect(calls.length === 2, 'legacy path must bind+bufferData only');
expect(calls[1][0] === 'data' && calls[1][2] === legacy && calls[1][3] === glCtx.STATIC_DRAW, 'legacy path must preserve exact bufferData(BufferSource, STATIC_DRAW)');
expect(stats.clientArrayLegacyBufferDataCalls === 1, 'legacy bufferData counter wrong');
expect(stats.clientArrayBufferSubDataCalls === 4, 'legacy path must not issue bufferSubData');

console.log(`verify-lwjgl-client-buffer-streaming: OK alloc=${stats.clientArrayBufferAllocations} subData=${stats.clientArrayBufferSubDataCalls} reuse=${stats.clientArrayBufferReuses} legacy=${stats.clientArrayLegacyBufferDataCalls} capacity=${stats.clientArrayBufferCapacityBytes}`);