package com.fs.starfarer.stubs.java.awt.image;

public class Raster {
    public void setSample(int x, int y, int b, float s) {}
    public int[] getPixel(int x, int y, int[] iArray) {
        if (iArray == null) iArray = new int[4];
        return iArray;
    }
}