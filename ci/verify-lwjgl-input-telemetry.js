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
if (!path) throw new Error('usage: node ci/verify-lwjgl-input-telemetry.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const inputEventNanos = extractFunction(source, 'inputEventNanos');
const enqueueKeyboardDirect = extractFunction(source, 'enqueueKeyboardDirect');
const keyboardNext = extractFunction(source, 'Java_org_lwjgl_input_Keyboard_nNext');
let nowMs = 100;
const keyboardInputState = { down: new Uint8Array(256), queue: [], current: null, repeatEvents: false };
const inputStats = {
  droppedEvents: 0,
  directKeyboardEnqueued: 0,
  directKeyboardDelivered: 0,
  directKeyboardDropped: 0,
  directKeyboardLatencySamples: 0,
  directKeyboardLatencyAvgMs: 0,
  directKeyboardLatencyMaxMs: 0,
  directKeyboardLastLatencyMs: 0,
  keyboardQueueHighWater: 0,
  keyboardQueueDepth: 0,
};
const context = vm.createContext({
  performance: { now: () => nowMs },
  keyboardInputState,
  inputStats,
  Math,
  Number,
  Uint8Array,
});
vm.runInContext(`${inputEventNanos}\n${enqueueKeyboardDirect}\n${keyboardNext}`, context);
const firstNanos = context.inputEventNanos();
context.enqueueKeyboardDirect({ key: 46, state: true, charCode: 99, nanos: firstNanos, repeat: false });
expect(inputStats.directKeyboardEnqueued === 1, 'enqueue counter missing');
expect(inputStats.keyboardQueueDepth === 1, 'queue depth not updated on enqueue');
nowMs = 112.5;
expect(context.Java_org_lwjgl_input_Keyboard_nNext(), 'queued event was not delivered');
expect(inputStats.directKeyboardDelivered === 1, 'delivery counter missing');
expect(inputStats.keyboardQueueDepth === 0, 'queue depth not updated on delivery');
expect(Math.abs(inputStats.directKeyboardLastLatencyMs - 12.5) < 0.001,
  `unexpected input latency ${inputStats.directKeyboardLastLatencyMs}`);
expect(Math.abs(inputStats.directKeyboardLatencyAvgMs - 12.5) < 0.001, 'latency average mismatch');
expect(Math.abs(inputStats.directKeyboardLatencyMaxMs - 12.5) < 0.001, 'latency max mismatch');

keyboardInputState.queue.length = 0;
inputStats.keyboardQueueDepth = 0;
for (let i = 0; i < 256; i++) {
  context.enqueueKeyboardDirect({ key: 1, state: true, charCode: 0, nanos: firstNanos, repeat: false });
}
context.enqueueKeyboardDirect({ key: 2, state: false, charCode: 0, nanos: firstNanos, repeat: false });
expect(keyboardInputState.queue.length === 256, 'keyboard queue cap changed');
expect(inputStats.keyboardQueueDepth === 256, 'queue depth cap telemetry mismatch');
expect(inputStats.directKeyboardDropped === 1 && inputStats.droppedEvents === 1,
  'keyboard overflow must be observable as a drop');
console.log(`verify-lwjgl-input-telemetry: OK latencyMs=${inputStats.directKeyboardLastLatencyMs} enqueued=${inputStats.directKeyboardEnqueued} dropped=${inputStats.directKeyboardDropped}`);
