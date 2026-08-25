#!/usr/bin/env node
'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const { PNG } = require('pngjs');

const target = process.env.STARSECTOR_GWT_TEST_URL || 'http://127.0.0.1:8877/';
const timeout = Number(process.env.STARSECTOR_GWT_TEST_TIMEOUT_MS || 240000);
const outputDir = process.env.STARSECTOR_GWT_TEST_OUTPUT_DIR || 'test_output/gwt-pages';
const profile = process.env.STARSECTOR_GWT_RENDERER_PROFILE || 'portable';
const profileLimits = {
  portable: { minFps: 35, maxP95Ms: 40, maxP99Ms: 55, maxJitterMs: 6 },
  // GitHub-hosted Chromium renders WebGL through SwiftShader. This profile is
  // a liveness/frame-progress gate; real 60 FPS authority remains a GPU-backed
  // browser run using the hardware profile below.
  'software-ci': { minFps: 6, maxP95Ms: 180, maxP99Ms: 260, maxJitterMs: 35 },
  hardware: { minFps: 58, maxP95Ms: 22, maxP99Ms: 30, maxJitterMs: 3 },
};
assert.ok(profileLimits[profile], `Unknown GWT renderer profile: ${profile}`);
const limits = {
  minFps: Number(process.env.STARSECTOR_GWT_MIN_FPS || profileLimits[profile].minFps),
  maxP95Ms: Number(process.env.STARSECTOR_GWT_MAX_P95_MS || profileLimits[profile].maxP95Ms),
  maxP99Ms: Number(process.env.STARSECTOR_GWT_MAX_P99_MS || profileLimits[profile].maxP99Ms),
  maxJitterMs: Number(process.env.STARSECTOR_GWT_MAX_JITTER_MS || profileLimits[profile].maxJitterMs),
};

function writeEvidence(evidence) {
  fs.writeFileSync(path.join(outputDir, 'result.json'), JSON.stringify(evidence, null, 2));
}

async function waitForRuntime(page) {
  await page.waitForFunction(() => {
    const gate = window.__gwtGate;
    const titleReady = gate && gate.logs.some(row => row.text.includes('[gwt-stage] state-enter:Title Screen State'));
    const texturesReady = gate && gate.logs.some(row => row.text.includes('[gwt-stage] texture-upload-ready') && row.text.includes('failed=0'));
    return titleReady && texturesReady && window.__starsectorFrameStats && window.__starsectorFrameStats.samples >= 600;
  }, null, { timeout });
  await page.waitForTimeout(11000);
}

async function waitForStage(page, stage) {
  await page.waitForFunction(expected => {
    const gate = window.__gwtGate;
    return gate && gate.logs.some(row => row.text.includes(`[gwt-stage] state-enter:${expected}`));
  }, stage, { timeout });
}

function verifyStats(stats, label) {
  assert.ok(stats && stats.samples >= 600, `${label} did not retain a 600-frame timing window`);
  assert.ok(stats.fps >= limits.minFps, `${label} FPS below ${profile} gate: ${JSON.stringify({ stats, limits })}`);
  assert.ok(stats.p95Ms <= limits.maxP95Ms, `${label} p95 above ${profile} gate: ${JSON.stringify({ stats, limits })}`);
  assert.ok(stats.p99Ms <= limits.maxP99Ms, `${label} p99 above ${profile} gate: ${JSON.stringify({ stats, limits })}`);
  assert.ok(stats.jitterMs <= limits.maxJitterMs, `${label} jitter above ${profile} gate: ${JSON.stringify({ stats, limits })}`);
}

