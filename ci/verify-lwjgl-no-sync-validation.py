from pathlib import Path
import re

SOURCE = Path('build/final/wasm-modules/lwjgl.js')
text = SOURCE.read_text(encoding='utf-8')


def fail(message: str) -> None:
    raise SystemExit(message)


def mask_non_code(source: str) -> str:
    """Preserve offsets while masking JS strings and comments for brace scanning."""
    out = list(source)
    i = 0
    n = len(source)
    while i < n:
        ch = source[i]
        nxt = source[i + 1] if i + 1 < n else ''
        if ch in ('"', "'", '`'):
            quote = ch
            out[i] = ' '
            i += 1
            while i < n:
                ch = source[i]
                out[i] = '\n' if ch == '\n' else ' '
                if ch == '\\':
                    i += 1
                    if i < n:
                        out[i] = '\n' if source[i] == '\n' else ' '
                        i += 1
                    continue
                i += 1
                if ch == quote:
                    break
            continue
        if ch == '/' and nxt == '/':
            out[i] = out[i + 1] = ' '
            i += 2
            while i < n and source[i] != '\n':
                out[i] = ' '
                i += 1
            continue
        if ch == '/' and nxt == '*':
            out[i] = out[i + 1] = ' '
            i += 2
            while i < n:
                if source[i] == '*' and i + 1 < n and source[i + 1] == '/':
                    out[i] = out[i + 1] = ' '
                    i += 2
                    break
                out[i] = '\n' if source[i] == '\n' else ' '
                i += 1
            continue
        i += 1
    return ''.join(out)


code = mask_non_code(text)


def guarded_blocks(pattern: str, label: str) -> list[tuple[int, int]]:
    blocks: list[tuple[int, int]] = []
    for match in re.finditer(pattern, code, flags=re.MULTILINE):
        open_brace = code.find('{', match.end())
        if open_brace < 0:
            fail(f'{label}: guard has no block')
        depth = 0
        for pos in range(open_brace, len(code)):
            if code[pos] == '{':
                depth += 1
            elif code[pos] == '}':
                depth -= 1
                if depth == 0:
                    blocks.append((open_brace, pos))
                    break
        else:
            fail(f'{label}: unbalanced guard block')
    if not blocks:
        fail(f'{label}: guard not found')
    return blocks


strict_blocks = guarded_blocks(r'if\s*\(\s*strictWebGLValidation\s*\)', 'strictWebGLValidation')
readback_blocks = guarded_blocks(
    r'if\s*\(\s*presentationReadbackDiagnostics\s*&&',
    'presentationReadbackDiagnostics',
)


def require_scoped(call_pattern: str, blocks: list[tuple[int, int]], label: str) -> None:
    positions = [m.start() for m in re.finditer(call_pattern, code)]
    if not positions:
        fail(f'{label}: expected call not found')
    for pos in positions:
        if not any(start < pos < end for start, end in blocks):
            line = text.count('\n', 0, pos) + 1
            fail(f'{label}: unguarded call at line {line}')


require_scoped(r'glCtx\.getError\s*\(', strict_blocks, 'getError')
require_scoped(r'glCtx\.readPixels\s*\(', readback_blocks, 'readPixels')
require_scoped(r'glCtx\.checkFramebufferStatus\s*\(', readback_blocks, 'framebuffer status readback')

viewport_diag = 'presentationStats.lastViewport = Array.from(glCtx.getParameter(glCtx.VIEWPORT))'
viewport_pos = code.find(viewport_diag)
if viewport_pos < 0 or not any(start < viewport_pos < end for start, end in readback_blocks):
    fail('viewport diagnostic query is not scoped to presentationReadbackDiagnostics')

print('Verified synchronous WebGL validation/readback is opt-in and block-scoped.')
