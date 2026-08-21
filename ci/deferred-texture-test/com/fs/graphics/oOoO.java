package com.fs.graphics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Test-only stand-in used to verify BrowserDeferredTextureQueue without OpenGL. */
public final class oOoO {
    public static final List<java.lang.String> LOADS = new ArrayList<java.lang.String>();
    public static final Map<java.lang.String, java.lang.String> REGISTRY = new LinkedHashMap<java.lang.String, java.lang.String>();
    private oOoO() {}
    public static void o00000(java.lang.String key, java.lang.String path) throws IOException {
        LOADS.add(key + "=" + path);
        if (!REGISTRY.containsKey(key)) REGISTRY.put(key, path);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map o00000() { return REGISTRY; }
}
