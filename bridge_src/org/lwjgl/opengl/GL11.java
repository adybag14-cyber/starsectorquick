package org.lwjgl.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.MemoryUtil;

public final class GL11 {
    private static boolean getIntegerLogged = false;
    private static boolean viewportLogged = false;
    private static boolean disableLogged = false;
    private static boolean matrixModeLogged = false;
    private static boolean loadIdentityLogged = false;
    private static boolean colorMaskLogged = false;
    private static boolean pushAttribLogged = false;
    private static boolean popAttribLogged = false;
    private static final Set<String> skippedTexParameterCalls = new HashSet<String>();
    static {
        System.out.println("Bridge GL11.<clinit>()");
    }

    public static final int GL_VERSION = 7938;
    public static final int GL_EXTENSIONS = 7939;
    public static final int GL_VIEWPORT = 2978;

    private static int nextTextureId = 1;
    private static final Set<Integer> liveTextureIds = new HashSet<Integer>();

    private GL11() {}

    private static long addr(Buffer buf) {
        if (buf == null) {
            return 0L;
        }
        long base = MemoryUtil.getAddress(buf);
        if (base == 0L) {
            return 0L;
        }
        int pos = buf.position();
        if (buf instanceof ByteBuffer) {
            return base + pos;
        }
        if (buf instanceof IntBuffer || buf instanceof FloatBuffer) {
            return base + (((long) pos) << 2);
        }
        return base;
    }

