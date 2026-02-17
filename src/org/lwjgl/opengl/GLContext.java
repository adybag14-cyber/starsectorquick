package org.lwjgl.opengl;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;

public final class GLContext {
    private static ContextCapabilities capabilities;

    static {
        try {
            capabilities = new ContextCapabilities(true);
        } catch (Throwable t) {}
    }

    public static ContextCapabilities getCapabilities() {
        return capabilities;
    }

    static ContextCapabilities getCapabilities(Object context) {
        return capabilities;
    }

    static void setCapabilities(ContextCapabilities caps) {
        capabilities = caps;
    }

    public static synchronized void useContext(Object context) throws LWJGLException {
        if (capabilities == null) {
            try {
                capabilities = new ContextCapabilities(true);
            } catch (Throwable t) {}
        }
    }

    public static synchronized void useContext(Object context, boolean forwardCompatible) throws LWJGLException {
        if (capabilities == null) {
            try {
                capabilities = new ContextCapabilities(true);
            } catch (Throwable t) {}
        }
    }

    public static synchronized void loadOpenGLLibrary() throws LWJGLException {}
    public static synchronized void unloadOpenGLLibrary() {}

    static long getFunctionAddress(String[] aliases) { return 1L; }
    static long getFunctionAddress(String name) { return 1L; }
    static int getSupportedExtensions(Set<String> supported_extensions) { return 0; }
    static void initNativeStubs(Class<?> extension_class, Set supported_extensions, String ext_name) {}
    static void resetNativeStubs(Class<?> clazz) {}
}
