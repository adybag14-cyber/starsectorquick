#!/usr/bin/env python3
import os, re, subprocess, sys
from pathlib import Path

def javap(cls, cp):
    p=subprocess.run(['javap','-classpath',cp,'-c','-p',cls],capture_output=True,text=True,encoding='utf-8',errors='replace')
    if p.returncode: raise SystemExit(p.stderr)
    return p.stdout
cp=os.pathsep.join(['jars/starfarer_obf.jar','jars/fs.common_obf.jar','jars/lwjgl.jar'])
caller=javap('com.fs.starfarer.renderers.damage.String',cp)
batcher=javap('com.fs.graphics.F',cp)
# The renderer must read forceNoVBO and pass that same local boolean as the third
# F(Object,int,boolean) argument at both constructor sites.
if caller.count('// String forceNoVBO') != 1:
    raise SystemExit(f'forceNoVBO load count mismatch: {caller.count("// String forceNoVBO")}')
ctor='com/fs/graphics/F."<init>":(Lcom/fs/graphics/Object;IZ)V'
if caller.count(ctor) != 2:
    raise SystemExit(f'graphics F constructor count mismatch: {caller.count(ctor)}')
# Capture enough context around both invokespecial instructions and require iload_1
# immediately in the argument setup. iload_1 is the forceNoVBO value stored after
# StarfarerSettings boolean lookup.
for m in re.finditer(re.escape(ctor),caller):
    context=caller[max(0,m.start()-700):m.end()+100]
    if not re.search(r'iload_1\s*\n\s*\d+: invokespecial[^\n]*'+re.escape(ctor),context):
        raise SystemExit('forceNoVBO is no longer passed unchanged to F constructor\n'+context)
# F starts VBO enabled, disables it when extension missing, and also disables it
# when constructor boolean is true. This proves false + advertised extension means active.
for token in ('// String GL_ARB_vertex_buffer_object','Field for:Z','org/lwjgl/opengl/GL11.glGetString'):
    if token not in batcher: raise SystemExit(f'missing F constructor VBO token: {token}')
# Constructor must contain the third-arg branch `iload_3; ifeq` followed by false write.
ctor_start=batcher.find('public com.fs.graphics.F(com.fs.graphics.Object, int, boolean);')
ctor_end=batcher.find('\n  public ',ctor_start+10)
ctor_block=batcher[ctor_start:ctor_end if ctor_end>0 else None]
if not re.search(r'iload_3\s*\n\s*\d+: ifeq\s+\d+.*?iconst_0\s*\n\s*\d+: putfield\s+#\d+\s+// Field for:Z',ctor_block,re.S):
    raise SystemExit('F constructor third-argument VBO disable branch changed')
print('verify-starsector-vbo-activation: OK forceNoVBO passes unchanged to 2 combat damage VBO batchers')
