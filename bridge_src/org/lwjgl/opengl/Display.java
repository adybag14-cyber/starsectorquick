package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import org.lwjgl.LWJGLException;

public final class Display {
    private static boolean getDrawableLogged = false;
    private static boolean getImplementationLogged = false;
    private static boolean setLocationLogged = false;
    private static boolean setIconLogged = false;
    private static boolean setTitleLogged = false;
    private static boolean setFullscreenLogged = false;
    private static boolean setVsyncLogged = false;
    private static boolean createPixelFormatLogged = false;
    private static boolean getDisplayModeLogged = false;
    private static boolean getDesktopDisplayModeLogged = false;
    private static boolean setDisplayModeLogged = false;
    private static boolean getAvailableDisplayModesLogged = false;
    private static boolean getWidthLogged = false;
    private static boolean getHeightLogged = false;
    private static boolean getPixelScaleLogged = false;
    private static int updateCount = 0;
    private static int swapBuffersCount = 0;
    private static int syncCount = 0;
    private static boolean inputPumpFailureLogged = false;
    private static int syncTargetFps = 0;
    private static long nextFrameDeadlineNanos = 0L;
    private static long syncSleepCount = 0L;
    private static long syncLateCount = 0L;
    private static long syncResetCount = 0L;
    static {
        System.out.println("Bridge Display.<clinit>()");
    }

    private static boolean created = false;
    private static boolean fullscreen = false;
    private static DisplayMode displayMode = new DisplayMode(1024, 768);
    private static final DisplayImplementation implementation = new LinuxDisplay();
    private static java.awt.Canvas parent;
    private static String title = "Starsector";
    private static int swapInterval = 0;

    private Display() {}

    private static void maybeLogLoop(String kind, int count) {
        if (count <= 3 || count == 10 || count % 300 == 0) {
            System.out.println("Bridge Display." + kind + " count=" + count);
        }
    }

    public static void create() throws LWJGLException {
        created = true;
    }

    public static void create(PixelFormat pixel_format) throws LWJGLException {
        if (!createPixelFormatLogged) {
            createPixelFormatLogged = true;
            System.out.println("Bridge Display.create(PixelFormat)");
        }
        create();
    }

