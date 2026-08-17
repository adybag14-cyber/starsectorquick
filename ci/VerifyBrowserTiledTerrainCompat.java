import java.util.Random;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BrowserTiledTerrainCompat;

public final class VerifyBrowserTiledTerrainCompat {
    public static void main(String[] args) throws Exception {
        verifyFastCase(1, 1, 1L);
        verifyFastCase(7, 5, 2L);
        verifyFastCase(130, 130, 3L);
        verifyFastCase(810, 500, 4L);
        verifyStockFallback(7, 5, 5L);
        System.out.println("Browser tiled-terrain transient codec verified");
    }

    private static void verifyFastCase(int width, int height, long seed) throws Exception {
        int[][] source = randomOccupancy(width, height, seed);
        long started = System.nanoTime();
        String fast = BrowserTiledTerrainCompat.encodeTilesFast(source);
        int[][] fastRoundTrip = BrowserTiledTerrainCompat.decodeTilesFast(fast, width, height);
        assertOccupancy(source, fastRoundTrip, "fast round-trip");
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        System.out.println("fast codec " + width + "x" + height + " elapsedMs=" + elapsedMs);
    }

    private static void verifyStockFallback(int width, int height, long seed) throws Exception {
        int[][] source = randomOccupancy(width, height, seed);
        String stock = BaseTiledTerrain.encodeTiles(source);
        int[][] stockThroughCompat = BrowserTiledTerrainCompat.decodeTilesFast(stock, width, height);
        assertOccupancy(source, stockThroughCompat, "stock fallback");
    }

    private static int[][] randomOccupancy(int width, int height, long seed) {
        Random random = new Random(seed);
        int[][] source = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                source[x][y] = random.nextBoolean() ? 1 : -1;
            }
        }
        return source;
    }

    private static void assertOccupancy(int[][] expected, int[][] actual, String label) {
        if (actual.length != expected.length || actual[0].length != expected[0].length) {
            throw new AssertionError(label + " dimensions differ");
        }
        for (int x = 0; x < expected.length; x++) {
            for (int y = 0; y < expected[0].length; y++) {
                boolean a = expected[x][y] >= 0;
                boolean b = actual[x][y] >= 0;
                if (a != b) {
                    throw new AssertionError(label + " mismatch x=" + x + " y=" + y);
                }
            }
        }
    }
}
