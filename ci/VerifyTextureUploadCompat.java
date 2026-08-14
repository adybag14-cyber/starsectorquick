import com.fs.starfarer.TextureUploadCompat;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
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

    private static byte[] remaining(ByteBuffer input) {
        ByteBuffer copy = input.duplicate();
        copy.position(0);
        byte[] out = new byte[copy.remaining()];
        copy.get(out);
        return out;
    }

    private static void assertColor(String label, Color actual, Color expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError(label + " actual=" + actual + " expected=" + expected);
        }
    }

    private static TextureUploadCompat.PreparedTexture reference(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int paddedWidth = 2;
        int paddedHeight = 2;
        while (paddedWidth < width) paddedWidth *= 2;
        while (paddedHeight < height) paddedHeight *= 2;
        boolean alpha = image.getColorModel().hasAlpha();
        int channels = alpha ? 4 : 3;
        byte[] out = new byte[paddedWidth * paddedHeight * channels];
        float sumR = 0f, sumG = 0f, sumB = 0f, count = 0f;
        float[] hr = new float[256], hg = new float[256], hb = new float[256];
        int[] pixel = new int[alpha ? 4 : 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.getRaster().getPixel(x, height - y - 1, pixel);
                if (alpha && pixel[3] == 0) continue;
                int dst = (y * paddedWidth + x) * channels;
                out[dst] = (byte) pixel[0];
                out[dst + 1] = (byte) pixel[1];
                out[dst + 2] = (byte) pixel[2];
                if (alpha) out[dst + 3] = (byte) pixel[3];
                sumR += pixel[0]; sumG += pixel[1]; sumB += pixel[2];
                hr[pixel[0]] += 1f; hg[pixel[1]] += 1f; hb[pixel[2]] += 1f; count += 1f;
            }
        }
        Color avg = Color.white, median = Color.white, accent = Color.white;
        if (count > 0f) {
            avg = new Color((int)(sumR/count), (int)(sumG/count), (int)(sumB/count), 255);
            float half = count * 0.5f;
            median = new Color((int)high(hr,half), (int)high(hg,half), (int)high(hb,half), 255);
            accent = new Color((int)low(hr,half), (int)low(hg,half), (int)high(hb,count), 255);
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(out.length);
        buffer.put(out).position(0);
        return makeReference(buffer, avg, median, accent, paddedWidth, paddedHeight, !alpha && width == 2048 && height == 2048);
    }

    private static TextureUploadCompat.PreparedTexture makeReference(ByteBuffer buffer, Color avg, Color median, Color accent,
                                                                       int paddedWidth, int paddedHeight, boolean reusable2048Rgb) {
        try {
            java.lang.reflect.Constructor<?> c = TextureUploadCompat.PreparedTexture.class
                    .getDeclaredConstructor(ByteBuffer.class, Color.class, Color.class, Color.class,
                            int.class, int.class, boolean.class);
            c.setAccessible(true);
            return (TextureUploadCompat.PreparedTexture)c.newInstance(
                    buffer, avg, median, accent, paddedWidth, paddedHeight, reusable2048Rgb);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static float low(float[] h, float threshold) {
        float a=0f; for(int i=0;i<=255;i++){ a+=h[i]; if(a>=threshold) return i; } return 0f;
    }
    private static float high(float[] h, float threshold) {
        float a=0f,w=0f; for(int i=255;i>=0;i--){ float v=h[i],take=v; if(a+v>threshold) take=threshold-a; a+=take; w+=i*take; if(a>=threshold) break; } return a>0f?w/a:0f;
    }

    private static void assertPrepared(String label, BufferedImage image) {
        TextureUploadCompat.PreparedTexture actual = TextureUploadCompat.prepareTexture(image);
        TextureUploadCompat.PreparedTexture expected = reference(image);
        assertBytes(label + " buffer", remaining(actual.getBuffer()), remaining(expected.getBuffer()));
        assertColor(label + " average", actual.getAverageColor(), expected.getAverageColor());
        assertColor(label + " median", actual.getMedianColor(), expected.getMedianColor());
        assertColor(label + " accent", actual.getAccentColor(), expected.getAccentColor());
        if (actual.getPaddedWidth() != expected.getPaddedWidth() || actual.getPaddedHeight() != expected.getPaddedHeight())
            throw new AssertionError(label + " padded dimensions actual=" + actual.getPaddedWidth() + "x" + actual.getPaddedHeight()
                    + " expected=" + expected.getPaddedWidth() + "x" + expected.getPaddedHeight());
        if (actual.isReusable2048Rgb() != expected.isReusable2048Rgb()) throw new AssertionError(label + " reusable flag");
        if (actual.getBuffer().position() != 0) throw new AssertionError(label + " buffer position");
    }

    public static void main(String[] args) {
        BufferedImage bgr = new BufferedImage(3, 2, BufferedImage.TYPE_3BYTE_BGR);
        bgr.setRGB(0,0,0xff112233); bgr.setRGB(1,0,0xffaa5500); bgr.setRGB(2,0,0xff10f020);
        bgr.setRGB(0,1,0xffcc8844); bgr.setRGB(1,1,0xff010203); bgr.setRGB(2,1,0xff8090a0);
        byte[] bgrRaw = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        assertBytes("TYPE_3BYTE_BGR canonical",
                TextureUploadCompat.canonicalizeImageBytes(bgrRaw, bgr),
                bytes(0x11,0x22,0x33,0xff, 0xaa,0x55,0x00,0xff, 0x10,0xf0,0x20,0xff,
                      0xcc,0x88,0x44,0xff, 0x01,0x02,0x03,0xff, 0x80,0x90,0xa0,0xff));
        assertPrepared("TYPE_3BYTE_BGR", bgr);

        BufferedImage abgr = new BufferedImage(3, 2, BufferedImage.TYPE_4BYTE_ABGR);
        abgr.setRGB(0,0,0x00112233); abgr.setRGB(1,0,0x80aa5500); abgr.setRGB(2,0,0xff10f020);
        abgr.setRGB(0,1,0x40cc8844); abgr.setRGB(1,1,0x00010203); abgr.setRGB(2,1,0xc08090a0);
        assertPrepared("TYPE_4BYTE_ABGR", abgr);

        BufferedImage bgrParent = new BufferedImage(5,4,BufferedImage.TYPE_3BYTE_BGR);
        for(int y=0;y<4;y++) for(int x=0;x<5;x++) bgrParent.setRGB(x,y,0xff000000 | ((x*41+y*7)&255)<<16 | ((x*13+y*37)&255)<<8 | ((x*17+y*29)&255));
        assertPrepared("BGR child", bgrParent.getSubimage(1,1,3,2));

        BufferedImage abgrParent = new BufferedImage(5,4,BufferedImage.TYPE_4BYTE_ABGR);
        for(int y=0;y<4;y++) for(int x=0;x<5;x++) abgrParent.setRGB(x,y,((x+y)%3==0?0:0x80)<<24 | ((x*41+y*7)&255)<<16 | ((x*13+y*37)&255)<<8 | ((x*17+y*29)&255));
        assertPrepared("ABGR child", abgrParent.getSubimage(1,1,3,2));

        BufferedImage premultiplied = new BufferedImage(3,2,BufferedImage.TYPE_INT_ARGB_PRE);
        premultiplied.setRGB(0,0,0x40102030); premultiplied.setRGB(1,0,0x8080a0c0); premultiplied.setRGB(2,0,0xff123456);
        premultiplied.setRGB(0,1,0x00010203); premultiplied.setRGB(1,1,0xc0abcdef); premultiplied.setRGB(2,1,0x7f557799);
        assertPrepared("TYPE_INT_ARGB_PRE fallback", premultiplied);

        BufferedImage gray = new BufferedImage(3,2,BufferedImage.TYPE_BYTE_GRAY);
        gray.getRaster().setSample(0,0,0,12); gray.getRaster().setSample(1,0,0,128); gray.getRaster().setSample(2,0,0,240);
        gray.getRaster().setSample(0,1,0,64); gray.getRaster().setSample(1,1,0,192); gray.getRaster().setSample(2,1,0,255);
        assertPrepared("TYPE_BYTE_GRAY fallback", gray);

        BufferedImage transparent = new BufferedImage(3,2,BufferedImage.TYPE_4BYTE_ABGR);
        transparent.setRGB(0,0,0x00112233); transparent.setRGB(1,1,0x00abcdef);
        assertPrepared("fully transparent ABGR", transparent);

        System.out.println("Verified canonical and bulk texture upload conversion.");
    }
}
