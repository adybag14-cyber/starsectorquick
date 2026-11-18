# Runtime performance investigation â€” 18 November 2026

This change reduces Java/native geometry transport and routine fleet-construction logging while retaining the game's resolution, textures, effects, simulation and input behavior. Final hardware and GitHub qualification is tracked in [PR #184](https://github.com/adybag14-cyber/starsectorquick/pull/184). A completed paired measurement is not, by itself, a performance or release pass.

## Implementation

Java collects xyz and texture coordinates in a reusable float array. A batch of exactly four vertices sends its 20 float values directly to a native receiver. This avoids both a direct-buffer copy and a JNI-memory-view read for that common case. It uses the same append function and current primitive mode; it does not combine or remove draws.

Other batch sizes copy their used prefix once into a reusable native-order direct buffer. The array and direct buffer each hold 1,024 vertices / 20 KiB: **40 KiB total transport storage**, allocated lazily. The cached native address stays at position zero. Capacity flushes append vertices without ending strips or fans.

Every existing color and renderer-state operation flushes pending vertices in order. Display-list compilation remains scalar. Calling a list flushes the prefix and returns to scalar transport until a fresh Java Begin handshake. Allocation/address failure retains scalar transport. The launcher enables batching while respecting an explicit Boolean `window.__LWJGL_VERTEX_BATCH__ = false`.

The generic JavaScript receiver reuses one whole-JNI-memory Float32 view, refreshing it only when memory identity or length changes. It retains address/alignment/count/bounds validation and obtains the current JNI data view each call. Counters record actual view creations, direct four-vertex calls, total batch calls and completed geometry. Quad-only submission creates no JNI memory view.

Fleet construction previously printed 48 injected diagnostic lines per constructor. The new helper is silent by default; `-Dstarsector.browserCampaignCreateDiagnostics=true` restores the exact messages. Tutorial startup, campaign creation checkpoints, economy probes and gameplay instrumentation remain intact. An inspected old local log contained 3,840 such lines for 80 fleets, totaling 475,360 bytes / 22.3% of that log snapshot. This cleanup is not credited with local FPS gains because the measured local runtime retains its previously patched game JARs.

The build refreshes the rebuilt `bridge.jar` size in `jars/index.list` and records its SHA-256. A matching launcher/service-worker version refreshes memoized and force-cached JAR packs for returning players. The existing save-data migration marker is unchanged.

## Verification

- Java native-signature compilation and **14,490 recorder checks** cover exact Float32 values, signed zero, NaN/infinities, real direct-memory addressing, color/state/list ordering, capacity, fallback, and storage reuse. Sentinels prove four-vertex calls do not modify direct memory.
- JavaScript verification passes **3,522 checks**, covering all ten supported primitive modes and both output layouts, exact scalar/generic/quad payload and draw equivalence, receiver-state guards, invalid memory ranges, reusable buffer views and memory replacement/growth/shrink.
- The paired harness passes **261 checks** for raw timing agreement, actual transport branches, counter conservation, valid generic-only workloads, legitimate memory growth, unchanged controls, retained partial failures and exact flag restoration.
- Shared experiment-state verification passes 41 checks.
- The production fleet transformer preserves the original constructor instruction sequence and checksum 276 under JVM verification, including live operand-stack values. Disabled logging is silent; opt-in restores all 48 lines in order.
- Jar-index tests check actual sizes, order, idempotence and rejection of missing artifacts/entries without partial writes.

Existing gameplay limits remain **8 FPS minimum, 150 ms frame p95, 220 ms frame p99, and 60 ms jitter p95**. Jitter is the p95 absolute deviation from the window's median interval; it is not an adjacent-frame difference. No visual limits, timeouts or performance thresholds were relaxed.

## Measurement method and limits

Each transport experiment uses one running campaign and fixed framebuffer, with counterbalanced **ABBABAAB** windows. Each window retains 240 consecutive presentation intervals after 24 warmup frames. Only the selected transport flag changes. Raw intervals, actual branch counters, control flags and partial failures are retained. Profiling, resource fetching and screenshots occur outside timed windows.

The scalar arm is **scalar transport inside the revised Java bridge**. It retains the added inactive branch/flush checks. These results cannot be presented as byte-identical published-build improvements. They measure application presentation intervals, not GPU completion, scan-out or end-to-end input latency. The local deep-gameplay workload uses the existing 8x campaign probe. No factor-of-two conversion to a personal computer's FPS is assumed.

Each new mechanism is tested separately. The existing depth-state experiment toggles only its three-value mirror while keeping all real GL writes. Its previous mixed results are retained in the September investigation documents; a profile alone does not justify enabling it.

## Four-vertex candidate: completed Full HD hardware trial

The final local deep-gameplay run used an actual RTX 4090 / D3D11, Ryzen 9 7950X3D, Chromium 145.0.7632.6 and a native 1920×1080 framebuffer. Depth caching stayed OFF during the vertex comparison, matching the public default. The existing 8x campaign gameplay probe was active.

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

[Raw hardware evidence](2026-11-18-quad-fullhd-hardware.json) contains all raw intervals, counters, GPU/bridge identities and gameplay results. [Candidate screenshot](2026-11-18-quad-fullhd-hardware.png) was captured after timing. CI and the normal tutorial follow-up are reported separately in the PR.

## Earlier local trials

These are development evidence, not the final four-vertex candidate's qualification.

