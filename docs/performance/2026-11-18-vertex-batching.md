# Vertex transport and fleet-spawn diagnostic work

## Change

The launcher enables bounded immediate-mode vertex transport batches. Java retains xyz and texture coordinates in one reusable native-order direct buffer (1,024 vertices, 20 KiB). Color and state boundaries flush pending vertices in order. JavaScript copies each batch into the existing immediate renderer; draw topology, vertex payload, resolution, textures, and simulation behavior are preserved.

An explicit `window.__LWJGL_VERTEX_BATCH__ = false` before launch selects the scalar transport for diagnosis and paired measurement. Display-list compilation stays scalar. Calling a list flushes the pending prefix and returns to scalar transport until the next Java `glBegin`. The inline no-WebGL fallback returns false from the new boolean native begin handshake.

Fleet constructor tracing is a separate change. The existing patch emitted 48 diagnostic lines for every fleet construction. The new helper is silent by default; `-Dstarsector.browserCampaignCreateDiagnostics=true` restores those exact messages. It reads this property at class initialization. The tutorial startup hook, campaign creation checkpoints, economy probes, and gameplay probe remain unchanged.

The build also refreshes the actual size of the rebuilt `bridge.jar` in `jars/index.list`, which previously omitted that rebuilt artifact from its refresh set.

## Local evidence, 18 November 2026

The first real campaign experiment used Windows Hermes, Ryzen 9 7950X3D, Chromium 145 / ANGLE SwiftShader, a 1024 x 768 framebuffer, and the existing full-campaign gameplay probe at 8x speed. It completed the existing campaign, rendering, input, menu, and ability checks.

The paired design is ABBABAAB: four scalar and four batched windows, each retaining 240 consecutive presentation intervals after 24 warm-up frames. Only the vertex flag changes in the same running campaign. Resolution, other renderer flags, and the game stay fixed. Raw intervals, branch counters, partial failures, and exact flag restoration are retained. No profiler, resource fetching, or screenshot runs inside the timed windows.

| Median of four windows per arm | Scalar transport | Batched transport | Change |
|---|---:|---:|---:|
| FPS | 27.2713 | 33.0794 | +21.30% |
| Mean frame time | 36.6808 ms | 30.2415 ms | -17.56% |
| p95 frame time | 43.65 ms | 34.70 ms | -20.50% |
| p99 frame time | 62.60 ms | 50.90 ms | -18.69% |
| p95 adjacent-frame jitter | 8.35 ms | 5.70 ms | -31.74% |

All four adjacent comparisons increased FPS: +37.56%, +6.80%, +21.41%, and +15.97%. Pooled 960-frame results were 27.4535 to 33.1333 FPS; pooled p95 was 46.1 to 35.2 ms and pooled p99 was 72.1 to 64.3 ms.

These are same-build transport comparisons. The scalar arm uses the legacy transport inside the revised Java bridge, so it includes the new inactive branch checks. It is not a byte-identical published-build comparison. A separate original-build baseline window produced 35.10 FPS in a different world/time; that unrelated value is not used to calculate an improvement.

The first experiment still contained an inactive matrix-reuse prototype in both arms. That prototype was removed from the proposed source after its own paired trial produced no consistent benefit (median FPS -0.535%, mixed pooled and tail results). Its complete source and raw evidence are preserved in the dedicated work folder. Follow-up runs use only the vertex change.

The constructor-log cleanup was not present in the measured runtime and is not credited with this FPS gain. Its observed avoided work was 3,840 constructor log lines across 80 fleet constructions, 475,360 bytes / 22.3% of the inspected browser log snapshot.

## Correctness and measurement checks

- Java native-signature compilation and 1,995 behavioral checks cover direct-memory addressing, color/state ordering, list fallback, capacity boundaries, toggles, and allocation fallback.
- JavaScript verification covers 3,270 checks, 369,520 submitted vertices, all 10 supported primitive modes, exact Float32 payload equality, draw topology, invalid memory ranges, and immediate buffer reuse.
- Paired-harness fixtures cover 154 checks, including real branch counters, raw timing agreement, unchanged controls, partial-failure evidence, post-measurement screenshot timing, and cleanup.
- Shared renderer flag-state tests pass 41 checks.
- Fleet diagnostic verification applies the production transformer to a 24-call executable constructor fixture. Original instruction order and checksum 276 are preserved, JVM verification succeeds with live stack values, disabled logging is silent, and opt-in restores all 48 lines in order.
- Jar-index tests check actual artifact sizes, ordering, idempotence, and rejection of missing bridge entries/files without partial writes.

Existing gameplay thresholds remain unchanged: 8 FPS minimum, 150 ms p95, 220 ms p99, and 60 ms p95 jitter. A successful paired report means valid evidence; it does not by itself assert a speed improvement.

## Full HD follow-up

The 1920 x 1080 run removed the matrix prototype and enabled vertex batching throughout gameplay. Chromium's default renderer setting still selected ANGLE SwiftShader on this host; this was not a hardware-GPU measurement.

| Median of four windows per arm | Scalar transport | Batched transport | Change |
|---|---:|---:|---:|
| FPS | 15.5695 | 16.8324 | +8.11% |
| p95 frame time | 74.10 ms | 66.95 ms | -9.65% |
| p99 frame time | 86.00 ms | 72.40 ms | -15.81% |
| p95 adjacent-frame jitter | 12.10 ms | 13.60 ms | +12.40% |

All four adjacent FPS comparisons improved. The jitter measure worsened by 1.5 ms; the candidate is not claimed to improve every stutter metric. The eight-window paired report is valid, but the overall deep-gameplay run failed a Character-panel interaction/return timeout. Rendering, keyboard/mouse input, and background safety passed with no graphics/runtime errors. This is retained as a qualification failure pending comparison with the published build, not relabelled a full pass. Raw results are in the adjacent full-HD JSON. The failure investigation identified two harness defects: fixed 1024-based click coordinates and a 1440-wide browser viewport that downsampled the Full HD canvas to 1396 pixels, dropping a healthy small ship below the unchanged center-subject pixel threshold. The harness now sizes its viewport for native-resolution capture and maps fixed native button centers through the actual canvas. A corrected deep-gameplay rerun is required; no visual thresholds or timeouts were lowered.

## Follow-up qualification

Original-build comparison and GitHub CI results are recorded after completion. The first CI attempt was blocked before tests by GitHub's ubuntu-latest moving to Ubuntu 26.04, which the locked Playwright 1.58.0 cannot install dependencies for. Browser jobs are pinned to Ubuntu 24.04; browser versions and acceptance thresholds are unchanged. No factor-of-two conversion from CI FPS to personal-PC FPS is assumed. These measurements concern application presentation intervals, not GPU completion or input latency.

All task files, caches, runtimes, and evidence are kept beneath `C:\Users\adyba\starsector-performance`. The storage guard uses a 20,000,000,000-byte limit with a 2 GB reserve.
