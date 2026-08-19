#!/usr/bin/env python3
import json, re, statistics
from pathlib import Path

ROOT = Path('test_output/frame-perf-ab')
ORDER = ['baseline-a', 'browser-loop', 'baseline-b']
BASELINE_SHA = '8efd0225f6d8e2c03e14080255ea9911c7eabf88'
CANDIDATE_SHA = '612c4e60b050de83ce4f73f04659ec36afc7e097'
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
        )
        shortcuts = [float(x['listenerReadyMs']) for x in (data.get('shortcutResults') or []) if x.get('listenerReadyMs') is not None]
        if shortcuts: row['shortcut_avg_ms'] = round(statistics.fmean(shortcuts), 2)
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
    a, c, b = by['baseline-a'], by['browser-loop'], by['baseline-b']
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
        '# Production vs browser-loop overhead same-runner A/B', '',
        f'Fixed seed `{SEED}`; expected world `218/917/59/21`.', '',
        '| variant | rc/verify | FPS | frame ms | p95 | p99 | jitter p95 | draws | saved | shortcut avg | world |',
        '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|',
    ]
    for r in rows:
        world = '-' if not r.get('world') else '/'.join(str(x) for x in r['world'])
        lines.append(f"| {r['name']} | {r.get('rc','-')} / {r.get('verify_rc','-')} | {fmt(r.get('fps'))} | {fmt(r.get('frame_ms'))} | {fmt(r.get('p95_ms'))} | {fmt(r.get('p99_ms'))} | {fmt(r.get('jitter_p95_ms'))} | {r.get('webgl_draws','-')} | {r.get('interleaved_saved','-')} | {fmt(r.get('shortcut_avg_ms'))} | {world} |")
    d = c['drift_adjusted']
    lines += ['', 'Drift-adjusted browser-loop delta:']
    for metric in ('fps','frame_ms','p95_ms','p99_ms','jitter_p95_ms'):
        v = d[metric]
        lines.append(f"- {metric}=n/a" if v['delta'] is None else f"- {metric}={v['delta']:+.3f} ({v['pct']:+.2f}%)")
    lines.append(f"- upload traffic: draws={c.get('interleaved_draws')} uploads={c.get('interleaved_uploads')} saved={c.get('interleaved_saved')} bytes={c.get('interleaved_bytes')}")
    (ROOT / 'summary.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('\n'.join(lines))
    for name, expected_ref in [('baseline-a', BASELINE_SHA), ('baseline-b', BASELINE_SHA), ('browser-loop', CANDIDATE_SHA)]:
        r = by[name]
        if r.get('rc') != 0 or r.get('verify_rc') != 0 or r.get('ok') is not True:
            raise SystemExit(f'candidate invalid: {name}: {r}')
        if r.get('fatal') or r.get('errors') or r.get('runtime_errors') or r.get('graphics_errors'):
            raise SystemExit(f'candidate errors: {name}: {r}')
        if r.get('seed') != SEED or r.get('world') != WORLD:
            raise SystemExit(f'world/seed mismatch: {name}: {r}')
        if r.get('ref') != expected_ref:
            raise SystemExit(f'ref mismatch: {name}: {r.get("ref")} != {expected_ref}')
    if not (c.get('interleaved_draws') and c.get('interleaved_draws') > 1000):
        raise SystemExit(f'interleaved draw marker inactive: {c}')
    if c.get('interleaved_uploads') != c.get('interleaved_draws'):
        raise SystemExit(f'interleaved upload/draw mismatch: {c}')
    if c.get('interleaved_saved') != c.get('interleaved_uploads') * 2:
        raise SystemExit(f'interleaved saved-call mismatch: {c}')
    if not (c.get('interleaved_bytes') and c.get('interleaved_bytes') > 0):
        raise SystemExit(f'interleaved byte marker inactive: {c}')
    return 0

if __name__ == '__main__': raise SystemExit(main())
