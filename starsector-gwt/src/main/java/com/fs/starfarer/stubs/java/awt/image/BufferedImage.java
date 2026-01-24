package com.fs.starfarer.stubs.java.awt.image;

public class BufferedImage {
    public static final int TYPE_INT_ARGB = 2;
    public BufferedImage(int width, int height, int imageType) {}
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public Raster getRaster() { return new Raster(); }
    public Raster getData() { return getRaster(); }
    public int getRGB(int x, int y) { return 0; }
    public void setRGB(int x, int y, int rgb) {}
}