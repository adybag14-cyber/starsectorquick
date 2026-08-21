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


def main() -> None:
    text = LWJGL.read_text(encoding="utf-8")
    if MARKER in text:
        print("LWJGL matrix-stack guard already present")
        return

    old_helpers = """var curMatrixStack = modelViewMatrixStack;
function getCurMatrixTop()
{
\treturn curMatrixStack[curMatrixStack.length - 1];
}
function setCurMatrixTop(m)
{
\tcurMatrixStack[curMatrixStack.length - 1] = m;
}
"""
    new_helpers = f"""var curMatrixStack = modelViewMatrixStack;
// {MARKER}: OpenGL matrix stacks always retain their base identity matrix.
var matrixStackWarnings = new Set();
function ensureCurMatrixStack()
{{
\tif(!Array.isArray(curMatrixStack))
\t\tthrow new Error(\"LWJGL current matrix stack is invalid\");
\tif(curMatrixStack.length === 0)
\t{{
\t\twarnOnce(
\t\t\tmatrixStackWarnings,
\t\t\t\"matrix-stack-empty-recovery\",
\t\t\t\"LWJGL recovered an empty matrix stack with an identity matrix.\"
\t\t);
\t\tcurMatrixStack.push(glMatrix.mat4.create());
\t}}
\treturn curMatrixStack;
}}
function getCurMatrixTop()
{{
\tvar stack = ensureCurMatrixStack();
\treturn stack[stack.length - 1];
}}
function setCurMatrixTop(m)
{{
\tvar stack = ensureCurMatrixStack();
\tstack[stack.length - 1] = m;
}}
"""
    text = replace_once(text, old_helpers, new_helpers, "matrix helper block")

    old_push_pop = """function Java_org_lwjgl_opengl_GL11_nglPushMatrix(lib, funcPtr)
{
\tif(curList)
\t\treturn pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPushMatrix);
\tcurMatrixStack.push(glMatrix.mat4.clone(curMatrixStack[curMatrixStack.length - 1]));
}

function Java_org_lwjgl_opengl_GL11_nglPopMatrix(lib, funcPtr)
{
\tif(curList)
\t\treturn pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPopMatrix);
\tcurMatrixStack.pop();
}
"""
    new_push_pop = """function Java_org_lwjgl_opengl_GL11_nglPushMatrix(lib, funcPtr)
{
\tif(curList)
\t\treturn pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPushMatrix);
\tvar stack = ensureCurMatrixStack();
\tstack.push(glMatrix.mat4.clone(stack[stack.length - 1]));
}

function Java_org_lwjgl_opengl_GL11_nglPopMatrix(lib, funcPtr)
{
\tif(curList)
\t\treturn pushInList(curList, arguments, Java_org_lwjgl_opengl_GL11_nglPopMatrix);
\tvar stack = ensureCurMatrixStack();
\tif(stack.length <= 1)
\t{
\t\twarnOnce(
\t\t\tmatrixStackWarnings,
\t\t\t\"matrix-stack-underflow\",
\t\t\t\"LWJGL ignored glPopMatrix at the base matrix to prevent stack underflow.\"
\t\t);
\t\treturn;
\t}
\tstack.pop();
}
"""
    # Diagnostic branches may prepend a conservative immediate-batching barrier
    # counter to Push/Pop. Preserve that instrumentation through this guard instead
    # of making the exact source anchor fail.
    barrier_line = "\timmediateProfileBarrierGeneration++;\n"
    matrix_barrier_line = "\timmediateProfileMatrixBarrierGeneration++;\n"
    old_push_pop_profile = old_push_pop.replace("{\n\tif(curList)", "{\n" + barrier_line + "\tif(curList)")
    new_push_pop_profile = new_push_pop.replace("{\n\tif(curList)", "{\n" + barrier_line + "\tif(curList)")
    old_push_pop_profile_matrix = old_push_pop.replace(
        "{\n\tif(curList)",
        "{\n" + barrier_line + matrix_barrier_line + "\tif(curList)",
    )
    new_push_pop_profile_matrix = new_push_pop.replace(
        "{\n\tif(curList)",
        "{\n" + barrier_line + matrix_barrier_line + "\tif(curList)",
    )
    if text.count(old_push_pop) == 1:
        text = text.replace(old_push_pop, new_push_pop, 1)
    elif text.count(old_push_pop_profile) == 1:
        text = text.replace(old_push_pop_profile, new_push_pop_profile, 1)
    elif text.count(old_push_pop_profile_matrix) == 1:
        text = text.replace(old_push_pop_profile_matrix, new_push_pop_profile_matrix, 1)
    else:
        raise RuntimeError(
            "push/pop block: expected exactly one stock or profiled match, "
            f"found stock={text.count(old_push_pop)} profiled={text.count(old_push_pop_profile)} "
            f"profiled_matrix={text.count(old_push_pop_profile_matrix)}"
        )

    LWJGL.write_text(text, encoding="utf-8")
    print("Applied LWJGL matrix-stack underflow guard")


if __name__ == "__main__":
    main()
