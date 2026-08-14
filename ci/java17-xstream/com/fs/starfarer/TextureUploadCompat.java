package com.fs.starfarer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Canonicalizes Java ImageIO byte-backed rasters into browser-friendly upload buffers. */
public final class TextureUploadCompat {
    private static final int[] BGR_BAND_OFFSETS = {2, 1, 0};
    private static final int[] ABGR_BAND_OFFSETS = {3, 2, 1, 0};

    private TextureUploadCompat() {}

    /** Prepared equivalent of TextureLoader's stock per-pixel buffer + color analysis. */
    public static final class PreparedTexture {
        private final ByteBuffer buffer;
        private final Color averageColor;
        private final Color medianColor;
        private final Color accentColor;

        private PreparedTexture(ByteBuffer buffer, Color averageColor, Color medianColor, Color accentColor) {
            this.buffer = buffer;
            this.averageColor = averageColor;
            this.medianColor = medianColor;
            this.accentColor = accentColor;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public Color getAverageColor() {
            return averageColor;
        }

        public Color getMedianColor() {
            return medianColor;
        }

        public Color getAccentColor() {
            return accentColor;
        }
    }

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

    /**
     * Reproduces TextureLoader's stock power-of-two padding, vertical flip,
     * transparent-pixel handling and three derived colors using primitive arrays
     * instead of Raster.getPixel() and indexed direct-buffer writes per pixel.
     */
    public static PreparedTexture prepareTexture(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int paddedWidth = nextPowerOfTwo(width);
        final int paddedHeight = nextPowerOfTwo(height);
        final boolean hasAlpha = image.getColorModel().hasAlpha();
        final int channels = hasAlpha ? 4 : 3;
        final int capacity = Math.multiplyExact(Math.multiplyExact(paddedWidth, paddedHeight), channels);

        DataBuffer dataBuffer = image.getRaster().getDataBuffer();
        byte[] raw = dataBuffer instanceof DataBufferByte ? ((DataBufferByte) dataBuffer).getData() : null;
        byte[] rgba = canonicalizeImageBytes(raw, image);
        byte[] packed = new byte[capacity];

        float sumR = 0f;
        float sumG = 0f;
        float sumB = 0f;
        float count = 0f;
        float[] histR = new float[256];
        float[] histG = new float[256];
        float[] histB = new float[256];

        for (int y = 0; y < height; y++) {
            int sourceRow = height - y - 1;
            int src = sourceRow * width * 4;
            int dst = y * paddedWidth * channels;
            for (int x = 0; x < width; x++, src += 4, dst += channels) {
                int r = rgba[src] & 0xff;
                int g = rgba[src + 1] & 0xff;
                int b = rgba[src + 2] & 0xff;
                int a = rgba[src + 3] & 0xff;
                if (hasAlpha && a == 0) {
                    continue;
                }

                packed[dst] = (byte) r;
                packed[dst + 1] = (byte) g;
                packed[dst + 2] = (byte) b;
                if (hasAlpha) packed[dst + 3] = (byte) a;

                sumR += r;
                sumG += g;
                sumB += b;
                histR[r] += 1f;
                histG[g] += 1f;
                histB[b] += 1f;
                count += 1f;
            }
        }

        Color average = Color.white;
        Color median = Color.white;
        Color accent = Color.white;
        if (count > 0f) {
            average = new Color(
                    clamp((int) (sumR / count)),
                    clamp((int) (sumG / count)),
                    clamp((int) (sumB / count)),
                    255);
            float half = count * 0.5f;
            median = new Color(
                    clamp((int) percentileLow(histR, half)),
                    clamp((int) percentileLow(histG, half)),
                    clamp((int) percentileLow(histB, half)),
                    255);
            accent = new Color(
                    clamp((int) weightedHigh(histR, count)),
                    clamp((int) weightedHigh(histG, count)),
                    clamp((int) percentileLow(histB, count)),
                    255);
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
        buffer.put(packed);
        buffer.position(0);
        buffer.limit(capacity);
        return new PreparedTexture(buffer, average, median, accent);
    }

    private static int nextPowerOfTwo(int value) {
        int out = 2;
        while (out < value) out *= 2;
        return out;
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }

    private static float percentileLow(float[] histogram, float threshold) {
        float accumulated = 0f;
        for (int i = 0; i <= 255; i++) {
            accumulated += histogram[i];
            if (accumulated >= threshold) return i;
        }
        return 0f;
    }

    private static float weightedHigh(float[] histogram, float threshold) {
        float consumed = 0f;
        float weighted = 0f;
        for (int i = 255; i >= 0; i--) {
            float available = histogram[i];
            float take = available;
            if (consumed + available > threshold) take = threshold - consumed;
            consumed += take;
            weighted += i * take;
            if (consumed >= threshold) break;
        }
        return consumed > 0f ? weighted / consumed : 0f;
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
