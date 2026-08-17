package com.fs.starfarer;

import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Exact memoizing wrapper for XStream's stock XmlFriendlyNameCoder.
 * XStream intentionally does not cache identity encodes/decodes; campaign XML
 * repeats a small vocabulary of aliases and reference attributes heavily.
 */
public final class BrowserMemoizingNameCoder extends XmlFriendlyNameCoder {
    private final Map<java.lang.String, java.lang.String> nodeEncode = new HashMap<java.lang.String, java.lang.String>();
    private final Map<java.lang.String, java.lang.String> nodeDecode = new HashMap<java.lang.String, java.lang.String>();
    private final Map<java.lang.String, java.lang.String> attributeEncode = new HashMap<java.lang.String, java.lang.String>();
    private final Map<java.lang.String, java.lang.String> attributeDecode = new HashMap<java.lang.String, java.lang.String>();

    @Override
    public java.lang.String encodeNode(final java.lang.String name) {
        return memo(nodeEncode, name, true, true);
    }

    @Override
    public java.lang.String decodeNode(final java.lang.String name) {
        return memo(nodeDecode, name, false, true);
    }

    @Override
    public java.lang.String encodeAttribute(final java.lang.String name) {
        return memo(attributeEncode, name, true, false);
    }

    @Override
    public java.lang.String decodeAttribute(final java.lang.String name) {
        return memo(attributeDecode, name, false, false);
    }

    private java.lang.String memo(final Map<java.lang.String, java.lang.String> cache,
                                  final java.lang.String name,
                                  final boolean encode,
                                  final boolean node) {
        final java.lang.String cached = cache.get(name);
        if (cached != null) return cached;
        final java.lang.String result;
        if (encode) {
            result = node ? super.encodeNode(name) : super.encodeAttribute(name);
        } else {
            result = node ? super.decodeNode(name) : super.decodeAttribute(name);
        }
        if (name != null && result != null) cache.put(name, result);
        return result;
    }

    @Override
    public java.lang.Object clone() {
        // AbstractReader clones the driver's NameCoder for each reader. Return a
        // fresh memo table while preserving exactly the stock coding rules.
        return new BrowserMemoizingNameCoder();
    }
}
