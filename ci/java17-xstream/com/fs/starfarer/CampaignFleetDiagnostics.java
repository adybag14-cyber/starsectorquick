package com.fs.starfarer;

/** Optional constructor tracing. The property is read once when this class loads. */
public final class CampaignFleetDiagnostics {
    // Opt in at JVM startup with -Dstarsector.browserCampaignCreateDiagnostics=true.
    // Ordinary campaign play avoids 48 PrintStream/native output calls per fleet.
    private static final boolean ENABLED =
            Boolean.getBoolean("starsector.browserCampaignCreateDiagnostics");

    private CampaignFleetDiagnostics() {}

    public static void log(java.lang.String message) {
        if (ENABLED) {
            System.out.println(message);
        }
    }
}
