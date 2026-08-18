package com.fs.starfarer.loading;

/** Diagnostic-only aggregate timer for LoadingUtils merged rules.csv load. */
public final class BrowserRulesCsvLoadDiag {
    public static final int DISCOVERY = 0;
    public static final int OVERRIDE = 1;
    public static final int READ = 2;
    public static final int PREPROCESS = 3;
    public static final int PARSE = 4;
    public static final int MERGE = 5;
    public static final int COPY = 6;
    private static final long[] NANOS = new long[7];
    private static boolean active;
    private static long last;
    private static int phase;
    private static int sources;

    private BrowserRulesCsvLoadDiag() {}

    public static void begin(String path) {
        active = "data/campaign/rules.csv".equals(path);
        if (!active) return;
        for (int i = 0; i < NANOS.length; i++) NANOS[i] = 0L;
        last = System.nanoTime();
        phase = DISCOVERY;
        sources = 0;
    }

    public static void transition(int next) {
        if (!active) return;
        long now = System.nanoTime();
        NANOS[phase] += now - last;
        last = now;
        phase = next;
    }

    public static void parsedSource() {
        if (!active) return;
        transition(MERGE);
        sources++;
    }

    public static void finish() {
        if (!active) return;
        long now = System.nanoTime();
        NANOS[phase] += now - last;
        long total = 0L;
        for (long value : NANOS) total += value;
        System.out.println("BrowserRulesCsvLoadDiag: sources=" + sources
                + " totalMs=" + ms(total)
                + " discoveryMs=" + ms(NANOS[DISCOVERY])
                + " overrideMs=" + ms(NANOS[OVERRIDE])
                + " readMs=" + ms(NANOS[READ])
                + " preprocessMs=" + ms(NANOS[PREPROCESS])
                + " parseMs=" + ms(NANOS[PARSE])
                + " mergeMs=" + ms(NANOS[MERGE])
                + " copyMs=" + ms(NANOS[COPY]));
        active = false;
        last = 0L;
    }

    private static double ms(long nanos) { return ((double) nanos) / 1000000.0d; }
}
