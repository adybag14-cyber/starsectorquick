package com.fs.starfarer;

/** Property-gated timing accumulator for the stock new-campaign creation path. */
public final class BrowserCampaignCreatePerfDiag {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserGameplayProbe";
    private static long startedMs;
    private static long sectorGeneratorStartedMs;
    private static int sectorGeneratorCount;
    private static long sectorGeneratorTotalMs;
    private static long advanceStartedMs;
    private static int advanceCount;
    private static long advanceTotalMs;
    private static long advanceMaxMs;
    private static long economyStepStartedMs;
    private static int economyStepCount;
    private static long economyStepTotalMs;
    private static long economyStepMaxMs;

    private BrowserCampaignCreatePerfDiag() {}

    private static boolean enabled() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    public static void begin() {
        if (!enabled()) return;
        startedMs = System.currentTimeMillis();
        sectorGeneratorStartedMs = 0L;
        sectorGeneratorCount = 0;
        sectorGeneratorTotalMs = 0L;
        advanceStartedMs = 0L;
        advanceCount = 0;
        advanceTotalMs = 0L;
        advanceMaxMs = 0L;
        economyStepStartedMs = 0L;
        economyStepCount = 0;
        economyStepTotalMs = 0L;
        economyStepMaxMs = 0L;
        mark("begin");
    }

    public static void mark(java.lang.String phase) {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        System.out.println(
                "BrowserCampaignCreatePerfDiag: t=" + now
                        + " elapsedMs=" + (startedMs == 0L ? -1L : now - startedMs)
                        + " phase=" + phase);
    }

    public static void beforeSectorGenerator() {
        if (!enabled()) return;
        sectorGeneratorStartedMs = System.currentTimeMillis();
        mark("sectorGenerator" + (sectorGeneratorCount + 1) + "-before");
    }

    public static void afterSectorGenerator() {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - sectorGeneratorStartedMs);
        sectorGeneratorCount++;
        sectorGeneratorTotalMs += elapsed;
        System.out.println(
                "BrowserCampaignCreatePerfDiag: t=" + now
                        + " elapsedMs=" + (startedMs == 0L ? -1L : now - startedMs)
                        + " phase=sectorGenerator" + sectorGeneratorCount + "-after"
                        + " callMs=" + elapsed);
    }

    public static void beforeAdvance() {
        if (!enabled()) return;
        advanceStartedMs = System.currentTimeMillis();
    }

    public static void afterAdvance() {
        if (!enabled()) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - advanceStartedMs);
        advanceCount++;
        advanceTotalMs += elapsed;
        if (elapsed > advanceMaxMs) advanceMaxMs = elapsed;
    }

    public static void beforeEconomyStep() {
        if (!enabled()) return;
        economyStepStartedMs = System.currentTimeMillis();
    }

    public static void afterEconomyStep() {
        if (!enabled()) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - economyStepStartedMs);
        economyStepCount++;
        economyStepTotalMs += elapsed;
        if (elapsed > economyStepMaxMs) economyStepMaxMs = elapsed;
    }

    public static void finish() {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        System.out.println(
                "BrowserCampaignCreatePerfDiag: summary totalMs="
                        + (startedMs == 0L ? -1L : now - startedMs)
                        + " sectorGenerators=" + sectorGeneratorCount
                        + " sectorGeneratorTotalMs=" + sectorGeneratorTotalMs
                        + " advances=" + advanceCount
                        + " advanceTotalMs=" + advanceTotalMs
                        + " advanceMaxMs=" + advanceMaxMs
                        + " economySteps=" + economyStepCount
                        + " economyStepTotalMs=" + economyStepTotalMs
                        + " economyStepMaxMs=" + economyStepMaxMs);
        mark("end");
    }
}
