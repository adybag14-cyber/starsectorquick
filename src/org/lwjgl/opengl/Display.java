package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import org.lwjgl.LWJGLException;

public final class Display {
    public static void create() throws LWJGLException { System.out.println("Display.create()"); }
    public static void create(PixelFormat pixel_format) throws LWJGLException { System.out.println("Display.create(PF)"); }
    public static void create(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException { System.out.println("Display.create(PF, Attr)"); }
    public static void create(PixelFormat pixel_format, Drawable shared_drawable) throws LWJGLException { System.out.println("Display.create(PF, Drawable)"); }
    public static void create(PixelFormat pixel_format, ContextAttribs attribs, Drawable shared_drawable) throws LWJGLException { System.out.println("Display.create(PF, Attr, Drawable)"); }
    public static void destroy() {}
    public static boolean isCreated() { return true; }
    public static boolean isActive() { return true; }
    public static boolean isVisible() { return true; }
    public static void setLocation(int x, int y) {}
    public static void setVSyncEnabled(boolean sync) { System.out.println("Display.setVSyncEnabled(" + sync + ")"); }
    public static void setTitle(String title) {}
    public static int setIcon(ByteBuffer[] icons) { return 0; }
    public static void setResizable(boolean resizable) {}
    public static boolean isResizable() { return false; }
    public static boolean wasResized() { return false; }
    public static int getX() { return 0; }
    public static int getY() { return 0; }
    public static int getWidth() { return 1024; }
    public static int getHeight() { return 768; }
    public static DisplayMode getDisplayMode() { 
        System.out.println("Display.getDisplayMode");
        return new DisplayMode(1024, 768); 
    }
    public static void setDisplayMode(DisplayMode mode) throws LWJGLException { System.out.println("Display.setDisplayMode"); }
    public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException { return new DisplayMode[]{new DisplayMode(1024, 768)}; }
    public static void setFullscreen(boolean fullscreen) throws LWJGLException {}
    public static boolean isFullscreen() { return false; }
    public static void setParent(java.awt.Canvas parent) throws LWJGLException {}
    public static void update() {}
    public static void update(boolean process_messages) {}
    public static void sync(int fps) {}
    public static String getAdapter() { return "CheerpJ"; }
    public static String getVersion() { return "1.0"; }
    public static void processMessages() {}
    public static void swapBuffers() throws LWJGLException {}
    public static Drawable getDrawable() { return null; }
    public static boolean isCloseRequested() { return false; }
    public static boolean isDirty() { return false; }
}
