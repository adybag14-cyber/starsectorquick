package com.fs.starfarer.campaign.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Browser-only O(1) duplicate rule-id index for a single Rules load. */
public final class BrowserRuleDuplicateIndex {
    private static final String PROPERTY = "starsector.browserRuleDuplicateIndex";
    private static final ThreadLocal<Map<String, Set<String>>> ACTIVE =
            new ThreadLocal<Map<String, Set<String>>>();

    private BrowserRuleDuplicateIndex() {}

    public static void begin() {
        if (!Boolean.getBoolean(PROPERTY)) {
            ACTIVE.remove();
            return;
        }
        ACTIVE.set(new HashMap<String, Set<String>>(1024));
    }

    public static boolean enabled() {
        return ACTIVE.get() != null;
    }

    /** Reuses the per-Rules-load browser state as a distinct traversal-bypass marker. */
    public static boolean deadVariableTrackingBypassEnabled() {
        return ACTIVE.get() != null;
    }

    public static void checkAndRecord(String trigger, String id) {
        Map<String, Set<String>> byTrigger = ACTIVE.get();
        if (byTrigger == null) return;
        Set<String> ids = byTrigger.get(trigger);
        if (ids == null) {
            ids = new HashSet<String>();
            byTrigger.put(trigger, ids);
        }
        if (!ids.add(id)) {
            throw new RuntimeException("Duplicate rule id:" + id);
        }
    }

    public static void finish() {
        Map<String, Set<String>> byTrigger = ACTIVE.get();
        if (byTrigger == null) return;
        int rules = 0;
        for (Set<String> ids : byTrigger.values()) rules += ids.size();
        System.out.println(
                "BrowserRuleDuplicateIndex: rules=" + rules + " triggers=" + byTrigger.size());
        ACTIVE.remove();
    }
}
