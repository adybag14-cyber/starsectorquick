package org.lwjgl.input;

public class Keyboard {
    public static final int KEY_ESCAPE = 1;
    public static native void create();
    public static native void destroy();
    public static native void poll();
    public static native boolean isCreated();
    public static native boolean isKeyDown(int key);
    public static native String getKeyName(int key);
    public static native int getKeyIndex(String keyName);
    public static native int getNumKeys();
    public static native boolean next();
    public static native int getEventKey();
    public static native boolean getEventKeyState();
    public static native char getEventCharacter();
    public static native long getEventNanoseconds();
    public static native boolean isRepeatEvent();
    public static native void enableRepeatEvents(boolean enable);
    public static native boolean areRepeatEventsEnabled();
}
