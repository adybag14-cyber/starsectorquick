package com.fs.starfarer.campaign.rules;

/** Diagnostic-only aggregate timer for Rules load subphases. */
public final class BrowserRulesPhaseDiag {
    public static final int PRE = 0;
    public static final int SETUP = 1;
    public static final int CONDITIONS = 2;
    public static final int OPTIONS = 3;
    public static final int SCRIPT = 4;
    public static final int REGISTER = 5;
    public static final int POST = 6;
    private static final long[] NANOS = new long[7];
    private static long last;
    private static int phase;
    private static int rows;
    private static int transitions;

    private BrowserRulesPhaseDiag() {}

    public static void begin() {
        for (int i = 0; i < NANOS.length; i++) NANOS[i] = 0L;
        last = System.nanoTime();
        phase = PRE;
        rows = 0;
        transitions = 0;
    }

    private static void advance(int next) {
        long now = System.nanoTime();
        if (last != 0L && phase >= 0 && phase < NANOS.length) NANOS[phase] += now - last;
        last = now;
        phase = next;
        transitions++;
    }

    public static void rowStart() {
        advance(SETUP);
        rows++;
    }

    public static void transition(int next) {
        advance(next);
    }

    public static void finish() {
        long now = System.nanoTime();
        if (last != 0L && phase >= 0 && phase < NANOS.length) NANOS[phase] += now - last;
        long total = 0L;
        for (long value : NANOS) total += value;
        System.out.println("BrowserRulesPhase: rows=" + rows
                + " transitions=" + transitions
                + " totalMs=" + ms(total)
                + " preMs=" + ms(NANOS[PRE])
                + " setupMs=" + ms(NANOS[SETUP])
                + " conditionsMs=" + ms(NANOS[CONDITIONS])
                + " optionsMs=" + ms(NANOS[OPTIONS])
                + " scriptMs=" + ms(NANOS[SCRIPT])
                + " registerMs=" + ms(NANOS[REGISTER])
                + " postMs=" + ms(NANOS[POST]));
        last = 0L;
    }

    private static double ms(long nanos) {
        return ((double) nanos) / 1000000.0d;
    }
}
