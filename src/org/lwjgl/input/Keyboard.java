package org.lwjgl.input;

public class Keyboard {
    public static final int KEY_ESCAPE = 1;
    private static boolean created = false;
    private static boolean repeatEvents = false;
    private static boolean repeatLogPrinted = false;

    public static void create() { created = true; }
    public static void destroy() { created = false; repeatEvents = false; }
    public static void poll() {}
    public static boolean isCreated() { return created; }
    public static boolean isKeyDown(int key) { return false; }
    public static String getKeyName(int key) { return "KEY_" + key; }
    public static int getKeyIndex(String keyName) { return KEY_ESCAPE; }
    public static int getNumKeys() { return 256; }
    public static boolean next() { return false; }
    public static int getEventKey() { return 0; }
    public static boolean getEventKeyState() { return false; }
    public static char getEventCharacter() { return 0; }
    public static long getEventNanoseconds() { return System.nanoTime(); }
    public static boolean isRepeatEvent() { return false; }
    public static void enableRepeatEvents(boolean enable) {
        if (!repeatLogPrinted) {
            repeatLogPrinted = true;
            System.out.println("Bridge Keyboard.enableRepeatEvents(" + enable + ")");
        }
        repeatEvents = enable;
    }
    public static boolean areRepeatEventsEnabled() { return repeatEvents; }
}
