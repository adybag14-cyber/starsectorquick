const fs = require('fs');
const { chromium } = require('playwright');

const baseUrl = process.env.XSTREAM_PROBE_URL || 'http://127.0.0.1:8000/launch.html';
const outDir = process.env.XSTREAM_PROBE_OUTPUT || 'test_output/xstream-microprobe';
fs.mkdirSync(outDir, { recursive: true });

const probes = [
  { main: 'XStreamStaxValueProbe', marker: 'XStreamStaxValueProbe: DONE', timeoutMs: 45000 },
  { main: 'XStreamPostFieldProbe', marker: 'XStreamPostFieldProbe: DONE', timeoutMs: 45000 },
  { main: 'XStreamCampaignEngineFastFieldProbe', marker: 'XStreamCampaignEngineFastFieldProbe: DONE', timeoutMs: 45000 },
  { main: 'XStreamSunUnsafeProbe', marker: 'XStreamSunUnsafeProbe: DONE', timeoutMs: 45000 },
  { main: 'XStreamPureJavaProbe', marker: 'XStreamPureJavaProbe: DONE', timeoutMs: 45000 },
];

async function runProbe(spec) {
  const logs = [];
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] });
  let ok = false;
  let reason = 'timeout';
  try {
    const page = await browser.newPage();
    await page.addInitScript(mainClass => {
      window.__STARSECTOR_MAIN_CLASS__ = mainClass;
      window.__STARSECTOR_AUTO_CAMPAIGN__ = false;
    }, spec.main);
    let resolveMarker;
    const markerPromise = new Promise(resolve => { resolveMarker = resolve; });
    page.on('console', msg => {
      const line = `[${msg.type()}] ${msg.text()}`;
      logs.push(line);
      if (msg.text().includes(spec.marker)) resolveMarker({ ok: true, reason: 'marker' });
    });
    page.on('pageerror', err => {
      logs.push(`[pageerror] ${String(err && (err.stack || err.message) || err)}`);
      resolveMarker({ ok: false, reason: 'pageerror' });
    });
    const target = `${baseUrl}?autostart=1&xstreamProbe=${encodeURIComponent(spec.main)}&ci=${Date.now()}`;
    logs.push(`[probe] opening ${target}`);
    await page.goto(target, { waitUntil: 'domcontentloaded', timeout: 30000 });
    const result = await Promise.race([
      markerPromise,
      new Promise(resolve => setTimeout(() => resolve({ ok: false, reason: 'timeout' }), spec.timeoutMs)),
    ]);
    ok = result.ok;
    reason = result.reason;
  } catch (err) {
    reason = String(err && (err.stack || err.message) || err);
    logs.push(`[probe-error] ${reason}`);
  } finally {
    await Promise.race([browser.close(), new Promise(resolve => setTimeout(resolve, 10000))]).catch(() => undefined);
  }
  fs.writeFileSync(`${outDir}/${spec.main}.log`, logs.join('\n'));
  console.log(`${spec.main}: ok=${ok} reason=${reason}`);
  for (const line of logs.filter(line => /XStream|Starting Main Class|Unsafe JNI fallback|pageerror/i.test(line)).slice(-80)) {
    console.log(line);
  }
  return { main: spec.main, ok, reason };
}

(async () => {
  const results = [];
  for (const spec of probes) results.push(await runProbe(spec));
  fs.writeFileSync(`${outDir}/result.json`, JSON.stringify(results, null, 2));
  if (results.some(r => !r.ok)) process.exitCode = 1;
})().catch(err => {
  console.error(err);
  process.exitCode = 1;
});
