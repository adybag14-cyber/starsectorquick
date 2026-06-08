package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;

public interface InputImplementation {
    boolean hasWheel();
    int getButtonCount();
    void createMouse() throws LWJGLException;
    void destroyMouse();
    void pollMouse(IntBuffer coordBuffer, ByteBuffer buttons);
    void readMouse(ByteBuffer buffer);
    void grabMouse(boolean grab);
    int getNativeCursorCapabilities();
    void setCursorPosition(int x, int y);
    void setNativeCursor(Object handle) throws LWJGLException;
    int getMinCursorSize();
    int getMaxCursorSize();
    void createKeyboard() throws LWJGLException;
    void destroyKeyboard();
    void pollKeyboard(ByteBuffer keyDownBuffer);
    void readKeyboard(ByteBuffer buffer);
    Object createCursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) throws LWJGLException;
    void destroyCursor(Object cursorHandle);
    int getWidth();
    int getHeight();
    boolean isInsideWindow();
}
