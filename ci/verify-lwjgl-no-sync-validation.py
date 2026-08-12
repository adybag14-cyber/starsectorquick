from pathlib import Path
p=Path('ci/verify-lwjgl-no-sync-validation.py')
s=Path('build/final/wasm-modules/lwjgl.js').read_text()
required=[
 'strictWebGLValidation',
 'presentationReadbackDiagnostics',
 'if(strictWebGLValidation)',
 'if(presentationReadbackDiagnostics && presentationStats.samples.length < 8',
]
for token in required:
 assert token in s, token
# Any remaining getError is allowed only in the explicit strict-validation blocks or the
# Java-facing glGetError shim which intentionally returns 0 without querying WebGL.
lines=s.splitlines()
for i,line in enumerate(lines):
 if 'glCtx.getError()' not in line: continue
 context='\n'.join(lines[max(0,i-4):i+2])
 assert 'strictWebGLValidation' in context, f'ungated getError at line {i+1}'
print('Verified synchronous WebGL validation/readback is opt-in.')
