package com.fs.starfarer;

import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;

/** Diagnostic-only boundary logging around the first CampaignEngine field write. */
public final class BrowserLoggingSunUnsafeReflectionProvider extends SunUnsafeReflectionProvider {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserXstreamLoadDiag";

    @Override
    public void writeField(final java.lang.Object object,
                           final java.lang.String fieldName,
                           final java.lang.Object value,
                           final java.lang.Class definedIn) {
        final boolean interesting = Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))
                && object != null
                && "com.fs.starfarer.campaign.CampaignEngine".equals(object.getClass().getName())
                && "isFastForwardIteration".equals(fieldName);
        if (interesting) {
            System.out.println("BrowserXStreamLoadDiag: writeField-before field=" + fieldName
                    + " value=" + java.lang.String.valueOf(value)
                    + " definedIn=" + (definedIn == null ? "<null>" : definedIn.getName()));
        }
        super.writeField(object, fieldName, value, definedIn);
        if (interesting) {
            System.out.println("BrowserXStreamLoadDiag: writeField-after field=" + fieldName);
        }
    }
}
