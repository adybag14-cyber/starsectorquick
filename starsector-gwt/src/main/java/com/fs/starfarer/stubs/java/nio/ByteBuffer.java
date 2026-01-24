package com.fs.starfarer.stubs.java.nio;

public abstract class ByteBuffer extends Buffer {
    public static ByteBuffer allocate(int capacity) { return null; }
    public static ByteBuffer allocateDirect(int capacity) { return null; }
    public abstract byte get();
    public abstract byte get(int index);
    public abstract ByteBuffer put(byte b);
    public abstract ByteBuffer put(int index, byte b);
    public abstract ByteBuffer put(byte[] src);
    public ByteBuffer order(ByteOrder bo) { return this; }
    public FloatBuffer asFloatBuffer() { return null; }
}