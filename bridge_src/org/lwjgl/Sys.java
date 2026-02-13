package org.lwjgl;

public final class Sys {
    static {
        try {
            System.loadLibrary("lwjgl");
        } catch (Throwable ignored) {}
    }

    private Sys() {}

    public static native long getTimerResolution();
    public static native long getTime();
    public static native void alert(String title, String message);

    public static boolean is64Bit() {
        return true;
    }

    public static void initialize() {}
}
