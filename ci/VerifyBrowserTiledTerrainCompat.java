import java.util.Random;
import javax.xml.bind.DatatypeConverter;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BrowserTiledTerrainCompat;

public final class VerifyBrowserTiledTerrainCompat {
    private static final String STOCK_NEBULA_PAYLOAD =
            "eJxtVV1oHFUU/u6dycwkrrsbu5g1bLPTkoeoQVatuhrJzmrAF0EffFCoOkX8QQQD+hBsbK+N1opFhIL6ICQPChYREZ98cae0"
            + "FIvgk6igTSbQFx8kCwpum82O59z5LXhh7t5vzg==ued8851z72IFenhCmY3KSQsbgAiqJozTnZnKuUFLAeYX5CAv3j9lbfVb"
            + "DqTlMLZ3z9hRn/dWAp4/+urHd//VuKYjWm9eXYtGyMd3g7WtYQGf6a89ezleSoOmTv/815saTqMChQ==zqD7bSQpP26iR6Ey"
            + "Et9EbCZnE/DFqvhVY/tzCBH4YoiezrcQQCC8C8cO9P4Gcb2bt/Q7WBQ71zjZ0ZDmlWYwI6IhRZJPeeRfn/JtHD+MEhptLYit"
            + "DEQ/o4HFVTguZVEi2LuMKg==Zlc5qGlfAq4NURWtFR+M/1TY1fyfCSneQ/a2wvkLjFdJUtJrz8PVTxk/zdMJI/oQP7Ae4kXG"
            + "yogUzp1lfs8xP4jhENunebXM04QYAMHH8CDdVEERsm7ys0zSI4Km0qMxsIdaKg==OH6M76MEcwX9m1EAqr6jwST5bwXV6dR4"
            + "ENi3E7Rsjq9fjOPOnWBftvnxQ2o5Uk2qfMImCttH1Y39jFqk6gvKGWAutavSgpz4JwvQUVbTMl7LcFnZN1jY1tJr9kqWbbyc"
            + "mn3hwQ==suSlnH4gLkydaOfY84KG0riUfaF68DpsqGL/MUvqF8es5i90/9RzvMhTzcUBxa9vBuuB5MjU0QxFATv4INSLP9Kk"
            + "j4SKl2/zRB2LF3zN7zBPFj3Pa7nEPM+s8v7+S1QN8X2Kfw==Dx9guybBmQ76OSk+GE6g8bHE36hp/FaQYGlqc1eTMKgV3tGr"
            + "OIiUXDrO4mZqGNQvJt8A8X4hKWsJG62EvxR4n77Sz+Vxv6TzENdL/wQDbqfbMru6h8NqkWB5UN5fsVB6BLRjDw==s4pCvJKE"
            + "847Hi/kUd9AiP+5GQiZEDy23HosHr05dhlvdJ2NfqBrEL2jn5WtBbtLhL5RT/EbFStKHKEPs4o20+iGZrRH6VSkSeiTaiPyl"
            + "SF2syoh6CuJUDB1UItQ2hFyKj8gsKg==PUhP4IpI8jfXMbnEoVkiVcX+dcilpEHgVDG2Dosa5YmU32PrPNcbKX51E14VppUS"
            + "OPmT7s7sBqhHeNihoqX49jtgTMMIEmic3YP4hK6JBNtzPXB5B4mC2G6i+x5wKJXHbXLdnQ==WxI85t1LhVcy00+tcF+LHL/O"
            + "QKhs/7I2eplDi8hJJPQlXD/WNR66PY2NZDFBTzc3JtcOnQ6Zhnd06hkUTjThMv01xBHin3JO18UY27OA9YR6+sovEsH4pIt8"
            + "L8cO0OWFxQ423Q==gF4aqJy1KEnZSMnxaKfXR36pVEvUEmb2QbBOFZKo+KSpIpGCotw0nNHD9SO3LyX+Av83HMybRTwORxWx"
            + "+R/B0/SP";

    public static void main(String[] args) throws Exception {
        verifyFastCase(1, 1, 1L);
        verifyFastCase(7, 5, 2L);
        verifyFastCase(130, 130, 3L);
        verifyFastCase(810, 500, 4L);
        verifyStockFallback(7, 5, 5L);
        verifyStockFallback(128, 128, 6L);
        verifyExportedNebulaPayload();
        verifyMalformedStockPayloadFails();
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


    private static void verifyExportedNebulaPayload() throws Exception {
        byte[] compressed = DatatypeConverter.parseBase64Binary(STOCK_NEBULA_PAYLOAD);
        if (compressed.length != 1066) {
            throw new AssertionError("exported nebula compressed length=" + compressed.length);
        }
        if ((compressed[0] & 0xFF) != 0x78 || (compressed[1] & 0xFF) != 0x9C) {
            throw new AssertionError("exported nebula zlib header mismatch");
        }
        int[][] tiles = BrowserTiledTerrainCompat.decodeTilesFast(
                STOCK_NEBULA_PAYLOAD, 128, 128);
        int occupied = 0;
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                if (tiles[x][y] >= 0) occupied++;
            }
        }
        if (occupied != 2064) {
            throw new AssertionError("exported nebula occupied=" + occupied);
        }
        System.out.println("exported nebula payload compressed=1066 occupied=" + occupied);
    }

    private static void verifyMalformedStockPayloadFails() throws Exception {
        try {
            BrowserTiledTerrainCompat.decodeTilesFast("AAAA", 8, 8);
            throw new AssertionError("malformed stock tiled-terrain payload unexpectedly decoded");
        } catch (java.util.zip.DataFormatException expected) {
            System.out.println("malformed stock tiled-terrain payload rejected: " + expected.getMessage());
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
