import com.fs.graphics.L;
import com.fs.starfarer.BrowserDeferredTextureQueue;

/** Property-off behavior must leave the stock predecode path intact. */
public final class VerifyEarlyImagePredecodeDisabledBehavior {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        System.clearProperty("starsector.browserEarlyImagePredecode");
        System.clearProperty("starsector.browserDeferredTextures");
        BrowserDeferredTextureQueue.queueEarlyImagePredecode("graphics/hud/off.png", 0);
        BrowserDeferredTextureQueue.startEarlyImagePredecode();
        require(L.PREDECODE.isEmpty(), "disabled early queue must be a no-op: " + L.PREDECODE);
        require(L.STARTS == 0, "disabled early start must be a no-op: " + L.STARTS);
        BrowserDeferredTextureQueue.queueImagePredecode("graphics/hud/off.png", 0);
        require(L.PREDECODE.size() == 1 && "graphics/hud/off.png".equals(L.PREDECODE.get(0)),
                "stock predecode must remain active when early mode is disabled: " + L.PREDECODE);
        require(BrowserDeferredTextureQueue.getEarlyPredecodeQueuedCount() == 0L, "disabled early queue counter changed");
        require(BrowserDeferredTextureQueue.getEarlyPredecodeStockSkipCount() == 0L, "disabled stock-skip counter changed");
        require(BrowserDeferredTextureQueue.getEarlyPredecodePendingCount() == 0, "disabled pending counter changed");
        System.out.println("VerifyEarlyImagePredecodeDisabledBehavior: OK stock fallback preserved");
    }
}
