# CheerpJ 4.2 JIT Machine-Read Deep Findings For Starsector (No Code Changes)

Date: 2026-02-13
Project: `C:\Users\Ady\Documents\starsectorquick`

## Scope
- Machine-read CheerpJ 4.2 runtime artifacts (`loader.js`, `cj3.js`, `cj3.wasm`).
- Correlate runtime internals with Starsector v19 watcher behavior in this project.
- Produce actionable internal JIT/runtime improvement targets for your use case.

## Method Used
- Downloaded runtime artifacts from `https://cjrtnc.leaningtech.com/4.2/`.
- Installed local analysis tooling in `C:\Users\Ady\Documents\_cheerpj42_machine_read` only:
  - `js-beautify`
  - `wabt`
  - `acorn`
- Generated analysis artifacts:
  - `cj3.pretty.js`
  - `cj3.wat`
  - `cj3_wat_summary.json`
  - `cj3_wat_import_call_counts.json`
  - `auX_option_keys.json`

## Machine-Read Results: CheerpJ 4.2 Internals

### 1) Public init surface has no explicit JIT/tier toggle
- `loader.js` is a bootstrap wrapper that loads `cj3.js` and `cj3.wasm`, then calls `cj3Init(options, ...)`.
- No direct public init key was found for explicit JIT tier policy (no `jit`, `tier`, `compilerMode` style option in loader surface).

### 2) `cj3Init` option parser (`auX`) confirms exposed knobs are mostly runtime behavior, not compiler policy
Extracted option keys (`26`):
- `version`
- `fetch`
- `windowOpen`
- `natives`
- `libraries`
- `overrideDocumentBase`
- `enableInputMethods`
- `enableDebug`
- `enableX11`
- `maximizeWindows`
- `status`
- `logCanvasUpdates`
- `preloadProgress`
- `preloadResources`
- `clipboardMode`
- `beepCallback`
- `appletTitleCallback`
- `execCallback`
- `overrideShortcuts`
- `tailscaleControlUrl`
- `tailscaleDnsIp`
- `tailscaleAuthKey`
- `tailscaleLoginUrlCb`
- `tailscaleIpCb`
- `appletParamFilter`
- `javaProperties`

Conclusion: current exposed knobs are integration/configuration focused, not JIT-policy focused.

### 3) Continuation model is central (high leverage for JIT/runtime improvements)
From `cj3.pretty.js`:
- Widespread `throw 'CheerpJContinue'` continuation points.
- `MessageChannel` + repeated `postMessage(null)` scheduling.
- `requestAnimationFrame(...)` and `setTimeout(...)` handoffs.

Interpretation: this runtime relies heavily on cooperative suspension/resume. Any JIT/runtime overhead on continuation entry/exit will amplify in your workload.

### 4) Memory growth path is dynamic and rebind-heavy
Observed behavior:
- Runtime `memory` created as `initial: 18`, `maximum: 4096` pages.
- `memory.grow(...)` hooks refresh all typed views (`Uint8Array`, `Uint16Array`, `Int32Array`, `BigInt64Array`, `Float32Array`, `Float64Array`, `DataView`).
- `L0(...)` grows to MB-aligned boundaries and immediately yields via `CheerpJContinue`.
- `cj3GlobalMemLimit()` currently returns `8388608` (8 MiB) from mapped symbol path.

Interpretation: growth and continuation interplay is another hotspot candidate under heavy init and bridge pressure.

### 5) `cj3.wasm` structural summary
From WAT conversion and scan:
- Imports: `33`
- Exports: `240`
- Functions: `1063`
- Globals: `153`
- Memories: `2`
- Tables: `1`
- Call-indirect sites: `390`
- Bulk-memory ops: present (`memory.init`, `data.drop`)
- SIMD op evidence: not found
- Atomic/thread op evidence: not found

Interpretation: heavy indirect dispatch exists; thread-atomics and SIMD are not evident in this module as currently shipped.

### 6) Internal JIT strings are present inside wasm data segments
Found embedded strings including:
- `WasmJIT: ...`
- `JIT failure - please report a bug: ...`
- `runOpcode ...`
- `runWideOpcode ...`

Interpretation: there is internal JIT machinery in the runtime, but not exposed as a simple external init switch in this build path.

