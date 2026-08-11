package com.fs.starfarer;

import java.awt.image.BufferedImage;

/** Canonicalizes Java ImageIO byte-backed rasters into OpenGL RGBA8 order. */
public final class TextureUploadCompat {
    private TextureUploadCompat() {}

    public static byte[] canonicalizeImageBytes(byte[] raw, BufferedImage image) {
        if (image == null) {
            return raw == null ? new byte[0] : raw;
        }
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int pixels = Math.max(0, width * height);
        final byte[] out = new byte[pixels * 4];
        final int type = image.getType();

        if (raw != null && type == BufferedImage.TYPE_3BYTE_BGR && raw.length >= pixels * 3) {
            for (int i = 0, src = 0, dst = 0; i < pixels; i++, src += 3, dst += 4) {
                out[dst] = raw[src + 2];
                out[dst + 1] = raw[src + 1];
                out[dst + 2] = raw[src];
                out[dst + 3] = (byte) 0xff;
            }
            return out;
        }

        if (raw != null && type == BufferedImage.TYPE_4BYTE_ABGR && raw.length >= pixels * 4) {
            for (int i = 0, src = 0, dst = 0; i < pixels; i++, src += 4, dst += 4) {
                out[dst] = raw[src + 3];
                out[dst + 1] = raw[src + 2];
                out[dst + 2] = raw[src + 1];
                out[dst + 3] = raw[src];
            }
            return out;
        }

        // Conservative fallback for any future/custom BufferedImage layout.
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0, dst = 0; i < argb.length; i++, dst += 4) {
            int pixel = argb[i];
            out[dst] = (byte) ((pixel >>> 16) & 0xff);
            out[dst + 1] = (byte) ((pixel >>> 8) & 0xff);
            out[dst + 2] = (byte) (pixel & 0xff);
            out[dst + 3] = (byte) ((pixel >>> 24) & 0xff);
        }
        return out;
    }
}
