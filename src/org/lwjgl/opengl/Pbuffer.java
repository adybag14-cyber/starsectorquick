package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;

public final class Pbuffer implements Drawable {
    public Pbuffer(int width, int height, PixelFormat pixelFormat, Drawable sharedDrawable) throws LWJGLException {}

    public Pbuffer(int width, int height, PixelFormat pixelFormat, RenderTexture renderTexture, Drawable sharedDrawable) throws LWJGLException {}

    public Pbuffer(int width, int height, PixelFormat pixelFormat, RenderTexture renderTexture, Drawable sharedDrawable, ContextAttribs attribs) throws LWJGLException {}

    public static int getCapabilities() {
        return 0;
    }

    public boolean isBufferLost() {
        return false;
    }

    public void setAttrib(int attrib, int value) {}

    public void bindTexImage(int buffer) {}

    public void releaseTexImage(int buffer) {}

    @Override
    public boolean isCurrent() throws LWJGLException {
        return true;
    }

    @Override
    public void makeCurrent() throws LWJGLException {}

    @Override
    public void releaseContext() throws LWJGLException {}

    @Override
    public void destroy() {}

    @Override
    public void setCLSharingProperties(PointerBuffer properties) throws LWJGLException {}
}
