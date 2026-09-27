#!/usr/bin/env node
'use strict';

// Browser-only regression tests: no Java runtime, game assets, or saved games.
const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');

(async () => {
  const root = path.resolve(__dirname, '..');
  const output = path.resolve(process.env.STARSECTOR_LAYOUT_OUTPUT_DIR || 'test_output/launcher-layout');
  fs.mkdirSync(output, { recursive: true });
  const results = [];
  const check = async (name, fn) => {
    try { await fn(); results.push({ name, passed: true }); }
    catch (err) { results.push({ name, passed: false, error: err.message }); }
  };
  const index = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
  const legacyIndex = fs.readFileSync(path.join(root, 'gwt-runtime/root-index.html'), 'utf8');
  const workflow = fs.readFileSync(path.join(root, '.github/workflows/deploy-pages.yml'), 'utf8');
  await check('canonical-and-legacy-entrypoints-use-cheerpj', () => {
    assert.equal(legacyIndex, index, 'legacy packaging entrypoint must agree with canonical index');
    assert.match(index, /url=\.\/launch\.html\?manual=1/i);
    assert.match(index, /replace\(new URL\('\.\/launch\.html\?manual=1'/);
    assert.match(index, /href="\.\/gwt\/"/);
    assert.doesNotMatch(index, /autostart=1/);
  });
  await check('publisher-does-not-override-cheerpj-default', () => {
    assert.doesNotMatch(workflow, /cp\s+gwt-runtime\/root-index\.html\s+\.pages-worktree\/index\.html/);
    assert.doesNotMatch(workflow, /STARSECTOR_LIVE_ROOT_MODE='gwt'/);
  });
  const html = fs.readFileSync(path.join(root, 'launch.html'), 'utf8')
    .replace(/<script\s+src=['"]https:\/\/cjrtnc\.leaningtech\.com\/4\.3\/loader\.js['"]><\/script>/i, '');
  const browser = await chromium.launch({ headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
    const page = await context.newPage();
    const pageErrors = [];
    page.on('pageerror', err => pageErrors.push(err.message));
    await page.route('http://launcher.test/**', route => {
      const url = new URL(route.request().url());
      const body = url.pathname.endsWith('/launch.html') ? html : index;
      return route.fulfill({ status: 200, contentType: 'text/html', body });
    });
    await page.goto('http://launcher.test/starsectorquick/');
    await check('root-navigation-keeps-resolution-selectable', async () => {
      await page.waitForURL(url => url.pathname.endsWith('/launch.html') && url.searchParams.get('manual') === '1');
      assert.equal(await page.locator('#settingsBtn').isEnabled(), true);
      assert.equal(await page.locator('#startBtn').isEnabled(), true);
      assert.equal(await page.locator('a[href="./gwt/"]').count(), 1);
    });
    const settle = () => page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    const measure = () => page.evaluate(() => {
      const host = document.getElementById('game-container');
      const rect = host.getBoundingClientRect();
      const canvas = host.querySelector('#lwjglCanvas');
      const canvasRect = canvas?.getBoundingClientRect();
      return {
        viewport: document.documentElement.clientWidth,
        scrollWidth: document.documentElement.scrollWidth,
        x: rect.x, right: rect.right, width: rect.width, height: rect.height,
        contentWidth: host.clientWidth, contentHeight: host.clientHeight,
        renderWidth: window.__STARSECTOR_RENDER_WIDTH__, renderHeight: window.__STARSECTOR_RENDER_HEIGHT__,
        canvas: canvas && { width: canvas.width, height: canvas.height, cssWidth: canvasRect.width, cssHeight: canvasRect.height },
      };
    });
    const assertFit = box => {
      assert.ok(Math.abs(box.x - (box.viewport - box.right)) <= 2, `unequal margins: ${JSON.stringify(box)}`);
      assert.ok(box.x >= 0 && box.right <= box.viewport + 1, `game outside viewport: ${JSON.stringify(box)}`);
      assert.ok(box.scrollWidth <= box.viewport + 1, `horizontal page overflow: ${JSON.stringify(box)}`);
      assert.ok(Math.abs(box.contentHeight - box.contentWidth * box.renderHeight / box.renderWidth) <= 1.1,
        `display aspect ratio changed: ${JSON.stringify(box)}`);
      if (box.canvas) {
        assert.equal(box.canvas.width, box.renderWidth);
        assert.equal(box.canvas.height, box.renderHeight);
        assert.ok(Math.abs(box.canvas.cssWidth - box.contentWidth) <= 1);
        assert.ok(Math.abs(box.canvas.cssHeight - box.contentHeight) <= 1);
      }
    };
    await page.evaluate(() => applyRenderResolution(1600, 900, false));
    await check('1600x900-centered-at-screenshot-desktop-size', async () => assertFit(await measure()));
    await page.screenshot({ path: path.join(output, 'desktop-1600x900.png'), fullPage: true });
    await check('unlaunched-placeholder-refits-on-resize', async () => {
      await page.setViewportSize({ width: 800, height: 900 });
      await settle();
      assertFit(await measure());
    });
    await page.evaluate(() => {
      const c = ensureLwjglCanvas(document.getElementById('game-container'), 1600, 900);
      const ctx = c.getContext('2d');
      ctx.fillStyle = 'rgb(17, 101, 203)';
      ctx.fillRect(0, 0, 10, 10);
      window.__layoutCanvas = c;
    });
    await check('repeated-layout-does-not-clear-running-canvas', async () => {
      await page.evaluate(() => ensureLwjglCanvas(document.getElementById('game-container'), 1600, 900));
      const pixel = await page.evaluate(() => Array.from(window.__layoutCanvas.getContext('2d').getImageData(2, 2, 1, 1).data));
      assert.deepEqual(pixel, [17, 101, 203, 255]);
    });
    await check('browser-resize-preserves-canvas-identity-and-pixels', async () => {
      await page.evaluate(() => {
        const ctx = window.__layoutCanvas.getContext('2d');
        ctx.fillStyle = 'rgb(17, 101, 203)'; ctx.fillRect(0, 0, 10, 10);
      });
      await page.setViewportSize({ width: 1366, height: 900 });
      await settle();
      const state = await page.evaluate(() => ({
        same: document.getElementById('lwjglCanvas') === window.__layoutCanvas,
        pixel: Array.from(window.__layoutCanvas.getContext('2d').getImageData(2, 2, 1, 1).data),
      }));
      assert.equal(state.same, true);
      assert.deepEqual(state.pixel, [17, 101, 203, 255]);
      assertFit(await measure());
    });
    const measurements = [];
    for (const viewport of [320, 375, 640, 800, 1024, 1366, 1600, 1920, 2560]) {
      await page.setViewportSize({ width: viewport, height: 1080 });
      await settle();
      for (const [width, height] of [[1024, 768], [1280, 720], [1600, 900], [1920, 1080], [2560, 1440], [3840, 2160], [1500, 844]]) {
        await page.evaluate(([w, h]) => applyRenderResolution(w, h, false), [width, height]);
        const box = await measure(); measurements.push(box);
        await check(`centered-fit-${viewport}px-${width}x${height}`, () => assertFit(box));
      }
    }
    await check('pointer-geometry-remains-proportional', async () => {
      await page.setViewportSize({ width: 800, height: 900 });
      await settle();
      const point = await page.evaluate(() => {
        const c = document.getElementById('lwjglCanvas');
        const r = c.getBoundingClientRect();
        window.__layoutPoint = null;
        c.addEventListener('pointerdown', event => {
          window.__layoutPoint = [(event.clientX - r.left) * c.width / r.width,
            (event.clientY - r.top) * c.height / r.height];
        }, { once: true });
        return { x: r.left + r.width * .5, y: r.top + r.height * .5, w: c.width, h: c.height };
      });
      await page.mouse.click(point.x, point.y);
      const actual = await page.evaluate(() => window.__layoutPoint);
      assert.ok(actual && Math.abs(actual[0] - point.w / 2) < 3 && Math.abs(actual[1] - point.h / 2) < 3, JSON.stringify({ point, actual }));
    });
    await check('resolution-locked-during-startup', async () => {
      const values = await page.evaluate(() => {
        const before = [window.__STARSECTOR_RENDER_WIDTH__, window.__STARSECTOR_RENDER_HEIGHT__];
        window.__STARSECTOR_STARTING__ = true;
        const accepted = applyRenderResolution(640, 360);
        return { before, accepted, after: [window.__STARSECTOR_RENDER_WIDTH__, window.__STARSECTOR_RENDER_HEIGHT__] };
      });
      assert.equal(values.accepted, false); assert.deepEqual(values.after, values.before);
    });
    await check('no-page-script-errors', () => assert.deepEqual(pageErrors, []));
    fs.writeFileSync(path.join(output, 'measurements.json'), JSON.stringify(measurements, null, 2));
    await context.close();
  } finally { await browser.close(); }
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify({ checkedAt: new Date().toISOString(), results }, null, 2));
  for (const row of results) console.log(`${row.passed ? 'PASS' : 'FAIL'} ${row.name}${row.error ? ': ' + row.error : ''}`);
  console.log(`Launcher layout: ${results.filter(r => r.passed).length} passed, ${results.filter(r => !r.passed).length} failed`);
  process.exitCode = results.some(r => !r.passed) ? 1 : 0;
})().catch(error => { console.error(error); process.exitCode = 1; });
