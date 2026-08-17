package com.fs.starfarer.api.impl.campaign.terrain;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import javax.xml.bind.DatatypeConverter;

/**
 * Browser-only fast path for BaseTiledTerrain's transient initialization round-trip.
 * Stock init immediately compresses and decompresses a freshly parsed occupancy
 * grid. CheerpJ makes that transient Deflate/Inflate cycle disproportionately slow.
 * Persisted saves remain in Starsector's stock format because writeReplace is untouched.
 */
public final class BrowserTiledTerrainCompat {
    private static final java.lang.String PREFIX = "browser-raw-tiles-v1:";
    private static final int INFLATE_BUFFER_SIZE = 100;

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
        if (encoded == null || !encoded.startsWith(PREFIX)) {
            return decodeStockTilesSafely(encoded, width, height);
        }
        if (width < 0 || height < 0) {
            throw new DataFormatException("Negative browser tiled-terrain dimensions");
        }
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

    private static int[][] decodeStockTilesSafely(
            java.lang.String encoded, int width, int height) throws DataFormatException {
        if (encoded == null) {
            throw new DataFormatException("Null stock tiled-terrain payload");
        }
        if (width < 0 || height < 0) {
            throw new DataFormatException("Negative stock tiled-terrain dimensions");
        }
        final long totalLong = (long) width * (long) height;
        if (totalLong > Integer.MAX_VALUE) {
            throw new DataFormatException("Stock tiled-terrain dimensions overflow");
        }

        final byte[] compressed;
        try {
            compressed = DatatypeConverter.parseBase64Binary(encoded);
        } catch (final RuntimeException decodeFailure) {
            final DataFormatException wrapped =
                    new DataFormatException("Invalid stock tiled-terrain Base64 payload");
            wrapped.initCause(decodeFailure);
            throw wrapped;
        }
        if (compressed.length == 0 && totalLong != 0L) {
            throw new DataFormatException("Empty stock tiled-terrain compressed payload");
        }

        final int total = (int) totalLong;
        final int[][] tiles = new int[width][height];
        final Inflater inflater = new Inflater();
        final byte[] buffer = new byte[INFLATE_BUFFER_SIZE];
        int processed = 0;
        int zeroProgress = 0;
        try {
            inflater.setInput(compressed);
            while (!inflater.finished() && processed < total) {
                final int count = inflater.inflate(buffer);
                if (count == 0) {
                    if (inflater.finished()) {
                        break;
                    }
                    if (inflater.needsDictionary()) {
                        throw new DataFormatException(
                                "Stock tiled-terrain stream requires an unsupported dictionary");
                    }
                    if (inflater.needsInput()) {
                        throw new DataFormatException(
                                "Stock tiled-terrain stream ended before all tiles were restored");
                    }
                    if (++zeroProgress >= 2) {
                        throw new DataFormatException(
                                "Stock tiled-terrain inflater made no progress");
                    }
                    continue;
                }
                zeroProgress = 0;
                for (int i = 0; i < count && processed < total; i++) {
                    final int packed = buffer[i] & 0xFF;
                    for (int bit = 7; bit >= 0 && processed < total; bit--) {
                        final int x = processed % width;
                        final int y = processed / width;
                        tiles[x][y] = (packed & (1 << bit)) != 0 ? 1 : -1;
                        processed++;
                    }
                }
            }
        } finally {
            inflater.end();
        }

        if (processed != total) {
            throw new DataFormatException(
                    "Stock tiled-terrain payload restored " + processed
                            + " of " + total + " tiles");
        }
        return tiles;
    }
}
