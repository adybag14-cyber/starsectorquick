package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;

final class LinuxContextImplementation implements ContextImplementation {
    static {
        try {
            System.loadLibrary("lwjgl");
        } catch (Throwable ignored) {}
    }

    static native void nMakeCurrent();
    static native boolean nIsCurrent();
    static native void nSetSwapInterval(int value);
    static native void nSwapBuffers();

    public ByteBuffer create(PeerInfo peer_info, IntBuffer attribs, ByteBuffer shared_context_handle) throws LWJGLException {
        return ByteBuffer.allocateDirect(8);
    }

    public void swapBuffers() throws LWJGLException {
        nSwapBuffers();
    }

    public void releaseDrawable(ByteBuffer context_handle) throws LWJGLException {}

    public void releaseCurrentContext() throws LWJGLException {}

    public void update(ByteBuffer context_handle) {}

    public void makeCurrent(PeerInfo peer_info, ByteBuffer handle) throws LWJGLException {
        nMakeCurrent();
    }

    public boolean isCurrent(ByteBuffer handle) throws LWJGLException {
        return nIsCurrent();
    }

    public void setSwapInterval(int value) {
        nSetSwapInterval(value);
    }

    public void destroy(PeerInfo peer_info, ByteBuffer handle) throws LWJGLException {}
}
