package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;
import java.nio.ByteBuffer;

public abstract class PeerInfo {
    protected PeerInfo() {}
    protected PeerInfo(ByteBuffer handle) {}
    public void lock() throws LWJGLException {}
    public void unlock() throws LWJGLException {}
    public void destroy() {}
}