| Candidate / environment | Median scalar FPS â†’ batched FPS | Frame p95 | Frame p99 | Outcome |
|---|---:|---:|---:|---|
| Initial direct-buffer B, 1024Ã—768 SwiftShader | 27.27 â†’ 33.08 (+21.30%) | 43.65 â†’ 34.70 ms | 62.60 â†’ 50.90 ms | Full gameplay passed; all four adjacent FPS comparisons improved |
| B, 1920Ã—1080 default browser profile, actual SwiftShader | 15.57 â†’ 16.83 (+8.11%) | 74.10 â†’ 66.95 ms | 86.00 â†’ 72.40 ms | Paired evidence valid; jitter worsened and deep UI gate failed |
| Cached JNI view C, corrected native Full HD / SwiftShader | 26.89 â†’ 30.64 (+13.93%) | 42.10 â†’ 36.75 ms | 57.35 â†’ 51.95 ms | Full deep gameplay passed |
| Java array staging D, Full HD / actual RTX 4090 D3D11 | 27.15 â†’ 29.07 (+7.08%) | 44.35 â†’ 41.55 ms | 54.10 â†’ 68.60 ms | Full deep gameplay passed; p99 worsened |

The first B run retained an inactive matrix-reuse prototype in both arms. Its own paired experiment produced no consistent benefit (median FPS âˆ’0.535% with mixed/worse tails); that prototype was removed and archived.

The initial Full HD UI failure exposed two harness assumptions: 1024-based button coordinates and a viewport that downsampled a 1920-pixel canvas to 1396 pixels. The corrected harness captures the native framebuffer and maps fixed native button centers through the actual canvas. The corrected C and D runs passed all seven mouse panels/returns, seven shortcuts, ability checks, input and visual gates, without changing their thresholds.

D's hardware profile identified repeated driver state queries as a substantial separate cost. Its paired p99 regression also motivated the four-vertex path, which removes the remaining NIO copy from those small messages. Its performance must be measured rather than inferred from correctness checks.

## Latest completed GitHub result before four-vertex transport

Head `feecc1828a10afeed0001741c15a713db3c48dca` tested cached-view candidate C on Ubuntu 24.04, Intel Xeon 6975P-C (four logical CPUs), Chromium 145.0.7632.6 and ANGLE SwiftShader, with a 1024Ã—768 game framebuffer.

[The main job failed its unchanged jitter limit](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44388531355/job/135100178019). Its 8.051-second gameplay sample measured 16.535 FPS, 130.1 ms p95, 161.6 ms p99 and **81.1 ms jitter p95**, exceeding the 60 ms limit. All visual, input, panel, shortcut and ability checks passed; no JavaScript, screenshot, graphics or runtime errors occurred, and 747/747 observed images loaded.

Its valid paired experiment did **not** establish a consistent CI gain:

| Median of four windows | Scalar | Batched C |
|---|---:|---:|
| FPS | 15.8559 | 15.7520 |
| Frame p95 | 116.8 ms | 114.1 ms |
| Frame p99 | 146.9 ms | 176.8 ms |
| Jitter p95 | 57.2 ms | 57.55 ms |

Pooled FPS was 15.6554 â†’ 15.9257 (+1.73%); adjacent-pair FPS changes were +3.82%, +5.45%, âˆ’4.26%, +2.11%. These mixed figures and the failed gate remain part of the record. [Raw main artifact](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44388531355/artifacts/14468326057).

Graphics, launcher and no-actions campaign checks passed. The Full HD lane passed its existing resolution/input smoke contract; it is not described as a full deep-gameplay pass. Its generic opening-scene result remains false. [Full HD artifact](https://github.com/adybag14-cyber/starsectorquick/actions/runs/44388531355/artifacts/14468725493).

Browser CI is pinned to Ubuntu 24.04 because the locked Playwright 1.58 browser cannot install dependencies on the newer ubuntu-latest image. A verified OW2 ASM mirror handles observed Maven download failures without changing the pinned bytes or integrity check. One earlier startup failure loading ship_names.json is preserved with unresolved cause; it did not recur in the latest no-actions run.

## Published-build pilots excluded from causal claims

Two fresh-browser original-versus-candidate pilots failed scene/workload equality, including comparisons between two original runs. The first paused before the camera settled. The second added settling, center validation and geometry preflight but still had different background phases and notification overlays.

In the settled ACCA trial, original preflight frames had median 636 draws and 25,872â€“26,456 submitted elements, versus 600 draws and 20,634â€“20,646 elements in C. The difference was mostly 35 indexed-triangle calls; original screenshots contained convoy/survey notifications absent from C. A notification disappeared even while paused, proving that a stable central crop did not establish equal whole-frame work.

Exploratory FPS was 37.41 â†’ 39.46, while p95 was 29.80 â†’ 41.05 ms and p99 31.00 â†’ 55.55 ms. **None of these differences is attributed causally to batching.** Complete failed evidence is retained. The trial did independently prove zero new JNI memory views across 2,353,094 batch calls and successful Character/Escape checks in all four runs.

A future exact-original comparison needs an Aâ†’A common-save preflight that first proves scene, pause, UI and geometry preservation. The existing Continue smoke test checks world counts, not exact scene equality.

## Workspace and reproducibility

All work, caches, runtimes, source and raw evidence are under `C:\Users\adyba\starsector-performance`. The storage guard enforces a 20,000,000,000-byte maximum with a 2 GB reserve. Local experiments run one browser at a time and record source, renderer, bridge and actual GPU identities.

The Windows hardware profile `STARSECTOR_BROWSER_RENDERER=d3d11-hardware` explicitly selects D3D11 and rejects software fallback. Ordinary and CI SwiftShader profiles keep their existing arguments. The real GPU was verified as NVIDIA RTX 4090.

The four-vertex candidate and any separately qualified default change will be reported with their actual final results in PR #184; earlier prototype numbers are not a substitute for that qualification.
