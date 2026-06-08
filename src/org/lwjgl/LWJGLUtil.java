package org.lwjgl;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class LWJGLUtil {
    public static final int PLATFORM_LINUX = 1;
    public static final int PLATFORM_MACOSX = 2;
    public static final int PLATFORM_WINDOWS = 3;

    public static final String PLATFORM_LINUX_NAME = "linux";
    public static final String PLATFORM_MACOSX_NAME = "macosx";
    public static final String PLATFORM_WINDOWS_NAME = "windows";

    public static final ByteBuffer LWJGLIcon16x16 = ByteBuffer.allocateDirect(0);
    public static final ByteBuffer LWJGLIcon32x32 = ByteBuffer.allocateDirect(0);

    public static final boolean DEBUG = false;
    public static final boolean CHECKS = false;

    public interface TokenFilter {
        boolean accept(Field field, int value);
    }

    public static int getPlatform() {
        return PLATFORM_LINUX;
    }

    public static String getPlatformName() {
        return PLATFORM_LINUX_NAME;
    }

    public static String mapLibraryName(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.endsWith(".so") || name.endsWith(".dll") || name.endsWith(".dylib")) {
            return name;
        }
        return "lib" + name + ".so";
    }

    public static String[] getLibraryPaths(String libname, String platformPath, ClassLoader classloader) {
        return new String[] { mapLibraryName(libname), platformPath };
    }

    public static String[] getLibraryPaths(String libname, String[] platformPaths, ClassLoader classloader) {
        if (platformPaths == null || platformPaths.length == 0) {
            return new String[] { mapLibraryName(libname) };
        }
        String[] result = new String[platformPaths.length + 1];
        result[0] = mapLibraryName(libname);
        System.arraycopy(platformPaths, 0, result, 1, platformPaths.length);
        return result;
    }

    static void execPrivileged(String[] command) throws Exception {
        if (command == null || command.length == 0) {
            return;
        }
        new ProcessBuilder(command).start();
    }

    public static boolean getPrivilegedBoolean(String key) {
        return Boolean.parseBoolean(System.getProperty(key, "false"));
    }

    public static Integer getPrivilegedInteger(String key) {
        String value = System.getProperty(key);
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer getPrivilegedInteger(String key, int defaultValue) {
        Integer value = getPrivilegedInteger(key);
        return value != null ? value : Integer.valueOf(defaultValue);
    }

    public static void log(CharSequence s) {
        System.out.println("LWJGL: " + s);
    }

    public static boolean isMacOSXEqualsOrBetterThan(int majorVersion, int minorVersion) {
        return false;
    }

    public static Map<Integer, String> getClassTokens(
            TokenFilter filter, Map<Integer, String> target, Class<?>... classes) {
        Map<Integer, String> out = target != null ? target : new LinkedHashMap<Integer, String>();
        if (classes == null) {
            return out;
        }
        for (Class<?> cls : classes) {
            collectClassTokens(filter, out, cls);
        }
        return out;
    }

    public static Map<Integer, String> getClassTokens(
            TokenFilter filter, Map<Integer, String> target, Iterable<Class<?>> classes) {
        Map<Integer, String> out = target != null ? target : new LinkedHashMap<Integer, String>();
        if (classes == null) {
            return out;
        }
        for (Class<?> cls : classes) {
            collectClassTokens(filter, out, cls);
        }
        return out;
    }

    private static void collectClassTokens(TokenFilter filter, Map<Integer, String> out, Class<?> cls) {
        if (cls == null || out == null) {
            return;
        }
        Field[] fields = cls.getFields();
        if (fields == null) {
            return;
        }
        for (Field field : fields) {
            try {
                if (field == null || field.getType() != Integer.TYPE) {
                    continue;
                }
                int value = field.getInt(null);
                if (filter != null && !filter.accept(field, value)) {
                    continue;
                }
                if (!out.containsKey(Integer.valueOf(value))) {
                    out.put(Integer.valueOf(value), field.getName());
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static String toHexString(int value) {
        return "0x" + Integer.toHexString(value);
    }
}
