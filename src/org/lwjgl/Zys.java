package org.lwjgl;

public final class Zys {
    static { System.out.println("Zys stub loaded!"); }
    public static void initialize() { System.out.println("Zys.initialize() called."); }
    public static boolean is64Bit() { return true; }
    public static String getVersion() { return "2.9.3"; }
    public static long getTimerResolution() { return 1000; }
    public static long getTime() { return System.currentTimeMillis(); }
    public static void alert(String t, String m) { System.out.println("ALERT: " + t + " - " + m); }
}
