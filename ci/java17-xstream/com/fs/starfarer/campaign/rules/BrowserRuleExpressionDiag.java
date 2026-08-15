package com.fs.starfarer.campaign.rules;

/** Diagnostic-only aggregate timer for Rules expression construction/tokenization. */
public final class BrowserRuleExpressionDiag {
    private static boolean active;
    private static long expressionStart;
    private static long tokenStart;
    private static long expressionNanos;
    private static long tokenizeNanos;
    private static int expressions;
    private static int tokenizations;

    private BrowserRuleExpressionDiag() {}

    public static void beginRules() {
        active = true;
        expressionStart = 0L;
        tokenStart = 0L;
        expressionNanos = 0L;
        tokenizeNanos = 0L;
        expressions = 0;
        tokenizations = 0;
    }

    public static void expressionStart() {
        if (!active) return;
        expressionStart = System.nanoTime();
    }

    public static void tokenizeStart() {
        if (!active) return;
        tokenStart = System.nanoTime();
    }

    public static void tokenizeEnd() {
        if (!active || tokenStart == 0L) return;
        tokenizeNanos += System.nanoTime() - tokenStart;
        tokenStart = 0L;
        tokenizations++;
    }

    public static void expressionEnd() {
        if (!active || expressionStart == 0L) return;
        expressionNanos += System.nanoTime() - expressionStart;
        expressionStart = 0L;
        expressions++;
    }

    public static void finishRules() {
        if (!active) return;
        double totalMs = expressionNanos / 1000000.0d;
        double tokenizeMs = tokenizeNanos / 1000000.0d;
        double nonTokenMs = totalMs - tokenizeMs;
        double pct = expressionNanos == 0L ? 0.0d : (100.0d * tokenizeNanos / expressionNanos);
        System.out.println("BrowserRuleExpressionDiag: expressions=" + expressions
                + " tokenizations=" + tokenizations
                + " totalMs=" + totalMs
                + " tokenizeMs=" + tokenizeMs
                + " nonTokenMs=" + nonTokenMs
                + " tokenizePct=" + pct);
        active = false;
    }
}
