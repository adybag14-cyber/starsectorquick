#!/usr/bin/env python3
import json, re, statistics
from pathlib import Path

ROOT = Path('test_output/frame-perf-ab')
ORDER = ['baseline-a', 'input-query-off', 'baseline-b']
BASELINE_SHA = '60e21b1fef5985f69e0de1fd39290df696a67033'
CANDIDATE_SHA = 'ab9455cc500d9e7580a4a072b748862b2f47d42d'
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
            input_query_present=bool(gp.get('inputQueryTelemetryPresent')), input_query_active=bool(gp.get('inputQueryTelemetryActive')),
            input_keyboard_queries=int(gp.get('inputKeyboardStateQueries') or 0), input_keyboard_pressed=int(gp.get('inputKeyboardPressedQueries') or 0),
            input_mouse_button_queries=int(gp.get('inputMouseButtonQueries') or 0), input_mouse_pressed=int(gp.get('inputMousePressedQueries') or 0), input_mouse_position_queries=int(gp.get('inputMousePositionQueries') or 0),
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
    a, c, b = by['baseline-a'], by['input-query-off'], by['baseline-b']
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
        '# Current production vs input query telemetry opt-out same-runner A/B', '',
        f'Fixed seed `{SEED}`; expected world `218/917/59/21`.', '',
        '| variant | rc/verify | FPS | frame ms | p95 | p99 | jitter p95 | shortcut avg | input queries | draws | world |',
        '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|',
    ]
    for r in rows:
        world = '-' if not r.get('world') else '/'.join(str(x) for x in r['world'])
        q = sum(int(r.get(k) or 0) for k in ('input_keyboard_queries','input_keyboard_pressed','input_mouse_button_queries','input_mouse_pressed','input_mouse_position_queries'))
        lines.append(f"| {r['name']} | {r.get('rc','-')} / {r.get('verify_rc','-')} | {fmt(r.get('fps'))} | {fmt(r.get('frame_ms'))} | {fmt(r.get('p95_ms'))} | {fmt(r.get('p99_ms'))} | {fmt(r.get('jitter_p95_ms'))} | {fmt(r.get('shortcut_avg_ms'))} | {q} | {r.get('interleaved_draws','-')} | {world} |")
    d = c['drift_adjusted']
    lines += ['', 'Drift-adjusted input query telemetry opt-out delta:']
    for metric in ('fps','frame_ms','p95_ms','p99_ms','jitter_p95_ms'):
        v = d[metric]
        lines.append(f"- {metric}=n/a" if v['delta'] is None else f"- {metric}={v['delta']:+.3f} ({v['pct']:+.2f}%)")
    lines.append(f"- input query counters: keyboard={c.get('input_keyboard_queries')}/{c.get('input_keyboard_pressed')} mouse={c.get('input_mouse_button_queries')}/{c.get('input_mouse_pressed')}/{c.get('input_mouse_position_queries')} present={c.get('input_query_present')} active={c.get('input_query_active')}")
    (ROOT / 'summary.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('\n'.join(lines))
    for name, expected_ref in [('baseline-a', BASELINE_SHA), ('baseline-b', BASELINE_SHA), ('input-query-off', CANDIDATE_SHA)]:
        r = by[name]
        if r.get('rc') != 0 or r.get('verify_rc') != 0 or r.get('ok') is not True:
            raise SystemExit(f'candidate invalid: {name}: {r}')
        if r.get('fatal') or r.get('errors') or r.get('runtime_errors') or r.get('graphics_errors'):
            raise SystemExit(f'candidate errors: {name}: {r}')
        if r.get('seed') != SEED or r.get('world') != WORLD:
            raise SystemExit(f'world/seed mismatch: {name}: {r}')
        if r.get('ref') != expected_ref:
            raise SystemExit(f'ref mismatch: {name}: {r.get("ref")} != {expected_ref}')
    def query_total(r): return sum(int(r.get(k) or 0) for k in ('input_keyboard_queries','input_keyboard_pressed','input_mouse_button_queries','input_mouse_pressed','input_mouse_position_queries'))
    if int(c.get('interleaved_draws') or 0) < 1000 or c.get('input_query_present') is not True or c.get('input_query_active') is not False or query_total(c) != 0:
        raise SystemExit(f'input query telemetry opt-out activation invalid: total={query_total(c)} row={c}')
    if a.get('input_query_present') or b.get('input_query_present') or query_total(a) < 1000 or query_total(b) < 1000:
        raise SystemExit(f'baseline input query evidence invalid: A={query_total(a)} B={query_total(b)} a={a} b={b}')
    return 0

if __name__ == '__main__': raise SystemExit(main())
