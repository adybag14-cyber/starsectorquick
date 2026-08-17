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
 * and terrain mask semantics remain identical for the transformed call site.
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

        // Match Raster.getPixel(..., null): its newly allocated array is exactly
        // numBands long. Keeping that length preserves stock behavior even for an
        // unexpected non-RGB raster instead of accidentally masking an out-of-range
        // access in Misc.addNebulaFromPNG().
        int needed = raster.getNumBands();
        int[] scratch = PIXEL_SCRATCH.get();
        if (scratch == null || scratch.length != needed) {
            scratch = new int[needed];
            PIXEL_SCRATCH.set(scratch);
        }
        return raster.getPixel(x, y, scratch);
    }
}
