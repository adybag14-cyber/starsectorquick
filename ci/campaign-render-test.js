const fs = require('fs');
const crypto = require('crypto');
const { chromium } = require('playwright');
const { PNG } = require('pngjs');
const { campaignCenterSubjectGate, isCampaignFramePlayable } = require('./campaign-visual-gate');

const baseUrl = process.env.STARSECTOR_TEST_URL || 'http://127.0.0.1:8000/launch.html';
const timeoutMs = Number(process.env.STARSECTOR_TEST_TIMEOUT_MS || 360000);
const outputDir = process.env.STARSECTOR_TEST_OUTPUT_DIR || 'test_output/campaign-render';
const expectedState = String(process.env.STARSECTOR_EXPECT_STATE || 'campaign').toLowerCase();
const settleMs = Number(process.env.STARSECTOR_FRAME_SETTLE_MS || 15000);
const deepGameplay = /^(?:1|true|yes)$/i.test(String(process.env.STARSECTOR_DEEP_GAMEPLAY || 'false'));
const saveLoadSmokeEnabled = /^(?:1|true|yes)$/i.test(String(process.env.STARSECTOR_SAVE_LOAD_SMOKE || 'false'));
const saveLoadSmokeTimeoutMs = Math.max(60000, Number(process.env.STARSECTOR_SAVE_LOAD_TIMEOUT_MS || 360000));
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

function pixelDiffRatio(beforeBuffer, afterBuffer, region) {
  if (!beforeBuffer || !afterBuffer) return 0;
  const a = PNG.sync.read(beforeBuffer);
  const b = PNG.sync.read(afterBuffer);
  if (a.width !== b.width || a.height !== b.height) return 1;
  const r = region || { x0: 0, y0: 0, x1: 1, y1: 1 };
  const x0 = Math.max(0, Math.floor(a.width * r.x0));
  const x1 = Math.min(a.width, Math.ceil(a.width * r.x1));
  const y0 = Math.max(0, Math.floor(a.height * r.y0));
  const y1 = Math.min(a.height, Math.ceil(a.height * r.y1));
  let changed = 0;
  let total = 0;
  for (let y = y0; y < y1; y++) {
    for (let x = x0; x < x1; x++) {
      const i = (y * a.width + x) * 4;
      const delta = Math.abs(a.data[i] - b.data[i])
        + Math.abs(a.data[i + 1] - b.data[i + 1])
        + Math.abs(a.data[i + 2] - b.data[i + 2]);
      if (delta >= 24) changed += 1;
      total += 1;
    }
  }
  return total > 0 ? changed / total : 0;
}

function panelVisualThreshold(tab) {
  // Command/OUTPOSTS is intentionally sparse on a fresh campaign and changes
  // less of the screen than Character/Fleet/Refit/Cargo/Map/Intel. Keep a
  // meaningful visual gate, but key it to the actual screen rather than forcing
  // every tab through one 8% pixel-difference threshold.
  return tab === 'OUTPOSTS' ? 0.04 : 0.08;
}

async function waitForVisualTransition(canvas, baseline, options = {}) {
  const region = options.region || { x0: 0.08, y0: 0.04, x1: 0.96, y1: 0.88 };
  const threshold = Number(options.threshold ?? 0.025);
  const timeoutMs = Number(options.timeoutMs ?? 8000);
  const pollMs = Math.max(100, Number(options.pollMs ?? 400));
  const started = Date.now();
  let bestDiff = 0;
  let lastFrame = null;
  while (Date.now() - started <= timeoutMs) {
    await sleep(pollMs);
    lastFrame = await canvas.screenshot({ timeout: 10000 });
    const visualDiff = pixelDiffRatio(baseline, lastFrame, region);
    bestDiff = Math.max(bestDiff, visualDiff);
    if (visualDiff >= threshold) {
      return { opened: true, openMs: Date.now() - started, visualDiff, bestDiff, frame: lastFrame };
    }
  }
  return { opened: false, openMs: null, visualDiff: bestDiff, bestDiff, frame: lastFrame };
}

async function waitForCampaignFrame(canvas, options = {}) {
  const timeoutMs = Number(options.timeoutMs ?? 8000);
  const pollMs = Math.max(100, Number(options.pollMs ?? 400));
  const started = Date.now();
  let lastFrame = null;
  let lastStats = null;
  while (Date.now() - started <= timeoutMs) {
    await sleep(pollMs);
    lastFrame = await canvas.screenshot({ timeout: 10000 });
    lastStats = pixelStats(lastFrame);
    if (isCampaignFramePlayable(lastStats)) {
      return { ready: true, readyMs: Date.now() - started, frame: lastFrame, stats: lastStats };
    }
  }
  return { ready: false, readyMs: null, frame: lastFrame, stats: lastStats };
}

async function waitForLogMatch(logs, startIndex, pattern, options = {}) {
  const timeoutMs = Number(options.timeoutMs ?? 10000);
  const pollMs = Math.max(25, Number(options.pollMs ?? 100));
  const pump = typeof options.pump === 'function' ? options.pump : null;
  const started = Date.now();
  while (Date.now() - started <= timeoutMs) {
    if (pump) await pump().catch(() => undefined);
    for (let i = startIndex; i < logs.length; i++) {
      const line = logs[i];
      const match = line.match(pattern);
      if (match) return { matched: true, match, line, index: i, elapsedMs: Date.now() - started };
    }
    await sleep(pollMs);
  }
  return { matched: false, match: null, line: null, index: -1, elapsedMs: Date.now() - started };
}

async function waitForGameplayEvent(events, startIndex, predicate, options = {}) {
  const timeoutMs = Number(options.timeoutMs ?? 10000);
  const pollMs = Math.max(25, Number(options.pollMs ?? 100));
  const started = Date.now();
  while (Date.now() - started <= timeoutMs) {
    for (let i = startIndex; i < events.length; i++) {
      const event = events[i];
      if (predicate(event)) return { matched: true, event, index: i, elapsedMs: Date.now() - started };
    }
    await sleep(pollMs);
  }
  return { matched: false, event: null, index: -1, elapsedMs: Date.now() - started };
}

function latestAbilityReadiness(events, abilityId) {
  for (let i = events.length - 1; i >= 0; i--) {
    const event = events[i];
    if (event.id !== abilityId) continue;
    if (event.event === 'ability-ready') return true;
    if (event.event === 'ability-unready') return false;
  }
  return null;
}

function latestAbilityStableReadiness(events, abilityId) {
  for (let i = events.length - 1; i >= 0; i--) {
    const event = events[i];
    if (event.id !== abilityId) continue;
    if (event.event === 'ability-unready') return false;
    if (event.event === 'ability-ready-stable') return true;
  }
  return null;
}

async function waitForAbilityReady(events, abilityId, options = {}) {
  const initialState = latestAbilityStableReadiness(events, abilityId);
  if (initialState === true) {
    return { ready: true, initialState, finalState: true, elapsedMs: 0, source: 'stable-current' };
  }
  const startIndex = events.length;
  const ready = await waitForGameplayEvent(
    events, startIndex,
    event => event.id === abilityId && event.event === 'ability-ready-stable',
    { timeoutMs: Number(options.timeoutMs ?? 12000), pollMs: Number(options.pollMs ?? 100) }
  );
  return {
    ready: ready.matched,
    initialState,
    finalState: latestAbilityStableReadiness(events, abilityId),
    elapsedMs: ready.elapsedMs,
    source: ready.matched ? 'stable-transition' : 'timeout',
  };
}

function latestAbilityUiReadiness(events, abilityId) {
  for (let i = events.length - 1; i >= 0; i--) {
    const event = events[i];
    if (event.id !== abilityId) continue;
    if (event.event === 'ability-ui-ready') return true;
    if (event.event === 'ability-ui-unready'
        || event.event === 'ability-ui-lag'
        || event.event === 'ability-ui-stale-enabled') return false;
  }
  return null;
}

