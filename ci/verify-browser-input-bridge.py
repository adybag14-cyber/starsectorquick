#!/usr/bin/env python3
"""Verify that the winning LWJGL bridge classes are wired to browser input."""

from __future__ import annotations

import sys
from pathlib import Path


def require(text: str, token: str, context: str) -> None:
    if token not in text:
        raise RuntimeError(f"{context} is missing {token!r}")


def reject(text: str, token: str, context: str) -> None:
    if token in text:
        raise RuntimeError(f"{context} still contains forbidden {token!r}")


def main() -> int:
    if len(sys.argv) != 5:
        raise SystemExit(
            "usage: verify-browser-input-bridge.py <display-javap> <keyboard-javap> "
            "<mouse-javap> <lwjgl.js>"
        )

    display = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
    keyboard = Path(sys.argv[2]).read_text(encoding="utf-8", errors="replace")
    mouse = Path(sys.argv[3]).read_text(encoding="utf-8", errors="replace")
    js = Path(sys.argv[4]).read_text(encoding="utf-8")

    # Display.update(true) must preserve desktop LWJGL's device-poll contract.
    require(display, "processMessages:()V", "Display.update")
    require(display, "org/lwjgl/input/Mouse.poll:()V", "Display device poll")
    require(display, "org/lwjgl/input/Mouse.updateCursor:()V", "Display mouse cursor poll")
    require(display, "org/lwjgl/input/Keyboard.poll:()V", "Display keyboard poll")

    # The class that wins on the runtime classpath must no longer be an
    # always-false stub; public queries/events need native browser state.
    for token in (
        "nIsKeyDown:(I)Z",
        "nNext:()Z",
        "nGetEventKey:()I",
        "nGetEventKeyState:()Z",
    ):
        require(keyboard, token, "Keyboard bridge")
    for token in (
        "nIsButtonDown:(I)Z",
        "nNext:()Z",
        "nGetX:()I",
        "nGetY:()I",
        "nGetDX:()I",
        "nGetDY:()I",
    ):
        require(mouse, token, "Mouse bridge")

    require(js, "LWJGL_DIRECT_INPUT_BRIDGE_V1", "JavaScript input bridge")
    require(js, "glCanvas.tabIndex = 0", "keyboard focus")
    require(js, 'glCanvas.addEventListener("wheel"', "mouse wheel")
    require(js, 'window.addEventListener("keydown", keyHandler, true)', "keyboard capture")
    require(js, "shouldCaptureGameKeyboard", "desktop-style keyboard ownership")
    require(js, "keyboardGlobalCaptures", "unfocused game keyboard telemetry")
    require(js, "directKeyboardEnqueued", "direct keyboard enqueue telemetry")
    require(js, "directKeyboardDropped", "direct keyboard drop telemetry")
    require(js, "keyboardQueueDepth", "direct keyboard queue-depth telemetry")
    require(js, "keyboardDownCount", "direct keyboard release-state telemetry")
    require(js, "directKeyboardLatencyAvgMs", "direct keyboard latency telemetry")
    require(js, "directMouseLatencyAvgMs", "direct mouse latency telemetry")
    reject(js, "if(document.activeElement !== glCanvas && document.pointerLockElement !== glCanvas) return;", "focus-only keyboard capture")
    require(js, "lwjglKeyByCode", "LWJGL key mapping")
    require(js, "keyboardInputState.down[lwjglKey]", "keyboard state updates")
    require(js, "mouseInputState.buttons[directButton]", "mouse button state updates")
    require(js, "eventQueue[eventQueue.length - 1]", "X11 motion coalescing")
    reject(js, "eventQueue[0]?.type == evt.type", "legacy motion coalescing")

    # Direct JNI exports used by the bridge classes.
    for token in (
        "Java_org_lwjgl_input_Keyboard_nIsKeyDown,",
        "Java_org_lwjgl_input_Keyboard_nNext,",
        "Java_org_lwjgl_input_Mouse_nIsButtonDown,",
        "Java_org_lwjgl_input_Mouse_nNext,",
        "Java_org_lwjgl_input_Mouse_nGetX,",
        "Java_org_lwjgl_input_Mouse_nGetY,",
    ):
        require(js, token, "input JNI exports")

    # Keep the legacy Linux/X11 path valid too. Direct-buffer access must be
    # relative to the buffer address, never absolute offset zero.
    require(js, "bufferAddr + off", "X11 event buffer writes")
    require(js, "return Number(await buffer.address());", "X11 key event address")
    require(js, "Number(eventPtr) + 4", "X11 keysym lookup")
    require(js, "Number(eventPtr) + 20", "X11 character lookup")

    print("Verified live DOM-backed LWJGL keyboard/mouse bridge and X11 fallback.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
