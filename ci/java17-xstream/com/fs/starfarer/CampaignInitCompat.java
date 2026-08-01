package com.fs.starfarer;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/** Null-safe accessors for launcher/spec objects that may be absent in direct browser boot. */
public final class CampaignInitCompat {
    private static boolean sectorConfigNullLogged;
    private static boolean sectorConfigFailureLogged;

    private CampaignInitCompat() {}

    /**
     * CampaignGameManager normally receives the built-in sectorConfig spec from the desktop
     * resource-loader path and immediately calls its obfuscated `super()` list accessor.
     * CheerpJ direct launch can legitimately have no sectorConfig entry. The built-in sector
     * generator has already run at this point, so an absent optional generator list is an empty
     * list rather than a reason to abort the entire new game.
     */
    public static List getAdditionalSectorGenerators(Object sectorConfig) {
        if (sectorConfig == null) {
            if (!sectorConfigNullLogged) {
                sectorConfigNullLogged = true;
                System.out.println(
                        "Fixer: sectorConfig spec is absent after the primary sector generator; continuing with no additional sector generators.");
            }
            return Collections.emptyList();
        }
        try {
            Method accessor = findNoArgMethod(sectorConfig.getClass(), "super");
            if (accessor == null) {
                if (!sectorConfigFailureLogged) {
                    sectorConfigFailureLogged = true;
                    System.out.println(
                            "Fixer: sectorConfig additional-generator accessor is unavailable; continuing with no additional sector generators.");
                }
                return Collections.emptyList();
            }
            accessor.setAccessible(true);
            Object value = accessor.invoke(sectorConfig);
            if (value instanceof List) {
                return (List) value;
            }
            if (!sectorConfigFailureLogged) {
                sectorConfigFailureLogged = true;
                System.out.println(
                        "Fixer: sectorConfig additional-generator accessor returned no list; continuing with no additional sector generators.");
            }
            return Collections.emptyList();
        } catch (Throwable t) {
            if (!sectorConfigFailureLogged) {
                sectorConfigFailureLogged = true;
                System.out.println(
                        "Fixer: sectorConfig additional-generator lookup failed; continuing with no additional sector generators: "
                                + t.getClass().getName()
                                + (t.getMessage() == null ? "" : ": " + t.getMessage()));
            }
            return Collections.emptyList();
        }
    }

    private static Method findNoArgMethod(Class<?> type, java.lang.String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
            current = current.getSuperclass();
        }
        try {
            return type.getMethod(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
