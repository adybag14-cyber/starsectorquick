package com.fs.starfarer;

import com.fs.graphics.oOoO;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Browser quick-start deferral for nonessential textures with exact lazy loading on first lookup. */
public final class BrowserDeferredTextureQueue {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserDeferredTextures";
    private static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    private static final ConcurrentHashMap<java.lang.String, java.lang.String> DEFERRED = new ConcurrentHashMap<java.lang.String, java.lang.String>();
    private static final AtomicLong DEFERRED_COUNT = new AtomicLong();
    private static final AtomicLong LAZY_LOAD_COUNT = new AtomicLong();

    private BrowserDeferredTextureQueue() {}

    public static void loadOrDefer(java.lang.String key, java.lang.String path) throws IOException {
        if (!ENABLED || key == null || path == null || !shouldDeferPath(path)) {
            oOoO.o00000(key, path);
            return;
        }
        java.lang.String prior = DEFERRED.putIfAbsent(key, path);
        if (prior != null && !prior.equals(path)) {
            // oOoO's registry is first-registration-wins. Materialize the earlier
            // deferred source before processing the later duplicate so startup
            // deferral cannot invert that ordering.
            DEFERRED.remove(key, prior);
            oOoO.o00000(key, prior);
            oOoO.o00000(key, path);
            return;
        }
        if (prior == null) {
            long count = DEFERRED_COUNT.incrementAndGet();
            if (count == 1L) {
                System.out.println("BrowserDeferredTexture: first-deferred key=" + key + " path=" + path);
            }
        }
    }

    /**
     * Queue stock ImageIO predecode unless this exact resource will be deferred.
     * ResourceLoaderState$o ordinals are TEXTURE=0, TEXTURE_OPTIONAL=1,
     * TEXTURE_ALPHA_ADDER=2; the alpha-adder path must keep its stock predecode.
     */
    public static void queueImagePredecode(java.lang.String path, int resourceTypeOrdinal) {
        if (ENABLED && resourceTypeOrdinal != 2 && shouldDeferPath(path)) return;
        com.fs.graphics.L.\u00d600000(path);
    }

    /** Called before oOoO.new(key) checks the registry. */
    public static void ensureLoaded(java.lang.String key) {
        if (!ENABLED || key == null || DEFERRED.get(key) == null) return;
        synchronized (BrowserDeferredTextureQueue.class) {
            java.lang.String path = DEFERRED.get(key);
            if (path == null) return;
            try {
                oOoO.o00000(key, path);
                // Keep the map entry visible until registration has completed so
                // a racing lookup cannot take the fast-miss path too early.
                DEFERRED.remove(key, path);
                long count = LAZY_LOAD_COUNT.incrementAndGet();
                if (count == 1L) {
                    System.out.println("BrowserDeferredTexture: first-lazy-load key=" + key + " path=" + path);
                }
            } catch (IOException error) {
                throw new RuntimeException("deferred texture load failed key=" + key + " path=" + path, error);
            }
        }
    }

    public static boolean shouldDeferPath(java.lang.String path) {
        if (path == null) return false;
        java.lang.String value = path.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        while (value.startsWith("/")) value = value.substring(1);
        return value.startsWith("graphics/illustrations/")
                || value.startsWith("graphics/portraits/")
                || value.startsWith("graphics/hullmods/")
                || value.startsWith("graphics/icons/markets/")
                || value.startsWith("graphics/icons/cargo/")
                || value.startsWith("graphics/icons/intel/")
                || value.startsWith("graphics/icons/skills/")
                || value.startsWith("graphics/icons/missions/")
                || value.startsWith("graphics/icons/hullsys/")
                || value.startsWith("graphics/icons/industry/")
                || value.startsWith("graphics/icons/codex/")
                || value.startsWith("graphics/icons/reports/");
    }

    public static long getDeferredCount() { return DEFERRED_COUNT.get(); }
    public static long getLazyLoadCount() { return LAZY_LOAD_COUNT.get(); }
    public static int getPendingCount() { return DEFERRED.size(); }
}
