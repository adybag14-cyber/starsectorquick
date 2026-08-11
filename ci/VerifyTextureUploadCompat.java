import com.fs.starfarer.TextureUploadCompat;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;

public final class VerifyTextureUploadCompat {
    private VerifyTextureUploadCompat() {}

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) out[i] = (byte) values[i];
        return out;
    }

    private static void assertBytes(String label, byte[] actual, byte[] expected) {
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(label + " actual=" + Arrays.toString(actual)
                    + " expected=" + Arrays.toString(expected));
        }
    }

    public static void main(String[] args) {
        BufferedImage bgr = new BufferedImage(2, 1, BufferedImage.TYPE_3BYTE_BGR);
        bgr.setRGB(0, 0, 0xff112233);
        bgr.setRGB(1, 0, 0xffaa5500);
        byte[] bgrRaw = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        assertBytes("TYPE_3BYTE_BGR",
                TextureUploadCompat.canonicalizeImageBytes(bgrRaw, bgr),
                bytes(0x11, 0x22, 0x33, 0xff, 0xaa, 0x55, 0x00, 0xff));

        BufferedImage abgr = new BufferedImage(2, 1, BufferedImage.TYPE_4BYTE_ABGR);
        abgr.setRGB(0, 0, 0x80112233);
        abgr.setRGB(1, 0, 0x40aa5500);
        byte[] abgrRaw = ((DataBufferByte) abgr.getRaster().getDataBuffer()).getData();
        assertBytes("TYPE_4BYTE_ABGR",
                TextureUploadCompat.canonicalizeImageBytes(abgrRaw, abgr),
                bytes(0x11, 0x22, 0x33, 0x80, 0xaa, 0x55, 0x00, 0x40));

        BufferedImage fallback = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        fallback.setRGB(0, 0, 0x7f123456);
        assertBytes("fallback",
                TextureUploadCompat.canonicalizeImageBytes(null, fallback),
                bytes(0x12, 0x34, 0x56, 0x7f));

        System.out.println("Verified canonical RGBA texture upload conversion.");
    }
}
