const fs = require('fs');
const crypto = require('crypto');
const { chromium } = require('playwright');
const { PNG } = require('pngjs');

const baseUrl = process.env.STARSECTOR_TEST_URL || 'http://127.0.0.1:8000/launch.html';
const timeoutMs = Number(process.env.STARSECTOR_TEST_TIMEOUT_MS || 360000);
const outputDir = process.env.STARSECTOR_TEST_OUTPUT_DIR || 'test_output/campaign-render';
const expectedState = String(process.env.STARSECTOR_EXPECT_STATE || 'campaign').toLowerCase();
const settleMs = Number(process.env.STARSECTOR_FRAME_SETTLE_MS || 15000);
const configOverrides = process.env.STARSECTOR_WINDOW_CONFIG
  ? JSON.parse(process.env.STARSECTOR_WINDOW_CONFIG)
  : {};
fs.mkdirSync(outputDir, { recursive: true });

function pixelStats(buffer) {
  const png = PNG.sync.read(buffer);
  let nonBlack = 0;
  let opaque = 0;
  let sum = 0;
  let sumSq = 0;
  const pixels = png.width * png.height;
  for (let i = 0; i < png.data.length; i += 4) {
    const r = png.data[i];
    const g = png.data[i + 1];
    const b = png.data[i + 2];
    const a = png.data[i + 3];
    const y = (r + g + b) / 3;
    if (a > 0) opaque++;
    if (r > 8 || g > 8 || b > 8) nonBlack++;
    sum += y;
    sumSq += y * y;
  }
  const mean = sum / pixels;
  const variance = Math.max(0, sumSq / pixels - mean * mean);
  return {
    width: png.width,
    height: png.height,
    nonBlackPixels: nonBlack,
    nonBlackRatio: nonBlack / pixels,
    opaqueRatio: opaque / pixels,
    meanLuma: mean,
    variance,
    sha256: crypto.createHash('sha256').update(buffer).digest('hex')
  };
}

