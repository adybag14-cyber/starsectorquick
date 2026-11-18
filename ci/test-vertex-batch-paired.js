'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { runVertexBatchPairedExperiment } = require('./cheerpj-vertex-batch-paired');
const { captureExperimentFlags } = require('./renderer-experiment-state');
const { intervalStats } = require('./cheerpj-paired-experiment');

function fixture(initial, fault) {
  let clock = 0;
  let waits = 0;
  let faultApplied = false;
  let restorationAttempts = 0;
  const screenshots = [];
  let intervals = [];
  const canvas = { width: 1024, height: 768 };
  const window = {
    ...initial,
    __STARSECTOR_RUNTIME_STATE__: { state: 'campaign' },
    __lwjglVertexBatchStats: { enabledBegins: 0, disabledBegins: 0, batchCalls: 0, batchVertices: 0, legacyVertices: 0, drawVertices: 0, draws: 0 },
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
      window.__lwjglVertexBatchStats.draws += count;
      window.__lwjglVertexBatchStats.drawVertices += count * 8;
      if (enabled) {
        window.__lwjglVertexBatchStats.enabledBegins += count;
        window.__lwjglVertexBatchStats.batchCalls += count;
        window.__lwjglVertexBatchStats.batchVertices += count * 6;
        window.__lwjglVertexBatchStats.legacyVertices += count * 2;
      } else {
        window.__lwjglVertexBatchStats.disabledBegins += count;
        window.__lwjglVertexBatchStats.legacyVertices += count * 8;
      }
      // Deliberately slower candidate: a valid experiment must retain a negative
      // result instead of turning an attractive median into a qualification.
      intervals = Array(240).fill(enabled ? 110 : 100);
      if (enabled) { intervals[0] = 400; intervals[239] = 220; }
      clock += measurement ? intervals.reduce((a, b) => a + b, 0) + 100 : count * 100;
      if (measurement && index === 1 && fault && fault !== 'cleanup') {
        faultApplied = true;
        if (fault === 'counters') window.__lwjglVertexBatchStats.batchCalls -= count;
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
    }
    for (const [fault, error] of [
      ['counters', /batch branch was not exercised/],
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
      assert.equal(report.blocks.length, 1); checks++;
      assert.equal(f.screenshots.length, 0); checks++;
      assert.equal(report.blocks[0].intervalsMs.length, 240); checks++;
      assert.equal(report.failedWindow.index, 1); checks++;
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
  console.log('test-vertex-batch-paired: OK ' + checks + ' checks; paired raw evidence, retained geometry, control flags, screenshot timing, partial failures and cleanup');
})().catch(error => { console.error(error); process.exitCode = 1; });
