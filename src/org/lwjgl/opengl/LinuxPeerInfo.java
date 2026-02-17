package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import org.lwjgl.LWJGLException;

abstract class LinuxPeerInfo extends PeerInfo {
    LinuxPeerInfo() {
        super(createHandle());
    }
    private static ByteBuffer createHandle() { return ByteBuffer.allocateDirect(8); }
    public long getDrawable() { return 0; }
}
