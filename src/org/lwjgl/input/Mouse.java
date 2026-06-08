package org.lwjgl.input;

public class Mouse {
    private static boolean created = false;
    private static boolean grabbed = false;
    private static int x = 0;
    private static int y = 0;
    private static boolean setGrabbedLogged = false;

    public static void create() { created = true; }
    public static void destroy() { created = false; grabbed = false; }
    public static void poll() {}
    public static boolean isCreated() { return created; }
    public static boolean isButtonDown(int button) { return false; }
    public static String getButtonName(int button) { return "BUTTON" + button; }
    public static int getButtonIndex(String buttonName) { return 0; }
    public static boolean next() { return false; }
    public static int getEventButton() { return -1; }
    public static boolean getEventButtonState() { return false; }
    public static int getEventDX() { return 0; }
    public static int getEventDY() { return 0; }
    public static int getEventX() { return x; }
    public static int getEventY() { return y; }
    public static int getEventDWheel() { return 0; }
    public static long getEventNanoseconds() { return System.nanoTime(); }
    public static int getX() { return x; }
    public static int getY() { return y; }
    public static int getDX() { return 0; }
    public static int getDY() { return 0; }
    public static int getDWheel() { return 0; }
    public static int getButtonCount() { return 3; }
    public static boolean hasWheel() { return true; }
    public static boolean isGrabbed() { return grabbed; }
    public static void setGrabbed(boolean grab) {
        if (!setGrabbedLogged) {
            setGrabbedLogged = true;
            System.out.println("Bridge Mouse.setGrabbed(" + grab + ")");
        }
        grabbed = grab;
    }
    public static void setCursorPosition(int newX, int newY) { x = newX; y = newY; }
    public static Cursor setNativeCursor(Cursor cursor) { return cursor; }
    public static boolean isInsideWindow() { return true; }
    public static void setClipMouseCoordinatesToWindow(boolean clip) {}
}
