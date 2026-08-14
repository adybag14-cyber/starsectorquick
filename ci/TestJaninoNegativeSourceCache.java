import com.fs.starfarer.loading.LoadingUtils;
import com.fs.starfarer.loading.ooOo;

/** Proves browser miss memoization while preserving the disabled and successful-read paths. */
public final class TestJaninoNegativeSourceCache {
    public static void main(String[] args) throws Exception {
        System.setProperty("starsector.browserJaninoNegativeCache", "false");
        LoadingUtils.calls = 0;
        ooOo stock = new ooOo();
        if (stock.findResource("missing.java") != null
                || stock.findResource("missing.java") != null
                || LoadingUtils.calls != 2) {
            throw new AssertionError("disabled cache changed stock miss behavior calls=" + LoadingUtils.calls);
        }

        System.setProperty("starsector.browserJaninoNegativeCache", "true");
        LoadingUtils.calls = 0;
        ooOo cached = new ooOo();
        if (cached.findResource("missing.java") != null
                || cached.findResource("missing.java") != null
                || LoadingUtils.calls != 1) {
            throw new AssertionError("repeated miss was not memoized calls=" + LoadingUtils.calls);
        }
        if (cached.findResource("present.java") == null
                || cached.findResource("present.java") == null
                || LoadingUtils.calls != 3) {
            throw new AssertionError("successful source lookup was cached or altered calls=" + LoadingUtils.calls);
        }
        System.out.println("TestJaninoNegativeSourceCache: OK disabledMissReads=2 cachedMissReads=1 successfulReads=2");
    }
}
