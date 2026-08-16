package com.fs.starfarer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Bulk, semantics-preserving implementation of Misc.addNebulaFromPNG for CheerpJ.
 *
 * Stock Starsector calls Raster.getPixel(x, y, null) for every source pixel. That
 * virtual AWT call loop is extremely expensive under CheerpJ: the full-sector CI
 * tracer reaches Eos, whose first operation is addNebulaFromPNG(eos_nebula.png),
 * and then monopolizes the VM for the remainder of the 25-minute job.
 *
 * This helper performs one Raster.getPixels() call for the exact source rectangle
 * and applies the same RGB-sum > 0 mask, row order, 10,000-pixel chunk cap, tile
 * dimensions, 400-unit placement, age and systemwide-nebula flag as the stock
 * method. No nebula cells or campaign content are skipped.
 */
public final class BrowserNebulaCompat {
    private static final int STOCK_CHUNK_LIMIT = 10000;
    private static final float STOCK_TILE_SIZE = 400f;

    private BrowserNebulaCompat() {}

    public static SectorEntityToken addNebulaFromPNG(
            java.lang.String path,
            float centerX,
            float centerY,
            LocationAPI location,
            java.lang.String category,
            java.lang.String textureId,
            int tilesX,
            int tilesY,
            java.lang.String terrainId,
            StarAge age) {
        try {
            BufferedImage image = ImageIO.read(Global.getSettings().openStream(path));
            int width = image.getWidth();
            int height = image.getHeight();
            Raster raster = image.getData();

            // Stock code is written as 10k x 10k chunk loops but returns the first
            // terrain token from inside those loops. Reproduce that exact first
            // chunk for all image sizes, including the source-Y inversion.
            int chunkWidth = Math.min(STOCK_CHUNK_LIMIT, width);
            int chunkHeight = Math.min(STOCK_CHUNK_LIMIT, height);
            int sourceY = height - chunkHeight;
            int bands = raster.getNumBands();
            int[] pixels = raster.getPixels(0, sourceY, chunkWidth, chunkHeight, (int[]) null);

            java.lang.StringBuilder mask =
                    new java.lang.StringBuilder(chunkWidth * chunkHeight);
            int sample = 0;
            for (int y = 0; y < chunkHeight; y++) {
                for (int x = 0; x < chunkWidth; x++) {
                    // Intentionally access bands 0..2 exactly like stock. If an
                    // unexpected raster has fewer than three bands, preserve the
                    // same ArrayIndexOutOfBounds-style failure rather than hiding it.
                    int rgb = pixels[sample] + pixels[sample + 1] + pixels[sample + 2];
                    mask.append(rgb > 0 ? 'x' : ' ');
                    sample += bands;
                }
            }

            float terrainX =
                    centerX
                            - STOCK_TILE_SIZE * width / 2f
                            + STOCK_TILE_SIZE * chunkWidth / 2f;
            float terrainY =
                    centerY
                            - STOCK_TILE_SIZE * height / 2f
                            + STOCK_TILE_SIZE * chunkHeight / 2f;

            SectorEntityToken terrain =
                    location.addTerrain(
                            terrainId,
                            new BaseTiledTerrain.TileParams(
                                    mask.toString(),
                                    chunkWidth,
                                    chunkHeight,
                                    category,
                                    textureId,
                                    tilesX,
                                    tilesY,
                                    null));
            terrain.getLocation().set(terrainX, terrainY);

            if (location instanceof StarSystemAPI) {
                StarSystemAPI system = (StarSystemAPI) location;
                system.setAge(age);
                system.setHasSystemwideNebula(Boolean.TRUE);
            }
            return terrain;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
