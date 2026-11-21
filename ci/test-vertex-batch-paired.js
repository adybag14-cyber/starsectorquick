'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { runVertexBatchPairedExperiment } = require('./cheerpj-vertex-batch-paired');
const { captureExperimentFlags } = require('./renderer-experiment-state');
const { intervalStats } = require('./cheerpj-paired-experiment');

function fixture(initial, fault, transport = {}) {
  let clock = 0;
  let waits = 0;
  let faultApplied = false;
  let restorationAttempts = 0;
  let memoryViewReady = false;
  let pendingBatchVertices = 0;
  const screenshots = [];
  let intervals = [];
  const canvas = { width: 1024, height: 768 };
  const singleCallsPerPrimitive = transport.singleCalls ?? 1;
  const pairCallsPerPrimitive = transport.pairCalls ?? 1;
  const quadCallsPerPrimitive = transport.quadCalls ?? 1;
  const memoryCallsPerPrimitive = transport.memoryCalls ?? 0;
  const batchVerticesPerPrimitive = singleCallsPerPrimitive + pairCallsPerPrimitive * 2 + quadCallsPerPrimitive * 4
    + memoryCallsPerPrimitive * (transport.genericVertices ?? 6);
  const window = {
    ...initial,
    __STARSECTOR_RUNTIME_STATE__: { state: 'campaign' },
    __lwjglVertexBatchStats: { enabledBegins: 0, disabledBegins: 0, batchCalls: 0, singleCalls: 0, pairCalls: 0, quadCalls: 0, memoryViewCreations: 0, batchVertices: 0, legacyVertices: 0, drawVertices: 0, draws: 0 },
    __lwjglPresentationStats: { swapCount: 0 },
    __lwjglResetPresentationTimingWindow: () => { intervals = []; },
    __lwjglRefreshPresentationStats: () => {
      const s = intervalStats(intervals);
      Object.assign(window.__lwjglPresentationStats, {
        frameSampleCount: s.samples, recentFps: s.fps, recentFrameMs: s.meanMs,
        frameP50Ms: s.p50Ms, frameP95Ms: s.p95Ms, frameP99Ms: s.p99Ms,
        frameMaxMs: s.maxMs, frameJitterP95Ms: s.jitterP95Ms
      });
      if (fault === 'timing' && faultApplied) window.__lwjglPresentationStats.frameP99Ms++;
    },
    __lwjglSnapshotPresentationIntervals: () => ({
      count: fault === 'raw' && faultApplied ? 239 : intervals.length,
      intervalsMs: intervals.slice(), swapCount: window.__lwjglPresentationStats.swapCount
    }),
    __lwjglInvalidateExtendedAttribState: () => {},
    __lwjglInvalidateVertexArrayState: () => {}
  };
  const evaluate = async (fn, arg) => {
    if (fn.toString().includes('for (const row of saved)')) {
      restorationAttempts++;
      if (fault === 'cleanup') throw new Error('injected cleanup failure');
    }
    const value = vm.runInNewContext('(' + fn.toString() + ')(input)', {
      window, input: arg, document: { getElementById: id => id === 'lwjglCanvas' ? canvas : null },
      performance: { now: () => clock }
    });
    return structuredClone(value);
  };
  const page = {
    evaluate,
    screenshot: async options => {
      // Each completed block is persisted after its raw intervals and counters
      // pass validation. Reading it also catches a screenshot moved into a window.
      const completed = JSON.parse(fs.readFileSync(path.join(path.dirname(options.path), 'vertex-batch-paired.json'), 'utf8'));
      const snapshot = {
        path: options.path, waits, blocks: completed.blocks.length,
        enabled: window.__LWJGL_VERTEX_BATCH__, restorationAttempts
      };
      screenshots.push(snapshot);
      assert.equal(snapshot.waits, 16, 'screenshot must follow all eight timed windows');
      assert.equal(snapshot.blocks, 8, 'screenshot must follow eight validated blocks');
      assert.ok(completed.blocks.every(row => row.intervalsMs.length === 240));
      assert.equal(snapshot.enabled, true, 'screenshot must show the enabled candidate');
      assert.equal(snapshot.restorationAttempts, 0, 'screenshot must precede restoration');
      if (fault === 'screenshot') throw new Error('injected screenshot failure');
    },
    waitForFunction: async (fn, start, options) => {
      const measurement = waits % 2 === 1;
      const index = Math.floor(waits / 2);
      waits++;
      assert.equal(options.polling, 250);
      assert.equal(options.timeout, measurement ? 120000 : 60000);
      const count = measurement ? 241 : 24;
      window.__lwjglPresentationStats.swapCount += count;
      const enabled = window.__LWJGL_VERTEX_BATCH__;
      if (pendingBatchVertices) {
        // Complete the previous window's open primitive during warmup.
        window.__lwjglVertexBatchStats.batchVertices += pendingBatchVertices;
        window.__lwjglVertexBatchStats.drawVertices += pendingBatchVertices;
        window.__lwjglVertexBatchStats.draws++;
        pendingBatchVertices = 0;
      }
      window.__lwjglVertexBatchStats.draws += count;
      window.__lwjglVertexBatchStats.drawVertices += count * (batchVerticesPerPrimitive + 2);
      if (enabled) {
        window.__lwjglVertexBatchStats.enabledBegins += count;
        window.__lwjglVertexBatchStats.batchCalls += count * (singleCallsPerPrimitive + pairCallsPerPrimitive + quadCallsPerPrimitive + memoryCallsPerPrimitive);
        window.__lwjglVertexBatchStats.singleCalls += count * singleCallsPerPrimitive;
        window.__lwjglVertexBatchStats.pairCalls += count * pairCallsPerPrimitive;
        window.__lwjglVertexBatchStats.quadCalls += count * quadCallsPerPrimitive;
        if (memoryCallsPerPrimitive > 0) {
          if (!memoryViewReady) {
            window.__lwjglVertexBatchStats.memoryViewCreations++;
            memoryViewReady = true;
          }
          if (measurement && transport.growMemory) window.__lwjglVertexBatchStats.memoryViewCreations++;
        }
        window.__lwjglVertexBatchStats.batchVertices += count * batchVerticesPerPrimitive;
        window.__lwjglVertexBatchStats.legacyVertices += count * 2;
      } else {
        window.__lwjglVertexBatchStats.disabledBegins += count;
        window.__lwjglVertexBatchStats.legacyVertices += count * (batchVerticesPerPrimitive + 2);
      }
      if (measurement && enabled && (transport.openPairAtEnd || transport.openSingleAtEnd)) {
        // JNI call counters advance before completed-geometry counters at End.
        // Single/pair submissions need not match completed vertices per snapshot.
        window.__lwjglVertexBatchStats.enabledBegins++;
        if (transport.openSingleAtEnd) {
          window.__lwjglVertexBatchStats.singleCalls++;
          pendingBatchVertices = 1;
        } else {
          window.__lwjglVertexBatchStats.pairCalls++;
          pendingBatchVertices = 2;
        }
        window.__lwjglVertexBatchStats.batchCalls++;
      }
      if (!measurement && index === 1) {
        if (fault === 'initial-quad-counter') window.__lwjglVertexBatchStats.quadCalls = -1;
        if (fault === 'initial-single-counter') window.__lwjglVertexBatchStats.singleCalls = -1;
        if (fault === 'initial-single-missing') delete window.__lwjglVertexBatchStats.singleCalls;
        if (fault === 'initial-pair-counter') window.__lwjglVertexBatchStats.pairCalls = -1;
        if (fault === 'initial-pair-missing') delete window.__lwjglVertexBatchStats.pairCalls;
        if (fault === 'initial-view-counter') window.__lwjglVertexBatchStats.memoryViewCreations = Infinity;
      }
      // Deliberately slower candidate: a valid experiment must retain a negative
      // result instead of turning an attractive median into a qualification.
      intervals = Array(240).fill(enabled ? 110 : 100);
      if (enabled) { intervals[0] = 400; intervals[239] = 220; }
      clock += measurement ? intervals.reduce((a, b) => a + b, 0) + 100 : count * 100;
      if (measurement && index === (fault === 'pair-baseline' || fault === 'single-baseline' ? 3 : 1) && fault && fault !== 'cleanup') {
        faultApplied = true;
        if (fault === 'counters') {
          window.__lwjglVertexBatchStats.batchCalls -= count * 3;
          window.__lwjglVertexBatchStats.singleCalls -= count;
          window.__lwjglVertexBatchStats.quadCalls -= count;
          window.__lwjglVertexBatchStats.pairCalls -= count;
        }
        if (fault === 'quad-subset') window.__lwjglVertexBatchStats.quadCalls += count * 2;
        // Each specialized subset stays below batchCalls; only their sum fails.
        if (fault === 'combined-subset') window.__lwjglVertexBatchStats.pairCalls++;
        if (fault === 'pair-baseline') {
          window.__lwjglVertexBatchStats.pairCalls++;
          window.__lwjglVertexBatchStats.batchCalls++;
        }
        if (fault === 'single-baseline') {
          window.__lwjglVertexBatchStats.singleCalls++;
          window.__lwjglVertexBatchStats.batchCalls++;
        }
        if (fault === 'single-missing') delete window.__lwjglVertexBatchStats.singleCalls;
        if (fault === 'single-nonfinite') window.__lwjglVertexBatchStats.singleCalls = Infinity;
        if (fault === 'single-negative') window.__lwjglVertexBatchStats.singleCalls = -1;
        if (fault === 'single-fractional') window.__lwjglVertexBatchStats.singleCalls += 0.5;
        if (fault === 'single-unsafe') window.__lwjglVertexBatchStats.singleCalls = Number.MAX_SAFE_INTEGER + 1;
        if (fault === 'single-decreasing') window.__lwjglVertexBatchStats.singleCalls -= count + 1;
        if (fault === 'pair-missing') delete window.__lwjglVertexBatchStats.pairCalls;
        if (fault === 'pair-nonfinite') window.__lwjglVertexBatchStats.pairCalls = Infinity;
        if (fault === 'pair-negative') window.__lwjglVertexBatchStats.pairCalls = -1;
        if (fault === 'pair-fractional') window.__lwjglVertexBatchStats.pairCalls += 0.5;
        if (fault === 'pair-unsafe') window.__lwjglVertexBatchStats.pairCalls = Number.MAX_SAFE_INTEGER + 1;
        if (fault === 'pair-decreasing') window.__lwjglVertexBatchStats.pairCalls -= count + 1;
        if (fault === 'quad-nonfinite') window.__lwjglVertexBatchStats.quadCalls = Infinity;
        if (fault === 'quad-negative') window.__lwjglVertexBatchStats.quadCalls = -1;
        if (fault === 'quad-fractional') window.__lwjglVertexBatchStats.quadCalls += 0.5;
        if (fault === 'view-nonfinite') window.__lwjglVertexBatchStats.memoryViewCreations = NaN;
        if (fault === 'view-negative') window.__lwjglVertexBatchStats.memoryViewCreations = -1;
        if (fault === 'view-fractional') window.__lwjglVertexBatchStats.memoryViewCreations += 0.5;
        if (fault === 'conservation') window.__lwjglVertexBatchStats.drawVertices++;
        if (fault === 'begins') window.__lwjglVertexBatchStats.disabledBegins++;
        if (fault === 'geometry') window.__lwjglVertexBatchStats.draws -= count;
        if (fault === 'flags') window.__LWJGL_DEPTH_STATE_CACHE__ = !window.__LWJGL_DEPTH_STATE_CACHE__;
        if (fault === 'canvas') canvas.width++;
        if (fault === 'state') window.__STARSECTOR_RUNTIME_STATE__.state = 'title';
        if (fault === 'mode') window.__LWJGL_VERTEX_BATCH__ = false;
        if (fault === 'attribution') window.__starsectorStallAttribution = {};
        if (fault === 'wait') throw new Error('injected sampling failure');
      }
      assert.equal(await evaluate(fn, start), true);
    }
  };
  return { page, window, screenshots, waits: () => waits, restorationAttempts: () => restorationAttempts };
}

