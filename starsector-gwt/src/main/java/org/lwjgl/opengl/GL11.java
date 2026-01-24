package org.lwjgl.opengl;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLCanvasElement;
import elemental2.webgl.WebGLRenderingContext;
import com.fs.starfarer.stubs.java.nio.FloatBuffer;
import com.fs.starfarer.stubs.java.nio.ByteBuffer;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;
import jsinterop.annotations.JsPackage;

@JsType(namespace = JsPackage.GLOBAL, name = "gwtGL11")
public class GL11 {
    public static WebGLRenderingContext gl;

    @JsMethod
    public static void init(HTMLCanvasElement canvas) {
        gl = (WebGLRenderingContext) (Object) canvas.getContext("webgl");
        if (gl == null) {
            gl = (WebGLRenderingContext) (Object) canvas.getContext("experimental-webgl");
        }
    }

    public static final int GL_QUADS = 7;
    public static final int GL_TRIANGLES = 4;
    public static final int GL_COLOR_BUFFER_BIT = 16384;
    public static final int GL_DEPTH_BUFFER_BIT = 256;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE = 1;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_QUAD_STRIP = 0x0008;
    public static final int GL_POINT_SMOOTH = 0x0B10;
    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINE_SMOOTH = 0x0B20;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_LINE_SMOOTH_HINT = 0x0C52;
    public static final int GL_NICEST = 0x1102;
    public static final int GL_LINES = 0x0001;
    public static final int GL_FRONT_AND_BACK = 0x0408;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_FILL = 0x1B02;
    public static final int GL_POLYGON_SMOOTH = 0x0B41;
    public static final int GL_POLYGON_SMOOTH_HINT = 0x0C53;
    public static final int GL_ZERO = 0;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_VERTEX_ARRAY = 0x8074;
    public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;
    public static final int GL_COLOR_ARRAY = 0x8076;
    public static final int GL_DST_ALPHA = 0x0304;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GL_SCISSOR_TEST = 0x0C11;

    @JsMethod
    public static void glClear(int mask) {
        if (gl != null) gl.clear(mask);
    }

    @JsMethod
    public static void glClearColor(float r, float g, float b, float a) {
        if (gl != null) gl.clearColor(r, g, b, a);
    }

    public static float curR = 1, curG = 1, curB = 1, curA = 1;

    @JsMethod
    public static void glColor4f(float r, float g, float b, float a) {
        curR = r; curG = g; curB = b; curA = a;
    }

    @JsMethod
    public static void glBegin(int mode) {}
    @JsMethod
    public static void glEnd() {}
    @JsMethod
    public static void glColor4ub(byte r, byte g, byte b, byte a) {}
    @JsMethod
    public static void glVertex2f(float x, float y) {}
    @JsMethod
    public static void glTexCoord2f(float s, float t) {}
    
    @JsMethod
    public static void glEnable(int cap) {
        if (gl != null) gl.enable(cap);
    }
    @JsMethod
    public static void glDisable(int cap) {
        if (gl != null) gl.disable(cap);
    }
    
    @JsMethod
    public static void glMatrixMode(int mode) {}
    @JsMethod
    public static void glLoadIdentity() {}
    @JsMethod
    public static void glTranslatef(float x, float y, float z) {}
    @JsMethod
    public static void glRotatef(float angle, float x, float y, float z) {}
    @JsMethod
    public static void glScalef(float x, float y, float z) {}
    @JsMethod
    public static void glPushMatrix() {}
    @JsMethod
    public static void glPopMatrix() {}
    @JsMethod
    public static void glPointSize(float size) {}
    @JsMethod
    public static void glLineWidth(float width) {}
    @JsMethod
    public static void glBlendFunc(int sfactor, int dfactor) {
        if (gl != null) gl.blendFunc(sfactor, dfactor);
    }
    @JsMethod
    public static void glHint(int target, int mode) {
        if (gl != null) gl.hint(target, mode);
    }
    @JsMethod
    public static void glPolygonMode(int face, int mode) {}
    @JsMethod
    public static int glGetInteger(int pname) { return 0; }
    @JsMethod
    public static String glGetString(int name) { return ""; }
    
    @JsMethod
    public static void glEnableClientState(int cap) {}
    @JsMethod
    public static void glDisableClientState(int cap) {}
    @JsMethod
    public static void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {}
    @JsMethod
    public static void glColorPointer(int size, boolean unsigned, int stride, ByteBuffer pointer) {}
    @JsMethod
    public static void glVertexPointer(int size, int stride, FloatBuffer pointer) {}
    @JsMethod
    public static void glDrawArrays(int mode, int first, int count) {}
    @JsMethod
    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) {
        if (gl != null) gl.colorMask(r, g, b, a);
    }
    @JsMethod
    public static void glScissor(int x, int y, int width, int height) {
        if (gl != null) gl.scissor(x, y, width, height);
    }

    @JsMethod
    public static void glViewport(int x, int y, int width, int height) {
        if (gl != null) gl.viewport(x, y, width, height);
    }
}
