const fs = require('fs');
const vm = require('vm');

const source = fs.readFileSync('starsectorquick-sw.js', 'utf8');
const launchSource = fs.readFileSync('launch.html', 'utf8');
const workerVersion = source.match(/ALIAS_WORKER_VERSION\s*=\s*'([^']+)'/);
const launchVersion = launchSource.match(/aliasWorkerVersion\s*=\s*'([^']+)'/);
if (!workerVersion || !launchVersion || workerVersion[1] !== launchVersion[1]) {
  throw new Error(`service worker version mismatch worker=${workerVersion && workerVersion[1]} launch=${launchVersion && launchVersion[1]}`);
}


// The previous epoch can keep a whole old JAR pack alive in a returning user's
// worker and HTTP cache. This runtime update must use distinct asset URLs.
const previousWorkerVersion = '20260927-cheerpj-graphics-v10';
assert(workerVersion[1] !== previousWorkerVersion, 'vertex-batch runtime still uses the previous cached JAR epoch');
const launchFallback = launchSource.match(/const workerVersion = window\.__STARSECTOR_ALIAS_WORKER_VERSION__ \|\| '([^']+)'/);
assert(launchFallback && launchFallback[1] === workerVersion[1], 'launcher JAR-pack fallback version differs from worker');
const registrationExpression = launchSource.match(/const workerUrl = (`[^`]+`);/);
assert(registrationExpression, 'launcher worker registration URL missing');
const siteRootUrl = 'https://adybag14-cyber.github.io/starsectorquick/';
const registrationUrl = vm.runInNewContext(registrationExpression[1], { siteRootUrl, aliasWorkerVersion: workerVersion[1] });
assert(registrationUrl === siteRootUrl + 'starsectorquick-sw.js?v=' + workerVersion[1],
  'worker registration does not select the matching asset epoch');
const resetVersion = launchSource.match(/const resetVersion = '([^']+)'/);
assert(resetVersion && resetVersion[1] === '20260810-campaign-render-v6',
  'asset cache update must preserve the existing saved-storage migration marker');

function collectJavaOutsideData(root) {
  const found = [];
  const visit = dir => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = `${dir}/${entry.name}`;
      if (entry.isDirectory()) {
        visit(full);
      } else if (entry.isFile() && entry.name.toLowerCase().endsWith('.java')) {
        const normalized = full.replace(/\\/g, '/');
        if (!normalized.startsWith('starsector/starsector/data/')) found.push(normalized);
      }
    }
  };
  visit(root);
  return found;
}

const outsideDataJava = collectJavaOutsideData('starsector/starsector');
if (outsideDataJava.length) {
  throw new Error(`Java source outside authoritative data pack would be blocked: ${outsideDataJava.join(', ')}`);
}
const listeners = {};
const unexpectedNetwork = [];
const jarPackRequests = [];
const payload = new TextEncoder().encode('LEVELUP');
const manifest = {
  version: 1,
  bytes: payload.byteLength,
  files: {
    'scripts/plugins/LevelupPluginImpl.java': {
      offset: 0,
      length: payload.byteLength,
      type: 'text/plain; charset=utf-8'
    }
  }
 };
const jarPayload = new TextEncoder().encode('0123456789');
const jarManifest = {
  version: 1,
  bytes: jarPayload.byteLength,
  order: ['bridge.jar'],
  jars: {
    'bridge.jar': {
      offset: 2,
      length: 5,
      sha256: 'synthetic',
      type: 'application/java-archive'
    }
  }
};

const selfObject = {
  location: { origin: 'https://adybag14-cyber.github.io' },
  clients: { claim: async () => {} },
  skipWaiting: async () => {},
  addEventListener(type, handler) { listeners[type] = handler; }
};

async function mockFetch(input, options = {}) {
  const url = String(input instanceof Request ? input.url : input);
  if (/\/starsector-jar-pack-v1\.(?:json|bin)(?:\?|$)/.test(url)) {
    const parsed = new URL(url, selfObject.location.origin);
    jarPackRequests.push({ url: parsed.href, cache: options.cache, credentials: options.credentials });
    assert(parsed.searchParams.get('sw') === workerVersion[1],
      'JAR pack request can reuse another runtime epoch: ' + parsed.href);
  }
  if (url.includes('/starsectorquick/starsector-data-pack-v1.json')) {
    return new Response(JSON.stringify(manifest), {
      status: 200,
      headers: { 'content-type': 'application/json' }
    });
  }
  if (url.includes('/starsectorquick/starsector-data-pack-v1.bin')) {
    return new Response(payload, {
      status: 200,
      headers: { 'content-type': 'application/octet-stream' }
    });
  }
  if (url.includes('/starsectorquick/starsector-jar-pack-v1.json')) {
    return new Response(JSON.stringify(jarManifest), { status: 200, headers: { 'content-type': 'application/json' } });
  }
  if (url.includes('/starsectorquick/starsector-jar-pack-v1.bin')) {
    return new Response(jarPayload, { status: 200, headers: { 'content-type': 'application/octet-stream' } });
  }
  unexpectedNetwork.push(url);
  return new Response('network fallback', { status: 200 });
}

const context = {
  self: selfObject,
  fetch: mockFetch,
  Headers,
  Request,
  Response,
  URL,
  TextEncoder,
  setTimeout,
  clearTimeout,
  console
};
vm.createContext(context);
vm.runInContext(source, context, { filename: 'starsectorquick-sw.js' });

if (typeof listeners.fetch !== 'function') throw new Error('service worker fetch handler not registered');

async function dispatch(url, method = 'GET', headers = {}) {
  let responsePromise = null;
  const request = new Request(url, { method, headers });
  listeners.fetch({
    request,
    respondWith(value) { responsePromise = Promise.resolve(value); }
  });
  return responsePromise ? await responsePromise : null;
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

(async () => {
  const projectRootMiss = await dispatch('https://adybag14-cyber.github.io/starsectorquick/starsector/starsector/Global.java');
  assert(projectRootMiss && projectRootMiss.status === 404, 'project root Java miss was not handled locally');
  assert(projectRootMiss.headers.get('x-starsectorquick-negative-cache') === 'java-source-outside-data', 'project root Java miss reason missing');

  const legacyRootMiss = await dispatch('https://adybag14-cyber.github.io/starsector/starsector/Global.java');
  assert(legacyRootMiss && legacyRootMiss.status === 404, 'legacy root Java miss was not handled locally');
  assert(legacyRootMiss.headers.get('x-starsectorquick-negative-cache') === 'java-source-outside-data', 'legacy root Java miss reason missing');

  const existing = await dispatch('https://adybag14-cyber.github.io/starsectorquick/starsector/starsector/data/scripts/plugins/LevelupPluginImpl.java');
  assert(existing && existing.status === 200, 'real packed Java source was blocked');
  assert(await existing.text() === 'LEVELUP', 'real packed Java source payload mismatch');
  assert(existing.headers.get('x-starsectorquick-data-pack') === 'v1', 'real packed Java source did not come from data pack');

  const dataMiss = await dispatch('https://adybag14-cyber.github.io/starsectorquick/starsector/starsector/data/shipsystems/scripts/String.java');
  assert(dataMiss && dataMiss.status === 404, 'manifest-proven data miss was not handled locally');
  assert(dataMiss.headers.get('x-starsectorquick-negative-cache') === 'data-pack-miss', 'data miss reason missing');

  const legacyDataMiss = await dispatch('https://adybag14-cyber.github.io/starsector/starsector/data/shipsystems/scripts/String.java');
  assert(legacyDataMiss && legacyDataMiss.status === 404, 'legacy manifest-proven data miss was not handled locally');
  assert(legacyDataMiss.headers.get('x-starsectorquick-negative-cache') === 'data-pack-miss', 'legacy data miss reason missing');

  const directoryMiss = await dispatch('https://adybag14-cyber.github.io/starsectorquick/starsector/starsector/data/hulls');
  assert(directoryMiss && directoryMiss.status === 404, 'directory probe no longer handled locally');
  assert(directoryMiss.headers.get('x-starsectorquick-directory-probe') === 'direct-404', 'directory probe compatibility header missing');

  const packedJar = await dispatch('https://adybag14-cyber.github.io/starsectorquick/jars/bridge.jar');
  assert(packedJar && packedJar.status === 200, 'packed JAR full response failed');
  assert(await packedJar.text() === '23456', 'packed JAR full payload mismatch');
  assert(packedJar.headers.get('x-starsectorquick-jar-pack') === 'v1', 'packed JAR marker missing');

  const packedJarRange = await dispatch('https://adybag14-cyber.github.io/starsectorquick/jars/bridge.jar', 'GET', { Range: 'bytes=1-3' });
  assert(packedJarRange && packedJarRange.status === 206, 'packed JAR range response failed');
  assert(await packedJarRange.text() === '345', 'packed JAR range payload mismatch');
  assert(packedJarRange.headers.get('content-range') === 'bytes 1-3/5', 'packed JAR content-range mismatch');
  assert(packedJarRange.headers.get('x-starsectorquick-jar-pack') === 'v1', 'packed JAR range marker missing');

  const packedJarHead = await dispatch('https://adybag14-cyber.github.io/starsectorquick/jars/bridge.jar', 'HEAD');
  assert(packedJarHead && packedJarHead.status === 200, 'packed JAR HEAD response failed');
  assert(packedJarHead.headers.get('content-length') === '5', 'packed JAR HEAD content-length mismatch');
  assert(packedJarHead.headers.get('x-starsectorquick-jar-pack') === 'v1', 'packed JAR HEAD marker missing');

  const packedJarBadRange = await dispatch('https://adybag14-cyber.github.io/starsectorquick/jars/bridge.jar', 'GET', { Range: 'bytes=99-120' });
  assert(packedJarBadRange && packedJarBadRange.status === 416, 'packed JAR invalid range status mismatch');
  assert(packedJarBadRange.headers.get('content-range') === 'bytes */5', 'packed JAR invalid range content-range mismatch');
  assert(packedJarBadRange.headers.get('x-starsectorquick-jar-pack') === 'v1', 'packed JAR invalid range marker missing');

  const expectedJarPackUrls = ['json', 'bin'].map(extension =>
    siteRootUrl + 'starsector-jar-pack-v1.' + extension + '?sw=' + encodeURIComponent(workerVersion[1]));
  assert(jarPackRequests.length === 2, 'JAR manifest/body should load once and remain memoized for range/HEAD requests');
  assert(expectedJarPackUrls.every(url => jarPackRequests.some(request => request.url === url)),
    'worker did not fetch both versioned JAR-pack URLs');
  assert(jarPackRequests.every(request => request.cache === 'force-cache' && request.credentials === 'same-origin'),
    'JAR-pack cache and credential semantics changed');
  assert(packedJar.headers.get('x-starsectorquick-sw-version') === workerVersion[1],
    'JAR response does not identify the current worker epoch');

  assert(unexpectedNetwork.length === 0, `known misses escaped to network: ${unexpectedNetwork.join(', ')}`);
  console.log(`ServiceWorkerNegativeCache: OK version=${workerVersion[1]} versioned-jar-pack, preserved-storage-marker, root-java, legacy-java, packed-java, packed-jar, packed-jar-range, packed-jar-head, packed-jar-416, data-miss, legacy-data-miss, directory-miss`);
})().catch(error => {
  console.error(error && (error.stack || error.message) || error);
  process.exitCode = 1;
});
