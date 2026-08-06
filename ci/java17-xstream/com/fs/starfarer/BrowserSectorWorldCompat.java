package com.fs.starfarer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Narrow browser-world construction helpers used only by explicitly selected
 * diagnostics. These helpers create normal campaign entities; they never create
 * or register economy markets themselves.
 */
public final class BrowserSectorWorldCompat {
    private BrowserSectorWorldCompat() {}

    /**
     * Ensure the minimum stock entity graph needed for data/campaign/econ/corvus.json
     * to create the normal Asharu market: the Corvus star plus the `asharu` planet.
     * Economy.load() remains solely responsible for the market itself.
     */
    public static void ensureMinimalCorvusAsharuAnchor() {
        try {
            SectorAPI sector = Global.getSector();
            if (sector == null) {
                System.out.println("BrowserSectorAnchorDiag: sector=null");
                return;
            }

            StarSystemAPI system = sector.getStarSystem("Corvus");
            if (system == null) {
                system = sector.createStarSystem("Corvus");
                system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
                System.out.println("BrowserSectorAnchorDiag: created Corvus system shell");
            }

            PlanetAPI star = null;
            SectorEntityToken existingStar = system.getEntityById("corvus");
            if (existingStar instanceof PlanetAPI) {
                star = (PlanetAPI) existingStar;
            }
            if (star == null) {
                star = system.initStar(
                        "corvus",
                        "star_yellow",
                        775f,
                        500f,
                        10f,
                        1f,
                        3f);
                System.out.println("BrowserSectorAnchorDiag: created stock Corvus star");
            }

            SectorEntityToken existingAsharu = system.getEntityById("asharu");
            if (existingAsharu == null) {
                PlanetAPI asharu =
                        system.addPlanet(
                                "asharu",
                                star,
                                "Asharu",
                                "desert",
                                55f,
                                150f,
                                2800f,
                                100f);
                asharu.setCustomDescriptionId("planet_asharu");
                System.out.println("BrowserSectorAnchorDiag: created stock Asharu planet entity");
            } else {
                System.out.println("BrowserSectorAnchorDiag: Asharu entity already present");
            }

            sector.setCurrentLocation(system);
            sector.setRespawnLocation(system);
            sector.getRespawnCoordinates().set(-2500f, -3500f);
            System.out.println(
                    "BrowserSectorAnchorDiag: ready entities corvus="
                            + (system.getEntityById("corvus") != null)
                            + " asharu="
                            + (system.getEntityById("asharu") != null));
        } catch (Throwable t) {
            System.out.println(
                    "BrowserSectorAnchorDiag: failed="
                            + t.getClass().getName()
                            + (t.getMessage() == null ? "" : ": " + t.getMessage()));
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }
}