    private static IntBuffer newIntBuffer(int count) {
        return ByteBuffer.allocateDirect(Math.max(1, count) * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    private static boolean isSupportedTexParameterName(int pname) {
        switch (pname) {
            case 10240: // GL_TEXTURE_MAG_FILTER
            case 10241: // GL_TEXTURE_MIN_FILTER
            case 10242: // GL_TEXTURE_WRAP_S
            case 10243: // GL_TEXTURE_WRAP_T
                return true;
            default:
                return false;
        }
    }

    private static String textureParameterName(int pname) {
        switch (pname) {
            case 10240:
                return "GL_TEXTURE_MAG_FILTER";
            case 10241:
                return "GL_TEXTURE_MIN_FILTER";
            case 10242:
                return "GL_TEXTURE_WRAP_S";
            case 10243:
                return "GL_TEXTURE_WRAP_T";
            case 33082:
                return "GL_TEXTURE_MIN_LOD";
            case 33083:
                return "GL_TEXTURE_MAX_LOD";
            case 33084:
                return "GL_TEXTURE_BASE_LEVEL";
            case 33085:
                return "GL_TEXTURE_MAX_LEVEL";
            case 33169:
                return "GL_GENERATE_MIPMAP";
            default:
                return "0x" + Integer.toHexString(pname);
        }
    }

    private static void logSkippedTexParameter(int target, int pname, int param) {
        String key = target + ":" + pname + ":" + param;
        if (skippedTexParameterCalls.add(key)) {
            System.out.println(
                    "Bridge GL11.glTexParameteri skip target="
                            + target
                            + " pname="
                            + textureParameterName(pname)
                            + " ("
                            + pname
                            + ") param="
                            + param);
        }
    }

    public static void glAlphaFunc(int p0, float p1) { nglAlphaFunc(p0, p1, 0L); }
    static native void nglAlphaFunc(int p0, float p1, long p2);

    public static void glArrayElement(int p0) {}

    public static void glBegin(int p0) { nglBegin(p0, 0L); }
    static native void nglBegin(int p0, long p1);

    public static void glBindTexture(int p0, int p1) {
        if (p1 > 0) {
            liveTextureIds.add(Integer.valueOf(p1));
        }
        nglBindTexture(p0, p1, 0L);
    }
    static native void nglBindTexture(int p0, int p1, long p2);

    public static void glBlendFunc(int p0, int p1) { nglBlendFunc(p0, p1, 0L); }
    static native void nglBlendFunc(int p0, int p1, long p2);

    public static void glCallList(int p0) { nglCallList(p0, 0L); }
    static native void nglCallList(int p0, long p1);

    public static void glClear(int p0) { nglClear(p0, 0L); }
    static native void nglClear(int p0, long p1);

    public static void glClearColor(float p0, float p1, float p2, float p3) { nglClearColor(p0, p1, p2, p3, 0L); }
    static native void nglClearColor(float p0, float p1, float p2, float p3, long p4);

    public static void glColor3d(double p0, double p1, double p2) { glColor3f((float) p0, (float) p1, (float) p2); }

    public static void glColor3f(float p0, float p1, float p2) { nglColor3f(p0, p1, p2, 0L); }
    static native void nglColor3f(float p0, float p1, float p2, long p3);

    public static void glColor3ub(byte p0, byte p1, byte p2) { glColor4ub(p0, p1, p2, (byte) 255); }

    public static void glColor4f(float p0, float p1, float p2, float p3) { nglColor4f(p0, p1, p2, p3, 0L); }
    static native void nglColor4f(float p0, float p1, float p2, float p3, long p4);

    public static void glColor4ub(byte p0, byte p1, byte p2, byte p3) {
        glColor4f(
                (p0 & 0xFF) / 255.0f,
                (p1 & 0xFF) / 255.0f,
                (p2 & 0xFF) / 255.0f,
                (p3 & 0xFF) / 255.0f);
    }
    static native void nglColor4ub(byte p0, byte p1, byte p2, byte p3, long p4);

    public static void glColorMask(boolean p0, boolean p1, boolean p2, boolean p3) {
        if (!colorMaskLogged) {
            colorMaskLogged = true;
            System.out.println(
                    "Bridge GL11.glColorMask(" + p0 + ", " + p1 + ", " + p2 + ", " + p3 + ")");
        }
        nglColorMask(p0, p1, p2, p3, 0L);
    }
    static native void nglColorMask(boolean p0, boolean p1, boolean p2, boolean p3, long p4);

    public static void glColorPointer(int p0, int p1, FloatBuffer p2) { nglColorPointer(p0, 5126 /* GL_FLOAT */, p1, addr(p2), 0L); }
    public static void glColorPointer(int p0, int p1, int p2, long p3) { nglColorPointer(p0, p1, p2, p3, 0L); }
    public static void glColorPointer(int p0, boolean p1, int p2, ByteBuffer p3) { nglColorPointer(p0, p1 ? 5121 : 5120, p2, addr(p3), 0L); }
    static native void nglColorPointer(int p0, int p1, int p2, long p3, long p4);

    public static void glDeleteTextures(int p0) {
        liveTextureIds.remove(Integer.valueOf(p0));
    }

    public static void glDeleteTextures(IntBuffer p0) {
        if (p0 == null) {
            return;
        }
        IntBuffer dup = p0.duplicate();
        while (dup.hasRemaining()) {
            liveTextureIds.remove(Integer.valueOf(dup.get()));
        }
    }

    public static void glDisable(int p0) {
        if (!disableLogged) {
            disableLogged = true;
            System.out.println("Bridge GL11.glDisable(" + p0 + ")");
        }
        nglDisable(p0, 0L);
    }
    static native void nglDisable(int p0, long p1);

    public static void glDisableClientState(int p0) { nglDisableClientState(p0, 0L); }
    static native void nglDisableClientState(int p0, long p1);

    public static void glDrawArrays(int p0, int p1, int p2) { nglDrawArrays(p0, p1, p2, 0L); }
    static native void nglDrawArrays(int p0, int p1, int p2, long p3);

    public static void glDrawElements(int p0, IntBuffer p1) {
        if (p1 == null) {
            return;
        }
        glDrawArrays(p0, 0, p1.remaining());
    }

    public static void glEnable(int p0) { nglEnable(p0, 0L); }
    static native void nglEnable(int p0, long p1);

    public static void glEnableClientState(int p0) { nglEnableClientState(p0, 0L); }
    static native void nglEnableClientState(int p0, long p1);

    public static void glEnd() { nglEnd(0L); }
    static native void nglEnd(long p0);

    public static void glEndList() { nglEndList(0L); }
    static native void nglEndList(long p0);

    public static void glFinish() { nglFlush(0L); }

    public static void glFlush() { nglFlush(0L); }
    static native void nglFlush(long p0);

    public static int glGetError() {
        return 0;
    }

    public static int glGenLists(int p0) { return nglGenLists(p0, 0L); }
    static native int nglGenLists(int p0, long p1);

    public static void glDeleteLists(int p0, int p1) {}

    public static void glGenTextures(IntBuffer p0) {
        if (p0 == null || !p0.hasRemaining()) {
            return;
        }
        int count = p0.remaining();
        long ptr = addr(p0);
        if (ptr != 0L) {
            nglGenTextures(count, ptr, 0L);
            IntBuffer dup = p0.duplicate();
            while (dup.hasRemaining()) {
                liveTextureIds.add(Integer.valueOf(dup.get()));
            }
            return;
        }
        IntBuffer dup = p0.duplicate();
        while (dup.hasRemaining()) {
            int id = nextTextureId++;
            dup.put(id);
            liveTextureIds.add(Integer.valueOf(id));
        }
    }
    static native void nglGenTextures(int p0, long p1, long p2);

    public static int glGetInteger(int p0) {
        if (!getIntegerLogged) {
            getIntegerLogged = true;
            System.out.println("Bridge GL11.glGetInteger(" + p0 + ")");
        }
        IntBuffer tmp = newIntBuffer(4);
        glGetInteger(p0, tmp);
        return tmp.get(0);
    }

    public static void glGetInteger(int p0, IntBuffer p1) {
        if (p1 == null || !p1.hasRemaining()) {
            return;
        }
        long ptr = addr(p1);
        if (ptr != 0L) {
            nglGetIntegerv(p0, ptr, 0L);
        }
    }
    static native void nglGetIntegerv(int p0, long p1, long p2);

    public static String glGetString(int p0) {
        try {
            String s = nglGetString(p0, 0L);
            return s == null ? "" : s;
        } catch (Throwable ignored) {
            return "";
        }
    }
    static native String nglGetString(int p0, long p1);

    public static void glHint(int p0, int p1) {}

    public static void glLight(int p0, int p1, FloatBuffer p2) {
        long ptr = addr(p2);
        if (ptr != 0L) {
            nglLightfv(p0, p1, ptr, 0L);
        }
    }
    static native void nglLightfv(int p0, int p1, long p2, long p3);

    public static void glLineWidth(float p0) { nglLineWidth(p0, 0L); }
    static native void nglLineWidth(float p0, long p1);

    public static void glLoadIdentity() {
        if (!loadIdentityLogged) {
            loadIdentityLogged = true;
            System.out.println("Bridge GL11.glLoadIdentity()");
        }
        nglLoadIdentity(0L);
    }
    static native void nglLoadIdentity(long p0);

    public static void glMaterial(int p0, int p1, FloatBuffer p2) {}
    public static void glMateriali(int p0, int p1, int p2) {}
    public static void glColorMaterial(int p0, int p1) {}

    public static void glMatrixMode(int p0) {
        if (!matrixModeLogged) {
            matrixModeLogged = true;
            System.out.println("Bridge GL11.glMatrixMode(" + p0 + ")");
        }
        nglMatrixMode(p0, 0L);
    }
    static native void nglMatrixMode(int p0, long p1);

    public static void glNewList(int p0, int p1) { nglNewList(p0, p1, 0L); }
    static native void nglNewList(int p0, int p1, long p2);

    public static void glNormal3f(float p0, float p1, float p2) { nglNormal3f(p0, p1, p2, 0L); }
    static native void nglNormal3f(float p0, float p1, float p2, long p3);

    public static void glOrtho(double p0, double p1, double p2, double p3, double p4, double p5) { nglOrtho(p0, p1, p2, p3, p4, p5, 0L); }
    static native void nglOrtho(double p0, double p1, double p2, double p3, double p4, double p5, long p6);

    public static void glPointSize(float p0) {}
    public static void glPixelStorei(int p0, int p1) {}
    public static void glPolygonMode(int p0, int p1) {}
    public static boolean glIsEnabled(int p0) { return true; }

    public static void glPopAttrib() {
        if (!popAttribLogged) {
            popAttribLogged = true;
            System.out.println("Bridge GL11.glPopAttrib()");
        }
    }
    static native void nglPopAttrib(long p0);

    public static void glPopMatrix() { nglPopMatrix(0L); }
    static native void nglPopMatrix(long p0);

    public static void glPushAttrib(int p0) {
        if (!pushAttribLogged) {
            pushAttribLogged = true;
            System.out.println("Bridge GL11.glPushAttrib(" + p0 + ")");
        }
    }
    static native void nglPushAttrib(int p0, long p1);

    public static void glPushMatrix() { nglPushMatrix(0L); }
    static native void nglPushMatrix(long p0);

    public static void glMultMatrix(FloatBuffer p0) {
        long ptr = addr(p0);
        if (ptr != 0L) {
            nglMultMatrixf(ptr, 0L);
        }
    }
    static native void nglMultMatrixf(long p0, long p1);

    public static void glReadPixels(int p0, int p1, int p2, int p3, int p4, int p5, FloatBuffer p6) {
        if (p6 == null) {
            return;
        }
        for (int i = p6.position(); i < p6.limit(); i++) {
            p6.put(i, 0.0f);
        }
    }

    public static void glRotatef(float p0, float p1, float p2, float p3) { nglRotatef(p0, p1, p2, p3, 0L); }
    static native void nglRotatef(float p0, float p1, float p2, float p3, long p4);

    public static void glScalef(float p0, float p1, float p2) { nglScalef(p0, p1, p2, 0L); }
    static native void nglScalef(float p0, float p1, float p2, long p3);

    public static void glScissor(int p0, int p1, int p2, int p3) {}

    public static void glShadeModel(int p0) { nglShadeModel(p0, 0L); }
    static native void nglShadeModel(int p0, long p1);

    public static void glStencilFunc(int p0, int p1, int p2) {}
    public static void glStencilOp(int p0, int p1, int p2) {}

    public static void glTexCoord2f(float p0, float p1) { nglTexCoord2f(p0, p1, 0L); }
    static native void nglTexCoord2f(float p0, float p1, long p2);

    public static void glTexCoordPointer(int p0, int p1, FloatBuffer p2) { nglTexCoordPointer(p0, 5126 /* GL_FLOAT */, p1, addr(p2), 0L); }
    public static void glTexCoordPointer(int p0, int p1, int p2, long p3) { nglTexCoordPointer(p0, p1, p2, p3, 0L); }
    static native void nglTexCoordPointer(int p0, int p1, int p2, long p3, long p4);

    public static void glTexEnvf(int p0, int p1, float p2) {}

    public static void glTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, ByteBuffer p8) {
        nglTexImage2D(p0, p1, p2, p3, p4, p5, p6, p7, addr(p8), 0L);
    }
    static native void nglTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8, long p9);

