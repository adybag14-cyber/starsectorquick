package com.fs.starfarer.stubs.java.nio;

public class ByteOrder {
    public static final ByteOrder BIG_ENDIAN = new ByteOrder();
    public static final ByteOrder LITTLE_ENDIAN = new ByteOrder();
    public static ByteOrder nativeOrder() { return LITTLE_ENDIAN; }
}