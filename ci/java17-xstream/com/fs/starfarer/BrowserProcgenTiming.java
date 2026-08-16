package com.fs.starfarer;

import java.util.concurrent.ConcurrentHashMap;

/** Diagnostic-only timing for full campaign generation; no gameplay semantics. */
public final class BrowserProcgenTiming {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserProcgenTiming";
    private static final ConcurrentHashMap<java.lang.String, Long> STARTS = new ConcurrentHashMap<java.lang.String, Long>();
    private BrowserProcgenTiming() {}

    public static void stepBegin(java.lang.String label) {
        if (!Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "true")) || label == null) return;
        STARTS.put(label, Long.valueOf(System.nanoTime()));
        System.out.println("BrowserProcgenTiming: step-begin label=" + safe(label));
    }

    public static void stepEnd(java.lang.String label) {
        if (!Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "true")) || label == null) return;
        Long start = STARTS.remove(label);
        long elapsed = start == null ? -1L : System.nanoTime() - start.longValue();
        System.out.println("BrowserProcgenTiming: step-end label=" + safe(label)
                + " elapsedMs=" + (elapsed < 0L ? -1L : elapsed / 1000000L));
    }

    public static long beginOuter() {
        if (!Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "true"))) return 0L;
        System.out.println("BrowserProcgenTiming: outer-begin");
        return System.nanoTime();
    }

    public static void endOuter(long start) {
        if (!Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "true"))) return;
        long elapsed = start == 0L ? -1L : System.nanoTime() - start;
        System.out.println("BrowserProcgenTiming: outer-end elapsedMs="
                + (elapsed < 0L ? -1L : elapsed / 1000000L));
    }

    private static java.lang.String safe(java.lang.String value) {
        return value == null ? "<null>" : value.replace(' ', '_').replace('\n', '_').replace('\r', '_');
    }
}
