package com.fs.starfarer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGenProgress;
import com.fs.starfarer.api.campaign.SectorProcGenPlugin;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/** Null-safe/lightweight accessors for direct browser campaign boot. */
public final class CampaignInitCompat {
    private static boolean sectorConfigNullLogged;
    private static boolean sectorConfigFailureLogged;
    private static boolean procGenSkippedLogged;
    private static boolean procGenDelegatedLogged;

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
     * Browser campaign creation must preserve Starsector's real procedural sector.
     * Older quick-start builds replaced this pass with a no-op to shorten bootstrap,
     * which left the player in a lightweight/partial map. Keep the wrapper for browser
     * diagnostics, but delegate to the stock plugin by default. A deliberately partial
     * world is now available only through an explicit diagnostic property.
     */
    public static void generateSectorProcGen(
            SectorProcGenPlugin plugin,
            CharacterCreationData data,
            SectorGenProgress progress) {
        boolean skip = Boolean.parseBoolean(
                System.getProperty("starsector.browserSkipOuterSectorProcGen", "false"));
        if (skip) {
            if (!procGenSkippedLogged) {
                procGenSkippedLogged = true;
                System.out.println(
                        "Fixer: explicit diagnostic override is skipping outer-sector procedural generation.");
            }
            return;
        }
        if (plugin == null) {
            throw new IllegalStateException(
                    "SectorProcGenPlugin is null while full browser campaign generation is required");
        }
        if (!procGenDelegatedLogged) {
            procGenDelegatedLogged = true;
            System.out.println(
                    "Fixer: full campaign map enabled; delegating to stock outer-sector procedural generation.");
        }
        plugin.generate(data, progress);
    }

    /** Observational diagnostic only: never creates or repairs markets/entities. */
    public static void logEconomyState() {
        try {
            SectorAPI sector = Global.getSector();
            if (sector == null) {
                System.out.println("CampaignEconomyDiag: sector=null");
                return;
            }
            EconomyAPI economy = sector.getEconomy();
            if (economy == null) {
                System.out.println("CampaignEconomyDiag: economy=null");
                return;
            }
            List<MarketAPI> markets = economy.getMarketsCopy();
            int marketCount = markets == null ? 0 : markets.size();
            int withPrimaryEntity = 0;
            int withContainingLocation = 0;
            if (markets != null) {
                for (MarketAPI market : markets) {
                    if (market == null) {
                        continue;
                    }
                    if (market.getPrimaryEntity() != null) {
                        withPrimaryEntity++;
                    }
                    if (market.getContainingLocation() != null) {
                        withContainingLocation++;
                    }
                }
            }
            int starSystems = sector.getStarSystems() == null ? 0 : sector.getStarSystems().size();
            System.out.println(
                    "CampaignEconomyDiag: markets="
                            + marketCount
                            + " primaryEntities="
                            + withPrimaryEntity
                            + " containingLocations="
                            + withContainingLocation
                            + " starSystems="
                            + starSystems);
        } catch (Throwable t) {
            System.out.println(
                    "CampaignEconomyDiag: probe-error="
                            + t.getClass().getName()
                            + (t.getMessage() == null ? "" : ": " + t.getMessage()));
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
