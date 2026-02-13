package org.lwjgl;

public final class Zys {
    private Zys() {}

    public static void initialize() {}

    public static boolean is64Bit() {
        return true;
    }

    public static String getVersion() {
        return "2.9.3-cheerpj";
    }

    public static long getTimerResolution() {
        try {
            return Sys.getTimerResolution();
        } catch (Throwable ignored) {
            return 1000L;
        }
    }

    public static long getTime() {
        try {
            return Sys.getTime();
        } catch (Throwable ignored) {
            return System.currentTimeMillis();
        }
    }

    public static void alert(String title, String message) {
        try {
            Sys.alert(title, message);
        } catch (Throwable ignored) {
            System.err.println("ALERT: " + title + " - " + message);
        }
    }
}