## Use-Case Correlation (Starsector Integration)

### Current project integration shape
From `launch.html`:
- Uses CheerpJ 4.2 loader.
- `cheerpjInit({ version: 17, enableX11: true, natives, libraries, javaProperties, mounts })`.
- Loads `lwjgl_clean.js`, `gl4es.wasm`, `unsafe_jdk_alias.wasm`, `jawt.js`, and large Starsector classpath.
- Auto campaign bootstrap is enabled for visit-colony flow (`new_visit_colony`).

### Watcher corpus behavior (v19)
Scanned `159` files matching `tmp_visit_colony_autowatcher_v19_*.json`:
- Final status distribution:
  - `Graphics system is initializing`: `139`
  - `CheerpJ runtime ready`: `16`
  - `Class is loaded, main is starting`: `1`
  - `null`: `3`
- Runs with `draw > 0`: `0 / 159`
- Hard timeouts: `49`
- Completed (without reaching draw): `104`

Aggregate pressure:
- Average max native calls per run: `2,158,311`
- Average max swaps per run: `852.6`
- Peak run: `53,454,123` calls, `4092` swaps, `0` draw

Representative peak (`tmp_visit_colony_autowatcher_v19_2026-02-12T06-20-27-558Z.json`):
- `totalCalls=53,454,123`
- `begin=1,768,278`
- `swap=4092`
- `keyPoll=3516`
- `mousePoll=3516`
- `draw=0`

Recent run set (last 8 runs) still shows:
- Avg max calls: `13,426`
- Avg max swaps: `9`
- Avg first graphics-init status: `38.1s`
- Avg draw: `0`

### Failure churn that likely magnifies runtime/JIT load
In representative recent run (`tmp_visit_colony_autowatcher_v19_2026-02-13T11-34-13-581Z.json`):
- `http_error` events: `282`
- Unique 404 URLs: `94`
- Frequent missing-resource probes against Java-like paths (e.g. `.../String.java`, nested enum/class style names).
- Console-side failed resource logs are very high-volume.

Interpretation: repeated negative path resolution and interop-bound poll/swap loops can dominate runtime cycles before any real draw path is reached.

## Internal JIT/Runtime Improvements Most Likely To Help Your Case

### P0 (highest impact)
1. Aggressive monomorphic JNI bridge specialization.
- Fast-path codegen for dominant LWJGL signatures.
- Avoid generic varargs/object marshalling in hot call sites.

2. Continuation resume optimization for frame-yield paths.
- Minimize overhead around `Promise + requestAnimationFrame` resume points.
- Keep resumed code in optimized tier when call shape remains stable.

3. Poll/swap loop tiering policy.
- Early tier-up for loops that repeatedly hit `swap`/`poll` without `draw`.
- Add loop guards that reduce redundant re-entry overhead under stable no-progress states.

### P1
4. Negative lookup cache in runtime/JIT boundary.
- Cache repeated missing path/resource probes to short-circuit 404-heavy patterns.

5. Exception path de-duplication and cheap repeat handling.
- Avoid rebuilding expensive exception metadata for repetitive, known-failing probes.

6. Hot startup method prioritization.
- Prioritize class/resource loader helpers and early GL init methods before generic background tiers.

### P2
7. Memory growth path optimization.
- Reduce churn around grow + typed-view rebind + continuation hops.

8. Persistent profile reuse across reruns.
- Reuse prior hot-method profile data for same title/workload to reduce repeated warmup debt.

## What This Means For Your Specific Goal
Your target is campaign progression and colony interaction, but nearly all runs stall before first effective draw. The strongest lever is not generic graphics tuning; it is internal JIT/runtime efficiency around:
- JNI bridge call specialization
- continuation/resume overhead
- repeated negative resource path handling

Those three areas match the exact pressure pattern in your watcher telemetry.

## Suggested Validation Metrics After Internal JIT Changes
1. Time to first `draw > 0`.
2. `totalCalls / swap` during first 120s.
3. `swap` count before first draw.
4. Repeated 404/negative lookup count per run.
5. Time from `Class is loaded, main is starting` to first draw.

## Notes
- This report is analysis-only; no source code files were modified.
- Analysis artifacts are left in `C:\Users\Ady\Documents\_cheerpj42_machine_read` and generated watcher summaries in project root.
