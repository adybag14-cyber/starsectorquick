package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import org.lwjgl.LWJGLException;

public final class Displa2 {
    private Displa2() {}

    public static void create() throws LWJGLException {
        Display.create();
    }

    public static void create(PixelFormat pixel_format) throws LWJGLException {
        Display.create(pixel_format);
    }

    public static void create(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
        Display.create(pixel_format, attribs);
    }

    public static void create(PixelFormat pixel_format, Drawable shared_drawable) throws LWJGLException {
        Display.create(pixel_format, shared_drawable);
    }

    public static void create(PixelFormat pixel_format, ContextAttribs attribs, Drawable shared_drawable) throws LWJGLException {
        Display.create(pixel_format, attribs, shared_drawable);
    }

    public static void destroy() {
        Display.destroy();
    }

    public static boolean isCreated() {
        return Display.isCreated();
    }

    public static boolean isActive() {
        return Display.isActive();
    }

    public static boolean isVisible() {
        return Display.isVisible();
    }

    public static void setLocation(int x, int y) {
        Display.setLocation(x, y);
    }

    public static void setVSyncEnabled(boolean sync) {
        Display.setVSyncEnabled(sync);
    }

    public static void setTitle(String title) {
        Display.setTitle(title);
    }

    public static int setIcon(ByteBuffer[] icons) {
        return Display.setIcon(icons);
    }

    public static void setResizable(boolean resizable) {
        Display.setResizable(resizable);
    }

    public static boolean isResizable() {
        return Display.isResizable();
    }

    public static boolean wasResized() {
        return Display.wasResized();
    }

    public static int getX() {
        return Display.getX();
    }

    public static int getY() {
        return Display.getY();
    }

    public static int getWidth() {
        return Display.getWidth();
    }

    public static int getHeight() {
        return Display.getHeight();
    }

    public static DisplayMode getDisplayMode() {
        return Display.getDisplayMode();
    }

    public static DisplayMode getDesktopDisplayMode() {
        return Display.getDesktopDisplayMode();
    }

    public static void setDisplayMode(DisplayMode mode) throws LWJGLException {
        Display.setDisplayMode(mode);
    }

    public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        return Display.getAvailableDisplayModes();
    }

    public static void setFullscreen(boolean fullscreen) throws LWJGLException {
        Display.setFullscreen(fullscreen);
    }

    public static boolean isFullscreen() {
        return Display.isFullscreen();
    }

    public static float getPixelScaleFactor() {
        return Display.getPixelScaleFactor();
    }

    public static void setParent(java.awt.Canvas parent) throws LWJGLException {
        Display.setParent(parent);
    }

    public static void update() {
        Display.update();
    }

    public static void update(boolean process_messages) {
        Display.update(process_messages);
    }

    public static void sync(int fps) {
        Display.sync(fps);
    }

    public static void makeCurrent() throws LWJGLException {
        Display.makeCurrent();
    }

    public static String getAdapter() {
        return Display.getAdapter();
    }

    public static String getVersion() {
        return Display.getVersion();
    }

    public static void processMessages() {
        Display.processMessages();
    }

    public static void swapBuffers() throws LWJGLException {
        Display.swapBuffers();
    }

    public static Drawable getDrawable() {
        return Display.getDrawable();
    }

    public static boolean isCloseRequested() {
        return Display.isCloseRequested();
    }

    public static boolean isDirty() {
        return Display.isDirty();
    }
}
