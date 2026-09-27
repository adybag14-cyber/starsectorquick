'use strict';

// Deliberately separate observer-heavy profiling from the unprofiled timing
// windows. No skipped rendering, synthetic frames, or changed pass thresholds.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const os = require('node:os');
const { captureExperimentFlags } = require('./renderer-experiment-state');

function summarizeProfile(profile) {
  const nodes = new Map(profile.nodes.map(node => [node.id, node]));
  const costs = new Map();
  let totalUs = 0;
  for (let i = 0; i < (profile.samples || []).length; i++) {
    const node = nodes.get(profile.samples[i]);
    if (!node) continue;
    const us = Number(profile.timeDeltas?.[i] || 0);
    const frame = node.callFrame;
    const key = JSON.stringify([frame.functionName, frame.url, frame.lineNumber]);
    const row = costs.get(key) || { function: frame.functionName || '(anonymous)', url: frame.url,
      line: frame.lineNumber + 1, selfUs: 0, samples: 0 };
    row.selfUs += us; row.samples++; totalUs += us; costs.set(key, row);
  }
  return { totalUs, top: [...costs.values()].sort((a, b) => b.selfUs - a.selfUs).slice(0, 60)
    .map(row => ({ ...row, selfPercent: totalUs ? row.selfUs * 100 / totalUs : 0 })) };
}

