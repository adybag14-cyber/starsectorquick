package com.fs.starfarer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.ColorModel;
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
        private final int paddedWidth;
        private final int paddedHeight;
        private final boolean reusable2048Rgb;

        private PreparedTexture(ByteBuffer buffer, Color averageColor, Color medianColor, Color accentColor,
                                int paddedWidth, int paddedHeight, boolean reusable2048Rgb) {
            this.buffer = buffer;
            this.averageColor = averageColor;
            this.medianColor = medianColor;
            this.accentColor = accentColor;
            this.paddedWidth = paddedWidth;
            this.paddedHeight = paddedHeight;
            this.reusable2048Rgb = reusable2048Rgb;
        }

        public ByteBuffer getBuffer() { return buffer; }
        public Color getAverageColor() { return averageColor; }
        public Color getMedianColor() { return medianColor; }
        public Color getAccentColor() { return accentColor; }
        public int getPaddedWidth() { return paddedWidth; }
        public int getPaddedHeight() { return paddedHeight; }
        public boolean isReusable2048Rgb() { return reusable2048Rgb; }
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

        // Child/custom rasters can keep parent strides/translations or use
        // grayscale/indexed/premultiplied layouts. Convert through the image's
        // ColorModel instead of assuming raster bands are always RGBA.
        Raster raster = image.getRaster();
        ColorModel colorModel = image.getColorModel();
        Object dataElements = null;
        for (int y = 0, dst = 0; y < height; y++) {
            for (int x = 0; x < width; x++, dst += 4) {
                dataElements = raster.getDataElements(x, y, dataElements);
                int pixel = colorModel.getRGB(dataElements);
                out[dst] = (byte) ((pixel >>> 16) & 0xff);
                out[dst + 1] = (byte) ((pixel >>> 8) & 0xff);
                out[dst + 2] = (byte) (pixel & 0xff);
                out[dst + 3] = (byte) ((pixel >>> 24) & 0xff);
            }
        }
        return out;
    }

    /**
     * Reproduces TextureLoader's stock power-of-two padding, vertical flip,
     * transparent-pixel handling and three derived colors using primitive arrays
     * instead of Raster.getPixel() and indexed direct-buffer writes per pixel.
     */
    public static PreparedTexture prepareTexture(BufferedImage image) {
        return prepareTexture(image, null);
    }

    /**
     * Stock-equivalent texture preparation with an optional reusable buffer for
     * the original 2048x2048 opaque-RGB scratch-buffer special case.
     */
    public static PreparedTexture prepareTexture(BufferedImage image, ByteBuffer reusable2048RgbBuffer) {
        if (image == null) throw new IllegalArgumentException("image must not be null");

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int paddedWidth = nextPowerOfTwo(width);
        final int paddedHeight = nextPowerOfTwo(height);
        final boolean hasAlpha = image.getColorModel().hasAlpha();
        final int channels = hasAlpha ? 4 : 3;
        final int capacity = Math.multiplyExact(Math.multiplyExact(paddedWidth, paddedHeight), channels);
        final boolean reusable2048Rgb = !hasAlpha && width == 2048 && height == 2048;

        ByteBuffer buffer;
        if (reusable2048Rgb && reusable2048RgbBuffer != null
                && reusable2048RgbBuffer.capacity() >= capacity) {
            buffer = reusable2048RgbBuffer;
            buffer.clear();
            buffer.limit(capacity);
        } else {
            buffer = ByteBuffer.allocateDirect(capacity);
        }

        float sumR = 0f, sumG = 0f, sumB = 0f, count = 0f;
        float[] histR = new float[256], histG = new float[256], histB = new float[256];
        byte[] row = new byte[Math.multiplyExact(width, channels)];

        DataBuffer dataBuffer = image.getRaster().getDataBuffer();
        byte[] raw = dataBuffer instanceof DataBufferByte ? ((DataBufferByte) dataBuffer).getData() : null;
        int type = image.getType();
        boolean fastBgr = raw != null && type == BufferedImage.TYPE_3BYTE_BGR
                && isTightlyPackedByteRaster(image, raw, 3, BGR_BAND_OFFSETS);
        boolean fastAbgr = raw != null && type == BufferedImage.TYPE_4BYTE_ABGR
                && isTightlyPackedByteRaster(image, raw, 4, ABGR_BAND_OFFSETS);

        if (fastBgr || fastAbgr) {
            final int sourceStride = fastBgr ? 3 : 4;
            for (int y = 0; y < height; y++) {
                if (hasAlpha) Arrays.fill(row, (byte) 0);
                int src = (height - y - 1) * width * sourceStride;
                int dst = 0;
                for (int x = 0; x < width; x++, src += sourceStride, dst += channels) {
                    int r, g, b, a;
                    if (fastBgr) {
                        b = raw[src] & 0xff; g = raw[src + 1] & 0xff; r = raw[src + 2] & 0xff; a = 255;
                    } else {
                        a = raw[src] & 0xff; b = raw[src + 1] & 0xff; g = raw[src + 2] & 0xff; r = raw[src + 3] & 0xff;
                    }
                    if (hasAlpha && a == 0) continue;
                    row[dst] = (byte) r; row[dst + 1] = (byte) g; row[dst + 2] = (byte) b;
                    if (hasAlpha) row[dst + 3] = (byte) a;
                    sumR += r; sumG += g; sumB += b;
                    histR[r] += 1f; histG[g] += 1f; histB[b] += 1f; count += 1f;
                }
                buffer.position(y * paddedWidth * channels);
                buffer.put(row, 0, row.length);
            }
        } else {
            // Custom/premultiplied/indexed/grayscale layouts are uncommon in the
            // shipped runtime, but map them through ColorModel so grayscale+alpha
            // expands gray into RGB and keeps the actual alpha component.
            Raster raster = image.getRaster();
            ColorModel colorModel = image.getColorModel();
            Object dataElements = null;
            for (int y = 0; y < height; y++) {
                if (hasAlpha) Arrays.fill(row, (byte) 0);
                int sourceY = height - y - 1;
                int dst = 0;
                for (int x = 0; x < width; x++, dst += channels) {
                    dataElements = raster.getDataElements(x, sourceY, dataElements);
                    int argb = colorModel.getRGB(dataElements);
                    int a = (argb >>> 24) & 0xff;
                    int r = (argb >>> 16) & 0xff;
                    int g = (argb >>> 8) & 0xff;
                    int b = argb & 0xff;
                    if (hasAlpha && a == 0) continue;
                    row[dst] = (byte) r; row[dst + 1] = (byte) g; row[dst + 2] = (byte) b;
                    if (hasAlpha) row[dst + 3] = (byte) a;
                    sumR += r; sumG += g; sumB += b;
                    histR[r] += 1f; histG[g] += 1f; histB[b] += 1f; count += 1f;
                }
                buffer.position(y * paddedWidth * channels);
                buffer.put(row, 0, row.length);
            }
        }

        Color average = Color.white, median = Color.white, accent = Color.white;
        if (count > 0f) {
            average = new Color(clamp((int) (sumR / count)), clamp((int) (sumG / count)),
                    clamp((int) (sumB / count)), 255);
            float half = count * 0.5f;
            median = new Color(clamp((int) weightedHigh(histR, half)),
                    clamp((int) weightedHigh(histG, half)),
                    clamp((int) weightedHigh(histB, half)), 255);
            accent = new Color(clamp((int) percentileLow(histR, half)),
                    clamp((int) percentileLow(histG, half)),
                    clamp((int) weightedHigh(histB, count)), 255);
        }

        buffer.position(0);
        buffer.limit(capacity);
        return new PreparedTexture(buffer, average, median, accent,
                paddedWidth, paddedHeight, reusable2048Rgb);
    }

    /** Preserve TextureLoader's stock static scratch-buffer reuse without branching in obfuscated bytecode. */
    public static ByteBuffer selectReusable2048Rgb(PreparedTexture prepared, ByteBuffer existing) {
        return prepared != null && prepared.isReusable2048Rgb() ? prepared.getBuffer() : existing;
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
