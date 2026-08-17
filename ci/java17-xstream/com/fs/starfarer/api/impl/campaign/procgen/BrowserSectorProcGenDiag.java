package com.fs.starfarer.api.impl.campaign.procgen;

/** Low-overhead timing accumulator for full browser SectorProcGen diagnostics. */
public final class BrowserSectorProcGenDiag {
    private static final String ENABLE_PROPERTY = "starsector.browserGameplayProbe";
    private static long startedMs;
    private static long constellationStartedMs;
    private static long constellationTotalMs;
    private static long constellationMaxMs;
    private static int constellationCount;
    private static long randomArcStartedMs;
    private static long randomArcTotalMs;
    private static long randomArcMaxMs;
    private static int randomArcCount;

    private BrowserSectorProcGenDiag() {}

    private static boolean enabled() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    public static void begin() {
        if (!enabled()) return;
        startedMs = System.currentTimeMillis();
        constellationStartedMs = 0L;
        constellationTotalMs = 0L;
        constellationMaxMs = 0L;
        constellationCount = 0;
        randomArcStartedMs = 0L;
        randomArcTotalMs = 0L;
        randomArcMaxMs = 0L;
        randomArcCount = 0;
        mark("begin");
    }

    public static void mark(String label) {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        System.out.println(
                "BrowserSectorProcGenDiag: t=" + now
                        + " elapsedMs=" + (startedMs == 0L ? -1L : now - startedMs)
                        + " phase=" + label);
    }

    public static void beforeConstellation() {
        if (!enabled()) return;
        constellationStartedMs = System.currentTimeMillis();
    }

    public static void afterConstellation() {
        if (!enabled()) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - constellationStartedMs);
        constellationCount++;
        constellationTotalMs += elapsed;
        if (elapsed > constellationMaxMs) constellationMaxMs = elapsed;
        if (constellationCount == 1 || constellationCount % 10 == 0) {
            System.out.println(
                    "BrowserSectorProcGenDiag: constellations count=" + constellationCount
                            + " totalMs=" + constellationTotalMs
                            + " maxMs=" + constellationMaxMs);
        }
    }

    public static void beforeRandomArc() {
        if (!enabled()) return;
        randomArcStartedMs = System.currentTimeMillis();
    }

    public static void afterRandomArc() {
        if (!enabled()) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - randomArcStartedMs);
        randomArcCount++;
        randomArcTotalMs += elapsed;
        if (elapsed > randomArcMaxMs) randomArcMaxMs = elapsed;
    }

    public static void finish() {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        System.out.println(
                "BrowserSectorProcGenDiag: summary totalMs=" + (startedMs == 0L ? -1L : now - startedMs)
                        + " constellations=" + constellationCount
                        + " constellationTotalMs=" + constellationTotalMs
                        + " constellationMaxMs=" + constellationMaxMs
                        + " randomArcs=" + randomArcCount
                        + " randomArcTotalMs=" + randomArcTotalMs
                        + " randomArcMaxMs=" + randomArcMaxMs);
        mark("end");
    }
}
