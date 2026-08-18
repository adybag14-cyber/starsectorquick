package com.fs.starfarer.loading;

/** Diagnostic-only aggregate timer for the hot per-file variant loader loop. */
public final class BrowserVariantLoopDiag {
    private static long loopStart;
    private static long parseStart;
    private static long ctorStart;
    private static long registerStart;
    private static long parseNanos;
    private static long ctorNanos;
    private static long registerNanos;
    private static int parses;
    private static int constructors;
    private static int registrations;

    private BrowserVariantLoopDiag() {}

    public static void begin() {
        loopStart = System.nanoTime();
        parseStart = ctorStart = registerStart = 0L;
        parseNanos = ctorNanos = registerNanos = 0L;
        parses = constructors = registrations = 0;
    }

    public static void parseStart() { parseStart = System.nanoTime(); }
    public static void parseEnd() {
        if (parseStart != 0L) {
            parseNanos += System.nanoTime() - parseStart;
            parseStart = 0L;
            parses++;
        }
    }
    public static void ctorStart() { ctorStart = System.nanoTime(); }
    public static void ctorEnd() {
        if (ctorStart != 0L) {
            ctorNanos += System.nanoTime() - ctorStart;
            ctorStart = 0L;
            constructors++;
        }
    }
    public static void registerStart() { registerStart = System.nanoTime(); }
    public static void registerEnd() {
        if (registerStart != 0L) {
            registerNanos += System.nanoTime() - registerStart;
            registerStart = 0L;
            registrations++;
        }
    }

    public static void finishLoop() {
        if (loopStart == 0L) return;
        long total = System.nanoTime() - loopStart;
        long measured = parseNanos + ctorNanos + registerNanos;
        long residual = Math.max(0L, total - measured);
        System.out.println("BrowserVariantLoopDiag: parses=" + parses
                + " constructors=" + constructors
                + " registrations=" + registrations
                + " totalMs=" + ms(total)
                + " parseMs=" + ms(parseNanos)
                + " ctorMs=" + ms(ctorNanos)
                + " registerMs=" + ms(registerNanos)
                + " residualMs=" + ms(residual));
        loopStart = 0L;
    }

    private static double ms(long nanos) { return ((double)nanos) / 1000000.0d; }
}
