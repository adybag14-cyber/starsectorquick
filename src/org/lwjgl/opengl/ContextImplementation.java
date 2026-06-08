package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;

interface ContextImplementation {
    ByteBuffer create(PeerInfo peerInfo, IntBuffer attribs, ByteBuffer sharedContextHandle) throws LWJGLException;
    void swapBuffers() throws LWJGLException;
    void releaseDrawable(ByteBuffer contextHandle) throws LWJGLException;
    void releaseCurrentContext() throws LWJGLException;
    void update(ByteBuffer contextHandle);
    void makeCurrent(PeerInfo peerInfo, ByteBuffer handle) throws LWJGLException;
    boolean isCurrent(ByteBuffer handle) throws LWJGLException;
    void setSwapInterval(int value);
    void destroy(PeerInfo peerInfo, ByteBuffer handle) throws LWJGLException;
}
