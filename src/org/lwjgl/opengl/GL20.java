package org.lwjgl.opengl;
import java.nio.*;
public final class GL20 {
    public static final int GL_SHADING_LANGUAGE_VERSION = 0;
    public static final int GL_CURRENT_PROGRAM = 0;
    public static final int GL_SHADER_TYPE = 0;
    public static final int GL_DELETE_STATUS = 0;
    public static final int GL_COMPILE_STATUS = 0;
    public static final int GL_LINK_STATUS = 0;
    public static final int GL_VALIDATE_STATUS = 0;
    public static final int GL_INFO_LOG_LENGTH = 0;
    public static final int GL_ATTACHED_SHADERS = 0;
    public static final int GL_ACTIVE_UNIFORMS = 0;
    public static final int GL_ACTIVE_UNIFORM_MAX_LENGTH = 0;
    public static final int GL_ACTIVE_ATTRIBUTES = 0;
    public static final int GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 0;
    public static final int GL_SHADER_SOURCE_LENGTH = 0;
    public static final int GL_SHADER_OBJECT = 0;
    public static final int GL_FLOAT_VEC2 = 0;
    public static final int GL_FLOAT_VEC3 = 0;
    public static final int GL_FLOAT_VEC4 = 0;
    public static final int GL_INT_VEC2 = 0;
    public static final int GL_INT_VEC3 = 0;
    public static final int GL_INT_VEC4 = 0;
    public static final int GL_BOOL = 0;
    public static final int GL_BOOL_VEC2 = 0;
    public static final int GL_BOOL_VEC3 = 0;
    public static final int GL_BOOL_VEC4 = 0;
    public static final int GL_FLOAT_MAT2 = 0;
    public static final int GL_FLOAT_MAT3 = 0;
    public static final int GL_FLOAT_MAT4 = 0;
    public static final int GL_SAMPLER_1D = 0;
    public static final int GL_SAMPLER_2D = 0;
    public static final int GL_SAMPLER_3D = 0;
    public static final int GL_SAMPLER_CUBE = 0;
    public static final int GL_SAMPLER_1D_SHADOW = 0;
    public static final int GL_SAMPLER_2D_SHADOW = 0;
    public static final int GL_VERTEX_SHADER = 0;
    public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0;
    public static final int GL_MAX_VARYING_FLOATS = 0;
    public static final int GL_MAX_VERTEX_ATTRIBS = 0;
    public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0;
    public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0;
    public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0;
    public static final int GL_MAX_TEXTURE_COORDS = 0;
    public static final int GL_VERTEX_PROGRAM_POINT_SIZE = 0;
    public static final int GL_VERTEX_PROGRAM_TWO_SIDE = 0;
    public static final int GL_VERTEX_ATTRIB_ARRAY_ENABLED = 0;
    public static final int GL_VERTEX_ATTRIB_ARRAY_SIZE = 0;
    public static final int GL_VERTEX_ATTRIB_ARRAY_STRIDE = 0;
    public static final int GL_VERTEX_ATTRIB_ARRAY_TYPE = 0;
    public static final int GL_VERTEX_ATTRIB_ARRAY_NORMALIZED = 0;
    public static final int GL_CURRENT_VERTEX_ATTRIB = 0;
    public static final int GL_VERTEX_ATTRIB_ARRAY_POINTER = 0;
    public static final int GL_FRAGMENT_SHADER = 0;
    public static final int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0;
    public static final int GL_FRAGMENT_SHADER_DERIVATIVE_HINT = 0;
    public static final int GL_MAX_DRAW_BUFFERS = 0;
    public static final int GL_DRAW_BUFFER0 = 0;
    public static final int GL_DRAW_BUFFER1 = 0;
    public static final int GL_DRAW_BUFFER2 = 0;
    public static final int GL_DRAW_BUFFER3 = 0;
    public static final int GL_DRAW_BUFFER4 = 0;
    public static final int GL_DRAW_BUFFER5 = 0;
    public static final int GL_DRAW_BUFFER6 = 0;
    public static final int GL_DRAW_BUFFER7 = 0;
    public static final int GL_DRAW_BUFFER8 = 0;
    public static final int GL_DRAW_BUFFER9 = 0;
    public static final int GL_DRAW_BUFFER10 = 0;
    public static final int GL_DRAW_BUFFER11 = 0;
    public static final int GL_DRAW_BUFFER12 = 0;
    public static final int GL_DRAW_BUFFER13 = 0;
    public static final int GL_DRAW_BUFFER14 = 0;
    public static final int GL_DRAW_BUFFER15 = 0;
    public static final int GL_POINT_SPRITE = 0;
    public static final int GL_COORD_REPLACE = 0;
    public static final int GL_POINT_SPRITE_COORD_ORIGIN = 0;
    public static final int GL_LOWER_LEFT = 0;
    public static final int GL_UPPER_LEFT = 0;
    public static final int GL_STENCIL_BACK_FUNC = 0;
    public static final int GL_STENCIL_BACK_FAIL = 0;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_FAIL = 0;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_PASS = 0;
    public static final int GL_STENCIL_BACK_REF = 0;
    public static final int GL_STENCIL_BACK_VALUE_MASK = 0;
    public static final int GL_STENCIL_BACK_WRITEMASK = 0;
    public static final int GL_BLEND_EQUATION_RGB = 0;
    public static final int GL_BLEND_EQUATION_ALPHA = 0;
    public static void glShaderSource(int p0, java.nio.ByteBuffer p1) {}
    static void nglShaderSource(int p0, int p1, long p2, int p3, long p4) {}
    public static void glShaderSource(int p0, java.lang.CharSequence p1) {}
    public static void glShaderSource(int p0, java.lang.CharSequence[] p1) {}
    static void nglShaderSource3(int p0, int p1, long p2, long p3, long p4) {}
    public static int glCreateShader(int p0) { return 0; }
    static int nglCreateShader(int p0, long p1) { return 0; }
    public static boolean glIsShader(int p0) { return true; }
    static boolean nglIsShader(int p0, long p1) { return true; }
    public static void glCompileShader(int p0) {}
    static void nglCompileShader(int p0, long p1) {}
    public static void glDeleteShader(int p0) {}
    static void nglDeleteShader(int p0, long p1) {}
    public static int glCreateProgram() { return 0; }
    static int nglCreateProgram(long p0) { return 0; }
    public static boolean glIsProgram(int p0) { return true; }
    static boolean nglIsProgram(int p0, long p1) { return true; }
    public static void glAttachShader(int p0, int p1) {}
    static void nglAttachShader(int p0, int p1, long p2) {}
    public static void glDetachShader(int p0, int p1) {}
    static void nglDetachShader(int p0, int p1, long p2) {}
    public static void glLinkProgram(int p0) {}
    static void nglLinkProgram(int p0, long p1) {}
    public static void glUseProgram(int p0) {}
    static void nglUseProgram(int p0, long p1) {}
    public static void glValidateProgram(int p0) {}
    static void nglValidateProgram(int p0, long p1) {}
    public static void glDeleteProgram(int p0) {}
    static void nglDeleteProgram(int p0, long p1) {}
    public static void glUniform1f(int p0, float p1) {}
    static void nglUniform1f(int p0, float p1, long p2) {}
    public static void glUniform2f(int p0, float p1, float p2) {}
    static void nglUniform2f(int p0, float p1, float p2, long p3) {}
    public static void glUniform3f(int p0, float p1, float p2, float p3) {}
    static void nglUniform3f(int p0, float p1, float p2, float p3, long p4) {}
    public static void glUniform4f(int p0, float p1, float p2, float p3, float p4) {}
    static void nglUniform4f(int p0, float p1, float p2, float p3, float p4, long p5) {}
    public static void glUniform1i(int p0, int p1) {}
    static void nglUniform1i(int p0, int p1, long p2) {}
    public static void glUniform2i(int p0, int p1, int p2) {}
    static void nglUniform2i(int p0, int p1, int p2, long p3) {}
    public static void glUniform3i(int p0, int p1, int p2, int p3) {}
    static void nglUniform3i(int p0, int p1, int p2, int p3, long p4) {}
    public static void glUniform4i(int p0, int p1, int p2, int p3, int p4) {}
    static void nglUniform4i(int p0, int p1, int p2, int p3, int p4, long p5) {}
    public static void glUniform1(int p0, java.nio.FloatBuffer p1) {}
    static void nglUniform1fv(int p0, int p1, long p2, long p3) {}
    public static void glUniform2(int p0, java.nio.FloatBuffer p1) {}
    static void nglUniform2fv(int p0, int p1, long p2, long p3) {}
    public static void glUniform3(int p0, java.nio.FloatBuffer p1) {}
    static void nglUniform3fv(int p0, int p1, long p2, long p3) {}
    public static void glUniform4(int p0, java.nio.FloatBuffer p1) {}
    static void nglUniform4fv(int p0, int p1, long p2, long p3) {}
    public static void glUniform1(int p0, java.nio.IntBuffer p1) {}
    static void nglUniform1iv(int p0, int p1, long p2, long p3) {}
    public static void glUniform2(int p0, java.nio.IntBuffer p1) {}
    static void nglUniform2iv(int p0, int p1, long p2, long p3) {}
    public static void glUniform3(int p0, java.nio.IntBuffer p1) {}
    static void nglUniform3iv(int p0, int p1, long p2, long p3) {}
    public static void glUniform4(int p0, java.nio.IntBuffer p1) {}
    static void nglUniform4iv(int p0, int p1, long p2, long p3) {}
    public static void glUniformMatrix2(int p0, boolean p1, java.nio.FloatBuffer p2) {}
    static void nglUniformMatrix2fv(int p0, int p1, boolean p2, long p3, long p4) {}
    public static void glUniformMatrix3(int p0, boolean p1, java.nio.FloatBuffer p2) {}
    static void nglUniformMatrix3fv(int p0, int p1, boolean p2, long p3, long p4) {}
    public static void glUniformMatrix4(int p0, boolean p1, java.nio.FloatBuffer p2) {}
    static void nglUniformMatrix4fv(int p0, int p1, boolean p2, long p3, long p4) {}
    public static void glGetShader(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetShaderiv(int p0, int p1, long p2, long p3) {}
    public static int glGetShader(int p0, int p1) { return 0; }
    public static int glGetShaderi(int p0, int p1) { return 0; }
    public static void glGetProgram(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetProgramiv(int p0, int p1, long p2, long p3) {}
    public static int glGetProgram(int p0, int p1) { return 0; }
    public static int glGetProgrami(int p0, int p1) { return 0; }
    public static void glGetShaderInfoLog(int p0, java.nio.IntBuffer p1, java.nio.ByteBuffer p2) {}
    static void nglGetShaderInfoLog(int p0, int p1, long p2, long p3, long p4) {}
    public static java.lang.String glGetShaderInfoLog(int p0, int p1) { return ""; }
    public static void glGetProgramInfoLog(int p0, java.nio.IntBuffer p1, java.nio.ByteBuffer p2) {}
    static void nglGetProgramInfoLog(int p0, int p1, long p2, long p3, long p4) {}
    public static java.lang.String glGetProgramInfoLog(int p0, int p1) { return ""; }
    public static void glGetAttachedShaders(int p0, java.nio.IntBuffer p1, java.nio.IntBuffer p2) {}
    static void nglGetAttachedShaders(int p0, int p1, long p2, long p3, long p4) {}
    public static int glGetUniformLocation(int p0, java.nio.ByteBuffer p1) { return 0; }
    static int nglGetUniformLocation(int p0, long p1, long p2) { return 0; }
    public static int glGetUniformLocation(int p0, java.lang.CharSequence p1) { return 0; }
    public static void glGetActiveUniform(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.ByteBuffer p5) {}
    static void nglGetActiveUniform(int p0, int p1, int p2, long p3, long p4, long p5, long p6, long p7) {}
    public static java.lang.String glGetActiveUniform(int p0, int p1, int p2, java.nio.IntBuffer p3) { return ""; }
    public static java.lang.String glGetActiveUniform(int p0, int p1, int p2) { return ""; }
    public static int glGetActiveUniformSize(int p0, int p1) { return 0; }
    public static int glGetActiveUniformType(int p0, int p1) { return 0; }
    public static void glGetUniform(int p0, int p1, java.nio.FloatBuffer p2) {}
    static void nglGetUniformfv(int p0, int p1, long p2, long p3) {}
    public static void glGetUniform(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetUniformiv(int p0, int p1, long p2, long p3) {}
    public static void glGetShaderSource(int p0, java.nio.IntBuffer p1, java.nio.ByteBuffer p2) {}
    static void nglGetShaderSource(int p0, int p1, long p2, long p3, long p4) {}
    public static java.lang.String glGetShaderSource(int p0, int p1) { return ""; }
    public static void glVertexAttrib1s(int p0, short p1) {}
    static void nglVertexAttrib1s(int p0, short p1, long p2) {}
    public static void glVertexAttrib1f(int p0, float p1) {}
    static void nglVertexAttrib1f(int p0, float p1, long p2) {}
    public static void glVertexAttrib1d(int p0, double p1) {}
    static void nglVertexAttrib1d(int p0, double p1, long p2) {}
    public static void glVertexAttrib2s(int p0, short p1, short p2) {}
    static void nglVertexAttrib2s(int p0, short p1, short p2, long p3) {}
    public static void glVertexAttrib2f(int p0, float p1, float p2) {}
    static void nglVertexAttrib2f(int p0, float p1, float p2, long p3) {}
    public static void glVertexAttrib2d(int p0, double p1, double p2) {}
    static void nglVertexAttrib2d(int p0, double p1, double p2, long p3) {}
    public static void glVertexAttrib3s(int p0, short p1, short p2, short p3) {}
    static void nglVertexAttrib3s(int p0, short p1, short p2, short p3, long p4) {}
    public static void glVertexAttrib3f(int p0, float p1, float p2, float p3) {}
    static void nglVertexAttrib3f(int p0, float p1, float p2, float p3, long p4) {}
    public static void glVertexAttrib3d(int p0, double p1, double p2, double p3) {}
    static void nglVertexAttrib3d(int p0, double p1, double p2, double p3, long p4) {}
    public static void glVertexAttrib4s(int p0, short p1, short p2, short p3, short p4) {}
    static void nglVertexAttrib4s(int p0, short p1, short p2, short p3, short p4, long p5) {}
    public static void glVertexAttrib4f(int p0, float p1, float p2, float p3, float p4) {}
    static void nglVertexAttrib4f(int p0, float p1, float p2, float p3, float p4, long p5) {}
    public static void glVertexAttrib4d(int p0, double p1, double p2, double p3, double p4) {}
    static void nglVertexAttrib4d(int p0, double p1, double p2, double p3, double p4, long p5) {}
    public static void glVertexAttrib4Nub(int p0, byte p1, byte p2, byte p3, byte p4) {}
    static void nglVertexAttrib4Nub(int p0, byte p1, byte p2, byte p3, byte p4, long p5) {}
    public static void glVertexAttribPointer(int p0, int p1, boolean p2, int p3, java.nio.DoubleBuffer p4) {}
    public static void glVertexAttribPointer(int p0, int p1, boolean p2, int p3, java.nio.FloatBuffer p4) {}
    public static void glVertexAttribPointer(int p0, int p1, boolean p2, boolean p3, int p4, java.nio.ByteBuffer p5) {}
    public static void glVertexAttribPointer(int p0, int p1, boolean p2, boolean p3, int p4, java.nio.IntBuffer p5) {}
    public static void glVertexAttribPointer(int p0, int p1, boolean p2, boolean p3, int p4, java.nio.ShortBuffer p5) {}
    static void nglVertexAttribPointer(int p0, int p1, int p2, boolean p3, int p4, long p5, long p6) {}
    public static void glVertexAttribPointer(int p0, int p1, int p2, boolean p3, int p4, long p5) {}
    static void nglVertexAttribPointerBO(int p0, int p1, int p2, boolean p3, int p4, long p5, long p6) {}
    public static void glVertexAttribPointer(int p0, int p1, int p2, boolean p3, int p4, java.nio.ByteBuffer p5) {}
    public static void glEnableVertexAttribArray(int p0) {}
    static void nglEnableVertexAttribArray(int p0, long p1) {}
    public static void glDisableVertexAttribArray(int p0) {}
    static void nglDisableVertexAttribArray(int p0, long p1) {}
    public static void glGetVertexAttrib(int p0, int p1, java.nio.FloatBuffer p2) {}
    static void nglGetVertexAttribfv(int p0, int p1, long p2, long p3) {}
    public static void glGetVertexAttrib(int p0, int p1, java.nio.DoubleBuffer p2) {}
    static void nglGetVertexAttribdv(int p0, int p1, long p2, long p3) {}
    public static void glGetVertexAttrib(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetVertexAttribiv(int p0, int p1, long p2, long p3) {}
    public static java.nio.ByteBuffer glGetVertexAttribPointer(int p0, int p1, long p2) { return null; }
    static java.nio.ByteBuffer nglGetVertexAttribPointerv(int p0, int p1, long p2, long p3) { return null; }
    public static void glGetVertexAttribPointer(int p0, int p1, java.nio.ByteBuffer p2) {}
    static void nglGetVertexAttribPointerv2(int p0, int p1, long p2, long p3) {}
    public static void glBindAttribLocation(int p0, int p1, java.nio.ByteBuffer p2) {}
    static void nglBindAttribLocation(int p0, int p1, long p2, long p3) {}
    public static void glBindAttribLocation(int p0, int p1, java.lang.CharSequence p2) {}
    public static void glGetActiveAttrib(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.ByteBuffer p5) {}
    static void nglGetActiveAttrib(int p0, int p1, int p2, long p3, long p4, long p5, long p6, long p7) {}
    public static java.lang.String glGetActiveAttrib(int p0, int p1, int p2, java.nio.IntBuffer p3) { return ""; }
    public static java.lang.String glGetActiveAttrib(int p0, int p1, int p2) { return ""; }
    public static int glGetActiveAttribSize(int p0, int p1) { return 0; }
    public static int glGetActiveAttribType(int p0, int p1) { return 0; }
    public static int glGetAttribLocation(int p0, java.nio.ByteBuffer p1) { return 0; }
    static int nglGetAttribLocation(int p0, long p1, long p2) { return 0; }
    public static int glGetAttribLocation(int p0, java.lang.CharSequence p1) { return 0; }
    public static void glDrawBuffers(java.nio.IntBuffer p0) {}
    static void nglDrawBuffers(int p0, long p1, long p2) {}
    public static void glDrawBuffers(int p0) {}
    public static void glStencilOpSeparate(int p0, int p1, int p2, int p3) {}
    static void nglStencilOpSeparate(int p0, int p1, int p2, int p3, long p4) {}
    public static void glStencilFuncSeparate(int p0, int p1, int p2, int p3) {}
    static void nglStencilFuncSeparate(int p0, int p1, int p2, int p3, long p4) {}
    public static void glStencilMaskSeparate(int p0, int p1) {}
    static void nglStencilMaskSeparate(int p0, int p1, long p2) {}
    public static void glBlendEquationSeparate(int p0, int p1) {}
    static void nglBlendEquationSeparate(int p0, int p1, long p2) {}
}