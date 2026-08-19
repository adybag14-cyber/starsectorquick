#!/usr/bin/env python3
import re, sys
from pathlib import Path
if len(sys.argv) != 3:
    raise SystemExit('usage: verify-direct-input-noop-poll.py <Display.javap> <lwjgl.js>')
javap=Path(sys.argv[1]).read_text(encoding='utf-8',errors='replace')
js=Path(sys.argv[2]).read_text(encoding='utf-8',errors='replace')
for token in ('function Java_org_lwjgl_input_Keyboard_nPoll() {}','function Java_org_lwjgl_input_Mouse_nPoll() {}'):
    if token not in js: raise SystemExit(f'native poll is no longer no-op: {token}')
m=re.search(r'public static void processMessages\(\);\s+Code:(.*?)(?=\n\s+(?:public|private|static|protected))',javap,re.S)
if not m: raise SystemExit('processMessages javap block missing')
lines=[x.strip() for x in m.group(1).splitlines() if x.strip()]
try:
    flag=next(i for i,x in enumerate(lines) if 'LEGACY_INPUT_POLL' in x)
    poll=next(i for i,x in enumerate(lines) if 'pollDevices' in x)
    ret=next(i for i,x in enumerate(lines[flag+1:poll],flag+1) if re.search(r': return$',x))
except StopIteration:
    raise SystemExit('processMessages fast/fallback bytecode shape changed')
if not (flag < ret < poll): raise SystemExit(f'default return no longer dominates pollDevices flag={flag} ret={ret} poll={poll}')
cl=re.search(r'static \{\};\s+Code:(.*?)(?=\n\})',javap,re.S)
if not cl or cl.group(1).count('browserLegacyInputPoll') != 1:
    raise SystemExit('legacy input property must be resolved exactly once in <clinit>')
print('verify-direct-input-noop-poll: OK default skips 2 no-op JNI polls; legacy fallback preserved')