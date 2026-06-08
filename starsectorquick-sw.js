const PROJECT_PREFIX = '/starsectorquick/';
const LEGACY_REWRITES = [
  ['/starsector/starsector/', `${PROJECT_PREFIX}starsector/starsector/`],
  ['/starfarer.res/res/', `${PROJECT_PREFIX}starsector/starsector/`]
];

self.addEventListener('install', event => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener('activate', event => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;

  const rewrite = LEGACY_REWRITES.find(([from]) => url.pathname.startsWith(from));
  if (!rewrite) return;

  const [from, to] = rewrite;
  const target = new URL(event.request.url);
  target.pathname = to + url.pathname.slice(from.length);

  event.respondWith(fetch(new Request(target.toString(), event.request)));
});