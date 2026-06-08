package com.fs.starfarer.F;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;

public class oOOO {
    public oOOO() {}

    public static IntBuffer Zqv(String path) throws IOException {
        return o00000(path, 32, 32);
    }

    public static IntBuffer o00000(String path, int width, int height) throws IOException {
        BufferedImage image = readImage(path);
        return toBuffer(convertPixels(image, width, height), width, height);
    }

    private static BufferedImage readImage(String path) throws IOException {
        InputStream in = openResource(path);
        if (in == null) {
            throw new IOException("Missing cursor resource: " + path);
        }
        try {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Unable to decode cursor image: " + path);
            }
            return image;
        } finally {
            in.close();
        }
    }

    private static InputStream openResource(String path) throws IOException {
        String normalized = path == null ? null : path.startsWith("/") ? path.substring(1) : path;
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }

        try {
            Class<?> loadingUtilsClass = Class.forName("com.fs.starfarer.loading.LoadingUtils");
            Method open = loadingUtilsClass.getDeclaredMethod("void", String.class);
            open.setAccessible(true);
            Object result = open.invoke(null, normalized);
            if (result instanceof InputStream) {
                return (InputStream) result;
            }
        } catch (Throwable ignored) {
        }

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            InputStream contextStream = contextLoader.getResourceAsStream(normalized);
            if (contextStream != null) {
                return contextStream;
            }
        }

        ClassLoader ownLoader = oOOO.class.getClassLoader();
        if (ownLoader != null) {
            InputStream ownStream = ownLoader.getResourceAsStream(normalized);
            if (ownStream != null) {
                return ownStream;
            }
        }

        return ClassLoader.getSystemResourceAsStream(normalized);
    }

    private static int[] convertPixels(BufferedImage image, int width, int height) {
        if (image == null) {
            throw new RuntimeException("problem loading image (most likely cursor)");
        }

        BufferedImage argb = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argb.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        int[] src = new int[width * height];
        argb.getRGB(0, 0, width, height, src, 0, width);
        int[] flipped = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = src[(y * width) + x];
                int alpha = (pixel >>> 24) & 0xFF;
                int target = ((height - y - 1) * width) + x;
                flipped[target] = alpha < 150 ? 0 : (pixel | 0xFF000000);
            }
        }
        return flipped;
    }

    private static IntBuffer toBuffer(int[] pixels, int width, int height) {
        ByteBuffer buffer =
                ByteBuffer.allocateDirect(width * height * Integer.BYTES).order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(pixels);
        intBuffer.flip();
        return intBuffer;
    }
}
