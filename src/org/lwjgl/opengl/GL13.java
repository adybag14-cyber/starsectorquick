package org.lwjgl.opengl;
import java.nio.*;
public final class GL13 {
    public static final int GL_TEXTURE0 = 0;
    public static final int GL_TEXTURE1 = 0;
    public static final int GL_TEXTURE2 = 0;
    public static final int GL_TEXTURE3 = 0;
    public static final int GL_TEXTURE4 = 0;
    public static final int GL_TEXTURE5 = 0;
    public static final int GL_TEXTURE6 = 0;
    public static final int GL_TEXTURE7 = 0;
    public static final int GL_TEXTURE8 = 0;
    public static final int GL_TEXTURE9 = 0;
    public static final int GL_TEXTURE10 = 0;
    public static final int GL_TEXTURE11 = 0;
    public static final int GL_TEXTURE12 = 0;
    public static final int GL_TEXTURE13 = 0;
    public static final int GL_TEXTURE14 = 0;
    public static final int GL_TEXTURE15 = 0;
    public static final int GL_TEXTURE16 = 0;
    public static final int GL_TEXTURE17 = 0;
    public static final int GL_TEXTURE18 = 0;
    public static final int GL_TEXTURE19 = 0;
    public static final int GL_TEXTURE20 = 0;
    public static final int GL_TEXTURE21 = 0;
    public static final int GL_TEXTURE22 = 0;
    public static final int GL_TEXTURE23 = 0;
    public static final int GL_TEXTURE24 = 0;
    public static final int GL_TEXTURE25 = 0;
    public static final int GL_TEXTURE26 = 0;
    public static final int GL_TEXTURE27 = 0;
    public static final int GL_TEXTURE28 = 0;
    public static final int GL_TEXTURE29 = 0;
    public static final int GL_TEXTURE30 = 0;
    public static final int GL_TEXTURE31 = 0;
    public static final int GL_ACTIVE_TEXTURE = 0;
    public static final int GL_CLIENT_ACTIVE_TEXTURE = 0;
    public static final int GL_MAX_TEXTURE_UNITS = 0;
    public static final int GL_NORMAL_MAP = 0;
    public static final int GL_REFLECTION_MAP = 0;
    public static final int GL_TEXTURE_CUBE_MAP = 0;
    public static final int GL_TEXTURE_BINDING_CUBE_MAP = 0;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0;
    public static final int GL_PROXY_TEXTURE_CUBE_MAP = 0;
    public static final int GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0;
    public static final int GL_COMPRESSED_ALPHA = 0;
    public static final int GL_COMPRESSED_LUMINANCE = 0;
    public static final int GL_COMPRESSED_LUMINANCE_ALPHA = 0;
    public static final int GL_COMPRESSED_INTENSITY = 0;
    public static final int GL_COMPRESSED_RGB = 0;
    public static final int GL_COMPRESSED_RGBA = 0;
    public static final int GL_TEXTURE_COMPRESSION_HINT = 0;
    public static final int GL_TEXTURE_COMPRESSED_IMAGE_SIZE = 0;
    public static final int GL_TEXTURE_COMPRESSED = 0;
    public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0;
    public static final int GL_COMPRESSED_TEXTURE_FORMATS = 0;
    public static final int GL_MULTISAMPLE = 0;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0;
    public static final int GL_SAMPLE_ALPHA_TO_ONE = 0;
    public static final int GL_SAMPLE_COVERAGE = 0;
    public static final int GL_SAMPLE_BUFFERS = 0;
    public static final int GL_SAMPLES = 0;
    public static final int GL_SAMPLE_COVERAGE_VALUE = 0;
    public static final int GL_SAMPLE_COVERAGE_INVERT = 0;
    public static final int GL_MULTISAMPLE_BIT = 0;
    public static final int GL_TRANSPOSE_MODELVIEW_MATRIX = 0;
    public static final int GL_TRANSPOSE_PROJECTION_MATRIX = 0;
    public static final int GL_TRANSPOSE_TEXTURE_MATRIX = 0;
    public static final int GL_TRANSPOSE_COLOR_MATRIX = 0;
    public static final int GL_COMBINE = 0;
    public static final int GL_COMBINE_RGB = 0;
    public static final int GL_COMBINE_ALPHA = 0;
    public static final int GL_SOURCE0_RGB = 0;
    public static final int GL_SOURCE1_RGB = 0;
    public static final int GL_SOURCE2_RGB = 0;
    public static final int GL_SOURCE0_ALPHA = 0;
    public static final int GL_SOURCE1_ALPHA = 0;
    public static final int GL_SOURCE2_ALPHA = 0;
    public static final int GL_OPERAND0_RGB = 0;
    public static final int GL_OPERAND1_RGB = 0;
    public static final int GL_OPERAND2_RGB = 0;
    public static final int GL_OPERAND0_ALPHA = 0;
    public static final int GL_OPERAND1_ALPHA = 0;
    public static final int GL_OPERAND2_ALPHA = 0;
    public static final int GL_RGB_SCALE = 0;
    public static final int GL_ADD_SIGNED = 0;
    public static final int GL_INTERPOLATE = 0;
    public static final int GL_SUBTRACT = 0;
    public static final int GL_CONSTANT = 0;
    public static final int GL_PRIMARY_COLOR = 0;
    public static final int GL_PREVIOUS = 0;
    public static final int GL_DOT3_RGB = 0;
    public static final int GL_DOT3_RGBA = 0;
    public static final int GL_CLAMP_TO_BORDER = 0;
    public static void glActiveTexture(int p0) {}
    static void nglActiveTexture(int p0, long p1) {}
    public static void glClientActiveTexture(int p0) {}
    static void nglClientActiveTexture(int p0, long p1) {}
    public static void glCompressedTexImage1D(int p0, int p1, int p2, int p3, int p4, java.nio.ByteBuffer p5) {}
    static void nglCompressedTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6, long p7) {}
    public static void glCompressedTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {}
    static void nglCompressedTexImage1DBO(int p0, int p1, int p2, int p3, int p4, int p5, long p6, long p7) {}
    public static void glCompressedTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5) {}
    public static void glCompressedTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.ByteBuffer p6) {}
    static void nglCompressedTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7, long p8) {}
    public static void glCompressedTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7) {}
    static void nglCompressedTexImage2DBO(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7, long p8) {}
    public static void glCompressedTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {}
    public static void glCompressedTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.ByteBuffer p7) {}
    static void nglCompressedTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8, long p9) {}
    public static void glCompressedTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {}
    static void nglCompressedTexImage3DBO(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8, long p9) {}
    public static void glCompressedTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {}
    public static void glCompressedTexSubImage1D(int p0, int p1, int p2, int p3, int p4, java.nio.ByteBuffer p5) {}
    static void nglCompressedTexSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6, long p7) {}
    public static void glCompressedTexSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {}
    static void nglCompressedTexSubImage1DBO(int p0, int p1, int p2, int p3, int p4, int p5, long p6, long p7) {}
    public static void glCompressedTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.ByteBuffer p7) {}
    static void nglCompressedTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8, long p9) {}
    public static void glCompressedTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {}
    static void nglCompressedTexSubImage2DBO(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8, long p9) {}
    public static void glCompressedTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.ByteBuffer p9) {}
    static void nglCompressedTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10, long p11) {}
    public static void glCompressedTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10) {}
    static void nglCompressedTexSubImage3DBO(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10, long p11) {}
    public static void glGetCompressedTexImage(int p0, int p1, java.nio.ByteBuffer p2) {}
    public static void glGetCompressedTexImage(int p0, int p1, java.nio.IntBuffer p2) {}
    public static void glGetCompressedTexImage(int p0, int p1, java.nio.ShortBuffer p2) {}
    static void nglGetCompressedTexImage(int p0, int p1, long p2, long p3) {}
    public static void glGetCompressedTexImage(int p0, int p1, long p2) {}
    static void nglGetCompressedTexImageBO(int p0, int p1, long p2, long p3) {}
    public static void glMultiTexCoord1f(int p0, float p1) {}
    static void nglMultiTexCoord1f(int p0, float p1, long p2) {}
    public static void glMultiTexCoord1d(int p0, double p1) {}
    static void nglMultiTexCoord1d(int p0, double p1, long p2) {}
    public static void glMultiTexCoord2f(int p0, float p1, float p2) {}
    static void nglMultiTexCoord2f(int p0, float p1, float p2, long p3) {}
    public static void glMultiTexCoord2d(int p0, double p1, double p2) {}
    static void nglMultiTexCoord2d(int p0, double p1, double p2, long p3) {}
    public static void glMultiTexCoord3f(int p0, float p1, float p2, float p3) {}
    static void nglMultiTexCoord3f(int p0, float p1, float p2, float p3, long p4) {}
    public static void glMultiTexCoord3d(int p0, double p1, double p2, double p3) {}
    static void nglMultiTexCoord3d(int p0, double p1, double p2, double p3, long p4) {}
    public static void glMultiTexCoord4f(int p0, float p1, float p2, float p3, float p4) {}
    static void nglMultiTexCoord4f(int p0, float p1, float p2, float p3, float p4, long p5) {}
    public static void glMultiTexCoord4d(int p0, double p1, double p2, double p3, double p4) {}
    static void nglMultiTexCoord4d(int p0, double p1, double p2, double p3, double p4, long p5) {}
    public static void glLoadTransposeMatrix(java.nio.FloatBuffer p0) {}
    static void nglLoadTransposeMatrixf(long p0, long p1) {}
    public static void glLoadTransposeMatrix(java.nio.DoubleBuffer p0) {}
    static void nglLoadTransposeMatrixd(long p0, long p1) {}
    public static void glMultTransposeMatrix(java.nio.FloatBuffer p0) {}
    static void nglMultTransposeMatrixf(long p0, long p1) {}
    public static void glMultTransposeMatrix(java.nio.DoubleBuffer p0) {}
    static void nglMultTransposeMatrixd(long p0, long p1) {}
    public static void glSampleCoverage(float p0, boolean p1) {}
    static void nglSampleCoverage(float p0, boolean p1, long p2) {}
}