import com.fs.starfarer.loading.BrowserSpecCache;

/** Smoke the generated bulk cache without defining obfuscated LoadingUtils on HotSpot. */
public final class VerifyBulkSpecCache {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: VerifyBulkSpecCache cache.json");
        System.setProperty("starsector.browserBulkSpecCache", "true");
        System.setProperty("starsector.browserBulkSpecCacheSkipModCheck", "true");
        System.setProperty("starsector.browserSpecCachePath", args[0]);

        String lasher = BrowserSpecCache.getRaw("data/variants/lasher_Standard.variant");
        String ziggurat = BrowserSpecCache.getRaw("data/variants/ziggurat_HF.variant");
        if (lasher == null || !lasher.contains("\"variantId\": \"lasher_Standard\"")) {
            throw new AssertionError("lasher variant was not reconstructed from cache");
        }
        if (ziggurat == null || !ziggurat.contains("\"variantId\": \"ziggurat_Experimental\"")) {
            throw new AssertionError("ziggurat variant was not reconstructed from cache");
        }
        if (!ziggurat.contains("\"fluxbreakers\",")) {
            throw new AssertionError("non-standard trailing-comma source text was not preserved");
        }
        if (BrowserSpecCache.getFileCount() < 1000 || BrowserSpecCache.getHitCount() != 2L) {
            throw new AssertionError(
                    "unexpected cache state files=" + BrowserSpecCache.getFileCount()
                            + " hits=" + BrowserSpecCache.getHitCount());
        }
        System.out.println(
                "VerifyBulkSpecCache: OK files=" + BrowserSpecCache.getFileCount()
                        + " hits=" + BrowserSpecCache.getHitCount()
                        + " loadMs=" + BrowserSpecCache.getLoadMs());
    }
}