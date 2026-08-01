package com.fs.starfarer;

public final class WebRuntimeCompat {
    private static long traverseCount = 0L;
    private static long sleepCount = 0L;
    private static long zeroSleepCount = 0L;

    private WebRuntimeCompat() {}

    public static void onTraverse() {
        traverseCount++;
        if (traverseCount <= 4L || traverseCount % 100L == 0L) {
            System.out.println("WebRuntimeCompat: BaseGameState.traverse count=" + traverseCount);
        }
    }

    public static void safeSleep(long millis) throws InterruptedException {
        sleepCount++;
        if (millis <= 0L) {
            zeroSleepCount++;
            if (zeroSleepCount <= 4L || zeroSleepCount % 300L == 0L) {
                System.out.println("WebRuntimeCompat: skipped Thread.sleep(" + millis + ") count=" + zeroSleepCount);
            }
            return;
        }
        if (sleepCount <= 4L) {
            System.out.println("WebRuntimeCompat: Thread.sleep(" + millis + ")");
        }
        Thread.sleep(millis);
    }
}
