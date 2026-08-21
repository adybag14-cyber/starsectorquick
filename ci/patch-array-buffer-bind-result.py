#!/usr/bin/env python3
from pathlib import Path
p=Path('ci/campaign-render-test.js')
s=p.read_text(encoding='utf-8')
marker='arrayBufferBindCacheHitObserved: Boolean(perfAfter.arrayBufferBindCacheHitObserved)'
if marker not in s:
    anchor='      immediateInterleavedBytesDelta: Number(perfAfter.immediateInterleavedBytes || 0) - Number(perfBefore.immediateInterleavedBytes || 0),\n'
    if anchor not in s:
        raise SystemExit('missing immediateInterleavedBytesDelta anchor')
    s=s.replace(anchor, anchor+'      arrayBufferBindCacheHitObserved: Boolean(perfAfter.arrayBufferBindCacheHitObserved),\n', 1)
p.write_text(s,encoding='utf-8',newline='\n')
print('Applied ARRAY_BUFFER bind-cache result telemetry')
