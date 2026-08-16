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

/** Bulk, semantics-preserving implementation of Misc.addNebulaFromPNG for CheerpJ. */
public final class BrowserNebulaCompat {
    private static final int STOCK_CHUNK_LIMIT = 10000;
    private static final float STOCK_TILE_SIZE = 400f;

    private BrowserNebulaCompat() {}

    private static void phase(java.lang.String path, java.lang.String name, long started) {
        System.out.println(
                "BrowserNebulaCompat: path=" + path
                        + " phase=" + name
                        + " elapsedMs=" + (System.currentTimeMillis() - started));
    }

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
        long started = System.currentTimeMillis();
        phase(path, "begin", started);
        try {
            BufferedImage image = ImageIO.read(Global.getSettings().openStream(path));
            phase(path, "image-read", started);
            int width = image.getWidth();
            int height = image.getHeight();
            Raster raster = image.getData();
            phase(path, "raster-ready", started);

            int chunkWidth = Math.min(STOCK_CHUNK_LIMIT, width);
            int chunkHeight = Math.min(STOCK_CHUNK_LIMIT, height);
            int sourceY = height - chunkHeight;
            int bands = raster.getNumBands();
            int[] pixels = raster.getPixels(0, sourceY, chunkWidth, chunkHeight, (int[]) null);
            phase(path, "pixels-bulk-read", started);

            java.lang.StringBuilder mask =
                    new java.lang.StringBuilder(chunkWidth * chunkHeight);
            int sample = 0;
            for (int y = 0; y < chunkHeight; y++) {
                for (int x = 0; x < chunkWidth; x++) {
                    int rgb = pixels[sample] + pixels[sample + 1] + pixels[sample + 2];
                    mask.append(rgb > 0 ? 'x' : ' ');
                    sample += bands;
                }
            }
            phase(path, "mask-built", started);

            float terrainX =
                    centerX - STOCK_TILE_SIZE * width / 2f + STOCK_TILE_SIZE * chunkWidth / 2f;
            float terrainY =
                    centerY - STOCK_TILE_SIZE * height / 2f + STOCK_TILE_SIZE * chunkHeight / 2f;

            SectorEntityToken terrain =
                    location.addTerrain(
                            terrainId,
                            new BaseTiledTerrain.TileParams(
                                    mask.toString(), chunkWidth, chunkHeight,
                                    category, textureId, tilesX, tilesY, null));
            phase(path, "terrain-added", started);
            terrain.getLocation().set(terrainX, terrainY);

            if (location instanceof StarSystemAPI) {
                StarSystemAPI system = (StarSystemAPI) location;
                system.setAge(age);
                system.setHasSystemwideNebula(Boolean.TRUE);
            }
            phase(path, "complete", started);
            return terrain;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
