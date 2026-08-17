/*
 * Java 17 compatibility replacement for XStream 1.4.10's Fields helper.
 *
 * Java 17 module boundaries reject setAccessible(true) for java.base fields.
 * XStream 1.4.10 already has dedicated converters for EnumSet, EnumMap and
 * other JDK types, but those converters are disabled when Fields.locate()
 * returns null. Keep the Field metadata and use sun.misc.Unsafe as a fallback
 * for field access when normal reflection is blocked. This mirrors XStream's
 * own SunUnsafeReflectionProvider and preserves the original converter paths.
 */
package com.thoughtworks.xstream.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;

import sun.misc.Unsafe;

public class Fields {
    private static final String UNSAFE_READ_FAST_PATH_PROPERTY =
            "starsector.browserXstreamUnsafeReadFastPath";
    private static final Unsafe UNSAFE = initUnsafe();
    private static final boolean UNSAFE_READ_FAST_PATH =
            Boolean.parseBoolean(System.getProperty(UNSAFE_READ_FAST_PATH_PROPERTY, "false"));

    private static Unsafe initUnsafe() {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (final Throwable ignored) {
            return null;
        }
    }

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
                try {
                    field.setAccessible(true);
                } catch (final RuntimeException inaccessibleOnJava9Plus) {
                    if (UNSAFE == null) {
                        field = null;
                    }
                }
            }
        } catch (final SecurityException e) {
            field = null;
        } catch (final RuntimeException e) {
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
                try {
                    result.setAccessible(true);
                } catch (final RuntimeException inaccessibleOnJava9Plus) {
                    if (UNSAFE == null) {
                        throw inaccessibleOnJava9Plus;
                    }
                }
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
            return;
        } catch (final Throwable reflectionFailure) {
            if (UNSAFE != null) {
                try {
                    unsafeWrite(field, instance, value);
                    return;
                } catch (final Throwable unsafeFailure) {
                    throw wrap("Cannot write field", field.getType(), field.getName(), unsafeFailure);
                }
            }
            throw wrap("Cannot write field", field.getType(), field.getName(), reflectionFailure);
        }
    }

    public static Object read(final Field field, final Object instance) {
        // XStream's hot serialization path visits every field through this helper.
        // CheerpJ pays a high bridge/reflection cost for Field.get(), while XStream
        // already relies on sun.misc.Unsafe for its preferred reflection provider.
        // Keep volatile reads on the reflection path to preserve Java visibility
        // semantics; non-volatile fields can use the same raw field access XStream's
        // SunUnsafeReflectionProvider uses for construction/writes.
        if (UNSAFE_READ_FAST_PATH && UNSAFE != null
                && !Modifier.isVolatile(field.getModifiers())) {
            try {
                return unsafeRead(field, instance);
            } catch (final Throwable ignoredUnsafeFastPathFailure) {
                // Preserve the established compatibility path below.
            }
        }
        try {
            return field.get(instance);
        } catch (final Throwable reflectionFailure) {
            if (UNSAFE != null) {
                try {
                    return unsafeRead(field, instance);
                } catch (final Throwable unsafeFailure) {
                    throw wrap("Cannot read field", field.getType(), field.getName(), unsafeFailure);
                }
            }
            throw wrap("Cannot read field", field.getType(), field.getName(), reflectionFailure);
        }
    }

    public static boolean isUnsafeReadFastPathEnabled() {
        return UNSAFE_READ_FAST_PATH && UNSAFE != null;
    }

    private static Object unsafeRead(final Field field, final Object instance) {
        final boolean isStatic = Modifier.isStatic(field.getModifiers());
        final Object base = isStatic ? UNSAFE.staticFieldBase(field) : instance;
        final long offset = isStatic ? UNSAFE.staticFieldOffset(field) : UNSAFE.objectFieldOffset(field);
        final Class type = field.getType();
        if (!type.isPrimitive()) return UNSAFE.getObject(base, offset);
        if (type == Integer.TYPE) return Integer.valueOf(UNSAFE.getInt(base, offset));
        if (type == Long.TYPE) return Long.valueOf(UNSAFE.getLong(base, offset));
        if (type == Short.TYPE) return Short.valueOf(UNSAFE.getShort(base, offset));
        if (type == Character.TYPE) return Character.valueOf(UNSAFE.getChar(base, offset));
        if (type == Byte.TYPE) return Byte.valueOf(UNSAFE.getByte(base, offset));
        if (type == Float.TYPE) return Float.valueOf(UNSAFE.getFloat(base, offset));
        if (type == Double.TYPE) return Double.valueOf(UNSAFE.getDouble(base, offset));
        if (type == Boolean.TYPE) return Boolean.valueOf(UNSAFE.getBoolean(base, offset));
        throw new IllegalArgumentException("Unsupported primitive field type: " + type.getName());
    }

    private static void unsafeWrite(final Field field, final Object instance, final Object value) {
        final boolean isStatic = Modifier.isStatic(field.getModifiers());
        final Object base = isStatic ? UNSAFE.staticFieldBase(field) : instance;
        final long offset = isStatic ? UNSAFE.staticFieldOffset(field) : UNSAFE.objectFieldOffset(field);
        final Class type = field.getType();
        if (!type.isPrimitive()) { UNSAFE.putObject(base, offset, value); return; }
        if (type == Integer.TYPE) { UNSAFE.putInt(base, offset, ((Integer)value).intValue()); return; }
        if (type == Long.TYPE) { UNSAFE.putLong(base, offset, ((Long)value).longValue()); return; }
        if (type == Short.TYPE) { UNSAFE.putShort(base, offset, ((Short)value).shortValue()); return; }
        if (type == Character.TYPE) { UNSAFE.putChar(base, offset, ((Character)value).charValue()); return; }
        if (type == Byte.TYPE) { UNSAFE.putByte(base, offset, ((Byte)value).byteValue()); return; }
        if (type == Float.TYPE) { UNSAFE.putFloat(base, offset, ((Float)value).floatValue()); return; }
        if (type == Double.TYPE) { UNSAFE.putDouble(base, offset, ((Double)value).doubleValue()); return; }
        if (type == Boolean.TYPE) { UNSAFE.putBoolean(base, offset, ((Boolean)value).booleanValue()); return; }
        throw new IllegalArgumentException("Unsupported primitive field type: " + type.getName());
    }

    private static ObjectAccessException wrap(final String message, final Class type, final String name,
            final Throwable ex) {
        final ObjectAccessException exception = new ObjectAccessException(message, ex);
        exception.add("field", type.getName() + "." + name);
        return exception;
    }
}
