package org.lwjgl.opengl;

import java.awt.Canvas;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;

interface DisplayImplementation extends InputImplementation {
    void createWindow(DrawableLWJGL drawable, DisplayMode mode, Canvas parent, int x, int y) throws LWJGLException;
    void destroyWindow();
    void switchDisplayMode(DisplayMode mode) throws LWJGLException;
    void resetDisplayMode();
    int getGammaRampLength();
    void setGammaRamp(FloatBuffer gammaRamp) throws LWJGLException;
    String getAdapter();
    String getVersion();
    DisplayMode init() throws LWJGLException;
    void setTitle(String title);
    boolean isCloseRequested();
    boolean isVisible();
    boolean isActive();
    boolean isDirty();
    PeerInfo createPeerInfo(PixelFormat pixelFormat, ContextAttribs attribs) throws LWJGLException;
    void update();
    void reshape(int x, int y, int width, int height);
    DisplayMode[] getAvailableDisplayModes() throws LWJGLException;
    int getPbufferCapabilities();
    boolean isBufferLost(PeerInfo handle);
    PeerInfo createPbuffer(int width, int height, PixelFormat pixelFormat, ContextAttribs attribs, IntBuffer pixelFormatCaps, IntBuffer pbufferAttribs) throws LWJGLException;
    void setPbufferAttrib(PeerInfo handle, int attrib, int value);
    void bindTexImageToPbuffer(PeerInfo handle, int buffer);
    void releaseTexImageFromPbuffer(PeerInfo handle, int buffer);
    int setIcon(ByteBuffer[] icons);
    void setResizable(boolean resizable);
    boolean wasResized();
    int getWidth();
    int getHeight();
    int getX();
    int getY();
    float getPixelScaleFactor();
}
