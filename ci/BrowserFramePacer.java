package com.fs.starfarer;

/** Browser-specific frame pacing used by the patched BaseGameState render loop. */
public final class BrowserFramePacer {
    private BrowserFramePacer() {}

    public static void sleepOrYield(long delayMillis) throws InterruptedException {
        if (delayMillis > 0L) {
            Thread.sleep(delayMillis);
        } else {
            Thread.yield();
        }
    }
}
