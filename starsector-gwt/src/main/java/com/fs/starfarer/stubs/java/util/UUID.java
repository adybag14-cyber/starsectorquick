package com.fs.starfarer.stubs.java.util;

public class UUID {
    private final String id;
    private UUID(String id) { this.id = id; }
    public static UUID randomUUID() { return new UUID(String.valueOf(Math.random())); }
    public static UUID fromString(String name) { return new UUID(name); }
    public String toString() { return id; }
}
