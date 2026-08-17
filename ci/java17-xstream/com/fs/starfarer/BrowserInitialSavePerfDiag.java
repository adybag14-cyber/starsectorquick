package com.fs.starfarer;

/** Timing-only browser diagnostic for the synchronous campaign save path. */
public final class BrowserInitialSavePerfDiag {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserGameplayProbe";
    private static final java.lang.String SAVE_DIR_MARKER = "/files/browser-last-save-dir.txt";
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
        exportSaveDirectoryMarker();
    }

    private static void exportSaveDirectoryMarker() {
        try {
            final com.fs.starfarer.campaign.CampaignEngine engine =
                    com.fs.starfarer.campaign.CampaignEngine.getInstance();
            final java.lang.String saveDir = engine == null ? null : engine.getSaveDirName();
            if (saveDir == null || saveDir.trim().length() == 0) {
                System.out.println("BrowserInitialSavePerfDiag: save-dir marker skipped; no saveDirName");
                return;
            }
            final java.io.OutputStream out = new java.io.FileOutputStream(SAVE_DIR_MARKER);
            try {
                out.write(saveDir.getBytes("UTF-8"));
                out.flush();
            } finally {
                out.close();
            }
            System.out.println("BrowserInitialSavePerfDiag: save-dir=" + saveDir
                    + " marker=" + SAVE_DIR_MARKER);
        } catch (final Throwable t) {
            System.out.println("BrowserInitialSavePerfDiag: save-dir marker failed: "
                    + t.getClass().getName() + ": " + t.getMessage());
        }
    }
}
