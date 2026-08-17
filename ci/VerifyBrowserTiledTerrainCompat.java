import java.util.Random;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BrowserTiledTerrainCompat;

public final class VerifyBrowserTiledTerrainCompat {
    private static final String STOCK_CHUNKED_PAYLOAD =
                "eJxtVV1oHFUU/u6dycwkrrsbu5g1bLPTkoeoQVatuhrJzmrAF0EffFCoOkX8QQQD+hBsbK+N1opFhIL6ICQPChYREZ98cae0FIvg"
                + "k6igTSbQFx8kCwpum82O59z5LXhh7t5vzg==ued8851z72IFenhCmY3KSQsbgAiqJozTnZnKuUFLAeYX5CAv3j9lbfVbDqTlMLZ3"
                + "z9hRn/dWAp4/+urHd//VuKYjWm9eXYtGyMd3g7WtYQGf6a89ezleSoOmTv/815saTqMChQ==zqD7bSQpP26iR6EyEt9EbCZnE/DF"
                + "qvhVY/tzCBH4YoiezrcQQCC8C8cO9P4Gcb2bt/Q7WBQ71zjZ0ZDmlWYwI6IhRZJPeeRfn/JtHD+MEhptLYitDEQ/o4HFVTguZVEi"
                + "2LuMKg==Zlc5qGlfAq4NURWtFR+M/1TY1fyfCSneQ/a2wvkLjFdJUtJrz8PVTxk/zdMJI/oQP7Ae4kXGyogUzp1lfs8xP4jhENun"
                + "ebXM04QYAMHH8CDdVEERsm7ys0zSI4Km0qMxsIdaKg==OH6M76MEcwX9m1EAqr6jwST5bwXV6dR4ENi3E7Rsjq9fjOPOnWBftvnx"
                + "Q2o5Uk2qfMImCttH1Y39jFqk6gvKGWAutavSgpz4JwvQUVbTMl7LcFnZN1jY1tJr9kqWbbycmn3hwQ==suSlnH4gLkydaOfY84KG"
                + "0riUfaF68DpsqGL/MUvqF8es5i90/9RzvMhTzcUBxa9vBuuB5MjU0QxFATv4INSLP9Kkj4SKl2/zRB2LF3zN7zBPFj3Pa7nEPM+s"
                + "8v7+S1QN8X2Kfw==Dx9guybBmQ76OSk+GE6g8bHE36hp/FaQYGlqc1eTMKgV3tGrOIiUXDrO4mZqGNQvJt8A8X4hKWsJG62EvxR4"
                + "n77Sz+Vxv6TzENdL/wQDbqfbMru6h8NqkWB5UN5fsVB6BLRjDw==s4pCvJKE847Hi/kUd9AiP+5GQiZEDy23HosHr05dhlvdJ2Nf"
                + "qBrEL2jn5WtBbtLhL5RT/EbFStKHKEPs4o20+iGZrRH6VSkSeiTaiPylSF2syoh6CuJUDB1UItQ2hFyKj8gsKg==PUhP4IpI8jfX"
                + "MbnEoVkiVcX+dcilpEHgVDG2Dosa5YmU32PrPNcbKX51E14VppUSOPmT7s7sBqhHeNihoqX49jtgTMMIEmic3YP4hK6JBNtzPXB5"
                + "B4mC2G6i+x5wKJXHbXLdnQ==WxI85t1LhVcy00+tcF+LHL/OQKhs/7I2eplDi8hJJPQlXD/WNR66PY2NZDFBTzc3JtcOnQ6Zhnd0"
                + "6hkUTjThMv01xBHin3JO18UY27OA9YR6+sovEsH4pIt8L8cO0OWFxQ423Q==gF4aqJy1KEnZSMnxaKfXR36pVEvUEmb2QbBOFZKo"
                + "+KSpIpGCotw0nNHD9SO3LyX+Av83HMybRTwORxWx+R/B0/SP";

    public static void main(String[] args) throws Exception {
        verifyFastCase(1, 1, 1L);
        verifyFastCase(7, 5, 2L);
        verifyFastCase(130, 130, 3L);
        verifyFastCase(810, 500, 4L);
        verifyStockFallback(7, 5, 5L);
        verifyCapturedStockChunkedPayload();
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

    private static void verifyCapturedStockChunkedPayload() throws Exception {
        long started = System.nanoTime();
        int[][] decoded = BrowserTiledTerrainCompat.decodeTilesFast(STOCK_CHUNKED_PAYLOAD, 128, 128);
        int occupied = 0;
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < 128 * 128; index++) {
            boolean set = decoded[index % 128][index / 128] >= 0;
            if (set) occupied++;
            hash ^= set ? 1L : 0L;
            hash *= 0x100000001b3L;
        }
        if (occupied != 2064 || hash != 0x2edf4db6bdaa44efL) {
            throw new AssertionError(
                    "captured stock chunked payload mismatch occupied=" + occupied
                            + " hash=" + Long.toHexString(hash));
        }
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        System.out.println("captured stock chunked codec 128x128 elapsedMs=" + elapsedMs);

        boolean truncatedRejected = false;
        try {
            BrowserTiledTerrainCompat.decodeTilesFast(
                    STOCK_CHUNKED_PAYLOAD.substring(0, STOCK_CHUNKED_PAYLOAD.length() - 4), 128, 128);
        } catch (java.util.zip.DataFormatException expected) {
            truncatedRejected = true;
        }
        if (!truncatedRejected) {
            throw new AssertionError("truncated stock tiled-terrain payload was not rejected");
        }
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
