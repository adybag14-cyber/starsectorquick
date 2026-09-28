# Narrow depth-state synchronization investigation

This continues PR #183 without changing the public site or its saved games.
The candidate is not promoted based solely on a profile or a microbenchmark.

## Reproduced bottleneck

A bounded, observer-heavy trace of the existing default renderer on Devbox
measured 1,620 `getParameter(DEPTH_WRITEMASK)` calls over 12,132.2 ms. Those calls
spent approximately 4,841 ms synchronously inside WebGL. The full set of wrapped
GL calls spent approximately 5,570.7 ms. Chrome's matching trace records
`GLES2Implementation::GetBooleanv`, `CommandBufferHelper::Finish` and
`CommandBufferProxyImpl::WaitForGetOffset`: these are synchronous command-buffer
waits, not evidence of useful JavaScript work or harmless idle time.

Trace event durations overlap and are inclusive; they must not be added together.
Instrumentation and tracing change timing. The trace locates the cause of blocking,
but is not itself a headline FPS benchmark. GitHub-runner attribution and the
subsequent unprofiled, same-campaign paired test are recorded separately.

A four-uniform coalescing prototype also passed 264 checks and 524,288 bytes of
raster comparison. It is deliberately not included: `uniform1f` accounted for only
27.7 ms in the same instrumented sample, compared with 4,841 ms for the depth-mask
query. The patch addresses the measured bottleneck rather than accumulating
unrelated optimizations.

## Why this differs from the earlier extended cache

The earlier experiment grouped depth, viewport, stencil and enable snapshots.
It invalidated its depth group on changes and consequently could still request
synchronous driver state repeatedly. It also changed several independent groups
at once and did not show a reliable whole-campaign improvement.

This candidate owns only three depth-buffer values: write mask, comparison
function and clear value. All six native GL mutation sites remain real GL calls.
The mirror is updated after those writes; an unknown enum, NaN or unhandled value
invalidates it and the next snapshot uses the authoritative driver path. The
first snapshot is seeded from actual driver state. Nested snapshots are copies,
not aliases, and restoring an existing snapshot updates the mirror only after
performing the real GL restoration.

The public diagnostic `readExtendedAttribState('depth')` remains an independent
real-driver oracle. Tests must not accidentally compare the cache with itself.
Context-lost/restored and explicit external invalidation clear the mirror.
The static fixture additionally watches the complete set of native depth-write
sites, so adding an unreviewed mutation fails that guard.

Candidate selection is independent of the still-disabled broad cache:
`__LWJGL_DEPTH_STATE_CACHE__ = false` selects the original driver-query path.
RAF and flush pacing remain disabled. No draws, textures, effects, simulation
steps, resolution, frame counters or threshold values are removed or reduced.

## Correctness qualification

`ci/verify-lwjgl-depth-state.js` compares the actual renderer function bodies
against real WebGL2 driver state. It exercises hundreds of writes, Float32
conversion, signed zero, invalid enums, infinities/NaN fallbacks, nested restores,
display-list recording/playback, toggling and context-invalidation handling.
It checks repeated valid mutations followed by snapshots do not cause new driver
reads. It also renders depth-tested overlapping fragments and compares 262,144
output bytes with the original path. Separate clipping, color, extended-state,
frame-ring and fixed-function suites remain mandatory.

The initial local timing harness incorrectly required explicit depth-setter calls
in every sample. A constant-state frame can legitimately use only snapshot and
restore, so that assertion was not a valid product criterion. That failed trial is
retained. The corrected experiment requires real cache hits, zero synchronized
reads in each candidate window, actual original reads in baseline windows, and
records setter and restore counts separately. It does not weaken rendering or
performance limits.

## Performance experiment

The paired test uses one real, running campaign and fixed 1024x768 framebuffer.
Eight counterbalanced ABBABAAB windows each retain 240 consecutive intervals,
following 24 warm-up frames after each mode switch. Only the depth mirror changes;
color caching stays enabled, the broad cache stays disabled and pacing stays
synchronous. Screenshots, CPU profiling and the heavy attribution observer stay
outside the timed windows. Both execution branches are proved by read/hit counts.
Raw intervals, p95/p99, worst frame and jitter must be retained, even when they
conflict with an attractive mean FPS. Cleanup restores the original presence and
values of experiment flags.

The normal default gameplay gate still requires at least 8 FPS, p95 no more than
150 ms, p99 no more than 220 ms and p95 deviation-from-median jitter no more than
60 ms on the existing runner workload. Passing the paired experiment is not a
substitute for passing this gate, the tutorial, Full-HD, image-coverage and
save/Continue tests. Presentation intervals are not GPU-completion, display
scan-out or end-to-end input latency; removing synchronization can alter queuing.

## Reproduction

```text
node ci/test-stall-attribution.js
node ci/verify-lwjgl-depth-state.js
node ci/verify-lwjgl-color-attrib-cache.js
node ci/verify-lwjgl-extended-attrib-cache.js
node ci/verify-lwjgl-clipping-state.js
node ci/test-renderer-experiment-state.js
python ci/summarize-stall-attribution.py <runner-diagnostics-directory>
```

For heavy attribution set `STARSECTOR_STALL_ATTRIBUTION=true` only on the separate
diagnostic run. For unprofiled paired timing use `STARSECTOR_DEPTH_PAIRED=true` and
leave attribution off. Final source identity, hosted results, raw intervals and
artifacts are recorded in the PR and verification receipt; an older successful
run must not be described as qualification of a newer candidate.
