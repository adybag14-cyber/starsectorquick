import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';

const url = process.argv[2];
const outDir = path.resolve(process.argv[3] || 'campaign-check');
const durationMs = Number(process.argv[4] || 150000);
if (!url) throw new Error('usage: node verify_campaign.mjs <url> <out-dir> [duration-ms]');
fs.mkdirSync(outDir, { recursive: true });

const lines = [];
let maxDisplayUpdate = 0;
let swapBegin = 0;
let swapEnd = 0;
let asyncPresent = 0;
let campaignSeen = false;
let fatalSeen = false;

function record(kind, text) {
  const line = `[${new Date().toISOString()}] ${kind}: ${text}`;
  lines.push(line);
  process.stdout.write(line + '\n');
  const m = text.match(/Bridge Display\.update(?:\([^)]*\))? count=(\d+)/);
  if (m) maxDisplayUpdate = Math.max(maxDisplayUpdate, Number(m[1]));
  const sb = text.match(/\[SWAP\] (?:sync begin|async request|noop) count=(\d+)/);
  if (sb) swapBegin = Math.max(swapBegin, Number(sb[1]));
  const se = text.match(/\[SWAP\] sync end count=(\d+)/);
  if (se) swapEnd = Math.max(swapEnd, Number(se[1]));
  const ap = text.match(/\[SWAP\] async present count=(\d+)/);
  if (ap) asyncPresent = Math.max(asyncPresent, Number(ap[1]));
  if (/watcher state=Campaign State|reached Campaign State|Main campaign view/i.test(text)) campaignSeen = true;
  if (/Fatal campaign bootstrap failure|auto campaign aborting|Runtime failure:/i.test(text)) fatalSeen = true;
}

const browser = await chromium.launch({
  headless: true,
  args: [
    '--enable-webgl',
    '--ignore-gpu-blocklist',
    '--use-gl=angle',
    '--use-angle=swiftshader',
    '--disable-dev-shm-usage'
  ]
});
const context = await browser.newContext({ viewport: { width: 1280, height: 1100 } });
const page = await context.newPage();
page.setDefaultTimeout(10000);
page.on('console', msg => record(`console.${msg.type()}`, msg.text()));
page.on('pageerror', err => record('pageerror', err.stack || err.message || String(err)));
page.on('requestfailed', req => record('requestfailed', `${req.url()} :: ${req.failure()?.errorText || 'unknown'}`));
page.on('response', response => {
  if (response.status() >= 400) record('http', `${response.status()} ${response.url()}`);
});

let gotoError = null;
try {
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 120000 });
} catch (err) {
  gotoError = err.stack || err.message || String(err);
  record('goto-error', gotoError);
}

await new Promise(resolve => setTimeout(resolve, durationMs));

let runtimeState = null;
let runtimeDetail = null;
let stats = null;
try {
  runtimeState = await page.locator('body').getAttribute('data-runtime-state', { timeout: 8000 });
  runtimeDetail = await page.locator('body').getAttribute('data-runtime-detail', { timeout: 8000 });
} catch (err) {
  record('state-error', err.message || String(err));
}
try {
  stats = await page.evaluate(() => {
    const s = window.__lwjglNativeStats || null;
    if (!s) return null;
    return {
      totalCalls: s.totalCalls || 0,
      lastName: s.lastName || null,
      lastAt: s.lastAt || 0,
      errorsByName: s.errorsByName || {},
      callsByName: s.callsByName || {}
    };
  });
} catch (err) {
  record('stats-error', err.message || String(err));
}

let screenshotOk = false;
try {
  const target = page.locator('#game-container');
  await target.screenshot({ path: path.join(outDir, 'game.png'), timeout: 15000 });
  screenshotOk = true;
} catch (err) {
  record('screenshot-error', err.message || String(err));
}

const result = {
  url,
  durationMs,
  gotoError,
  runtimeState,
  runtimeDetail,
  maxDisplayUpdate,
  swapBegin,
  swapEnd,
  asyncPresent,
  campaignSeen,
  fatalSeen,
  screenshotOk,
  stats
};
fs.writeFileSync(path.join(outDir, 'console.log'), lines.join('\n') + '\n');
fs.writeFileSync(path.join(outDir, 'result.json'), JSON.stringify(result, null, 2));
console.log('RESULT_JSON=' + JSON.stringify(result));
await browser.close();
