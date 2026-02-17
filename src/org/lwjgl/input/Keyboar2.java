package org.lwjgl.input;

public class Keyboar2 {
    public static final int KEY_ESCAPE = Keyboard.KEY_ESCAPE;

    private Keyboar2() {}

    public static void create() { Keyboard.create(); }
    public static void destroy() { Keyboard.destroy(); }
    public static void poll() { Keyboard.poll(); }
    public static boolean isCreated() { return Keyboard.isCreated(); }
    public static boolean isKeyDown(int key) { return Keyboard.isKeyDown(key); }
    public static String getKeyName(int key) { return Keyboard.getKeyName(key); }
    public static int getKeyIndex(String keyName) { return Keyboard.getKeyIndex(keyName); }
    public static int getNumKeys() { return Keyboard.getNumKeys(); }
    public static boolean next() { return Keyboard.next(); }
    public static int getEventKey() { return Keyboard.getEventKey(); }
    public static boolean getEventKeyState() { return Keyboard.getEventKeyState(); }
    public static char getEventCharacter() { return Keyboard.getEventCharacter(); }
    public static long getEventNanoseconds() { return Keyboard.getEventNanoseconds(); }
    public static boolean isRepeatEvent() { return Keyboard.isRepeatEvent(); }
    public static void enableRepeatEvents(boolean enable) { Keyboard.enableRepeatEvents(enable); }
    public static boolean areRepeatEventsEnabled() { return Keyboard.areRepeatEventsEnabled(); }
}
