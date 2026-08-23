const ALIAS_WORKER_VERSION = '20260823-campaign-space-v9';
const PROJECT_PREFIX = '/starsectorquick/';
const CONTENT_RUNTIME_PREFIX = `${PROJECT_PREFIX}starsector/starsector/`;
const LEGACY_REWRITES = [
  ['/starsector/starsector/', `${PROJECT_PREFIX}starsector/starsector/`],
  ['/starfarer.res/res/', `${PROJECT_PREFIX}starsector/starsector/`]
];
const RANGE_NORMALIZED_PREFIXES = [
  `${PROJECT_PREFIX}starsector/starsector/data/`
];
const TEXT_LIKE_DATA_FILE = /\.(csv|faction|fnt|json|layout|list|proj|ship|skin|system|txt|variant|wpn)$/i;
const JAR_RANGE_PREFIX = `${PROJECT_PREFIX}jars/`;
const JAR_FILE = /\.jar$/i;
const INDEX_LIST_FILE = /(?:^|\/)index\.list$/i;
const TRANSIENT_FETCH_STATUS = new Set([408, 425, 429, 500, 502, 503, 504]);
const FULL_BODY_RETRY_DELAYS_MS = [100, 300, 800];
const DATA_PACK_INDEX_URL = `${PROJECT_PREFIX}starsector-data-pack-v1.json?sw=${ALIAS_WORKER_VERSION}`;
const DATA_PACK_BINARY_URL = `${PROJECT_PREFIX}starsector-data-pack-v1.bin?sw=${ALIAS_WORKER_VERSION}`;
const GRAPHICS_PACK_INDEX_URL = `${PROJECT_PREFIX}starsector-graphics-pack-v1.json?sw=${ALIAS_WORKER_VERSION}`;
const GRAPHICS_PACK_DIR = `${PROJECT_PREFIX}starsector-graphics-pack-v1/`;
const GRAPHICS_RUNTIME_PREFIX = `${PROJECT_PREFIX}starsector/starsector/graphics/`;
const JAR_PACK_INDEX_URL = `${PROJECT_PREFIX}starsector-jar-pack-v1.json?sw=${ALIAS_WORKER_VERSION}`;
const JAR_PACK_BINARY_URL = `${PROJECT_PREFIX}starsector-jar-pack-v1.bin?sw=${ALIAS_WORKER_VERSION}`;
const rangeBodyCache = new Map();
let dataPackPromise = null;
let jarPackPromise = null;
let graphicsPackIndexPromise = null;
const graphicsPackPromises = new Map();

