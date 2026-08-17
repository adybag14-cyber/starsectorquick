package com.fs.starfarer;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/** Diagnostic-only entry logging for XStream ID-reference conversion. */
public final class BrowserXStreamReferenceDiag {
    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("starsector.browserXstreamLoadDiag", "false"));

    private BrowserXStreamReferenceDiag() {}

    public static void enter(final Class type, final Converter converter,
            final HierarchicalStreamReader reader) {
        if (!ENABLED || reader == null) return;
        final java.lang.String node = safeNode(reader);
        if (!"primaryEntity".equals(node)) return;
        System.out.println("BrowserXStreamLoadDiag: reference-enter node=primaryEntity"
                + " type=" + (type == null ? "<null>" : type.getName())
                + " converter=" + (converter == null ? "<null>" : converter.getClass().getName())
                + " attrs=" + safeAttributes(reader));
    }

    private static java.lang.String safeAttributes(final HierarchicalStreamReader reader) {
        try {
            final java.lang.StringBuilder out = new java.lang.StringBuilder("{");
            final java.util.Iterator names = reader.getAttributeNames();
            int count = 0;
            while (names.hasNext() && count < 12) {
                final java.lang.String name = java.lang.String.valueOf(names.next());
                if (count > 0) out.append(',');
                out.append(name).append('=').append(java.lang.String.valueOf(reader.getAttribute(name)));
                count++;
            }
            if (names.hasNext()) out.append(",...");
            return out.append('}').toString();
        } catch (final Throwable t) {
            return "<error:" + t.getClass().getName() + ">";
        }
    }

    private static java.lang.String safeNode(final HierarchicalStreamReader reader) {
        try {
            return reader.getNodeName();
        } catch (final Throwable ignored) {
            return "<unavailable>";
        }
    }
}
