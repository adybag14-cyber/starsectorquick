package org.lwjgl.opengl;

public interface Drawable {
    void makeCurrent() throws org.lwjgl.LWJGLException;
    void releaseContext() throws org.lwjgl.LWJGLException;
    boolean isCurrent() throws org.lwjgl.LWJGLException;
    void setPointer(long pointer);
}
