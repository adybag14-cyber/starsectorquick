# CheerpJ Internal JIT Improvements For Starsector (starsectorquick)

Date: 2026-02-13
Scope: Analysis only. No source-file edits were made.

## Current Integration Snapshot

- `launch.html` initializes CheerpJ with `version: 17`, `enableX11: true`, JS LWJGL natives, and wasm/native library mappings (`gl4es.wasm`, `unsafe_jdk_alias.wasm`, `jawt.js`) (`launch.html:821` to `launch.html:847`).
- The runtime drives Starsector through `Fixer` with a large classpath and automation properties for campaign entry/colony visit (`launch.html:741` to `launch.html:1014`).
- The active LWJGL bridge (`build/final/wasm-modules/lwjgl_clean.js`) implements most hot native paths in JavaScript and uses a Promise + `requestAnimationFrame` in `LinuxContextImplementation_nSwapBuffers`.

## What Your Runs Show

### 1) Very high native-call volume before gameplay

From recent watcher runs (`tmp_visit_colony_autowatcher_v19_*.json`), many sessions remain at:

- `status=Graphics system is initializing`
- high native call count (hundreds of thousands to millions)
- `swap` increasing heavily
- `draw=0`

Example (2026-02-13T07-32-20-735Z):

- `totalCalls=4,853,124`
- `swap=4,248`
- `draw=0`
- `keyPoll=4,246`
- `mousePoll=4,246`

Interpretation: runtime is hot in presentation/polling loops, but never reaches real GL draw path.

### 2) Startup path is long and class-loading heavy

`verify_run_latest.log` reaches campaign/rules loading around ~190s timestamps (`PAGE LOG: 190xxx ...`) while still producing repeated script-loader messages. This startup profile makes tiering decisions in the JIT highly important.

### 3) Failure-path churn is substantial

A prior run (`tmp_visit_colony_autowatcher_v19_2026-02-10T17-31-09-042Z.json`) includes `1210` repeated "Directories are not supported" errors, indicating expensive repeated negative path handling and exception/error traffic.

## JIT Improvements That Would Help This Use Case Most

## P0: JNI/JS Bridge Tiering and Specialization

1. Add ultra-fast tier-up for "interop hotspot" methods.
- Target methods like `nSwapBuffers`, key/mouse poll, event dequeue, and GL11 wrappers.
- Promote these wrappers aggressively (few hundred calls) instead of waiting for generic hot thresholds.

2. Generate per-signature native trampolines.
- Avoid generic varargs/object-array marshaling for repeatedly identical signatures.
- Keep direct typed paths for `(env, int, int, ptr)` and similar call shapes.

3. Inline cache native target resolution.
- Most calls are monomorphic in this workload; cache the resolved native function pointer directly.
- Minimize dynamic property lookups for every frame/poll call.

## P0: Async Frame Boundary Optimization

4. Optimize `Promise + requestAnimationFrame` suspend/resume paths used each swap.
- Current swap path allocates Promise/closure per frame in JS bridge style.
- JIT/runtime support for a lightweight "frame-yield" primitive would reduce allocation and continuation overhead.

5. Reduce cross-boundary deopt risk around async resumes.
- Keep resumed continuation code in optimized tier when call shape remains stable.

## P1: Typed Memory Access Optimizations

6. Hoist and reuse `DataView`/heap-base metadata in tight native loops.
- Many wrappers repeatedly call `env.getJNIDataView()` and create short-lived typed views.

7. Add escape analysis/scalar replacement for transient typed-array wrappers.
- Eliminate short-lived `Uint8Array`/`Int32Array` views where possible.

8. Fuse bounds checks for contiguous pointer access patterns.
- Texture upload and vertex fetch paths repeatedly do predictable pointer arithmetic.

## P1: Poll-Loop and Time Intrinsics

9. Intrinsic fast paths for `nanoTime/currentTimeMillis` and poll methods.
- Your traces show key/mouse polling near swap frequency.
- Lowering these to minimal overhead primitives helps sustained loop throughput.

10. Loop-oriented optimization profile for "poll until state transition" behavior.
- Detect and optimize loops with stable branch patterns and monotonic counters.

## P1: Exception and Failure-Path De-dup in Optimized Code

11. Cheap repeat-exception handling for known reflection/module-open failures.
- Avoid rebuilding heavy exception metadata repeatedly in hot startup loops.

12. Negative lookup caching in runtime path resolution.
- Cache repeated "not found/unsupported directory" probes to avoid repeated work and error churn.

## P2: Startup/Classpath Warmup Strategy in JIT

13. Prioritize compilation of classloading, path normalization, and CSV/JSON parse helpers early.
- Your startup spends a lot of time in resource loading and rules/script initialization before gameplay.

14. Keep a small startup profile cache (if CheerpJ supports persistent tiering metadata).
- Re-runs of the same title benefit from pre-learned hot methods and call shapes.

## Why These JIT Changes Match Your Use Case

Your target workflow is campaign automation with colony visit. In that path, the system must move quickly from:

- runtime ready
- graphics init
- first real draw
- campaign state reachable

Right now, telemetry shows large interop/poll traffic and swap activity with no draw progression. The highest-value JIT work is therefore interop specialization, async-frame boundary efficiency, and poll-loop optimization.

## Recommended Validation Metrics (Post-JIT)

Track these on the same watcher JSON harness:

1. Time to first `draw > 0`.
2. `totalCalls / swap` ratio during first 120 seconds.
3. Time to `campaignSeen=true` and `colonyVisitLikely=true`.
4. Count of repeated path/dir errors per run.
5. Number of swap frames spent with `draw=0` after graphics init status.

## Practical Priority Order

1. Interop trampolines + tiering thresholds (P0)
2. Async frame-yield optimization for swap path (P0)
3. Typed memory + poll-loop optimization (P1)
4. Exception/path negative-cache optimizations (P1)
5. Startup profile reuse/persistent tier hints (P2)

These changes are internal to CheerpJ/runtime JIT behavior and directly aligned with the bottlenecks visible in your current Starsector traces.
