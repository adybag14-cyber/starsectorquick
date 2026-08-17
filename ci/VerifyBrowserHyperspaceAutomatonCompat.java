import com.fs.starfarer.api.impl.campaign.terrain.BrowserHyperspaceAutomatonCompat;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAutomaton;
import java.util.Random;

/** Verifies stock HyperspaceAutomaton save payloads decode with exact 2-bit values. */
public final class VerifyBrowserHyperspaceAutomatonCompat {
    public static void main(String[] args) throws Exception {
        verifyCase(4, 5, 1L);
        verifyCase(128, 128, 2L);
        verifyCase(810, 500, 3L);
        verifyTruncatedPayload();
        System.out.println("Browser hyperspace automaton codec verified");
    }

    private static void verifyCase(int width, int height, long seed) throws Exception {
        int[][] expected = randomCells(width, height, seed);
        String encoded = HyperspaceAutomaton.encodeTiles(expected);
        long started = System.nanoTime();
        int[][] actual = BrowserHyperspaceAutomatonCompat.decodeTilesFast(
                encoded, width, height);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        assertSame(expected, actual, "stock automaton " + width + "x" + height);
        if (width * height >= 128 * 128 && encoded.indexOf('=') < 0) {
            throw new AssertionError("expected padded stock automaton payload");
        }
        System.out.println("hyperspace automaton " + width + "x" + height
                + " encodedChars=" + encoded.length() + " elapsedMs=" + elapsedMs);
    }

    private static void verifyTruncatedPayload() throws Exception {
        int[][] cells = randomCells(128, 128, 4L);
        String encoded = HyperspaceAutomaton.encodeTiles(cells);
        boolean rejected = false;
        try {
            BrowserHyperspaceAutomatonCompat.decodeTilesFast(
                    encoded.substring(0, encoded.length() - 4), 128, 128);
        } catch (java.util.zip.DataFormatException expected) {
            rejected = true;
        }
        if (!rejected) {
            throw new AssertionError("truncated hyperspace automaton payload was not rejected");
        }
    }

    private static int[][] randomCells(int width, int height, long seed) {
        Random random = new Random(seed);
        int[][] cells = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = random.nextInt(3);
            }
        }
        return cells;
    }

    private static void assertSame(int[][] expected, int[][] actual, String label) {
        if (actual.length != expected.length || actual[0].length != expected[0].length) {
            throw new AssertionError(label + " dimensions differ");
        }
        for (int x = 0; x < expected.length; x++) {
            for (int y = 0; y < expected[x].length; y++) {
                if (expected[x][y] != actual[x][y]) {
                    throw new AssertionError(label + " differs at " + x + "," + y
                            + " expected=" + expected[x][y] + " actual=" + actual[x][y]);
                }
            }
        }
    }
}
