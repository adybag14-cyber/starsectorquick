#!/usr/bin/env node
'use strict';
const fs = require('fs');
const assert = require('assert');
const { chromium } = require('playwright');

(async () => {
  const liveUrl = String(process.env.STARSECTOR_RESOLUTION_TEST_URL || '').trim();
  const liveRootMode = String(process.env.STARSECTOR_LIVE_ROOT_MODE || 'launch').trim();
  const testUrl = liveUrl || 'http://resolution.test/launch.html';
  const indexHtml = fs.readFileSync('index.html', 'utf8');
  assert.match(indexHtml, /launch\.html\?manual=1/g, 'Pages root must open the configurable manual launcher');
  assert.doesNotMatch(indexHtml, /launch\.html\?autostart=1/, 'Pages root must not disable settings through immediate autostart');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1366, height: 768 } });
  const page = await context.newPage();
  if (!liveUrl) {
    let html = fs.readFileSync('launch.html', 'utf8');
    html = html.replace(/<script\s+src=['"]https:\/\/cjrtnc\.leaningtech\.com\/4\.3\/loader\.js['"]><\/script>/i, '');
    await page.route('http://resolution.test/**', route => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith('/launch.html')) return route.fulfill({ status: 200, contentType: 'text/html', body: html });
      return route.fulfill({ status: 404, body: '' });
    });
  } else {
    const rootUrl = new URL('./', testUrl).toString();
    await page.goto(`${rootUrl}?resolutionRoute=${Date.now()}`, { waitUntil: 'domcontentloaded' });
    if (liveRootMode === 'gwt') {
      await page.waitForURL(url => url.pathname.endsWith('/gwt/'));
      assert.strictEqual(await page.locator('#resolutionButton').isVisible(), true, 'Pages root exposes the GWT resolution button');
    } else {
      await page.waitForURL(url => url.pathname.endsWith('/launch.html') && url.searchParams.get('manual') === '1');
      assert.strictEqual(await page.locator('#settingsBtn').isVisible(), true, 'Pages root exposes the resolution button');
      assert.strictEqual(await page.locator('#settingsBtn').isEnabled(), true, 'Pages root keeps resolution enabled before launch');
    }
  }
  await page.goto(`${testUrl}?manual=1`, { waitUntil: 'domcontentloaded' });
  assert.strictEqual(await page.locator('#launcher-settings').isHidden(), true, 'settings panel starts hidden');
  assert.strictEqual(await page.locator('#startBtn').isEnabled(), true, 'manual launcher waits for user');
  assert.strictEqual(await page.locator('#settingsBtn').isVisible(), true, 'resolution button is immediately visible');
  assert.match(await page.locator('#settingsBtn').innerText(), /RESOLUTION.*1024\s*[×x]\s*768/i);
  assert.match(await page.locator('#settingsBtn').getAttribute('aria-label'), /1024 by 768/i);
  await page.locator('#settingsBtn').click();
  assert.strictEqual(await page.locator('#launcher-settings').isVisible(), true, 'settings panel opens');
  assert.strictEqual(await page.locator('#resolutionPreset option').first().getAttribute('value'), '1024x768', '1024x768 is the lowest named UI-safe preset');
  assert.strictEqual(await page.locator('#resolutionPreset option[value="640x360"]').count(), 0, 'rejected sub-1024 performance preset stays unpublished');
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
  assert.match(await page.locator('#settingsBtn').innerText(), /1920\s*[×x]\s*1080/);
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

  await page.goto(`${testUrl}?manual=1&resolution=1920x1080`, { waitUntil: 'domcontentloaded' });
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
  console.log(`verify-launch-resolution-ui: OK target=${testUrl} 1920x1080 internal, fitted CSS ${Math.round(canvas.cssWidth)}x${Math.round(canvas.cssHeight)}, custom persistence 1500x844`);
})().catch(err => { console.error(err); process.exit(1); });
