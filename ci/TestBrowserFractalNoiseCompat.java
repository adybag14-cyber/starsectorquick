public final class TestBrowserFractalNoiseCompat {
    public static void main(String[] args) {
        double[][] cases = {
            {1.0d, -2147483648d}, {1.0d, -17d}, {1.0d, 0d}, {1.0d, 1d},
            {1.0d, 42d}, {1.0d, 2147483647d}, {0.5d, 9d}, {2.0d, -5d},
            {-1.0d, 7d}, {3.25d, 4d}
        };
        for (double[] item : cases) {
            double expected = Math.pow(item[0], item[1]);
            double actual = com.fs.starfarer.api.util.BrowserFractalNoiseCompat.pow(item[0], item[1]);
            if (Double.doubleToRawLongBits(expected) != Double.doubleToRawLongBits(actual)) {
                throw new AssertionError("pow mismatch base=" + item[0] + " exponent=" + item[1]
                        + " expected=" + expected + " actual=" + actual);
            }
        }
        System.out.println("Browser fractal-noise pow semantics verified enabled="
                + Boolean.getBoolean("starsector.browserQuickResourceLoad"));
    }
}
