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

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

function withTimeout(promise, ms, label) {
  let timer;
  return Promise.race([
    promise.finally(() => clearTimeout(timer)),
    new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error(`${label} timed out after ${ms}ms`)), ms);
    })
  ]);
}

function pixelStats(buffer) {
  if (!buffer) return null;
  const png = PNG.sync.read(buffer);
  let nonBlack = 0;
  let opaque = 0;
  let dark = 0;
  let midTone = 0;
  let colorful = 0;
  let centerBright = 0;
  let centerWarm = 0;
  let sum = 0;
  let sumSq = 0;
  const quantizedColors = new Set();
  const pixels = png.width * png.height;
  const centerX = png.width / 2;
  const centerY = png.height / 2;
  const centerRadius = Math.max(32, Math.floor(Math.min(png.width, png.height) / 16));
  for (let i = 0; i < png.data.length; i += 4) {
    const pixelIndex = i / 4;
    const x = pixelIndex % png.width;
    const yPos = Math.floor(pixelIndex / png.width);
    const r = png.data[i];
    const g = png.data[i + 1];
    const b = png.data[i + 2];
    const a = png.data[i + 3];
    const y = (r + g + b) / 3;
    const chroma = Math.max(r, g, b) - Math.min(r, g, b);
    if (a > 0) opaque++;
    if (r > 8 || g > 8 || b > 8) nonBlack++;
    if (y < 12) dark++;
    if (y >= 20 && y <= 220) midTone++;
    if (chroma > 20) colorful++;
    if (Math.abs(x - centerX) <= centerRadius && Math.abs(yPos - centerY) <= centerRadius) {
      if (Math.max(r, g, b) > 80) centerBright++;
      if (r > 45 && r > g * 1.15 && r > b * 1.3) centerWarm++;
    }
    quantizedColors.add(((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4));
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
    darkRatio: dark / pixels,
    midToneRatio: midTone / pixels,
    colorfulRatio: colorful / pixels,
    centerBrightPixels: centerBright,
    centerWarmPixels: centerWarm,
    quantizedColorCount: quantizedColors.size,
    meanLuma: mean,
    variance,
    sha256: crypto.createHash('sha256').update(buffer).digest('hex')
  };
}

(async () => {
  const logs = [];
  const errors = [];
  const httpErrors = [];
  const screenshotErrors = [];
  const graphicsErrors = [];
  const disallowedRecovery = [];
  let campaignSeenAt = 0;
  let titleSeenAt = 0;
  let fatalSeenAt = 0;
  let disallowedRecoverySeenAt = 0;
  let updateMax = 0;
  let swapMax = 0;
  let browser;

  const flushLogs = () => {
    try {
      fs.writeFileSync(`${outputDir}/browser-live.log`, logs.join('\n'));
    } catch (_) {}
  };

  browser = await chromium.launch({ headless: true, args: ['--enable-unsafe-swiftshader'] });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1180 }, serviceWorkers: 'allow' });
  const page = await context.newPage();
  page.setDefaultTimeout(10000);
  page.setDefaultNavigationTimeout(60000);

  const defaultConfig = {
    __STARSECTOR_AUTO_CAMPAIGN__: true,
    __STARSECTOR_AUTO_CAMPAIGN_MODE__: 'new_direct',
    __STARSECTOR_DIRECT_LAUNCH__: true,
    __STARSECTOR_AUTO_VISIT_COLONY__: false,
    __STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__: 'small',
    __STARSECTOR_AUTO_CAMPAIGN_TIMEOUT_MS__: 900000,
    __STARSECTOR_AUTO_CAMPAIGN_DIRECT_ATTEMPT_TIMEOUT_MS__: 45000,
    __STARSECTOR_FORCE_CHEERPJ_STORAGE_RESET__: true,
    __STARSECTOR_RENDER_WIDTH__: 1024,
    __STARSECTOR_RENDER_HEIGHT__: 768,
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
    if (/(?:^|\b)Fatal\s*:\s*|Exception in thread|auto campaign aborting|exhausted all new-game/i.test(text) && !fatalSeenAt) {
      fatalSeenAt = Date.now();
    }
    if (/GL_INVALID_(?:ENUM|OPERATION).*glVertexAttribPointer|LWJGL vertexAttribPointer error=|Unsupported LWJGL client array type=|Failed to convert LWJGL client array|Unsupported LWJGL alpha-test func=|WebGL: too many errors/i.test(text)) {
      graphicsErrors.push(text);
    }
    const recoveryUsed =
      /synthetic player fleet create success|auto campaign synthesized player fleet/i.test(text)
      || /synthetic readiness fleet fallback result=(?!null\b)/i.test(text)
      || /(?:created|seeded) minimal fallback market/i.test(text);
    if (recoveryUsed) {
      disallowedRecovery.push(text);
      if (!disallowedRecoverySeenAt) disallowedRecoverySeenAt = Date.now();
    }
    if (logs.length % 25 === 0 || fatalSeenAt || disallowedRecoverySeenAt || campaignSeenAt) flushLogs();
  });
  page.on('pageerror', error => {
    errors.push(String(error && (error.stack || error.message) || error));
    flushLogs();
  });
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
  while (Date.now() < deadline && errors.length === 0 && !fatalSeenAt && !disallowedRecoverySeenAt) {
    const state = await withTimeout(
      page.evaluate(() => document.body.dataset.runtimeState || ''),
      5000,
      'runtime-state evaluate'
    ).catch(error => {
      logs.push(`[diagnostic] ${error.message}`);
      return '';
    });
    if (['main-returned', 'failed', 'fatal'].includes(state)) break;
    if (expectedState === 'campaign' && campaignSeenAt) break;
    if (expectedState === 'title' && titleSeenAt && Date.now() - titleSeenAt >= settleMs) break;
    await sleep(1000);
  }

  flushLogs();
  const game = page.locator('#game-container');
  const gameCanvas = page.locator('#lwjglCanvas');
  const safeScreenshot = async (path, label) => {
    try {
      await withTimeout(gameCanvas.waitFor({ state: 'visible', timeout: 10000 }), 12000, `${label} canvas visibility`);
      return await withTimeout(gameCanvas.screenshot({ path, timeout: 10000 }), 12000, label);
    } catch (error) {
      screenshotErrors.push(String(error && (error.stack || error.message) || error));
      logs.push(`[diagnostic] ${label} failed: ${error.message || error}`);
      flushLogs();
      return null;
    }
  };

  const first = await safeScreenshot(`${outputDir}/frame-first.png`, 'first frame screenshot');
  await sleep(settleMs);
  const second = await safeScreenshot(`${outputDir}/frame-second.png`, 'second frame screenshot');

  // Exercise real input only after the visual evidence has been captured so the
  // probe cannot alter the screenshot gate. This catches the previous failure
  // where DOM events existed but the runtime-winning LWJGL Mouse/Keyboard
  // classes always returned false/no events.
  const inputBefore = await withTimeout(
    page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) })),
    5000,
    'input baseline'
  ).catch(() => ({}));
  try {
    const box = await gameCanvas.boundingBox();
    await gameCanvas.focus();
    if (box) {
      const cx = box.x + box.width * 0.57;
      const cy = box.y + box.height * 0.43;
      await page.mouse.move(cx, cy);
      await page.mouse.move(cx + 32, cy + 18, { steps: 3 });
      await page.mouse.down({ button: 'left' });
      await sleep(650);
      await page.mouse.up({ button: 'left' });
      await page.mouse.wheel(0, -120);
    }
    await page.keyboard.down('w');
    await sleep(900);
    await page.keyboard.up('w');
    await sleep(1800);
  } catch (error) {
    errors.push(`input probe failed: ${error.message || error}`);
  }
  const inputAfter = await withTimeout(
    page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) })),
    5000,
    'input result'
  ).catch(() => ({}));
  const inputKeyboardResponsive = expectedState !== 'campaign' || Boolean(
    (inputAfter.directKeyboardDelivered || 0) > (inputBefore.directKeyboardDelivered || 0)
    || (inputAfter.keyboardPressedQueries || 0) > (inputBefore.keyboardPressedQueries || 0)
  );
  const inputMouseResponsive = expectedState !== 'campaign' || Boolean(
    (inputAfter.directMouseDelivered || 0) > (inputBefore.directMouseDelivered || 0)
    || (inputAfter.mousePressedQueries || 0) > (inputBefore.mousePressedQueries || 0)
    || (inputAfter.mousePositionQueries || 0) > (inputBefore.mousePositionQueries || 0)
  );
  const inputResponsive = inputKeyboardResponsive && inputMouseResponsive;

  // Exercise the actual bottom campaign controls. A map-only input probe missed a
  // production crash where the first Character click reached ScriptStore with an
  // empty LevelupPlugin repository and terminated the whole game.
  const campaignUiControls = [
    ['Character', 0.085, 0.976],
    ['Fleet', 0.205, 0.976],
    ['Cargo', 0.490, 0.976],
    ['Map', 0.610, 0.976],
    ['Command', 0.805, 0.976],
  ];
  const uiControlResults = [];
  if (expectedState === 'campaign' && !fatalSeenAt) {
    try {
      const box = await gameCanvas.boundingBox();
      if (!box) throw new Error('campaign canvas has no bounding box');
      for (const [name, nx, ny] of campaignUiControls) {
        if (fatalSeenAt || errors.length > 0) break;
        const x = box.x + box.width * nx;
        const y = box.y + box.height * ny;
        logs.push(`[ui-probe] click ${name} css=(${x.toFixed(1)},${y.toFixed(1)}) logical=(${Math.round(nx * 1024)},${Math.round(ny * 768)})`);
        await page.mouse.move(x, y);
        await page.mouse.click(x, y, { button: 'left', delay: 80 });
        await sleep(2200);
        const afterClick = await withTimeout(page.evaluate(() => ({
          bodyState: document.body.dataset.runtimeState || '',
          bodyDetail: document.body.dataset.runtimeDetail || '',
          runtime: window.__STARSECTOR_RUNTIME_STATE__ || null,
        })), 5000, `${name} UI state`).catch(error => ({
          bodyState: 'unresponsive',
          bodyDetail: error.message || String(error),
          runtime: null,
        }));
        const failed = Boolean(fatalSeenAt)
          || ['main-returned', 'failed', 'fatal', 'unresponsive'].includes(afterClick.bodyState)
          || afterClick.runtime?.state === 'fatal';
        uiControlResults.push({ name, failed, ...afterClick });
        flushLogs();
        if (failed) break;
        await page.keyboard.press('Escape');
        await sleep(900);
      }
    } catch (error) {
      errors.push(`campaign UI control probe failed: ${error.message || error}`);
    }
  }
  const uiControlsSafe = expectedState !== 'campaign' || Boolean(
    uiControlResults.length === campaignUiControls.length
    && uiControlResults.every(item => !item.failed)
    && !fatalSeenAt
  );

  const state = await withTimeout(page.evaluate(() => ({
    runtime: window.__STARSECTOR_RUNTIME_STATE__ || null,
    bodyState: document.body.dataset.runtimeState || '',
    bodyDetail: document.body.dataset.runtimeDetail || '',
    button: document.getElementById('startBtn')?.textContent || '',
    nativeStats: window.__lwjglNativeStats || null,
    presentationStats: window.__lwjglPresentationStats || null,
    inputStats: window.__lwjglInputStats || null,
    webglState: (() => {
      const canvas = document.querySelector('#game-container canvas');
      const gl = canvas && canvas.getContext('webgl2');
      if (!gl) return null;
      try {
        return {
          drawingBufferWidth: gl.drawingBufferWidth,
          drawingBufferHeight: gl.drawingBufferHeight,
          viewport: Array.from(gl.getParameter(gl.VIEWPORT)),
          colorMask: Array.from(gl.getParameter(gl.COLOR_WRITEMASK)),
          clearColor: Array.from(gl.getParameter(gl.COLOR_CLEAR_VALUE)),
          framebufferStatus: gl.checkFramebufferStatus(gl.FRAMEBUFFER),
          contextAttributes: gl.getContextAttributes()
        };
      } catch (error) {
        return { error: String(error && (error.message || error) || error) };
      }
    })(),
    canvases: [...document.querySelectorAll('canvas')].map(c => ({
      width: c.width,
      height: c.height,
      clientWidth: c.clientWidth,
      clientHeight: c.clientHeight
    }))
  })), 10000, 'final state evaluate').catch(error => {
    errors.push(String(error && (error.stack || error.message) || error));
    return {
      runtime: null,
      bodyState: 'unresponsive',
      bodyDetail: error.message || String(error),
      button: '',
      nativeStats: null,
      canvases: []
    };
  });

  const firstStats = pixelStats(first);
  const secondStats = pixelStats(second);
  const frameChanged = Boolean(firstStats && secondStats && firstStats.sha256 !== secondStats.sha256);
  const rendered = Boolean(secondStats && secondStats.nonBlackRatio > 0.01 && secondStats.variance > 2);
  const campaignTextureRichness = expectedState !== 'campaign' || Boolean(secondStats
    && secondStats.quantizedColorCount >= 200
    && secondStats.variance >= 400);
  const campaignCenterSubject = expectedState !== 'campaign' || Boolean(secondStats
    && secondStats.centerBrightPixels >= 150
    && secondStats.centerWarmPixels >= 8);
  const campaignVisualQuality = expectedState !== 'campaign' || Boolean(secondStats
    && secondStats.nonBlackRatio > 0.03
    && secondStats.darkRatio < 0.94
    && secondStats.midToneRatio > 0.025
    && campaignTextureRichness
    && campaignCenterSubject);
  const campaign = Boolean(campaignSeenAt) || state.bodyState === 'campaign';
  const title = Boolean(titleSeenAt);
  const progressing = updateMax >= 10 || swapMax >= 10 || frameChanged;
  const reachedExpected = expectedState === 'title' ? title : campaign;
  const nativeStatsEnabled = state.nativeStats?.enabled === true;
  const bridgeCalls = state.nativeStats?.callsByName || {};
  const legacyTexCoordCalls = Number(bridgeCalls.Java_org_lwjgl_opengl_GL11_nglTexCoord2f || 0);
  const batchedVertexCalls = Number(bridgeCalls.Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord || 0);
  // Production intentionally bypasses the heavyweight native-statistics wrapper.
  // Static bridge verification proves the batched path in that mode; if a
  // diagnostics run explicitly enables stats, retain the runtime count gate.
  const immediateBridgeEfficient = expectedState !== 'campaign' || !nativeStatsEnabled || Boolean(
    batchedVertexCalls >= 1000
    && legacyTexCoordCalls <= Math.max(100, batchedVertexCalls * 0.05)
  );
  const ok = reachedExpected && rendered && campaignVisualQuality && progressing
    && inputResponsive && uiControlsSafe && immediateBridgeEfficient && errors.length === 0 && !fatalSeenAt
    && graphicsErrors.length === 0
    && disallowedRecovery.length === 0
    && screenshotErrors.length === 0
    && !['main-returned', 'failed', 'fatal', 'unresponsive'].includes(state.bodyState);

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
    disallowedRecoverySeenAt,
    disallowedRecovery,
    rendered,
    campaignTextureRichness,
    campaignCenterSubject,
    campaignVisualQuality,
    inputKeyboardResponsive,
    inputMouseResponsive,
    inputResponsive,
    uiControlsSafe,
    uiControlResults,
    inputBefore,
    inputAfter,
    nativeStatsEnabled,
    immediateBridgeEfficient,
    legacyTexCoordCalls,
    batchedVertexCalls,
    progressing,
    frameChanged,
    updateMax,
    swapMax,
    firstStats,
    secondStats,
    errors,
    screenshotErrors,
    graphicsErrors: [...new Set(graphicsErrors)],
    httpErrors: [...new Set(httpErrors)],
    state,
    logTail: logs.slice(-500)
  };
  fs.writeFileSync(`${outputDir}/result.json`, JSON.stringify(result, null, 2));
  fs.writeFileSync(`${outputDir}/browser.log`, logs.join('\n'));
  console.log(JSON.stringify(result, null, 2));
  await withTimeout(browser.close(), 10000, 'browser close').catch(error => {
    console.error(error.message || error);
  });
  process.exit(ok ? 0 : 1);
})().catch(error => {
  console.error(error && (error.stack || error.message) || error);
  process.exit(2);
});