(async () => {
  const parent = path.join(__dirname, '..', 'test_output');
  fs.mkdirSync(parent, { recursive: true });
  const directory = fs.mkdtempSync(path.join(parent, 'vertex-batch-paired-contract-'));
  const initial = {
    __LWJGL_COLOR_ATTRIB_CACHE__: false, __LWJGL_EXTENDED_ATTRIB_CACHE__: true,
    __LWJGL_CAMPAIGN_PACING__: 'flush', __LWJGL_DEPTH_STATE_CACHE__: false,
    __LWJGL_VERTEX_ARRAY_CACHE__: true, __LWJGL_VERTEX_BATCH__: true
  };
  let checks = 0;
  const log = console.log;
  try {
    console.log = () => {};
    for (const flags of [{}, initial]) {
      const f = fixture(flags);
      const saved = await captureExperimentFlags(f.page);
      const report = await runVertexBatchPairedExperiment(f.page, directory);
      assert.equal(report.ok, true); checks++;
      assert.equal(report.flagsRestored, true); checks++;
      assert.equal(report.baseline, 'scalar transport inside revised Java bridge'); checks++;
      assert.equal(report.candidate, 'reusable Java array/direct-buffer staging with primitive native calls for 1, 2 or 4 vertices and generic direct-memory batches'); checks++;
      assert.match(report.note, /disabled branch retains revised bridge branch\/flush overhead; this is not a byte-identical published baseline/); checks++;
      assert.deepEqual(await captureExperimentFlags(f.page), saved); checks++;
      assert.equal(f.waits(), 16); checks++;
      assert.equal(report.candidateScreenshot, 'vertex-batch-enabled.png'); checks++;
      assert.deepEqual(f.screenshots, [{
        path: path.join(directory, 'vertex-batch-enabled.png'), waits: 16,
        blocks: 8, enabled: true, restorationAttempts: 0
      }]); checks++;
      assert.equal(f.restorationAttempts(), 1); checks++;
      assert.deepEqual(report.blocks.map(row => row.enabled), [false, true, true, false, true, false, false, true]); checks++;
      assert.equal(report.blocks.flatMap(row => row.intervalsMs).length, 1920); checks++;
      assert.ok(report.blocks.every(row => row.intervalsMs.length === 240 && row.swapDelta === 241)); checks++;
      assert.ok(report.summary.changePercent.fps < 0); checks++;
      assert.equal(report.summary.pooled.candidate.maxMs, 400); checks++;
      assert.equal(report.summary.pooled.baseline.samples, 960); checks++;
      assert.equal(report.summary.pooled.candidate.samples, 960); checks++;
      assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.batchCalls > 0 && row.counters.batchVertices > 0 && row.counters.legacyVertices > 0)); checks++;
      assert.ok(report.blocks.filter(row => !row.enabled).every(row => row.counters.batchCalls === 0 && row.counters.legacyVertices === row.counters.drawVertices)); checks++;
      assert.ok(report.blocks.every(row => row.counters.memoryViewCreations === 0), 'single/pair/quad-only transport needs no generic memory view'); checks++;
      assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.singleCalls === 241 && row.counters.pairCalls === 241 && row.counters.quadCalls === 241 && row.counters.batchCalls === 723)); checks++;
      assert.ok(report.blocks.filter(row => !row.enabled).every(row => row.counters.singleCalls === 0 && row.counters.pairCalls === 0 && row.counters.quadCalls === 0)); checks++;
    }
    // Single-only, pair-only, generic-only and mixed batches are all valid.
    // In-flight native calls need not end within the same measurement window.
    for (const transport of [
      { singleCalls: 6, pairCalls: 0, quadCalls: 0, memoryCalls: 0 },
      { singleCalls: 0, pairCalls: 3, quadCalls: 0, memoryCalls: 0 },
      { singleCalls: 0, pairCalls: 0, quadCalls: 0, memoryCalls: 1 },
      { singleCalls: 1, pairCalls: 1, quadCalls: 1, memoryCalls: 1, genericVertices: 3 },
      { singleCalls: 0, pairCalls: 0, quadCalls: 1, memoryCalls: 1, growMemory: true },
      { singleCalls: 0, pairCalls: 3, quadCalls: 0, memoryCalls: 0, openPairAtEnd: true },
      { singleCalls: 6, pairCalls: 0, quadCalls: 0, memoryCalls: 0, openSingleAtEnd: true }
    ]) {
      const f = fixture(initial, undefined, transport);
      const saved = await captureExperimentFlags(f.page);
      const report = await runVertexBatchPairedExperiment(f.page, directory);
      assert.equal(report.ok, true); checks++;
      assert.equal(report.flagsRestored, true); checks++;
      assert.deepEqual(await captureExperimentFlags(f.page), saved); checks++;
      assert.equal(report.blocks.length, 8); checks++;
      assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.quadCalls === transport.quadCalls * 241)); checks++;
      assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.singleCalls === transport.singleCalls * 241 + (transport.openSingleAtEnd ? 1 : 0))); checks++;
      assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.pairCalls === transport.pairCalls * 241 + (transport.openPairAtEnd ? 1 : 0))); checks++;
      assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.batchCalls === (transport.singleCalls + transport.pairCalls + transport.quadCalls + transport.memoryCalls) * 241 + (transport.openPairAtEnd || transport.openSingleAtEnd ? 1 : 0))); checks++;
      assert.ok(report.blocks.every(row => row.counters.singleCalls + row.counters.pairCalls + row.counters.quadCalls <= row.counters.batchCalls)); checks++;
      assert.ok(report.blocks.filter(row => !row.enabled).every(row => row.counters.singleCalls === 0 && row.counters.pairCalls === 0 && row.counters.quadCalls === 0 && row.counters.batchCalls === 0)); checks++;
      assert.ok(report.blocks.every(row => row.counters.drawVertices === row.counters.batchVertices + row.counters.legacyVertices)); checks++;
      if (transport.openPairAtEnd) {
        assert.ok(report.blocks.filter(row => row.enabled).every(row => 2 * row.counters.pairCalls > row.counters.batchVertices),
          'native submissions and completed geometry may straddle snapshots'); checks++;
      }
      if (transport.openSingleAtEnd) {
        assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.singleCalls > row.counters.batchVertices),
          'single submissions can precede completed geometry across snapshots'); checks++;
      }
      assert.ok(report.blocks.filter(row => row.enabled).every(row => row.counters.memoryViewCreations === (transport.growMemory ? 1 : 0))); checks++;
      assert.ok(report.blocks.filter(row => !row.enabled).every(row => row.counters.memoryViewCreations === 0)); checks++;
    }
    for (const [fault, error, failedIndex = 1] of [
      ['counters', /batch branch was not exercised/],
      ['quad-subset', /single, pair and quad calls exceed total batch calls/],
      ['combined-subset', /single, pair and quad calls exceed total batch calls/],
      ['pair-baseline', /pair calls during baseline/, 3],
      ['single-baseline', /single calls during baseline/, 3],
      ['single-missing', /invalid final counter: singleCalls/],
      ['single-nonfinite', /invalid final counter: singleCalls/],
      ['single-negative', /invalid final counter: singleCalls/],
      ['single-fractional', /invalid final counter: singleCalls/],
      ['single-unsafe', /invalid final counter: singleCalls/],
      ['single-decreasing', /invalid final counter: singleCalls/],
      ['initial-single-counter', /invalid initial counter: singleCalls/],
      ['initial-single-missing', /invalid initial counter: singleCalls/],
      ['pair-missing', /invalid final counter: pairCalls/],
      ['pair-nonfinite', /invalid final counter: pairCalls/],
      ['pair-negative', /invalid final counter: pairCalls/],
      ['pair-fractional', /invalid final counter: pairCalls/],
      ['pair-unsafe', /invalid final counter: pairCalls/],
      ['pair-decreasing', /invalid final counter: pairCalls/],
      ['initial-pair-counter', /invalid initial counter: pairCalls/],
      ['initial-pair-missing', /invalid initial counter: pairCalls/],
      ['quad-nonfinite', /invalid final counter: quadCalls/],
      ['quad-negative', /invalid final counter: quadCalls/],
      ['quad-fractional', /invalid final counter: quadCalls/],
      ['view-nonfinite', /invalid final counter: memoryViewCreations/],
      ['view-negative', /invalid final counter: memoryViewCreations/],
      ['view-fractional', /invalid final counter: memoryViewCreations/],
      ['initial-quad-counter', /invalid initial counter: quadCalls/],
      ['initial-view-counter', /invalid initial counter: memoryViewCreations/],
      ['conservation', /completed vertex accounting disagrees/],
      ['begins', /disabled primitive during candidate/],
      ['geometry', /no immediate geometry drawn/],
      ['flags', /control flags changed/],
      ['canvas', /framebuffer changed/],
      ['timing', /raw intervals disagree/],
      ['state', /campaign/],
      ['raw', /239/],
      ['mode', /vertex batch branch changed/],
      ['attribution', /attribution observer was active/],
      ['wait', /injected sampling failure/]
    ]) {
      const f = fixture(initial, fault);
      const saved = await captureExperimentFlags(f.page);
      await assert.rejects(runVertexBatchPairedExperiment(f.page, directory), error); checks++;
      const report = JSON.parse(fs.readFileSync(path.join(directory, 'vertex-batch-paired.json'), 'utf8'));
      assert.equal(report.ok, false); checks++;
      assert.equal(report.flagsRestored, true); checks++;
      assert.deepEqual(await captureExperimentFlags(f.page), saved); checks++;
      assert.equal(report.blocks.length, failedIndex); checks++;
      assert.equal(f.screenshots.length, 0); checks++;
      assert.equal(report.blocks[0].intervalsMs.length, 240); checks++;
      assert.equal(report.failedWindow.index, failedIndex); checks++;
      if (fault !== 'wait') { assert.equal(report.failedWindow.after.raw.intervalsMs.length, 240); checks++; }
    }
    const screenshotFailure = fixture(initial, 'screenshot');
    const savedBeforeScreenshot = await captureExperimentFlags(screenshotFailure.page);
    await assert.rejects(runVertexBatchPairedExperiment(screenshotFailure.page, directory), /injected screenshot failure/); checks++;
    const screenshotReport = JSON.parse(fs.readFileSync(path.join(directory, 'vertex-batch-paired.json'), 'utf8'));
    assert.equal(screenshotReport.ok, false); checks++;
    assert.equal(screenshotReport.flagsRestored, true); checks++;
    assert.deepEqual(await captureExperimentFlags(screenshotFailure.page), savedBeforeScreenshot); checks++;
    assert.equal(screenshotReport.blocks.length, 8); checks++;
    assert.equal(screenshotFailure.screenshots.length, 1); checks++;
    assert.equal(screenshotFailure.restorationAttempts(), 1); checks++;
    assert.equal(screenshotReport.candidateScreenshot, undefined); checks++;
    const f = fixture({}, 'cleanup');
    await assert.rejects(runVertexBatchPairedExperiment(f.page, directory), /injected cleanup failure/); checks++;
    const failed = JSON.parse(fs.readFileSync(path.join(directory, 'vertex-batch-paired.json'), 'utf8'));
    assert.equal(failed.ok, false); checks++;
    assert.equal(failed.flagsRestored, false); checks++;
    assert.equal(failed.blocks.length, 8); checks++;
    assert.equal(f.screenshots.length, 1); checks++;
    assert.equal(f.screenshots[0].restorationAttempts, 0); checks++;
    const runner = fs.readFileSync(path.join(__dirname, 'cheerpj-runner-diagnostics.js'), 'utf8');
    const hook = runner.indexOf("process.env.STARSECTOR_VERTEX_BATCH_PAIRED === 'true'");
    assert.ok(hook > 0 && hook < runner.indexOf("session.send('Profiler.start')")); checks++;
  } finally {
    console.log = log;
    fs.rmSync(directory, { recursive: true, force: true });
  }
  console.log('test-vertex-batch-paired: OK ' + checks + ' checks; paired raw evidence, single/pair/quad/view counters, retained geometry and snapshot boundaries, control flags, screenshot timing, partial failures and cleanup');
})().catch(error => { console.error(error); process.exitCode = 1; });