(async () => {
  const logs = [];
  const errors = [];
  const httpErrors = [];
  let campaignSeenAt = 0;
  let titleSeenAt = 0;
  let fatalSeenAt = 0;
  let updateMax = 0;
  let swapMax = 0;
  const browser = await chromium.launch({ headless: true, args: ['--enable-unsafe-swiftshader'] });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1180 }, serviceWorkers: 'allow' });
  const page = await context.newPage();

  const defaultConfig = {
    __STARSECTOR_AUTO_CAMPAIGN__: true,
    __STARSECTOR_AUTO_CAMPAIGN_MODE__: 'new_direct',
    __STARSECTOR_DIRECT_LAUNCH__: true,
    __STARSECTOR_AUTO_VISIT_COLONY__: false,
    __STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__: 'small',
    __STARSECTOR_AUTO_CAMPAIGN_TIMEOUT_MS__: 900000,
    __STARSECTOR_AUTO_CAMPAIGN_DIRECT_ATTEMPT_TIMEOUT_MS__: 45000,
    __STARSECTOR_FORCE_CHEERPJ_STORAGE_RESET__: true,
    __LWJGL_FIRST_LOG_LIMIT__: 512
  };
  const windowConfig = { ...defaultConfig, ...configOverrides };
  await page.addInitScript(config => {
    for (const [key, value] of Object.entries(config)) window[key] = value;
  }, windowConfig);

  page.on('console', message => {
    const text = message.text();
    logs.push(`[${message.type()}] ${text}`);
    const update = text.match(/Bridge Display\.update(?:\([^)]*\))? count=(\d+)/i);
    if (update) updateMax = Math.max(updateMax, Number(update[1]));
    const swap = text.match(/Bridge Display\.swapBuffers count=(\d+)/i);
    if (swap) swapMax = Math.max(swapMax, Number(swap[1]));
    if (/watcher state=Campaign State|reached Campaign State/i.test(text) && !campaignSeenAt) {
      campaignSeenAt = Date.now();
    }
    if (/watcher state=Title Screen State|Main loop inTitle Screen State/i.test(text) && !titleSeenAt) {
      titleSeenAt = Date.now();
    }
    if (/Fatal:|Exception in thread|auto campaign aborting|exhausted all new-game/i.test(text) && !fatalSeenAt) {
      fatalSeenAt = Date.now();
    }
  });
  page.on('pageerror', error => errors.push(String(error && (error.stack || error.message) || error)));
  page.on('requestfailed', request => {
    const failure = request.failure();
    logs.push(`[requestfailed] ${request.url()} :: ${failure ? failure.errorText : 'unknown'}`);
  });
  page.on('response', response => {
    if (response.status() >= 400) {
      const item = `${response.status()} ${response.url()}`;
      httpErrors.push(item);
      logs.push(`[http] ${item}`);
    }
  });

  const target = `${baseUrl}?autostart=1&ci=${Date.now()}`;
  console.log(`Opening ${target}`);
  console.log(`Expected state=${expectedState} config=${JSON.stringify(windowConfig)}`);
  await page.goto(target, { waitUntil: 'domcontentloaded', timeout: 60000 });

  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline && errors.length === 0 && !fatalSeenAt) {
    const state = await page.evaluate(() => document.body.dataset.runtimeState || '').catch(() => '');
    if (['main-returned', 'failed', 'fatal'].includes(state)) break;
    if (expectedState === 'campaign' && campaignSeenAt) break;
    if (expectedState === 'title' && titleSeenAt && Date.now() - titleSeenAt >= settleMs) break;
    await page.waitForTimeout(1000);
  }

  const game = page.locator('#game-container');
  const first = await game.screenshot({ path: `${outputDir}/frame-first.png` });
  await page.waitForTimeout(settleMs);
  const second = await game.screenshot({ path: `${outputDir}/frame-second.png` });

  const state = await page.evaluate(() => ({
    runtime: window.__STARSECTOR_RUNTIME_STATE__ || null,
    bodyState: document.body.dataset.runtimeState || '',
    bodyDetail: document.body.dataset.runtimeDetail || '',
    button: document.getElementById('startBtn')?.textContent || '',
    nativeStats: window.__lwjglNativeStats || null,
    canvases: [...document.querySelectorAll('canvas')].map(c => ({
      width: c.width,
      height: c.height,
      clientWidth: c.clientWidth,
      clientHeight: c.clientHeight
    }))
  }));

  const firstStats = pixelStats(first);
  const secondStats = pixelStats(second);
  const frameChanged = firstStats.sha256 !== secondStats.sha256;
  const rendered = secondStats.nonBlackRatio > 0.01 && secondStats.variance > 2;
  const campaign = Boolean(campaignSeenAt) || state.bodyState === 'campaign';
  const title = Boolean(titleSeenAt);
  const progressing = updateMax >= 10 || swapMax >= 10 || frameChanged;
  const reachedExpected = expectedState === 'title' ? title : campaign;
  const ok = reachedExpected && rendered && progressing && errors.length === 0 && !fatalSeenAt
    && !['main-returned', 'failed', 'fatal'].includes(state.bodyState);

  const result = {
    ok,
    target,
    expectedState,
    windowConfig,
    campaign,
    campaignSeenAt,
    title,
    titleSeenAt,
    fatalSeenAt,
    rendered,
    progressing,
    frameChanged,
    updateMax,
    swapMax,
    firstStats,
    secondStats,
    errors,
    httpErrors: [...new Set(httpErrors)],
    state,
    logTail: logs.slice(-500)
  };
  fs.writeFileSync(`${outputDir}/result.json`, JSON.stringify(result, null, 2));
  fs.writeFileSync(`${outputDir}/browser.log`, logs.join('\n'));
  console.log(JSON.stringify(result, null, 2));
  await browser.close();
  process.exit(ok ? 0 : 1);
})().catch(error => {
  console.error(error && (error.stack || error.message) || error);
  process.exit(2);
});
