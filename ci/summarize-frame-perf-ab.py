#!/usr/bin/env python3
import json, re, statistics
from pathlib import Path

ROOT = Path('test_output/frame-perf-ab')
ORDER = ['baseline-a', 'quad-strip-stitch', 'baseline-b']
BASELINE_SHA = '457d36910eea99e83518f936db17b1d2a8420617'
CANDIDATE_SHA = 'b262337cb4e0c8a3280c66ce129a0257a594462d'
SEED = 'SEK968276040'
WORLD = (218, 917, 59, 21)

def statuses():
    out = {}
    p = ROOT / 'status.tsv'
    if not p.exists(): return out
    for line in p.read_text(encoding='utf-8').splitlines():
        if not line.strip(): continue
        name, ref, rc, verify = line.split('\t')
        out[name] = {'ref': ref, 'rc': int(rc), 'verify_rc': int(verify)}
    return out

def parse(name):
    d = ROOT / name
    rp, lp = d / 'result.json', d / 'browser.log'
    row = {'name': name, 'result': rp.exists(), 'log': lp.exists()}
    if rp.exists():
        data = json.loads(rp.read_text(encoding='utf-8'))
        gp = data.get('gameplayPerformance') or {}
        row.update(
            ok=data.get('ok'), fatal=data.get('fatalSeenAt'), errors=len(data.get('errors') or []),
            runtime_errors=len(data.get('runtimeErrorSignals') or []), graphics_errors=len(data.get('graphicsErrors') or []),
            fps=gp.get('recentFps'), frame_ms=gp.get('recentFrameMs'), p50_ms=gp.get('frameP50Ms'), p95_ms=gp.get('frameP95Ms'), p99_ms=gp.get('frameP99Ms'), jitter_p95_ms=gp.get('frameJitterP95Ms'), swaps=gp.get('swapDelta'),
            webgl_draws=gp.get('webglDrawDelta'), quad_saved=gp.get('quadDrawCallsSavedDelta'),
            interleaved_draws=gp.get('immediateInterleavedDrawDelta'),
            interleaved_uploads=gp.get('immediateInterleavedUploadDelta'),
            interleaved_saved=gp.get('immediateInterleavedUploadsSavedDelta'),
            interleaved_bytes=gp.get('immediateInterleavedBytesDelta'),
            quad_strip_deferred=gp.get('immediateQuadStripDeferredRunDelta'),
            quad_strip_batched_runs=gp.get('immediateQuadStripBatchedRunDelta'),
            quad_strip_batched_strips=gp.get('immediateQuadStripBatchedStripDelta'),
            quad_strip_saved=gp.get('immediateQuadStripDrawCallsSavedDelta'),
            quad_strip_bridges=gp.get('immediateQuadStripBridgeVertexDelta'),
            quad_strip_uploads_saved=gp.get('immediateQuadStripUploadsSavedDelta'),
            core_calls=gp.get('coreStateCallsDelta'), core_changes=gp.get('coreStateChangesDelta'),
            core_skipped=gp.get('coreStateSkippedDelta'), core_queries_avoided=gp.get('coreStateSnapshotQueriesAvoidedDelta'),
        )
        shortcuts = [float(x['listenerReadyMs']) for x in (data.get('shortcutResults') or []) if x.get('listenerReadyMs') is not None]
        if shortcuts: row['shortcut_avg_ms'] = round(statistics.fmean(shortcuts), 2)
        input_stats = ((data.get('state') or {}).get('inputStats') or {})
        row['mouse_snapshots'] = int(input_stats.get('mousePollSnapshots') or 0)
        row['mouse_snapshot_clamps'] = int(input_stats.get('mousePollPackedClamps') or 0)
        row['mouse_button_queries'] = int(input_stats.get('mouseButtonQueries') or 0)
        row['mouse_position_queries'] = int(input_stats.get('mousePositionQueries') or 0)
    if lp.exists():
        text = lp.read_text(encoding='utf-8', errors='ignore')
        worlds = re.findall(r'world-ready systems=(\d+) planets=(\d+) markets=(\d+) factions=(\d+)', text)
        if worlds: row['world'] = tuple(int(x) for x in worlds[-1])
        seeds = re.findall(r'seedString=(SEK\d+)', text)
        if seeds: row['seed'] = seeds[-1]
    return row

def fmt(v): return '-' if v is None else f'{float(v):.2f}'

