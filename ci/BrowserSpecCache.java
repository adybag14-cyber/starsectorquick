package com.fs.starfarer.loading;

import com.fs.starfarer.launcher.ModManager;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;

/** Browser-only read-through cache for stock small spec files. */
public final class BrowserSpecCache {
    private static final String ENABLE_PROPERTY = "starsector.browserBulkSpecCache";
    private static final String PATH_PROPERTY = "starsector.browserSpecCachePath";
    private static final String SKIP_MOD_CHECK_PROPERTY = "starsector.browserBulkSpecCacheSkipModCheck";
    private static volatile Map<String, String> files;
    private static volatile List<String> variantPaths;
    private static volatile boolean disabled;
    private static final AtomicLong HITS = new AtomicLong();
    private static final AtomicLong MISSES = new AtomicLong();
    private static final AtomicBoolean FIRST_HIT_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean SYSTEM_HIT_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean SKILL_HIT_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean VARIANT_DISCOVERY_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean DIRECT_VARIANT_MANIFEST_LOGGED = new AtomicBoolean();
    private static volatile long loadMs;

    private BrowserSpecCache() {}

    public static String getRaw(String path) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || disabled || path == null || !isEligible(path)) {
            return null;
        }
        try {
            Map<String, String> current = files;
            if (current == null) {
                current = ensureLoaded();
            }
            if (current == null || current.isEmpty()) return null;
            String normalized = normalize(path);
            String value = current.get(normalized);
            if (value != null) {
                HITS.incrementAndGet();
                if (FIRST_HIT_LOGGED.compareAndSet(false, true)) {
                    System.out.println("BrowserSpecCache: first-hit path=" + normalized);
                }
                if (normalized.endsWith(".system") && SYSTEM_HIT_LOGGED.compareAndSet(false, true)) {
                    System.out.println("BrowserSpecCache: first-system-hit path=" + normalized);
                }
                if (normalized.endsWith(".skill") && SKILL_HIT_LOGGED.compareAndSet(false, true)) {
                    System.out.println("BrowserSpecCache: first-skill-hit path=" + normalized);
                }
                return value;
            }
            MISSES.incrementAndGet();
            return null;
        } catch (Throwable error) {
            disabled = true;
            System.out.println("BrowserSpecCache: disabled after failure: " + describe(error));
            return null;
        }
    }

    /**
     * Return the complete generated stock variant manifest before SpecStore performs
     * any synchronous directory discovery. Null means the caller must execute the
     * untouched stock discovery path. Recheck enabled mods even when the cache was
     * loaded earlier so a modded/changed session never takes the stock-only shortcut.
     */
    public static List<String> directVariantPathsOrNull() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || disabled) return null;
        try {
            if (!Boolean.getBoolean(SKIP_MOD_CHECK_PROPERTY)) {
                List<ModManager.ModSpec> enabled = ModManager.getInstance().getEnabledMods();
                if (enabled != null && !enabled.isEmpty()) return null;
            }
            if (files == null) ensureLoaded();
            List<String> cached = variantPaths;
            if (cached == null || cached.isEmpty()) return null;
            if (DIRECT_VARIANT_MANIFEST_LOGGED.compareAndSet(false, true)) {
                System.out.println("BrowserSpecCache: direct-variant-manifest files=" + cached.size());
            }
            return new ArrayList<String>(cached);
        } catch (Throwable error) {
            disabled = true;
            System.out.println("BrowserSpecCache: direct variant manifest disabled after failure: " + describe(error));
            return null;
        }
    }

    /**
     * Replace SpecStore's root-only variant discovery result with the complete
     * stock variant manifest already present in the browser spec cache. When the
     * cache is unavailable (including external-mod sessions), return the original
     * list unchanged so Starsector's normal directory discovery remains exact.
     */
    public static List<String> expandVariantPaths(List<String> rootPaths) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || disabled) return rootPaths;
        try {
            if (files == null) ensureLoaded();
            List<String> cached = variantPaths;
            if (cached == null || cached.isEmpty()) return rootPaths;
            if (VARIANT_DISCOVERY_LOGGED.compareAndSet(false, true)) {
                System.out.println("BrowserSpecCache: variant-discovery files=" + cached.size());
            }
            return new ArrayList<String>(cached);
        } catch (Throwable error) {
            disabled = true;
            System.out.println("BrowserSpecCache: variant discovery disabled after failure: " + describe(error));
            return rootPaths;
        }
    }

    /**
     * Once expandVariantPaths() has supplied every stock .variant path, the
     * following SpecStore child-directory walk would only repeat synchronous
     * browser directory probes. Suppress that walk only while the stock cache is
     * active; otherwise preserve the exact original directory list.
     */
    public static List<String> filterVariantDirectories(List<String> directories) {
        List<String> cached = variantPaths;
        if (Boolean.getBoolean(ENABLE_PROPERTY) && !disabled && cached != null && !cached.isEmpty()) {
            return Collections.emptyList();
        }
        return directories;
    }

    public static int getVariantPathCount() {
        List<String> cached = variantPaths;
        return cached == null ? 0 : cached.size();
    }

    public static long getHitCount() {
        return HITS.get();
    }

    public static long getMissCount() {
        return MISSES.get();
    }

    public static long getLoadMs() {
        return loadMs;
    }

    public static int getFileCount() {
        Map<String, String> current = files;
        return current == null ? 0 : current.size();
    }

    private static synchronized Map<String, String> ensureLoaded() throws Exception {
        if (files != null) return files;
        if (disabled) return null;

        if (!Boolean.getBoolean(SKIP_MOD_CHECK_PROPERTY)) {
            List<ModManager.ModSpec> enabled = ModManager.getInstance().getEnabledMods();
            if (enabled != null && !enabled.isEmpty()) {
                disabled = true;
                System.out.println("BrowserSpecCache: disabled because external mods are enabled count=" + enabled.size());
                return null;
            }
        }

        String packPath = System.getProperty(PATH_PROPERTY, "").trim();
        if (packPath.isEmpty()) {
            disabled = true;
            System.out.println("BrowserSpecCache: disabled because " + PATH_PROPERTY + " is empty");
            return null;
        }

        long started = System.currentTimeMillis();
        String text;
        try (InputStream in = new FileInputStream(packPath)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(2 * 1024 * 1024);
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) out.write(buffer, 0, read);
            }
            text = new String(out.toByteArray(), "UTF-8");
        }

        JSONObject root = new JSONObject(text);
        if (root.optInt("version", -1) != 1) {
            throw new IllegalStateException("unsupported cache version=" + root.optInt("version", -1));
        }
        JSONObject packed = root.getJSONObject("files");
        HashMap<String, String> loaded = new HashMap<String, String>(Math.max(16, packed.length() * 2));
        Iterator<?> keys = packed.keys();
        while (keys.hasNext()) {
            String key = String.valueOf(keys.next());
            loaded.put(normalize(key), packed.getString(key));
        }
        int expected = root.optInt("fileCount", loaded.size());
        if (expected != loaded.size() || loaded.size() < 1000) {
            throw new IllegalStateException("cache file-count mismatch expected=" + expected + " actual=" + loaded.size());
        }

        ArrayList<String> variants = new ArrayList<String>();
        for (String path : loaded.keySet()) {
            String normalized = normalize(path);
            if (normalized.startsWith("data/variants/")
                    && normalized.toLowerCase(Locale.ROOT).endsWith(".variant")) {
                variants.add(normalized);
            }
        }
        Collections.sort(variants);
        if (variants.isEmpty()) {
            throw new IllegalStateException("cache contains no stock variant paths");
        }

        loadMs = Math.max(0L, System.currentTimeMillis() - started);
        variantPaths = Collections.unmodifiableList(variants);
        files = Collections.unmodifiableMap(loaded);
        System.out.println(
                "BrowserSpecCache: ready files=" + loaded.size()
                        + " sourceBytes=" + root.optLong("sourceBytes", -1L)
                        + " loadMs=" + loadMs
                        + " path=" + packPath);
        return files;
    }

    private static boolean isEligible(String path) {
        String value = normalize(path).toLowerCase(Locale.ROOT);
        return value.endsWith(".variant")
                || value.endsWith(".ship")
                || value.endsWith(".skin")
                || value.endsWith(".wpn")
                || value.endsWith(".proj")
                || value.endsWith(".system")
                || value.endsWith(".skill");
    }

    private static String normalize(String path) {
        String value = path.replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        return value;
    }

    private static String describe(Throwable error) {
        if (error == null) return "unknown";
        String message = error.getMessage();
        return error.getClass().getName() + (message == null || message.isEmpty() ? "" : ": " + message);
    }
}
