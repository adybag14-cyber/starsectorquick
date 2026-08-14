import com.fs.starfarer.loading.BrowserSpecCache;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;

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

        JSONObject payload = new JSONObject(new String(
                Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8));
        int expectedVariants = payload.getJSONObject("extensions").getInt(".variant");
        List<String> rootOnly = Arrays.asList("data/variants/sentinel-root.variant");
        List<String> expanded = BrowserSpecCache.expandVariantPaths(rootOnly);
        if (expanded == rootOnly || expanded.size() != expectedVariants) {
            throw new AssertionError(
                    "variant manifest mismatch expected=" + expectedVariants + " actual=" + expanded.size());
        }
        if (!expanded.contains("data/variants/lasher_Standard.variant")
                || !expanded.contains("data/variants/ziggurat_HF.variant")
                || expanded.contains("data/variants/sentinel-root.variant")) {
            throw new AssertionError("variant manifest contents are not the generated stock cache contents");
        }
        List<String> directories = Arrays.asList("data/variants/lasher", "data/variants/aurora");
        if (!BrowserSpecCache.filterVariantDirectories(directories).isEmpty()) {
            throw new AssertionError("cached variant discovery did not suppress redundant child-directory probes");
        }

        // Disabling the browser cache must restore Starsector's original discovery inputs exactly.
        System.setProperty("starsector.browserBulkSpecCache", "false");
        if (BrowserSpecCache.expandVariantPaths(rootOnly) != rootOnly
                || BrowserSpecCache.filterVariantDirectories(directories) != directories) {
            throw new AssertionError("disabled variant cache did not preserve original discovery lists");
        }
        System.setProperty("starsector.browserBulkSpecCache", "true");

        if (BrowserSpecCache.getFileCount() < 1000
                || BrowserSpecCache.getHitCount() != 2L
                || BrowserSpecCache.getVariantPathCount() != expectedVariants) {
            throw new AssertionError(
                    "unexpected cache state files=" + BrowserSpecCache.getFileCount()
                            + " hits=" + BrowserSpecCache.getHitCount()
                            + " variants=" + BrowserSpecCache.getVariantPathCount());
        }
        System.out.println(
                "VerifyBulkSpecCache: OK files=" + BrowserSpecCache.getFileCount()
                        + " variants=" + BrowserSpecCache.getVariantPathCount()
                        + " hits=" + BrowserSpecCache.getHitCount()
                        + " loadMs=" + BrowserSpecCache.getLoadMs());
    }
}