self.addEventListener('install', event => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener('activate', event => {
  event.waitUntil(self.clients.claim());
});

const parseByteRange = (rangeHeader, size) => {
  const match = /^bytes=(\d*)-(\d*)$/.exec(rangeHeader || '');
  if (!match) return null;

  let start;
  let end;
  if (match[1] === '') {
    const suffixLength = Number.parseInt(match[2], 10);
    if (!Number.isFinite(suffixLength) || suffixLength <= 0) return null;
    start = Math.max(size - suffixLength, 0);
    end = size - 1;
  } else {
    start = Number.parseInt(match[1], 10);
    end = match[2] === '' ? size - 1 : Number.parseInt(match[2], 10);
  }

  if (!Number.isFinite(start) || !Number.isFinite(end) || start < 0 || end < start || start >= size) {
    return null;
  }
  return { start, end: Math.min(end, size - 1) };
};

const copyTextHeaders = (upstream, length, retries = 0) => {
  const headers = new Headers();
  for (const name of ['cache-control', 'content-type', 'etag', 'expires', 'last-modified']) {
    const value = upstream.headers.get(name);
    if (value) headers.set(name, value);
  }
  headers.set('accept-ranges', 'bytes');
  headers.set('content-length', String(length));
  headers.set('x-starsectorquick-sw-version', ALIAS_WORKER_VERSION);
  headers.set('x-starsectorquick-sw-retries', String(retries));
  return headers;
};

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const loadDataPack = () => {
  if (dataPackPromise) return dataPackPromise;
  dataPackPromise = Promise.all([
    fetch(DATA_PACK_INDEX_URL, { cache: 'force-cache', credentials: 'same-origin' }),
    fetch(DATA_PACK_BINARY_URL, { cache: 'force-cache', credentials: 'same-origin' })
  ]).then(async ([indexResponse, binaryResponse]) => {
    if (!indexResponse.ok) throw new Error(`data-pack index HTTP ${indexResponse.status}`);
    if (!binaryResponse.ok) throw new Error(`data-pack binary HTTP ${binaryResponse.status}`);
    const [manifest, body] = await Promise.all([indexResponse.json(), binaryResponse.arrayBuffer()]);
    if (!manifest || manifest.version !== 1 || !manifest.files || typeof manifest.files !== 'object') {
      throw new Error('invalid data-pack manifest');
    }
    if (Number(manifest.bytes) !== body.byteLength) {
      throw new Error(`data-pack length mismatch manifest=${manifest.bytes} body=${body.byteLength}`);
    }
    return { manifest, body };
  });
  return dataPackPromise;
};

const loadJarPack = () => {
  if (jarPackPromise) return jarPackPromise;
  jarPackPromise = Promise.all([
    fetch(JAR_PACK_INDEX_URL, { cache: 'force-cache', credentials: 'same-origin' }),
    fetch(JAR_PACK_BINARY_URL, { cache: 'force-cache', credentials: 'same-origin' })
  ]).then(async ([indexResponse, binaryResponse]) => {
    if (!indexResponse.ok) throw new Error(`jar-pack index HTTP ${indexResponse.status}`);
    if (!binaryResponse.ok) throw new Error(`jar-pack binary HTTP ${binaryResponse.status}`);
    const [manifest, body] = await Promise.all([indexResponse.json(), binaryResponse.blob()]);
    if (!manifest || manifest.version !== 1 || !manifest.jars || typeof manifest.jars !== 'object') {
      throw new Error('invalid jar-pack manifest');
    }
    if (Number(manifest.bytes) !== body.size) {
      throw new Error(`jar-pack length mismatch manifest=${manifest.bytes} body=${body.size}`);
    }
    return { manifest, body };
  }).catch(error => {
    jarPackPromise = null;
    throw error;
  });
  return jarPackPromise;
};

const fetchResponseWithRetry = async (requestUrl, options = {}) => {
  let response = null;
  let lastError = null;
  let retries = 0;
  for (let attempt = 0; attempt <= FULL_BODY_RETRY_DELAYS_MS.length; attempt += 1) {
    const target = new URL(requestUrl.toString(), self.location.origin);
    if (attempt > 0) {
      retries = attempt;
      target.searchParams.set('sw-asset-retry', `${ALIAS_WORKER_VERSION}-${attempt}-${Date.now()}`);
    }
    try {
      response = await fetch(target.toString(), {
        ...options,
        cache: 'no-store',
        redirect: 'follow'
      });
      lastError = null;
    } catch (error) {
      response = null;
      lastError = error;
    }
    if (response && response.ok) return { response, retries };
    if (response && !TRANSIENT_FETCH_STATUS.has(response.status)) return { response, retries };
    if (attempt < FULL_BODY_RETRY_DELAYS_MS.length) await sleep(FULL_BODY_RETRY_DELAYS_MS[attempt]);
  }
  if (!response) throw lastError || new Error(`Unable to fetch ${requestUrl}`);
  return { response, retries };
};

const loadGraphicsPackIndex = () => {
  if (graphicsPackIndexPromise) return graphicsPackIndexPromise;
  graphicsPackIndexPromise = fetchResponseWithRetry(GRAPHICS_PACK_INDEX_URL, {
    method: 'GET',
    credentials: 'same-origin'
  }).then(async ({ response }) => {
    if (!response.ok) throw new Error(`graphics-pack index HTTP ${response.status}`);
    const manifest = await response.json();
    if (!manifest || manifest.version !== 1 || !manifest.files || !manifest.packs) {
      throw new Error('invalid graphics-pack manifest');
    }
    return manifest;
  }).catch(error => {
    graphicsPackIndexPromise = null;
    throw error;
  });
  return graphicsPackIndexPromise;
};

const loadGraphicsPack = (packName, expectedBytes) => {
  if (graphicsPackPromises.has(packName)) return graphicsPackPromises.get(packName);
  const promise = fetchResponseWithRetry(`${GRAPHICS_PACK_DIR}${encodeURIComponent(packName)}?sw=${ALIAS_WORKER_VERSION}`, {
    method: 'GET',
    credentials: 'same-origin'
  }).then(async ({ response, retries }) => {
    if (!response.ok) throw new Error(`graphics-pack ${packName} HTTP ${response.status}`);
    const body = await response.arrayBuffer();
    if (Number(expectedBytes) !== body.byteLength) {
      throw new Error(`graphics-pack ${packName} length mismatch expected=${expectedBytes} body=${body.byteLength}`);
    }
    return { body, retries };
  }).catch(error => {
    graphicsPackPromises.delete(packName);
    throw error;
  });
  graphicsPackPromises.set(packName, promise);
  return promise;
};

const runtimeGraphicsRelativePath = url => {
  if (!url.pathname.startsWith(GRAPHICS_RUNTIME_PREFIX)) return null;
  try {
    const relative = decodeURIComponent(url.pathname.slice(GRAPHICS_RUNTIME_PREFIX.length));
    if (!relative || relative.startsWith('/') || relative.split('/').includes('..')) return null;
    return relative;
  } catch {
    return null;
  }
};

const packedGraphicsHeaders = (entry, length, retries = 0) => {
  const headers = new Headers();
  headers.set('accept-ranges', 'bytes');
  headers.set('cache-control', 'public, max-age=600');
  headers.set('content-length', String(length));
  headers.set('content-type', entry.type || 'application/octet-stream');
  headers.set('x-starsectorquick-sw-version', ALIAS_WORKER_VERSION);
  headers.set('x-starsectorquick-graphics-pack', 'v1');
  headers.set('x-starsectorquick-sw-retries', String(retries));
  return headers;
};

const respondFromGraphicsPack = async (url, request) => {
  const relative = runtimeGraphicsRelativePath(url);
  if (!relative) return null;
  let manifest;
  try {
    manifest = await loadGraphicsPackIndex();
  } catch {
    return null;
  }
  const entry = manifest.files[relative];
  if (!entry) return null;
  const packMeta = manifest.packs[entry.pack];
  if (!packMeta) return null;

  let pack;
  try {
    pack = await loadGraphicsPack(entry.pack, packMeta.bytes);
  } catch {
    return null;
  }
  const offset = Number(entry.offset);
  const length = Number(entry.length);
  if (!Number.isSafeInteger(offset) || !Number.isSafeInteger(length) || offset < 0 || length < 0 || offset + length > pack.body.size) {
    return null;
  }

  if (request.method === 'HEAD') {
    return new Response(null, { status: 200, statusText: 'OK', headers: packedGraphicsHeaders(entry, length, pack.retries) });
  }
  const rangeHeader = request.headers.get('range');
  if (rangeHeader) {
    const range = parseByteRange(rangeHeader, length);
    if (!range) {
      return new Response(null, {
        status: 416,
        statusText: 'Range Not Satisfiable',
        headers: {
          'content-range': `bytes */${length}`,
          'x-starsectorquick-sw-version': ALIAS_WORKER_VERSION,
          'x-starsectorquick-graphics-pack': 'v1'
        }
      });
    }
    const slice = pack.body.slice(offset + range.start, offset + range.end + 1);
    const headers = packedGraphicsHeaders(entry, slice.byteLength, pack.retries);
    headers.set('content-range', `bytes ${range.start}-${range.end}/${length}`);
    return new Response(slice, { status: 206, statusText: 'Partial Content', headers });
  }
  const body = pack.body.slice(offset, offset + length);
  return new Response(body, { status: 200, statusText: 'OK', headers: packedGraphicsHeaders(entry, length, pack.retries) });
};

const runtimeDataRelativePath = url => {
  const prefix = RANGE_NORMALIZED_PREFIXES.find(candidate => url.pathname.startsWith(candidate));
  if (!prefix) return null;
  try {
    const relative = decodeURIComponent(url.pathname.slice(prefix.length));
    if (!relative || relative.startsWith('/') || relative.split('/').includes('..')) return null;
    return relative;
  } catch {
    return null;
  }
};

const packedDataHeaders = (entry, length) => {
  const headers = new Headers();
  headers.set('accept-ranges', 'bytes');
  headers.set('cache-control', 'public, max-age=600');
  headers.set('content-length', String(length));
  headers.set('content-type', entry.type || 'application/octet-stream');
  headers.set('x-starsectorquick-sw-version', ALIAS_WORKER_VERSION);
  headers.set('x-starsectorquick-data-pack', 'v1');
  return headers;
};

const respondFromDataPack = async (url, request) => {
  const relative = runtimeDataRelativePath(url);
  if (!relative) return null;

  let pack;
  try {
    pack = await loadDataPack();
  } catch {
    return null;
  }
  const entry = pack.manifest.files[relative];
  if (!entry) return respondWithKnownMissingRuntimePath(request, 'data-pack-miss');

  const offset = Number(entry.offset);
  const length = Number(entry.length);
  if (!Number.isSafeInteger(offset) || !Number.isSafeInteger(length) || offset < 0 || length < 0 || offset + length > pack.body.byteLength) {
    return null;
  }

  if (request.method === 'HEAD') {
    return new Response(null, { status: 200, statusText: 'OK', headers: packedDataHeaders(entry, length) });
  }

  const rangeHeader = request.headers.get('range');
  if (rangeHeader) {
    const range = parseByteRange(rangeHeader, length);
    if (!range) {
      return new Response(null, {
        status: 416,
        statusText: 'Range Not Satisfiable',
        headers: {
          'content-range': `bytes */${length}`,
          'x-starsectorquick-sw-version': ALIAS_WORKER_VERSION,
          'x-starsectorquick-data-pack': 'v1'
        }
      });
    }
    const slice = pack.body.slice(offset + range.start, offset + range.end + 1);
    const headers = packedDataHeaders(entry, slice.byteLength);
    headers.set('content-range', `bytes ${range.start}-${range.end}/${length}`);
    return new Response(slice, { status: 206, statusText: 'Partial Content', headers });
  }

  const body = pack.body.slice(offset, offset + length);
  return new Response(body, { status: 200, statusText: 'OK', headers: packedDataHeaders(entry, length) });
};

const runtimeJarRelativePath = url => {
  if (!url.pathname.startsWith(JAR_RANGE_PREFIX) || !JAR_FILE.test(url.pathname)) return null;
  try {
    const relative = decodeURIComponent(url.pathname.slice(JAR_RANGE_PREFIX.length));
    if (!relative || relative.startsWith('/') || relative.split('/').includes('..')) return null;
    return relative;
  } catch {
    return null;
  }
};

const packedJarHeaders = (entry, length) => {
  const headers = new Headers();
  headers.set('accept-ranges', 'bytes');
  headers.set('cache-control', 'public, max-age=600');
  headers.set('content-length', String(length));
  headers.set('content-type', entry.type || 'application/java-archive');
  if (entry.sha256) headers.set('etag', `\"jarpack-${entry.sha256}\"`);
  headers.set('x-starsectorquick-sw-version', ALIAS_WORKER_VERSION);
  headers.set('x-starsectorquick-jar-pack', 'v1');
  return headers;
};

const respondFromJarPack = async (url, request) => {
  const relative = runtimeJarRelativePath(url);
  if (!relative) return null;
  let pack;
  try {
    pack = await loadJarPack();
  } catch {
    return null;
  }
  const entry = pack.manifest.jars[relative];
  if (!entry) return null;
  const offset = Number(entry.offset);
  const length = Number(entry.length);
  if (!Number.isSafeInteger(offset) || !Number.isSafeInteger(length) || offset < 0 || length <= 0 || offset + length > pack.body.byteLength) {
    return null;
  }
  if (request.method === 'HEAD') {
    return new Response(null, { status: 200, statusText: 'OK', headers: packedJarHeaders(entry, length) });
  }
  const rangeHeader = request.headers.get('range');
  if (rangeHeader) {
    const range = parseByteRange(rangeHeader, length);
    if (!range) {
      return new Response(null, {
        status: 416,
        statusText: 'Range Not Satisfiable',
        headers: {
          'content-range': `bytes */${length}`,
          'x-starsectorquick-sw-version': ALIAS_WORKER_VERSION,
          'x-starsectorquick-jar-pack': 'v1'
        }
      });
    }
    const slice = pack.body.slice(offset + range.start, offset + range.end + 1, entry.type || 'application/java-archive');
    const headers = packedJarHeaders(entry, slice.size);
    headers.set('content-range', `bytes ${range.start}-${range.end}/${length}`);
    return new Response(slice, { status: 206, statusText: 'Partial Content', headers });
  }
  const body = pack.body.slice(offset, offset + length, entry.type || 'application/java-archive');
  return new Response(body, { status: 200, statusText: 'OK', headers: packedJarHeaders(entry, length) });
};

const respondWithPackedJarOrFallback = async (url, request) => {
  const packed = await respondFromJarPack(url, request);
  if (packed) return packed;
  if (shouldProxyJarRange(url, request)) return respondWithJarRange(url, request);
  return fetch(request);
};

const fetchFullBody = async requestUrl => {
  const cacheKey = requestUrl.toString();
  const cached = rangeBodyCache.get(cacheKey);
  if (cached) return cached;

  let upstream = null;
  let lastError = null;
  let retries = 0;
  for (let attempt = 0; attempt <= FULL_BODY_RETRY_DELAYS_MS.length; attempt += 1) {
    const target = new URL(requestUrl.toString());
    if (attempt > 0) {
      retries = attempt;
      target.searchParams.set('sw-body-retry', `${ALIAS_WORKER_VERSION}-${attempt}-${Date.now()}`);
    }
    try {
      upstream = await fetch(target.toString(), {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        redirect: 'follow'
      });
      lastError = null;
    } catch (error) {
      upstream = null;
      lastError = error;
    }

    if (upstream && upstream.ok) break;
    if (upstream && !TRANSIENT_FETCH_STATUS.has(upstream.status)) return { upstream, retries };
    if (attempt < FULL_BODY_RETRY_DELAYS_MS.length) {
      await sleep(FULL_BODY_RETRY_DELAYS_MS[attempt]);
    }
  }

  if (!upstream) throw lastError || new Error(`Unable to fetch ${requestUrl}`);
  if (!upstream.ok) return { upstream, retries };

  const body = await upstream.arrayBuffer();
  const entry = { upstream, body, retries };
  if (body.byteLength < 2 * 1024 * 1024) {
    rangeBodyCache.set(cacheKey, entry);
  }
  return entry;
};

const shouldNormalizeRange = (url, request) => {
  if (!request.headers.has('range')) return false;
  if (!RANGE_NORMALIZED_PREFIXES.some(prefix => url.pathname.startsWith(prefix))) return false;
  return TEXT_LIKE_DATA_FILE.test(url.pathname);
};

const isRuntimeDirectoryProbe = url => {
  if (![...RANGE_NORMALIZED_PREFIXES, GRAPHICS_RUNTIME_PREFIX].some(prefix => url.pathname.startsWith(prefix))) return false;
  const pathname = url.pathname;
  if (pathname.endsWith('/')) return true;
  const leaf = pathname.slice(pathname.lastIndexOf('/') + 1);
  return leaf.length > 0 && !leaf.includes('.');
};

const respondWithKnownMissingRuntimePath = (request, reason) => {
  const headers = new Headers({
    'cache-control': 'no-store',
    'content-type': 'text/plain; charset=utf-8',
    'x-starsectorquick-sw-version': ALIAS_WORKER_VERSION,
    'x-starsectorquick-negative-cache': reason
  });
  const body = request.method === 'HEAD' ? null : 'Not found';
  return new Response(body, { status: 404, statusText: 'Not Found', headers });
};

const respondWithMissingRuntimeDirectory = request => {
  const response = respondWithKnownMissingRuntimePath(request, 'directory-probe');
  response.headers.set('x-starsectorquick-directory-probe', 'direct-404');
  return response;
};

const isKnownMissingRuntimeJavaSource = url => {
  if (!url.pathname.startsWith(CONTENT_RUNTIME_PREFIX)) return false;
  if (url.pathname.startsWith(`${CONTENT_RUNTIME_PREFIX}data/`)) return false;
  return /\.java$/i.test(url.pathname);
};

const shouldNormalizeFullIndexList = (url, request) => {
  if (request.headers.has('range')) return false;
  if (!RANGE_NORMALIZED_PREFIXES.some(prefix => url.pathname.startsWith(prefix))) return false;
  return INDEX_LIST_FILE.test(url.pathname);
};

const shouldProxyJarRange = (url, request) => {
  if (!request.headers.has('range')) return false;
  if (!url.pathname.startsWith(JAR_RANGE_PREFIX)) return false;
  return JAR_FILE.test(url.pathname);
};

const buildJarRangeHeaders = request => {
  const headers = new Headers();
  const range = request.headers.get('range');
  const ifRange = request.headers.get('if-range');
  if (range) headers.set('range', range);
  if (ifRange) headers.set('if-range', ifRange);
  return headers;
};

const fetchJarRange = (url, request, bust) => {
  const target = new URL(url.toString());
  if (bust) target.searchParams.set('sw-range-retry', `${ALIAS_WORKER_VERSION}-${Date.now()}`);
  return fetch(target.toString(), {
    method: request.method,
    headers: buildJarRangeHeaders(request),
    credentials: 'same-origin',
    cache: 'no-store',
    redirect: 'follow'
  });
};

const respondWithJarRange = async (url, request) => {
  let response = await fetchJarRange(url, request, false);
  if (request.method === 'GET' && response.status !== 206) {
    response = await fetchJarRange(url, request, true);
  }
  return response;
};

const respondWithNormalizedRange = async (url, request) => {
  const full = await fetchFullBody(url);
  if (full.upstream && !full.body) return full.upstream;

  if (request.method === 'HEAD') {
    return new Response(null, {
      status: 200,
      statusText: 'OK',
      headers: copyTextHeaders(full.upstream, full.body.byteLength, full.retries)
    });
  }

  const range = parseByteRange(request.headers.get('range'), full.body.byteLength);
  if (!range) {
    return new Response(null, {
      status: 416,
      statusText: 'Range Not Satisfiable',
      headers: { 'content-range': `bytes */${full.body.byteLength}` }
    });
  }

  const slice = full.body.slice(range.start, range.end + 1);
  const headers = copyTextHeaders(full.upstream, slice.byteLength, full.retries);
  headers.set('content-range', `bytes ${range.start}-${range.end}/${full.body.byteLength}`);
  return new Response(slice, {
    status: 206,
    statusText: 'Partial Content',
    headers
  });
};

const respondWithNormalizedFullIndexList = async (url, request) => {
  const full = await fetchFullBody(url);
  if (full.upstream && !full.body) return full.upstream;

  const headers = copyTextHeaders(full.upstream, full.body.byteLength, full.retries);
  return new Response(request.method === 'HEAD' ? null : full.body, {
    status: 200,
    statusText: 'OK',
    headers
  });
};

const fetchRuntimeDataFallback = (url, request) => {
  if (shouldNormalizeRange(url, request)) return respondWithNormalizedRange(url, request);
  if (shouldNormalizeFullIndexList(url, request)) return respondWithNormalizedFullIndexList(url, request);
  return fetch(url.toString(), {
    method: request.method,
    headers: request.headers,
    credentials: request.credentials,
    cache: request.cache,
    redirect: request.redirect,
    referrer: request.referrer,
    referrerPolicy: request.referrerPolicy,
    integrity: request.integrity
  });
};

const respondWithPackedDataOrFallback = async (url, request) => {
  const packed = await respondFromDataPack(url, request);
  return packed || fetchRuntimeDataFallback(url, request);
};

const fetchRuntimeGraphicsFallback = async (url, request) => {
  const headers = new Headers(request.headers);
  const { response } = await fetchResponseWithRetry(url, {
    method: request.method,
    headers,
    credentials: request.credentials
  });
  return response;
};

const respondWithPackedGraphicsOrFallback = async (url, request) => {
  const packed = await respondFromGraphicsPack(url, request);
  return packed || fetchRuntimeGraphicsFallback(url, request);
};

self.addEventListener('message', event => {
  if (!event.data) return;
  if (event.data.type === 'warm-starsector-data-pack' || event.data.type === 'warm-starsector-runtime-packs') {
    event.waitUntil(Promise.all([
      loadDataPack().catch(() => null),
      loadJarPack().catch(() => null),
      loadGraphicsPackIndex().catch(() => null)
    ]));
  }
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET' && event.request.method !== 'HEAD') return;

  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;

  if (runtimeJarRelativePath(url)) {
    event.respondWith(respondWithPackedJarOrFallback(url, event.request));
    return;
  }

  if (isRuntimeDirectoryProbe(url)) {
    event.respondWith(Promise.resolve(respondWithMissingRuntimeDirectory(event.request)));
    return;
  }

  if (isKnownMissingRuntimeJavaSource(url)) {
    event.respondWith(Promise.resolve(respondWithKnownMissingRuntimePath(event.request, 'java-source-outside-data')));
    return;
  }

  if (runtimeDataRelativePath(url)) {
    event.respondWith(respondWithPackedDataOrFallback(url, event.request));
    return;
  }

  if (runtimeGraphicsRelativePath(url)) {
    event.respondWith(respondWithPackedGraphicsOrFallback(url, event.request));
    return;
  }

  if (shouldNormalizeRange(url, event.request)) {
    event.respondWith(respondWithNormalizedRange(url, event.request));
    return;
  }

  if (shouldNormalizeFullIndexList(url, event.request)) {
    event.respondWith(respondWithNormalizedFullIndexList(url, event.request));
    return;
  }

  const rewrite = LEGACY_REWRITES.find(([from]) => url.pathname.startsWith(from));
  if (!rewrite) return;

  const [from, to] = rewrite;
  const target = new URL(event.request.url);
  target.pathname = to + url.pathname.slice(from.length);

  if (isRuntimeDirectoryProbe(target)) {
    event.respondWith(Promise.resolve(respondWithMissingRuntimeDirectory(event.request)));
    return;
  }

  if (isKnownMissingRuntimeJavaSource(target)) {
    event.respondWith(Promise.resolve(respondWithKnownMissingRuntimePath(event.request, 'java-source-outside-data')));
    return;
  }

  if (runtimeDataRelativePath(target)) {
    event.respondWith(respondWithPackedDataOrFallback(target, event.request));
    return;
  }

  if (runtimeGraphicsRelativePath(target)) {
    event.respondWith(respondWithPackedGraphicsOrFallback(target, event.request));
    return;
  }

  if (shouldNormalizeRange(target, event.request)) {
    event.respondWith(respondWithNormalizedRange(target, event.request));
    return;
  }

  if (shouldNormalizeFullIndexList(target, event.request)) {
    event.respondWith(respondWithNormalizedFullIndexList(target, event.request));
    return;
  }

  event.respondWith(fetch(target.toString(), {
    method: event.request.method,
    headers: event.request.headers,
    credentials: event.request.credentials,
    cache: event.request.cache,
    redirect: event.request.redirect,
    referrer: event.request.referrer,
    referrerPolicy: event.request.referrerPolicy,
    integrity: event.request.integrity
  }));
});
