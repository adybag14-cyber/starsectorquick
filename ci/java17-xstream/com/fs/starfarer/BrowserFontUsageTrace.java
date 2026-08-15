package com.fs.starfarer;

import java.util.concurrent.ConcurrentHashMap;

/** Diagnostic-only first-use trace for the stock font registry. */
public final class BrowserFontUsageTrace {
    private static final ConcurrentHashMap<java.lang.String, java.lang.Boolean> SEEN =
            new ConcurrentHashMap<java.lang.String, java.lang.Boolean>();

    private BrowserFontUsageTrace() {}

    public static void record(java.lang.String key) {
        if (key == null) return;
        if (SEEN.putIfAbsent(key, java.lang.Boolean.TRUE) == null) {
            java.lang.System.out.println(
                    "BrowserFontUsageTrace: first-use epochMs=" + java.lang.System.currentTimeMillis() + " key=" + key);
        }
    }
}
