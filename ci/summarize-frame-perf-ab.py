#!/usr/bin/env python3
import json, math, re, statistics
from pathlib import Path

ROOT = Path('test_output/frame-perf-ab')
ORDER = ['baseline-a', 'stream-hint', 'texture', 'leak-off', 'matrix-inplace', 'baseline-b']
METRICS = ['fps','frame_ms','p50_ms','p95_ms','p99_ms','max_ms','jitter_stddev_ms','jitter_p95_ms','shortcut_avg_ms','shortcut_p95_ms','shortcut_max_ms','dropped_estimate']

def pct(values, q):
    if not values: return None
    s=sorted(values); return s[max(0,min(len(s)-1,math.ceil(q*len(s))-1))]

def statuses():
    out={}; p=ROOT/'status.tsv'
    if not p.exists(): return out
    for line in p.read_text(encoding='utf-8').splitlines():
        if not line.strip(): continue
        name,ref,rc,verify=line.split('\t'); out[name]={'ref':ref,'rc':int(rc),'verify_rc':int(verify)}
    return out

def parse(name):
    d=ROOT/name; rp=d/'result.json'; lp=d/'browser.log'; row={'name':name,'result':rp.exists(),'log':lp.exists()}
    if rp.exists():
        data=json.loads(rp.read_text(encoding='utf-8')); gp=data.get('gameplayPerformance') or {}
        row.update(ok=data.get('ok'), fatal=data.get('fatalSeenAt'), errors=len(data.get('errors') or []),
          fps=gp.get('recentFps'), frame_ms=gp.get('recentFrameMs'), p50_ms=gp.get('frameP50Ms'),
          p95_ms=gp.get('frameP95Ms'), p99_ms=gp.get('frameP99Ms'), max_ms=gp.get('frameMaxMs'),
          jitter_stddev_ms=gp.get('frameJitterStdDevMs'), jitter_p95_ms=gp.get('frameJitterP95Ms'),
          long_frames=gp.get('longFrameDelta'), dropped_estimate=gp.get('droppedFrameEstimateDelta'),
          swaps=gp.get('swapDelta'), webgl_draws=gp.get('webglDrawDelta'),
          matrix_uploads=gp.get('matrixUniformUploadDelta'), matrix_saved=gp.get('matrixUniformSavedDelta'),
          attrib_pointer_updates=gp.get('vertexAttribPointerUpdateDelta'), attrib_pointer_saved=gp.get('vertexAttribPointerSavedDelta'),
          attrib_enable_changes=gp.get('vertexAttribEnableChangeDelta'), attrib_enable_saved=gp.get('vertexAttribEnableSavedDelta'),
          vertex_buffer_uploads=gp.get('vertexBufferUploadDelta'), vertex_upload_bytes=gp.get('vertexUploadBytesDelta'),
          attrib_pushes=gp.get('attribPushDelta'), attrib_pops=gp.get('attribPopDelta'),
          attrib_queries_avoided=gp.get('attribSnapshotQueriesAvoidedDelta'), attrib_queries_executed=gp.get('attribSnapshotQueriesExecutedDelta'),
          texture_bind_calls=gp.get('textureBindCallsDelta'), texture_bind_changes=gp.get('textureBindChangesDelta'),
          texture_bind_skipped=gp.get('textureBindSkippedDelta'), texture_bind_invalidations=gp.get('textureBindInvalidationsDelta'),
          matrix_inplace_ops=gp.get('matrixInPlaceOpsDelta'), matrix_temp_alloc_avoided=gp.get('matrixTempAllocationsAvoidedDelta'),
          matrix_legacy_ops=gp.get('matrixLegacyOpsDelta'))
        shortcuts=[float(x['listenerReadyMs']) for x in (data.get('shortcutResults') or []) if x.get('listenerReadyMs') is not None]
        if shortcuts:
            row.update(shortcut_avg_ms=round(statistics.fmean(shortcuts),2), shortcut_p95_ms=pct(shortcuts,.95), shortcut_max_ms=max(shortcuts), shortcut_count=len(shortcuts))
        after=data.get('shortcutAfter') or {}
        row.update(keyboard_queue_latency_avg_ms=after.get('directKeyboardLatencyAvgMs'), keyboard_queue_latency_max_ms=after.get('directKeyboardLatencyMaxMs'),
                   keyboard_drops=after.get('directKeyboardDropped'), keyboard_queue_high_water=after.get('keyboardQueueHighWater'))
    if lp.exists():
        text=lp.read_text(encoding='utf-8',errors='ignore')
        worlds=re.findall(r'world-ready systems=(\d+) planets=(\d+) markets=(\d+) factions=(\d+)',text)
        if worlds: row['world']=tuple(int(x) for x in worlds[-1])
        seeds=re.findall(r'seedString=(SEK\d+)',text)
        if seeds: row['seed']=seeds[-1]
        pacer=re.findall(r'BrowserFramePacer: calls=(\d+).*?sleeps=(\d+).*?late=(\d+).*?resets=(\d+)',text)
        if pacer: row['pacer']=tuple(int(x) for x in pacer[-1])
    return row

