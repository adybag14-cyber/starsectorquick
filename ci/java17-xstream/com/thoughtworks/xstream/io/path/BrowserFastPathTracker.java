package com.thoughtworks.xstream.io.path;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Exact PathTracker implementation that materializes sibling-indexed path
 * segments once on push instead of rebuilding them through peekElement() for
 * every getPath() call. Disabled mode delegates to stock PathTracker exactly.
 */
public final class BrowserFastPathTracker extends PathTracker {
    private static final boolean ENABLED = Boolean.getBoolean("starsector.browserXstreamFastPathTracker");
    private static final AtomicBoolean LOGGED = new AtomicBoolean();
    private int pointer;
    private int capacity = 16;
    private String[] indexedPathStack = new String[capacity];
    @SuppressWarnings("rawtypes")
    private Map[] indexMapStack = new Map[capacity];
    private Path currentPath;

    public BrowserFastPathTracker() {
        super();
        if (ENABLED && LOGGED.compareAndSet(false, true)) {
            System.out.println("BrowserFastPathTracker: enabled exact indexed path cache");
        }
    }

    @Override
    public void pushElement(String name) {
        if (!ENABLED) {
            super.pushElement(name);
            return;
        }
        if (pointer + 1 >= capacity) resize(capacity * 2);
        @SuppressWarnings("unchecked")
        Map<String, Integer> counts = (Map<String, Integer>) indexMapStack[pointer];
        if (counts == null) {
            counts = new HashMap<String, Integer>();
            indexMapStack[pointer] = counts;
        }
        Integer prior = counts.get(name);
        int ordinal = prior == null ? 1 : prior.intValue() + 1;
        counts.put(name, Integer.valueOf(ordinal));
        indexedPathStack[pointer] = ordinal <= 1 ? name : name + "[" + ordinal + "]";
        pointer++;
        currentPath = null;
    }

    @Override
    public void popElement() {
        if (!ENABLED) {
            super.popElement();
            return;
        }
        indexMapStack[pointer] = null;
        indexedPathStack[pointer] = null;
        currentPath = null;
        pointer--;
    }

    @Override
    public String peekElement() {
        return peekElement(0);
    }

    @Override
    public String peekElement(int index) {
        if (!ENABLED) return super.peekElement(index);
        if (index < -pointer || index > 0) throw new ArrayIndexOutOfBoundsException(index);
        int stackIndex = pointer + index - 1;
        return indexedPathStack[stackIndex];
    }

    @Override
    public int depth() {
        return ENABLED ? pointer : super.depth();
    }

    @Override
    public Path getPath() {
        if (!ENABLED) return super.getPath();
        Path cached = currentPath;
        if (cached == null) {
            String[] chunks = new String[pointer + 1];
            chunks[0] = "";
            if (pointer > 0) System.arraycopy(indexedPathStack, 0, chunks, 1, pointer);
            cached = new Path(chunks);
            currentPath = cached;
        }
        return cached;
    }

    private void resize(int size) {
        String[] nextPath = new String[size];
        @SuppressWarnings("rawtypes")
        Map[] nextMaps = new Map[size];
        System.arraycopy(indexedPathStack, 0, nextPath, 0, indexedPathStack.length);
        System.arraycopy(indexMapStack, 0, nextMaps, 0, indexMapStack.length);
        indexedPathStack = nextPath;
        indexMapStack = nextMaps;
        capacity = size;
    }
}
