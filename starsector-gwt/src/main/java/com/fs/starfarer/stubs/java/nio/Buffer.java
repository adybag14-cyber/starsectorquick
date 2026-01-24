package com.fs.starfarer.stubs.java.nio;

public abstract class Buffer {
    public abstract int capacity();
    public abstract int position();
    public abstract Buffer position(int newPosition);
    public abstract int limit();
    public abstract Buffer limit(int newLimit);
    public abstract Buffer flip();
    public abstract Buffer clear();
}
