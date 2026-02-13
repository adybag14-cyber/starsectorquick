package org.lwjgl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class MemoryUtil {
    public interface Accessor {}

    private static final Object UNSAFE;
    private static final Method OBJECT_FIELD_OFFSET;
    private static final Method GET_LONG_OBJECT_OFFSET;
    private static final long ADDRESS_OFFSET;
    private static final Field ADDRESS_FIELD;

    static {
        Object unsafe = null;
        Method objectFieldOffset = null;
        Method getLongObjectOffset = null;
        long addressOffset = -1L;
        Field addressField = null;

        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = theUnsafe.get(null);
            objectFieldOffset = unsafeClass.getMethod("objectFieldOffset", Field.class);
            getLongObjectOffset = unsafeClass.getMethod("getLong", Object.class, long.class);
            Field f = Buffer.class.getDeclaredField("address");
            addressOffset = ((Long) objectFieldOffset.invoke(unsafe, f)).longValue();
            addressField = f;
        } catch (Throwable ignored) {}

        if (unsafe == null) {
            try {
                Field f = Buffer.class.getDeclaredField("address");
                f.setAccessible(true);
                addressField = f;
            } catch (Throwable ignored) {}
        }

        UNSAFE = unsafe;
        OBJECT_FIELD_OFFSET = objectFieldOffset;
        GET_LONG_OBJECT_OFFSET = getLongObjectOffset;
        ADDRESS_OFFSET = addressOffset;
        ADDRESS_FIELD = addressField;
    }

    private MemoryUtil() {}

    public static long getAddress(Buffer buffer) {
        if (buffer == null) {
            return 0L;
        }
        if (UNSAFE != null && OBJECT_FIELD_OFFSET != null && GET_LONG_OBJECT_OFFSET != null && ADDRESS_OFFSET >= 0L) {
            try {
                return ((Long) GET_LONG_OBJECT_OFFSET.invoke(UNSAFE, buffer, Long.valueOf(ADDRESS_OFFSET))).longValue();
            } catch (Throwable ignored) {}
        }
        if (ADDRESS_FIELD == null) {
            return 0L;
        }
        try {
            return ADDRESS_FIELD.getLong(buffer);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    public static long getAddress(ByteBuffer buffer) { return getAddress((Buffer) buffer); }
    public static long getAddress(ShortBuffer buffer) { return getAddress((Buffer) buffer); }
    public static long getAddress(IntBuffer buffer) { return getAddress((Buffer) buffer); }
    public static long getAddress(LongBuffer buffer) { return getAddress((Buffer) buffer); }
    public static long getAddress(FloatBuffer buffer) { return getAddress((Buffer) buffer); }
    public static long getAddress(DoubleBuffer buffer) { return getAddress((Buffer) buffer); }
    public static long getAddress(CharBuffer buffer) { return getAddress((Buffer) buffer); }

    public static long getAddress(Buffer buffer, int position) {
        long base = getAddress(buffer);
        if (base == 0L || buffer == null) {
            return 0L;
        }
        if (buffer instanceof ByteBuffer) return base + position;
        if (buffer instanceof ShortBuffer || buffer instanceof CharBuffer) return base + (((long) position) << 1);
        if (buffer instanceof IntBuffer || buffer instanceof FloatBuffer) return base + (((long) position) << 2);
        if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) return base + (((long) position) << 3);
        return base + position;
    }

    public static long getAddress(ByteBuffer buffer, int position) { return getAddress((Buffer) buffer, position); }
    public static long getAddress(ShortBuffer buffer, int position) { return getAddress((Buffer) buffer, position); }
    public static long getAddress(IntBuffer buffer, int position) { return getAddress((Buffer) buffer, position); }
    public static long getAddress(LongBuffer buffer, int position) { return getAddress((Buffer) buffer, position); }
    public static long getAddress(FloatBuffer buffer, int position) { return getAddress((Buffer) buffer, position); }
    public static long getAddress(DoubleBuffer buffer, int position) { return getAddress((Buffer) buffer, position); }
    public static long getAddress(CharBuffer buffer, int position) { return getAddress((Buffer) buffer, position); }

    public static long getAddressSafe(ByteBuffer buffer) { return buffer == null ? 0L : getAddress(buffer); }
    public static long getAddressSafe(ShortBuffer buffer) { return buffer == null ? 0L : getAddress(buffer); }
    public static long getAddressSafe(IntBuffer buffer) { return buffer == null ? 0L : getAddress(buffer); }
    public static long getAddressSafe(LongBuffer buffer) { return buffer == null ? 0L : getAddress(buffer); }
    public static long getAddressSafe(FloatBuffer buffer) { return buffer == null ? 0L : getAddress(buffer); }
    public static long getAddressSafe(DoubleBuffer buffer) { return buffer == null ? 0L : getAddress(buffer); }

    public static long getAddress0(Buffer buffer) { return getAddress(buffer); }
    public static long getAddress0Safe(Buffer buffer) { return buffer == null ? 0L : getAddress(buffer); }
    public static long getAddress(org.lwjgl.PointerBuffer buffer) {
        if (buffer == null) {
            return 0L;
        }
        return getAddress(buffer.getBuffer());
    }
    public static long getAddress(org.lwjgl.PointerBuffer buffer, int position) {
        if (buffer == null) {
            return 0L;
        }
        return getAddress(buffer.getBuffer(), position * org.lwjgl.PointerBuffer.getPointerSize());
    }
    public static long getAddressSafe(org.lwjgl.PointerBuffer buffer) { return buffer == null ? 0L : getAddress(buffer); }
    public static long getAddress0(org.lwjgl.PointerBuffer buffer) { return getAddress(buffer); }

    public static ByteBuffer encodeASCII(CharSequence text) { return encode(text, StandardCharsets.US_ASCII); }
    public static ByteBuffer encodeUTF8(CharSequence text) { return encode(text, StandardCharsets.UTF_8); }
    public static ByteBuffer encodeUTF16(CharSequence text) { return encode(text, StandardCharsets.UTF_16LE); }
    public static ByteBuffer encode(CharSequence text, Charset charset) {
        if (text == null) {
            return null;
        }
        Charset cs = charset == null ? StandardCharsets.UTF_8 : charset;
        byte[] raw = text.toString().getBytes(cs);
        ByteBuffer out = ByteBuffer.allocateDirect(raw.length + 1);
        out.put(raw);
        out.put((byte) 0);
        out.flip();
        return out;
    }
    public static ByteBuffer encode(CharBuffer text, Charset charset) { return text == null ? null : encode((CharSequence) text, charset); }

    public static String decodeUTF8(ByteBuffer buffer) { return decode(buffer, StandardCharsets.UTF_8); }
    public static String decode(ByteBuffer buffer, Charset charset) {
        if (buffer == null) {
            return null;
        }
        Charset cs = charset == null ? StandardCharsets.UTF_8 : charset;
        ByteBuffer dup = buffer.duplicate();
        byte[] raw = new byte[Math.max(0, dup.remaining())];
        dup.get(raw);
        int len = raw.length;
        while (len > 0 && raw[len - 1] == 0) {
            len--;
        }
        return new String(raw, 0, len, cs);
    }

    public static Field getAddressField() { return ADDRESS_FIELD; }
    public static Field getDeclaredFieldRecursive(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }
    public static Accessor loadAccessor(String type) { return null; }
    public static String decodeImpl(ByteBuffer buffer, Charset charset) { return decode(buffer, charset); }
}
