package com.fs.starfarer;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/** Diagnostic-only inclusive/self timing for XStream conversion frames. */
public final class BrowserXStreamConvertDiag {
    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("starsector.browserXstreamLoadDiag", "false"));
    private static long[] starts = new long[256];
    private static long[] childMs = new long[256];
    private static Class[] types = new Class[256];
    private static Converter[] converters = new Converter[256];
    private static java.lang.String[] nodes = new java.lang.String[256];
    private static int depth;

    private BrowserXStreamConvertDiag() {}

    public static void enter(final Class type, final Converter converter,
            final HierarchicalStreamReader reader) {
        if (!ENABLED) return;
        final int frame = depth++;
        ensureCapacity(frame + 1);
        starts[frame] = System.currentTimeMillis();
        childMs[frame] = 0L;
        types[frame] = type;
        converters[frame] = converter;
        nodes[frame] = safeNode(reader);
    }

    public static void exit() {
        if (!ENABLED || depth <= 0) return;
        final int frame = --depth;
        final long elapsedMs = System.currentTimeMillis() - starts[frame];
        final long selfMs = Math.max(0L, elapsedMs - childMs[frame]);
        if (frame > 0) childMs[frame - 1] += elapsedMs;
        if (selfMs >= 100L || (elapsedMs >= 1000L && frame <= 8)) {
            final Class type = types[frame];
            final Converter converter = converters[frame];
            System.out.println("BrowserXStreamLoadDiag: convert type="
                    + (type == null ? "<null>" : type.getName())
                    + " converter=" + (converter == null ? "<null>" : converter.getClass().getName())
                    + " node=" + nodes[frame]
                    + " elapsedMs=" + elapsedMs
                    + " selfMs=" + selfMs
                    + " depth=" + frame);
        }
        starts[frame] = 0L;
        childMs[frame] = 0L;
        types[frame] = null;
        converters[frame] = null;
        nodes[frame] = null;
    }

    private static void ensureCapacity(final int required) {
        if (required <= starts.length) return;
        int size = starts.length;
        while (size < required) size *= 2;
        starts = java.util.Arrays.copyOf(starts, size);
        childMs = java.util.Arrays.copyOf(childMs, size);
        types = (Class[]) java.util.Arrays.copyOf(types, size);
        converters = (Converter[]) java.util.Arrays.copyOf(converters, size);
        nodes = (java.lang.String[]) java.util.Arrays.copyOf(nodes, size);
    }

    private static java.lang.String safeNode(final HierarchicalStreamReader reader) {
        if (reader == null) return "<null-reader>";
        try {
            return reader.getNodeName();
        } catch (final Throwable ignored) {
            return "<unavailable>";
        }
    }
}
