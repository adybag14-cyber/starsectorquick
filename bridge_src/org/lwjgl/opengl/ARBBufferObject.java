package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.MemoryUtil;

/** Minimal LWJGL 2.9.3 ARB buffer facade used by Starsector's graphics batcher. */
public class ARBBufferObject {
    public static final int GL_STREAM_DRAW_ARB = 35040;
    public static final int GL_STREAM_READ_ARB = 35041;
    public static final int GL_STREAM_COPY_ARB = 35042;
    public static final int GL_STATIC_DRAW_ARB = 35044;
    public static final int GL_STATIC_READ_ARB = 35045;
    public static final int GL_STATIC_COPY_ARB = 35046;
    public static final int GL_DYNAMIC_DRAW_ARB = 35048;
    public static final int GL_DYNAMIC_READ_ARB = 35049;
    public static final int GL_DYNAMIC_COPY_ARB = 35050;
    public static final int GL_READ_ONLY_ARB = 35000;
    public static final int GL_WRITE_ONLY_ARB = 35001;
    public static final int GL_READ_WRITE_ARB = 35002;
    public static final int GL_BUFFER_SIZE_ARB = 34660;
    public static final int GL_BUFFER_USAGE_ARB = 34661;
    public static final int GL_BUFFER_ACCESS_ARB = 35003;
    public static final int GL_BUFFER_MAPPED_ARB = 35004;
    public static final int GL_BUFFER_MAP_POINTER_ARB = 35005;

    public ARBBufferObject() {}

    private static IntBuffer oneInt(int value) {
        IntBuffer out = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        out.put(0, value);
        return out;
    }

    public static void glBindBufferARB(int target, int buffer) {
        nglBindBufferARB(target, buffer, 0L);
    }
    static native void nglBindBufferARB(int target, int buffer, long functionPointer);

    public static void glDeleteBuffersARB(IntBuffer buffers) {
        if (buffers == null || !buffers.hasRemaining()) return;
        nglDeleteBuffersARB(buffers.remaining(), MemoryUtil.getAddress(buffers, buffers.position()), 0L);
    }
    static native void nglDeleteBuffersARB(int count, long buffers, long functionPointer);

    public static void glDeleteBuffersARB(int buffer) {
        IntBuffer tmp = oneInt(buffer);
        nglDeleteBuffersARB(1, MemoryUtil.getAddress(tmp, 0), 0L);
    }

    public static void glGenBuffersARB(IntBuffer buffers) {
        if (buffers == null || !buffers.hasRemaining()) return;
        nglGenBuffersARB(buffers.remaining(), MemoryUtil.getAddress(buffers, buffers.position()), 0L);
    }
    static native void nglGenBuffersARB(int count, long buffers, long functionPointer);

    public static int glGenBuffersARB() {
        IntBuffer tmp = oneInt(0);
        nglGenBuffersARB(1, MemoryUtil.getAddress(tmp, 0), 0L);
        return tmp.get(0);
    }

    public static void glBufferDataARB(int target, long size, int usage) {
        nglBufferDataARB(target, size, 0L, usage, 0L);
    }
    static native void nglBufferDataARB(int target, long size, long data, int usage, long functionPointer);

    public static void glBufferSubDataARB(int target, long offset, FloatBuffer data) {
        if (data == null || !data.hasRemaining()) return;
        long bytes = ((long) data.remaining()) << 2;
        nglBufferSubDataARB(target, offset, bytes, MemoryUtil.getAddress(data, data.position()), 0L);
    }

    public static void glBufferSubDataARB(int target, long offset, ByteBuffer data) {
        if (data == null || !data.hasRemaining()) return;
        nglBufferSubDataARB(target, offset, data.remaining(), MemoryUtil.getAddress(data, data.position()), 0L);
    }
    static native void nglBufferSubDataARB(int target, long offset, long size, long data, long functionPointer);
}
