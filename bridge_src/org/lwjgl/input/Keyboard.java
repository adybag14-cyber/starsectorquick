package org.lwjgl.input;

/** Browser-backed LWJGL 2 keyboard compatibility bridge. */
public class Keyboard {
    public static final int KEY_NONE = 0;
    public static final int KEY_ESCAPE = 1;
    private static boolean created = false;
    private static boolean repeatEvents = false;
    private static boolean repeatLogPrinted = false;

    private Keyboard() {}

    public static void create() {
        created = true;
        nReset();
    }

    public static void destroy() {
        created = false;
        repeatEvents = false;
        nReset();
    }

    public static void poll() { nPoll(); }
    public static boolean isCreated() { return created; }
    public static boolean isKeyDown(int key) { return created && nIsKeyDown(key); }
    public static String getKeyName(int key) { return "KEY_" + key; }
    public static int getKeyIndex(String keyName) { return KEY_NONE; }
    public static int getNumKeys() { return 256; }
    public static boolean next() { return created && nNext(); }
    public static int getEventKey() { return nGetEventKey(); }
    public static boolean getEventKeyState() { return nGetEventKeyState(); }
    public static char getEventCharacter() { return (char) nGetEventCharacter(); }
    public static long getEventNanoseconds() { return nGetEventNanoseconds(); }
    public static boolean isRepeatEvent() { return nIsRepeatEvent(); }

    public static void enableRepeatEvents(boolean enable) {
        if (!repeatLogPrinted) {
            repeatLogPrinted = true;
            System.out.println("Bridge Keyboard.enableRepeatEvents(" + enable + ")");
        }
        repeatEvents = enable;
        nSetRepeatEvents(enable);
    }

    public static boolean areRepeatEventsEnabled() { return repeatEvents; }

    private static native void nReset();
    private static native void nPoll();
    private static native boolean nIsKeyDown(int key);
    private static native boolean nNext();
    private static native int nGetEventKey();
    private static native boolean nGetEventKeyState();
    private static native int nGetEventCharacter();
    private static native long nGetEventNanoseconds();
    private static native boolean nIsRepeatEvent();
    private static native void nSetRepeatEvents(boolean enable);
}
