package org.lwjgl.opengl;

public final class GL11Stub {
    public static void nglDisable(int cap, long addr) { System.out.println("GL11Stub.nglDisable(" + cap + ")"); }
    public static void nglEnable(int cap, long addr) { System.out.println("GL11Stub.nglEnable(" + cap + ")"); }
    public static void nglViewport(int x, int y, int w, int h, long addr) { System.out.println("GL11Stub.nglViewport"); }
    public static void nglClear(int mask, long addr) {}
    public static void nglClearColor(float r, float g, float b, float a, long addr) {}
    public static int nglGetError(long addr) { return 0; }
    public static String glGetString(int name) { 
        if (name == 7938) return "2.1";
        return "";
    }
}