    public static void create(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormat pixel_format, Drawable shared_drawable) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormat pixel_format, Drawable shared_drawable, ContextAttribs attribs) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormat pixel_format, ContextAttribs attribs, Drawable shared_drawable) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormatLWJGL pixel_format) throws LWJGLException {
        create(pixel_format instanceof PixelFormat ? (PixelFormat) pixel_format : new PixelFormat());
    }

    public static void create(PixelFormatLWJGL pixel_format, Drawable shared_drawable) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormatLWJGL pixel_format, org.lwjgl.opengles.ContextAttribs attribs) throws LWJGLException {
        create(pixel_format);
    }

    public static void create(PixelFormatLWJGL pixel_format, Drawable shared_drawable, org.lwjgl.opengles.ContextAttribs attribs) throws LWJGLException {
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

    public static void setLocation(int x, int y) {
        if (!setLocationLogged) {
            setLocationLogged = true;
            System.out.println("Bridge Display.setLocation(" + x + ", " + y + ")");
        }
    }

    public static void setVSyncEnabled(boolean sync) {
        if (!setVsyncLogged) {
            setVsyncLogged = true;
            System.out.println("Bridge Display.setVSyncEnabled(" + sync + ")");
        }
        // Some builds of the JS lwjgl shim do not expose nSetSwapInterval reliably.
        // Keep this as a no-op to avoid fatal startup linkage errors.
        setSwapInterval(sync ? 1 : 0);
    }

    public static void setTitle(String value) {
        if (!setTitleLogged) {
            setTitleLogged = true;
            System.out.println("Bridge Display.setTitle(" + value + ")");
        }
        title = value == null ? "" : value;
    }

    public static int setIcon(ByteBuffer[] icons) {
        if (!setIconLogged) {
            setIconLogged = true;
            System.out.println(
                    "Bridge Display.setIcon(count=" + (icons == null ? 0 : icons.length) + ")");
        }
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
        if (!getWidthLogged) {
            getWidthLogged = true;
            System.out.println("Bridge Display.getWidth()");
        }
        return displayMode.getWidth();
    }

    public static int getHeight() {
        if (!getHeightLogged) {
            getHeightLogged = true;
            System.out.println("Bridge Display.getHeight()");
        }
        return displayMode.getHeight();
    }

    public static DisplayMode getDisplayMode() {
        if (!getDisplayModeLogged) {
            getDisplayModeLogged = true;
            System.out.println("Bridge Display.getDisplayMode()");
        }
        return displayMode;
    }

    public static DisplayMode getDesktopDisplayMode() {
        if (!getDesktopDisplayModeLogged) {
            getDesktopDisplayModeLogged = true;
            System.out.println("Bridge Display.getDesktopDisplayMode()");
        }
        return displayMode;
    }

    public static void setDisplayMode(DisplayMode mode) throws LWJGLException {
        if (!setDisplayModeLogged) {
            setDisplayModeLogged = true;
            System.out.println("Bridge Display.setDisplayMode(" + mode + ")");
        }
        if (mode != null) {
            displayMode = mode;
        }
    }

    public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        if (!getAvailableDisplayModesLogged) {
            getAvailableDisplayModesLogged = true;
            System.out.println("Bridge Display.getAvailableDisplayModes()");
        }
        return new DisplayMode[] { displayMode, new DisplayMode(1280, 720), new DisplayMode(1366, 768) };
    }

    public static void setFullscreen(boolean value) throws LWJGLException {
        if (!setFullscreenLogged) {
            setFullscreenLogged = true;
            System.out.println("Bridge Display.setFullscreen(" + value + ")");
        }
        fullscreen = value;
    }

    public static boolean isFullscreen() {
        return fullscreen;
    }

    public static float getPixelScaleFactor() {
        if (!getPixelScaleLogged) {
            getPixelScaleLogged = true;
            System.out.println("Bridge Display.getPixelScaleFactor()");
        }
        return 1.0f;
    }

    public static void setParent(java.awt.Canvas value) throws LWJGLException { parent = value; }
    public static java.awt.Canvas getParent() { return parent; }
    public static String getTitle() { return title; }
    public static void setDisplayConfiguration(float gamma, float brightness, float contrast) throws LWJGLException {}
    public static void setDisplayModeAndFullscreen(DisplayMode mode) throws LWJGLException {
        setDisplayMode(mode);
        setFullscreen(true);
    }

    public static void update() {
        update(true);
    }

    public static void update(boolean process_messages) {
        updateCount++;
        maybeLogLoop("update(" + process_messages + ")", updateCount);
        LinuxContextImplementation.nSwapBuffers();
        if (process_messages) {
            processMessages();
        }
    }

    private static boolean isBrowserFramePacingEnabled() {
        String value = System.getProperty("starsector.browserFramePacing");
        if (value == null) return true;
        value = value.trim();
        return !("false".equalsIgnoreCase(value) || "0".equals(value) || "off".equalsIgnoreCase(value));
    }

    private static void resetFramePacing() {
        syncTargetFps = 0;
        nextFrameDeadlineNanos = 0L;
    }

    public static void sync(int fps) {
        syncCount++;
        boolean pacingEnabled = isBrowserFramePacingEnabled();
        if (syncCount <= 3 || syncCount == 10 || syncCount % 300 == 0) {
            System.out.println(
                    "Bridge Display.sync(" + fps + ") count=" + syncCount
                            + " pacing=" + pacingEnabled
                            + " sleeps=" + syncSleepCount
                            + " late=" + syncLateCount
                            + " resets=" + syncResetCount);
        }
        if (!pacingEnabled || fps <= 0) {
            resetFramePacing();
            return;
        }

        // Clamp only pathological values; normal Starsector settings pass 60.
        int targetFps = Math.max(1, Math.min(1000, fps));
        long frameNanos = 1000000000L / targetFps;
        long now = System.nanoTime();
        if (syncTargetFps != targetFps || nextFrameDeadlineNanos <= 0L) {
            syncTargetFps = targetFps;
            nextFrameDeadlineNanos = now + frameNanos;
            syncResetCount++;
            return;
        }

        long remaining = nextFrameDeadlineNanos - now;
        if (remaining > 0L) {
            long sleepMillis = remaining / 1000000L;
            int sleepNanos = (int) (remaining % 1000000L);
            try {
                Thread.sleep(sleepMillis, sleepNanos);
                syncSleepCount++;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                resetFramePacing();
                return;
            }
            now = System.nanoTime();
        } else {
            syncLateCount++;
        }

        // Keep cadence anchored to the prior deadline. If the browser was paused
        // or a frame missed several periods, skip those periods instead of issuing
        // a burst of catch-up frames (a major source of visible pacing jitter).
        long behind = now - nextFrameDeadlineNanos;
        if (behind > frameNanos * 4L) {
            nextFrameDeadlineNanos = now + frameNanos;
            syncResetCount++;
        } else if (behind >= 0L) {
            nextFrameDeadlineNanos += (behind / frameNanos + 1L) * frameNanos;
        } else {
            nextFrameDeadlineNanos += frameNanos;
        }
    }

    public static void makeCurrent() throws LWJGLException {
        LinuxContextImplementation.nMakeCurrent();
    }

    public static void releaseContext() throws LWJGLException {}
    public static boolean isCurrent() throws LWJGLException { return true; }
    public static void setInitialBackground(float r, float g, float b) {}
    public static void setSwapInterval(int value) { swapInterval = value; }

    public static String getAdapter() {
        return "CheerpJ WebGL";
    }

    public static String getVersion() {
        return "2.1";
    }

    public static void processMessages() {
        try {
            // Desktop LWJGL drains the X11 event queue here. The browser bridge
            // synthesizes those X11 events from DOM input in lwjgl.js, so skipping
            // this call leaves keyboard/mouse events permanently queued.
            // DOM listeners in lwjgl.js maintain direct LWJGL input state; the
            // bridge LinuxDisplay class is intentionally minimal, so polling the
            // devices is the browser equivalent of desktop processMessages().
            pollDevices();
        } catch (Throwable t) {
            if (!inputPumpFailureLogged) {
                inputPumpFailureLogged = true;
                System.out.println("Bridge Display input pump failed: " + t);
            }
        }
    }

    public static void swapBuffers() throws LWJGLException {
        swapBuffersCount++;
        maybeLogLoop("swapBuffers", swapBuffersCount);
        LinuxContextImplementation.nSwapBuffers();
    }

    public static Drawable getDrawable() {
        if (!getDrawableLogged) {
            getDrawableLogged = true;
            System.out.println("Bridge Display.getDrawable()");
        }
        return null;
    }

    static DisplayImplementation getImplementation() {
        if (!getImplementationLogged) {
            getImplementationLogged = true;
            System.out.println("Bridge Display.getImplementation()");
        }
        return implementation;
    }

    public static boolean isCloseRequested() {
        return false;
    }

    public static boolean isDirty() {
        return false;
    }

    static boolean getPrivilegedBoolean(String key) {
        return Boolean.getBoolean(key);
    }

    static String getPrivilegedString(String key) {
        return System.getProperty(key);
    }

    static void pollDevices() {
        try {
            if (org.lwjgl.input.Mouse.isCreated()) {
                org.lwjgl.input.Mouse.poll();
                org.lwjgl.input.Mouse.updateCursor();
            }
            if (org.lwjgl.input.Keyboard.isCreated()) {
                org.lwjgl.input.Keyboard.poll();
            }
        } catch (Throwable t) {
            if (!inputPumpFailureLogged) {
                inputPumpFailureLogged = true;
                System.out.println("Bridge Display device poll failed: " + t);
            }
        }
    }
}
