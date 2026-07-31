/*
 * Java 17-safe replacement for XStream 1.4.10's attributed-character
 * attribute converter. The original converter makes the protected
 * Attribute.getName() method accessible, which Java 17 rejects without
 * --add-opens java.base/java.text.
 */
package com.thoughtworks.xstream.converters.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

public class AbstractAttributedCharacterIteratorAttributeConverter
        extends AbstractSingleValueConverter {

    private static final Map instanceMaps = new HashMap();

    private final Class type;
    private transient Map attributeMap;

    public AbstractAttributedCharacterIteratorAttributeConverter(final Class type) {
        super();
        if (!AttributedCharacterIterator.Attribute.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(type.getName()
                    + " is not a " + AttributedCharacterIterator.Attribute.class.getName());
        }
        this.type = type;
        readResolve();
    }

    public boolean canConvert(final Class candidate) {
        return candidate == type && attributeMap != null && !attributeMap.isEmpty();
    }

    public String toString(final Object source) {
        final AttributedCharacterIterator.Attribute attribute =
                (AttributedCharacterIterator.Attribute) source;
        final String text = attribute.toString();
        final String className = attribute.getClass().getName();
        if (text.startsWith(className + "(") && text.endsWith(")")) {
            return text.substring(className.length() + 1, text.length() - 1);
        }
        if (text.startsWith(className)) {
            return text.substring(className.length());
        }
        return text;
    }

    public Object fromString(final String str) {
        if (attributeMap.containsKey(str)) {
            return attributeMap.get(str);
        }
        final ConversionException exception = new ConversionException("Cannot find attribute");
        exception.add("attribute-type", type.getName());
        exception.add("attribute-name", str);
        throw exception;
    }

    private Object readResolve() {
        attributeMap = (Map) instanceMaps.get(type.getName());
        if (attributeMap != null) {
            return this;
        }

        attributeMap = new HashMap();
        try {
            // TextAttribute exposes its constants as public static fields. Using
            // getFields()/Field.get avoids illegal access to java.text internals.
            final Field[] fields = type.getFields();
            for (int i = 0; i < fields.length; ++i) {
                final Field field = fields[i];
                if (Modifier.isStatic(field.getModifiers())
                        && AttributedCharacterIterator.Attribute.class
                                .isAssignableFrom(field.getType())) {
                    final Object value = field.get(null);
                    if (value instanceof AttributedCharacterIterator.Attribute) {
                        attributeMap.put(toString(value), value);
                    }
                }
            }
        } catch (final Throwable ignored) {
            attributeMap.clear();
        }
        instanceMaps.put(type.getName(), attributeMap);
        return this;
    }
}
