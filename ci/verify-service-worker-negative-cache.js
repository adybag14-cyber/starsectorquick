const fs = require('fs');
const vm = require('vm');

const source = fs.readFileSync('starsectorquick-sw.js', 'utf8');
const launchSource = fs.readFileSync('launch.html', 'utf8');
const workerVersion = source.match(/ALIAS_WORKER_VERSION\s*=\s*'([^']+)'/);
const launchVersion = launchSource.match(/aliasWorkerVersion\s*=\s*'([^']+)'/);
if (!workerVersion || !launchVersion || workerVersion[1] !== launchVersion[1]) {
  throw new Error(`service worker version mismatch worker=${workerVersion && workerVersion[1]} launch=${launchVersion && launchVersion[1]}`);
}

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

const selfObject = {
  location: { origin: 'https://adybag14-cyber.github.io' },
  clients: { claim: async () => {} },
  skipWaiting: async () => {},
  addEventListener(type, handler) { listeners[type] = handler; }
};

async function mockFetch(input) {
  const url = String(input instanceof Request ? input.url : input);
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

async function dispatch(url, method = 'GET') {
  let responsePromise = null;
  const request = new Request(url, { method });
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

  assert(unexpectedNetwork.length === 0, `known misses escaped to network: ${unexpectedNetwork.join(', ')}`);
  console.log(`ServiceWorkerNegativeCache: OK version=${workerVersion[1]} root-java, legacy-java, packed-java, data-miss, legacy-data-miss, directory-miss`);
})().catch(error => {
  console.error(error && (error.stack || error.message) || error);
  process.exitCode = 1;
});
