package com.fs.starfarer.stubs.java.util.zip;

public class Deflater {
    public static final int BEST_COMPRESSION = 9;
    public Deflater() {}
    public Deflater(int level) {}
    public Deflater(int level, boolean nowrap) {}
    public void setInput(byte[] b) {}
    public void setInput(byte[] b, int off, int len) {}
    public void finish() {}
    public int deflate(byte[] b) { return 0; }
    public void end() {}
    public boolean finished() { return true; }
}
