package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;
import java.nio.ByteBuffer;

public abstract class PeerInfo {
    private final ByteBuffer handle;
    private Thread lockingThread;
    private int lockCount;

    protected PeerInfo() {
        this(ByteBuffer.allocateDirect(8));
    }

    protected PeerInfo(ByteBuffer handle) {
        this.handle = handle == null ? ByteBuffer.allocateDirect(8) : handle;
    }

    private void lockAndInitHandle() throws LWJGLException {
        doLockAndInitHandle();
    }

    public final synchronized void unlock() throws LWJGLException {
        if (lockCount > 0) {
            lockCount--;
            if (lockCount == 0) {
                try {
                    doUnlock();
                } finally {
                    lockingThread = null;
                }
            }
        }
    }

    protected abstract void doLockAndInitHandle() throws LWJGLException;
    protected abstract void doUnlock() throws LWJGLException;

    public final synchronized ByteBuffer lockAndGetHandle() throws LWJGLException {
        if (lockCount == 0) {
            lockAndInitHandle();
            lockingThread = Thread.currentThread();
        }
        lockCount++;
        return handle;
    }

    protected final ByteBuffer getHandle() {
        return handle;
    }

    public void destroy() {}
}
