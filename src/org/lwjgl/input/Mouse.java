package org.lwjgl.input;

public class Mouse {
    public static native void create();
    public static native void destroy();
    public static native void poll();
    public static native boolean isCreated();
    public static native boolean isButtonDown(int button);
    public static native String getButtonName(int button);
    public static native int getButtonIndex(String buttonName);
    public static native boolean next();
    public static native int getEventButton();
    public static native boolean getEventButtonState();
    public static native int getEventDX();
    public static native int getEventDY();
    public static native int getEventX();
    public static native int getEventY();
    public static native int getEventDWheel();
    public static native long getEventNanoseconds();
    public static native int getX();
    public static native int getY();
    public static native int getDX();
    public static native int getDY();
    public static native int getDWheel();
    public static native int getButtonCount();
    public static native boolean hasWheel();
    public static native boolean isGrabbed();
    public static native void setGrabbed(boolean grab);
    public static native void setCursorPosition(int x, int y);
    public static native Cursor setNativeCursor(Cursor cursor);
    public static native boolean isInsideWindow();
    public static native void setClipMouseCoordinatesToWindow(boolean clip);
}
