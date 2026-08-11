#!/usr/bin/env python3
"""Verify the fixed-function WebGL compatibility state bridge."""

from __future__ import annotations

import sys
from pathlib import Path


def function_block(text: str, name: str) -> str:
    signature = f"function {name}("
    start = text.find(signature)
    if start < 0:
        raise RuntimeError(f"missing function: {name}")
    body_start = text.find("{", start)
    if body_start < 0:
        raise RuntimeError(f"missing body for function: {name}")

    depth = 0
    for index in range(body_start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[start : index + 1]
    raise RuntimeError(f"unterminated function: {name}")


def require(block: str, token: str, context: str) -> None:
    if token not in block:
        raise RuntimeError(f"{context} is missing {token!r}\n{block}")


def reject(block: str, token: str, context: str) -> None:
    if token in block:
        raise RuntimeError(f"{context} still contains forbidden {token!r}\n{block}")


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify-lwjgl-fixed-function.py <lwjgl.js>")

    text = Path(sys.argv[1]).read_text(encoding="utf-8")
    require(text, "LWJGL_ATTRIB_STACK_COMPAT_V1", "module")
    require(text, "LWJGL_INTEGER_PIXEL_STORE_COMPAT_V1", "module")

    enable = function_block(text, "Java_org_lwjgl_opengl_GL11_nglEnable")
    disable = function_block(text, "Java_org_lwjgl_opengl_GL11_nglDisable")
    is_enabled = function_block(text, "Java_org_lwjgl_opengl_GL11_nglIsEnabled")
    push = function_block(text, "Java_org_lwjgl_opengl_GL11_nglPushAttrib")
    pop = function_block(text, "Java_org_lwjgl_opengl_GL11_nglPopAttrib")
    snapshot = function_block(text, "snapshotAttribState")
    restore = function_block(text, "restoreAttribState")
    set_compat = function_block(text, "setCompatEnableState")
    get_integer = function_block(text, "Java_org_lwjgl_opengl_GL11_nglGetIntegerv")
    pixel_store = function_block(text, "Java_org_lwjgl_opengl_GL11_nglPixelStorei")

    require(enable, "setTexture2DEnabled(true);", "glEnable")
    reject(enable, "uniform1f(texMaskLocation", "glEnable")
    require(disable, "setTexture2DEnabled(false);", "glDisable")
    require(is_enabled, "return getCompatEnableState(cap);", "glIsEnabled")
    require(push, "attribStateStack.push(snapshotAttribState(mask));", "glPushAttrib")
    require(pop, "restoreAttribState(attribStateStack.pop());", "glPopAttrib")
    require(set_compat, "setTexture2DEnabled(enabled);", "compat enable restoration")
    require(get_integer, "glCtx.getParameter(id)", "glGetIntegerv")
    require(get_integer, "buf[0] = value | 0;", "glGetIntegerv scalar state")
    require(pixel_store, "glCtx.pixelStorei(pname, param);", "glPixelStorei")

    for mask in (
        "0x2000/*GL_ENABLE_BIT*/",
        "0x4000/*GL_COLOR_BUFFER_BIT*/",
        "0x0100/*GL_DEPTH_BUFFER_BIT*/",
        "0x0800/*GL_VIEWPORT_BIT*/",
    ):
        require(snapshot, mask, "attribute snapshot")

    for token in (
        "glCtx.blendFuncSeparate",
        "glCtx.colorMask",
        "glCtx.depthMask",
        "glCtx.depthFunc",
        "glCtx.viewport",
        "glCtx.depthRange",
    ):
        require(restore, token, "attribute restoration")

    for export in (
        "Java_org_lwjgl_opengl_GL11_nglIsEnabled,",
        "Java_org_lwjgl_opengl_GL11_nglPushAttrib,",
        "Java_org_lwjgl_opengl_GL11_nglPopAttrib,",
        "Java_org_lwjgl_opengl_GL11_nglPixelStorei,",
    ):
        require(text, export, "module exports")

    print("Verified LWJGL fixed-function state, integer queries, and pixel-store compatibility.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
