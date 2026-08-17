package com.fs.starfarer;

/** Timing-only browser diagnostic for the synchronous campaign save path. */
public final class BrowserInitialSavePerfDiag {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserGameplayProbe";
    private static long startedMs;

    private BrowserInitialSavePerfDiag() {}

    private static boolean enabled() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    public static void begin() {
        if (!enabled()) return;
        startedMs = System.currentTimeMillis();
        mark("begin");
    }

    public static void mark(java.lang.String phase) {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        System.out.println(
                "BrowserInitialSavePerfDiag: t=" + now
                        + " elapsedMs=" + (startedMs == 0L ? -1L : now - startedMs)
                        + " phase=" + phase);
    }

    public static void finish() {
        if (!enabled()) return;
        mark("end");
    }
}
