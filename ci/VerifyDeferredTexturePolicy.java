import com.fs.starfarer.BrowserDeferredTextureQueue;

public final class VerifyDeferredTexturePolicy {
    private VerifyDeferredTexturePolicy() {}
    private static void expect(String path, boolean expected) {
        boolean actual = BrowserDeferredTextureQueue.shouldDeferPath(path);
        if (actual != expected) throw new AssertionError(path + " actual=" + actual + " expected=" + expected);
    }
    public static void main(String[] args) {
        expect("graphics/illustrations/urban02.jpg", true);
        expect("graphics\\portraits\\godiva.jpg", true);
        expect("/graphics/portraits/portrait14.png", true);
        expect("graphics/hullmods/automated_repair_unit.png", true);
        expect("graphics/icons/markets/commodity_exports.png", true);
        expect("graphics/icons/cargo/supplies.png", true);
        expect("graphics/icons/intel/generic_intel.png", true);
        expect("graphics/icons/skills/elite_combat.png", true);
        expect("graphics/icons/missions/bounty.png", true);
        expect("graphics/icons/hullsys/fortress_shield.png", true);
        expect("graphics/icons/industry/heavyindustry.png", true);
        expect("graphics/icons/codex/ship.png", true);
        expect("graphics/icons/reports/report.png", true);
        expect("graphics/factions/hegemony.png", true);
        expect("graphics/planets/terran.jpg", true);
        expect("graphics/stations/station_mining00.png", true);
        expect("graphics/warroom/escort.png", true);
        expect("graphics/weapons/energy/beamfringe.png", true);
        expect("graphics/damage/damage1.png", true);
        expect("graphics/icons/tactical/assault.png", true);
        expect("graphics/debris/debris1.png", true);
        expect("graphics/missiles/harpoon/harpoon.png", true);
        expect("graphics/asteroids/asteroid1.png", true);
        expect("data/missions/forlornhope/icon.jpg", true);
        expect("graphics/ui/bgs/panel01_top_left.png", false);
        expect("graphics/hud/player_status_bg2.png", false);
        expect("graphics/ships/hound/hound_base.png", true);
        expect("graphics/fx/slipstream_layer1.png", false);
        expect("graphics/backgrounds/hyperspace_bg_cool.jpg", false);
        expect("graphics/terrain/deep_hyperspace2.png", false);
        expect("graphics/icons/campaign/burn.png", false);
        expect("graphics/icons/abilities/emergency_burn.png", false);
        expect("graphics/hud/player_status_bg2.png", false);
        expect("graphics/ships/lasher/lasher_base.png", true);
        expect("graphics/fonts/orbitron20aa.fnt", false);
        System.out.println("VerifyDeferredTexturePolicy: OK");
    }
}
