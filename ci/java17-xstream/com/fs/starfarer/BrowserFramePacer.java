package com.fs.starfarer;

/** Browser frame pacer for BaseGameState's existing sleep boundary. */
public final class BrowserFramePacer {
    private static final String ENABLE_PROPERTY = "starsector.browserFramePacing";
    private static long nextDeadlineNanos;
    private static float targetFps;
    private static long calls;
    private static long preciseSleeps;
    private static long lateFrames;
    private static long resets;

    private BrowserFramePacer() {}

    public static void sleep(long requestedMillis, float remainingSeconds, float fps) throws InterruptedException {
        calls++;
        if (!isEnabled() || !(fps > 0f) || Float.isInfinite(fps) || Float.isNaN(fps)) {
            resetDeadline();
            Thread.sleep(Math.max(0L, requestedMillis));
            return;
        }

        long frameNanos = Math.max(1L, (long)(1000000000.0d / (double)fps));
        long now = System.nanoTime();
        if (targetFps != fps || nextDeadlineNanos <= 0L) {
            targetFps = fps;
            nextDeadlineNanos = now + Math.max(0L, (long)(remainingSeconds * 1000000000.0f));
            resets++;
        }

        if (!(remainingSeconds > 0f)) {
            lateFrames++;
            nextDeadlineNanos = now + frameNanos;
            Thread.sleep(0L);
            maybeLog(requestedMillis, fps, 0L);
            return;
        }

        long remaining = nextDeadlineNanos - now;
        if (remaining > 0L) {
            long millis = remaining / 1000000L;
            int nanos = (int)(remaining % 1000000L);
            Thread.sleep(millis, nanos);
            preciseSleeps++;
            now = System.nanoTime();
        } else {
            lateFrames++;
        }

        long behind = now - nextDeadlineNanos;
        if (behind > frameNanos * 4L) {
            nextDeadlineNanos = now + frameNanos;
            resets++;
        } else if (behind >= 0L) {
            nextDeadlineNanos += (behind / frameNanos + 1L) * frameNanos;
        } else {
            nextDeadlineNanos += frameNanos;
        }
        maybeLog(requestedMillis, fps, remaining);
    }

    private static boolean isEnabled() {
        String value = System.getProperty(ENABLE_PROPERTY);
        if (value == null) return true;
        value = value.trim();
        return !("false".equalsIgnoreCase(value) || "0".equals(value) || "off".equalsIgnoreCase(value));
    }

    private static void resetDeadline() {
        targetFps = 0f;
        nextDeadlineNanos = 0L;
    }

    private static void maybeLog(long requestedMillis, float fps, long remainingNanos) {
        if (calls <= 3L || calls == 60L || calls % 1200L == 0L) {
            System.out.println(
                    "BrowserFramePacer: calls=" + calls
                            + " fps=" + fps
                            + " requestedMs=" + requestedMillis
                            + " remainingUs=" + (remainingNanos / 1000L)
                            + " sleeps=" + preciseSleeps
                            + " late=" + lateFrames
                            + " resets=" + resets);
        }
    }
}
