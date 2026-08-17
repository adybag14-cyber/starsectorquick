import java.util.Random;

public final class TestBrowserEconomyRandomCompat {
    private static final long[] SEEDS = {
        Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L, 2L, 17L, 42L,
        0x5DEECE66DL, 0x123456789ABCDEFL, -0x123456789ABCDEFL,
        2147483647L, -2147483648L, 0x7fffffff00000000L
    };

    public static void main(String[] args) {
        for (long seed : SEEDS) verify(seed);
        long x = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < 10000; i++) {
            x ^= x << 13;
            x ^= x >>> 7;
            x ^= x << 17;
            verify(x);
        }
        System.out.println("Browser economy Random compatibility verified enabled="
                + Boolean.getBoolean("starsector.browserQuickResourceLoad"));
    }

    private static void verify(long seed) {
        Random expected = new Random(seed);
        Random actual = com.fs.starfarer.campaign.econ.reach.BrowserEconomyRandomCompat.acquire(seed);
        for (int i = 0; i < 4; i++) {
            float a = expected.nextFloat();
            float b = actual.nextFloat();
            if (Float.floatToRawIntBits(a) != Float.floatToRawIntBits(b)) {
                throw new AssertionError("nextFloat mismatch seed=" + seed + " index=" + i
                        + " expected=" + a + " actual=" + b);
            }
        }
    }
}
