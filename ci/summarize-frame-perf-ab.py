#!/usr/bin/env python3
import json, re, statistics
from pathlib import Path

ROOT = Path('test_output/frame-perf-ab')
ORDER = ['baseline-a', 'interleaved', 'baseline-b']
BASELINE_SHA = 'd4be7870e41eece34b9a73b3fe7a685818e4a5d6'
CANDIDATE_SHA = '7b80ff795da2a52eb7e985a8cadeff3528062592'
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
            fps=gp.get('recentFps'), frame_ms=gp.get('recentFrameMs'), swaps=gp.get('swapDelta'),
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
    a, c, b = by['baseline-a'], by['interleaved'], by['baseline-b']
    expected_fps = (float(a['fps']) + float(b['fps'])) / 2.0 if a.get('fps') is not None and b.get('fps') is not None else None
    expected_frame = (float(a['frame_ms']) + float(b['frame_ms'])) / 2.0 if a.get('frame_ms') is not None and b.get('frame_ms') is not None else None
    fps_delta = None if expected_fps is None or c.get('fps') is None else float(c['fps']) - expected_fps
    frame_delta = None if expected_frame is None or c.get('frame_ms') is None else float(c['frame_ms']) - expected_frame
    c['drift_adjusted'] = {
        'fps_expected': expected_fps, 'fps_delta': fps_delta,
        'fps_pct': None if expected_fps in (None, 0) or fps_delta is None else fps_delta / expected_fps * 100.0,
        'frame_expected': expected_frame, 'frame_delta': frame_delta,
        'frame_pct': None if expected_frame in (None, 0) or frame_delta is None else frame_delta / expected_frame * 100.0,
    }
    ROOT.mkdir(parents=True, exist_ok=True)
    (ROOT / 'summary.json').write_text(json.dumps(rows, indent=2) + '\n', encoding='utf-8')
    lines = [
        '# Production vs immediate-interleaved same-runner A/B', '',
        f'Fixed seed `{SEED}`; expected world `218/917/59/21`.', '',
        '| variant | rc/verify | FPS | frame ms | draws | interleaved saved | shortcut avg | world |',
        '|---|---:|---:|---:|---:|---:|---:|---|',
    ]
    for r in rows:
        world = '-' if not r.get('world') else '/'.join(str(x) for x in r['world'])
        lines.append(f"| {r['name']} | {r.get('rc','-')} / {r.get('verify_rc','-')} | {fmt(r.get('fps'))} | {fmt(r.get('frame_ms'))} | {r.get('webgl_draws','-')} | {r.get('interleaved_saved','-')} | {fmt(r.get('shortcut_avg_ms'))} | {world} |")
    d = c['drift_adjusted']
    lines += ['', 'Drift-adjusted interleaved delta:',
              f"- fps={d['fps_delta']:+.3f} ({d['fps_pct']:+.2f}%)" if d['fps_delta'] is not None else '- fps=n/a',
              f"- frame_ms={d['frame_delta']:+.3f} ({d['frame_pct']:+.2f}%)" if d['frame_delta'] is not None else '- frame_ms=n/a',
              f"- upload traffic: draws={c.get('interleaved_draws')} uploads={c.get('interleaved_uploads')} saved={c.get('interleaved_saved')} bytes={c.get('interleaved_bytes')}"]
    (ROOT / 'summary.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('\n'.join(lines))
    for name, expected_ref in [('baseline-a', BASELINE_SHA), ('baseline-b', BASELINE_SHA), ('interleaved', CANDIDATE_SHA)]:
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