def main():
    st = statuses(); rows = [parse(n) for n in ORDER]
    for r in rows: r.update(st.get(r['name'], {}))
    by = {r['name']: r for r in rows}
    a, c, b = by['baseline-a'], by['quad-strip-stitch'], by['baseline-b']
    c['drift_adjusted'] = {}
    for metric in ('fps','frame_ms','p95_ms','p99_ms','jitter_p95_ms'):
        av, bv, cv = a.get(metric), b.get(metric), c.get(metric)
        expected = None if av is None or bv is None else (float(av) + float(bv)) / 2.0
        delta = None if expected is None or cv is None else float(cv) - expected
        c['drift_adjusted'][metric] = {
            'expected': expected,
            'delta': delta,
            'pct': None if expected in (None, 0) or delta is None else delta / expected * 100.0,
        }
    ROOT.mkdir(parents=True, exist_ok=True)
    (ROOT / 'summary.json').write_text(json.dumps(rows, indent=2) + '\n', encoding='utf-8')
    lines = [
        '# Production vs immediate quad-strip stitch same-runner A/B', '',
        f'Fixed seed `{SEED}`; expected world `218/917/59/21`.', '',
        '| variant | rc/verify | FPS | frame ms | p95 | p99 | jitter p95 | shortcut avg | quad saved | batch runs | batched strips | uploads saved | draws | uploads | world |',
        '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|',
    ]
    for r in rows:
        world = '-' if not r.get('world') else '/'.join(str(x) for x in r['world'])
        lines.append(f"| {r['name']} | {r.get('rc','-')} / {r.get('verify_rc','-')} | {fmt(r.get('fps'))} | {fmt(r.get('frame_ms'))} | {fmt(r.get('p95_ms'))} | {fmt(r.get('p99_ms'))} | {fmt(r.get('jitter_p95_ms'))} | {fmt(r.get('shortcut_avg_ms'))} | {r.get('quad_strip_saved','-')} | {r.get('quad_strip_batched_runs','-')} | {r.get('quad_strip_batched_strips','-')} | {r.get('quad_strip_uploads_saved','-')} | {r.get('interleaved_draws','-')} | {r.get('interleaved_uploads','-')} | {world} |")
    d = c['drift_adjusted']
    lines += ['', 'Drift-adjusted quad-strip-stitch delta:']
    for metric in ('fps','frame_ms','p95_ms','p99_ms','jitter_p95_ms'):
        v = d[metric]
        lines.append(f"- {metric}=n/a" if v['delta'] is None else f"- {metric}={v['delta']:+.3f} ({v['pct']:+.2f}%)")
    lines.append(f"- quad-strip traffic: deferred={c.get('quad_strip_deferred')} batchedRuns={c.get('quad_strip_batched_runs')} batchedStrips={c.get('quad_strip_batched_strips')} drawSaved={c.get('quad_strip_saved')} bridges={c.get('quad_strip_bridges')} uploadsSaved={c.get('quad_strip_uploads_saved')} draws={c.get('interleaved_draws')} uploads={c.get('interleaved_uploads')}")
    (ROOT / 'summary.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('\n'.join(lines))
    for name, expected_ref in [('baseline-a', BASELINE_SHA), ('baseline-b', BASELINE_SHA), ('quad-strip-stitch', CANDIDATE_SHA)]:
        r = by[name]
        if r.get('rc') != 0 or r.get('verify_rc') != 0 or r.get('ok') is not True:
            raise SystemExit(f'candidate invalid: {name}: {r}')
        if r.get('fatal') or r.get('errors') or r.get('runtime_errors') or r.get('graphics_errors'):
            raise SystemExit(f'candidate errors: {name}: {r}')
        if r.get('seed') != SEED or r.get('world') != WORLD:
            raise SystemExit(f'world/seed mismatch: {name}: {r}')
        if r.get('ref') != expected_ref:
            raise SystemExit(f'ref mismatch: {name}: {r.get("ref")} != {expected_ref}')
    if int(c.get('interleaved_draws') or 0) < 1000 or int(c.get('interleaved_uploads') or 0) < 1000:
        raise SystemExit(f'quad-strip traffic evidence invalid: {c}')
    deferred = int(c.get('quad_strip_deferred') or 0)
    runs = int(c.get('quad_strip_batched_runs') or 0)
    strips = int(c.get('quad_strip_batched_strips') or 0)
    saved = int(c.get('quad_strip_saved') or 0)
    bridges = int(c.get('quad_strip_bridges') or 0)
    uploads_saved = int(c.get('quad_strip_uploads_saved') or 0)
    if deferred <= 0 or runs <= 0 or strips <= runs or saved <= 0 or uploads_saved <= 0:
        raise SystemExit(f'quad-strip candidate did not activate or save work: {c}')
    if saved != strips - runs or bridges != saved * 2 or uploads_saved != saved:
        raise SystemExit(f'quad-strip telemetry accounting mismatch: {c}')
    for baseline in (a, b):
        if any(int(baseline.get(k) or 0) != 0 for k in ('quad_strip_deferred','quad_strip_batched_runs','quad_strip_batched_strips','quad_strip_saved','quad_strip_bridges','quad_strip_uploads_saved')):
            raise SystemExit(f'production baseline unexpectedly reports quad-strip batching: {baseline}')
    return 0

if __name__ == '__main__': raise SystemExit(main())
