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

campaign = Path('ci/campaign-render-test.js').read_text(encoding='utf-8')
for token in ('vboActive', 'vboStats.generated', 'vboStats.subDataCalls', 'vboStats.vboDraws'):
    if token not in campaign:
        raise SystemExit(f'missing VBO runtime gate: {token}')

for name in ('data/config/settings.json','resources/settings.json','starsector/starsector/settings.json','starsector/starsector/data/config/settings.json'):
    text=Path(name).read_text(encoding='utf-8-sig')
    if '"forceNoVBO":false' not in text:
        raise SystemExit(f'VBO candidate is not enabled in {name}')
print('Verified Starsector ARB VBO bridge and candidate settings.')
