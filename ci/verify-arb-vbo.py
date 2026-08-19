#!/usr/bin/env python3
from pathlib import Path

js = Path('build/final/wasm-modules/lwjgl.js').read_text(encoding='utf-8')
required = [
    'LWJGL_ARB_VBO_COMPAT_V1',
    'Java_org_lwjgl_opengl_ARBBufferObject_nglGenBuffersARB',
    'Java_org_lwjgl_opengl_ARBBufferObject_nglDeleteBuffersARB',
    'Java_org_lwjgl_opengl_ARBBufferObject_nglBindBufferARB',
    'Java_org_lwjgl_opengl_ARBBufferObject_nglBufferDataARB',
    'Java_org_lwjgl_opengl_ARBBufferObject_nglBufferSubDataARB',
    'GL_ARB_vertex_buffer_object',
    'data.vbo > 0',
    'glCtx.bufferSubData',
    'window.__lwjglVboStats',
]
for token in required:
    if token not in js:
        raise SystemExit(f'missing VBO bridge token: {token}')


for source in (
    Path('bridge_src/org/lwjgl/opengl/ARBBufferObject.java'),
    Path('bridge_src/org/lwjgl/opengl/ARBVertexBufferObject.java'),
):
    if not source.is_file():
        raise SystemExit(f'missing bridge-owned ARB facade: {source}')
arb = Path('bridge_src/org/lwjgl/opengl/ARBBufferObject.java').read_text(encoding='utf-8')
for token in ('glGenBuffersARB()', 'glDeleteBuffersARB(int buffer)', 'glBindBufferARB(int target, int buffer)', 'glBufferDataARB(int target, long size, int usage)', 'glBufferSubDataARB(int target, long offset, FloatBuffer data)'):
    if token not in arb:
        raise SystemExit(f'missing bridge ARB API: {token}')

campaign = Path('ci/campaign-render-test.js').read_text(encoding='utf-8')
for token in ('vboActive', 'vboStats.generated', 'vboStats.subDataCalls', 'vboStats.vboDraws'):
    if token not in campaign:
        raise SystemExit(f'missing VBO campaign telemetry: {token}')
combat = Path('ci/vbo-combat-smoke.js').read_text(encoding='utf-8')
for token in ('__STARSECTOR_BOOT__', "'combat'", 'stats.generated', 'stats.subDataCalls', 'stats.vboDraws', '[vbo-combat-smoke]'):
    if token not in combat:
        raise SystemExit(f'missing VBO combat activation gate: {token}')
runner = Path('ci/run-campaign-experiment.sh').read_text(encoding='utf-8')
for token in ('STARSECTOR_VBO_COMBAT_SMOKE', 'node ci/vbo-combat-smoke.js', 'Verified browser combat ARB VBO activation'):
    if token not in runner:
        raise SystemExit(f'missing VBO combat workflow hook: {token}')

import json
import re


def parse_starsector_settings(path: Path):
    raw = path.read_text(encoding='utf-8-sig')
    lines = []
    for line in raw.splitlines():
        in_string = False
        escaped = False
        keep = []
        for ch in line:
            if escaped:
                keep.append(ch)
                escaped = False
                continue
            if ch == '\\' and in_string:
                keep.append(ch)
                escaped = True
                continue
            if ch == '"':
                keep.append(ch)
                in_string = not in_string
                continue
            if ch == '#' and not in_string:
                break
            keep.append(ch)
        lines.append(''.join(keep))
    cleaned = '\n'.join(lines)
    # Normalize Starsector numeric extensions outside strings: leading-decimal
    # floats (.5 / -.5) and Java-style float suffixes (1f / 0.5f).
    out = []
    i = 0
    in_string = False
    escaped = False
    while i < len(cleaned):
        ch = cleaned[i]
        if escaped:
            out.append(ch); escaped = False; i += 1; continue
        if ch == '\\' and in_string:
            out.append(ch); escaped = True; i += 1; continue
        if ch == '"':
            out.append(ch); in_string = not in_string; i += 1; continue
        if not in_string:
            prev = cleaned[i - 1] if i > 0 else ' '
            if ch == '.' and i + 1 < len(cleaned) and cleaned[i + 1].isdigit() and (prev.isspace() or prev in ':,[]'):
                out.append('0')
            elif ch == '-' and i + 2 < len(cleaned) and cleaned[i + 1] == '.' and cleaned[i + 2].isdigit() and (prev.isspace() or prev in ':,[]'):
                out.append('-0.')
                i += 2
                continue
            elif ch in 'fF' and out and out[-1][-1:].isdigit():
                nxt = cleaned[i + 1] if i + 1 < len(cleaned) else ''
                if not nxt or nxt.isspace() or nxt in ',;}]':
                    i += 1
                    continue
            elif ch == ';':
                # Older Starsector settings snapshots use semicolons as entry delimiters.
                ch = ','
        out.append(ch)
        i += 1
    normalized = ''.join(out)
    # Starsector also permits trailing commas. Remove them only outside strings.
    out2 = []
    i = 0
    in_string = False
    escaped = False
    while i < len(normalized):
        ch = normalized[i]
        if escaped:
            out2.append(ch); escaped = False; i += 1; continue
        if ch == '\\' and in_string:
            out2.append(ch); escaped = True; i += 1; continue
        if ch == '"':
            out2.append(ch); in_string = not in_string; i += 1; continue
        if ch == ',' and not in_string:
            j = i + 1
            while j < len(normalized) and normalized[j].isspace():
                j += 1
            if j < len(normalized) and normalized[j] in '}]':
                i += 1
                continue
        out2.append(ch)
        i += 1
    final_text = ''.join(out2)
    try:
        value = json.loads(final_text)
    except json.JSONDecodeError as exc:
        context = final_text.splitlines()
        lo = max(0, exc.lineno - 3)
        hi = min(len(context), exc.lineno + 2)
        excerpt = '\n'.join(f'{i + 1}: {context[i]}' for i in range(lo, hi))
        raise RuntimeError(f'{path}: Starsector settings parse failed: {exc}\n{excerpt}') from exc
    if not isinstance(value, dict):
        raise RuntimeError(f'{path}: expected root object')
    return value


for name in ('data/config/settings.json','resources/settings.json','starsector/starsector/settings.json','starsector/starsector/data/config/settings.json'):
    value = parse_starsector_settings(Path(name))
    if value.get('forceNoVBO') is not False:
        raise SystemExit(f'{name}: root-level forceNoVBO must be boolean false, got {value.get("forceNoVBO")!r}')

print('Verified Starsector ARB VBO bridge and candidate settings.')
