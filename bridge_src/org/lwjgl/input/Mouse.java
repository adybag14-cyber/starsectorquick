package org.lwjgl.input;

/** Browser-backed LWJGL 2 mouse compatibility bridge. */
public class Mouse {
    private static boolean created = false;
    private static boolean grabbed = false;
    private static boolean setGrabbedLogged = false;

    private Mouse() {}

    public static void create() {
        created = true;
        nReset();
    }

    public static void destroy() {
        created = false;
        grabbed = false;
        nSetGrabbed(false);
        nReset();
    }

    public static void poll() { nPoll(); }
    public static void updateCursor() {}
    public static boolean isCreated() { return created; }
    public static boolean isButtonDown(int button) { return created && nIsButtonDown(button); }
    public static String getButtonName(int button) { return "BUTTON" + button; }
    public static int getButtonIndex(String buttonName) { return 0; }
    public static boolean next() { return created && nNext(); }
    public static int getEventButton() { return nGetEventButton(); }
    public static boolean getEventButtonState() { return nGetEventButtonState(); }
    public static int getEventDX() { return nGetEventDX(); }
    public static int getEventDY() { return nGetEventDY(); }
    public static int getEventX() { return nGetEventX(); }
    public static int getEventY() { return nGetEventY(); }
    public static int getEventDWheel() { return nGetEventDWheel(); }
    public static long getEventNanoseconds() { return nGetEventNanoseconds(); }
    public static int getX() { return nGetX(); }
    public static int getY() { return nGetY(); }
    public static int getDX() { return nGetDX(); }
    public static int getDY() { return nGetDY(); }
    public static int getDWheel() { return nGetDWheel(); }
    public static int getButtonCount() { return 5; }
    public static boolean hasWheel() { return true; }
    public static boolean isGrabbed() { return grabbed; }

    public static void setGrabbed(boolean grab) {
        if (!setGrabbedLogged) {
            setGrabbedLogged = true;
            System.out.println("Bridge Mouse.setGrabbed(" + grab + ")");
        }
        grabbed = grab;
        nSetGrabbed(grab);
    }

    public static void setCursorPosition(int newX, int newY) { nSetCursorPosition(newX, newY); }
    public static Cursor setNativeCursor(Cursor cursor) { return cursor; }
    public static boolean isInsideWindow() { return nIsInsideWindow(); }
    public static void setClipMouseCoordinatesToWindow(boolean clip) {}

    private static native void nReset();
    private static native void nPoll();
    private static native boolean nIsButtonDown(int button);
    private static native boolean nNext();
    private static native int nGetEventButton();
    private static native boolean nGetEventButtonState();
    private static native int nGetEventDX();
    private static native int nGetEventDY();
    private static native int nGetEventX();
    private static native int nGetEventY();
    private static native int nGetEventDWheel();
    private static native long nGetEventNanoseconds();
    private static native int nGetX();
    private static native int nGetY();
    private static native int nGetDX();
    private static native int nGetDY();
    private static native int nGetDWheel();
    private static native void nSetGrabbed(boolean grab);
    private static native void nSetCursorPosition(int x, int y);
    private static native boolean nIsInsideWindow();
}
