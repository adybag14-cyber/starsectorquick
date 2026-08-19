package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * Browser-only ID-reference marshaller prototype.
 *
 * XStream's stock ID marshaller inherits XPath bookkeeping even though numeric
 * ID references do not encode paths. This implementation preserves the public
 * ID/reference contract while using identity maps and the raw writer. It is not
 * installed unless a separate runtime patch explicitly selects this strategy.
 */
public final class BrowserFastReferenceByIdMarshallingStrategy extends ReferenceByIdMarshallingStrategy {
    @Override
    protected TreeMarshaller createMarshallingContext(HierarchicalStreamWriter writer,
                                                       ConverterLookup converterLookup,
                                                       Mapper mapper) {
        return new FastMarshaller(writer, converterLookup, mapper);
    }

    private static final class FastMarshaller extends ReferenceByIdMarshaller {
        private final IdentityHashMap<Object, String> references = new IdentityHashMap<Object, String>();
        private final IdentityHashMap<Object, String> implicitElements = new IdentityHashMap<Object, String>();
        private final IdentityHashMap<Object, Boolean> sameNodeReplacement = new IdentityHashMap<Object, Boolean>();
        private int nextId = 1;

        FastMarshaller(HierarchicalStreamWriter rawWriter, ConverterLookup converterLookup, Mapper mapper) {
            super(rawWriter, converterLookup, mapper);
            // AbstractReferenceMarshaller wraps this in PathTrackingWriter. ID
            // references do not encode paths, so use the exact underlying writer.
            this.writer = rawWriter;
        }

        @Override
        public void convert(Object value, Converter converter) {
            final Mapper mapper = getMapper();
            if (mapper.isImmutableValueType(value.getClass())) {
                converter.marshal(value, writer, this);
                return;
            }

            final String existing = references.get(value);
            if (existing != null && sameNodeReplacement.remove(value) != null) {
                converter.marshal(value, writer, new FastContext(existing));
                return;
            }
            if (existing != null) {
                final String referenceAttr = mapper.aliasForSystemAttribute("reference");
                if (referenceAttr != null) writer.addAttribute(referenceAttr, existing);
                return;
            }

            final String id = String.valueOf(nextId++);
            final String idAttr = mapper.aliasForSystemAttribute("id");
            if (idAttr != null) writer.addAttribute(idAttr, id);
            references.put(value, id);
            converter.marshal(value, writer, new FastContext(id));
        }

        private final class FastContext implements ReferencingMarshallingContext {
            private final String currentId;

            FastContext(String currentId) {
                this.currentId = currentId;
            }

            @Override
            public void convertAnother(Object value) {
                FastMarshaller.this.convertAnother(value);
            }

            @Override
            public void convertAnother(Object value, Converter converter) {
                FastMarshaller.this.convertAnother(value, converter);
            }

            @Override
            public Object get(Object key) {
                return FastMarshaller.this.get(key);
            }

            @Override
            public void put(Object key, Object value) {
                FastMarshaller.this.put(key, value);
            }

            @Override
            public Iterator keys() {
                return FastMarshaller.this.keys();
            }

            @Override
            public Path currentPath() {
                // No Starsector class calls this method. Built-in reflective
                // converters use replace/registerImplicit instead.
                return null;
            }

            @Override
            public Object lookupReference(Object value) {
                final String id = references.get(value);
                if (id == null) throw new NullPointerException();
                return id;
            }

            @Override
            public void replace(Object original, Object replacement) {
                references.put(replacement, currentId);
                sameNodeReplacement.put(replacement, Boolean.TRUE);
            }

            @Override
            public void registerImplicit(Object value) {
                if (implicitElements.containsKey(value)) {
                    throw new AbstractReferenceMarshaller.ReferencedImplicitElementException(value, null);
                }
                implicitElements.put(value, currentId);
            }
        }
    }
}
