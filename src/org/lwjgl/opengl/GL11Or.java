package org.lwjgl.opengl;

import java.nio.*;

public final class GL11Or {
    public static final int GL_VERSION = 7938;
    public static final int GL_EXTENSIONS = 7939;
    
    public static void glDisable(int p0) { System.out.println("GL11.glDisable(" + p0 + ")"); }
    public static void glEnable(int p0) { System.out.println("GL11.glEnable(" + p0 + ")"); }
    public static void glViewport(int p0, int p1, int p2, int p3) { System.out.println("GL11.glViewport"); }
    public static void glMatrixMode(int p0) {}
    public static void glLoadIdentity() {}
    public static void glColorMask(boolean p0, boolean p1, boolean p2, boolean p3) {}
    public static String glGetString(int p0) {
        if (p0 == 7938) return "2.1";
        if (p0 == 7939) return "GL_EXT_framebuffer_object GL_ARB_shader_objects";
        return "";
    }
    public static int glGetInteger(int p0) { return 0; }
    public static void glGetInteger(int p0, IntBuffer p1) {}
    public static boolean glIsEnabled(int p0) { return true; }
    public static void glClear(int p0) {}
    public static void glClearColor(float p0, float p1, float p2, float p3) {}
    public static void glFlush() {}
    public static int glGetError() { return 0; }
    public static void glBindTexture(int p0, int p1) {}
    public static void glTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, ByteBuffer p8) {}
    public static void glTexParameteri(int p0, int p1, int p2) {}
    public static void glReadPixels(int p0, int p1, int p2, int p3, int p4, int p5, ByteBuffer p6) {}
    public static void glReadPixels(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {}
    public static void glColor4f(float p0, float p1, float p2, float p3) {}
    public static void glColor4ub(byte p0, byte p1, byte p2, byte p3) {}
    public static void glGenLists(int p0) {}
}
