package com.fs.starfarer.api.impl.campaign.terrain;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Browser-only fast path for BaseTiledTerrain tile serialization.
 *
 * <p>Stock BaseTiledTerrain encodes compressed data in 100-byte chunks and
 * Base64-encodes each chunk separately before concatenating the padded strings.
 * Java 8 JAXB's permissive DatatypeConverter accepts those repeated padded blocks,
 * but the Java-17 compatibility shim used by the browser runtime does not preserve
 * that behavior; it truncates the compressed stream at an internal padding marker.
 * BaseTiledTerrain's stock Inflater loop then has no needsInput/no-progress guard.
 * This decoder preserves the exact stock on-disk format, restores the Java-8-style
 * block semantics for this sole JAXB-using game class, and bounds Inflate progress.
 * The browser-raw prefix remains the transient init fast path.
 */
public final class BrowserTiledTerrainCompat {
    private static final java.lang.String PREFIX = "browser-raw-tiles-v1:";

    private BrowserTiledTerrainCompat() {
    }

    public static java.lang.String encodeTilesFast(int[][] tiles) {
        if (tiles == null || tiles.length == 0 || tiles[0] == null) {
            return BaseTiledTerrain.encodeTiles(tiles);
        }
        final int width = tiles.length;
        final int height = tiles[0].length;
        final int total = width * height;
        java.lang.StringBuilder out = new java.lang.StringBuilder(PREFIX.length() + total);
        out.append(PREFIX);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.append(tiles[x][y] >= 0 ? '1' : '0');
            }
        }
        return out.toString();
    }

    public static int[][] decodeTilesFast(java.lang.String encoded, int width, int height)
            throws DataFormatException {
        validateDimensions(width, height);
        if (encoded != null && encoded.startsWith(PREFIX)) {
            return decodeBrowserRaw(encoded, width, height);
        }
        return decodeStockChunked(encoded, width, height);
    }

    private static int[][] decodeBrowserRaw(java.lang.String encoded, int width, int height)
            throws DataFormatException {
        final long expectedLong = (long) PREFIX.length() + (long) width * (long) height;
        if (expectedLong > Integer.MAX_VALUE || encoded.length() != (int) expectedLong) {
            throw new DataFormatException(
                    "Browser tiled-terrain payload size mismatch expected=" + expectedLong
                            + " actual=" + encoded.length());
        }
        int[][] tiles = new int[width][height];
        int index = PREFIX.length();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char value = encoded.charAt(index++);
                if (value == '1') {
                    tiles[x][y] = 1;
                } else if (value == '0') {
                    tiles[x][y] = -1;
                } else {
                    throw new DataFormatException(
                            "Invalid browser tiled-terrain occupancy character at index=" + (index - 1));
                }
            }
        }
        return tiles;
    }

    private static int[][] decodeStockChunked(java.lang.String encoded, int width, int height)
            throws DataFormatException {
        if (encoded == null) {
            throw new DataFormatException("Null stock tiled-terrain payload");
        }
        final long totalLong = (long) width * (long) height;
        if (totalLong > Integer.MAX_VALUE) {
            throw new DataFormatException("Tiled-terrain dimensions are too large");
        }
        final int total = (int) totalLong;
        final int expectedRawBytes = (total + 7) >>> 3;
        final byte[] raw = decodeStockChunkedBytes(encoded, expectedRawBytes);

        final int[][] tiles = new int[width][height];
        for (int index = 0; index < total; index++) {
            final int value = raw[index >>> 3] & 0xff;
            final int mask = 1 << (7 - (index & 7));
            final int x = index % width;
            final int y = index / width;
            tiles[x][y] = (value & mask) != 0 ? 1 : -1;
        }
        return tiles;
    }

    /**
     * Decodes concatenated independently padded Base64 blocks. Processing in
     * quartets is sufficient because every Base64 block ends on a quartet
     * boundary; unlike standard decoders, padding does not terminate the whole
     * input and the next quartet begins a new stock compression chunk.
     */
    static byte[] decodeStockChunkedBytes(java.lang.String encoded, int expectedRawBytes)
            throws DataFormatException {
        if (expectedRawBytes < 0) {
            throw new DataFormatException("Negative expected chunked payload size");
        }
        final byte[] compressed = decodeConcatenatedBase64(encoded);
        return inflateExactly(compressed, expectedRawBytes);
    }

    private static byte[] decodeConcatenatedBase64(java.lang.String encoded)
            throws DataFormatException {
        if ((encoded.length() & 3) != 0) {
            throw new DataFormatException(
                    "Stock tiled-terrain Base64 length is not a multiple of four: " + encoded.length());
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream((encoded.length() / 4) * 3);
        for (int offset = 0; offset < encoded.length(); offset += 4) {
            final char c0 = encoded.charAt(offset);
            final char c1 = encoded.charAt(offset + 1);
            final char c2 = encoded.charAt(offset + 2);
            final char c3 = encoded.charAt(offset + 3);
            final int v0 = base64Value(c0);
            final int v1 = base64Value(c1);
            if (v0 < 0 || v1 < 0) {
                throw invalidBase64(offset);
            }
            out.write((v0 << 2) | (v1 >>> 4));

            if (c2 == '=') {
                if (c3 != '=' || (v1 & 0x0f) != 0) {
                    throw invalidBase64(offset);
                }
                continue;
            }
            final int v2 = base64Value(c2);
            if (v2 < 0) {
                throw invalidBase64(offset);
            }
            out.write(((v1 & 0x0f) << 4) | (v2 >>> 2));

            if (c3 == '=') {
                if ((v2 & 0x03) != 0) {
                    throw invalidBase64(offset);
                }
                continue;
            }
            final int v3 = base64Value(c3);
            if (v3 < 0) {
                throw invalidBase64(offset);
            }
            out.write(((v2 & 0x03) << 6) | v3);
        }
        return out.toByteArray();
    }

    private static byte[] inflateExactly(byte[] compressed, int expectedBytes)
            throws DataFormatException {
        final Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            final ByteArrayOutputStream out = new ByteArrayOutputStream(expectedBytes);
            final byte[] buffer = new byte[Math.max(32, Math.min(4096, expectedBytes + 1))];
            while (!inflater.finished()) {
                final int read = inflater.inflate(buffer);
                if (read > 0) {
                    if (out.size() + read > expectedBytes) {
                        throw new DataFormatException(
                                "Stock tiled-terrain inflated beyond expected size=" + expectedBytes);
                    }
                    out.write(buffer, 0, read);
                    continue;
                }
                if (inflater.finished()) {
                    break;
                }
                if (inflater.needsDictionary()) {
                    throw new DataFormatException("Stock tiled-terrain stream needs a dictionary");
                }
                if (inflater.needsInput()) {
                    throw new DataFormatException("Truncated stock tiled-terrain compressed stream");
                }
                throw new DataFormatException("Stock tiled-terrain inflater made no progress");
            }
            if (out.size() != expectedBytes) {
                throw new DataFormatException(
                        "Stock tiled-terrain inflated size mismatch expected=" + expectedBytes
                                + " actual=" + out.size());
            }
            return out.toByteArray();
        } finally {
            inflater.end();
        }
    }

    private static int base64Value(char value) {
        if (value >= 'A' && value <= 'Z') return value - 'A';
        if (value >= 'a' && value <= 'z') return value - 'a' + 26;
        if (value >= '0' && value <= '9') return value - '0' + 52;
        if (value == '+') return 62;
        if (value == '/') return 63;
        return -1;
    }

    private static DataFormatException invalidBase64(int offset) {
        return new DataFormatException("Invalid stock tiled-terrain Base64 quartet at offset=" + offset);
    }

    private static void validateDimensions(int width, int height) throws DataFormatException {
        if (width < 0 || height < 0) {
            throw new DataFormatException("Negative browser tiled-terrain dimensions");
        }
        if ((long) width * (long) height > Integer.MAX_VALUE) {
            throw new DataFormatException("Browser tiled-terrain dimensions are too large");
        }
    }
}
