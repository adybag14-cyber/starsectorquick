import com.fs.graphics.oOoO;
import com.fs.starfarer.BrowserDeferredTextureQueue;

public final class VerifyDeferredTextureBehavior {
    private VerifyDeferredTextureBehavior() {}
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        System.setProperty("starsector.browserDeferredTextures", "true");

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

        BrowserDeferredTextureQueue.loadOrDefer("dup", "graphics/illustrations/first.jpg");
        BrowserDeferredTextureQueue.loadOrDefer("dup", "graphics/illustrations/second.jpg");
        require(oOoO.LOADS.get(2).equals("dup=graphics/illustrations/first.jpg"), "duplicate must materialize first source first: " + oOoO.LOADS);
        require(oOoO.LOADS.get(3).equals("dup=graphics/illustrations/second.jpg"), "duplicate second source ordering: " + oOoO.LOADS);
        require(BrowserDeferredTextureQueue.getPendingCount() == 0, "different-path duplicate must not remain deferred");

        System.out.println("VerifyDeferredTextureBehavior: OK loads=" + oOoO.LOADS);
    }
}
