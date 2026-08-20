package com.fs.starfarer;

import com.fs.graphics.oOoO;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Browser quick-start deferral for nonessential textures with exact lazy loading on first lookup. */
public final class BrowserDeferredTextureQueue {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserDeferredTextures";
    private static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    private static final java.lang.String EARLY_PREDECODE_PROPERTY = "starsector.browserEarlyImagePredecode";
    private static final boolean EARLY_PREDECODE_ENABLED = Boolean.getBoolean(EARLY_PREDECODE_PROPERTY);
    private static final ConcurrentHashMap<java.lang.String, java.lang.String> DEFERRED = new ConcurrentHashMap<java.lang.String, java.lang.String>();
    private static final AtomicLong DEFERRED_COUNT = new AtomicLong();
    private static final AtomicLong LAZY_LOAD_COUNT = new AtomicLong();
    private static final java.lang.String GAMEPLAY_PREWARM_PROPERTY = "starsector.browserGameplayPrewarm";
    private static final java.lang.String GAMEPLAY_PREWARM_DELAY_PROPERTY = "starsector.browserGameplayPrewarmDelayMs";
    private static final java.lang.String GAMEPLAY_PREWARM_PAUSE_PROPERTY = "starsector.browserGameplayPrewarmPauseMs";
    private static final ConcurrentLinkedQueue<PrewarmItem> PREWARM_UI = new ConcurrentLinkedQueue<PrewarmItem>();
    private static final ConcurrentLinkedQueue<PrewarmItem> PREWARM_REFIT = new ConcurrentLinkedQueue<PrewarmItem>();
    private static final ConcurrentLinkedQueue<PrewarmItem> PREWARM_WORLD = new ConcurrentLinkedQueue<PrewarmItem>();
    private static final AtomicBoolean PREWARM_STARTED = new AtomicBoolean();
    private static final AtomicBoolean PREWARM_DONE = new AtomicBoolean();
    private static final AtomicLong PREDECODE_COUNT = new AtomicLong();
    private static final AtomicLong PREDECODE_FAILED = new AtomicLong();
    private static final ConcurrentHashMap<java.lang.String, AtomicLong> EARLY_PREDECODE_COUNTS = new ConcurrentHashMap<java.lang.String, AtomicLong>();
    private static final AtomicLong EARLY_PREDECODE_QUEUED = new AtomicLong();
    private static final AtomicLong EARLY_PREDECODE_STOCK_SKIPS = new AtomicLong();
    private static final AtomicBoolean EARLY_PREDECODE_STARTED = new AtomicBoolean();

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
            enqueueGameplayPrewarm(key, path);
        }
    }

    /**
     * Queue stock ImageIO predecode unless this exact resource will be deferred.
     * ResourceLoaderState$o ordinals are TEXTURE=0, TEXTURE_OPTIONAL=1,
     * TEXTURE_ALPHA_ADDER=2; the alpha-adder path must keep its stock predecode.
     */
    /**
     * Queue browser-startup image decode at resource-registration time. This is
     * the same stock L predecode queue used later by ResourceLoaderState; the
     * per-path counters preserve the stock pass multiplicity exactly.
     */
    public static void queueEarlyImagePredecode(java.lang.String path, int resourceTypeOrdinal) {
        // Freeze the early set when the worker starts. Resources discovered by SpecStore
        // and later phases stay on Starsector's normal post-SpecStore predecode pass so
        // the background decoder cannot compete with the full spec-loading workload.
        if (!EARLY_PREDECODE_ENABLED || EARLY_PREDECODE_STARTED.get()
                || path == null || resourceTypeOrdinal < 0 || resourceTypeOrdinal > 2) return;
        if (ENABLED && resourceTypeOrdinal != 2 && shouldDeferPath(path)) return;
        java.lang.String key = normalize(path);
        // UI is the highest-cost pre-SpecStore group and is already fully queued here.
        // Keep early overlap bounded to this group so ImageIO does not starve SpecStore
        // or leave a large decoder backlog for ResourceLoaderState finalization.
        if (!key.startsWith("graphics/ui/")) return;
        AtomicLong counter = EARLY_PREDECODE_COUNTS.get(key);
        if (counter == null) {
            AtomicLong fresh = new AtomicLong();
            AtomicLong prior = EARLY_PREDECODE_COUNTS.putIfAbsent(key, fresh);
            counter = prior == null ? fresh : prior;
        }
        counter.incrementAndGet();
        EARLY_PREDECODE_QUEUED.incrementAndGet();
        com.fs.graphics.L.\u00d600000(path);
    }

    /** Start the stock ImageIO worker before SpecStore so already-queued UI can overlap it. */
    public static void startEarlyImagePredecode() {
        if (!EARLY_PREDECODE_ENABLED || EARLY_PREDECODE_QUEUED.get() <= 0L
                || !EARLY_PREDECODE_STARTED.compareAndSet(false, true)) return;
        System.out.println("BrowserEarlyImagePredecode: start queued=" + EARLY_PREDECODE_QUEUED.get()
                + " paths=" + EARLY_PREDECODE_COUNTS.size());
        com.fs.graphics.L.o00000();
    }

    public static void queueImagePredecode(java.lang.String path, int resourceTypeOrdinal) {
        if (ENABLED && resourceTypeOrdinal != 2 && shouldDeferPath(path)) return;
        if (EARLY_PREDECODE_ENABLED && path != null) {
            java.lang.String key = normalize(path);
            AtomicLong counter = EARLY_PREDECODE_COUNTS.get(key);
            if (counter != null) {
                for (;;) {
                    long before = counter.get();
                    if (before <= 0L) break;
                    if (counter.compareAndSet(before, before - 1L)) {
                        EARLY_PREDECODE_STOCK_SKIPS.incrementAndGet();
                        if (before == 1L) EARLY_PREDECODE_COUNTS.remove(key, counter);
                        return;
                    }
                }
            }
        }
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

    /**
     * Start a low-priority post-Campaign ImageIO warmup. This intentionally does
     * not call the texture registry or OpenGL: it only runs the same background
     * image predecode routine ResourceLoader normally uses, after first-playable's
     * critical path has passed. Actual texture registration remains render-thread
     * lazy and exact on first use.
     */
    public static void startGameplayPrewarm() {
        if (!ENABLED || !Boolean.getBoolean(GAMEPLAY_PREWARM_PROPERTY)
                || !PREWARM_STARTED.compareAndSet(false, true)) return;
        final long delayMs = readLongProperty(GAMEPLAY_PREWARM_DELAY_PROPERTY, 5000L, 0L, 60000L);
        final long pauseMs = readLongProperty(GAMEPLAY_PREWARM_PAUSE_PROPERTY, 2L, 0L, 1000L);
        System.out.println("BrowserDeferredTexturePrewarm: scheduled delayMs=" + delayMs
                + " pauseMs=" + pauseMs
                + " ui=" + PREWARM_UI.size()
                + " refit=" + PREWARM_REFIT.size()
                + " world=" + PREWARM_WORLD.size());
        Thread thread = new Thread(new Runnable() {
            @Override public void run() { runGameplayPrewarm(delayMs, pauseMs); }
        }, "starsector-browser-gameplay-predecode");
        thread.setDaemon(true);
        try { thread.setPriority(Thread.MIN_PRIORITY); } catch (Throwable ignored) {}
        thread.start();
    }

    private static void runGameplayPrewarm(long delayMs, long pauseMs) {
        long started = System.currentTimeMillis();
        try {
            if (delayMs > 0L) Thread.sleep(delayMs);
            drainPrewarmQueue(PREWARM_UI, pauseMs);
            drainPrewarmQueue(PREWARM_REFIT, pauseMs);
            drainPrewarmQueue(PREWARM_WORLD, pauseMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            PREWARM_DONE.set(true);
            System.out.println("BrowserDeferredTexturePrewarm: complete decoded=" + PREDECODE_COUNT.get()
                    + " failed=" + PREDECODE_FAILED.get()
                    + " elapsedMs=" + Math.max(0L, System.currentTimeMillis() - started)
                    + " pending=" + getGameplayPrewarmPendingCount());
        }
    }

    private static void drainPrewarmQueue(ConcurrentLinkedQueue<PrewarmItem> queue, long pauseMs)
            throws InterruptedException {
        for (;;) {
            PrewarmItem item = queue.poll();
            if (item == null) return;
            if (DEFERRED.get(item.key) == null) continue;
            try {
                com.fs.graphics.L.\u00d600000(item.path);
                long count = PREDECODE_COUNT.incrementAndGet();
                if (count == 1L || count % 100L == 0L) {
                    System.out.println("BrowserDeferredTexturePrewarm: decoded=" + count
                            + " pending=" + getGameplayPrewarmPendingCount()
                            + " path=" + item.path);
                }
            } catch (Throwable error) {
                long failed = PREDECODE_FAILED.incrementAndGet();
                if (failed <= 8L) {
                    System.out.println("BrowserDeferredTexturePrewarm: predecode failed path=" + item.path
                            + " error=" + error.getClass().getName()
                            + (error.getMessage() == null ? "" : ": " + error.getMessage()));
                }
            }
            if (pauseMs > 0L) Thread.sleep(pauseMs);
            else Thread.yield();
        }
    }

    private static void enqueueGameplayPrewarm(java.lang.String key, java.lang.String path) {
        if (key == null || path == null) return;
        java.lang.String value = normalize(path);
        int priority = gameplayPrewarmPriority(value);
        if (priority < 0) return;
        PrewarmItem item = new PrewarmItem(key, path);
        if (priority == 0) PREWARM_UI.add(item);
        else if (priority == 1) PREWARM_REFIT.add(item);
        else PREWARM_WORLD.add(item);
    }

    private static int gameplayPrewarmPriority(java.lang.String value) {
        if (value.startsWith("graphics/portraits/")
                || value.startsWith("graphics/icons/skills/")
                || value.startsWith("graphics/hullmods/")
                || value.startsWith("graphics/icons/cargo/")
                || value.startsWith("graphics/icons/intel/")
                || value.startsWith("graphics/ui/buttons/")
                || isDeferredFleetTabStockAsset(value)) return 0;
        if (value.startsWith("graphics/ships/")
                || value.startsWith("graphics/icons/hullsys/")) return 1;
        if (value.startsWith("graphics/factions/")
                || value.startsWith("graphics/planets/")
                || value.startsWith("graphics/stations/")
                || value.startsWith("graphics/icons/markets/")
                || value.startsWith("graphics/icons/industry/")
                || value.startsWith("graphics/icons/reports/")) return 2;
        return -1;
    }

    private static java.lang.String normalize(java.lang.String path) {
        java.lang.String value = path.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        while (value.startsWith("/")) value = value.substring(1);
        return value;
    }

    private static long readLongProperty(java.lang.String name, long fallback, long min, long max) {
        try {
            java.lang.String raw = System.getProperty(name, "").trim();
            if (raw.isEmpty()) return fallback;
            long value = Long.parseLong(raw);
            return Math.max(min, Math.min(max, value));
        } catch (Throwable ignored) { return fallback; }
    }

    private static final class PrewarmItem {
        final java.lang.String key;
        final java.lang.String path;
        PrewarmItem(java.lang.String key, java.lang.String path) { this.key = key; this.path = path; }
    }

    public static long getGameplayPredecodeCount() { return PREDECODE_COUNT.get(); }
    public static long getGameplayPredecodeFailedCount() { return PREDECODE_FAILED.get(); }
    public static int getGameplayPrewarmPendingCount() {
        return PREWARM_UI.size() + PREWARM_REFIT.size() + PREWARM_WORLD.size();
    }
    public static boolean isGameplayPrewarmDone() { return PREWARM_DONE.get(); }

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
                || value.startsWith("graphics/icons/reports/")
                || value.startsWith("graphics/factions/")
                || value.startsWith("graphics/planets/")
                || value.startsWith("graphics/stations/")
                || value.startsWith("graphics/warroom/")
                || value.startsWith("graphics/ui/buttons/")
                || isDeferredFleetTabStockAsset(value)
                || value.startsWith("graphics/damage/")
                || value.startsWith("graphics/icons/tactical/")
                || value.startsWith("graphics/debris/")
                || value.startsWith("graphics/missiles/")
                || value.startsWith("graphics/asteroids/")
                || value.startsWith("data/missions/");
    }

    private static boolean isDeferredFleetTabStockAsset(java.lang.String value) {
        return value.equals("graphics/ui/icons/fleettab/lr_doodad_bg.png")
                || value.equals("graphics/ui/icons/fleettab/buy.png")
                || value.equals("graphics/ui/icons/fleettab/cargo_24x16b.png")
                || value.equals("graphics/ui/icons/fleettab/chassis14x.png")
                || value.equals("graphics/ui/icons/fleettab/cr16x.png")
                || value.equals("graphics/ui/icons/fleettab/cr24x16.png")
                || value.equals("graphics/ui/icons/fleettab/cr32x.png")
                || value.equals("graphics/ui/icons/fleettab/hull16x.png")
                || value.equals("graphics/ui/icons/fleettab/hull32x.png")
                || value.equals("graphics/ui/icons/fleettab/logistics_80x18.png")
                || value.equals("graphics/ui/icons/fleettab/logistics_priority.png")
                || value.equals("graphics/ui/icons/fleettab/logistics_priority2.png")
                || value.equals("graphics/ui/icons/fleettab/logistics_priority_1box.png")
                || value.equals("graphics/ui/icons/fleettab/logistics_priority_2box.png")
                || value.equals("graphics/ui/icons/fleettab/logistics_priority_3box.png")
                || value.equals("graphics/ui/icons/fleettab/logistics_priority_low.png")
                || value.equals("graphics/ui/icons/fleettab/more_info.png")
                || value.equals("graphics/ui/icons/fleettab/mothball.png")
                || value.equals("graphics/ui/icons/fleettab/rank0_16x24.png")
                || value.equals("graphics/ui/icons/fleettab/rank1_16x24.png")
                || value.equals("graphics/ui/icons/fleettab/rank2_16x24.png")
                || value.equals("graphics/ui/icons/fleettab/rank3_16x24.png")
                || value.equals("graphics/ui/icons/fleettab/refit.png")
                || value.equals("graphics/ui/icons/fleettab/repair_rate_24x16.png")
                || value.equals("graphics/ui/icons/fleettab/scuttle.png")
                || value.equals("graphics/ui/icons/fleettab/sell.png")
                || value.equals("graphics/ui/icons/fleettab/ship_store.png")
                || value.equals("graphics/ui/icons/fleettab/ship_take.png")
                || value.equals("graphics/ui/icons/fleettab/supplies16x.png")
                || value.equals("graphics/ui/icons/fleettab/supplies_24x16.png")
                || value.equals("graphics/ui/icons/fleettab/suspend_repairs.png");
    }

    public static long getDeferredCount() { return DEFERRED_COUNT.get(); }
    public static long getLazyLoadCount() { return LAZY_LOAD_COUNT.get(); }
    public static int getPendingCount() { return DEFERRED.size(); }
    public static long getEarlyPredecodeQueuedCount() { return EARLY_PREDECODE_QUEUED.get(); }
    public static long getEarlyPredecodeStockSkipCount() { return EARLY_PREDECODE_STOCK_SKIPS.get(); }
    public static int getEarlyPredecodePendingCount() { return EARLY_PREDECODE_COUNTS.size(); }
}
