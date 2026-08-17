package com.fs.starfarer;

import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;

/** Diagnostic-only boundary logging around selected browser XStream field writes. */
public final class BrowserLoggingSunUnsafeReflectionProvider extends SunUnsafeReflectionProvider {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserXstreamLoadDiag";

    @Override
    public void writeField(final java.lang.Object object,
                           final java.lang.String fieldName,
                           final java.lang.Object value,
                           final java.lang.Class definedIn) {
        final java.lang.String objectClass = object == null ? "<null>" : object.getClass().getName();
        final boolean campaignFirst = "com.fs.starfarer.campaign.CampaignEngine".equals(objectClass)
                && "isFastForwardIteration".equals(fieldName);
        final boolean marketPrimary = ("com.fs.starfarer.campaign.econ.Market".equals(objectClass)
                || "com.fs.starfarer.campaign.econ.PlanetConditionMarket".equals(objectClass))
                && "primaryEntity".equals(fieldName);
        final boolean interesting = Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))
                && (campaignFirst || marketPrimary);
        if (interesting) {
            System.out.println("BrowserXStreamLoadDiag: writeField-before object=" + objectClass
                    + " field=" + fieldName
                    + " valueClass=" + (value == null ? "<null>" : value.getClass().getName())
                    + " definedIn=" + (definedIn == null ? "<null>" : definedIn.getName()));
        }
        super.writeField(object, fieldName, value, definedIn);
        if (interesting) {
            System.out.println("BrowserXStreamLoadDiag: writeField-after object=" + objectClass
                    + " field=" + fieldName);
        }
    }
}
