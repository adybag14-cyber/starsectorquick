'use strict';
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { intervalStats, summarizeBlocks } = require('./cheerpj-paired-experiment');
const { captureExperimentFlags, restoreExperimentFlags } = require('./renderer-experiment-state');

const order = [false, true, true, false, true, false, false, true];
const counterNames = ['enabledBegins', 'disabledBegins', 'batchCalls', 'quadCalls', 'memoryViewCreations', 'batchVertices', 'legacyVertices', 'drawVertices', 'draws'];

// A completed experiment can have a negative performance result. "ok" means
// valid paired evidence, never that the candidate was faster or is qualified.
async function runVertexBatchPairedExperiment(page, out) {
  const report = {
    design: 'ABBABAAB',
    baseline: 'scalar transport inside revised Java bridge',
    candidate: 'reusable Java array with direct four-vertex calls and generic direct-memory batches',
    framesPerWindow: 240,
    warmupFrames: 24,
    measurement: 'last 240 consecutive presentation intervals at prespecified polling stop',
    note: 'One running campaign/browser. Only vertex transport batching changes; other renderer flags and framebuffer stay fixed. Application presentation, not GPU completion or input latency. No profiler, screenshots or resource fetching inside timed windows. The disabled branch retains revised bridge branch/flush overhead; this is not a byte-identical published baseline.',
    blocks: []
  };
  const file = path.join(out, 'vertex-batch-paired.json');
  const original = await captureExperimentFlags(page);
  report.originalFlags = original;
  const controls = original.filter(row => row.name !== '__LWJGL_VERTEX_BATCH__');
  const controlNames = controls.map(row => row.name);
  let canvas = null;
  let currentWindow = null;
  try {
    for (const [index, enabled] of order.entries()) {
      currentWindow = { index, enabled, phase: 'warmup' };
      const warm = await page.evaluate(enabled => {
        if (!window.__lwjglVertexBatchStats
            || typeof window.__lwjglResetPresentationTimingWindow !== 'function'
            || typeof window.__lwjglRefreshPresentationStats !== 'function'
            || typeof window.__lwjglSnapshotPresentationIntervals !== 'function')
          throw new Error('Vertex batching experiment missing');
        if (window.__STARSECTOR_RUNTIME_STATE__?.state !== 'campaign')
          throw new Error('Vertex batching experiment requires a real campaign');
        if (window.__starsectorStallAttribution)
          throw new Error('Attribution observer must be outside paired timing');
        window.__LWJGL_VERTEX_BATCH__ = enabled;
        return window.__lwjglPresentationStats.swapCount;
      }, enabled);
      await page.waitForFunction(start => window.__lwjglPresentationStats.swapCount >= start + 24,
        warm, { timeout: 60000, polling: 250 });
      const before = await page.evaluate(names => {
        window.__lwjglResetPresentationTimingWindow();
        const canvas = document.getElementById('lwjglCanvas');
        return {
          at: performance.now(), swap: window.__lwjglPresentationStats.swapCount,
          state: window.__STARSECTOR_RUNTIME_STATE__?.state,
          counters: { ...window.__lwjglVertexBatchStats },
          canvas: [canvas?.width, canvas?.height],
          flags: names.map(name => ({ name, present: Object.prototype.hasOwnProperty.call(window, name), value: window[name] }))
        };
      }, controlNames);
      currentWindow = { index, enabled, phase: 'measurement', before };
      assert.equal(before.state, 'campaign');
      assert.deepEqual(before.flags, controls, 'control flags changed before the window');
      assert.ok(before.canvas.every(value => Number.isInteger(value) && value > 0), 'missing framebuffer');
      if (canvas === null) canvas = before.canvas;
      assert.deepEqual(before.canvas, canvas, 'framebuffer changed between windows');
      await page.waitForFunction(start => window.__lwjglPresentationStats.swapCount >= start + 241,
        before.swap, { timeout: 120000, polling: 250 });
      const after = await page.evaluate(names => {
        window.__lwjglRefreshPresentationStats();
        const canvas = document.getElementById('lwjglCanvas');
        return {
          at: performance.now(), state: window.__STARSECTOR_RUNTIME_STATE__?.state,
          enabled: window.__LWJGL_VERTEX_BATCH__,
          counters: { ...window.__lwjglVertexBatchStats },
          raw: window.__lwjglSnapshotPresentationIntervals(),
          reported: { ...window.__lwjglPresentationStats },
          canvas: [canvas?.width, canvas?.height],
          flags: names.map(name => ({ name, present: Object.prototype.hasOwnProperty.call(window, name), value: window[name] })),
          attributionActive: Boolean(window.__starsectorStallAttribution)
        };
      }, controlNames);
      // Retain this snapshot even if a subsequent validity check fails.
      currentWindow.after = after;
      assert.equal(after.state, 'campaign');
      assert.equal(after.enabled, enabled, 'vertex batch branch changed inside the window');
      assert.deepEqual(after.flags, controls, 'control flags changed inside the window');
      assert.deepEqual(after.canvas, canvas, 'framebuffer changed inside the window');
      assert.equal(after.attributionActive, false, 'attribution observer was active');
      assert.equal(after.raw.count, 240);
      assert.equal(after.raw.intervalsMs.length, 240);
      assert.equal(after.reported.frameSampleCount, 240);
      assert.ok(after.raw.swapCount - before.swap >= 241, 'not enough real swaps');
      assert.ok(after.at > before.at, 'nonpositive measurement duration');
      const counters = {};
      for (const name of counterNames) {
        assert.ok(Number.isSafeInteger(before.counters[name]) && before.counters[name] >= 0, 'invalid initial counter: ' + name);
        assert.ok(Number.isSafeInteger(after.counters[name]) && after.counters[name] >= before.counters[name], 'invalid final counter: ' + name);
        counters[name] = after.counters[name] - before.counters[name];
      }
      assert.ok(counters.quadCalls <= counters.batchCalls, 'quad calls exceed total batch calls');
      assert.ok(counters.draws > 0 && counters.drawVertices > 0, 'no immediate geometry drawn');
      // These three totals are published together at glEnd, so snapshots taken
      // between JNI calls still conserve completed primitive vertices exactly.
      assert.equal(counters.drawVertices, counters.batchVertices + counters.legacyVertices, 'completed vertex accounting disagrees');
      if (enabled) {
        assert.ok(counters.enabledBegins > 0, 'batching was not enabled for any primitive');
        assert.equal(counters.disabledBegins, 0, 'disabled primitive during candidate');
        assert.ok(counters.batchCalls > 0 && counters.batchVertices > 0, 'batch branch was not exercised');
      } else {
        assert.ok(counters.disabledBegins > 0 && counters.legacyVertices > 0, 'legacy branch was not exercised');
        assert.equal(counters.enabledBegins, 0, 'enabled primitive during baseline');
        assert.equal(counters.batchCalls, 0, 'batch calls during baseline');
        assert.equal(counters.quadCalls, 0, 'quad calls during baseline');
        assert.equal(counters.batchVertices, 0, 'batch vertices during baseline');
      }
      const statistics = intervalStats(after.raw.intervalsMs);
      for (const [key, reported] of [
        ['meanMs', 'recentFrameMs'], ['fps', 'recentFps'], ['p50Ms', 'frameP50Ms'],
        ['p95Ms', 'frameP95Ms'], ['p99Ms', 'frameP99Ms'], ['maxMs', 'frameMaxMs'],
        ['jitterP95Ms', 'frameJitterP95Ms']
      ]) {
        assert.ok(Number.isFinite(after.reported[reported]) && Math.abs(statistics[key] - after.reported[reported]) < 1e-6,
          'raw intervals disagree with runtime: ' + reported);
      }
      const row = {
        index, enabled, statistics, counters, canvas: after.canvas,
        wallMs: after.at - before.at, swapDelta: after.raw.swapCount - before.swap,
        intervalsMs: after.raw.intervalsMs
      };
      report.blocks.push(row);
      currentWindow = null;
      fs.writeFileSync(file, JSON.stringify(report, null, 2));
      console.log('[vertex-batch-paired]', JSON.stringify({ ...row, intervalsMs: undefined }));
    }
    report.summary = summarizeBlocks(report.blocks);
    // Visual evidence is captured only after the last timed window has ended.
    await page.screenshot({ path: path.join(out, 'vertex-batch-enabled.png') });
    report.candidateScreenshot = 'vertex-batch-enabled.png';
    report.ok = true;
  } catch (error) {
    report.ok = false;
    report.error = error.stack || String(error);
    if (currentWindow) report.failedWindow = currentWindow;
    throw error;
  } finally {
    try {
      await restoreExperimentFlags(page, original);
      report.flagsRestored = true;
    } catch (cleanupError) {
      report.ok = false;
      report.flagsRestored = false;
      report.cleanupError = String(cleanupError);
      throw cleanupError;
    } finally {
      fs.writeFileSync(file, JSON.stringify(report, null, 2));
    }
  }
  return report;
}

module.exports = { runVertexBatchPairedExperiment };
