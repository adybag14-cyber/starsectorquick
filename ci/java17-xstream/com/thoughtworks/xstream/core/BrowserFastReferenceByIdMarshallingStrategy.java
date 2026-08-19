package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.io.AbstractWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.WriterWrapper;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Browser-only ID-reference marshaller prototype.
 *
 * XStream's stock ID marshaller inherits full XPath construction even though
 * numeric ID references never encode those paths. This implementation keeps the
 * exact same-node/ancestor decisions using immutable linked node tokens, while
 * avoiding PathTrackingWriter's per-path String[]/Path construction.
 */
public final class BrowserFastReferenceByIdMarshallingStrategy extends ReferenceByIdMarshallingStrategy {
    @Override
    protected TreeMarshaller createMarshallingContext(HierarchicalStreamWriter writer,
                                                       ConverterLookup converterLookup,
                                                       Mapper mapper) {
        return new FastMarshaller(writer, converterLookup, mapper);
    }

    private static final class FastMarshaller extends ReferenceByIdMarshaller {
        private final IdentityHashMap<Object, Ref> references = new IdentityHashMap<Object, Ref>();
        private final IdentityHashMap<Object, String> implicitElements = new IdentityHashMap<Object, String>();
        private final TokenTrackingWriter tokenWriter;
        private Token lastPath;
        private int nextId = 1;

        FastMarshaller(HierarchicalStreamWriter rawWriter, ConverterLookup converterLookup, Mapper mapper) {
            super(rawWriter, converterLookup, mapper);
            tokenWriter = new TokenTrackingWriter(rawWriter);
            // AbstractReferenceMarshaller installed PathTrackingWriter in super().
            // Replace only that wrapper; TokenTrackingWriter still preserves the
            // same current-path equality/ancestor semantics used by ID mode.
            this.writer = tokenWriter;
        }

        @Override
        public void convert(Object value, Converter converter) {
            final Mapper mapper = getMapper();
            if (mapper.isImmutableValueType(value.getClass())) {
                converter.marshal(value, writer, this);
                return;
            }

            final Token currentPath = tokenWriter.currentPath();
            final Ref existing = references.get(value);
            if (existing != null && !samePath(existing.path, currentPath)) {
                final String referenceAttr = mapper.aliasForSystemAttribute("reference");
                if (referenceAttr != null) writer.addAttribute(referenceAttr, existing.id);
                return;
            }

            final String newReferenceKey = existing == null
                    ? String.valueOf(nextId++) : existing.id;
            if (lastPath == null || !isAncestor(currentPath, lastPath)) {
                final String idAttr = mapper.aliasForSystemAttribute("id");
                if (idAttr != null) writer.addAttribute(idAttr, newReferenceKey);
                lastPath = currentPath;
                references.put(value, new Ref(newReferenceKey, currentPath));
            }
            converter.marshal(value, writer, new FastContext(newReferenceKey, currentPath));
        }

        private final class FastContext implements ReferencingMarshallingContext {
            private final String currentId;
            private final Token currentPath;

            FastContext(String currentId, Token currentPath) {
                this.currentId = currentId;
                this.currentPath = currentPath;
            }

            @Override public void convertAnother(Object value) { FastMarshaller.this.convertAnother(value); }
            @Override public void convertAnother(Object value, Converter converter) {
                FastMarshaller.this.convertAnother(value, converter);
            }
            @Override public Object get(Object key) { return FastMarshaller.this.get(key); }
            @Override public void put(Object key, Object value) { FastMarshaller.this.put(key, value); }
            @Override public Iterator keys() { return FastMarshaller.this.keys(); }

            @Override
            public Path currentPath() {
                return currentPath == null ? null : currentPath.toPath();
            }

            @Override
            public Object lookupReference(Object value) {
                // Match stock behavior: dereferencing a missing identity throws NPE.
                return references.get(value).id;
            }

            @Override
            public void replace(Object original, Object replacement) {
                references.put(replacement, new Ref(currentId, currentPath));
            }

            @Override
            public void registerImplicit(Object value) {
                if (implicitElements.containsKey(value)) {
                    throw new AbstractReferenceMarshaller.ReferencedImplicitElementException(
                            value, currentPath());
                }
                implicitElements.put(value, currentId);
            }
        }
    }

    /** Minimal writer wrapper: one linked token per open node, no full path arrays. */
    private static final class TokenTrackingWriter extends WriterWrapper {
        private final boolean encodeNames;
        private final Map<String, Integer> rootCounts = new HashMap<String, Integer>();
        private Token current;

        TokenTrackingWriter(HierarchicalStreamWriter wrapped) {
            super(wrapped);
            encodeNames = wrapped.underlyingWriter() instanceof AbstractWriter;
        }

        @Override
        public void startNode(String name) {
            push(name);
            super.startNode(name);
        }

        @Override
        public void startNode(String name, Class type) {
            push(name);
            super.startNode(name, type);
        }

        @Override
        public void endNode() {
            super.endNode();
            if (current != null) current = current.parent;
        }

        Token currentPath() { return current; }

        private void push(String name) {
            String pathName = name;
            if (encodeNames) {
                pathName = ((AbstractWriter) wrapped.underlyingWriter()).encodeNode(name);
            }
            final Token parent = current;
            final Map<String, Integer> counts = parent == null ? rootCounts : parent.childCounts();
            final Integer prior = counts.get(pathName);
            final int ordinal = prior == null ? 1 : prior.intValue() + 1;
            counts.put(pathName, Integer.valueOf(ordinal));
            if (ordinal > 1) pathName = pathName + "[" + ordinal + "]";
            current = new Token(parent, pathName);
        }
    }

    private static final class Token {
        final Token parent;
        final String name;
        final int depth;
        private Map<String, Integer> childCounts;
        private Path materialized;

        Token(Token parent, String name) {
            this.parent = parent;
            this.name = name;
            this.depth = parent == null ? 1 : parent.depth + 1;
        }

        Map<String, Integer> childCounts() {
            Map<String, Integer> counts = childCounts;
            if (counts == null) {
                counts = new HashMap<String, Integer>();
                childCounts = counts;
            }
            return counts;
        }

        Path toPath() {
            Path cached = materialized;
            if (cached != null) return cached;
            String[] chunks = new String[depth + 1];
            chunks[0] = "";
            Token t = this;
            for (int i = depth; i >= 1; i--) {
                chunks[i] = t.name;
                t = t.parent;
            }
            cached = new Path(chunks);
            materialized = cached;
            return cached;
        }
    }

    private static final class Ref {
        final String id;
        final Token path;
        Ref(String id, Token path) { this.id = id; this.path = path; }
    }

    private static boolean samePath(Token a, Token b) {
        if (a == b) return true;
        if (a == null || b == null || a.depth != b.depth) return false;
        Token x = a, y = b;
        while (x != null && y != null) {
            if (!x.name.equals(y.name)) return false;
            x = x.parent;
            y = y.parent;
        }
        return x == y;
    }

    private static boolean isAncestor(Token ancestor, Token path) {
        if (ancestor == null || path == null || path.depth < ancestor.depth) return false;
        Token aligned = path;
        for (int i = path.depth; i > ancestor.depth; i--) aligned = aligned.parent;
        return samePath(ancestor, aligned);
    }
}
