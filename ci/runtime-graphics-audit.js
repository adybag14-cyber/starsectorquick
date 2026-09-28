'use strict';

function graphicsKey(value) {
  try {
    const pathname = decodeURIComponent(new URL(value).pathname);
    const match = pathname.match(/\/graphics\/(.+\.(?:png|jpe?g|gif|bmp|tga))$/i);
    return match ? 'graphics/'+match[1] : null;
  } catch (_) { return null; }
}

function createRuntimeGraphicsAudit(maxImages = 8192) {
  const images = new Map();
  const directFailures = new Set();
  const packFailures = new Set();
  let overflow = false;
  let abortedRequests = 0;
  const entryFor = key => {
    if (!images.has(key)) {
      if (images.size >= maxImages) { overflow = true; return null; }
      images.set(key,{path:key,successfulResponses:0,failedResponses:0,statuses:new Set()});
    }
    return images.get(key);
  };
  return {
    response(url, status) {
      const key = graphicsKey(url);
      if (key) {
        const entry = entryFor(key); if (!entry) return;
        entry.statuses.add(status);
        if (status >= 200 && status < 400) entry.successfulResponses++;
        else if (status >= 400) entry.failedResponses++;
      } else if (status >= 400 && /\/starsector-graphics-pack-v1\/[^?#]+\.bin(?:[?#]|$)/.test(url)) {
        if (packFailures.size < 64) packFailures.add(url.split('?')[0]);
        else overflow = true;
      }
    },
    requestFailed(url, reason) {
      if (!graphicsKey(url)) return;
      // An abandoned over-read is not by itself evidence of a missing image.
      // A real HTTP error is still recorded separately by response().
      if (String(reason).includes('ERR_ABORTED')) { abortedRequests++; return; }
      const entry = entryFor(graphicsKey(url));
      if (entry) { entry.failedResponses++; entry.statuses.add(String(reason).slice(0,100)); }
    },
    console(text) {
      if (/BrowserDeferredTexture: direct-miss-load-failed|deferred texture load failed key=/i.test(text)) {
        if (directFailures.size < 64) directFailures.add(String(text).slice(0,1000));
        else overflow = true;
      }
    },
    summary() {
      const rows = [...images.values()].map(entry=>({...entry,statuses:[...entry.statuses]}));
      const unresolved = rows.filter(entry=>entry.failedResponses && !entry.successfulResponses);
      return {attemptedImages:rows.length,successfulImages:rows.filter(entry=>entry.successfulResponses).length,
        failedResponses:rows.reduce((sum,row)=>sum+row.failedResponses,0),
        recoveredRequestFailures:rows.filter(entry=>entry.failedResponses && entry.successfulResponses),
        unresolvedRequestFailures:unresolved,directLoadFailures:[...directFailures],
        backingPackFailures:[...packFailures],abortedRequests,overflow,
        safe:!overflow && !unresolved.length && !directFailures.size,
        coverage:'Observed campaign/panel/ability requests only; not a claim every packaged sprite was rendered.'};
    },
  };
}
module.exports={graphicsKey,createRuntimeGraphicsAudit};
