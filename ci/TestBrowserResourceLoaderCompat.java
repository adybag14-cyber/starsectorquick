import com.fs.starfarer.loading.BrowserResourceLoaderCompat;

public final class TestBrowserResourceLoaderCompat {
    private static final String PROPERTY = "starsector.browserResourceLoaderThreads";

    public static void main(String[] args) {
        System.clearProperty(PROPERTY);
        expect(2, BrowserResourceLoaderCompat.resolveWorkerCount(2), "stock fallback");
        System.setProperty(PROPERTY, "4");
        expect(4, BrowserResourceLoaderCompat.resolveWorkerCount(2), "four workers");
        System.setProperty(PROPERTY, "0");
        expect(2, BrowserResourceLoaderCompat.resolveWorkerCount(2), "non-positive fallback");
        System.setProperty(PROPERTY, "99");
        expect(8, BrowserResourceLoaderCompat.resolveWorkerCount(2), "upper clamp");
        System.clearProperty(PROPERTY);
        System.out.println("Browser resource-loader worker policy verified");
    }

    private static void expect(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
