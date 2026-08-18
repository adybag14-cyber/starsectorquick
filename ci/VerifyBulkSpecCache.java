import com.fs.starfarer.loading.BrowserSpecCache;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
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
        String system = BrowserSpecCache.getRaw("data/shipsystems/acausaldisruptor.system");
        String skill = BrowserSpecCache.getRaw("data/characters/skills/advanced_countermeasures.skill");
        if (lasher == null || !lasher.contains("\"variantId\": \"lasher_Standard\"")) {
            throw new AssertionError("lasher variant was not reconstructed from cache");
        }
        if (ziggurat == null || !ziggurat.contains("\"variantId\": \"ziggurat_Experimental\"")) {
            throw new AssertionError("ziggurat variant was not reconstructed from cache");
        }
        if (!ziggurat.contains("\"fluxbreakers\",")) {
            throw new AssertionError("non-standard trailing-comma source text was not preserved");
        }
        if (system == null || !system.contains("\"id\":\"acausaldisruptor\"")
                || !system.contains("# handled in the script instead of here")) {
            throw new AssertionError("ship-system source text was not reconstructed from cache");
        }
        if (skill == null || !skill.contains("\"id\":\"advanced_countermeasures\"")
                || !skill.contains("AdvancedCountermeasures$Level3B")) {
            throw new AssertionError("skill source text was not reconstructed from cache");
        }

        JSONObject payload = new JSONObject(new String(
                Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8));
        JSONObject packed = payload.getJSONObject("files");
        int parsedFiles = 0;
        Iterator<?> packedKeys = packed.keys();
        while (packedKeys.hasNext()) {
            String path = String.valueOf(packedKeys.next());
            String raw = packed.getString(path);
            JSONObject reference = parseLikeStock(path, raw);
            JSONObject fast = BrowserSpecCache.parseRaw(path, raw);
            if (!reference.toString().equals(fast.toString())) {
                throw new AssertionError("fast JSON parser mismatch path=" + path);
            }
            parsedFiles++;
        }
        if (parsedFiles != payload.getInt("fileCount")
                || BrowserSpecCache.getFastJsonParseCount() != parsedFiles) {
            throw new AssertionError(
                    "fast JSON parser count mismatch parsed=" + parsedFiles
                            + " counter=" + BrowserSpecCache.getFastJsonParseCount());
        }
        String malformed = "{\"broken\":";
        String referenceError = parseError("bad.variant", malformed, true);
        String fastError = parseError("bad.variant", malformed, false);
        if (!referenceError.equals(fastError)) {
            throw new AssertionError(
                    "fast JSON parser exception mismatch reference=" + referenceError
                            + " fast=" + fastError);
        }

        JSONObject extensions = payload.getJSONObject("extensions");
        int expectedVariants = extensions.getInt(".variant");
        int expectedSystems = extensions.getInt(".system");
        int expectedSkills = extensions.getInt(".skill");
        if (expectedSystems != 63 || expectedSkills != 70) {
            throw new AssertionError(
                    "unexpected stock system/skill counts systems=" + expectedSystems + " skills=" + expectedSkills);
        }
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
                || BrowserSpecCache.getHitCount() != 4L
                || BrowserSpecCache.getVariantPathCount() != expectedVariants) {
            throw new AssertionError(
                    "unexpected cache state files=" + BrowserSpecCache.getFileCount()
                            + " hits=" + BrowserSpecCache.getHitCount()
                            + " variants=" + BrowserSpecCache.getVariantPathCount()
                            + " systems=" + expectedSystems
                            + " skills=" + expectedSkills);
        }
        System.out.println(
                "VerifyBulkSpecCache: OK files=" + BrowserSpecCache.getFileCount()
                        + " variants=" + BrowserSpecCache.getVariantPathCount()
                        + " systems=" + expectedSystems
                        + " skills=" + expectedSkills
                        + " hits=" + BrowserSpecCache.getHitCount()
                        + " loadMs=" + BrowserSpecCache.getLoadMs());
    }
    private static JSONObject parseLikeStock(String path, String raw) throws org.json.JSONException {
        try {
            StringBuffer filtered = new StringBuffer();
            boolean comment = false;
            boolean quoted = false;
            for (int i = 0; i < raw.length(); i++) {
                char ch = raw.charAt(i);
                if (ch == '"') quoted = !quoted;
                if (ch == '\n' || ch == '\r') {
                    comment = false;
                    quoted = false;
                    if (ch == '\n') filtered.append('\n');
                } else if (ch == '#' && !quoted) {
                    comment = true;
                } else if (!comment) {
                    filtered.append(ch);
                }
            }
            return new JSONObject(filtered.toString());
        } catch (org.json.JSONException error) {
            throw new org.json.JSONException(path + "\n" + error.getMessage());
        }
    }

    private static String parseError(String path, String raw, boolean reference) {
        try {
            if (reference) parseLikeStock(path, raw);
            else BrowserSpecCache.parseRaw(path, raw);
            return "<no-error>";
        } catch (org.json.JSONException error) {
            return error.getMessage();
        }
    }

}
