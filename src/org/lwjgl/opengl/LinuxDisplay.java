package org.lwjgl.opengl;

import java.awt.Canvas;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PeerInfo;

final class LinuxDisplay implements DisplayImplementation {
    public void createWindow(DrawableLWJGL drawable, DisplayMode mode, Canvas parent, int x, int y) throws LWJGLException {
        System.out.println("LinuxDisplay.createWindow");
    }
    public void destroyWindow() {
        System.out.println("LinuxDisplay.destroyWindow");
    }
    public void switchDisplayMode(DisplayMode mode) {}
    public void resetDisplayMode() {}
    public int getGammaRampLength() { return 0; }
    public void setGammaRamp(FloatBuffer gammaRamp) {}
    public String getAdapter() { return "CheerpJ Adapter"; }
    public String getVersion() { return "1.0"; }
    public DisplayMode init() throws LWJGLException {
        System.out.println("LinuxDisplay.init");
        return new DisplayMode(1024, 768);
    }
    public void setTitle(String title) { System.out.println("LinuxDisplay.setTitle: " + title); }
    public boolean isCloseRequested() { return false; }
    public boolean isVisible() { return true; }
    public boolean isActive() { return true; }
    public boolean isDirty() { return false; }
    public PeerInfo createPeerInfo(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
        System.out.println("LinuxDisplay.createPeerInfo");
        return new LinuxDisplayPeerInfo(pixel_format);
    }
    public void update() { System.out.println("LinuxDisplay.update"); }
    public void reshape(int x, int y, int width, int height) {}
    public DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        return new DisplayMode[] { new DisplayMode(1024, 768) };
    }
    public boolean hasWheel() { return true; }
    public int getButtonCount() { return 3; }
    public void createMouse() { System.out.println("LinuxDisplay.createMouse"); }
    public void destroyMouse() {}
    public void pollMouse(IntBuffer coord_buffer, ByteBuffer buttons) {}
    public void readMouse(ByteBuffer buffer) {}
    public void setCursorPosition(int x, int y) {}
    public void grabMouse(boolean new_grab) {}
    public int getNativeCursorCapabilities() { return 0; }
    public void setNativeCursor(Object handle) {}
    public int getMinCursorSize() { return 16; }
    public int getMaxCursorSize() { return 64; }
    public void createKeyboard() { System.out.println("LinuxDisplay.createKeyboard"); }
    public void destroyKeyboard() {}
    public void pollKeyboard(ByteBuffer keyDownBuffer) {}
    public void readKeyboard(ByteBuffer buffer) {}
    public Object createCursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) { return null; }
    public void destroyCursor(Object cursorHandle) {}
    public int getPbufferCapabilities() { return 0; }
    public boolean isBufferLost(PeerInfo handle) { return false; }
    public PeerInfo createPbuffer(int width, int height, PixelFormat pixel_format, ContextAttribs attribs, IntBuffer pixelFormatCaps, IntBuffer pBufferAttribs) throws LWJGLException {
        return new LinuxPbufferPeerInfo(width, height, pixel_format);
    }
    public void setPbufferAttrib(PeerInfo handle, int attrib, int value) {}
    public void bindTexImageToPbuffer(PeerInfo handle, int buffer) {}
    public void releaseTexImageFromPbuffer(PeerInfo handle, int buffer) {}
    public int setIcon(ByteBuffer[] icons) { return 0; }
    public int getX() { return 0; }
    public int getY() { return 0; }
    public int getWidth() { return 1024; }
    public int getHeight() { return 768; }
    public boolean isInsideWindow() { return true; }
    public void setResizable(boolean resizable) {}
    public boolean wasResized() { return false; }
    public float getPixelScaleFactor() { return 1.0f; }
}