import com.fs.starfarer.BrowserFramePacer;

public final class ProbeBrowserFramePacer {
    public static void main(String[] args) throws Exception {
        System.setProperty("starsector.browserFramePacing", "true");
        long start = System.nanoTime();
        // requestedMillis=0 deliberately proves a positive sub-ms exact remainder
        // is not mistaken for a render-bound frame after stock's f2i truncation.
        BrowserFramePacer.sleep(0L, 0.0005f, 60f);
        long subMillisNanos = System.nanoTime() - start;
        if (subMillisNanos <= 0L) throw new AssertionError("sub-ms pacing did not run");

        start = System.nanoTime();
        for (int i = 0; i < 5; i++) BrowserFramePacer.sleep(16L, 1f / 60f, 60f);
        long pacedMs = (System.nanoTime() - start) / 1000000L;
        if (pacedMs < 45L || pacedMs > 2000L) {
            throw new AssertionError("deadline pacing window unexpected: " + pacedMs + "ms");
        }

        System.setProperty("starsector.browserFramePacing", "false");
        start = System.nanoTime();
        for (int i = 0; i < 20; i++) BrowserFramePacer.sleep(0L, 0f, 60f);
        long disabledMs = (System.nanoTime() - start) / 1000000L;
        if (disabledMs > 500L) throw new AssertionError("disabled pacing adds delay: " + disabledMs + "ms");
        System.out.println("ProbeBrowserFramePacer: OK pacedMs=" + pacedMs + " disabledMs=" + disabledMs);
    }
}
