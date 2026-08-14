package com.fs.starfarer.loading;

/** Test-only source reader stub used to verify ooOo miss memoization. */
public final class LoadingUtils {
    public static int calls;

    public static String Õ00000(String path) throws Exception {
        calls++;
        if ("missing.java".equals(path)) throw new java.io.IOException("missing");
        return "class Present {}";
    }
}
