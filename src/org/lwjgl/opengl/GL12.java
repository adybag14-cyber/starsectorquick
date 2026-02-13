package org.lwjgl.opengl;
import java.nio.*;
public final class GL12 {
    public static final int GL_TEXTURE_BINDING_3D = 0;
    public static final int GL_PACK_SKIP_IMAGES = 0;
    public static final int GL_PACK_IMAGE_HEIGHT = 0;
    public static final int GL_UNPACK_SKIP_IMAGES = 0;
    public static final int GL_UNPACK_IMAGE_HEIGHT = 0;
    public static final int GL_TEXTURE_3D = 0;
    public static final int GL_PROXY_TEXTURE_3D = 0;
    public static final int GL_TEXTURE_DEPTH = 0;
    public static final int GL_TEXTURE_WRAP_R = 0;
    public static final int GL_MAX_3D_TEXTURE_SIZE = 0;
    public static final int GL_BGR = 0;
    public static final int GL_BGRA = 0;
    public static final int GL_UNSIGNED_BYTE_3_3_2 = 0;
    public static final int GL_UNSIGNED_BYTE_2_3_3_REV = 0;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0;
    public static final int GL_UNSIGNED_SHORT_5_6_5_REV = 0;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4_REV = 0;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0;
    public static final int GL_UNSIGNED_SHORT_1_5_5_5_REV = 0;
    public static final int GL_UNSIGNED_INT_8_8_8_8 = 0;
    public static final int GL_UNSIGNED_INT_8_8_8_8_REV = 0;
    public static final int GL_UNSIGNED_INT_10_10_10_2 = 0;
    public static final int GL_UNSIGNED_INT_2_10_10_10_REV = 0;
    public static final int GL_RESCALE_NORMAL = 0;
    public static final int GL_LIGHT_MODEL_COLOR_CONTROL = 0;
    public static final int GL_SINGLE_COLOR = 0;
    public static final int GL_SEPARATE_SPECULAR_COLOR = 0;
    public static final int GL_CLAMP_TO_EDGE = 0;
    public static final int GL_TEXTURE_MIN_LOD = 0;
    public static final int GL_TEXTURE_MAX_LOD = 0;
    public static final int GL_TEXTURE_BASE_LEVEL = 0;
    public static final int GL_TEXTURE_MAX_LEVEL = 0;
    public static final int GL_MAX_ELEMENTS_VERTICES = 0;
    public static final int GL_MAX_ELEMENTS_INDICES = 0;
    public static final int GL_ALIASED_POINT_SIZE_RANGE = 0;
    public static final int GL_ALIASED_LINE_WIDTH_RANGE = 0;
    public static final int GL_SMOOTH_POINT_SIZE_RANGE = 0;
    public static final int GL_SMOOTH_POINT_SIZE_GRANULARITY = 0;
    public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 0;
    public static final int GL_SMOOTH_LINE_WIDTH_GRANULARITY = 0;
    public static void glDrawRangeElements(int p0, int p1, int p2, java.nio.ByteBuffer p3) {}
    public static void glDrawRangeElements(int p0, int p1, int p2, java.nio.IntBuffer p3) {}
    public static void glDrawRangeElements(int p0, int p1, int p2, java.nio.ShortBuffer p3) {}
    static void nglDrawRangeElements(int p0, int p1, int p2, int p3, int p4, long p5, long p6) {}
    public static void glDrawRangeElements(int p0, int p1, int p2, int p3, int p4, long p5) {}
    static void nglDrawRangeElementsBO(int p0, int p1, int p2, int p3, int p4, long p5, long p6) {}
    public static void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.ByteBuffer p9) {}
    public static void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.DoubleBuffer p9) {}
    public static void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.FloatBuffer p9) {}
    public static void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.IntBuffer p9) {}
    public static void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.ShortBuffer p9) {}
    static void nglTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9, long p10) {}
    public static void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9) {}
    static void nglTexImage3DBO(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9, long p10) {}
    public static void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.ByteBuffer p10) {}
    public static void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.DoubleBuffer p10) {}
    public static void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.FloatBuffer p10) {}
    public static void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.IntBuffer p10) {}
    public static void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.ShortBuffer p10) {}
    static void nglTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10, long p11) {}
    public static void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10) {}
    static void nglTexSubImage3DBO(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10, long p11) {}
    public static void glCopyTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {}
    static void nglCopyTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9) {}
}