package com.fs.starfarer.launcher;

public class Object {
    private static Object instance = new Object();
    
    public static Object getInstance() { return instance; }
    
    public static Object M10000() { return instance; }
    public static Object M20000() { return instance; }
    
    public void ii() {}
    public int dd() { return 0; }
}