def expected(a,b,pos):
    if a is None or b is None: return None
    return float(a)+(float(b)-float(a))*(pos/float(len(ORDER)-1))

def fmt(v): return '-' if v is None else f'{float(v):.2f}'

def main():
    st=statuses(); rows=[parse(n) for n in ORDER]
    for r in rows: r.update(st.get(r['name'],{}))
    by={r['name']:r for r in rows}; a=by['baseline-a']; b=by['baseline-b']
    for pos,name in enumerate(ORDER):
        if name.startswith('baseline'): continue
        r=by[name]; r['drift_adjusted']={}
        for m in METRICS:
            exp=expected(a.get(m),b.get(m),pos); value=r.get(m)
            if exp is None or value is None: r['drift_adjusted'][m]=None; continue
            delta=float(value)-exp; r['drift_adjusted'][m]={'expected':round(exp,3),'delta':round(delta,3),'pct':None if exp==0 else round(delta/exp*100,2)}
    ROOT.mkdir(parents=True,exist_ok=True); (ROOT/'summary.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8')
    lines=['# Same-runner browser frame performance A/B','', 'Fixed seed `SEK968276040`; expected world `218/917/59/21`.','',
      '| variant | rc/verify | FPS | frame ms | p95 | p99 | jitter p95 | shortcut avg | world |','|---|---:|---:|---:|---:|---:|---:|---:|---|']
    for r in rows:
        world='-' if not r.get('world') else '/'.join(str(x) for x in r['world'])
        lines.append(f"| {r['name']} | {r.get('rc','-')} / {r.get('verify_rc','-')} | {fmt(r.get('fps'))} | {fmt(r.get('frame_ms'))} | {fmt(r.get('p95_ms'))} | {fmt(r.get('p99_ms'))} | {fmt(r.get('jitter_p95_ms'))} | {fmt(r.get('shortcut_avg_ms'))} | {world} |")
    lines += ['', 'Drift-adjusted candidate deltas:']
    for name in ['stream-hint','texture','leak-off','matrix-inplace']:
        r=by[name]; parts=[]
        for m in ['fps','frame_ms','p95_ms','p99_ms','jitter_p95_ms','shortcut_avg_ms']:
            v=r['drift_adjusted'].get(m)
            parts.append(f"{m}=n/a" if v is None else f"{m}={v['delta']:+.2f} ({v['pct']:+.2f}%)")
        lines.append(f"- {name}: "+', '.join(parts))
        lines.append(f"  counters: texture={r.get('texture_bind_calls','-')}calls/{r.get('texture_bind_changes','-')}changes/{r.get('texture_bind_skipped','-')}skipped/{r.get('texture_bind_invalidations','-')}invalidations matrixInPlace={r.get('matrix_inplace_ops','-')} ops/{r.get('matrix_temp_alloc_avoided','-')} tempAllocAvoided legacyMatrix={r.get('matrix_legacy_ops','-')}")
    (ROOT/'summary.md').write_text('\n'.join(lines)+'\n',encoding='utf-8'); print('\n'.join(lines))
    for name in ['baseline-a','baseline-b']:
        r=by[name]
        if r.get('rc')!=0 or r.get('verify_rc')!=0 or r.get('ok') is not True: raise SystemExit(f'baseline failed: {name}: {r}')
        if r.get('seed')!='SEK968276040' or r.get('world')!=(218,917,59,21): raise SystemExit(f'baseline world mismatch: {name}: {r}')
    return 0

if __name__=='__main__': raise SystemExit(main())
