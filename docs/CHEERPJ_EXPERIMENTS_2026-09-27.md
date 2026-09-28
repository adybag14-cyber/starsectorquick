# CheerpJ renderer experiments and correctness work

This is the continuation of PR #183. No deployment or merge is authorized by this
report. The source branch keeps the established color-state cache enabled; the
extended state cache and campaign RAF/flush modes remain opt-in experiments.

## Reproducible checkpoints

| Source | Experiment / evidence |
| --- | --- |
| `8c75a48fcd5d9ea04dbe036368b09171a4cf1561` | Initial color-cache and stock-art repair candidate, hosted run 36329254577. Same-runner FPS +9.27%, p95 improved, p99 +1.27% worse. |
| `dbc9ebc846b9eba5f48176a5148e212882c40f1c` | Extended-cache experiment, hosted run 36331817766. Mixed changes; not promoted. |
| `fb656702be8d02370e4bc0b721671dd34e1d42b6` | Four-way pacing screen, incremental logging and compositor fallback, hosted run 36333536569. All workflows passed. |

Runs use actual CheerpJ campaigns. These are application presentation intervals,
not GPU-completion timestamps or monitor scan-out. Different hosted runs can use
different CPUs. Compare toggles within the same running campaign, not absolute
numbers from unrelated machines or different instrumentation revisions.

## Extended state cache: retain the negative result

The first hosted eight-window paired experiment returned roughly +0.7% median
window FPS, +0.7% p95 (worse), -4.2% p99, and +2.3% jitter (worse). The local
shared-workstation trial was substantially negative. This does not support
turning the cache on by default. The conservative implementation and raw-data
harness remain available to investigate why.

## Pacing screen: a lead, not a default

Run 36333536569 screened four configurations twice in reverse order, with 240
intervals per window. Values below are statistics pooled from each configuration's
480 recorded intervals, not averages mislabeled as one continuous trace:

| Configuration | FPS | p95 ms | p99 ms | jitter p95 ms | max ms |
| --- | ---: | ---: | ---: | ---: | ---: |
| Established color cache; extended cache off | 9.607 | 144.7 | 177.7 | 52.6 | 460.0 |
| Extended cache | 9.892 | 144.9 | 152.3 | 53.6 | 206.1 |
| Extended cache + flush | 9.675 | 141.2 | 175.0 | 51.2 | 614.1 |
| Extended cache + RAF | 9.554 | 117.3 | 123.9 | 13.3 | 182.4 |

The RAF condition produced steadier intervals but no throughput win. This screen
also changed the extended-cache setting relative to baseline. Therefore the next
paired test isolates RAF alone with BOTH modes using the same color cache and
extended cache OFF. It alternates ABBABAAB, includes 24 warm-up frames per switch,
retains all 240 raw intervals per window, verifies actual RAF completion, and
restores the pre-experiment flags even on failure. No setting is promoted merely
because one p95 or FPS statistic is favorable. Input latency and full combat
performance need their own measurements.

The prior paired experiment's cleanup left the experimental cache enabled after
finishing. That did not change production defaults, but could contaminate a later
profile in the same test process. The shared cleanup now preserves both each
flag's prior value and whether the property was absent. Profile metadata records
the actual flags.

## Correctness: clipping can hide valid drawing

A separate real-WebGL corpus found concrete state-restoration omissions:

- `GL_SCISSOR_BIT` did not restore the scissor box or its enable flag.
- The color/depth/stencil attribute groups did not restore the enable flags they
  own unless callers also requested `GL_ENABLE_BIT`.
- The offscreen-to-canvas presentation blit inherited the game's scissor test,
  so an empty or small clip could suppress the completed frame copy.

These are independently reproduced bridge defects, not proof that every absent
sprite reported by a user has the same cause. A full-screen shader draw supplies
a nonempty pixel witness: with a stale zero-area clip the old bridge produces zero
of the expected 1,024 red pixels.

The fix restores only the state selected by each mask. Whole-frame presentation
temporarily disables clipping, restores it and the render framebuffer in a
`finally` block, and preserves the next game draw's intended clipping. A bounded
counter records how many presentation calls actually required that protection.

`ci/verify-lwjgl-clipping-state.js` uses the actual shipped function bodies in a
real WebGL2 context. Its 149 cases include all subsets of six masks in both cache
modes, nested independent snapshots, invalid writes, clipped full-frame copies,
an unclipped control and an injected exceptional blit. The exact pre-fix source
`fb656702...` yields **42 passing controls / 107 failing cases**. The corrected
candidate yields **149 passed / 0 failed, 303 assertions**. The first development
fixture incorrectly requested antialiasing for a single-sample blit destination;
that setup error was corrected to production's `antialias:false` before the
recorded negative control. It is not counted as a product defect.

## Test harness work is not a renderer speedup

The original live log writer rewrote the complete growing log on every console
event. The replacement preserves the exact Unicode/multiline log bytes and uses
append-only writes. A 2,000-entry deterministic fixture writes 212,889 bytes
instead of 212,389,495 bytes. This is an I/O-volume comparison, not a measured
1,000x end-to-end speedup. Failed/possibly partial appends are not blindly retried;
an evidence error fails the harness.

Locator screenshots can wait indefinitely for a continuously rendering canvas
to become stable. The fallback captures actual compositor pixels and extracts the
canvas using the existing border-based crop checks. It never manufactures a
frame, changes resolution, or suppresses rendering. Initial captures had this
fallback, but gameplay panel/shortcut/ability captures still bypassed it; a local
campaign exposed that gap. Those calls now use the same strict capture path.
Failure of both paths still fails the test, and diagnostic fallback events remain
in the log. Screenshot time is not silently subtracted from interaction latency.

Existing real animated-canvas tests compare three crops (921,600 pixels). Routing
fixtures ensure every visual poll uses the supplied capture function and that
capture failure propagates rather than becoming a visual pass.

## Asset scope and unresolved areas

The previous work verifies all 3,199 official images after repairs, including the
six genuinely invisible 4x4 placeholders. The final pack gate checks all ranges,
hashes and official bytes; the complete 3,381-entry published index is preserved.
No source font files are changed. Eleven remaining literal references are comments
and five channel-icon references are also unresolved in the official archive.
No invented replacement icons are added.

The campaign/panel/ability corpus does not certify all combat effects, missions,
mods or every live sprite path. OpenAL stubs still exist; audio is not fixed or
certified by these changes.

## Verification commands

```text
node ci/verify-lwjgl-clipping-state.js
node ci/verify-lwjgl-color-attrib-cache.js
node ci/verify-lwjgl-extended-attrib-cache.js
node ci/test-campaign-frame-boundary.js
node ci/test-renderer-experiment-state.js
node ci/test-compositor-viewport.js
node ci/test-campaign-capture-routing.js
node ci/test-incremental-log-writer.js
node ci/test-paired-experiment.js
python ci/test-runtime-graphics-audit.py -v
```

The workflow and PR record bind final hosted results to their exact head. Passing
an earlier checkpoint is not certification of a later commit. Keep failed trials,
negative results and p99 tradeoffs available when interpreting any improvement.

Primary semantics references consulted 2026-09-27:
- https://learn.microsoft.com/en-us/windows/win32/opengl/glpushattrib
- https://developer.mozilla.org/en-US/docs/Web/API/WebGLRenderingContext/scissor
- https://developer.mozilla.org/en-US/docs/Web/API/WebGL2RenderingContext/blitFramebuffer
- https://cheerpj.com/docs/changelog