async function collectDiagnostics(page, context, outputDir) {
  const out = path.join(outputDir, 'runner-diagnostics');
  fs.mkdirSync(out, { recursive: true });
  const report = { checkedAt: new Date().toISOString(), commit: process.env.GITHUB_SHA || null,
    runId: process.env.GITHUB_RUN_ID || null, attempt: process.env.GITHUB_RUN_ATTEMPT || null,
    machine: { platform: process.platform, architecture: process.arch, cpu: os.cpus()[0]?.model,
      logicalCpus: os.cpus().length, totalMemory: os.totalmem(), browser: context.browser().version() },
    windows: [], profile: null };
  try {
    report.runtime = await page.evaluate(() => ({
      state: window.__STARSECTOR_RUNTIME_STATE__, renderer: window.__lwjglGraphicsInfo,
      canvas: [document.getElementById('lwjglCanvas')?.width, document.getElementById('lwjglCanvas')?.height],
      resources: performance.getEntriesByType('resource').filter(entry => /cjrtnc|lwjgl\.js/.test(entry.name))
        .map(entry => ({ url: entry.name, transferSize: entry.transferSize, encodedBodySize: entry.encodedBodySize })),
    }));
    if (report.runtime.state?.state !== 'campaign') throw new Error('Profiling requires a real campaign');
    const loader = report.runtime.resources.find(entry => /\/loader\.js(?:\?|$)/.test(entry.url));
    if (loader) {
      const response = await context.request.get(loader.url);
      const bytes = await response.body();
      report.loader = { url: loader.url, status: response.status(), bytes: bytes.length,
        sha256: crypto.createHash('sha256').update(bytes).digest('hex'),
        lastModified: response.headers()['last-modified'] || null, etag: response.headers().etag || null };
    }
    // Let late ability effects settle before the fixed-size windows. Keep the
    // campaign and all its normal rendering intact throughout every sample.
    await page.waitForTimeout(5000);
    const windows = Number(process.env.STARSECTOR_DIAGNOSTIC_WINDOWS || 3);
    if (!Number.isInteger(windows) || windows < 1 || windows > 8) throw new Error('Invalid diagnostic window count');
    for (let i = 0; i < windows; i++) {
      const start = await page.evaluate(() => {
        window.__lwjglResetPresentationTimingWindow();
        return { at: performance.now(), swaps: window.__lwjglPresentationStats.swapCount };
      });
      await page.waitForFunction(swaps => window.__lwjglPresentationStats.swapCount >= swaps + 241,
        start.swaps, { timeout: 120000, polling: 250 });
      const result = await page.evaluate(() => {
        window.__lwjglRefreshPresentationStats();
        return { at: performance.now(), stats: { ...window.__lwjglPresentationStats },
          state: window.__STARSECTOR_RUNTIME_STATE__?.state };
      });
      if (result.state !== 'campaign' || result.stats.frameSampleCount !== 240) throw new Error('Invalid timing window');
      report.windows.push({ index: i, wallMs: result.at - start.at, swapDelta: result.stats.swapCount - start.swaps,
        sampleCount: result.stats.frameSampleCount, fps: result.stats.recentFps,
        meanMs: result.stats.recentFrameMs, p50Ms: result.stats.frameP50Ms, p95Ms: result.stats.frameP95Ms,
        p99Ms: result.stats.frameP99Ms, jitterP95Ms: result.stats.frameJitterP95Ms });
      console.log('[runner-timing]', JSON.stringify(report.windows.at(-1)));
    }
    if (process.env.STARSECTOR_COLOR_CACHE_AB === 'true') {
      // Eight counterbalanced windows in the SAME real, unpaused campaign and
      // browser. No screenshots, CPU profiling or resource fetches in a window.
      report.comparison = { design: 'ABBABAAB', baseline: 'uncached color snapshots',
        candidate: 'color snapshot shadow', blocks: [] };
      const order = [false, true, true, false, true, false, false, true];
      for (let i = 0; i < order.length; i++) {
        const enabled = order[i];
        const warmStart = await page.evaluate(enabled => {
          if (!window.__lwjglColorAttribCacheStats) throw new Error('Candidate color cache not loaded');
          window.__LWJGL_COLOR_ATTRIB_CACHE__ = enabled;
          return window.__lwjglPresentationStats.swapCount;
        }, enabled);
        await page.waitForFunction(start => window.__lwjglPresentationStats.swapCount >= start + 24,
          warmStart, { timeout: 30000, polling: 250 });
        const before = await page.evaluate(() => {
          window.__lwjglResetPresentationTimingWindow();
          return { swaps: window.__lwjglPresentationStats.swapCount, cache: {...window.__lwjglColorAttribCacheStats} };
        });
        await page.waitForFunction(start => window.__lwjglPresentationStats.swapCount >= start + 241,
          before.swaps, { timeout: 120000, polling: 250 });
        const after = await page.evaluate(() => {
          window.__lwjglRefreshPresentationStats();
          const s=window.__lwjglPresentationStats;
          return { state: window.__STARSECTOR_RUNTIME_STATE__?.state, enabled: window.__LWJGL_COLOR_ATTRIB_CACHE__,
            sampleCount:s.frameSampleCount, fps:s.recentFps, meanMs:s.recentFrameMs,
            p95Ms:s.frameP95Ms, p99Ms:s.frameP99Ms, jitterP95Ms:s.frameJitterP95Ms,
            cache:{...window.__lwjglColorAttribCacheStats} };
        });
        if (after.state!=='campaign' || after.sampleCount!==240 || after.enabled!==enabled) throw new Error('Invalid A/B window');
        const block={index:i,enabled,...after,readbackDelta:after.cache.readbacks-before.cache.readbacks,
          hitDelta:after.cache.hits-before.cache.hits};
        if(enabled ? block.hitDelta<=0 : block.readbackDelta<=0) throw new Error('A/B branch not exercised');
        report.comparison.blocks.push(block);
        console.log('[runner-color-ab]',JSON.stringify(block));
      }
      const median=values=>{const v=[...values].sort((a,b)=>a-b);return (v[(v.length-1)>>1]+v[v.length>>1])/2;};
      const metrics=['fps','meanMs','p95Ms','p99Ms','jitterP95Ms'];
      report.comparison.medians={baseline:{},candidate:{},changePercent:{}};
      for(const metric of metrics){
        const a=median(report.comparison.blocks.filter(b=>!b.enabled).map(b=>b[metric]));
        const b=median(report.comparison.blocks.filter(b=>b.enabled).map(b=>b[metric]));
        report.comparison.medians.baseline[metric]=a;
        report.comparison.medians.candidate[metric]=b;
        report.comparison.medians.changePercent[metric]=a?(b-a)*100/a:null;
      }
      await page.evaluate(()=>{window.__LWJGL_COLOR_ATTRIB_CACHE__=true;});
    }
    if (process.env.STARSECTOR_EXTENDED_CACHE_AB === 'true') {
      report.extendedComparison = await require('./cheerpj-paired-experiment').runPairedExperiment(page, out);
    }
    if (process.env.STARSECTOR_PACING_SCREEN === 'true') {
      report.pacingScreen = await require('./cheerpj-pacing-screen').runPacingScreen(page, out);
    }
    if (process.env.STARSECTOR_RAF_PAIRED === 'true') {
      report.rafPaired = await require('./cheerpj-raf-paired').runRafPairedExperiment(page, out);
    }
    if (process.env.STARSECTOR_DEPTH_PAIRED === 'true') {
      report.depthPaired = await require('./cheerpj-depth-paired').runDepthPairedExperiment(page, out);
    }
    report.profileFlags = await captureExperimentFlags(page);
    const session = await context.newCDPSession(page);
    try {
      await session.send('Profiler.enable');
      await session.send('Profiler.setSamplingInterval', { interval: 1000 });
      await session.send('Profiler.start');
      await page.waitForTimeout(15000);
      const { profile } = await session.send('Profiler.stop');
      fs.writeFileSync(path.join(out, 'campaign.cpuprofile'), JSON.stringify(profile));
      report.profile = summarizeProfile(profile);
    } finally { await session.detach(); }
    if (process.env.STARSECTOR_STALL_ATTRIBUTION === 'true') {
      report.stallAttribution = await require('./cheerpj-stall-attribution').collectStallAttribution(page, context, out, Number(process.env.STARSECTOR_STALL_DURATION_MS || 12000));
    }
    report.ok = true;
  } catch (error) { report.ok = false; report.error = error.stack || String(error); }
  finally { fs.writeFileSync(path.join(out, 'report.json'), JSON.stringify(report, null, 2)); }
  if (!report.ok) throw new Error(report.error);
  return report;
}
module.exports = { collectDiagnostics, summarizeProfile };
