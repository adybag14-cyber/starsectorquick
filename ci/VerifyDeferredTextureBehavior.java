import com.fs.graphics.oOoO;
import com.fs.graphics.L;
import com.fs.starfarer.BrowserDeferredTextureQueue;

public final class VerifyDeferredTextureBehavior {
    private VerifyDeferredTextureBehavior() {}
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        System.setProperty("starsector.browserDeferredTextures", "true");
        System.setProperty("starsector.browserGameplayPrewarm", "true");
        System.setProperty("starsector.browserGameplayPrewarmDelayMs", "0");
        System.setProperty("starsector.browserGameplayPrewarmPauseMs", "0");
        System.setProperty("starsector.browserEarlyImagePredecode", "true");

        BrowserDeferredTextureQueue.queueEarlyImagePredecode("graphics/portraits/deferred-early.png", 0);
        require(L.PREDECODE.isEmpty(), "deferred texture must not enter early predecode");
        BrowserDeferredTextureQueue.queueEarlyImagePredecode("graphics/ui/early.png", 0);
        BrowserDeferredTextureQueue.queueEarlyImagePredecode("graphics/ui/early.png", 0);
        require(L.PREDECODE.size() == 2, "early predecode multiplicity must be preserved: " + L.PREDECODE);
        BrowserDeferredTextureQueue.startEarlyImagePredecode();
        BrowserDeferredTextureQueue.startEarlyImagePredecode();
        require(L.STARTS == 1, "early worker should start once: " + L.STARTS);
        long queuedAtStart = BrowserDeferredTextureQueue.getEarlyPredecodeQueuedCount();
        BrowserDeferredTextureQueue.queueEarlyImagePredecode("graphics/ui/late-after-start.png", 0);
        require(BrowserDeferredTextureQueue.getEarlyPredecodeQueuedCount() == queuedAtStart,
                "early queue must freeze after worker start");
        require(!L.PREDECODE.contains("graphics/ui/late-after-start.png"),
                "post-start resource must remain on stock predecode path");
        BrowserDeferredTextureQueue.queueImagePredecode("graphics/ui/early.png", 0);
        BrowserDeferredTextureQueue.queueImagePredecode("graphics/ui/early.png", 0);
        require(L.PREDECODE.size() == 2, "stock pass must consume early multiplicity without requeue: " + L.PREDECODE);
        BrowserDeferredTextureQueue.queueImagePredecode("graphics/ui/early.png", 0);
        require(L.PREDECODE.size() == 3, "stock pass beyond early multiplicity must queue normally: " + L.PREDECODE);
        require(BrowserDeferredTextureQueue.getEarlyPredecodeQueuedCount() == 2L, "unexpected early queued count");
        require(BrowserDeferredTextureQueue.getEarlyPredecodeStockSkipCount() == 2L, "unexpected early stock skip count");
        require(BrowserDeferredTextureQueue.getEarlyPredecodePendingCount() == 0, "early multiplicity counters must drain");
        L.PREDECODE.clear();

        BrowserDeferredTextureQueue.queueImagePredecode("graphics/portraits/deferred.png", 0);
        BrowserDeferredTextureQueue.queueImagePredecode("graphics/illustrations/optional.jpg", 1);
        require(L.PREDECODE.isEmpty(), "normal/optional deferred textures should skip image predecode: " + L.PREDECODE);
        BrowserDeferredTextureQueue.queueImagePredecode("graphics/portraits/alpha.png", 2);
        BrowserDeferredTextureQueue.queueImagePredecode("graphics/hud/player_status_bg2.png", 0);
        require(L.PREDECODE.size() == 2
                && L.PREDECODE.get(0).equals("graphics/portraits/alpha.png")
                && L.PREDECODE.get(1).equals("graphics/hud/player_status_bg2.png"),
                "alpha-adder and nondeferred images must keep stock predecode: " + L.PREDECODE);

        BrowserDeferredTextureQueue.loadOrDefer("hud", "graphics/hud/player_status_bg2.png");
        require(oOoO.LOADS.size() == 1 && oOoO.LOADS.get(0).startsWith("hud="), "HUD should load immediately: " + oOoO.LOADS);

        BrowserDeferredTextureQueue.loadOrDefer("portrait", "graphics/portraits/test.png");
        require(oOoO.LOADS.size() == 1, "portrait should be deferred");
        require(BrowserDeferredTextureQueue.getPendingCount() == 1, "pending portrait count");
        require(BrowserDeferredTextureQueue.getDeferredCount() == 1L, "deferred counter");
        BrowserDeferredTextureQueue.ensureLoaded("portrait");
        require(oOoO.LOADS.size() == 2 && oOoO.LOADS.get(1).equals("portrait=graphics/portraits/test.png"), "lazy portrait load: " + oOoO.LOADS);
        require(BrowserDeferredTextureQueue.getPendingCount() == 0, "pending should clear after lazy load");
        require(BrowserDeferredTextureQueue.getLazyLoadCount() == 1L, "lazy counter");
        BrowserDeferredTextureQueue.ensureLoaded("portrait");
        require(oOoO.LOADS.size() == 2, "second lookup must not reload");

