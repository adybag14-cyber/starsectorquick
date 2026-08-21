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
if (!path) throw new Error('usage: node ci/verify-lwjgl-frame-timing.js <lwjgl.js>');
const source = fs.readFileSync(path, 'utf8');
const percentile = extractFunction(source, 'presentationPercentile');
const update = extractFunction(source, 'updatePresentationTimingStats');
expect(source.includes('updatePresentationTimingStats();'), 'swap path does not update timing telemetry');
expect(source.includes('presentationStats.droppedFrameEstimate +='), 'dropped-frame telemetry is not wired into swaps');
expect(source.includes('presentationStats.longFrameCount++'), 'long-frame telemetry is not wired into swaps');

const presentationStats = {
  targetFps: 60,
  targetFrameMs: 1000 / 60,
  frameP50Ms: 0,
  frameP95Ms: 0,
  frameP99Ms: 0,
  frameMinMs: 0,
  frameMaxMs: 0,
  frameJitterStdDevMs: 0,
  frameJitterP95Ms: 0,
  recentLongFrameCount: 0,
  recentDroppedFrameEstimate: 0,
};
const context = vm.createContext({
  presentationStats,
  recentFrameIntervals: [16, 16, 17, 17, 16, 50],
  Math,
  Array,
});
vm.runInContext(`${percentile}\n${update}`, context);
context.updatePresentationTimingStats();
expect(presentationStats.frameP50Ms === 16, `unexpected p50 ${presentationStats.frameP50Ms}`);
expect(presentationStats.frameP95Ms === 50, `unexpected p95 ${presentationStats.frameP95Ms}`);
expect(presentationStats.frameP99Ms === 50, `unexpected p99 ${presentationStats.frameP99Ms}`);
expect(presentationStats.frameMinMs === 16 && presentationStats.frameMaxMs === 50,
  `unexpected min/max ${presentationStats.frameMinMs}/${presentationStats.frameMaxMs}`);
expect(presentationStats.frameJitterStdDevMs > 0, 'jitter stddev should detect the long frame');
expect(presentationStats.frameJitterP95Ms === 34, `unexpected jitter p95 ${presentationStats.frameJitterP95Ms}`);
expect(presentationStats.recentLongFrameCount === 1, `unexpected long-frame count ${presentationStats.recentLongFrameCount}`);
expect(presentationStats.recentDroppedFrameEstimate === 2,
  `unexpected dropped estimate ${presentationStats.recentDroppedFrameEstimate}`);
console.log(`verify-lwjgl-frame-timing: OK p50=${presentationStats.frameP50Ms} p95=${presentationStats.frameP95Ms} jitter=${presentationStats.frameJitterStdDevMs.toFixed(2)}`);
