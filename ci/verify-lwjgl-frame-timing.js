const assert = require('assert');
const fs = require('fs');
const vm = require('vm');

const sourcePath = process.argv[2] || 'build/final/wasm-modules/lwjgl.js';
const source = fs.readFileSync(sourcePath, 'utf8');
const start = source.indexOf('var frameCount = 0;');
const end = source.indexOf('// Set to a non-zero value to stop after a certain number of frames');

assert(start >= 0 && end > start, 'frame-timing source section is missing');
const section = source.slice(start, end);
assert.match(section, /LWJGL_FRAME_TIMING_RING_V1/);
assert.match(section, /new Float64Array\(240\)/);

const swapStart = source.indexOf('function Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers()');
const swapEnd = source.indexOf('\nfunction ', swapStart + 16);
assert(swapStart >= 0 && swapEnd > swapStart, 'swap-buffer function is missing');
const swap = source.slice(swapStart, swapEnd);
assert.match(swap, /recordPresentationFrameInterval\(swapNow\)/);
assert.doesNotMatch(swap, /updatePresentationTimingStats\(/, 'tail calculation must remain on demand');
const recordStart = source.indexOf('function recordPresentationFrameInterval(swapNow)');
const recordEnd = source.indexOf('function updatePresentationTimingStats()', recordStart);
assert(recordStart >= 0 && recordEnd > recordStart, 'frame recorder is missing');
const recorder = source.slice(recordStart, recordEnd);
assert.doesNotMatch(recorder, /new\s|\.push\(|\.shift\(|\.slice\(|\.sort\(/,
  'always-on timing recorder must not allocate or sort');

const context = vm.createContext({
  window: {},
  Float64Array,
  Number,
  Math,
  Array,
  detailedDrawTelemetryEnabled: false,
});
vm.runInContext(section, context, { filename: sourcePath });

assert.strictEqual(context.window.__lwjglPresentationStats.targetFps, 60);
assert.strictEqual(typeof context.window.__lwjglRefreshPresentationStats, 'function');
assert.strictEqual(typeof context.window.__lwjglResetPresentationTimingWindow, 'function');

context.recordPresentationFrameInterval(0);
context.recordPresentationFrameInterval(16);
context.recordPresentationFrameInterval(33);
context.recordPresentationFrameInterval(83);
context.recordPresentationFrameInterval(100);
const stats = context.updatePresentationTimingStats();
assert.strictEqual(stats.frameSampleCount, 4);
assert.strictEqual(stats.lastFrameMs, 17);
assert.strictEqual(stats.recentFrameMs, 25);
assert.strictEqual(stats.recentFps, 40);
assert.strictEqual(stats.frameP50Ms, 17);
assert.strictEqual(stats.frameP95Ms, 50);
assert.strictEqual(stats.frameP99Ms, 50);
assert.strictEqual(stats.frameMinMs, 16);
assert.strictEqual(stats.frameMaxMs, 50);
assert.strictEqual(stats.frameJitterP95Ms, 33);
assert.strictEqual(stats.recentLongFrameCount, 1);
assert.strictEqual(stats.longFrameCount, 1);
assert.strictEqual(stats.recentDroppedFrameEstimate, 2);
assert.strictEqual(stats.droppedFrameEstimate, 2);

context.window.__lwjglResetPresentationTimingWindow();
assert.strictEqual(stats.frameSampleCount, 0);
assert.strictEqual(stats.recentFps, 0);
assert.strictEqual(stats.frameP99Ms, 0);
assert.strictEqual(stats.longFrameCount, 1, 'window reset must preserve cumulative long-frame count');

for (let i = 0; i <= 300; i++) context.recordPresentationFrameInterval(i * 10);
context.window.__lwjglRefreshPresentationStats();
assert.strictEqual(stats.frameSampleCount, 240, 'timing ring must remain bounded');
assert.strictEqual(stats.recentFps, 100);
assert.strictEqual(stats.frameP95Ms, 10);
assert.strictEqual(stats.frameJitterP95Ms, 0);

console.log(`verify-lwjgl-frame-timing: OK samples=${stats.frameSampleCount} fps=${stats.recentFps.toFixed(2)} p95=${stats.frameP95Ms.toFixed(2)} jitterP95=${stats.frameJitterP95Ms.toFixed(2)}`);
