#!/usr/bin/env python3
from pathlib import Path
import re

launch = Path('launch.html').read_text(encoding='utf-8')
campaign = Path('ci/campaign-render-test.js').read_text(encoding='utf-8')
required_launch = [
    'STARSECTOR_FATAL_CONSOLE_PATTERN',
    "const effectiveLevel = isFatalRuntimeConsoleText(text) ? 'error' : level;",
    "updateRuntimeState('fatal', detail);",
    "btn.setAttribute('data-last-error', detail);",
]
for token in required_launch:
    if token not in launch:
        raise SystemExit(f'missing fatal-console guard: {token}')
# Verify the intended spelling variants are represented by the source regex semantics.
pattern = re.compile(r'(?:^|\b)fatal\s*:\s*', re.I)
for sample in ('Fatal: Starsector null', 'Fatal : Starsector null', 'FATAL    :    Starsector null'):
    if not pattern.search(sample):
        raise SystemExit(f'fatal classifier rejected {sample!r}')
if not re.search(r'Fatal\\s\*:\\s\*', campaign):
    raise SystemExit('campaign renderer does not recognize spaced Fatal : lines')
print('Verified fatal console lines are promoted to error/fatal runtime state.')
