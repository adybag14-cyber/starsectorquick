package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.MemoryUtil;
import org.lwjgl.Zys;

public final class GLContex2 {
    private static final ThreadLocal<ContextCapabilities> current_capabilities = new ThreadLocal<ContextCapabilities>();
    private static final Map<Object, ContextCapabilities> capability_cache = new WeakHashMap<Object, ContextCapabilities>();
    private static int gl_ref_count;
    private static boolean did_auto_load;

    public static ContextCapabilities getCapabilities() {
        ContextCapabilities caps = current_capabilities.get();
        if (caps == null) {
            // CheerpJ path: no native GL context is created. Create a synthetic one on demand.
            try {
                Object key = Thread.currentThread();
                ContextCapabilities cached = capability_cache.get(key);
                if (cached == null) {
                    cached = new ContextCapabilities(false);
                    capability_cache.put(key, cached);
                }
                current_capabilities.set(cached);
                caps = cached;
                System.out.println("GLContex2: created fallback context capabilities");
            } catch (Throwable t) {
                throw new RuntimeException("No OpenGL context found in the current thread.", t);
            }
        }
        return caps;
    }

    static ContextCapabilities getCapabilities(Object context) {
        return capability_cache.get(context);
    }

    static void setCapabilities(ContextCapabilities capabilities) {
        current_capabilities.set(capabilities);
    }

    static long getFunctionAddress(String[] aliases) {
        return 1L;
    }

    static long getFunctionAddress(String name) {
        return 1L;
    }

    static int getSupportedExtensions(Set<String> supported_extensions) {
        try {
            String version = GL1A.glGetString(GL1A.GL_VERSION);
            if (version != null) {
                supported_extensions.add("OpenGL11");
                supported_extensions.add("OpenGL12");
                supported_extensions.add("OpenGL13");
                supported_extensions.add("OpenGL14");
                supported_extensions.add("OpenGL15");
                supported_extensions.add("OpenGL20");
                
                String extensions_string = GL1A.glGetString(GL1A.GL_EXTENSIONS);
                if (extensions_string != null) {
                    StringTokenizer tokenizer = new StringTokenizer(extensions_string);
                    while (tokenizer.hasMoreTokens()) {
                        supported_extensions.add(tokenizer.nextToken());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    static void initNativeStubs(Class<?> extension_class, Set supported_extensions, String ext_name) {
        resetNativeStubs(extension_class);
    }

    public static synchronized void useContext(Object context) throws LWJGLException {
        useContext(context, false);
    }

    public static synchronized void useContext(Object context, boolean forwardCompatible) throws LWJGLException {
        if (context == null) {
            setCapabilities(null);
            return;
        }
        if (gl_ref_count == 0) {
            loadOpenGLLibrary();
            did_auto_load = true;
        }
        ContextCapabilities capabilities = capability_cache.get(context);
        if (capabilities == null) {
            new ContextCapabilities(forwardCompatible);
            capability_cache.put(context, getCapabilities());
        } else {
            setCapabilities(capabilities);
        }
    }

    public static synchronized void loadOpenGLLibrary() throws LWJGLException {
        System.out.println("GLContext.loadOpenGLLibrary");
        ++gl_ref_count;
    }

    public static synchronized void unloadOpenGLLibrary() {
        if (--gl_ref_count == 0) {
        }
    }

    static void resetNativeStubs(Class<?> clazz) {}

    static {
        Zys.initialize();
    }
}