async function readRenderer(page) {
  return page.evaluate(() => {
    const canvas = document.getElementById('gameCanvas');
    const gl = canvas && (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
    if (!gl) return null;
    const debug = gl.getExtension('WEBGL_debug_renderer_info');
    return {
      vendor: String(gl.getParameter(gl.VENDOR) || ''),
      renderer: String(gl.getParameter(gl.RENDERER) || ''),
      unmaskedVendor: debug ? String(gl.getParameter(debug.UNMASKED_VENDOR_WEBGL) || '') : '',
      unmaskedRenderer: debug ? String(gl.getParameter(debug.UNMASKED_RENDERER_WEBGL) || '') : '',
    };
  });
}

function verifyRenderer(renderer) {
  assert.ok(renderer, 'GWT WebGL renderer information is unavailable');
  if (profile === 'software-ci') {
    const description = Object.values(renderer).join(' ');
    assert.match(description, /swiftshader|llvmpipe|software|subzero/i,
      `software-ci profile used on a non-software renderer: ${JSON.stringify(renderer)}`);
  }
}

async function clickCanvas(page, xRatio, yRatio) {
  const canvas = page.locator('#gameCanvas');
  const box = await canvas.boundingBox();
  assert.ok(box && box.width > 0 && box.height > 0, 'GWT canvas has no clickable bounds');
  await page.mouse.click(box.x + box.width * xRatio, box.y + box.height * yRatio);
}

async function analyzeCanvas(page, filename) {
  const screenshotPath = path.join(outputDir, filename);
  await page.locator('#gameCanvas').screenshot({ path: screenshotPath });
  const png = PNG.sync.read(fs.readFileSync(screenshotPath));
  let samples = 0;
  let nearWhite = 0;
  let dark = 0;
  let hudSamples = 0;
  let hudCyan = 0;
  for (let y = 0; y < png.height; y += 4) {
    for (let x = 0; x < png.width; x += 4) {
      const offset = (png.width * y + x) << 2;
      const red = png.data[offset];
      const green = png.data[offset + 1];
      const blue = png.data[offset + 2];
      samples++;
      if (red >= 245 && green >= 245 && blue >= 245) nearWhite++;
      if (red <= 70 && green <= 70 && blue <= 70) dark++;
      if (y >= png.height * 0.65) {
        hudSamples++;
        if (green > red * 1.15 && blue > red * 1.15 && green + blue > 70) hudCyan++;
      }
    }
  }
  return {
    width: png.width,
    height: png.height,
    samples,
    nearWhiteFraction: nearWhite / samples,
    darkFraction: dark / samples,
    hudCyanFraction: hudCyan / hudSamples,
  };
}

function verifyGalatiaBackground(pixels) {
  assert.ok(pixels.nearWhiteFraction < 0.55,
    `Galatia framebuffer is mostly white: ${JSON.stringify(pixels)}`);
  assert.ok(pixels.darkFraction > 0.2,
    `Galatia framebuffer is missing the dark space field: ${JSON.stringify(pixels)}`);
  assert.ok(pixels.hudCyanFraction > 0.04,
    `Galatia framebuffer is missing the playable campaign HUD: ${JSON.stringify(pixels)}`);
}

async function dismissCampaignBriefing(page) {
  await page.waitForTimeout(profile === 'software-ci' ? 15000 : 7000);
  for (let step = 0; step < 3; step++) {
    const pixels = await analyzeCanvas(page, `campaign-briefing-${step}.png`);
    if (pixels.hudCyanFraction > 0.04) return;
    await clickCanvas(page, 0.196, 0.735); // Continue or Finish
    await page.waitForTimeout(2500);
  }
  const pixels = await analyzeCanvas(page, 'campaign-briefing-unresolved.png');
  assert.ok(pixels.hudCyanFraction > 0.04,
    `GWT tutorial briefing did not return to the playable HUD: ${JSON.stringify(pixels)}`);
}

async function startGalatiaTutorial(page) {
  // Coordinates are normalized to the rendered canvas, so the path remains
  // stable when the browser scales 1280x720 to fit the viewport.
  await page.keyboard.press('Enter'); // dismiss the first-run title tip, if present
  await page.waitForTimeout(750);
  await clickCanvas(page, 0.81, 0.422); // New Game
  await page.waitForTimeout(2500);
  await clickCanvas(page, 0.72, 0.24); // Generate name
  await page.waitForTimeout(500);
  await clickCanvas(page, 0.18, 0.735); // Continue
  await page.waitForTimeout(2500);
  await clickCanvas(page, 0.395, 0.778); // Scavenger start
  await page.waitForTimeout(2500);
  await clickCanvas(page, 0.416, 0.735); // Kite + officer
  await page.waitForTimeout(2500);
  await clickCanvas(page, 0.196, 0.735); // Normal difficulty
  await page.waitForTimeout(2500);
  await clickCanvas(page, 0.211, 0.735); // Start with tutorial
  await waitForStage(page, 'Campaign State');
  await page.screenshot({ path: path.join(outputDir, 'campaign-state-entered.png'), fullPage: true });
  await dismissCampaignBriefing(page);
  await page.waitForTimeout(profile === 'software-ci' ? 30000 : 11000);
}

(async () => {
  fs.mkdirSync(outputDir, { recursive: true });
  let browser;
  const evidence = { target, profile, limits };
  try {
    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
    await context.clearCookies();
    const page = await context.newPage();
    const consoleRows = [];
    evidence.consoleRows = consoleRows;
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
    evidence.renderer = await readRenderer(page);
    verifyRenderer(evidence.renderer);
    evidence.baseline = await page.evaluate(() => ({ ...window.__starsectorFrameStats }));
    await page.screenshot({ path: path.join(outputDir, 'title-1024x768.png'), fullPage: true });
    verifyStats(evidence.baseline, 'GWT title 1024x768');

    await page.locator('#resolutionButton').click();
    await page.locator('#resolutionPreset').selectOption('1280x720');
    await page.locator('#applyResolution').click();
    await page.waitForFunction(() => {
      const canvas = document.getElementById('gameCanvas');
      return canvas && canvas.width === 1280 && canvas.height === 720;
    }, null, { timeout });
    await waitForRuntime(page);
    assert.match(await page.locator('#resolutionButton').innerText(), /1280\s*[×x]\s*720/);
    evidence.resized = await page.evaluate(() => ({ ...window.__starsectorFrameStats }));
    await page.screenshot({ path: path.join(outputDir, 'title-1280x720.png'), fullPage: true });
    verifyStats(evidence.resized, 'GWT title 1280x720');

    await startGalatiaTutorial(page);
    evidence.campaign = await page.evaluate(() => ({ ...window.__starsectorFrameStats }));
    evidence.galatiaPixels = await analyzeCanvas(page, 'galatia-campaign-1280x720.png');
    evidence.stateEntries = await page.evaluate(() => window.__gwtGate.logs
      .filter(row => row.text.includes('[gwt-stage] state-enter:'))
      .map(row => row.text));
    verifyStats(evidence.campaign, 'GWT Galatia campaign 1280x720');
    verifyGalatiaBackground(evidence.galatiaPixels);
    assert.ok(evidence.stateEntries.some(text => text.includes('Campaign State')),
      `GWT campaign state was not entered: ${JSON.stringify(evidence.stateEntries)}`);

    const failures = consoleRows.filter(row =>
      row.type === 'pageerror' || row.text.includes('GWT frame failure:') ||
      row.text.includes('GWT asset preload failed:') || row.text.includes('failed=1'));
    evidence.failures = failures;
    assert.deepStrictEqual(failures, [], `GWT runtime errors: ${JSON.stringify(failures, null, 2)}`);
    writeEvidence(evidence);
    console.log(
      `verify-gwt-pages-runtime: OK target=${target} profile=${profile} ` +
      `baseline=${evidence.baseline.fps.toFixed(2)}fps resized=${evidence.resized.fps.toFixed(2)}fps ` +
      `campaign=${evidence.campaign.fps.toFixed(2)}fps white=${evidence.galatiaPixels.nearWhiteFraction.toFixed(4)}`,
    );
  } catch (error) {
    evidence.error = error && (error.stack || error.message) || String(error);
    writeEvidence(evidence);
    throw error;
  } finally {
    if (browser) await browser.close();
  }
})().catch(error => {
  console.error(error);
  process.exit(1);
});
