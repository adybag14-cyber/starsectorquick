package com.fs.starfarer.loading;

import com.fs.starfarer.launcher.ModManager;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
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
    private static volatile boolean disabled;
    private static final AtomicLong HITS = new AtomicLong();
    private static final AtomicLong MISSES = new AtomicLong();
    private static final AtomicBoolean FIRST_HIT_LOGGED = new AtomicBoolean();
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
            String value = current.get(normalize(path));
            if (value != null) {
                HITS.incrementAndGet();
                if (FIRST_HIT_LOGGED.compareAndSet(false, true)) {
                    System.out.println("BrowserSpecCache: first-hit path=" + normalize(path));
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

        loadMs = Math.max(0L, System.currentTimeMillis() - started);
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
                || value.endsWith(".proj");
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