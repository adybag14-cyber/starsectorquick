package com.fs.starfarer.api.impl.campaign.terrain;

import java.util.zip.DataFormatException;

/** Browser compatibility decoder for HyperspaceAutomaton saved 2-bit cell grids. */
public final class BrowserHyperspaceAutomatonCompat {
    private BrowserHyperspaceAutomatonCompat() {
    }

    public static int[][] decodeTilesFast(java.lang.String encoded, int width, int height)
            throws DataFormatException {
        if (encoded == null) {
            throw new DataFormatException("Null hyperspace automaton payload");
        }
        if (width < 0 || height < 0) {
            throw new DataFormatException("Negative hyperspace automaton dimensions");
        }
        final long totalLong = (long) width * (long) height;
        if (totalLong > Integer.MAX_VALUE) {
            throw new DataFormatException("Hyperspace automaton dimensions are too large");
        }
        final int total = (int) totalLong;
        // Stock HyperspaceAutomaton stores four 2-bit cells per raw byte.
        final int expectedRawBytes = (total + 3) >>> 2;
        final byte[] raw = BrowserTiledTerrainCompat.decodeStockChunkedBytes(
                encoded, expectedRawBytes);

        final int[][] tiles = new int[width][height];
        for (int index = 0; index < total; index++) {
            final int value = raw[index >>> 2] & 0xff;
            final int shift = (3 - (index & 3)) << 1;
            final int x = index % width;
            final int y = index / width;
            tiles[x][y] = (value >>> shift) & 3;
        }
        return tiles;
    }
}
