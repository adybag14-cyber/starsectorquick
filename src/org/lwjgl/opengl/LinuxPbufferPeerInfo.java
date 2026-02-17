package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

final class LinuxPbufferPeerInfo extends LinuxPeerInfo {
    LinuxPbufferPeerInfo(int width, int height, PixelFormat pixel_format) throws LWJGLException {
        super();
    }
    protected void doLockAndInitHandle() throws LWJGLException {}
    protected void doUnlock() throws LWJGLException {}
    public void destroy() {}
}
