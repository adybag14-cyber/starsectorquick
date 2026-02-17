package org.lwjgl.opengl;
import java.nio.*;
public final class GL15 {
    public static final int GL_ARRAY_BUFFER = 0;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0;
    public static final int GL_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_VERTEX_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_NORMAL_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_COLOR_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_INDEX_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_TEXTURE_COORD_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_EDGE_FLAG_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_SECONDARY_COLOR_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_FOG_COORDINATE_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_WEIGHT_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_STREAM_DRAW = 0;
    public static final int GL_STREAM_READ = 0;
    public static final int GL_STREAM_COPY = 0;
    public static final int GL_STATIC_DRAW = 0;
    public static final int GL_STATIC_READ = 0;
    public static final int GL_STATIC_COPY = 0;
    public static final int GL_DYNAMIC_DRAW = 0;
    public static final int GL_DYNAMIC_READ = 0;
    public static final int GL_DYNAMIC_COPY = 0;
    public static final int GL_READ_ONLY = 0;
    public static final int GL_WRITE_ONLY = 0;
    public static final int GL_READ_WRITE = 0;
    public static final int GL_BUFFER_SIZE = 0;
    public static final int GL_BUFFER_USAGE = 0;
    public static final int GL_BUFFER_ACCESS = 0;
    public static final int GL_BUFFER_MAPPED = 0;
    public static final int GL_BUFFER_MAP_POINTER = 0;
    public static final int GL_FOG_COORD_SRC = 0;
    public static final int GL_FOG_COORD = 0;
    public static final int GL_CURRENT_FOG_COORD = 0;
    public static final int GL_FOG_COORD_ARRAY_TYPE = 0;
    public static final int GL_FOG_COORD_ARRAY_STRIDE = 0;
    public static final int GL_FOG_COORD_ARRAY_POINTER = 0;
    public static final int GL_FOG_COORD_ARRAY = 0;
    public static final int GL_FOG_COORD_ARRAY_BUFFER_BINDING = 0;
    public static final int GL_SRC0_RGB = 0;
    public static final int GL_SRC1_RGB = 0;
    public static final int GL_SRC2_RGB = 0;
    public static final int GL_SRC0_ALPHA = 0;
    public static final int GL_SRC1_ALPHA = 0;
    public static final int GL_SRC2_ALPHA = 0;
    public static final int GL_SAMPLES_PASSED = 0;
    public static final int GL_QUERY_COUNTER_BITS = 0;
    public static final int GL_CURRENT_QUERY = 0;
    public static final int GL_QUERY_RESULT = 0;
    public static final int GL_QUERY_RESULT_AVAILABLE = 0;
    public static void glBindBuffer(int p0, int p1) {}
    static void nglBindBuffer(int p0, int p1, long p2) {}
    public static void glDeleteBuffers(java.nio.IntBuffer p0) {}
    static void nglDeleteBuffers(int p0, long p1, long p2) {}
    public static void glDeleteBuffers(int p0) {}
    public static void glGenBuffers(java.nio.IntBuffer p0) {}
    static void nglGenBuffers(int p0, long p1, long p2) {}
    public static int glGenBuffers() { return 0; }
    public static boolean glIsBuffer(int p0) { return true; }
    static boolean nglIsBuffer(int p0, long p1) { return true; }
    public static void glBufferData(int p0, long p1, int p2) {}
    public static void glBufferData(int p0, java.nio.ByteBuffer p1, int p2) {}
    public static void glBufferData(int p0, java.nio.DoubleBuffer p1, int p2) {}
    public static void glBufferData(int p0, java.nio.FloatBuffer p1, int p2) {}
    public static void glBufferData(int p0, java.nio.IntBuffer p1, int p2) {}
    public static void glBufferData(int p0, java.nio.ShortBuffer p1, int p2) {}
    static void nglBufferData(int p0, long p1, long p2, int p3, long p4) {}
    public static void glBufferSubData(int p0, long p1, java.nio.ByteBuffer p2) {}
    public static void glBufferSubData(int p0, long p1, java.nio.DoubleBuffer p2) {}
    public static void glBufferSubData(int p0, long p1, java.nio.FloatBuffer p2) {}
    public static void glBufferSubData(int p0, long p1, java.nio.IntBuffer p2) {}
    public static void glBufferSubData(int p0, long p1, java.nio.ShortBuffer p2) {}
    static void nglBufferSubData(int p0, long p1, long p2, long p3, long p4) {}
    public static void glGetBufferSubData(int p0, long p1, java.nio.ByteBuffer p2) {}
    public static void glGetBufferSubData(int p0, long p1, java.nio.DoubleBuffer p2) {}
    public static void glGetBufferSubData(int p0, long p1, java.nio.FloatBuffer p2) {}
    public static void glGetBufferSubData(int p0, long p1, java.nio.IntBuffer p2) {}
    public static void glGetBufferSubData(int p0, long p1, java.nio.ShortBuffer p2) {}
    static void nglGetBufferSubData(int p0, long p1, long p2, long p3, long p4) {}
    public static java.nio.ByteBuffer glMapBuffer(int p0, int p1, java.nio.ByteBuffer p2) { return null; }
    public static java.nio.ByteBuffer glMapBuffer(int p0, int p1, long p2, java.nio.ByteBuffer p3) { return null; }
    static java.nio.ByteBuffer nglMapBuffer(int p0, int p1, long p2, java.nio.ByteBuffer p3, long p4) { return null; }
    public static boolean glUnmapBuffer(int p0) { return true; }
    static boolean nglUnmapBuffer(int p0, long p1) { return true; }
    public static void glGetBufferParameter(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetBufferParameteriv(int p0, int p1, long p2, long p3) {}
    public static int glGetBufferParameter(int p0, int p1) { return 0; }
    public static int glGetBufferParameteri(int p0, int p1) { return 0; }
    public static java.nio.ByteBuffer glGetBufferPointer(int p0, int p1) { return null; }
    static java.nio.ByteBuffer nglGetBufferPointerv(int p0, int p1, long p2, long p3) { return null; }
    public static void glGenQueries(java.nio.IntBuffer p0) {}
    static void nglGenQueries(int p0, long p1, long p2) {}
    public static int glGenQueries() { return 0; }
    public static void glDeleteQueries(java.nio.IntBuffer p0) {}
    static void nglDeleteQueries(int p0, long p1, long p2) {}
    public static void glDeleteQueries(int p0) {}
    public static boolean glIsQuery(int p0) { return true; }
    static boolean nglIsQuery(int p0, long p1) { return true; }
    public static void glBeginQuery(int p0, int p1) {}
    static void nglBeginQuery(int p0, int p1, long p2) {}
    public static void glEndQuery(int p0) {}
    static void nglEndQuery(int p0, long p1) {}
    public static void glGetQuery(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetQueryiv(int p0, int p1, long p2, long p3) {}
    public static int glGetQuery(int p0, int p1) { return 0; }
    public static int glGetQueryi(int p0, int p1) { return 0; }
    public static void glGetQueryObject(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetQueryObjectiv(int p0, int p1, long p2, long p3) {}
    public static int glGetQueryObjecti(int p0, int p1) { return 0; }
    public static void glGetQueryObjectu(int p0, int p1, java.nio.IntBuffer p2) {}
    static void nglGetQueryObjectuiv(int p0, int p1, long p2, long p3) {}
    public static int glGetQueryObjectui(int p0, int p1) { return 0; }
}