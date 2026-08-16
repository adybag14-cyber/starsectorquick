package com.fs.starfarer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI.AbilitySlotAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI.AbilitySlotsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.tutorial.CampaignTutorialScript;

/** Starts the stock Galatia campaign tutorial for the public browser quick-start. */
public final class BrowserTutorialCompat {
    public static final String ENABLE_PROPERTY = "starsector.browserTutorial";
    private static final String INSTALLED_MEMORY_KEY = "$browserTutorialInstalled";

    private BrowserTutorialCompat() {}

    /**
     * Runs at the same lifecycle point where CharacterCreationData scripts run in
     * CampaignGameManager.create(): after the real player fleet exists and before
     * the new campaign is handed to normal play.
     *
     * The browser direct path does not pass through NGCAddStandardStartingScript,
     * so its CharacterCreationData currently has an empty scripts list. Recreate
     * only the tutorial-specific branch of that stock starting script here. Cargo
     * balancing remains owned by Fixer and mature/deep CI keeps this disabled.
     */
    public static void startTutorialIfRequested() {
        if (!Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))) {
            return;
        }

        try {
            SectorAPI sector = Global.getSector();
            if (sector == null) {
                throw new IllegalStateException("sector is null");
            }
            MemoryAPI memory = sector.getMemoryWithoutUpdate();
            if (memory != null && memory.getBoolean(INSTALLED_MEMORY_KEY)) {
                return;
            }

            CampaignFleetAPI fleet = sector.getPlayerFleet();
            if (fleet == null) {
                throw new IllegalStateException("player fleet is null");
            }
            StarSystemAPI galatia = sector.getStarSystem("galatia");
            if (galatia == null) {
                throw new IllegalStateException("Galatia star system is missing");
            }

            // NGCAddStandardStartingScript sets this before normal tutorial play.
            if (memory != null) {
                memory.set(CampaignTutorialScript.USE_TUTORIAL_RESPAWN, true);
            }

            // Match the stock tutorial branch: mature campaign abilities are
            // cleared and the tutorial script grants/progresses them normally.
            fleet.clearAbilities();
            AbilitySlotsAPI slots = sector.getUIData().getAbilitySlotsAPI();
            for (int bar = 0; bar < 5; bar++) {
                slots.setCurrBarIndex(bar);
                for (int slotIndex = 0; slotIndex < 10; slotIndex++) {
                    AbilitySlotAPI slot = slots.getCurrSlotsCopy().get(slotIndex);
                    slot.setAbilityId(null);
                }
            }
            slots.setCurrBarIndex(0);
            fleet.clearFloatingText();
            fleet.setTransponderOn(false);

            galatia.addScript(new CampaignTutorialScript(galatia));
            if (memory != null) {
                memory.set(INSTALLED_MEMORY_KEY, true);
            }
            System.out.println(
                    "BrowserTutorialCompat: started stock Galatia tutorial system="
                            + galatia.getName()
                            + " fleet="
                            + fleet.getName());
        } catch (Throwable t) {
            System.out.println(
                    "BrowserTutorialCompat: failed to start requested tutorial: "
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