async function waitForAbilityUiReady(events, abilityId, options = {}) {
  const initialState = latestAbilityUiReadiness(events, abilityId);
  if (initialState === true) {
    return { ready: true, initialState, finalState: true, elapsedMs: 0, source: 'ui-current' };
  }
  const startIndex = events.length;
  const ready = await waitForGameplayEvent(
    events, startIndex,
    event => event.id === abilityId && event.event === 'ability-ui-ready',
    { timeoutMs: Number(options.timeoutMs ?? 12000), pollMs: Number(options.pollMs ?? 100) }
  );
  return {
    ready: ready.matched,
    initialState,
    finalState: latestAbilityUiReadiness(events, abilityId),
    elapsedMs: ready.elapsedMs,
    source: ready.matched ? 'ui-transition' : 'timeout',
  };
}

async function waitForPresentationFrames(page, minFrames = 3, options = {}) {
  const timeoutMs = Number(options.timeoutMs ?? 5000);
  const pollMs = Math.max(50, Number(options.pollMs ?? 100));
  const readSwapCount = () => page.evaluate(() => Number(window.__lwjglPresentationStats?.swapCount || 0));
  const start = await readSwapCount().catch(() => 0);
  const started = Date.now();
  let current = start;
  while (Date.now() - started <= timeoutMs) {
    await sleep(pollMs);
    current = await readSwapCount().catch(() => current);
    if (current - start >= minFrames) {
      return { advanced: true, frames: current - start, elapsedMs: Date.now() - started };
    }
  }
  return { advanced: false, frames: current - start, elapsedMs: Date.now() - started };
}

