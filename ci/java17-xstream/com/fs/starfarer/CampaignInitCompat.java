package com.fs.starfarer;

import com.fs.starfarer.api.campaign.SectorGenProgress;
import com.fs.starfarer.api.campaign.SectorProcGenPlugin;
import com.fs.starfarer.api.characters.CharacterCreationData;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/** Null-safe/lightweight accessors for direct browser campaign boot. */
public final class CampaignInitCompat {
    private static boolean sectorConfigNullLogged;
    private static boolean sectorConfigFailureLogged;
    private static boolean procGenSkippedLogged;

    private CampaignInitCompat() {}

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
            return value instanceof List ? (List) value : Collections.emptyList();
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

    /**
     * The desktop procedural sector pass constructs many stars, planets and nebulae.
     * On CheerpJ that pass can monopolize the VM for minutes and uses terrain/planet
     * constructors that are not yet browser-safe. The lightweight browser SectorGen
     * already establishes the campaign runtime; defer optional procgen so creation can
     * return to CampaignState and the game can render/respond.
     */
    public static void generateSectorProcGen(
            SectorProcGenPlugin plugin,
            CharacterCreationData data,
            SectorGenProgress progress) {
        if (!procGenSkippedLogged) {
            procGenSkippedLogged = true;
            System.out.println(
                    "Fixer: skipping desktop procedural sector generation during CheerpJ bootstrap; entering lightweight campaign world.");
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
