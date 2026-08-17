package com.fs.starfarer;

/** Diagnostic-only boundary markers for NascentGravityWell.readResolve(). */
public final class BrowserNascentGravityWellLoadDiag {
    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("starsector.browserXstreamLoadDiag", "false"));

    private BrowserNascentGravityWellLoadDiag() {}

    public static void mark(java.lang.String phase) {
        if (ENABLED) {
            System.out.println("BrowserNascentGravityWellLoadDiag: " + phase
                    + " t=" + System.currentTimeMillis());
        }
    }
}
