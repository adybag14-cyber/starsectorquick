# Vertex-array write coalescing: bounded experiment

Parent: `3703bd9b79327370ddc1ca7ef9d36277f444eaab` (round 4).
This changes no production setting and authorizes no deployment.

## Why investigate this

The previous qualified GitHub artifact (run 36343404961) contains a 4.06-second
observer-heavy WebGL trace with 46,236 draw calls, 138,708 vertex-array enables,
and 22,865 vertex-array disables. The main-thread profile includes these calls;
this is a measured optimization lead, not a claim that their inclusive timings
can be subtracted from frame time.

The source repeatedly enables all three immediate-mode attribute arrays on each
draw, even when their state has not changed. Current bridge code owns one default
VAO and three linked-program slots. A source assertion rejects adding a second
VAO without extending the ownership model.

## Candidate

The opt-in `__LWJGL_VERTEX_ARRAY_CACHE__ === true` path omits only duplicate
`enableVertexAttribArray`/`disableVertexAttribArray` requests for the three owned
slots. All eight bridge call sites pass through one helper. All vertex-pointer,
buffer, shader, constant-value and draw operations remain unchanged. Unknown or
invalid indices still reach WebGL validation; exceptions do not seed the mirror.
Context-loss/restoration events and explicit invalidation clear the mirror.

This is not a universal WebGL state cache: external direct mutations or a new VAO
must invalidate or extend the bridge ownership model. The helper is OFF unless
explicitly enabled. The existing software-driver CI profile, depth mirror setting,
resolution, rendering content, and performance thresholds are unchanged.

## Tests

The real-WebGL corpus compares actual driver flags and raster pixels with the
cache on and off: 2,540 checks, 524,288 equal raster bytes, 27 distinct pixel
witnesses, invalid-index forwarding, known local exception propagation,
context-restoration invalidation and dynamic toggling. The repeated-state fixture
omits 1,000 redundant writes without omitting drawing.

Older renderer fixtures load the real new helper as a dependency, not a mock
implementation; their original assertions and uncached call counts are retained.
The original revision remains a valid control through optional source extraction.

## Measurement plan, specified before hosted results

Eight counterbalanced ABBABAAB windows in one real campaign/browser, 24 warm-up
frames after each switch and 240 recorded presentation intervals per window.
Both arms use the same explicit software driver, color/depth settings, scene and
resolution. The only toggle is vertex-array write coalescing. Request, forwarded
and coalesced counters must reconcile for every window. Both branches must run.
Raw samples, window medians, pooled distributions, p95/p99/jitter and worst
intervals remain in evidence even when the main gameplay gate fails.

No profiling, screenshots or resource fetching are inserted into timing windows.
The existing four-way experiments and earlier color/depth numbers are not relabelled
as this experiment's results. A controlled pair measures coalescing within the
instrumented candidate, not the total change against another commit/CPU/world.

No default promotion based on a single passing run or isolated favorable metric.
Retain the fallback and keep the option OFF if the throughput/tail tradeoff is
unclear. The next review considers FPS, p95, p99, jitter, worst stalls and functional
coverage together. Frame intervals are application presentation, not physical
scan-out or input latency. No 60-FPS claim is intended.

## Primary references

- MDN WebGL best practices: avoid redundant VAO mutations (queried 2026-09-27):
  https://developer.mozilla.org/en-US/docs/Web/API/WebGL_API/WebGL_best_practices
- Chromium SwiftShader driver/fallback distinction:
  https://chromium.googlesource.com/chromium/src/+/main/docs/gpu/swiftshader.md

Final test verdicts and artifact identities are recorded in PR #183. An earlier
successful head is not certification of this candidate.

## Local browser-control observation

The unchanged round-4 two-profile smoke test reported CONTEXT_LOST_WEBGL (37442)
on the local fallback profile after four RAF callbacks, before loading any
candidate renderer. A bounded two-profile diagnostic reproduced that loss while
the explicit software-driver profile retained its context and exact pixel. Both
failures are retained; no test assertion or CI driver policy is weakened. Hosted
qualification must still run the original two-profile contract.
