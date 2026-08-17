package com.fs.starfarer.campaign.econ.reach;

/** Timing-only accumulator for ReachEconomy.nextStep() during browser diagnostics. */
public final class BrowserReachEconomyPerfDiag {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserGameplayProbe";
    private static int stepIndex;
    private static long stepStartedMs;
    private static final long[] batchStartedMs = new long[5];
    private static final long[] batchTotalMs = new long[5];
    private static final long[] batchMaxMs = new long[5];
    private static final int[] batchCount = new int[5];

    private BrowserReachEconomyPerfDiag() {}

    private static boolean enabled() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    public static void begin() {
        if (!enabled()) return;
        stepIndex++;
        stepStartedMs = System.currentTimeMillis();
        for (int i = 1; i <= 4; i++) {
            batchStartedMs[i] = 0L;
            batchTotalMs[i] = 0L;
            batchMaxMs[i] = 0L;
            batchCount[i] = 0;
        }
    }

    public static void beforeBatch(int kind) {
        if (!enabled() || kind < 1 || kind > 4) return;
        batchStartedMs[kind] = System.currentTimeMillis();
    }

    public static void afterBatch(int kind) {
        if (!enabled() || kind < 1 || kind > 4) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - batchStartedMs[kind]);
        batchCount[kind]++;
        batchTotalMs[kind] += elapsed;
        if (elapsed > batchMaxMs[kind]) batchMaxMs[kind] = elapsed;
    }

    public static void finish() {
        if (!enabled()) return;
        long total = Math.max(0L, System.currentTimeMillis() - stepStartedMs);
        System.out.println(
                "BrowserReachEconomyPerfDiag: step=" + stepIndex
                        + " totalMs=" + total
                        + " mainCount=" + batchCount[1]
                        + " mainMs=" + batchTotalMs[1]
                        + " mainMaxMs=" + batchMaxMs[1]
                        + " reapplyCount=" + batchCount[2]
                        + " reapplyMs=" + batchTotalMs[2]
                        + " reapplyMaxMs=" + batchMaxMs[2]
                        + " immigrationCount=" + batchCount[3]
                        + " immigrationMs=" + batchTotalMs[3]
                        + " immigrationMaxMs=" + batchMaxMs[3]
                        + " finishCount=" + batchCount[4]
                        + " finishMs=" + batchTotalMs[4]
                        + " finishMaxMs=" + batchMaxMs[4]);
    }
}
