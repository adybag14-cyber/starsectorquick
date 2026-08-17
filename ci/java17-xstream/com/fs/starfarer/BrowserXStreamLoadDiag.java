package com.fs.starfarer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.ReaderWrapper;

/** Diagnostic-only progress wrapper for browser campaign XStream loads. */
public final class BrowserXStreamLoadDiag {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserXstreamLoadDiag";
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"));
    private static int nextReaderId = 1;

    private BrowserXStreamLoadDiag() {}

    public static void logProvider(final XStream xstream) {
        if (!ENABLED || xstream == null) return;
        try {
            System.out.println("BrowserXStreamLoadDiag: provider="
                    + xstream.getReflectionProvider().getClass().getName());
        } catch (final Throwable t) {
            System.out.println("BrowserXStreamLoadDiag: provider=<error:" + t.getClass().getName() + ">");
        }
    }

    public static HierarchicalStreamReader wrap(final HierarchicalStreamReader reader) {
        if (!ENABLED || reader == null) return reader;
        return new CountingReader(reader, allocateReaderId());
    }

    private static synchronized int allocateReaderId() {
        return nextReaderId++;
    }

    private static final class CountingReader extends ReaderWrapper {
        private final int id;
        private final long startedAt = System.currentTimeMillis();
        private long nodes;
        private int depth;

        CountingReader(final HierarchicalStreamReader wrapped, final int id) {
            super(wrapped);
            this.id = id;
            System.out.println("BrowserXStreamLoadDiag: reader=" + id
                    + " open root=" + safeNodeName());
        }

        @Override
        public void moveDown() {
            super.moveDown();
            nodes++;
            depth++;
            if (nodes == 1L || nodes % 50000L == 0L) {
                System.out.println("BrowserXStreamLoadDiag: reader=" + id
                        + " nodes=" + nodes
                        + " depth=" + depth
                        + " elapsedMs=" + (System.currentTimeMillis() - startedAt)
                        + " node=" + safeNodeName());
            }
        }

        @Override
        public java.lang.String getValue() {
            final java.lang.String node = safeNodeName();
            final boolean firstCampaignBoolean = "isFastForwardIteration".equals(node);
            if (firstCampaignBoolean) {
                System.out.println("BrowserXStreamLoadDiag: reader=" + id
                        + " getValue-before node=" + node
                        + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
            }
            final java.lang.String value = super.getValue();
            if (firstCampaignBoolean) {
                System.out.println("BrowserXStreamLoadDiag: reader=" + id
                        + " getValue-after node=" + node
                        + " value=" + safeValue(value)
                        + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
            }
            return value;
        }

        @Override
        public void moveUp() {
            super.moveUp();
            if (depth > 0) depth--;
        }

        @Override
        public void close() {
            System.out.println("BrowserXStreamLoadDiag: reader=" + id
                    + " close nodes=" + nodes
                    + " depth=" + depth
                    + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
            super.close();
        }

        private java.lang.String safeValue(final java.lang.String value) {
            if (value == null) return "<null>";
            final java.lang.String normalized = value.replace('\n', ' ').replace('\r', ' ');
            return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
        }

        private java.lang.String safeNodeName() {
            try {
                return getNodeName();
            } catch (final Throwable ignored) {
                return "<unavailable>";
            }
        }
    }
}
