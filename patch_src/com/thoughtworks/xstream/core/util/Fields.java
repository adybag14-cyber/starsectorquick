package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Runtime patch for Java 9+ module access restrictions.
 *
 * The upstream XStream 1.4.10 implementation only catches SecurityException
 * around setAccessible(true). On modern JDKs this can throw
 * InaccessibleObjectException (RuntimeException), which aborts static init for
 * converters such as TreeMapConverter. We tolerate that and fall back to
 * returning null from locate(...), which is supported by XStream's converters.
 */
public class Fields {
    public static Field locate(Class definedIn, Class fieldType, boolean isStatic) {
        Field result = null;
        try {
            Field[] fields = definedIn.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (Modifier.isStatic(fields[i].getModifiers()) == isStatic
                        && fieldType.isAssignableFrom(fields[i].getType())) {
                    result = fields[i];
                }
            }
            if (result != null && !result.isAccessible()) {
                try {
                    result.setAccessible(true);
                } catch (RuntimeException ex) {
                    // Includes InaccessibleObjectException on modular JDKs.
                    return null;
                }
            }
        } catch (SecurityException e) {
            // Keep upstream behavior: ignore and return best effort.
        } catch (NoClassDefFoundError e) {
            // Keep upstream behavior: ignore and return best effort.
        }
        return result;
    }

    public static Field find(Class definedIn, String fieldName) {
        try {
            Field field = definedIn.getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                try {
                    field.setAccessible(true);
                } catch (RuntimeException ex) {
                    throw wrap("Cannot access field", definedIn, fieldName, ex);
                }
            }
            return field;
        } catch (SecurityException e) {
            throw wrap("Cannot access field", definedIn, fieldName, e);
        } catch (NoSuchFieldException e) {
            throw wrap("Cannot access field", definedIn, fieldName, e);
        } catch (NoClassDefFoundError e) {
            throw wrap("Cannot access field", definedIn, fieldName, e);
        }
    }

    public static void write(Field field, Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (SecurityException e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        } catch (IllegalArgumentException e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        } catch (IllegalAccessException e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        } catch (NoClassDefFoundError e) {
            throw wrap("Cannot write field", field.getType(), field.getName(), e);
        }
    }

    public static Object read(Field field, Object instance) {
        try {
            return field.get(instance);
        } catch (SecurityException e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        } catch (IllegalArgumentException e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        } catch (IllegalAccessException e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        } catch (NoClassDefFoundError e) {
            throw wrap("Cannot read field", field.getType(), field.getName(), e);
        }
    }

    private static ObjectAccessException wrap(
            String message, Class type, String fieldName, Throwable cause) {
        String field = type == null ? fieldName : type.getName() + "." + fieldName;
        return new ObjectAccessException(message + " (" + field + ")", cause);
    }
}
