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

        BufferedImage bgrParent = new BufferedImage(4, 3, BufferedImage.TYPE_3BYTE_BGR);
        bgrParent.setRGB(1, 1, 0xff102030);
        bgrParent.setRGB(2, 1, 0xff405060);
        bgrParent.setRGB(1, 2, 0xff708090);
        bgrParent.setRGB(2, 2, 0xffa0b0c0);
        BufferedImage bgrChild = bgrParent.getSubimage(1, 1, 2, 2);
        byte[] bgrChildRaw = ((DataBufferByte) bgrChild.getRaster().getDataBuffer()).getData();
        assertBytes("TYPE_3BYTE_BGR child raster",
                TextureUploadCompat.canonicalizeImageBytes(bgrChildRaw, bgrChild),
                bytes(0x10, 0x20, 0x30, 0xff, 0x40, 0x50, 0x60, 0xff,
                        0x70, 0x80, 0x90, 0xff, 0xa0, 0xb0, 0xc0, 0xff));

        BufferedImage abgrParent = new BufferedImage(4, 3, BufferedImage.TYPE_4BYTE_ABGR);
        abgrParent.setRGB(1, 1, 0x80102030);
        abgrParent.setRGB(2, 1, 0x60405060);
        abgrParent.setRGB(1, 2, 0x40708090);
        abgrParent.setRGB(2, 2, 0x20a0b0c0);
        BufferedImage abgrChild = abgrParent.getSubimage(1, 1, 2, 2);
        byte[] abgrChildRaw = ((DataBufferByte) abgrChild.getRaster().getDataBuffer()).getData();
        assertBytes("TYPE_4BYTE_ABGR child raster",
                TextureUploadCompat.canonicalizeImageBytes(abgrChildRaw, abgrChild),
                bytes(0x10, 0x20, 0x30, 0x80, 0x40, 0x50, 0x60, 0x60,
                        0x70, 0x80, 0x90, 0x40, 0xa0, 0xb0, 0xc0, 0x20));

        System.out.println("Verified canonical RGBA texture upload conversion.");
    }
}
