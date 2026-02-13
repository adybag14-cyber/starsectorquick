package org.lwjgl.opengl;
import java.nio.*;
public final class GL14 {
    public static final int GL_GENERATE_MIPMAP = 0;
    public static final int GL_GENERATE_MIPMAP_HINT = 0;
    public static final int GL_DEPTH_COMPONENT16 = 0;
    public static final int GL_DEPTH_COMPONENT24 = 0;
    public static final int GL_DEPTH_COMPONENT32 = 0;
    public static final int GL_TEXTURE_DEPTH_SIZE = 0;
    public static final int GL_DEPTH_TEXTURE_MODE = 0;
    public static final int GL_TEXTURE_COMPARE_MODE = 0;
    public static final int GL_TEXTURE_COMPARE_FUNC = 0;
    public static final int GL_COMPARE_R_TO_TEXTURE = 0;
    public static final int GL_FOG_COORDINATE_SOURCE = 0;
    public static final int GL_FOG_COORDINATE = 0;
    public static final int GL_FRAGMENT_DEPTH = 0;
    public static final int GL_CURRENT_FOG_COORDINATE = 0;
    public static final int GL_FOG_COORDINATE_ARRAY_TYPE = 0;
    public static final int GL_FOG_COORDINATE_ARRAY_STRIDE = 0;
    public static final int GL_FOG_COORDINATE_ARRAY_POINTER = 0;
    public static final int GL_FOG_COORDINATE_ARRAY = 0;
    public static final int GL_POINT_SIZE_MIN = 0;
    public static final int GL_POINT_SIZE_MAX = 0;
    public static final int GL_POINT_FADE_THRESHOLD_SIZE = 0;
    public static final int GL_POINT_DISTANCE_ATTENUATION = 0;
    public static final int GL_COLOR_SUM = 0;
    public static final int GL_CURRENT_SECONDARY_COLOR = 0;
    public static final int GL_SECONDARY_COLOR_ARRAY_SIZE = 0;
    public static final int GL_SECONDARY_COLOR_ARRAY_TYPE = 0;
    public static final int GL_SECONDARY_COLOR_ARRAY_STRIDE = 0;
    public static final int GL_SECONDARY_COLOR_ARRAY_POINTER = 0;
    public static final int GL_SECONDARY_COLOR_ARRAY = 0;
    public static final int GL_BLEND_DST_RGB = 0;
    public static final int GL_BLEND_SRC_RGB = 0;
    public static final int GL_BLEND_DST_ALPHA = 0;
    public static final int GL_BLEND_SRC_ALPHA = 0;
    public static final int GL_INCR_WRAP = 0;
    public static final int GL_DECR_WRAP = 0;
    public static final int GL_TEXTURE_FILTER_CONTROL = 0;
    public static final int GL_TEXTURE_LOD_BIAS = 0;
    public static final int GL_MAX_TEXTURE_LOD_BIAS = 0;
    public static final int GL_MIRRORED_REPEAT = 0;
    public static final int GL_BLEND_COLOR = 0;
    public static final int GL_BLEND_EQUATION = 0;
    public static final int GL_FUNC_ADD = 0;
    public static final int GL_FUNC_SUBTRACT = 0;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 0;
    public static final int GL_MIN = 0;
    public static final int GL_MAX = 0;
    public static void glBlendEquation(int p0) {}
    static void nglBlendEquation(int p0, long p1) {}
    public static void glBlendColor(float p0, float p1, float p2, float p3) {}
    static void nglBlendColor(float p0, float p1, float p2, float p3, long p4) {}
    public static void glFogCoordf(float p0) {}
    static void nglFogCoordf(float p0, long p1) {}
    public static void glFogCoordd(double p0) {}
    static void nglFogCoordd(double p0, long p1) {}
    public static void glFogCoordPointer(int p0, java.nio.DoubleBuffer p1) {}
    public static void glFogCoordPointer(int p0, java.nio.FloatBuffer p1) {}
    static void nglFogCoordPointer(int p0, int p1, long p2, long p3) {}
    public static void glFogCoordPointer(int p0, int p1, long p2) {}
    static void nglFogCoordPointerBO(int p0, int p1, long p2, long p3) {}
    public static void glMultiDrawArrays(int p0, java.nio.IntBuffer p1, java.nio.IntBuffer p2) {}
    static void nglMultiDrawArrays(int p0, long p1, long p2, int p3, long p4) {}
    public static void glPointParameteri(int p0, int p1) {}
    static void nglPointParameteri(int p0, int p1, long p2) {}
    public static void glPointParameterf(int p0, float p1) {}
    static void nglPointParameterf(int p0, float p1, long p2) {}
    public static void glPointParameter(int p0, java.nio.IntBuffer p1) {}
    static void nglPointParameteriv(int p0, long p1, long p2) {}
    public static void glPointParameter(int p0, java.nio.FloatBuffer p1) {}
    static void nglPointParameterfv(int p0, long p1, long p2) {}
    public static void glSecondaryColor3b(byte p0, byte p1, byte p2) {}
    static void nglSecondaryColor3b(byte p0, byte p1, byte p2, long p3) {}
    public static void glSecondaryColor3f(float p0, float p1, float p2) {}
    static void nglSecondaryColor3f(float p0, float p1, float p2, long p3) {}
    public static void glSecondaryColor3d(double p0, double p1, double p2) {}
    static void nglSecondaryColor3d(double p0, double p1, double p2, long p3) {}
    public static void glSecondaryColor3ub(byte p0, byte p1, byte p2) {}
    static void nglSecondaryColor3ub(byte p0, byte p1, byte p2, long p3) {}
    public static void glSecondaryColorPointer(int p0, int p1, java.nio.DoubleBuffer p2) {}
    public static void glSecondaryColorPointer(int p0, int p1, java.nio.FloatBuffer p2) {}
    public static void glSecondaryColorPointer(int p0, boolean p1, int p2, java.nio.ByteBuffer p3) {}
    static void nglSecondaryColorPointer(int p0, int p1, int p2, long p3, long p4) {}
    public static void glSecondaryColorPointer(int p0, int p1, int p2, long p3) {}
    static void nglSecondaryColorPointerBO(int p0, int p1, int p2, long p3, long p4) {}
    public static void glBlendFuncSeparate(int p0, int p1, int p2, int p3) {}
    static void nglBlendFuncSeparate(int p0, int p1, int p2, int p3, long p4) {}
    public static void glWindowPos2f(float p0, float p1) {}
    static void nglWindowPos2f(float p0, float p1, long p2) {}
    public static void glWindowPos2d(double p0, double p1) {}
    static void nglWindowPos2d(double p0, double p1, long p2) {}
    public static void glWindowPos2i(int p0, int p1) {}
    static void nglWindowPos2i(int p0, int p1, long p2) {}
    public static void glWindowPos3f(float p0, float p1, float p2) {}
    static void nglWindowPos3f(float p0, float p1, float p2, long p3) {}
    public static void glWindowPos3d(double p0, double p1, double p2) {}
    static void nglWindowPos3d(double p0, double p1, double p2, long p3) {}
    public static void glWindowPos3i(int p0, int p1, int p2) {}
    static void nglWindowPos3i(int p0, int p1, int p2, long p3) {}
}