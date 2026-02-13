package org.lwjgl.input;

public class Mous2 {
    private Mous2() {}

    public static void create() { Mouse.create(); }
    public static void destroy() { Mouse.destroy(); }
    public static void poll() { Mouse.poll(); }
    public static boolean isCreated() { return Mouse.isCreated(); }
    public static boolean isButtonDown(int button) { return Mouse.isButtonDown(button); }
    public static String getButtonName(int button) { return Mouse.getButtonName(button); }
    public static int getButtonIndex(String buttonName) { return Mouse.getButtonIndex(buttonName); }
    public static boolean next() { return Mouse.next(); }
    public static int getEventButton() { return Mouse.getEventButton(); }
    public static boolean getEventButtonState() { return Mouse.getEventButtonState(); }
    public static int getEventDX() { return Mouse.getEventDX(); }
    public static int getEventDY() { return Mouse.getEventDY(); }
    public static int getEventX() { return Mouse.getEventX(); }
    public static int getEventY() { return Mouse.getEventY(); }
    public static int getEventDWheel() { return Mouse.getEventDWheel(); }
    public static long getEventNanoseconds() { return Mouse.getEventNanoseconds(); }
    public static int getX() { return Mouse.getX(); }
    public static int getY() { return Mouse.getY(); }
    public static int getDX() { return Mouse.getDX(); }
    public static int getDY() { return Mouse.getDY(); }
    public static int getDWheel() { return Mouse.getDWheel(); }
    public static int getButtonCount() { return Mouse.getButtonCount(); }
    public static boolean hasWheel() { return Mouse.hasWheel(); }
    public static boolean isGrabbed() { return Mouse.isGrabbed(); }
    public static void setGrabbed(boolean grab) { Mouse.setGrabbed(grab); }
    public static void setCursorPosition(int x, int y) { Mouse.setCursorPosition(x, y); }
    public static Cursor setNativeCursor(Cursor cursor) { return Mouse.setNativeCursor(cursor); }
    public static boolean isInsideWindow() { return Mouse.isInsideWindow(); }
    public static void setClipMouseCoordinatesToWindow(boolean clip) { Mouse.setClipMouseCoordinatesToWindow(clip); }
}
