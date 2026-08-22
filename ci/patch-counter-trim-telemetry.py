#!/usr/bin/env python3
from pathlib import Path
p=Path('ci/campaign-render-test.js')
s=p.read_text(encoding='utf-8')
marker='immediateCounterTrimActive: Boolean(perfAfter.immediateCounterTrimActive),'
if marker not in s:
    anchor='      immediateInterleavedBytesDelta: Number(perfAfter.immediateInterleavedBytes || 0) - Number(perfBefore.immediateInterleavedBytes || 0),\n'
    assert anchor in s
    s=s.replace(anchor,anchor+'      '+marker+'\n',1)
p.write_text(s,encoding='utf-8',newline='\n')
print('Applied counter-trim result telemetry')
