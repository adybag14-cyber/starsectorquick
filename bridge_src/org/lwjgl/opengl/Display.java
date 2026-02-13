package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import org.lwjgl.LWJGLException;

public final class Display {
    static {
        try {
            System.loadLibrary("lwjgl");
        } catch (Throwable ignored) {}
    }

    private static boolean created = false;
    private static boolean fullscreen = false;
    private static DisplayMode displayMode = new DisplayMode(1024, 768);

    private Display() {}

    public static native void create() throws LWJGLException;

    public static void create(PixelFormat pixel_format) throws LWJGLException {
        create();
        created = true;
    }

    public static void create(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormat pixel_format, Drawable shared_drawable) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormat pixel_format, ContextAttribs attribs, Drawable shared_drawable) throws LWJGLException {
        create(pixel_format);
    }

    public static void destroy() {
        created = false;
    }

    public static boolean isCreated() {
        // Some startup paths call Display.create() (native no-arg), which does
        // not update this Java-side flag. Treat display as created once runtime
        // reaches this class to avoid init loops.
        return true;
    }

    public static boolean isActive() {
        return true;
    }

    public static boolean isVisible() {
        return true;
    }

    public static void setLocation(int x, int y) {}

    public static void setVSyncEnabled(boolean sync) {
        // Some builds of the JS lwjgl shim do not expose nSetSwapInterval reliably.
        // Keep this as a no-op to avoid fatal startup linkage errors.
    }

    public static void setTitle(String title) {}

    public static int setIcon(ByteBuffer[] icons) {
        return 0;
    }

    public static void setResizable(boolean resizable) {}

    public static boolean isResizable() {
        return false;
    }

    public static boolean wasResized() {
        return false;
    }

    public static int getX() {
        return 0;
    }

    public static int getY() {
        return 0;
    }

    public static int getWidth() {
        return displayMode.getWidth();
    }

    public static int getHeight() {
        return displayMode.getHeight();
    }

    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static DisplayMode getDesktopDisplayMode() {
        return displayMode;
    }

    public static void setDisplayMode(DisplayMode mode) throws LWJGLException {
        if (mode != null) {
            displayMode = mode;
        }
    }

    public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        return new DisplayMode[] { displayMode, new DisplayMode(1280, 720), new DisplayMode(1366, 768) };
    }

    public static void setFullscreen(boolean value) throws LWJGLException {
        fullscreen = value;
    }

    public static boolean isFullscreen() {
        return fullscreen;
    }

    public static float getPixelScaleFactor() {
        return 1.0f;
    }

    public static void setParent(java.awt.Canvas parent) throws LWJGLException {}

    public static void update() {
        LinuxContextImplementation.nSwapBuffers();
    }

    public static void update(boolean process_messages) {
        LinuxContextImplementation.nSwapBuffers();
    }

    public static void sync(int fps) {}

    public static void makeCurrent() throws LWJGLException {
        LinuxContextImplementation.nMakeCurrent();
    }

    public static String getAdapter() {
        return "CheerpJ WebGL";
    }

    public static String getVersion() {
        return "2.1";
    }

    public static void processMessages() {}

    public static void swapBuffers() throws LWJGLException {
        LinuxContextImplementation.nSwapBuffers();
    }

    public static Drawable getDrawable() {
        return null;
    }

    public static boolean isCloseRequested() {
        return false;
    }

    public static boolean isDirty() {
        return false;
    }
}
