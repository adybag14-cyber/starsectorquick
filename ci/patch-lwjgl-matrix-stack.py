#!/usr/bin/env python3
"""Harden the WebGL LWJGL matrix stacks against OpenGL stack underflow.

The browser bridge previously allowed glPopMatrix() to remove the final identity
matrix. The next glPushMatrix() then attempted to clone ``undefined`` and stopped
the CheerpJ VM. This patch is intentionally idempotent so it can be used both in
CI and by the one-shot branch materialisation workflow.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LWJGL = ROOT / "build" / "final" / "wasm-modules" / "lwjgl.js"
MARKER = "LWJGL_MATRIX_STACK_GUARD_V1"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def patch_text(text: str) -> tuple[str, bool]:
    if MARKER in text:
        return text, False

    old_stack_declaration = "var curMatrixStack = modelViewMatrixStack;\n"
    new_stack_declaration = f"""var curMatrixStack = modelViewMatrixStack;
// {MARKER}: OpenGL matrix stacks always retain their base identity matrix.
var matrixStackWarnings = new Set();
function ensureCurMatrixStack()
{{
\tif(!Array.isArray(curMatrixStack))
\t\tthrow new Error("LWJGL current matrix stack is invalid");
\tif(curMatrixStack.length === 0)
\t{{
\t\twarnOnce(
\t\t\tmatrixStackWarnings,
\t\t\t"matrix-stack-empty-recovery",
\t\t\t"LWJGL recovered an empty matrix stack with an identity matrix."
\t\t);
\t\tcurMatrixStack.push(glMatrix.mat4.create());
\t}}
\treturn curMatrixStack;
}}
"""
    text = replace_once(
        text,
        old_stack_declaration,
        new_stack_declaration,
        "current matrix-stack declaration",
    )

    old_get_top = """function getCurMatrixTop()
{
\treturn curMatrixStack[curMatrixStack.length - 1];
}
"""
    new_get_top = """function getCurMatrixTop()
{
\tvar stack = ensureCurMatrixStack();
\treturn stack[stack.length - 1];
}
"""
    text = replace_once(text, old_get_top, new_get_top, "get matrix top helper")

    text = replace_once(
        text,
        "\tcurMatrixStack[curMatrixStack.length - 1] = m;",
        "\tvar stack = ensureCurMatrixStack();\n\tstack[stack.length - 1] = m;",
        "set matrix top assignment",
    )
    text = replace_once(
        text,
        "\tcurMatrixStack.push(glMatrix.mat4.clone(curMatrixStack[curMatrixStack.length - 1]));",
        "\tvar stack = ensureCurMatrixStack();\n\tstack.push(glMatrix.mat4.clone(stack[stack.length - 1]));",
        "matrix push",
    )
    text = replace_once(
        text,
        "\tcurMatrixStack.pop();",
        """\tvar stack = ensureCurMatrixStack();
\tif(stack.length <= 1)
\t{
\t\twarnOnce(
\t\t\tmatrixStackWarnings,
\t\t\t"matrix-stack-underflow",
\t\t\t"LWJGL ignored glPopMatrix at the base matrix to prevent stack underflow."
\t\t);
\t\treturn;
\t}
\tstack.pop();""",
        "matrix pop",
    )

    return text, True


def main() -> None:
    text = LWJGL.read_text(encoding="utf-8")
    text, changed = patch_text(text)
    if not changed:
        print("LWJGL matrix-stack guard already present")
        return

    LWJGL.write_text(text, encoding="utf-8")
    print("Applied LWJGL matrix-stack underflow guard")


if __name__ == "__main__":
    main()
