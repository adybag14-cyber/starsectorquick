package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;
import java.nio.ByteBuffer;

final class LinuxDisplayPeerInfo extends LinuxPeerInfo {
    final boolean egl = false;
    LinuxDisplayPeerInfo() throws LWJGLException {
        super();
    }
    LinuxDisplayPeerInfo(PixelFormat pixel_format) throws LWJGLException {
        super();
    }
    protected void doLockAndInitHandle() throws LWJGLException {}
    protected void doUnlock() throws LWJGLException {}
    public void destroy() {}
}