    public static void glTexParameteri(int p0, int p1, int p2) {
        if (!isSupportedTexParameterName(p1)) {
            logSkippedTexParameter(p0, p1, p2);
            return;
        }
        nglTexParameteri(p0, p1, p2, 0L);
    }
    static native void nglTexParameteri(int p0, int p1, int p2, long p3);

    public static void glTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, ByteBuffer p8) {
        nglTexSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, addr(p8), 0L);
    }
    static native void nglTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8, long p9);

    public static void glTranslated(double p0, double p1, double p2) { glTranslatef((float) p0, (float) p1, (float) p2); }

    public static void glTranslatef(float p0, float p1, float p2) { nglTranslatef(p0, p1, p2, 0L); }
    static native void nglTranslatef(float p0, float p1, float p2, long p3);

    public static void glVertex2f(float p0, float p1) { glVertex3f(p0, p1, 0.0f); }
    static native void nglVertex2f(float p0, float p1, long p2);

    public static void glVertex3d(double p0, double p1, double p2) { glVertex3f((float) p0, (float) p1, (float) p2); }

    public static void glVertex3f(float p0, float p1, float p2) { nglVertex3f(p0, p1, p2, 0L); }
    static native void nglVertex3f(float p0, float p1, float p2, long p3);

    public static void glVertexPointer(int p0, int p1, FloatBuffer p2) { nglVertexPointer(p0, 5126 /* GL_FLOAT */, p1, addr(p2), 0L); }
    public static void glVertexPointer(int p0, int p1, IntBuffer p2) { nglVertexPointer(p0, 5124 /* GL_INT */, p1, addr(p2), 0L); }
    public static void glVertexPointer(int p0, int p1, int p2, long p3) { nglVertexPointer(p0, p1, p2, p3, 0L); }
    static native void nglVertexPointer(int p0, int p1, int p2, long p3, long p4);

    public static void glInterleavedArrays(int p0, int p1, FloatBuffer p2) {}

    public static void glViewport(int p0, int p1, int p2, int p3) {
        if (!viewportLogged) {
            viewportLogged = true;
            System.out.println(
                    "Bridge GL11.glViewport("
                            + p0
                            + ", "
                            + p1
                            + ", "
                            + p2
                            + ", "
                            + p3
                            + ")");
        }
        nglViewport(p0, p1, p2, p3, 0L);
    }
    static native void nglViewport(int p0, int p1, int p2, int p3, long p4);
}
