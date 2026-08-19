package com.fs.starfarer;

import com.thoughtworks.xstream.converters.Converter;

/** Diagnostic-only shallow inclusive/self timing for XStream marshal conversion frames. */
public final class BrowserXStreamMarshalDiag {
    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("starsector.browserGameplayProbe", "false"));
    private static final int MAX_TIMED_DEPTH = 8;
    private static long[] starts = new long[64];
    private static long[] childMs = new long[64];
    private static Class[] types = new Class[64];
    private static Converter[] converters = new Converter[64];
    private static int depth;

    private BrowserXStreamMarshalDiag() {}

    public static void enter(final Object value, final Converter converter) {
        if (!ENABLED) return;
        final int frame = depth++;
        if (frame > MAX_TIMED_DEPTH) return;
        ensureCapacity(frame + 1);
        starts[frame] = System.currentTimeMillis();
        childMs[frame] = 0L;
        types[frame] = value == null ? null : value.getClass();
        converters[frame] = converter;
    }

    public static void exit() {
        if (!ENABLED || depth <= 0) return;
        final int frame = --depth;
        if (frame > MAX_TIMED_DEPTH) return;
        final long elapsedMs = Math.max(0L, System.currentTimeMillis() - starts[frame]);
        final long selfMs = Math.max(0L, elapsedMs - childMs[frame]);
        if (frame > 0 && frame - 1 <= MAX_TIMED_DEPTH) childMs[frame - 1] += elapsedMs;
        if (selfMs >= 100L || elapsedMs >= 500L) {
            final Class type = types[frame];
            final Converter converter = converters[frame];
            System.out.println("BrowserXStreamMarshalDiag: type="
                    + (type == null ? "<null>" : type.getName())
                    + " converter=" + (converter == null ? "<null>" : converter.getClass().getName())
                    + " elapsedMs=" + elapsedMs
                    + " selfMs=" + selfMs
                    + " depth=" + frame);
        }
        starts[frame] = 0L;
        childMs[frame] = 0L;
        types[frame] = null;
        converters[frame] = null;
    }

    private static void ensureCapacity(final int required) {
        if (required <= starts.length) return;
        int size = starts.length;
        while (size < required) size *= 2;
        starts = java.util.Arrays.copyOf(starts, size);
        childMs = java.util.Arrays.copyOf(childMs, size);
        types = (Class[]) java.util.Arrays.copyOf(types, size);
        converters = (Converter[]) java.util.Arrays.copyOf(converters, size);
    }
}
