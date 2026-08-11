#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path


def method_block(text: str, signature: str) -> str:
    start = text.find(signature)
    if start < 0:
        raise RuntimeError(f"missing javap method: {signature}")
    next_method = text.find("\n  public static ", start + len(signature))
    next_native = text.find("\n  static native ", start + len(signature))
    ends = [x for x in (next_method, next_native) if x >= 0]
    end = min(ends) if ends else len(text)
    return text[start:end]


def require_constant(text: str, signature: str, value: int) -> None:
    block = method_block(text, signature)
    token = f"sipush        {value}"
    if token not in block:
        raise RuntimeError(f"{signature} does not load expected GL enum {value}\n{block}")
    if "ngl" not in block or "(IIIJJ)V" not in block:
        raise RuntimeError(f"{signature} does not call the expected native client-array bridge\n{block}")


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify-bridge-client-arrays.py <javap-output>")
    text = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
    require_constant(text, "public static void glColorPointer(int, int, java.nio.FloatBuffer);", 5126)
    require_constant(text, "public static void glTexCoordPointer(int, int, java.nio.FloatBuffer);", 5126)
    require_constant(text, "public static void glVertexPointer(int, int, java.nio.FloatBuffer);", 5126)
    require_constant(text, "public static void glVertexPointer(int, int, java.nio.IntBuffer);", 5124)
    print("Verified LWJGL client-array bridge GL type/stride wiring.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
