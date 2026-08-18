package com.fs.starfarer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Diagnostic-only aggregate profiler for ResourceLoaderState's startup queue. */
public final class BrowserResourceQueueProfile {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserResourceQueueProfile";
    private static final Map<java.lang.String, Stats> QUEUED = new HashMap<java.lang.String, Stats>();
    private static final Map<java.lang.String, Timing> LOADS = new HashMap<java.lang.String, Timing>();
    private static final Set<java.lang.String> UNIQUE = new HashSet<java.lang.String>();
    // beginLoad/endLoad run on ResourceLoaderState.init's serial queue thread.
    // Keep one active record instead of a ThreadLocal to minimize diagnostic overhead.
    private static Active activeLoad;
    private static final List<Slow> SLOW = new ArrayList<Slow>();
    private static long rawCount;
    private static long uniqueCount;
    private static long uniqueWeight;

    private BrowserResourceQueueProfile() {}

    public static void record(Object type, java.lang.String path, int weight) {
        if (!enabled() || type == null || path == null) return;
        java.lang.String typeName = java.lang.String.valueOf(type);
        java.lang.String normalized = normalize(path);
        if (normalized.isEmpty()) return;
        java.lang.String group = typeName + "|" + group(normalized);
        java.lang.String uniqueKey = typeName + "|" + normalized;
        synchronized (BrowserResourceQueueProfile.class) {
            rawCount++;
            Stats s = QUEUED.get(group);
            if (s == null) { s = new Stats(group); QUEUED.put(group, s); }
            s.raw++;
            if (UNIQUE.add(uniqueKey)) {
                uniqueCount++;
                uniqueWeight += weight;
                s.unique++;
                s.weight += weight;
            }
        }
    }

    public static void beginLoad(Object type, java.lang.String path) {
        if (!enabled() || type == null || path == null) return;
        java.lang.String normalized = normalize(path);
        if (normalized.isEmpty()) return;
        activeLoad = new Active(java.lang.String.valueOf(type), normalized, System.nanoTime());
    }

    public static void endLoad(Object type, java.lang.String path) {
        if (!enabled()) return;
        Active active = activeLoad;
        activeLoad = null;
        if (active == null) return;
        long elapsed = Math.max(0L, System.nanoTime() - active.startedNs);
        java.lang.String normalized = path == null ? active.path : normalize(path);
        if (normalized.isEmpty()) normalized = active.path;
        java.lang.String typeName = type == null ? active.type : java.lang.String.valueOf(type);
        java.lang.String key = typeName + "|" + group(normalized);
        synchronized (BrowserResourceQueueProfile.class) {
            Timing timing = LOADS.get(key);
            if (timing == null) { timing = new Timing(key); LOADS.put(key, timing); }
            timing.calls++;
            timing.totalNs += elapsed;
            if (elapsed > timing.maxNs) timing.maxNs = elapsed;
            if (elapsed >= 1000000L) {
                SLOW.add(new Slow(typeName, normalized, elapsed));
                if (SLOW.size() > 120) {
                    Collections.sort(SLOW, SLOW_DESC);
                    while (SLOW.size() > 80) SLOW.remove(SLOW.size() - 1);
                }
            }
        }
    }

    public static void printPhase(java.lang.String phase) {
        if (!enabled()) return;
        long textureRaw = 0L, textureUnique = 0L;
        long optionalRaw = 0L, optionalUnique = 0L;
        long alphaRaw = 0L, alphaUnique = 0L;
        long soundRaw = 0L, soundUnique = 0L;
        long fontRaw = 0L, fontUnique = 0L;
        long scriptsRaw = 0L, scriptsUnique = 0L;
        long uiRaw = 0L, uiUnique = 0L, fxRaw = 0L, fxUnique = 0L;
        long backgroundsRaw = 0L, backgroundsUnique = 0L, terrainRaw = 0L, terrainUnique = 0L;
        long abilityRaw = 0L, abilityUnique = 0L, campaignRaw = 0L, campaignUnique = 0L;
        long hudRaw = 0L, hudUnique = 0L, portraitsRaw = 0L, portraitsUnique = 0L;
        synchronized (BrowserResourceQueueProfile.class) {
            for (Stats s : QUEUED.values()) {
                if (s.key.startsWith("TEXTURE|")) { textureRaw += s.raw; textureUnique += s.unique; }
                if ("TEXTURE|graphics/ui".equals(s.key)) { uiRaw = s.raw; uiUnique = s.unique; }
                else if ("TEXTURE|graphics/fx".equals(s.key)) { fxRaw = s.raw; fxUnique = s.unique; }
                else if ("TEXTURE|graphics/backgrounds".equals(s.key)) { backgroundsRaw = s.raw; backgroundsUnique = s.unique; }
                else if ("TEXTURE|graphics/terrain".equals(s.key)) { terrainRaw = s.raw; terrainUnique = s.unique; }
                else if ("TEXTURE|graphics/icons/abilities".equals(s.key)) { abilityRaw = s.raw; abilityUnique = s.unique; }
                else if ("TEXTURE|graphics/icons/campaign".equals(s.key)) { campaignRaw = s.raw; campaignUnique = s.unique; }
                else if ("TEXTURE|graphics/hud".equals(s.key)) { hudRaw = s.raw; hudUnique = s.unique; }
                else if ("TEXTURE|graphics/portraits".equals(s.key)) { portraitsRaw = s.raw; portraitsUnique = s.unique; }
                else if (s.key.startsWith("TEXTURE_OPTIONAL|")) { optionalRaw += s.raw; optionalUnique += s.unique; }
                else if (s.key.startsWith("TEXTURE_ALPHA_ADDER|")) { alphaRaw += s.raw; alphaUnique += s.unique; }
                else if (s.key.startsWith("SOUND|")) { soundRaw += s.raw; soundUnique += s.unique; }
                else if (s.key.startsWith("FONT|")) { fontRaw += s.raw; fontUnique += s.unique; }
                else if (s.key.startsWith("SCRIPTS|")) { scriptsRaw += s.raw; scriptsUnique += s.unique; }
            }
            System.out.println("BrowserResourceQueuePhase: phase=" + phase
                    + " raw=" + rawCount + " unique=" + uniqueCount
                    + " texture=" + textureRaw + "/" + textureUnique
                    + " optional=" + optionalRaw + "/" + optionalUnique
                    + " alpha=" + alphaRaw + "/" + alphaUnique
                    + " sound=" + soundRaw + "/" + soundUnique
                    + " font=" + fontRaw + "/" + fontUnique
                    + " scripts=" + scriptsRaw + "/" + scriptsUnique
                    + " ui=" + uiRaw + "/" + uiUnique
                    + " fx=" + fxRaw + "/" + fxUnique
                    + " backgrounds=" + backgroundsRaw + "/" + backgroundsUnique
                    + " terrain=" + terrainRaw + "/" + terrainUnique
                    + " abilities=" + abilityRaw + "/" + abilityUnique
                    + " campaignIcons=" + campaignRaw + "/" + campaignUnique
                    + " hud=" + hudRaw + "/" + hudUnique
                    + " portraits=" + portraitsRaw + "/" + portraitsUnique);
        }
    }

