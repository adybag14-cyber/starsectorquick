package com.fs.starfarer.stubs.java.util.concurrent.atomic;

public class AtomicLong {
    private long value;
    public AtomicLong(long initialValue) { this.value = initialValue; }
    public long get() { return value; }
    public void set(long newValue) { this.value = newValue; }
    public long incrementAndGet() { return ++value; }
    public boolean compareAndSet(long expect, long update) {
        if (value == expect) {
            value = update;
            return true;
        }
        return false;
    }
}