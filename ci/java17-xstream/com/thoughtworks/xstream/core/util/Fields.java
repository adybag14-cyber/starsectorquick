/*
 * Java 17 compatibility replacement for XStream 1.4.10's Fields helper.
 *
 * XStream 1.4.10 calls setAccessible(true) while its collection converters are
 * being initialized. Java 17 throws InaccessibleObjectException for private
 * java.base fields unless the process was started with --add-opens. CheerpJ
 * does not expose that JVM startup flag, so inaccessible optional fields must
 * be treated exactly like fields blocked by a SecurityManager: unavailable.
 */
package com.thoughtworks.xstream.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;

public class Fields {
    public static Field locate(final Class definedIn, final Class fieldType, final boolean isStatic) {
        Field field = null;
        try {
            final Field[] fields = definedIn.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                if (Modifier.isStatic(fields[i].getModifiers()) == isStatic
                        && fieldType.isAssignableFrom(fields[i].getType())) {
                    field = fields[i];
                }
            }
            if (field != null && !field.isAccessible()) {
                field.setAccessible(true);
            }
        } catch (final SecurityException e) {
            field = null;
        } catch (final RuntimeException e) {
            // Java 9+ InaccessibleObjectException is a RuntimeException.
            field = null;
        } catch (final NoClassDefFoundError e) {
            field = null;
        }
        return field;
    }

    public static Field find(final Class type, final String name) {
        try {
            final Field result = type.getDeclaredField(name);
            if (!result.isAccessible()) {
                result.setAccessible(true);
            }
            return result;
        } catch (final SecurityException e) {
            throw wrap("Cannot access field", type, name, e);
        } catch (final RuntimeException e) {
            throw wrap("Cannot access field", type, name, e);
        } catch (final NoSuchFieldException e) {
            throw wrap("Cannot access field", type, name, e);
        } catch (final NoClassDefFoundError e) {
            throw wrap("Cannot access field", type, name, e);
        }
    }

    public static void write(final Field field, final Object instance, final Object value) {
        try {
            field.set(instance, value);
        } catch (final SecurityException e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        } catch (final RuntimeException e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        } catch (final IllegalAccessException e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        } catch (final NoClassDefFoundError e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        }
    }

    public static Object read(final Field field, final Object instance) {
        try {
            return field.get(instance);
        } catch (final SecurityException e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        } catch (final RuntimeException e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        } catch (final IllegalAccessException e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        } catch (final NoClassDefFoundError e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        }
    }

    private static ObjectAccessException wrap(final String message, final Class type, final String name,
            final Throwable ex) {
        final ObjectAccessException exception = new ObjectAccessException(message, ex);
        exception.add("field", type.getName() + "." + name);
        return exception;
    }
}
