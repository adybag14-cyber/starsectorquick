package org.lwjgl.opengl;

public final class GL1D {
    private GL1D() {}

    public static void glBlendEquation(int mode) {
        // Graceful no-op when blend equation JNI is not available in the wasm bridge.
    }
}