(async () => {
  const logs = [];
  const errors = [];
  const httpErrors = [];
  const localNegativeMisses = [];
  const screenshotErrors = [];
  const graphicsErrors = [];
  const runtimeErrorSignals = [];
  const gameplayEvents = [];
  const disallowedRecovery = [];
  let campaignSeenAt = 0;
  let titleSeenAt = 0;
  let fatalSeenAt = 0;
  let disallowedRecoverySeenAt = 0;
  let updateMax = 0;
  let swapMax = 0;
  let startingSuppliesTarget = null;
  let startingSuppliesAfter = null;
  let startingSuppliesReady = false;
  let playableResources = null;
  let starterAbilityMappingReady = false;
  const starterAbilitySlots = {};
  let jarPackResponses = 0;
  let jarPackResponseBytes = 0;
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
    __STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__: 'normal',
    __STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__: deepGameplay ? 'Corvus' : 'Galatia',
    __STARSECTOR_BROWSER_TUTORIAL__: !deepGameplay,
    __STARSECTOR_AUTO_CAMPAIGN_TIMEOUT_MS__: 900000,
    __STARSECTOR_AUTO_CAMPAIGN_DIRECT_ATTEMPT_TIMEOUT_MS__: 45000,
    __STARSECTOR_FORCE_CHEERPJ_STORAGE_RESET__: true,
    __STARSECTOR_RENDER_WIDTH__: 1024,
    __STARSECTOR_RENDER_HEIGHT__: 768,
    __LWJGL_FIRST_LOG_LIMIT__: 512,
    __STARSECTOR_BROWSER_GAMEPLAY_PROBE__: deepGameplay,
    __STARSECTOR_BROWSER_GAMEPLAY_SPEEDUP_MULT__: deepGameplay ? 8 : 0
  };
  const windowConfig = { ...defaultConfig, ...configOverrides };
  await page.addInitScript(config => {
    for (const [key, value] of Object.entries(config)) window[key] = value;
  }, windowConfig);

  page.on('console', message => {
    const text = message.text();
    logs.push(`[${message.type()}] ${text}`);
    if (text.includes('BrowserGameplayProbe:')) {
      const payload = text.slice(text.indexOf('BrowserGameplayProbe:') + 'BrowserGameplayProbe:'.length).trim();
      const event = { raw: text, at: Date.now() };
      for (const token of payload.split(/\s+/)) {
        const eq = token.indexOf('=');
        if (eq <= 0) continue;
        event[token.slice(0, eq)] = token.slice(eq + 1);
      }
      gameplayEvents.push(event);
    }
    const update = text.match(/Bridge Display\.update(?:\([^)]*\))? count=(\d+)/i);
    if (update) updateMax = Math.max(updateMax, Number(update[1]));
    const swap = text.match(/Bridge Display\.swapBuffers count=(\d+)/i);
    if (swap) swapMax = Math.max(swapMax, Number(swap[1]));
    const supplies = text.match(/Fixer: auto campaign playable starting supplies .*?target=([0-9.+-]+)\s+after=([0-9.+-]+)\s+ready=(true|false)/i);
    if (supplies) {
      startingSuppliesTarget = Number(supplies[1]);
      startingSuppliesAfter = Number(supplies[2]);
      startingSuppliesReady = supplies[3].toLowerCase() === 'true'
        && Number.isFinite(startingSuppliesTarget)
        && Number.isFinite(startingSuppliesAfter);
    }
    const resources = text.match(/Fixer: auto campaign playable resources supplies=([0-9.+-]+)\s+fuel=([0-9.+-]+)\/([0-9.+-]+)\s+crew=([0-9.+-]+)\/min=([0-9.+-]+)\/max=([0-9.+-]+)\s+cargo=([0-9.+-]+)\/([0-9.+-]+)\s+credits=([0-9.+-]+)\s+ready=(true|false)/i);
    if (resources) {
      playableResources = {
        supplies: Number(resources[1]),
        fuel: Number(resources[2]),
        maxFuel: Number(resources[3]),
        crew: Number(resources[4]),
        minCrew: Number(resources[5]),
        maxPersonnel: Number(resources[6]),
        cargoUsed: Number(resources[7]),
        maxCargo: Number(resources[8]),
        credits: Number(resources[9]),
        ready: resources[10].toLowerCase() === 'true',
      };
    }
    const starterAbilities = text.match(/Fixer: auto campaign starter abilities mapped=([^\r\n]+?)\s+ready=(true|false)/i);
    if (starterAbilities) {
      starterAbilityMappingReady = starterAbilities[2].toLowerCase() === 'true';
      for (const entry of starterAbilities[1].split(',')) {
        const match = entry.trim().match(/^(\d+)=(.+)$/);
        if (match) starterAbilitySlots[match[1]] = match[2];
      }
    }
    if (/watcher state=Campaign State|reached Campaign State/i.test(text) && !campaignSeenAt) {
      campaignSeenAt = Date.now();
    }
    if (/watcher state=Title Screen State|Main loop inTitle Screen State/i.test(text) && !titleSeenAt) {
      titleSeenAt = Date.now();
    }
    const hardRuntimeError = /fatal\s+starsector\s+null|NullPointerException|Exception in thread|(?:^|\b)Fatal\s*:\s*|auto campaign aborting|exhausted all new-game/i.test(text);
    if (hardRuntimeError) {
      runtimeErrorSignals.push(text);
      if (!fatalSeenAt) fatalSeenAt = Date.now();
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
    const headers = response.headers();
    if (headers['x-starsectorquick-jar-pack'] === 'v1') {
      jarPackResponses += 1;
      jarPackResponseBytes += Number(headers['content-length'] || 0) || 0;
    }
    if (response.status() >= 400) {
      const item = `${response.status()} ${response.url()}`;
      const negativeReason = headers['x-starsectorquick-negative-cache'] || '';
      if (negativeReason) {
        localNegativeMisses.push(`${item} reason=${negativeReason}`);
        logs.push(`[http-local-miss] ${item} reason=${negativeReason}`);
      } else {
        httpErrors.push(item);
        logs.push(`[http] ${item}`);
      }
    }
  });

  const navigationStartedAt = Date.now();
  const target = `${baseUrl}?autostart=1&ci=${navigationStartedAt}`;
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
  const configuredScreenshotTimeoutMs = Number(process.env.STARSECTOR_SCREENSHOT_TIMEOUT_MS || 12000);
  const screenshotTimeoutMs = Number.isFinite(configuredScreenshotTimeoutMs)
    ? Math.max(12000, configuredScreenshotTimeoutMs)
    : 12000;
  const screenshotActionTimeoutMs = Math.max(10000, screenshotTimeoutMs - 2000);
  const safeScreenshot = async (path, label) => {
    let primaryError = null;
    try {
      await withTimeout(
        gameCanvas.waitFor({ state: 'visible', timeout: screenshotActionTimeoutMs }),
        screenshotTimeoutMs,
        `${label} canvas visibility`,
      );
      return await withTimeout(
        gameCanvas.screenshot({ path, timeout: screenshotActionTimeoutMs }),
        screenshotTimeoutMs,
        label,
      );
    } catch (error) {
      primaryError = error;
      // A hot continuously-rendering canvas can occasionally make Playwright's
      // Locator.screenshot actionability/stability wait hit its timeout even
      // though the WebGL surface is present and rendering. Retry once through a
      // clipped Page screenshot, which captures the same canvas pixels without
      // requiring locator stability. Keep the gate strict if this fallback fails.
      logs.push(`[diagnostic] ${label} locator capture failed; retrying clipped page capture: ${error.message || error}`);
      flushLogs();
    }

    try {
      await sleep(250);
      const box = await withTimeout(
        gameCanvas.boundingBox(),
        screenshotTimeoutMs,
        `${label} fallback bounding box`,
      );
      if (!box || box.width <= 0 || box.height <= 0) {
        throw new Error(`${label} fallback canvas has no visible bounding box`);
      }
      const viewport = page.viewportSize();
      const x = Math.max(0, box.x);
      const y = Math.max(0, box.y);
      const right = viewport ? Math.min(viewport.width, box.x + box.width) : box.x + box.width;
      const bottom = viewport ? Math.min(viewport.height, box.y + box.height) : box.y + box.height;
      const clip = {
        x,
        y,
        width: Math.max(1, right - x),
        height: Math.max(1, bottom - y),
      };
      const fallback = await withTimeout(
        page.screenshot({ path, clip }),
        screenshotTimeoutMs,
        `${label} clipped page fallback`,
      );
      logs.push(`[diagnostic] ${label} recovered with clipped page capture`);
      flushLogs();
      return fallback;
    } catch (fallbackError) {
      const primary = String(primaryError && (primaryError.stack || primaryError.message) || primaryError || 'unknown locator screenshot failure');
      const fallback = String(fallbackError && (fallbackError.stack || fallbackError.message) || fallbackError);
      screenshotErrors.push(`${primary}
Fallback screenshot failed:
${fallback}`);
      logs.push(`[diagnostic] ${label} failed after clipped fallback: ${fallbackError.message || fallbackError}`);
      flushLogs();
      return null;
    }
  };

  const first = await safeScreenshot(`${outputDir}/frame-first.png`, 'first frame screenshot');
  const firstFrameCapturedAt = first ? Date.now() : null;
  await sleep(settleMs);
  const second = await safeScreenshot(`${outputDir}/frame-second.png`, 'second frame screenshot');
  const secondFrameCapturedAt = second ? Date.now() : null;

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
  // CampaignState's bottom bar is seven 125px buttons separated by 6px,
  // inset 6px from the left and 10px from the bottom of the 1024x768 logical
  // viewport. Slot 1 is Character (the exact reproduced crash); sweeping every
  // slot avoids silently missing controls whose live labels differ by game state.
  const campaignControlTabs = [
    ['Character', 'CHARACTER'],
    ['Fleet', 'FLEET'],
    ['Refit', 'REFIT'],
    ['Crew/Cargo', 'CARGO'],
    ['Map', 'MAP'],
    ['Intel', 'INTEL'],
    ['Command', 'OUTPOSTS'],
  ];
  const campaignUiControls = campaignControlTabs.map(([label, tab], slot) => [
    label, tab,
    (68.5 + 131 * slot) / 1024,
    748 / 768,
  ]);
  const uiControlResults = [];
  if (expectedState === 'campaign' && !fatalSeenAt) {
    try {
      const box = await gameCanvas.boundingBox();
      if (!box) throw new Error('campaign canvas has no bounding box');
      for (const [name, expectedTab, nx, ny] of campaignUiControls) {
        if (fatalSeenAt || errors.length > 0) break;
        const x = box.x + box.width * nx;
        const y = box.y + box.height * ny;
        const beforePanel = deepGameplay ? await gameCanvas.screenshot({ timeout: 10000 }) : null;
        const probeStart = logs.length;
        const gameplayStart = gameplayEvents.length;
        const panelStartedAt = Date.now();
        logs.push(`[ui-probe] click ${name} expectedTab=${expectedTab} css=(${x.toFixed(1)},${y.toFixed(1)}) logical=(${Math.round(nx * 1024)},${Math.round(ny * 768)})`);
        await page.mouse.move(x, y);
        await page.mouse.click(x, y, { button: 'left', delay: 80 });
        let tabReady = null;
        let panelTransition = null;
        let panelFrame = null;
        let panelVisualDiff = null;
        let listenerReadyMs = null;
        let visualReadyMs = null;
        let panelOpened = !deepGameplay;
        if (deepGameplay) {
          tabReady = await waitForGameplayEvent(
            gameplayEvents, gameplayStart,
            event => event.event === 'core-tab-ready' && event.tab === expectedTab,
            { timeoutMs: 10000, pollMs: 100 }
          );
          listenerReadyMs = tabReady.matched ? Date.now() - panelStartedAt : null;
          if (tabReady.matched) {
            panelTransition = await waitForVisualTransition(gameCanvas, beforePanel, {
              timeoutMs: 9000,
              pollMs: 800,
              threshold: panelVisualThreshold(expectedTab),
              region: { x0: 0.08, y0: 0.04, x1: 0.96, y1: 0.88 },
            });
            panelFrame = panelTransition.frame;
            panelVisualDiff = panelTransition.visualDiff;
            panelOpened = panelTransition.opened;
            visualReadyMs = panelOpened ? Date.now() - panelStartedAt : null;
          }
        } else {
          await sleep(2200);
        }
        if (panelFrame) fs.writeFileSync(`${outputDir}/gameplay-panel-${name.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}.png`, panelFrame);
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
          || afterClick.runtime?.state === 'fatal'
          || !panelOpened;
        const controlResult = {
          name,
          expectedTab,
          tabReady: Boolean(tabReady?.matched),
          failed,
          panelOpened,
          listenerReadyMs,
          visualReadyMs,
          panelVisualDiff,
          returnedToCampaign: !deepGameplay,
          returnReadyMs: null,
          returnVisualDiff: null,
          ...afterClick,
        };
        uiControlResults.push(controlResult);
        logs.push(`[ui-probe] result ${name} expectedTab=${expectedTab} tabReady=${Boolean(tabReady?.matched)} panelOpened=${panelOpened} listenerReadyMs=${listenerReadyMs ?? 'n/a'} visualReadyMs=${visualReadyMs ?? 'n/a'} visualDiff=${panelVisualDiff == null ? 'n/a' : panelVisualDiff.toFixed(4)} failed=${failed}`);
        flushLogs();

        // Clean up every listener that actually fired, even when its visual-ready
        // deadline failed, so a single slow panel cannot contaminate later probes.
        if (deepGameplay && tabReady?.matched) {
          if (panelOpened) {
            await page.mouse.move(box.x + box.width * 0.55, box.y + box.height * 0.45);
            await page.mouse.wheel(0, 480);
            await sleep(350);
          }
          await page.keyboard.press('Escape');
          const returned = await waitForCampaignFrame(gameCanvas, { timeoutMs: 8000, pollMs: 800 });
          const returnFrame = returned.frame;
          if (returnFrame) fs.writeFileSync(`${outputDir}/gameplay-return-${name.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}.png`, returnFrame);
          controlResult.returnReadyMs = returned.readyMs;
          controlResult.returnVisualDiff = returnFrame && panelFrame
            ? pixelDiffRatio(panelFrame, returnFrame, { x0: 0.08, y0: 0.04, x1: 0.96, y1: 0.88 })
            : 0;
          controlResult.returnedToCampaign = returned.ready && !fatalSeenAt;
          controlResult.failed = controlResult.failed || !controlResult.returnedToCampaign;
          logs.push(`[ui-probe] return ${name} returned=${controlResult.returnedToCampaign} readyMs=${controlResult.returnReadyMs ?? 'n/a'} visualDiff=${controlResult.returnVisualDiff.toFixed(4)} failed=${controlResult.failed}`);
          flushLogs();
        } else if (!deepGameplay) {
          await page.keyboard.press('Escape');
          await sleep(900);
        }
        if (controlResult.failed) break;
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

  // Reproduce a real browser focus transition before testing campaign shortcuts.
  // The old bridge dropped every key unless the canvas itself was active, while
  // desktop LWJGL continues to own the keyboard across in-game UI panels.
  const shortcutKeys = [
    ['Character', 'c', 'CHARACTER'],
    ['Fleet', 'f', 'FLEET'],
    ['Refit', 'r', 'REFIT'],
    ['Crew/Cargo', 'i', 'CARGO'],
    ['Map', 'Tab', 'MAP'],
    ['Intel', 'e', 'INTEL'],
    ['Command', 'd', 'OUTPOSTS'],
  ];
  const shortcutResults = [];
  const shortcutBefore = await withTimeout(
    page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) })),
    5000,
    'shortcut input baseline'
  ).catch(() => ({}));
  if (expectedState === 'campaign' && !fatalSeenAt) {
    try {
      for (const [name, key, expectedTab] of shortcutKeys) {
        await page.evaluate(() => {
          document.body.tabIndex = -1;
          document.body.focus({ preventScroll: true });
        });
        const before = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
        const activeBefore = await page.evaluate(() => document.activeElement?.tagName || '');
        const beforeFrame = deepGameplay ? await gameCanvas.screenshot({ timeout: 10000 }) : null;
        const probeStart = logs.length;
        const gameplayStart = gameplayEvents.length;
        const shortcutStartedAt = Date.now();
        await page.keyboard.press(key);
        let transition = null;
        let shortcutFrame = null;
        if (deepGameplay) {
          const ready = await waitForGameplayEvent(
            gameplayEvents, gameplayStart,
            event => event.event === 'core-tab-ready' && event.tab === expectedTab,
            { timeoutMs: 8000, pollMs: 100 }
          );
          const listenerReadyMs = ready.matched ? Date.now() - shortcutStartedAt : null;
          if (ready.matched) {
            const visual = await waitForVisualTransition(gameCanvas, beforeFrame, {
              timeoutMs: 8000,
              pollMs: 800,
              threshold: panelVisualThreshold(expectedTab),
              region: { x0: 0.08, y0: 0.04, x1: 0.96, y1: 0.88 },
            });
            shortcutFrame = visual.frame;
            transition = {
              opened: visual.opened,
              listenerReadyMs,
              visualReadyMs: visual.opened ? Date.now() - shortcutStartedAt : null,
              visualDiff: visual.visualDiff,
              readyMatched: true,
            };
          } else {
            transition = { opened: false, listenerReadyMs: null, visualReadyMs: null, visualDiff: 0, readyMatched: false };
          }
        } else {
          await sleep(1600);
        }
        const after = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
        const runtime = await page.evaluate(() => ({
          bodyState: document.body.dataset.runtimeState || '',
          runtime: window.__STARSECTOR_RUNTIME_STATE__ || null,
        }));
        const deliveredDelta = Number(after.directKeyboardDelivered || 0) - Number(before.directKeyboardDelivered || 0);
        const enqueuedDelta = Number(after.directKeyboardEnqueued || 0) - Number(before.directKeyboardEnqueued || 0);
        const droppedDelta = Number(after.directKeyboardDropped || 0) - Number(before.directKeyboardDropped || 0);
        const globalDelta = Number(after.keyboardGlobalCaptures || 0) - Number(before.keyboardGlobalCaptures || 0);
        const queueDepthBefore = Number(before.keyboardQueueDepth || 0);
        const queueDepthAfter = Number(after.keyboardQueueDepth || 0);
        const downCountBefore = Number(before.keyboardDownCount || 0);
        const downCountAfter = Number(after.keyboardDownCount || 0);
        const semanticOpened = !deepGameplay || Boolean(transition?.opened);
        // Opening a panel can change when Starsector drains Keyboard.next(). Dev mode
        // exposes that timing: keydown is consumed immediately while keyup may remain
        // queued. Validate the bridge at the enqueue/state boundary instead of requiring
        // both events to be consumed within an arbitrary 1.6 s window.
        const queueIntegrityOk = enqueuedDelta >= 2 && droppedDelta === 0 && downCountAfter === downCountBefore;
        const directInputOk = deliveredDelta >= 1 && queueIntegrityOk;
        const failed = Boolean(fatalSeenAt) || runtime.runtime?.state === 'fatal'
          || ['main-returned', 'failed', 'fatal', 'unresponsive'].includes(runtime.bodyState)
          || !directInputOk || globalDelta < 2 || !semanticOpened;
        const shortcutResult = {
          name,
          key,
          expectedTab,
          activeBefore,
          deliveredDelta,
          enqueuedDelta,
          droppedDelta,
          globalDelta,
          queueDepthBefore,
          queueDepthAfter,
          downCountBefore,
          downCountAfter,
          keyboardLatencyAvgMs: Number(after.directKeyboardLatencyAvgMs || 0),
          keyboardLatencyMaxMs: Number(after.directKeyboardLatencyMaxMs || 0),
          directInputOk,
          semanticOpened,
          tabReady: Boolean(transition?.readyMatched),
          listenerReadyMs: transition?.listenerReadyMs ?? null,
          visualReadyMs: transition?.visualReadyMs ?? null,
          visualDiff: transition?.visualDiff ?? null,
          failed,
        };
        shortcutResults.push(shortcutResult);
        logs.push(`[shortcut-probe] ${name} key=${key} expectedTab=${expectedTab} activeBefore=${activeBefore} deliveredDelta=${deliveredDelta} enqueuedDelta=${enqueuedDelta} droppedDelta=${droppedDelta} globalDelta=${globalDelta} queue=${queueDepthBefore}->${queueDepthAfter} down=${downCountBefore}->${downCountAfter} latencyAvgMs=${shortcutResult.keyboardLatencyAvgMs.toFixed(2)} latencyMaxMs=${shortcutResult.keyboardLatencyMaxMs.toFixed(2)} opened=${semanticOpened} listenerReadyMs=${shortcutResult.listenerReadyMs ?? 'n/a'} visualReadyMs=${shortcutResult.visualReadyMs ?? 'n/a'} visualDiff=${transition?.visualDiff == null ? 'n/a' : transition.visualDiff.toFixed(4)} failed=${failed}`);
        flushLogs();

        if (deepGameplay && transition?.readyMatched) {
          await page.keyboard.press('Escape');
          const returned = await waitForCampaignFrame(gameCanvas, { timeoutMs: 8000, pollMs: 800 });
          shortcutResult.returnedToCampaign = returned.ready;
          shortcutResult.returnReadyMs = returned.readyMs;
          shortcutResult.failed = shortcutResult.failed || !returned.ready;
        } else if (!deepGameplay) {
          await page.keyboard.press('Escape');
          await sleep(900);
        }
        if (shortcutResult.failed) break;
      }
    } catch (error) {
      errors.push(`campaign shortcut probe failed: ${error.message || error}`);
    }
  }
  const shortcutAfter = await withTimeout(
    page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) })),
    5000,
    'shortcut input result'
  ).catch(() => ({}));
  const shortcutsResponsive = expectedState !== 'campaign' || Boolean(
    shortcutResults.length === shortcutKeys.length
    && shortcutResults.every(item => !item.failed && item.directInputOk && item.globalDelta >= 2)
    && Number(shortcutAfter.keyboardGlobalCaptures || 0) > Number(shortcutBefore.keyboardGlobalCaptures || 0)
  );

  const expectedAbilityIds = [
    'transponder', 'go_dark', 'sensor_burst', 'emergency_burn',
    'sustained_burn', 'scavenge', 'interdiction_pulse', 'distress_call',
  ];
  const durationAbilityIds = new Set([
    'sensor_burst', 'emergency_burn', 'scavenge', 'interdiction_pulse', 'distress_call',
  ]);
  const contextUnavailableAbilityIds = new Set(['scavenge']);
  // Exercise short/independent abilities before the long Emergency Burn so a
  // slow software-rendered campaign clock cannot hide coverage of keys 7/8.
  const abilityExecutionOrder = [1, 2, 3, 5, 7, 8, 6, 4];
  const terminalDurationAbilityIds = new Set(['emergency_burn']);
  const abilityKeyResults = [];
  if (deepGameplay && expectedState === 'campaign' && !fatalSeenAt) {
    const abilityRegion = { x0: 0.25, y0: 0.76, x1: 0.93, y1: 0.96 };
    for (const digit of abilityExecutionOrder) {
      if (fatalSeenAt) break;
      const key = String(digit);
      const expectedId = expectedAbilityIds[digit - 1];
      const readinessRequired = !contextUnavailableAbilityIds.has(expectedId);
      const readiness = readinessRequired
        ? await waitForAbilityReady(gameplayEvents, expectedId, { timeoutMs: 12000, pollMs: 100 })
        : {
            ready: latestAbilityReadiness(gameplayEvents, expectedId) === true,
            initialState: latestAbilityReadiness(gameplayEvents, expectedId),
            finalState: latestAbilityReadiness(gameplayEvents, expectedId),
            elapsedMs: 0,
            source: 'context-optional',
          };
      const uiReadiness = readinessRequired
        ? await waitForAbilityUiReady(gameplayEvents, expectedId, { timeoutMs: 12000, pollMs: 100 })
        : {
            ready: latestAbilityUiReadiness(gameplayEvents, expectedId) === true,
            initialState: latestAbilityUiReadiness(gameplayEvents, expectedId),
            finalState: latestAbilityUiReadiness(gameplayEvents, expectedId),
            elapsedMs: 0,
            source: 'context-optional',
          };
      const logStart = logs.length;
      const gameplayStart = gameplayEvents.length;
      const beforeInput = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
      const beforeFrame = await gameCanvas.screenshot({ timeout: 10000 });

      await page.keyboard.press(key);
      let pressReady = await waitForGameplayEvent(
        gameplayEvents, gameplayStart,
        event => event.event === 'ability-press' && event.id === expectedId,
        { timeoutMs: 3500, pollMs: 100 }
      );
      await sleep(pressReady.matched ? 450 : 150);

      // Transponder intentionally uses a desktop-style confirmation double tap.
      // The first key primes the warning and the second must reach the same ability
      // and produce a real activation/deactivation state transition.
      let confirmationPress = false;
      let currentEvents = gameplayEvents.slice(gameplayStart);
      let currentStates = currentEvents.filter(event => event.event === 'ability-activate' || event.event === 'ability-deactivate');
      if (expectedId === 'transponder' && pressReady.matched && currentStates.length === 0) {
        confirmationPress = true;
        const confirmationStart = gameplayEvents.length;
        await page.keyboard.press(key);
        const confirmed = await waitForGameplayEvent(
          gameplayEvents, confirmationStart,
          event => (event.event === 'ability-activate' || event.event === 'ability-deactivate') && event.id === expectedId,
          { timeoutMs: 3500, pollMs: 100 }
        );
        await sleep(confirmed.matched ? 450 : 150);
      }

      const afterFrame = await gameCanvas.screenshot({ timeout: 10000 });
      fs.writeFileSync(`${outputDir}/gameplay-ability-${digit}.png`, afterFrame);
      const afterInput = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
      const events = gameplayEvents.slice(gameplayStart);
      const pressEvents = events.filter(event => event.event === 'ability-press');
      const uiActionEvents = events.filter(event => event.event === 'ability-ui-action');
      const stateEvents = events.filter(event => event.event === 'ability-activate' || event.event === 'ability-deactivate');
      const ids = [...new Set(pressEvents.map(event => event.id).filter(Boolean))];
      const slotMapped = starterAbilityMappingReady && starterAbilitySlots[String(digit)] === expectedId;
      const pressObserved = pressEvents.some(event => event.id === expectedId);
      const mapped = slotMapped;
      const wrongIds = ids.filter(id => id !== expectedId);
      const usable = pressEvents.some(event => event.id === expectedId && event.usable === 'true');
      const contextUnavailable = contextUnavailableAbilityIds.has(expectedId) && !pressObserved;
      const pressRequired = !contextUnavailableAbilityIds.has(expectedId);
      const uiActionObserved = uiActionEvents.some(event => event.id === expectedId);
      const preReady = readiness.ready && uiReadiness.ready;
      // A real press reported usable=true is stronger runtime evidence than the
      // pre-press stable probe. The latter can be stale for a few frames after
      // another duration ability settles under software rendering. Keep the UI
      // readiness requirement and all mapping/action/state gates intact.
      const runtimeReady = uiReadiness.ready && (readiness.ready || (pressObserved && usable));
      const deliveredDelta = Number(afterInput.directKeyboardDelivered || 0) - Number(beforeInput.directKeyboardDelivered || 0);
      const globalDelta = Number(afterInput.keyboardGlobalCaptures || 0) - Number(beforeInput.keyboardGlobalCaptures || 0);
      const visualDiff = pixelDiffRatio(beforeFrame, afterFrame, abilityRegion);
      const stateChanged = stateEvents.some(event => event.id === expectedId);
      const animatedOrChanged = contextUnavailable || !usable || stateChanged || visualDiff >= 0.002;
      const minKeyboardEvents = confirmationPress ? 4 : 2;
      const failed = deliveredDelta < minKeyboardEvents || globalDelta < minKeyboardEvents
        || !mapped || (pressRequired && (!runtimeReady || !uiActionObserved || !pressObserved)) || wrongIds.length > 0
        || !animatedOrChanged || Boolean(fatalSeenAt);
      abilityKeyResults.push({
        digit, expectedId, mapped, slotMapped, pressObserved, uiActionObserved, contextUnavailable, preReady, runtimeReady, readiness, uiReadiness, ids, wrongIds, usable,
        confirmationPress, deliveredDelta, globalDelta, visualDiff, stateChanged, stateEvents: stateEvents.length,
        pressReadyMs: pressReady.matched ? pressReady.elapsedMs : null, failed,
      });
      logs.push(`[ability-key-probe] key=${digit} expected=${expectedId} mapped=${mapped} preReady=${preReady} runtimeReady=${runtimeReady} pluginReadyWaitMs=${readiness.elapsedMs} pluginReadySource=${readiness.source} uiReady=${uiReadiness.ready} uiReadyWaitMs=${uiReadiness.elapsedMs} uiReadySource=${uiReadiness.source} uiAction=${uiActionObserved} pressObserved=${pressObserved} contextUnavailable=${contextUnavailable} ids=${ids.join(',') || '-'} usable=${usable} confirm=${confirmationPress} deliveredDelta=${deliveredDelta} globalDelta=${globalDelta} visualDiff=${visualDiff.toFixed(4)} stateChanged=${stateChanged} stateEvents=${stateEvents.length} pressReadyMs=${pressReady.matched ? pressReady.elapsedMs : 'n/a'} failed=${failed}`);
      flushLogs();
      if (failed) break;

      // Duration abilities keep incompatible abilities force-disabled until their
      // deactivation fade has reached a true zero-progress state. A deactivation
      // callback alone is too early (notably for Emergency Burn). Wait for the
      // test-only ability-settled event emitted at the end of BaseDurationAbility
      // advance(), then allow three real presentation frames for disableFrames to
      // clear before probing the next numeric slot.
      if (durationAbilityIds.has(expectedId) && stateEvents.some(event => event.id === expectedId && event.event === 'ability-activate')) {
        const lifecycleRequired = !terminalDurationAbilityIds.has(expectedId);
        if (!lifecycleRequired) {
          const currentResult = abilityKeyResults[abilityKeyResults.length - 1];
          currentResult.durationLifecycleRequired = false;
          currentResult.durationSettled = gameplayEvents.slice(gameplayStart).some(
            event => event.id === expectedId && event.event === 'ability-settled'
          );
          logs.push(`[ability-settle] key=${digit} id=${expectedId} terminal=true lifecycleRequired=false settled=${currentResult.durationSettled} failed=false`);
          flushLogs();
          continue;
        }
        let fullySettled = gameplayEvents.slice(gameplayStart).some(
          event => event.id === expectedId && event.event === 'ability-settled'
        );
        let settleMs = 0;
        let fastForwardInput = null;
        let settledEvent = null;
        if (!fullySettled) {
          const fastBefore = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
          try {
            // Starsector's tutorial teaches holding FAST_FORWARD (stock binding:
            // Shift). Exercise that real gameplay control while long abilities run.
            await page.keyboard.down('Shift');
            settledEvent = await waitForGameplayEvent(
              gameplayEvents, gameplayStart,
              event => event.id === expectedId && event.event === 'ability-settled',
              { timeoutMs: 60000, pollMs: 100 }
            );
          } finally {
            await page.keyboard.up('Shift').catch(() => undefined);
          }
          const fastAfter = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
          fastForwardInput = {
            deliveredDelta: Number(fastAfter.directKeyboardDelivered || 0) - Number(fastBefore.directKeyboardDelivered || 0),
            globalDelta: Number(fastAfter.keyboardGlobalCaptures || 0) - Number(fastBefore.keyboardGlobalCaptures || 0),
          };
          fullySettled = Boolean(settledEvent && settledEvent.matched);
          settleMs = settledEvent ? settledEvent.elapsedMs : 60000;
        }
        const lifecycleEvents = gameplayEvents.slice(gameplayStart);
        const deactivated = lifecycleEvents.some(
          event => event.id === expectedId && event.event === 'ability-deactivate'
        );
        const recovery = fullySettled
          ? await waitForPresentationFrames(page, 3, { timeoutMs: 5000, pollMs: 100 })
          : { advanced: false, frames: 0, elapsedMs: 0 };
        const currentResult = abilityKeyResults[abilityKeyResults.length - 1];
        const settledElapsedDays = settledEvent?.event?.elapsedDays == null
          ? null
          : Number(settledEvent.event.elapsedDays);
        currentResult.durationDeactivated = deactivated;
        currentResult.durationSettled = fullySettled;
        currentResult.durationSettleMs = settleMs;
        currentResult.durationElapsedDays = Number.isFinite(settledElapsedDays) ? settledElapsedDays : null;
        currentResult.durationRecovery = recovery;
        currentResult.fastForwardInput = fastForwardInput;
        if (!deactivated || !fullySettled || !recovery.advanced) currentResult.failed = true;
        const ffDelivered = fastForwardInput ? fastForwardInput.deliveredDelta : 0;
        const ffGlobal = fastForwardInput ? fastForwardInput.globalDelta : 0;
        logs.push(`[ability-settle] key=${digit} id=${expectedId} deactivated=${deactivated} settled=${fullySettled} settleMs=${settleMs} elapsedDays=${currentResult.durationElapsedDays ?? 'n/a'} fastForwardDelivered=${ffDelivered} fastForwardGlobal=${ffGlobal} recoveryFrames=${recovery.frames} recoveryMs=${recovery.elapsedMs} failed=${currentResult.failed}`);
        flushLogs();
        if (currentResult.failed) break;
      }

      // Toggle-style abilities are pressed again to return the campaign to a
      // neutral baseline before the next slot. Transponder was already pressed
      // twice to activate; this third press only primes its off confirmation, so
      // follow it with a fourth when needed.
      if (stateEvents.some(event => event.id === expectedId && event.event === 'ability-activate' && event.active === 'true')) {
        const cleanupStart = gameplayEvents.length;
        await page.keyboard.press(key);
        await sleep(300);
        if (expectedId === 'transponder' && !gameplayEvents.slice(cleanupStart).some(event => event.id === expectedId && event.event === 'ability-deactivate')) {
          await page.keyboard.press(key);
        }
        const deactivated = await waitForGameplayEvent(
          gameplayEvents, cleanupStart,
          event => event.id === expectedId && event.event === 'ability-deactivate',
          { timeoutMs: 5000, pollMs: 100 }
        );
        let fullySettled = false;
        let toggleSettleMs = null;
        let toggleFastForwardInput = null;
        if (deactivated.matched) {
          // ability-settled can arrive in the same render/update burst as the
          // deactivation callback. Start immediately after the matched
          // deactivation event so a one-shot settled signal cannot be lost
          // between the deactivation wait returning and this second wait.
          const settleStart = deactivated.index + 1;
          const fastBefore = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
          let settled;
          try {
            await page.keyboard.down('Shift');
            settled = await waitForGameplayEvent(
              gameplayEvents, settleStart,
              event => event.id === expectedId && event.event === 'ability-settled',
              { timeoutMs: 15000, pollMs: 100 }
            );
          } finally {
            await page.keyboard.up('Shift').catch(() => undefined);
          }
          const fastAfter = await page.evaluate(() => ({ ...(window.__lwjglInputStats || {}) }));
          toggleFastForwardInput = {
            deliveredDelta: Number(fastAfter.directKeyboardDelivered || 0) - Number(fastBefore.directKeyboardDelivered || 0),
            globalDelta: Number(fastAfter.keyboardGlobalCaptures || 0) - Number(fastBefore.keyboardGlobalCaptures || 0),
          };
          fullySettled = Boolean(settled && settled.matched);
          toggleSettleMs = settled ? settled.elapsedMs : 15000;
        }
        const currentResult = abilityKeyResults[abilityKeyResults.length - 1];
        currentResult.toggleDeactivated = deactivated.matched;
        currentResult.toggleSettled = fullySettled;
        currentResult.toggleSettleMs = toggleSettleMs;
        currentResult.toggleFastForwardInput = toggleFastForwardInput;
        if (!deactivated.matched || !fullySettled) currentResult.failed = true;
        logs.push(`[ability-toggle-cleanup] key=${digit} id=${expectedId} deactivated=${deactivated.matched} settled=${fullySettled} settleMs=${toggleSettleMs ?? 'n/a'} fastForwardDelivered=${toggleFastForwardInput?.deliveredDelta ?? 0} fastForwardGlobal=${toggleFastForwardInput?.globalDelta ?? 0} failed=${currentResult.failed}`);
        flushLogs();
        if (currentResult.failed) break;
        await sleep(500);
      }
    }
  }
  abilityKeyResults.sort((a, b) => a.digit - b.digit);
  const abilityKeysSafe = !deepGameplay || expectedState !== 'campaign' || Boolean(
    starterAbilityMappingReady
    && abilityKeyResults.length === 8
    && abilityKeyResults.every(item => !item.failed && item.mapped && item.expectedId === expectedAbilityIds[item.digit - 1])
    && abilityKeyResults.filter(item => item.usable && item.stateChanged).length >= 4
  );

  let gameplayPerformance = null;
  if (deepGameplay && expectedState === 'campaign' && !fatalSeenAt) {
    const perfBefore = await page.evaluate(() => ({ ...(window.__lwjglPresentationStats || {}) }));
    const started = Date.now();
    await page.keyboard.down('w');
    await sleep(2200);
    await page.keyboard.up('w');
    await sleep(5800);
    const perfAfter = await page.evaluate(() => ({ ...(window.__lwjglPresentationStats || {}) }));
    gameplayPerformance = {
      durationMs: Date.now() - started,
      swapDelta: Number(perfAfter.swapCount || 0) - Number(perfBefore.swapCount || 0),
      recentFps: Number(perfAfter.recentFps || 0),
      recentFrameMs: Number(perfAfter.recentFrameMs || 0),
      frameP50Ms: Number(perfAfter.frameP50Ms || 0),
      frameP95Ms: Number(perfAfter.frameP95Ms || 0),
      frameP99Ms: Number(perfAfter.frameP99Ms || 0),
      frameMaxMs: Number(perfAfter.frameMaxMs || 0),
      frameJitterStdDevMs: Number(perfAfter.frameJitterStdDevMs || 0),
      frameJitterP95Ms: Number(perfAfter.frameJitterP95Ms || 0),
      longFrameDelta: Number(perfAfter.longFrameCount || 0) - Number(perfBefore.longFrameCount || 0),
      droppedFrameEstimateDelta: Number(perfAfter.droppedFrameEstimate || 0) - Number(perfBefore.droppedFrameEstimate || 0),
      webglDrawDelta: Number(perfAfter.webglDrawCalls || 0) - Number(perfBefore.webglDrawCalls || 0),
      quadBatchDelta: Number(perfAfter.quadBatches || 0) - Number(perfBefore.quadBatches || 0),
      quadCountDelta: Number(perfAfter.quadQuads || 0) - Number(perfBefore.quadQuads || 0),
      quadDrawCallsSavedDelta: Number(perfAfter.quadDrawCallsSaved || 0) - Number(perfBefore.quadDrawCallsSaved || 0),
    };
    gameplayPerformance.responsive = gameplayPerformance.swapDelta >= 20 && gameplayPerformance.recentFps >= 2;
    logs.push(`[gameplay-performance] durationMs=${gameplayPerformance.durationMs} swaps=${gameplayPerformance.swapDelta} fps=${gameplayPerformance.recentFps.toFixed(2)} frameMs=${gameplayPerformance.recentFrameMs.toFixed(2)} p50=${gameplayPerformance.frameP50Ms.toFixed(2)} p95=${gameplayPerformance.frameP95Ms.toFixed(2)} p99=${gameplayPerformance.frameP99Ms.toFixed(2)} max=${gameplayPerformance.frameMaxMs.toFixed(2)} jitterStdDev=${gameplayPerformance.frameJitterStdDevMs.toFixed(2)} jitterP95=${gameplayPerformance.frameJitterP95Ms.toFixed(2)} longFrames=${gameplayPerformance.longFrameDelta} droppedEstimate=${gameplayPerformance.droppedFrameEstimateDelta} webglDraws=${gameplayPerformance.webglDrawDelta} quadBatches=${gameplayPerformance.quadBatchDelta} quads=${gameplayPerformance.quadCountDelta} drawCallsSaved=${gameplayPerformance.quadDrawCallsSavedDelta} responsive=${gameplayPerformance.responsive}`);
  }
  const gameplayPerformanceSafe = !deepGameplay || expectedState !== 'campaign' || Boolean(gameplayPerformance?.responsive);

  const state = await withTimeout(page.evaluate(() => ({
    runtime: window.__STARSECTOR_RUNTIME_STATE__ || null,
    bodyState: document.body.dataset.runtimeState || '',
    bodyDetail: document.body.dataset.runtimeDetail || '',
    button: document.getElementById('startBtn')?.textContent || '',
    nativeStats: window.__lwjglNativeStats || null,
    presentationStats: window.__lwjglPresentationStats || null,
    inputStats: window.__lwjglInputStats || null,
    bootTiming: window.__STARSECTOR_BOOT_TIMING__ || null,
    webglState: (() => {
      if (window.__lwjglGraphicsInfo) return window.__lwjglGraphicsInfo;
      const canvas = window.lwjglCanvasElement || document.getElementById('lwjglCanvas') || document.querySelector('#game-container canvas');
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
          contextAttributes: gl.getContextAttributes(),
          version: gl.getParameter(gl.VERSION),
          shadingLanguageVersion: gl.getParameter(gl.SHADING_LANGUAGE_VERSION),
          vendor: gl.getParameter(gl.VENDOR),
          renderer: gl.getParameter(gl.RENDERER),
          maxTextureSize: gl.getParameter(gl.MAX_TEXTURE_SIZE),
          maxVertexAttribs: gl.getParameter(gl.MAX_VERTEX_ATTRIBS),
          maxCombinedTextureUnits: gl.getParameter(gl.MAX_COMBINED_TEXTURE_IMAGE_UNITS),
          maxDrawBuffers: gl.getParameter(gl.MAX_DRAW_BUFFERS),
          maxSamples: gl.getParameter(gl.MAX_SAMPLES),
          extensionCount: gl.getSupportedExtensions()?.length || 0,
          extensions: gl.getSupportedExtensions() || []
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
  const firstFramePlayable = expectedState !== 'campaign' || isCampaignFramePlayable(firstStats);
  const secondFramePlayable = expectedState !== 'campaign' || isCampaignFramePlayable(secondStats);
  const firstPlayableFrameAt = expectedState !== 'campaign' ? null
    : (firstFramePlayable ? firstFrameCapturedAt
      : (secondFramePlayable ? secondFrameCapturedAt : null));
  const timeToFirstPlayableFrameMs = firstPlayableFrameAt
    ? firstPlayableFrameAt - navigationStartedAt
    : null;
  const frameChanged = Boolean(firstStats && secondStats && firstStats.sha256 !== secondStats.sha256);
  const rendered = Boolean(secondStats && secondStats.nonBlackRatio > 0.01 && secondStats.variance > 2);
  const campaignTextureRichness = expectedState !== 'campaign' || Boolean(secondStats
    && secondStats.quantizedColorCount >= 200
    && secondStats.variance >= 400);
  // The starter ship can rotate between captures, changing how much warm engine
  // glow lands inside the 96px center box. Accept either independently captured
  // frame, while still requiring the original warm signature or a substantially
  // larger bright centered subject. Texture richness/variance remain separate gates.
  const campaignCenterGate = campaignCenterSubjectGate(expectedState, firstStats, secondStats);
  const campaignCenterSubjectFirst = campaignCenterGate.first;
  const campaignCenterSubjectSecond = campaignCenterGate.second;
  const campaignCenterSubject = campaignCenterGate.overall;
  const browserTutorialVisual = expectedState === 'campaign'
    && !deepGameplay
    && windowConfig.__STARSECTOR_BROWSER_TUTORIAL__ === true;
  // The stock tutorial owns the opening composition and may present an almost
  // full-screen tutorial layer instead of the mature campaign's centered starter
  // ship. Keep strong render/readability/richness checks, but do not require the
  // mature center-ship signature while tutorial mode is explicitly active.
  const tutorialFrameQuality = stats => Boolean(stats
    && stats.nonBlackRatio > 0.075
    && stats.darkRatio < 0.97
    && stats.midToneRatio > 0.025
    && stats.quantizedColorCount >= 150
    && stats.variance >= 400);
  const tutorialVisualQuality = !browserTutorialVisual
    || tutorialFrameQuality(firstStats)
    || tutorialFrameQuality(secondStats);
  const campaignVisualQuality = expectedState !== 'campaign' || (browserTutorialVisual
    ? tutorialVisualQuality
    : Boolean(secondStats
      && secondStats.nonBlackRatio > 0.03
      && secondStats.darkRatio < 0.94
      && secondStats.midToneRatio > 0.025
      && campaignTextureRichness
      && campaignCenterSubject));
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
  const directNewCampaign = expectedState === 'campaign'
    && String(windowConfig.__STARSECTOR_AUTO_CAMPAIGN_MODE__ || '').toLowerCase().includes('new');
  const startingResourcesReady = !directNewCampaign || Boolean(
    startingSuppliesReady
    && startingSuppliesTarget > 0
    && startingSuppliesAfter >= startingSuppliesTarget - 0.1
    && (!deepGameplay || (playableResources
      && playableResources.ready
      && playableResources.fuel > 0
      && playableResources.crew + 0.1 >= playableResources.minCrew
      && (playableResources.maxCargo <= 0 || playableResources.cargoUsed <= playableResources.maxCargo + 0.1)
      && playableResources.credits >= 1999))
  );
  let saveLoadSmoke = {
    enabled: saveLoadSmokeEnabled,
    attempted: false,
    ok: !saveLoadSmokeEnabled,
    reason: saveLoadSmokeEnabled ? 'not-run' : 'disabled',
    firstWorld: null,
    loadedWorld: null,
    loadedBodyState: null,
    fallbackNewGameSeen: false,
    fatalSeen: false,
    elapsedMs: null,
  };
  if (saveLoadSmokeEnabled) {
    const worldPattern = /Fixer: auto campaign world-ready systems=(\d+)\s+planets=(\d+)\s+markets=(\d+)\s+factions=(\d+)/i;
    const firstWorldMatch = [...logs].reverse()
      .map(line => line.match(worldPattern))
      .find(Boolean);
    const firstWorld = firstWorldMatch ? {
      systems: Number(firstWorldMatch[1]),
      planets: Number(firstWorldMatch[2]),
      markets: Number(firstWorldMatch[3]),
      factions: Number(firstWorldMatch[4]),
    } : null;
    saveLoadSmoke.firstWorld = firstWorld;

    if (expectedState !== 'campaign') {
      saveLoadSmoke.reason = 'save/load smoke requires campaign state';
    } else if (!campaign || !firstWorld || fatalSeenAt || errors.length > 0) {
      saveLoadSmoke.reason = 'first launch was not a clean full campaign';
    } else {
      saveLoadSmoke.attempted = true;
      const reloadLogs = [];
      const reloadErrors = [];
      let reloadFatal = false;
      let reloadFallbackNewGame = false;
      let loadedWorld = null;
      let loadedBodyState = '';
      const reloadStartedAt = Date.now();
      const reloadConfig = {
        ...windowConfig,
        // Keep the optimized direct-resource path, but make Continue the only
        // allowed campaign transition. There is deliberately no "new" token,
        // so a missing/corrupt save cannot be hidden by creating another world.
        __STARSECTOR_AUTO_CAMPAIGN_MODE__: 'continue_direct',
        __STARSECTOR_DIRECT_LAUNCH__: true,
        __STARSECTOR_FORCE_CHEERPJ_STORAGE_RESET__: false,
        __STARSECTOR_AUTO_CAMPAIGN_FALLBACK_MS__: 300000,
        __STARSECTOR_BROWSER_TUTORIAL__: false,
        __STARSECTOR_BROWSER_GAMEPLAY_PROBE__: false,
      };

      await sleep(1500);
      await withTimeout(page.close(), 10000, 'first save/load-smoke page close').catch(error => {
        reloadErrors.push(`first-page-close: ${error.message || error}`);
      });
      const reloadPage = await context.newPage();
      reloadPage.setDefaultTimeout(10000);
      reloadPage.setDefaultNavigationTimeout(60000);
      await reloadPage.addInitScript(config => {
        for (const [key, value] of Object.entries(config)) window[key] = value;
      }, reloadConfig);
      reloadPage.on('console', message => {
        const text = message.text();
        reloadLogs.push(`[${message.type()}] ${text}`);
        const world = text.match(worldPattern);
        if (world) {
          loadedWorld = {
            systems: Number(world[1]),
            planets: Number(world[2]),
            markets: Number(world[3]),
            factions: Number(world[4]),
          };
        }
        if (/Fixer: direct-new-game stage=invoke-create(?:\s|$)|Fixer: auto campaign data prepared/i.test(text)) {
          reloadFallbackNewGame = true;
        }
        if (/fatal\s+starsector\s+null|NullPointerException|Exception in thread|(?:^|\b)Fatal\s*:\s*|auto campaign aborting/i.test(text)) {
          reloadFatal = true;
        }
      });
      reloadPage.on('pageerror', error => {
        reloadErrors.push(String(error && (error.stack || error.message) || error));
      });

      const reloadTarget = `${baseUrl}?autostart=1&saveLoadSmoke=1&ci=${Date.now()}`;
      reloadLogs.push(`[save-load-smoke] opening ${reloadTarget}`);
      reloadLogs.push(`[save-load-smoke] config=${JSON.stringify(reloadConfig)}`);
      try {
        await reloadPage.goto(reloadTarget, { waitUntil: 'domcontentloaded', timeout: 60000 });
        const reloadDeadline = Date.now() + saveLoadSmokeTimeoutMs;
        while (Date.now() < reloadDeadline && !reloadFatal && !reloadFallbackNewGame && reloadErrors.length === 0) {
          loadedBodyState = await withTimeout(
            reloadPage.evaluate(() => document.body.dataset.runtimeState || ''),
            5000,
            'save/load-smoke runtime-state evaluate'
          ).catch(error => {
            reloadLogs.push(`[diagnostic] ${error.message || error}`);
            return '';
          });
          if (loadedBodyState === 'campaign' && loadedWorld) break;
          if (['main-returned', 'failed', 'fatal'].includes(loadedBodyState)) break;
          await sleep(1000);
        }
      } catch (error) {
        reloadErrors.push(`reload-navigation: ${error.message || error}`);
      }

      const worldMatches = Boolean(loadedWorld
        && loadedWorld.systems === firstWorld.systems
        && loadedWorld.planets === firstWorld.planets
        && loadedWorld.markets === firstWorld.markets
        && loadedWorld.factions === firstWorld.factions);
      const reloadOk = loadedBodyState === 'campaign'
        && worldMatches
        && !reloadFatal
        && !reloadFallbackNewGame
        && reloadErrors.length === 0;
      saveLoadSmoke = {
        enabled: true,
        attempted: true,
        ok: reloadOk,
        reason: reloadOk ? 'continued saved full campaign' : 'saved campaign did not reload cleanly',
        firstWorld,
        loadedWorld,
        loadedBodyState,
        fallbackNewGameSeen: reloadFallbackNewGame,
        fatalSeen: reloadFatal,
        errors: reloadErrors,
        elapsedMs: Date.now() - reloadStartedAt,
      };
      reloadLogs.push(`[save-load-smoke] result=${JSON.stringify(saveLoadSmoke)}`);
      fs.writeFileSync(`${outputDir}/save-load-smoke.log`, reloadLogs.join('\n'));
      await withTimeout(reloadPage.close(), 10000, 'save/load-smoke reload page close').catch(() => undefined);
    }
  }

  const ok = reachedExpected && rendered && campaignVisualQuality && progressing
    && inputResponsive && uiControlsSafe && shortcutsResponsive && startingResourcesReady
    && abilityKeysSafe && gameplayPerformanceSafe
    && immediateBridgeEfficient && errors.length === 0 && runtimeErrorSignals.length === 0 && !fatalSeenAt
    && graphicsErrors.length === 0
    && disallowedRecovery.length === 0
    && screenshotErrors.length === 0
    && saveLoadSmoke.ok
    && !['main-returned', 'failed', 'fatal', 'unresponsive'].includes(state.bodyState);

  const result = {
    ok,
    target,
    expectedState,
    windowConfig,
    campaign,
    campaignSeenAt,
    navigationStartedAt,
    timeToCampaignMs: campaignSeenAt ? campaignSeenAt - navigationStartedAt : null,
    firstFrameCapturedAt,
    secondFrameCapturedAt,
    firstFramePlayable,
    secondFramePlayable,
    firstPlayableFrameAt,
    timeToFirstPlayableFrameMs,
    title,
    titleSeenAt,
    timeToTitleMs: titleSeenAt ? titleSeenAt - navigationStartedAt : null,
    fatalSeenAt,
    disallowedRecoverySeenAt,
    disallowedRecovery,
    rendered,
    campaignTextureRichness,
    campaignCenterSubjectFirst,
    campaignCenterSubjectSecond,
    campaignCenterSubject,
    browserTutorialVisual,
    tutorialVisualQuality,
    campaignVisualQuality,
    inputKeyboardResponsive,
    inputMouseResponsive,
    inputResponsive,
    uiControlsSafe,
    uiControlResults,
    shortcutsResponsive,
    shortcutResults,
    deepGameplay,
    abilityKeysSafe,
    abilityKeyResults,
    gameplayPerformanceSafe,
    gameplayPerformance,
    shortcutBefore,
    shortcutAfter,
    jarPackResponses,
    jarPackResponseBytes,
    bootTiming: state.bootTiming || null,
    startingResourcesReady,
    startingSuppliesTarget,
    startingSuppliesAfter,
    startingSuppliesReady,
    playableResources,
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
    runtimeErrorSignals: [...new Set(runtimeErrorSignals)],
    gameplayEvents,
    starterAbilityMappingReady,
    starterAbilitySlots,
    httpErrors: [...new Set(httpErrors)],
    localNegativeMisses: [...new Set(localNegativeMisses)],
    saveLoadSmoke,
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
