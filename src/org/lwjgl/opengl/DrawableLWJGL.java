package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

interface DrawableLWJGL extends Drawable {
    void setPixelFormat(PixelFormatLWJGL pixelFormat) throws LWJGLException;
    void setPixelFormat(PixelFormatLWJGL pixelFormat, ContextAttribs attribs) throws LWJGLException;
    PixelFormatLWJGL getPixelFormat();
    Context getContext();
    Context createSharedContext() throws LWJGLException;
    void checkGLError();
    void setSwapInterval(int value);
    void swapBuffers() throws LWJGLException;
    void initContext(float r, float g, float b);
}
