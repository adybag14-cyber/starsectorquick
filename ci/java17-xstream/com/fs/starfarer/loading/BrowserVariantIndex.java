package com.fs.starfarer.loading;

import com.fs.starfarer.launcher.ModManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Browser fast path for core variant discovery using deployed index.list files. */
public final class BrowserVariantIndex {
    private static final String ENABLE_PROPERTY = "starsector.browserQuickVariantIndex";
    private static final String CONTENT_ROOT_PROPERTY = "starsector.contentRoot";
    private static final String VARIANT_ROOT = "data/variants";
    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<String, List<String>>();
    private static volatile Boolean externalSourcesPresent;
    private static volatile boolean fallbackLogged;
    private static volatile boolean activeLogged;

    private BrowserVariantIndex() {}

    public static List<String> tryListVariants(String directory, String extension) {
        if (!enabled() || !"variant".equals(extension) || !isVariantDirectory(directory)) return null;
        if (hasExternalVariantSources()) return null;
        File root = contentRoot();
        if (root == null) return null;
        try {
            List<String> result = cachedVariantFiles(root, normalize(directory), extension);
            logActiveOnce();
            return result;
        } catch (IOException ex) {
            logFallbackOnce(ex);
            return null;
        }
    }

    public static List<String> tryListDirectories(String directory) {
        if (!enabled() || !VARIANT_ROOT.equals(normalize(directory))) return null;
        if (hasExternalVariantSources()) return null;
        File root = contentRoot();
        if (root == null) return null;
        try {
            List<String> result = cachedDirectories(root, VARIANT_ROOT);
            logActiveOnce();
            return result;
        } catch (IOException ex) {
            logFallbackOnce(ex);
            return null;
        }
    }

    static List<String> readVariantFiles(File root, String directory, String extension) throws IOException {
        return readIndex(root, normalize(directory), extension, false);
    }

    static List<String> readDirectories(File root, String directory) throws IOException {
        return readIndex(root, normalize(directory), null, true);
    }

    private static List<String> cachedVariantFiles(File root, String directory, String extension) throws IOException {
        String key = root.getAbsolutePath() + "|files|" + directory + "|" + extension;
        List<String> cached = CACHE.get(key);
        if (cached != null) return cached;
        List<String> loaded = Collections.unmodifiableList(readVariantFiles(root, directory, extension));
        List<String> previous = CACHE.putIfAbsent(key, loaded);
        return previous == null ? loaded : previous;
    }

    private static List<String> cachedDirectories(File root, String directory) throws IOException {
        String key = root.getAbsolutePath() + "|dirs|" + directory;
        List<String> cached = CACHE.get(key);
        if (cached != null) return cached;
        List<String> loaded = Collections.unmodifiableList(readDirectories(root, directory));
        List<String> previous = CACHE.putIfAbsent(key, loaded);
        return previous == null ? loaded : previous;
    }

    private static List<String> readIndex(File root, String directory, String extension, boolean directories)
            throws IOException {
        File index = new File(root, directory.replace('/', File.separatorChar) + File.separator + "index.list");
        if (!index.isFile()) throw new IOException("variant index missing: " + index);
        List<String> result = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(index), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    if (!line.isEmpty() && line.charAt(0) == '\ufeff') line = line.substring(1);
                }
                if (line.isEmpty()) continue;
                int tab = line.indexOf('\t');
                String name = (tab >= 0 ? line.substring(0, tab) : line).trim();
                if (name.isEmpty()) continue;
                boolean isDirectory = name.endsWith("/");
                if (directories) {
                    if (!isDirectory) continue;
                    String child = name.substring(0, name.length() - 1);
                    if (!child.isEmpty()) result.add(directory + "/" + child);
                } else {
                    if (isDirectory) continue;
                    if (extension != null && !name.endsWith("." + extension)) continue;
                    result.add(directory + "/" + name);
                }
            }
        }
        return result;
    }

    private static boolean enabled() { return Boolean.getBoolean(ENABLE_PROPERTY); }
    private static boolean isVariantDirectory(String directory) {
        String normalized = normalize(directory);
        return VARIANT_ROOT.equals(normalized) || normalized.startsWith(VARIANT_ROOT + "/");
    }
    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.replace('\\', '/');
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
    private static File contentRoot() {
        String value = System.getProperty(CONTENT_ROOT_PROPERTY, "").trim();
        if (value.isEmpty()) value = System.getProperty("user.dir", "").trim();
        return value.isEmpty() ? null : new File(value);
    }
    private static boolean hasExternalVariantSources() {
        Boolean cached = externalSourcesPresent;
        if (cached != null) return cached.booleanValue();
        boolean present = true;
        try {
            present = ModManager.getInstance().getEnabledMods() != null
                    && !ModManager.getInstance().getEnabledMods().isEmpty();
            if (!present) {
                File root = contentRoot();
                if (root != null) {
                    File missionIndex = new File(root, "files" + File.separator + "saves" + File.separator
                            + "missions" + File.separator + "variants" + File.separator + "index.list");
                    present = missionIndex.isFile();
                }
            }
        } catch (Throwable ex) {
            present = true;
        }
        externalSourcesPresent = Boolean.valueOf(present);
        return present;
    }
    private static void logActiveOnce() {
        if (activeLogged) return;
        activeLogged = true;
        System.out.println("BrowserVariantIndex: using index.list for core variant discovery.");
    }
    private static void logFallbackOnce(Throwable ex) {
        if (fallbackLogged) return;
        fallbackLogged = true;
        System.out.println("BrowserVariantIndex: manifest fast path unavailable; using stock discovery: "
                + ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}
