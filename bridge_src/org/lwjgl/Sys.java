package org.lwjgl;

public final class Sys {
    private static boolean initializedLogged = false;
    private Sys() {}

    public static long getTimerResolution() {
        return 1000L;
    }

    public static long getTime() {
        return System.currentTimeMillis();
    }

    public static void alert(String title, String message) {
        System.err.println("LWJGL alert: " + title + " - " + message);
    }

    public static boolean is64Bit() {
        return true;
    }

    public static void initialize() {
        if (!initializedLogged) {
            initializedLogged = true;
            System.out.println("Bridge Sys.initialize()");
        }
    }

    public static String getVersion() {
        return "2.9.3-cheerpj";
    }

    public static String getClipboard() {
        return "";
    }
}
