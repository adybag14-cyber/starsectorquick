/*
 * Derived from XStream 1.4.10 SerializationMembers.
 * Copyright (C) 2004, 2005 Joe Walnes.
 * Copyright (C) 2006, 2007, 2008, 2010, 2011, 2014, 2015, 2016 XStream Committers.
 * Distributed under XStream's BSD-style license.
 *
 * Java 17 compatibility change: the original assumes setAccessible(true)
 * succeeds for JDK serialization hooks such as EnumSet.writeReplace(). Java
 * 9+ module boundaries can reject that access. Treat a module-blocked hook as
 * unavailable and let XStream's registered converter handle the type instead.
 */
package com.thoughtworks.xstream.core.util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ErrorWritingException;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.core.Caching;

public class SerializationMembers implements Caching {
    private static final Method NO_METHOD = (new Object() {
        private void noMethod() {}
    }).getClass().getDeclaredMethods()[0];
    private static final Object[] EMPTY_ARGS = new Object[0];
    private static final Class[] EMPTY_CLASSES = new Class[0];
    private static final Map NO_FIELDS = Collections.EMPTY_MAP;
    private static final int PERSISTENT_FIELDS_MODIFIER = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
    private static final FastField[] OBJECT_TYPE_FIELDS = {
        new FastField(Object.class, "readResolve"),
        new FastField(Object.class, "writeReplace"),
        new FastField(Object.class, "readObject"),
        new FastField(Object.class, "writeObject")
    };
    private Map declaredCache = Collections.synchronizedMap(new HashMap());
    private Map resRepCache = Collections.synchronizedMap(new HashMap());
    private final Map fieldCache = Collections.synchronizedMap(new HashMap());
    {
        for (int i = 0; i < OBJECT_TYPE_FIELDS.length; ++i) declaredCache.put(OBJECT_TYPE_FIELDS[i], NO_METHOD);
        for (int i = 0; i < 2; ++i) resRepCache.put(OBJECT_TYPE_FIELDS[i], NO_METHOD);
    }

    public Object callReadResolve(final Object result) {
        if (result == null) return null;
        final Class resultType = result.getClass();
        final Method method = getRRMethod(resultType, "readResolve");
        if (method == null) return result;
        ErrorWritingException ex = null;
        try {
            return method.invoke(result, EMPTY_ARGS);
        } catch (final IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot access method", e);
        } catch (final InvocationTargetException e) {
            ex = new ConversionException("Failed calling method", e.getTargetException());
        }
        ex.add("method", resultType.getName() + ".readResolve()");
        throw ex;
    }

    public Object callWriteReplace(final Object object) {
        if (object == null) return null;
        final Class objectType = object.getClass();
        final Method method = getRRMethod(objectType, "writeReplace");
        if (method == null) return object;
        ErrorWritingException ex = null;
        try {
            Object replaced = method.invoke(object, EMPTY_ARGS);
            if (replaced != null && !object.getClass().equals(replaced.getClass())) replaced = callWriteReplace(replaced);
            return replaced;
        } catch (final IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot access method", e);
        } catch (final InvocationTargetException e) {
            ex = new ConversionException("Failed calling method", e.getTargetException());
        } catch (final ErrorWritingException e) {
            ex = e;
        }
        ex.add("method", objectType.getName() + ".writeReplace()");
        throw ex;
    }

    public boolean supportsReadObject(final Class type, final boolean includeBaseClasses) {
        return getMethod(type, "readObject", new Class[]{ObjectInputStream.class}, includeBaseClasses) != null;
    }

    public void callReadObject(final Class type, final Object object, final ObjectInputStream stream) {
        ErrorWritingException ex = null;
        try {
            final Method method = getMethod(type, "readObject", new Class[]{ObjectInputStream.class}, false);
            method.invoke(object, new Object[]{stream});
        } catch (final IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot access method", e);
        } catch (final InvocationTargetException e) {
            ex = new ConversionException("Failed calling method", e.getTargetException());
        }
        if (ex != null) {
            ex.add("method", object.getClass().getName() + ".readObject()");
            throw ex;
        }
    }

    public boolean supportsWriteObject(final Class type, final boolean includeBaseClasses) {
        return getMethod(type, "writeObject", new Class[]{ObjectOutputStream.class}, includeBaseClasses) != null;
    }