        java.lang.String deferredMiss = "graphics/damage/damage1.png";
        int loadsBeforeDeferredMiss = oOoO.LOADS.size();
        require(!oOoO.REGISTRY.containsKey(deferredMiss), "deferred miss must start absent from registry");
        BrowserDeferredTextureQueue.ensureLoaded(deferredMiss);
        require(oOoO.LOADS.size() == loadsBeforeDeferredMiss + 1
                && oOoO.LOADS.get(loadsBeforeDeferredMiss).equals(deferredMiss + "=" + deferredMiss),
                "concrete deferred registry miss must load exact path once: " + oOoO.LOADS);
        require(oOoO.REGISTRY.containsKey(deferredMiss), "direct miss must populate registry");
        require(BrowserDeferredTextureQueue.getDirectMissLoadCount() == 1L, "direct miss load counter");
        require(BrowserDeferredTextureQueue.getDirectMissLoadFailedCount() == 0L, "direct miss failure counter");
        BrowserDeferredTextureQueue.ensureLoaded(deferredMiss);
        require(oOoO.LOADS.size() == loadsBeforeDeferredMiss + 1, "resolved deferred miss must not reload");
        BrowserDeferredTextureQueue.ensureLoaded("deferred-symbolic-id");
        require(oOoO.LOADS.size() == loadsBeforeDeferredMiss + 1, "non-path key must not infer a file load");

        int loadsBeforeDuplicate = oOoO.LOADS.size();
        BrowserDeferredTextureQueue.loadOrDefer("dup", "graphics/illustrations/first.jpg");
        BrowserDeferredTextureQueue.loadOrDefer("dup", "graphics/illustrations/second.jpg");
        require(oOoO.LOADS.get(loadsBeforeDuplicate).equals("dup=graphics/illustrations/first.jpg"), "duplicate must materialize first source first: " + oOoO.LOADS);
        require(oOoO.LOADS.get(loadsBeforeDuplicate + 1).equals("dup=graphics/illustrations/second.jpg"), "duplicate second source ordering: " + oOoO.LOADS);
        require(BrowserDeferredTextureQueue.getPendingCount() == 0, "different-path duplicate must not remain deferred");

        int predecodeBeforeWarm = L.PREDECODE.size();
        int eagerBefore = oOoO.LOADS.size();
        BrowserDeferredTextureQueue.loadOrDefer("ship", "graphics/ships/lasher/lasher_base.png");
        BrowserDeferredTextureQueue.loadOrDefer("weapon", "graphics/weapons/energy/beamfringe.png");
        require(oOoO.LOADS.size() == eagerBefore + 2, "ship/weapon textures must be eager for combat correctness: " + oOoO.LOADS);
        BrowserDeferredTextureQueue.loadOrDefer("skill", "graphics/icons/skills/elite_combat.png");
        BrowserDeferredTextureQueue.loadOrDefer("tactical", "graphics/icons/tactical/assault.png");
        BrowserDeferredTextureQueue.loadOrDefer("planet", "graphics/planets/terran.jpg");
        BrowserDeferredTextureQueue.loadOrDefer("combat", "graphics/damage/damage2.png");
        BrowserDeferredTextureQueue.startGameplayPrewarm();
        long deadline = System.currentTimeMillis() + 2000L;
        while (!BrowserDeferredTextureQueue.isGameplayPrewarmDone() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5L);
        }
        require(BrowserDeferredTextureQueue.isGameplayPrewarmDone(), "gameplay prewarm did not complete");
        require(BrowserDeferredTextureQueue.getGameplayPredecodeCount() == 4L,
                "expected UI/combat/world predecode count=4 actual=" + BrowserDeferredTextureQueue.getGameplayPredecodeCount());
        require(BrowserDeferredTextureQueue.getGameplayPredecodeFailedCount() == 0L, "unexpected predecode failures");
        require(BrowserDeferredTextureQueue.getGameplayPrewarmPendingCount() == 0, "prewarm queues should drain");
        require(L.PREDECODE.size() == predecodeBeforeWarm + 4, "unexpected background predecode list=" + L.PREDECODE);
        require(L.PREDECODE.contains("graphics/icons/skills/elite_combat.png"), "skill missing from prewarm");
        require(L.PREDECODE.contains("graphics/icons/tactical/assault.png"), "tactical icon missing from prewarm");
        require(L.PREDECODE.contains("graphics/planets/terran.jpg"), "planet missing from prewarm");
        require(L.PREDECODE.contains("graphics/damage/damage2.png"), "combat effect missing from prewarm");
        require(!L.PREDECODE.contains("graphics/ships/lasher/lasher_base.png"), "eager ship must not enter deferred prewarm");
        require(!L.PREDECODE.contains("graphics/weapons/energy/beamfringe.png"), "eager weapon must not enter deferred prewarm");

        System.out.println("VerifyDeferredTextureBehavior: OK loads=" + oOoO.LOADS
                + " predecoded=" + BrowserDeferredTextureQueue.getGameplayPredecodeCount());
    }
}
