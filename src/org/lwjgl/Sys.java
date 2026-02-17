package org.lwjgl;

public final class Sys {
    static {
        System.out.println("Sys stub loaded!");
    }
    public static void initialize() {
        System.out.println("Sys.initialize() called.");
    }
    public static boolean is64Bit() { return true; }
}