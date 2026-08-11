package com.fs.starfarer;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.util.Arrays;

/** Canonicalizes Java ImageIO byte-backed rasters into OpenGL RGBA8 order. */
public final class TextureUploadCompat {
    private static final int[] BGR_BAND_OFFSETS = {2, 1, 0};
    private static final int[] ABGR_BAND_OFFSETS = {3, 2, 1, 0};

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

        if (raw != null && type == BufferedImage.TYPE_3BYTE_BGR
                && isTightlyPackedByteRaster(image, raw, 3, BGR_BAND_OFFSETS)) {
            for (int i = 0, src = 0, dst = 0; i < pixels; i++, src += 3, dst += 4) {
                out[dst] = raw[src + 2];
                out[dst + 1] = raw[src + 1];
                out[dst + 2] = raw[src];
                out[dst + 3] = (byte) 0xff;
            }
            return out;
        }

        if (raw != null && type == BufferedImage.TYPE_4BYTE_ABGR
                && isTightlyPackedByteRaster(image, raw, 4, ABGR_BAND_OFFSETS)) {
            for (int i = 0, src = 0, dst = 0; i < pixels; i++, src += 4, dst += 4) {
                out[dst] = raw[src + 3];
                out[dst + 1] = raw[src + 2];
                out[dst + 2] = raw[src + 1];
                out[dst + 3] = raw[src];
            }
            return out;
        }

        // Child rasters can keep the parent's backing array, scanline stride and
        // non-zero sample-model translation. Reading those bytes from offset zero
        // uploads an unrelated region, so use the logical pixels for any layout
        // that is not the exact packed ImageIO form handled above.
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

    private static boolean isTightlyPackedByteRaster(BufferedImage image, byte[] raw,
                                                      int pixelStride,
                                                      int[] expectedBandOffsets) {
        Raster raster = image.getRaster();
        DataBuffer dataBuffer = raster.getDataBuffer();
        SampleModel sampleModel = raster.getSampleModel();
        if (!(dataBuffer instanceof DataBufferByte)
                || !(sampleModel instanceof ComponentSampleModel)) {
            return false;
        }
        DataBufferByte bytes = (DataBufferByte) dataBuffer;
        ComponentSampleModel components = (ComponentSampleModel) sampleModel;
        int width = image.getWidth();
        int height = image.getHeight();
        long required = (long) width * (long) height * pixelStride;
        if (required > raw.length || bytes.getNumBanks() != 1 || bytes.getOffset() != 0
                || bytes.getData() != raw || raster.getMinX() != 0 || raster.getMinY() != 0
                || raster.getSampleModelTranslateX() != 0
                || raster.getSampleModelTranslateY() != 0
                || components.getPixelStride() != pixelStride
                || components.getScanlineStride() != width * pixelStride
                || !Arrays.equals(components.getBandOffsets(), expectedBandOffsets)) {
            return false;
        }
        int[] bankIndices = components.getBankIndices();
        for (int bank : bankIndices) {
            if (bank != 0) return false;
        }
        return true;
    }
}