    public static void printQueueSummary() {
        if (!enabled()) return;
        List<Stats> values;
        synchronized (BrowserResourceQueueProfile.class) { values = new ArrayList<Stats>(QUEUED.values()); }
        Collections.sort(values, new Comparator<Stats>() {
            @Override public int compare(Stats a, Stats b) {
                int c = b.unique - a.unique;
                if (c != 0) return c;
                return a.key.compareTo(b.key);
            }
        });
        System.out.println("BrowserResourceQueueProfile: queued raw=" + rawCount + " unique=" + uniqueCount + " weight=" + uniqueWeight + " groups=" + values.size());
        for (Stats s : values) {
            System.out.println("BrowserResourceQueueProfile: queued-group key=" + s.key + " raw=" + s.raw + " unique=" + s.unique + " weight=" + s.weight);
        }
    }

    public static void printLoadSummary() {
        if (!enabled()) return;
        List<Timing> values;
        List<Slow> slow;
        synchronized (BrowserResourceQueueProfile.class) {
            values = new ArrayList<Timing>(LOADS.values());
            slow = new ArrayList<Slow>(SLOW);
        }
        Collections.sort(values, new Comparator<Timing>() {
            @Override public int compare(Timing a, Timing b) {
                if (a.totalNs < b.totalNs) return 1;
                if (a.totalNs > b.totalNs) return -1;
                return a.key.compareTo(b.key);
            }
        });
        long totalNs = 0L; long calls = 0L;
        for (Timing t : values) { totalNs += t.totalNs; calls += t.calls; }
        System.out.println("BrowserResourceQueueProfile: loaded calls=" + calls + " measuredMs=" + ms(totalNs) + " groups=" + values.size() + " queuedRaw=" + rawCount + " queuedUnique=" + uniqueCount + " queuedWeight=" + uniqueWeight);
        for (Timing t : values) {
            System.out.println("BrowserResourceQueueProfile: load-group key=" + t.key + " calls=" + t.calls + " totalMs=" + ms(t.totalNs) + " maxMs=" + ms(t.maxNs));
        }
        Collections.sort(slow, SLOW_DESC);
        int limit = Math.min(40, slow.size());
        for (int i = 0; i < limit; i++) {
            Slow s = slow.get(i);
            System.out.println("BrowserResourceQueueProfile: slow type=" + s.type + " ms=" + ms(s.ns) + " path=" + s.path);
        }
    }

    private static boolean enabled() { return Boolean.getBoolean(ENABLE_PROPERTY); }
    private static long ms(long ns) { return ns / 1000000L; }

    private static java.lang.String normalize(java.lang.String path) {
        java.lang.String v = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        while (v.startsWith("/")) v = v.substring(1);
        return v;
    }

    private static java.lang.String group(java.lang.String path) {
        java.lang.String[] p = path.split("/");
        if (p.length == 0) return "<empty>";
        if (p.length >= 3 && "graphics".equals(p[0]) && "icons".equals(p[1])) return p[0] + "/" + p[1] + "/" + p[2];
        if (p.length >= 2) return p[0] + "/" + p[1];
        return p[0];
    }

    private static final Comparator<Slow> SLOW_DESC = new Comparator<Slow>() {
        @Override public int compare(Slow a, Slow b) {
            if (a.ns < b.ns) return 1;
            if (a.ns > b.ns) return -1;
            return a.path.compareTo(b.path);
        }
    };

    private static final class Stats { final java.lang.String key; int raw; int unique; long weight; Stats(java.lang.String key){this.key=key;} }
    private static final class Timing { final java.lang.String key; long calls; long totalNs; long maxNs; Timing(java.lang.String key){this.key=key;} }
    private static final class Active { final java.lang.String type,path; final long startedNs; Active(java.lang.String type,java.lang.String path,long startedNs){this.type=type;this.path=path;this.startedNs=startedNs;} }
    private static final class Slow { final java.lang.String type,path; final long ns; Slow(java.lang.String type,java.lang.String path,long ns){this.type=type;this.path=path;this.ns=ns;} }
}
