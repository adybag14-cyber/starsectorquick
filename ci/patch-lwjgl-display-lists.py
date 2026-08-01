#!/usr/bin/env python3
"""Keep legacy OpenGL display-list compilation from terminating CheerpJ.

Some OpenGL calls are deliberately executed immediately rather than stored in a
display list. The old bridge treated any such call while a list was open as a
fatal JavaScript exception. Real OpenGL executes many non-listable operations
immediately, and harmless fixed-function calls such as glNormal3f are irrelevant
to this bridge's unlit shader. Warn once and continue instead of stopping the
entire Java VM.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LWJGL = ROOT / "build" / "final" / "wasm-modules" / "lwjgl.js"
MARKER = "LWJGL_DISPLAY_LIST_NONFATAL_V1"


def main() -> None:
    text = LWJGL.read_text(encoding="utf-8")
    if MARKER in text:
        print("LWJGL display-list compatibility guard already present")
        return

    old = """function checkNoList(list)
{
\tif(list != null)
\t\tthrow new Error(\"Unsupported command in list\");
}
"""
    new = f"""// {MARKER}: non-listable legacy calls execute immediately, as in desktop GL.
var displayListCompatibilityWarnings = new Set();
function checkNoList(list)
{{
\tif(list != null)
\t{{
\t\twarnOnce(
\t\t\tdisplayListCompatibilityWarnings,
\t\t\t\"immediate-command-during-list\",
\t\t\t\"LWJGL executed a non-recorded legacy OpenGL command immediately while compiling a display list.\"
\t\t);
\t}}
}}
"""
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"display-list guard: expected exactly one match, found {count}")
    LWJGL.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("Applied nonfatal LWJGL display-list compatibility guard")


if __name__ == "__main__":
    main()
