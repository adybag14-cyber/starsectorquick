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
