package com.fs.starfarer;

import java.awt.image.Raster;

/**
 * Allocation-free compatibility helpers for browser-side campaign terrain decoding.
 *
 * Starsector's Misc.addNebulaFromPNG() passes null to Raster.getPixel() for every
 * image pixel. The JDK contract allocates a new int[] whenever that argument is
 * null. On CheerpJ this turns small nebula masks into a very allocation-heavy hot
 * loop and can monopolize the VM for minutes. Reuse one per-thread scratch array
 * while delegating to Raster.getPixel() unchanged, so the returned sample values
 * and terrain mask semantics remain identical.
 */
public final class BrowserRasterCompat {
    private static final ThreadLocal<int[]> PIXEL_SCRATCH = new ThreadLocal<int[]>();

    private BrowserRasterCompat() {}

    public static int[] getPixel(Raster raster, int x, int y, int[] out) {
        if (raster == null) {
            throw new NullPointerException("raster");
        }
        if (out != null) {
            return raster.getPixel(x, y, out);
        }

        int needed = Math.max(3, raster.getNumBands());
        int[] scratch = PIXEL_SCRATCH.get();
        if (scratch == null || scratch.length < needed) {
            scratch = new int[needed];
            PIXEL_SCRATCH.set(scratch);
        }
        return raster.getPixel(x, y, scratch);
    }
}
