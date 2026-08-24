#!/usr/bin/env node
'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const target = process.env.STARSECTOR_GWT_TEST_URL || 'http://127.0.0.1:8877/';
const timeout = Number(process.env.STARSECTOR_GWT_TEST_TIMEOUT_MS || 240000);
const outputDir = process.env.STARSECTOR_GWT_TEST_OUTPUT_DIR || 'test_output/gwt-pages';
const limits = {
  minFps: Number(process.env.STARSECTOR_GWT_MIN_FPS || 35),
  maxP95Ms: Number(process.env.STARSECTOR_GWT_MAX_P95_MS || 40),
  maxP99Ms: Number(process.env.STARSECTOR_GWT_MAX_P99_MS || 55),
  maxJitterMs: Number(process.env.STARSECTOR_GWT_MAX_JITTER_MS || 6),
};

async function waitForRuntime(page) {
  await page.waitForFunction(() => {
    const gate = window.__gwtGate;
    const titleReady = gate && gate.logs.some(row => row.text.includes('[gwt-stage] state-enter:Title Screen State'));
    const texturesReady = gate && gate.logs.some(row => row.text.includes('[gwt-stage] texture-upload-ready') && row.text.includes('failed=0'));
    return titleReady && texturesReady && window.__starsectorFrameStats && window.__starsectorFrameStats.samples >= 600;
  }, null, { timeout });
  await page.waitForTimeout(11000);
}

function verifyStats(stats) {
  assert.ok(stats.fps >= limits.minFps, `GWT FPS below deployment gate: ${JSON.stringify({ stats, limits })}`);
  assert.ok(stats.p95Ms <= limits.maxP95Ms, `GWT p95 above deployment gate: ${JSON.stringify({ stats, limits })}`);
  assert.ok(stats.p99Ms <= limits.maxP99Ms, `GWT p99 above deployment gate: ${JSON.stringify({ stats, limits })}`);
  assert.ok(stats.jitterMs <= limits.maxJitterMs, `GWT jitter above deployment gate: ${JSON.stringify({ stats, limits })}`);
}

(async () => {
  fs.mkdirSync(outputDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
  await context.clearCookies();
  const page = await context.newPage();
  const consoleRows = [];
  page.on('console', message => consoleRows.push({ type: message.type(), text: message.text() }));
  page.on('pageerror', error => consoleRows.push({ type: 'pageerror', text: error.stack || error.message }));

  await page.goto(`${target}${target.includes('?') ? '&' : '?'}verify=${Date.now()}`, {
    waitUntil: 'domcontentloaded', timeout,
  });
  await waitForRuntime(page);
  assert.strictEqual(await page.locator('#resolutionButton').isVisible(), true, 'GWT resolution button is hidden');
  assert.deepStrictEqual(
    [await page.locator('#gameCanvas').getAttribute('width'), await page.locator('#gameCanvas').getAttribute('height')],
    ['1024', '768'],
  );
  const baseline = await page.evaluate(() => ({ ...window.__starsectorFrameStats }));
  verifyStats(baseline);
  await page.screenshot({ path: path.join(outputDir, 'title-1024x768.png'), fullPage: true });

  await page.locator('#resolutionButton').click();
  await page.locator('#resolutionPreset').selectOption('1280x720');
  await page.locator('#applyResolution').click();
  await page.waitForFunction(() => {
    const canvas = document.getElementById('gameCanvas');
    return canvas && canvas.width === 1280 && canvas.height === 720;
  }, null, { timeout });
  await waitForRuntime(page);
  assert.match(await page.locator('#resolutionButton').innerText(), /1280\s*[×x]\s*720/);
  const resized = await page.evaluate(() => ({ ...window.__starsectorFrameStats }));
  verifyStats(resized);
  await page.screenshot({ path: path.join(outputDir, 'title-1280x720.png'), fullPage: true });

  const failures = consoleRows.filter(row =>
    row.type === 'pageerror' || row.text.includes('GWT frame failure:') ||
    row.text.includes('GWT asset preload failed:') || row.text.includes('failed=1'));
  assert.deepStrictEqual(failures, [], `GWT runtime errors: ${JSON.stringify(failures, null, 2)}`);
  const evidence = { target, limits, baseline, resized, consoleRows };
  fs.writeFileSync(path.join(outputDir, 'result.json'), JSON.stringify(evidence, null, 2));
  await browser.close();
  console.log(`verify-gwt-pages-runtime: OK target=${target} baseline=${baseline.fps.toFixed(2)}fps resized=${resized.fps.toFixed(2)}fps`);
})().catch(error => {
  console.error(error);
  process.exit(1);
});
