# Runtime performance investigation — 18 November 2026

**Candidate E is retained:** direct four-vertex native calls plus generic direct-memory batches. The direct-array F experiment passed correctness and gameplay checks but was rejected after inconsistent throughput and worse p99/worst-frame results in its hardware run. Exact-head CI qualification of retained E under the public defaults is pending in [PR #184](https://github.com/adybag14-cyber/starsectorquick/pull/184). Resolution, textures, effects, simulation and input behavior are preserved. A completed paired measurement is not, by itself, a performance or release pass.

## Current implementation: retained candidate E

Java collects xyz and texture coordinates in a reusable float array. A batch of exactly four vertices sends its 20 float values directly as primitive native arguments. This avoids a direct-buffer copy and JNI-memory-view read for that common case. It uses the same append function and current primitive mode; it does not combine or remove draws.

Other batch sizes copy their used prefix once into a reusable native-order direct buffer. The array and direct buffer each hold 1,024 vertices / 20 KiB: **40 KiB total payload transport storage**, allocated lazily. Object headers and runtime metadata are additional. The cached native address stays at position zero. Capacity flushes append vertices without ending strips or fans.

Color and renderer-state operations flush pending vertices in order. Display-list compilation remains scalar. Calling a list flushes the prefix and returns to scalar transport until a fresh Java Begin handshake. Allocation/address failure retains scalar transport. The launcher enables batching while respecting an explicit Boolean `window.__LWJGL_VERTEX_BATCH__ = false`.

The generic JavaScript receiver reuses one whole-JNI-memory Float32 view, refreshing it only when memory identity or length changes. It retains address/alignment/count/bounds validation and obtains the current JNI data view each call. Counters record actual view creations, direct four-vertex calls, total batch calls and completed geometry. Quad-only submission creates no JNI memory view. The direct Java-array F receiver and its producer are not part of retained E; that prototype and its evidence are documented below.

Fleet construction previously printed 48 injected diagnostic lines per constructor. The new helper is silent by default; `-Dstarsector.browserCampaignCreateDiagnostics=true` restores the exact messages. Tutorial startup, campaign creation checkpoints, economy probes and gameplay instrumentation remain intact. An inspected old local log contained 3,840 such lines for 80 fleets, totaling 475,360 bytes / 22.3% of that log snapshot. This cleanup is not credited with local FPS gains because the measured local runtime retains its previously patched game JARs.

The build refreshes the rebuilt `bridge.jar` size in `jars/index.list` and records its SHA-256. A matching launcher/service-worker version refreshes memoized and force-cached JAR packs for returning players. The existing save-data migration marker is unchanged.

## Verification of retained E

- Java native-signature compilation and **14,490 recorder checks** cover exact Float32 values, signed zero, NaN/infinities, real direct-memory addressing, color/state/list ordering, capacity, fallback and storage reuse. Sentinels prove four-vertex calls do not modify direct memory.
- JavaScript verification passes **3,522 checks**, covering all ten supported primitive modes and both output layouts, exact scalar/generic/quad payload and draw equivalence, receiver-state guards, invalid memory ranges, reusable buffer views and memory replacement/growth/shrink.
- The paired harness passes **261 checks** for raw timing agreement, actual transport branches, counter conservation, valid generic-only workloads, legitimate memory growth, unchanged controls, retained partial failures and exact flag restoration.
- Shared experiment-state verification passes **41 checks**.
- The production fleet transformer preserves the original constructor instruction sequence and checksum 276 under JVM verification, including live operand-stack values. Disabled logging is silent; opt-in restores all 48 lines in order.
- Jar-index tests check actual sizes, order, idempotence and rejection of missing artifacts/entries without partial writes.

Existing gameplay limits remain **8 FPS minimum, 150 ms frame p95, 220 ms frame p99, and 60 ms jitter p95**. Jitter is the p95 absolute deviation from the window's median interval; it is not an adjacent-frame difference. No visual limits, timeouts or performance thresholds were relaxed.

The strict campaign job now runs the actual public renderer defaults: its experiment configuration is `{}`, batching follows the launcher default, and depth caching defaults OFF. Earlier E CI injected depth caching ON; that historical configuration is recorded below. Depth ON/OFF remains a separate paired diagnostic that restores the complete captured renderer flag state before later diagnostics. The strict limits remain 8/150/220/60. **Correcting which configuration is qualified is not an FPS optimization, and differences between those separate CI runs are not a measured speedup.**

## Measurement method and limits

Each transport experiment uses one running campaign and fixed framebuffer, with counterbalanced **ABBABAAB** windows. Each window retains 240 consecutive presentation intervals after 24 warmup frames. Only the selected transport flag changes. Raw intervals, actual branch counters, control flags and partial failures are retained. Counter deltas span the full observed swapDelta between snapshots, while FPS and frame-time statistics use the last 240 presentation intervals; raw counter totals therefore must not be treated as counts for exactly those 240 intervals. Per-swap normalization uses the recorded swapDelta. Profiling, resource fetching and screenshots occur outside timed windows.

The scalar arm is **scalar transport inside the revised Java bridge**. It retains the added inactive branch/flush checks. These results cannot be presented as byte-identical published-build improvements. They measure application presentation intervals, not GPU completion, scan-out or end-to-end input latency. The local deep-gameplay workload uses the existing 8x campaign probe. No factor-of-two conversion to a personal computer's FPS is assumed.

Each new mechanism is tested separately. The existing depth-state experiment toggles only its three-value mirror while keeping all real GL writes. Its previous mixed results are retained in the September investigation documents; a profile alone does not justify enabling it.

## Retained E: exact-head CI qualification pending

<!-- E_EXACT_HEAD_CI_PENDING: replace only after the retained-E source head has completed the public-default strict jobs. -->
Retained E is the current implementation. Its next exact-head CI run must qualify the actual public renderer defaults with the unchanged 8/150/220/60 limits. Earlier E measurements and F's passed gameplay gate do not substitute for that result. The depth diagnostic remains separate and must restore the captured flags.

## Rejected experiment F: direct Java-array transport

F was not retained because its real-game comparison did not establish a consistent throughput gain and showed worse p99 and worst-frame behavior in this run. This is an experimental selection decision, not evidence that the array API is incorrect or that hidden CheerpJ allocations/GC caused the result. E/F measurements from different campaigns are not a controlled direct comparison of those two implementations.

### F hardware result

The actual RTX 4090 / D3D11 run used a native 1920 x 1080 framebuffer, public renderer defaults with depth caching OFF, and the existing 8x deep-gameplay probe. All game, visual, input, seven-panel/return, seven-shortcut and ability checks passed. JavaScript, screenshot, graphics and runtime error arrays were empty. The unchanged gameplay gate passed at **34.4922 FPS, 34.9 ms p95, 39.7 ms p99 and 6.2 ms jitter p95**.

| F transport metric | Revised-bridge scalar | F direct-array/quad transport |
|---|---:|---:|
| Median-window FPS | 34.7551 | 34.5648 (-0.55%) |
| Median-window frame p95 | 34.45 ms | 34.15 ms |
| Median-window frame p99 | 51.80 ms | 70.65 ms (+36.39%) |
| Median-window jitter p95 | 6.60 ms | 6.85 ms |
| Pooled 960-frame FPS | 33.2574 | 34.1695 |
| Pooled frame p95 | 38.70 ms | 35.30 ms |
| Pooled frame p99 | 52.70 ms | 72.50 ms |
| Pooled jitter p95 | 9.70 ms | 7.50 ms |
| Worst recorded frame | 77.70 ms | 143.10 ms |

Adjacent FPS changes were **+4.95%, +12.45%, -4.77%, -2.28%**. All four adjacent p99 changes were worse: **+27.6, +25.1, +8.4 and +10.1 ms**. The positive pooled FPS/p95/jitter results are retained alongside the negative median throughput, median jitter, p99 and worst-frame results. They do not overturn the inconsistent benefit and tail regression that led to rejecting F.

All recorded candidate batch messages used the array or four-vertex receiver: **1,040,509 array calls plus 331,429 four-vertex calls = 1,371,938 batch calls**, with zero JNI memory-view creations in the timed windows. Geometry conservation passed. Median completed immediate primitives per swap were 591.171 scalar versus 594.506 candidate; median completed immediate vertices were 17,370.841 versus 17,879.667. Individual windows changed as the unpaused campaign evolved. These counters are not complete GPU-work measurements or proof of an exactly frozen scene.

[Complete F hardware evidence](2026-11-18-array-fullhd-hardware.json) preserves all raw intervals, per-swap geometry, counters, flags, gameplay outcomes and actual frozen implementation hashes. [F screenshot](2026-11-18-array-fullhd-hardware.png) was captured outside timing. The identity's source commit predates the uncommitted F prototype; the measured bytes are identified by renderer SHA-256 `0611afbc8c8a4095fd5db148cc05c9026f3cb5fea84814d14301bd5aaa610726` and bridge SHA-256 `e3cf3e7f4968ef1630da3f19afb738c04d3157d5293c7a6abf57a4662e904313`.

### Archived F mechanism

Java collects xyz and texture coordinates in one reusable `float[5120]`: 1,024 vertices and **20 KiB of payload staging storage**, allocated lazily. A batch of exactly four vertices still sends its 20 float values as primitive native arguments. Every other nonempty batch passes the same Java `float[]` and used vertex count to a synchronous JavaScript receiver. It consumes that prefix before Java reuses the staging array and retains no Java-array handle, typed-array subview, or backing buffer across calls. In the F prototype this removed the generic path's explicit NIO copy and direct-buffer staging allocation.

Both receivers append through the existing vertex function and current primitive mode. Capacity flushes append without ending strips or fans; they do not combine or remove draws. Color and renderer-state boundaries flush pending vertices in order. Display-list compilation remains scalar, and a list call flushes the prefix and returns to scalar transport until a fresh Java Begin handshake. A staging-array allocation failure retains scalar transport. The launcher enables batching while respecting an explicit Boolean `window.__LWJGL_VERTEX_BATCH__ = false`.

The old JavaScript direct-memory receiver remains exported for compatibility and verification. It retains address/alignment/count/bounds validation and its whole-JNI-memory Float32 view cache, refreshed when memory identity or length changes. F's Java producer uses the array and four-vertex receivers. Counters distinguish `arrayCalls`, `quadCalls`, total `batchCalls`, actual memory-view creations and completed geometry; `quadCalls + arrayCalls <= batchCalls` allows the compatibility memory path. The 20 KiB figure describes the Java payload array and excludes object headers and runtime metadata.

The official [CJ3Library conversion rules](https://cheerpj.com/docs/reference/CJ3Library) specify the `Float32Array` / `float[]` mapping by reference. The [native-library guide](https://cheerpj.com/docs/guides/implementing-native-libraries) documents the static-native argument convention and permits returning a value directly or through a Promise. F's receiver returns synchronously. **Neither these API rules nor the probe below prove that CheerpJ creates zero hidden conversion or wrapper allocations.** Game FPS and frame-time tails require separate measurement.

### Archived F correctness checks

The following counts apply to the rejected F prototype, not retained E:

- F's production native-signature compilation is clean, with **13,871 Java recorder checks** covering exact Float32 values, signed zero, NaN/infinities, used prefixes, reused staging storage, allocation fallback, color/state/list ordering and capacity boundaries.
- The JavaScript receiver verifier passes **4,527 checks**, including 369,520 core submitted vertices, 516 repeated-array batches, 1,036 retained-memory cache batches and four same-buffer resize cases. It covers supported primitive modes, both output layouts, scalar/array/quad payload and draw equivalence, receiver guards and the compatibility direct-memory receiver.
- The paired harness passes **385 checks** for raw timing agreement, actual array/quad/memory transport branches, safe nonnegative monotonic counters, the combined subset invariant, completed-vertex conservation, unchanged controls, retained partial failures and exact flag restoration. Array-only, old-memory-only and mixed workloads are valid; array calls during the scalar arm are rejected.
Shared experiment-state verification also passed 41 checks.

### Archived F Java-array interop probe

[Raw interop evidence](2026-11-18-java-array-interop.json) records **74 native calls, 251,834 JavaScript checks and 1,570 Java checks**, with main exit code zero. The receiver was not declared async, returned no Promise, and the probe retained zero Java-array handles. Reused-array contents and bidirectional visibility were checked through the actual Java 17 / CheerpJ 4.3 boundary.

The probe deliberately allocated 32 MiB of direct memory: observed JNI memory grew from **10 MiB to 42 MiB**, with **nine native calls after the first observed growth**. The run retained its actual memory lengths and passed subsequent payload checks. Browser page-error evidence is empty. One aborted request to `https://cjrtnc.leaningtech.com/4.3/etc/localtime` remains recorded as `net::ERR_ABORTED`; it has not been erased or relabeled.

This validates the API behavior used by F. It is not a game benchmark, an allocation profiler, or proof that CheerpJ does no internal copying/wrapper allocation.

## Retained candidate E: earlier Full HD hardware trial

E used the reused 20 KiB Java array plus a 20 KiB direct buffer for generic messages: 40 KiB total payload staging. Its four-vertex branch avoided that direct-buffer copy, while other counts retained it. The rejected F experiment replaced that remaining generic transfer.

The historical E local deep-gameplay run used an actual RTX 4090 / D3D11, Ryzen 9 7950X3D, Chromium 145.0.7632.6 and a native 1920×1080 framebuffer. Depth caching stayed OFF during the vertex comparison, matching the public default. The existing 8x campaign gameplay probe was active.

All seven mouse panel/return tests, seven shortcuts, ability checks, visual checks and input checks passed. All JavaScript, graphics, screenshot and runtime error arrays were empty. The separate unchanged gameplay gate passed at 33.18 FPS, 39.7 ms p95, 45.1 ms p99 and 10.2 ms jitter p95.

| Vertex transport metric | Scalar control | Four-vertex candidate |
|---|---:|---:|
| Median-window FPS | 33.1005 | 35.1883 (+6.31%) |
| Median-window frame p95 | 35.15 ms | 33.80 ms |
| Median-window frame p99 | 42.75 ms | 39.75 ms |
| Median-window jitter p95 | 5.45 ms | 6.10 ms |
| Pooled 960-frame FPS | 32.8311 | 34.9728 |
| Pooled frame p95 | 38.50 ms | 34.70 ms |
| Pooled frame p99 | 51.20 ms | 42.70 ms |
| Pooled jitter p95 | 8.90 ms | 6.80 ms |
| Worst recorded frame | 75.60 ms | 88.00 ms |

Three of four adjacent comparisons increased FPS: +9.94%, +3.06%, +15.72%, then -2.19%. Thus the overall throughput and p95/p99 improved, but improvement is not claimed for every window, worst frame or jitter summary. The scalar-control caveat above still applies.

The four candidate windows exercised 335,297 direct four-vertex calls and 1,367,408 total batch calls, with zero new memory views during any timed window. Direct four-vertex coverage was about 24.5% of batch calls; an average batch size near four was not evidence that every message had four vertices.

A separate depth-cache toggle in the same session increased median-window FPS 32.58→39.32, but pooled p95 and jitter worsened and the final adjacent FPS pair regressed. Its default remains OFF. No broad cache, pacing, or depth-default change is included.

[Raw hardware evidence](2026-11-18-quad-fullhd-hardware.json) contains all raw intervals, counters, GPU/bridge identities and gameplay results. [Candidate screenshot](2026-11-18-quad-fullhd-hardware.png) was captured after timing. E's CI and normal tutorial follow-ups are reported below; the next exact-head public-default CI qualification remains separate.

## Earlier local trials

These are earlier development evidence, not the next exact-head retained-E qualification.

| Candidate / environment | Median scalar FPS → batched FPS | Frame p95 | Frame p99 | Outcome |
|---|---:|---:|---:|---|
| Initial direct-buffer B, 1024×768 SwiftShader | 27.27 → 33.08 (+21.30%) | 43.65 → 34.70 ms | 62.60 → 50.90 ms | Full gameplay passed; all four adjacent FPS comparisons improved |
| B, 1920×1080 default browser profile, actual SwiftShader | 15.57 → 16.83 (+8.11%) | 74.10 → 66.95 ms | 86.00 → 72.40 ms | Paired evidence valid; jitter worsened and deep UI gate failed |
| Cached JNI view C, corrected native Full HD / SwiftShader | 26.89 → 30.64 (+13.93%) | 42.10 → 36.75 ms | 57.35 → 51.95 ms | Full deep gameplay passed |
| Java array staging D, Full HD / actual RTX 4090 D3D11 | 27.15 → 29.07 (+7.08%) | 44.35 → 41.55 ms | 54.10 → 68.60 ms | Full deep gameplay passed; p99 worsened |

The first B run retained an inactive matrix-reuse prototype in both arms. Its own paired experiment produced no consistent benefit (median FPS −0.535% with mixed/worse tails); that prototype was removed and archived.

The initial Full HD UI failure exposed two harness assumptions: 1024-based button coordinates and a viewport that downsampled a 1920-pixel canvas to 1396 pixels. The corrected harness captures the native framebuffer and maps fixed native button centers through the actual canvas. The corrected C and D runs passed all seven mouse panels/returns, seven shortcuts, ability checks, input and visual gates, without changing their thresholds.

D's hardware profile identified repeated driver state queries as a substantial separate cost. Its paired p99 regression also motivated the four-vertex path, which removes the remaining NIO copy from those small messages. Its performance must be measured rather than inferred from correctness checks.

## Latest completed GitHub result before four-vertex transport

Head `feecc1828a10afeed0001741c15a713db3c48dca` tested cached-view candidate C on Ubuntu 24.04, Intel Xeon 6975P-C (four logical CPUs), Chromium 145.0.7632.6 and ANGLE SwiftShader, with a 1024×768 game framebuffer.

[The main job failed its unchanged jitter limit](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44388531355/job/135100178019). Its 8.051-second gameplay sample measured 16.535 FPS, 130.1 ms p95, 161.6 ms p99 and **81.1 ms jitter p95**, exceeding the 60 ms limit. All visual, input, panel, shortcut and ability checks passed; no JavaScript, screenshot, graphics or runtime errors occurred, and 747/747 observed images loaded.

Its valid paired experiment did **not** establish a consistent CI gain:

| Median of four windows | Scalar | Batched C |
|---|---:|---:|
| FPS | 15.8559 | 15.7520 |
| Frame p95 | 116.8 ms | 114.1 ms |
| Frame p99 | 146.9 ms | 176.8 ms |
| Jitter p95 | 57.2 ms | 57.55 ms |

Pooled FPS was 15.6554 → 15.9257 (+1.73%); adjacent-pair FPS changes were +3.82%, +5.45%, −4.26%, +2.11%. These mixed figures and the failed gate remain part of the record. [Raw main artifact](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44388531355/artifacts/14468326057).

Graphics, launcher and no-actions campaign checks passed. The Full HD lane passed its existing resolution/input smoke contract; it is not described as a full deep-gameplay pass. Its generic opening-scene result remains false. [Full HD artifact](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44388531355/artifacts/14468725493).

Browser CI is pinned to Ubuntu 24.04 because the locked Playwright 1.58 browser cannot install dependencies on the newer ubuntu-latest image. A verified OW2 ASM mirror handles observed Maven download failures without changing the pinned bytes or integrity check. One earlier startup failure loading ship_names.json is preserved with unresolved cause; it did not recur in the latest no-actions run.

## Historical candidate E: GitHub CI failure and small paired gain

E head `36e1060667bc3be650809549737f66f49b89f1bf`, checked as merge `5f282d0fcfdb2ef171f2cf47d5a7800f6c612ad7`, ran on the four-logical-CPU Intel Xeon 6975P-C runner with Chromium 145.0.7632.6, ANGLE SwiftShader and a 1024 x 768 game framebuffer. This historical workflow injected **depth caching ON** in both transport arms. It was not a public-default qualification.

[The main job failed](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44394299360/job/135120271353). Its unchanged 8.023-second gameplay sample measured **17.2633 FPS, 120.3 ms p95, 136.3 ms p99 and 74.8 ms jitter p95**. FPS and p95/p99 passed; jitter exceeded 60 ms. All visual, input, seven-panel/shortcut and ability checks passed; JavaScript, screenshot, graphics and runtime error arrays were empty. All 748 observed images loaded, with three aborted requests retained in the evidence. Pages-overlay validation was skipped after the failed main gate. The main result and overall matrix conclusion remain failure.

| E CI transport metric | Revised-bridge scalar | E batching |
|---|---:|---:|
| Median-window FPS | 16.1494 | 16.8121 (+0.6626 FPS / +4.10%) |
| Median-window frame p95 | 116.20 ms | 116.05 ms |
| Median-window frame p99 | 159.05 ms | 160.90 ms |
| Median-window jitter p95 | 62.55 ms | 63.25 ms |
| Pooled 960-frame FPS | 16.1419 | 16.8138 |
| Pooled frame p95 | 117.60 ms | 116.50 ms |
| Pooled frame p99 | 163.60 ms | 162.80 ms |
| Pooled jitter p95 | 63.50 ms | 64.30 ms |
| Worst recorded frame | 570.50 ms | 528.70 ms |

Three of four adjacent FPS pairs improved: +7.51%, +5.15%, -0.69%, +4.88%. Three adjacent p99 comparisons worsened, as did median-window p99 and both jitter summaries. Candidate counters recorded 1,044,339 total batch calls, including 205,667 direct four-vertex calls, with zero new memory views. Scalar and candidate windows had different draw/vertex totals as the unpaused campaign evolved; completed-vertex conservation does not establish equal cross-window workload. The measured paired FPS increase is confined to that experiment configuration; it is not a full CI pass or an exact-original comparison.

Graphics verification passed with E's historical 14,490 Java checks, 3,522 renderer checks, 261 paired checks and 41 state checks; launcher layout passed 73 checks. The no-actions campaign job passed. The Full HD lane passed its existing resolution/input smoke contract with seven semantic shortcut checks and a native 1920 x 1080 canvas; its generic result remained false and deep gameplay was disabled. It is not a full gameplay/FPS pass.

[Complete E CI summary and raw paired windows](2026-11-18-quad-ci.json) retain the negative results, identities and artifact hashes. The [main artifact](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44394299360/artifacts/14469899695) and [Full HD artifact](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44394299360/artifacts/14469769634) are separately identified.

## Historical candidate E: normal Galatia tutorial hardware follow-up

This Full HD RTX 4090 / D3D11 run used the normal Galatia tutorial, with depth caching OFF and batching enabled by the launcher rather than an injected vertex override. It did not enable the deep-gameplay probe. Both initial center-subject captures saw the welcome overlay. Consequently generic `ok`, campaign visual quality and tutorial visual quality remain **false**, while rendering, keyboard/mouse responsiveness, seven mouse-panel checks and seven shortcut checks passed. Browser, graphics and runtime error arrays were empty. These statuses are preserved; the welcome-overlay failure is not silently converted into full qualification.

| E normal tutorial transport metric | Revised-bridge scalar | E batching |
|---|---:|---:|
| Median-window FPS | 17.9650 | 18.5193 (+3.09%) |
| Median-window frame p95 | 62.20 ms | 59.30 ms |
| Median-window frame p99 | 80.90 ms | 62.75 ms |
| Median-window jitter p95 | 7.45 ms | 5.95 ms |
| Pooled 960-frame FPS | 17.9498 | 18.5365 |
| Pooled frame p95 | 63.30 ms | 59.00 ms |
| Pooled frame p99 | 80.90 ms | 63.40 ms |
| Pooled jitter p95 | 8.70 ms | 6.00 ms |
| Worst recorded frame | 108.90 ms | 121.00 ms |

All four adjacent FPS comparisons improved (+4.32%, +2.84%, +4.16%, +1.77%); all four p95 and p99 comparisons improved. The worst recorded frame nevertheless increased. The paired run used one unpaused campaign, not the exact published bridge. [Raw E normal tutorial evidence](2026-11-18-quad-tutorial-hardware.json) retains those limits and the failed welcome-center result. Its local launcher labels also had a subsequently corrected UTF-8 round-trip issue; the production launch source was unaffected.

## Historical normal tutorial depth diagnostic: negative frame-time result

A separate normal Galatia tutorial run toggled only the depth-state cache. Its initial welcome-overlay center gates and generic result also remained false; rendering and input/UI/shortcut checks passed with no browser, graphics or runtime errors. This was valid paired diagnostic evidence, not a full deep-gameplay pass.

| Normal tutorial depth metric | Authoritative queries / cache OFF | Depth cache ON |
|---|---:|---:|
| Median-window FPS | 13.3185 | 14.1140 (+5.97%) |
| Median-window frame p95 | 83.15 ms | 129.60 ms |
| Median-window frame p99 | 99.75 ms | 168.20 ms |
| Median-window jitter p95 | 8.40 ms | 89.00 ms |
| Pooled 960-frame FPS | 13.8004 | 14.5728 |
| Pooled frame p95 | 83.50 ms | 130.60 ms |
| Pooled frame p99 | 95.90 ms | 186.10 ms |
| Pooled jitter p95 | 21.60 ms | 88.50 ms |
| Worst recorded frame | 110.50 ms | 196.60 ms |

One adjacent FPS pair regressed (-6.33%); every adjacent p95 and p99 comparison worsened. Median-window jitter increased more than tenfold and exceeded the unchanged 60 ms gameplay limit. An increased FPS median does not outweigh this stutter regression. **Depth caching remains OFF by default.** [Raw normal tutorial depth evidence](2026-11-18-depth-tutorial-hardware.json) preserves all windows, optional completed-geometry counters, initial visual failure and restoration status. Do not compare its absolute FPS to the separate vertex run as if they shared an identical world or measurement time.

## Published-build pilots excluded from causal claims

Two fresh-browser original-versus-candidate pilots failed scene/workload equality, including comparisons between two original runs. The first paused before the camera settled. The second added settling, center validation and geometry preflight but still had different background phases and notification overlays.

In the settled ACCA trial, original preflight frames had median 636 draws and 25,872–26,456 submitted elements, versus 600 draws and 20,634–20,646 elements in C. The difference was mostly 35 indexed-triangle calls; original screenshots contained convoy/survey notifications absent from C. A notification disappeared even while paused, proving that a stable central crop did not establish equal whole-frame work.

Exploratory FPS was 37.41 → 39.46, while p95 was 29.80 → 41.05 ms and p99 31.00 → 55.55 ms. **None of these differences is attributed causally to batching.** Complete failed evidence is retained. The trial did independently prove zero new JNI memory views across 2,353,094 batch calls and successful Character/Escape checks in all four runs.

A later normal tutorial original/E check also remains exploratory: its initial diagnostic windows measured 20.1241 versus 16.4237 FPS, but scene equality was not established. Later attribution showed 1,094.165 versus 1,279.959 draw/upload calls per frame; those traces began at different campaign times, after E's extra paired windows, so they cannot attribute the initial difference to Java wrapper overhead. Welcome screenshots had different starfields, and no matching late original screenshot was saved. The task archives retain these runs under `evidence/published-tutorial-fullhd-hardware` and `evidence/vertex-quad-tutorial-fullhd-hardware`.

A future exact-original comparison needs an A→A common-save preflight that first proves scene, pause, UI and geometry preservation. The existing Continue smoke test checks world counts, not exact scene equality.

## Workspace and reproducibility

All work, caches, runtimes, source and raw evidence are under `C:\Users\adyba\starsector-performance`. The storage guard enforces a 20,000,000,000-byte maximum with a 2 GB reserve. Local experiments run one browser at a time and record source, renderer, bridge and actual GPU identities.

The Windows hardware profile `STARSECTOR_BROWSER_RENDERER=d3d11-hardware` explicitly selects D3D11 and rejects software fallback. Ordinary and CI SwiftShader profiles keep their existing arguments. The real GPU was verified as NVIDIA RTX 4090.

Retained E's next exact-head public-default CI result will be reported in PR #184. Earlier prototype numbers and rejected F's successful gameplay gate do not substitute for that qualification. F's complete outcome and raw evidence remain preserved.
