package com.fs.starfarer.loading;

import java.util.concurrent.atomic.AtomicBoolean;

/** Browser-only tuning for Starsector's existing ResourceLoaderState worker pool. */
public final class BrowserResourceLoaderCompat {
    private static final java.lang.String THREADS_PROPERTY = "starsector.browserResourceLoaderThreads";
    private static final AtomicBoolean LOGGED = new AtomicBoolean();

    private BrowserResourceLoaderCompat() {}

    public static int resolveWorkerCount(int stockCount) {
        int fallback = stockCount > 0 ? stockCount : 1;
        int requested = Integer.getInteger(THREADS_PROPERTY, fallback).intValue();
        int resolved = requested > 0 ? requested : fallback;
        if (resolved > 8) resolved = 8;
        if (LOGGED.compareAndSet(false, true)) {
            System.out.println(
                    "BrowserResourceLoader: worker-pool stock=" + stockCount
                            + " requested=" + requested + " resolved=" + resolved);
        }
        return resolved;
    }
}
