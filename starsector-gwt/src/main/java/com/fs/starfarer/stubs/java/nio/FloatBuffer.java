package com.fs.starfarer.stubs.java.nio;

public abstract class FloatBuffer extends Buffer {
    public static FloatBuffer allocate(int capacity) { return null; }
    public abstract float get();
    public abstract float get(int index);
    public abstract FloatBuffer put(float f);
    public abstract FloatBuffer put(int index, float f);
    public abstract FloatBuffer put(float[] src);
}
