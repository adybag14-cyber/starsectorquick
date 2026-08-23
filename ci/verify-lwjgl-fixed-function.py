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
    require(text, "LWJGL_GENERATE_MIPMAP_COMPAT_V1", "module")
    require(text, "LWJGL_IMMEDIATE_VERTEX_BATCH_V1", "module")
    require(text, "WEBGL_QUAD_INDEX_BATCH_V2", "module")
    require(text, "LWJGL_RASTER_STATE_COMPAT_V1", "module")
    require(text, "LWJGL_POINT_SIZE_COMPAT_V1", "module")
    require(text, "LWJGL_COMPAT_LARGE_DRAW_DIAGNOSTICS_V1", "module")
    require(text, "preserveDrawingBuffer: false", "production WebGL context")
    require(text, 'powerPreference: "high-performance"', "production WebGL context")
    require(text, "DEPTH24_STENCIL8", "stencil-capable framebuffer")
    require(text, "DEPTH_STENCIL_ATTACHMENT", "stencil-capable framebuffer")

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
    tex_parameter = function_block(text, "Java_org_lwjgl_opengl_GL11_nglTexParameteri")
    tex_image = function_block(text, "Java_org_lwjgl_opengl_GL11_nglTexImage2D")
    tex_sub_image = function_block(text, "Java_org_lwjgl_opengl_GL11_nglTexSubImage2D")
    vertex_batch = function_block(text, "Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord")
    scissor = function_block(text, "Java_org_lwjgl_opengl_GL11_nglScissor")
    stencil_func = function_block(text, "Java_org_lwjgl_opengl_GL11_nglStencilFunc")
    stencil_op = function_block(text, "Java_org_lwjgl_opengl_GL11_nglStencilOp")
    point_size = function_block(text, "Java_org_lwjgl_opengl_GL11_nglPointSize")
    copy_tex_image = function_block(text, "Java_org_lwjgl_opengl_GL11_nglCopyTexImage2D")
    quad_indices = function_block(text, "ensureQuadIndexCapacity")
    draw_arrays = function_block(text, "drawArraysImpl")
    ensure_framebuffer = function_block(text, "ensureFramebufferSize")

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
    require(tex_parameter, "textureGenerateMipmap[boundTexture2DId] = !!param;", "GL_GENERATE_MIPMAP state")
    require(tex_parameter, "param = glCtx.CLAMP_TO_EDGE;", "GL_CLAMP translation")
    require(tex_image, "glCtx.generateMipmap(target);", "level-0 texture upload mipmaps")
    require(tex_image, "textureStorageUploadFormat[boundTexture2DId] = upload.format;", "texture storage format tracking")
    require(tex_sub_image, "glCtx.generateMipmap(target);", "level-0 texture sub-upload mipmaps")
    require(tex_sub_image, "var storageFormat = textureStorageUploadFormat[boundTexture2DId] || format;", "texture sub-upload storage compatibility")
    require(tex_sub_image, "normalizeTextureUpload(v, memPtr, width, height, storageFormat, format, type);", "texture sub-upload normalization")
    require(vertex_batch, "appendImmediateVertex", "combined immediate-mode vertex path")
    require(scissor, "glCtx.scissor", "scissor state")
    require(stencil_func, "glCtx.stencilFunc", "stencil comparison state")
    require(stencil_op, "glCtx.stencilOp", "stencil operations")
    require(point_size, "glCtx.uniform1f(pointSizeLocation", "point-size state")
    require(snapshot, "0x0400/*GL_STENCIL_BUFFER_BIT*/", "attribute snapshot stencil state")
    require(snapshot, "0x0001/*GL_CURRENT_BIT*/", "attribute snapshot current state")
    require(snapshot, "0x00040000/*GL_TEXTURE_BIT*/", "attribute snapshot texture state")
    require(restore, "glCtx.stencilFunc", "attribute restoration stencil state")
    require(restore, "boundTexture2DId = state.texture.boundTexture2DId;", "attribute restoration texture binding")
    require(restore, "immediateModeData.currentColor = state.current.color.slice();", "attribute restoration current color")
    require(ensure_framebuffer, "var restoreTexture2D = boundTexture2DId > 0 ? textureObjects[boundTexture2DId] : null;", "framebuffer resize texture preservation")
    require(ensure_framebuffer, "glCtx.bindTexture(glCtx.TEXTURE_2D, restoreTexture2D);", "framebuffer resize texture restoration")
    require(copy_tex_image, "glCtx.copyTexImage2D", "texture framebuffer copy")
    require(copy_tex_image, "glCtx.generateMipmap(target);", "level-0 texture framebuffer-copy mipmaps")
    require(quad_indices, "new Uint32Array(quadCount * 6)", "quad index cache")
    require(quad_indices, "indices[i + 5] = v + 3", "quad triangle expansion")
    require(quad_indices, "capacity *= 2", "geometric quad-index growth")
    require(quad_indices, "glCtx.ELEMENT_ARRAY_BUFFER", "quad index buffer upload")
    require(draw_arrays, "assert(first == 0);", "preserved client-array first contract")
    require(draw_arrays, "if(quadCount <= 1)", "single-quad fast path")
    require(draw_arrays, "glCtx.drawElements(glCtx.TRIANGLES, quadCount * 6, glCtx.UNSIGNED_INT, 0);", "batched GL_QUADS draw")
    require(draw_arrays, "presentationStats.quadDrawCallsSaved += quadCount - 1", "quad draw-call savings telemetry")
    reject(draw_arrays, "for(var i=0;i<count;i+=4)", "old per-quad WebGL draw loop")

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
        "Java_org_lwjgl_opengl_GL11_nglVertex3fTexCoord,",
        "Java_org_lwjgl_opengl_GL11_nglScissor,",
        "Java_org_lwjgl_opengl_GL11_nglStencilFunc,",
        "Java_org_lwjgl_opengl_GL11_nglStencilOp,",
        "Java_org_lwjgl_opengl_GL11_nglPointSize,",
        "Java_org_lwjgl_opengl_GL11_nglCopyTexImage2D,",
    ):
        require(text, export, "module exports")

    print("Verified LWJGL fixed-function state, integer queries, and pixel-store compatibility.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
