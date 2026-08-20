#!/usr/bin/env python3
import re
import sys
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit('usage: verify-gl11-color-jni-cache.py <GL11.java> <GL11.javap>')
src = Path(sys.argv[1]).read_text(encoding='utf-8')
bc = Path(sys.argv[2]).read_text(encoding='utf-8', errors='ignore')

def require(cond, msg):
    if not cond:
        raise SystemExit(msg)

def body(name, next_name):
    a = src.index(name)
    b = src.index(next_name, a)
    return src[a:b]

require('BROWSER_COLOR_JNI_CACHE_V1' in src, 'color JNI cache marker missing')
require('private static boolean currentColorCacheValid = true;' in src, 'initial valid state missing')
for c in 'RGBA':
    require(f'private static float currentColor{c} = 1.0f;' in src, f'initial white {c} missing')

c3 = body('public static void glColor3f(', 'static native void nglColor3f')
c4 = body('public static void glColor4f(', 'static native void nglColor4f')
for name, text, native in [('glColor3f', c3, 'nglColor3f'), ('glColor4f', c4, 'nglColor4f')]:
    require('!compilingList' in text, f'{name} does not preserve display-list recording')
    require('currentColorCacheValid' in text, f'{name} cache-valid guard missing')
    require('return;' in text, f'{name} duplicate fast-return missing')
    require(text.count(native + '(') == 1, f'{name} native call count changed')

call = body('public static void glCallList(', 'static native void nglCallList')
require('nglCallList' in call and 'currentColorCacheValid = false;' in call,
        'display-list playback does not invalidate color cache')
pop = body('public static void glPopAttrib()', 'static native void nglPopAttrib')
require('nglPopAttrib' in pop and 'currentColorCacheValid = false;' in pop,
        'glPopAttrib does not invalidate color cache')

# Bytecode must contain early returns in color wrappers before their native call and
# both conservative invalidation stores after state-changing native operations.
for meth, native in [('glColor3f', 'nglColor3f'), ('glColor4f', 'nglColor4f')]:
    m = re.search(rf'public static void {meth}\([^\n]+\);\s+Code:(.*?)(?=\n\s*(?:public|static|private|protected) )', bc, re.S)
    require(m is not None, f'javap method {meth} missing')
    block = m.group(1)
    native_pos = block.find(native)
    require(native_pos >= 0, f'{meth} native invocation missing')
    require('return' in block[:native_pos], f'{meth} has no pre-native fast return')
    require('compilingList' in block[:native_pos], f'{meth} bytecode list guard missing')

for meth, expected in [('glCallList', 1), ('glColor3f', 2), ('glColor4f', 2), ('glPopAttrib', 1)]:
    m = re.search(rf'public static void {meth}[^;]*;\s+Code:(.*?)(?=\n\s*(?:public|static|private|protected) )', bc, re.S)
    require(m is not None and m.group(1).count('currentColorCacheValid:Z') >= expected, f'{meth} compiled cache wiring missing')
print('verify-gl11-color-jni-cache: OK duplicate fast-return + list/attrib invalidation')
