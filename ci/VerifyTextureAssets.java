import com.fs.starfarer.TextureUploadCompat;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/** Verifies every shipped texture converts exactly to Java's logical RGBA pixels. */
public final class VerifyTextureAssets {
    private VerifyTextureAssets() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: VerifyTextureAssets <graphics-root>");
        }
        Path root = Paths.get(args[0]);
        long images = 0;
        long pixels = 0;
        Map<Integer, Long> types = new TreeMap<Integer, Long>();

        try (Stream<Path> paths = Files.walk(root)) {
            Iterator<Path> iterator = paths.filter(Files::isRegularFile)
                    .filter(VerifyTextureAssets::isImage).iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                BufferedImage image = ImageIO.read(path.toFile());
                if (image == null) {
                    throw new IOException("ImageIO returned null for " + path);
                }
                DataBuffer buffer = image.getRaster().getDataBuffer();
                byte[] raw = buffer instanceof DataBufferByte
                        ? ((DataBufferByte) buffer).getData() : null;
                byte[] rgba = TextureUploadCompat.canonicalizeImageBytes(raw, image);
                int width = image.getWidth();
                int height = image.getHeight();
                int expectedLength = Math.multiplyExact(Math.multiplyExact(width, height), 4);
                if (rgba.length != expectedLength) {
                    throw new AssertionError(path + " output length=" + rgba.length
                            + " expected=" + expectedLength);
                }

                int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
                for (int i = 0, offset = 0; i < argb.length; i++, offset += 4) {
                    int pixel = argb[i];
                    int r = (pixel >>> 16) & 0xff;
                    int g = (pixel >>> 8) & 0xff;
                    int b = pixel & 0xff;
                    int a = (pixel >>> 24) & 0xff;
                    if ((rgba[offset] & 0xff) != r || (rgba[offset + 1] & 0xff) != g
                            || (rgba[offset + 2] & 0xff) != b
                            || (rgba[offset + 3] & 0xff) != a) {
                        throw new AssertionError(path + " pixel=" + i + " actual="
                                + (rgba[offset] & 0xff) + "," + (rgba[offset + 1] & 0xff)
                                + "," + (rgba[offset + 2] & 0xff) + ","
                                + (rgba[offset + 3] & 0xff) + " expected=" + r + "," + g
                                + "," + b + "," + a + " imageType=" + image.getType()
                                + " rawLength=" + (raw == null ? -1 : raw.length));
                    }
                }
                images++;
                pixels += argb.length;
                Long count = types.get(image.getType());
                types.put(image.getType(), count == null ? 1L : count + 1L);
                image.flush();
            }
        }
        System.out.println("Verified texture assets images=" + images + " pixels=" + pixels
                + " imageTypes=" + types);
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
}
