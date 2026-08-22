#!/usr/bin/env node
'use strict';
const fs = require('fs');
const assert = require('assert');
const { chromium } = require('playwright');

(async () => {
  let html = fs.readFileSync('launch.html', 'utf8');
  html = html.replace(/<script\s+src=['"]https:\/\/cjrtnc\.leaningtech\.com\/4\.3\/loader\.js['"]><\/script>/i, '');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1366, height: 768 } });
  const page = await context.newPage();
  await page.route('http://resolution.test/**', route => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/launch.html')) return route.fulfill({ status: 200, contentType: 'text/html', body: html });
    return route.fulfill({ status: 404, body: '' });
  });
  await page.goto('http://resolution.test/launch.html?manual=1');
  assert.strictEqual(await page.locator('#launcher-settings').isHidden(), true, 'settings panel starts hidden');
  assert.strictEqual(await page.locator('#startBtn').isEnabled(), true, 'manual launcher waits for user');
  await page.locator('#settingsBtn').click();
  assert.strictEqual(await page.locator('#launcher-settings').isVisible(), true, 'settings panel opens');
  await page.locator('#resolutionPreset').selectOption('1920x1080');
  const fullHd = await page.evaluate(() => ({
    w: window.__STARSECTOR_RENDER_WIDTH__, h: window.__STARSECTOR_RENDER_HEIGHT__,
    stored: JSON.parse(localStorage.getItem('starsectorquick.renderResolution.v1')),
    box: { w: document.getElementById('game-container').style.width, h: document.getElementById('game-container').style.height },
    status: document.getElementById('resolutionStatus').textContent,
  }));
  assert.deepStrictEqual([fullHd.w, fullHd.h], [1920, 1080]);
  assert.deepStrictEqual(fullHd.stored, { width: 1920, height: 1080 });
  assert.match(fullHd.status, /1920\s*×\s*1080/);
  assert.ok(parseInt(fullHd.box.w, 10) <= 1322, `fit width ${fullHd.box.w}`);
  assert.ok(parseInt(fullHd.box.w, 10) > 1000, `fit width too small ${fullHd.box.w}`);

  await page.locator('#resolutionPreset').selectOption('custom');
  await page.locator('#resolutionWidth').fill('1500');
  await page.locator('#resolutionHeight').fill('844');
  await page.locator('#applyResolutionBtn').click();
  const custom = await page.evaluate(() => ({
    w: window.__STARSECTOR_RENDER_WIDTH__, h: window.__STARSECTOR_RENDER_HEIGHT__,
    stored: JSON.parse(localStorage.getItem('starsectorquick.renderResolution.v1')),
    preset: document.getElementById('resolutionPreset').value,
  }));
  assert.deepStrictEqual([custom.w, custom.h, custom.preset], [1500, 844, 'custom']);
  assert.deepStrictEqual(custom.stored, { width: 1500, height: 844 });

  await page.reload();
  await page.waitForSelector('#settingsBtn');
  const persisted = await page.evaluate(() => [window.__STARSECTOR_RENDER_WIDTH__, window.__STARSECTOR_RENDER_HEIGHT__, document.getElementById('resolutionPreset').value]);
  assert.deepStrictEqual(persisted, [1500, 844, 'custom'], 'custom resolution persists');

  await page.goto('http://resolution.test/launch.html?manual=1&resolution=1920x1080');
  const queryOverride = await page.evaluate(() => [window.__STARSECTOR_RENDER_WIDTH__, window.__STARSECTOR_RENDER_HEIGHT__, document.getElementById('resolutionPreset').value]);
  assert.deepStrictEqual(queryOverride, [1920, 1080, '1920x1080'], 'query override selects Full HD');

  const canvas = await page.evaluate(() => {
    const c = ensureLwjglCanvas(document.getElementById('game-container'), 1920, 1080);
    const rect = c.getBoundingClientRect();
    return { width: c.width, height: c.height, cssWidth: rect.width, cssHeight: rect.height, scale: Number(document.getElementById('game-container').dataset.renderScale) };
  });
  assert.deepStrictEqual([canvas.width, canvas.height], [1920, 1080]);
  assert.ok(canvas.cssWidth < canvas.width && canvas.scale > 0 && canvas.scale < 1, JSON.stringify(canvas));
  assert.ok(Math.abs((canvas.cssWidth / canvas.cssHeight) - (16 / 9)) < 0.01, JSON.stringify(canvas));

  await browser.close();
  console.log(`verify-launch-resolution-ui: OK 1920x1080 internal, fitted CSS ${Math.round(canvas.cssWidth)}x${Math.round(canvas.cssHeight)}, custom persistence 1500x844`);
})().catch(err => { console.error(err); process.exit(1); });