    public void callWriteObject(final Class type, final Object instance, final ObjectOutputStream stream) {
        ErrorWritingException ex = null;
        try {
            final Method method = getMethod(type, "writeObject", new Class[]{ObjectOutputStream.class}, false);
            method.invoke(instance, new Object[]{stream});
        } catch (final IllegalAccessException e) {
            ex = new ObjectAccessException("Cannot access method", e);
        } catch (final InvocationTargetException e) {
            ex = new ConversionException("Failed calling method", e.getTargetException());
        }
        if (ex != null) {
            ex.add("method", instance.getClass().getName() + ".writeObject()");
            throw ex;
        }
    }

    private Method getMethod(final Class type, final String name, final Class[] parameterTypes,
            final boolean includeBaseClasses) {
        final Method method = getMethod(type, name, parameterTypes);
        return method == NO_METHOD || (!includeBaseClasses && !method.getDeclaringClass().equals(type)) ? null : method;
    }

    private Method getMethod(final Class type, final String name, final Class[] parameterTypes) {
        if (type == null) return null;
        final FastField key = new FastField(type, name);
        Method result = (Method) declaredCache.get(key);
        if (result == null) {
            try {
                result = type.getDeclaredMethod(name, parameterTypes);
                if (!result.isAccessible()) {
                    try {
                        result.setAccessible(true);
                    } catch (final RuntimeException inaccessibleOnJava9Plus) {
                        result = NO_METHOD;
                    }
                }
            } catch (final NoSuchMethodException e) {
                result = getMethod(type.getSuperclass(), name, parameterTypes);
            } catch (final RuntimeException inaccessibleMetadataOnJava9Plus) {
                result = NO_METHOD;
            }
            declaredCache.put(key, result);
        }
        return result;
    }

    private Method getRRMethod(final Class type, final String name) {
        final FastField key = new FastField(type, name);
        Method result = (Method) resRepCache.get(key);
        if (result == null) {
            result = getMethod(type, name, EMPTY_CLASSES, true);
            if (result != null && result.getDeclaringClass() != type) {
                if ((result.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) == 0) {
                    if ((result.getModifiers() & Modifier.PRIVATE) > 0
                            || type.getPackage() != result.getDeclaringClass().getPackage()) result = NO_METHOD;
                }
            } else if (result == null) {
                result = NO_METHOD;
            }
            resRepCache.put(key, result);
        }
        return result == NO_METHOD ? null : result;
    }

    public Map getSerializablePersistentFields(final Class type) {
        if (type == null) return null;
        Map result = (Map) fieldCache.get(type.getName());
        if (result == null) {
            ErrorWritingException ex = null;
            try {
                final Field field = type.getDeclaredField("serialPersistentFields");
                if ((field.getModifiers() & PERSISTENT_FIELDS_MODIFIER) == PERSISTENT_FIELDS_MODIFIER) {
                    try {
                        field.setAccessible(true);
                        final ObjectStreamField[] fields = (ObjectStreamField[]) field.get(null);
                        if (fields != null) {
                            result = new HashMap();
                            for (int i = 0; i < fields.length; ++i) result.put(fields[i].getName(), fields[i]);
                        }
                    } catch (final RuntimeException inaccessibleOnJava9Plus) {
                        result = NO_FIELDS;
                    }
                }
            } catch (final NoSuchFieldException e) {
            } catch (final IllegalAccessException e) {
                ex = new ObjectAccessException("Cannot get field", e);
            } catch (final ClassCastException e) {
                ex = new ConversionException("Incompatible field type", e);
            } catch (final RuntimeException inaccessibleMetadataOnJava9Plus) {
                result = NO_FIELDS;
            }
            if (ex != null) {
                ex.add("field", type.getName() + ".serialPersistentFields");
                throw ex;
            }
            if (result == null) result = NO_FIELDS;
            fieldCache.put(type.getName(), result);
        }
        return result == NO_FIELDS ? null : result;
    }

    public void flushCache() {
        declaredCache.keySet().retainAll(Arrays.asList(OBJECT_TYPE_FIELDS));
        resRepCache.keySet().retainAll(Arrays.asList(OBJECT_TYPE_FIELDS));
    }
}
