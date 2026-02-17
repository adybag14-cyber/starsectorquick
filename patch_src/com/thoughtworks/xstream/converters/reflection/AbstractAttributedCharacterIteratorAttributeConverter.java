package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.util.Fields;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Runtime-safe variant of XStream 1.4.10 converter for modular JDKs.
 *
 * The upstream static initializer only catches SecurityException/NoSuchMethodException.
 * On modular runtimes setAccessible(true) can throw RuntimeException
 * (InaccessibleObjectException). We tolerate this and keep method lookup optional.
 */
public class AbstractAttributedCharacterIteratorAttributeConverter extends AbstractSingleValueConverter {
    private static final Map instanceMaps = new HashMap();
    private static final Method getName;
    private final Class type;
    private transient Map attributeMap;

    public AbstractAttributedCharacterIteratorAttributeConverter(Class type) {
        if (!AttributedCharacterIterator.Attribute.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(
                    type.getName() + " is not a " + AttributedCharacterIterator.Attribute.class.getName());
        }
        this.type = type;
        this.readResolve();
    }

    public boolean canConvert(Class type) {
        return type == this.type && !this.attributeMap.isEmpty();
    }

    public String toString(Object source) {
        return this.getName((AttributedCharacterIterator.Attribute) source);
    }

    private String getName(AttributedCharacterIterator.Attribute attribute) {
        ReflectiveOperationException ex = null;
        if (getName != null) {
            try {
                return (String) getName.invoke(attribute, (Object[]) null);
            } catch (IllegalAccessException e) {
                ex = e;
            } catch (InvocationTargetException e) {
                ex = e;
            }
        }
        String className = attribute.getClass().getName();
        String s = attribute.toString();
        if (s.startsWith(className)) {
            return s.substring(className.length() + 1, s.length() - 1);
        }
        ConversionException exception = new ConversionException("Cannot find name of attribute", ex);
        exception.add("attribute-type", className);
        throw exception;
    }

    public Object fromString(String str) {
        if (this.attributeMap.containsKey(str)) {
            return this.attributeMap.get(str);
        }
        ConversionException exception = new ConversionException("Cannot find attribute");
        exception.add("attribute-type", this.type.getName());
        exception.add("attribute-name", str);
        throw exception;
    }

    private Object readResolve() {
        this.attributeMap = (Map) instanceMaps.get(this.type.getName());
        if (this.attributeMap == null) {
            this.attributeMap = new HashMap();
            Field instanceMap = Fields.locate(this.type, Map.class, true);
            if (instanceMap != null) {
                try {
                    Map map = (Map) Fields.read(instanceMap, null);
                    if (map != null) {
                        boolean valid = true;
                        Iterator iter = map.entrySet().iterator();
                        while (valid && iter.hasNext()) {
                            Map.Entry entry = (Map.Entry) iter.next();
                            valid = entry.getKey().getClass() == String.class
                                    && entry.getValue().getClass() == this.type;
                        }
                        if (valid) {
                            this.attributeMap.putAll(map);
                        }
                    }
                } catch (ObjectAccessException ignored) {
                }
            }
            if (this.attributeMap.isEmpty()) {
                try {
                    Field[] fields = this.type.getDeclaredFields();
                    for (int i = 0; i < fields.length; ++i) {
                        if (fields[i].getType() != this.type || !Modifier.isStatic(fields[i].getModifiers())) {
                            continue;
                        }
                        AttributedCharacterIterator.Attribute attribute =
                                (AttributedCharacterIterator.Attribute) Fields.read(fields[i], null);
                        this.attributeMap.put(this.toString(attribute), attribute);
                    }
                } catch (SecurityException e) {
                    this.attributeMap.clear();
                } catch (ObjectAccessException e) {
                    this.attributeMap.clear();
                } catch (NoClassDefFoundError e) {
                    this.attributeMap.clear();
                }
            }
            instanceMaps.put(this.type.getName(), this.attributeMap);
        }
        return this;
    }

    static {
        Method method = null;
        try {
            method = AttributedCharacterIterator.Attribute.class.getDeclaredMethod("getName", new Class[0]);
            if (!method.isAccessible()) {
                try {
                    method.setAccessible(true);
                } catch (RuntimeException ex) {
                    method = null;
                }
            }
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (RuntimeException e) {
            method = null;
        }
        getName = method;
    }
}
