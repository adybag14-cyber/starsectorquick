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
        expect("graphics/icons/campaign/burn.png", false);
        expect("graphics/icons/abilities/emergency_burn.png", false);
        expect("graphics/hud/player_status_bg2.png", false);
        expect("graphics/ships/lasher/lasher_base.png", false);
        expect("graphics/fonts/orbitron20aa.fnt", false);
        System.out.println("VerifyDeferredTexturePolicy: OK");
    }
}
