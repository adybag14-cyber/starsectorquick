const ALIAS_WORKER_VERSION = '20260608-range-normalize-v2';
const PROJECT_PREFIX = '/starsectorquick/';
const LEGACY_REWRITES = [
  ['/starsector/starsector/', `${PROJECT_PREFIX}starsector/starsector/`],
  ['/starfarer.res/res/', `${PROJECT_PREFIX}starsector/starsector/`]
];
const RANGE_NORMALIZED_PREFIXES = [
  `${PROJECT_PREFIX}starsector/starsector/data/`
];
const TEXT_LIKE_DATA_FILE = /\.(csv|faction|fnt|json|layout|list|proj|ship|skin|system|txt|variant|wpn)$/i;
const rangeBodyCache = new Map();

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

const copyTextHeaders = (upstream, length) => {
  const headers = new Headers();
  for (const name of ['cache-control', 'content-type', 'etag', 'expires', 'last-modified']) {
    const value = upstream.headers.get(name);
    if (value) headers.set(name, value);
  }
  headers.set('accept-ranges', 'bytes');
  headers.set('content-length', String(length));
  return headers;
};

const fetchFullBody = async requestUrl => {
  const cacheKey = requestUrl.toString();
  const cached = rangeBodyCache.get(cacheKey);
  if (cached) return cached;

  const upstream = await fetch(requestUrl.toString(), {
    method: 'GET',
    credentials: 'same-origin',
    cache: 'no-store',
    redirect: 'follow'
  });
  if (!upstream.ok) return { upstream };

  const body = await upstream.arrayBuffer();
  const entry = { upstream, body };
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

const respondWithNormalizedRange = async (url, request) => {
  const full = await fetchFullBody(url);
  if (full.upstream && !full.body) return full.upstream;

  if (request.method === 'HEAD') {
    return new Response(null, {
      status: 200,
      statusText: 'OK',
      headers: copyTextHeaders(full.upstream, full.body.byteLength)
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
  const headers = copyTextHeaders(full.upstream, slice.byteLength);
  headers.set('content-range', `bytes ${range.start}-${range.end}/${full.body.byteLength}`);
  return new Response(slice, {
    status: 206,
    statusText: 'Partial Content',
    headers
  });
};

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET' && event.request.method !== 'HEAD') return;

  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;

  if (shouldNormalizeRange(url, event.request)) {
    event.respondWith(respondWithNormalizedRange(url, event.request));
    return;
  }

  const rewrite = LEGACY_REWRITES.find(([from]) => url.pathname.startsWith(from));
  if (!rewrite) return;

  const [from, to] = rewrite;
  const target = new URL(event.request.url);
  target.pathname = to + url.pathname.slice(from.length);

  if (shouldNormalizeRange(target, event.request)) {
    event.respondWith(respondWithNormalizedRange(target, event.request));
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