import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BrowserTiledTerrainCompat;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;

/** Verifies the Java-8-compatible chunked Base64 transport used by game save codecs. */
public final class VerifyBrowserChunkedBase64Transport {
    public static void main(String[] args) throws Exception {
        verifyLength(0, 1L);
        verifyLength(1, 2L);
        verifyLength(99, 3L);
        verifyLength(100, 4L);
        verifyLength(101, 5L);
        verifyLength(1066, 6L);
        verifyLength(100000, 7L);
        System.out.println("Browser chunked Base64 transport verified");
    }

    private static void verifyLength(int length, long seed) throws Exception {
        byte[] raw = new byte[length];
        new Random(seed).nextBytes(raw);
        byte[] compressed = deflate(raw);
        String encoded = encodeStockChunks(compressed);
        byte[] restored = BrowserTiledTerrainCompat.decodeStockChunkedCompressedBytes(encoded);
        if (!Arrays.equals(compressed, restored)) {
            throw new AssertionError("chunked transport differs length=" + length);
        }
        System.out.println("chunked transport raw=" + length
                + " compressed=" + compressed.length + " encodedChars=" + encoded.length());
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater();
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            while (!deflater.finished()) {
                int read = deflater.deflate(buffer);
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static String encodeStockChunks(byte[] compressed) {
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < compressed.length; offset += 100) {
            int end = Math.min(offset + 100, compressed.length);
            result.append(BaseTiledTerrain.toHexString(Arrays.copyOfRange(compressed, offset, end)));
        }
        return result.toString();
    }
}
