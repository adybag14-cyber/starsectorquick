package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class GL1A {
    private GL1A() {}

    public static String glGetString(int p0) { return GL11.glGetString(p0); }
    public static int glGetError() { return GL11.glGetError(); }
    public static int glGetInteger(int p0) { return GL11.glGetInteger(p0); }
    public static void glGetInteger(int p0, IntBuffer p1) { GL11.glGetInteger(p0, p1); }

    public static void glAlphaFunc(int p0, float p1) { GL11.glAlphaFunc(p0, p1); }
    public static void glArrayElement(int p0) { GL11.glArrayElement(p0); }
    public static void glBegin(int p0) { GL11.glBegin(p0); }
    public static void glBindTexture(int p0, int p1) { GL11.glBindTexture(p0, p1); }
    public static void glBlendFunc(int p0, int p1) { GL11.glBlendFunc(p0, p1); }
    public static void glCallList(int p0) { GL11.glCallList(p0); }
    public static void glClear(int p0) { GL11.glClear(p0); }
    public static void glClearColor(float p0, float p1, float p2, float p3) { GL11.glClearColor(p0, p1, p2, p3); }
    public static void glColor3d(double p0, double p1, double p2) { GL11.glColor3d(p0, p1, p2); }
    public static void glColor3f(float p0, float p1, float p2) { GL11.glColor3f(p0, p1, p2); }
    public static void glColor3ub(byte p0, byte p1, byte p2) { GL11.glColor3ub(p0, p1, p2); }
    public static void glColor4f(float p0, float p1, float p2, float p3) { GL11.glColor4f(p0, p1, p2, p3); }
    public static void glColor4ub(byte p0, byte p1, byte p2, byte p3) { GL11.glColor4ub(p0, p1, p2, p3); }
    public static void glColorMask(boolean p0, boolean p1, boolean p2, boolean p3) { GL11.glColorMask(p0, p1, p2, p3); }
    public static void glColorPointer(int p0, int p1, FloatBuffer p2) { GL11.glColorPointer(p0, p1, p2); }
    public static void glColorPointer(int p0, int p1, int p2, long p3) { GL11.glColorPointer(p0, p1, p2, p3); }
    public static void glColorPointer(int p0, boolean p1, int p2, ByteBuffer p3) { GL11.glColorPointer(p0, p1, p2, p3); }
    public static void glDeleteTextures(int p0) { GL11.glDeleteTextures(p0); }
    public static void glDeleteTextures(IntBuffer p0) { GL11.glDeleteTextures(p0); }
    public static void glDisable(int p0) { GL11.glDisable(p0); }
    public static void glDisableClientState(int p0) { GL11.glDisableClientState(p0); }
    public static void glDrawArrays(int p0, int p1, int p2) { GL11.glDrawArrays(p0, p1, p2); }
    public static void glDrawElements(int p0, IntBuffer p1) { GL11.glDrawElements(p0, p1); }
    public static void glEnable(int p0) { GL11.glEnable(p0); }
    public static void glEnableClientState(int p0) { GL11.glEnableClientState(p0); }
    public static void glEnd() { GL11.glEnd(); }
    public static void glEndList() { GL11.glEndList(); }
    public static void glFinish() { GL11.glFinish(); }
    public static void glFlush() { GL11.glFlush(); }
    public static int glGenLists(int p0) { return GL11.glGenLists(p0); }
    public static void glDeleteLists(int p0, int p1) { GL11.glDeleteLists(p0, p1); }
    public static void glGenTextures(IntBuffer p0) { GL11.glGenTextures(p0); }
    public static void glHint(int p0, int p1) { GL11.glHint(p0, p1); }
    public static boolean glIsEnabled(int p0) { return GL11.glIsEnabled(p0); }
    public static void glLight(int p0, int p1, FloatBuffer p2) { GL11.glLight(p0, p1, p2); }
    public static void glLineWidth(float p0) { GL11.glLineWidth(p0); }
    public static void glLoadIdentity() { GL11.glLoadIdentity(); }
    public static void glMaterial(int p0, int p1, FloatBuffer p2) { GL11.glMaterial(p0, p1, p2); }
    public static void glMateriali(int p0, int p1, int p2) { GL11.glMateriali(p0, p1, p2); }
    public static void glMatrixMode(int p0) { GL11.glMatrixMode(p0); }
    public static void glNewList(int p0, int p1) { GL11.glNewList(p0, p1); }
    public static void glNormal3f(float p0, float p1, float p2) { GL11.glNormal3f(p0, p1, p2); }
    public static void glOrtho(double p0, double p1, double p2, double p3, double p4, double p5) { GL11.glOrtho(p0, p1, p2, p3, p4, p5); }
    public static void glPixelStorei(int p0, int p1) { GL11.glPixelStorei(p0, p1); }
    public static void glPointSize(float p0) { GL11.glPointSize(p0); }
    public static void glPolygonMode(int p0, int p1) { GL11.glPolygonMode(p0, p1); }
    public static void glPopAttrib() { GL11.glPopAttrib(); }
    public static void glPopMatrix() { GL11.glPopMatrix(); }
    public static void glPushAttrib(int p0) { GL11.glPushAttrib(p0); }
    public static void glPushMatrix() { GL11.glPushMatrix(); }
    public static void glMultMatrix(FloatBuffer p0) { GL11.glMultMatrix(p0); }
    public static void glReadPixels(int p0, int p1, int p2, int p3, int p4, int p5, FloatBuffer p6) { GL11.glReadPixels(p0, p1, p2, p3, p4, p5, p6); }
    public static void glRotatef(float p0, float p1, float p2, float p3) { GL11.glRotatef(p0, p1, p2, p3); }
    public static void glScalef(float p0, float p1, float p2) { GL11.glScalef(p0, p1, p2); }
    public static void glScissor(int p0, int p1, int p2, int p3) { GL11.glScissor(p0, p1, p2, p3); }
    public static void glShadeModel(int p0) { GL11.glShadeModel(p0); }
    public static void glStencilFunc(int p0, int p1, int p2) { GL11.glStencilFunc(p0, p1, p2); }
    public static void glStencilOp(int p0, int p1, int p2) { GL11.glStencilOp(p0, p1, p2); }
    public static void glTexCoord2f(float p0, float p1) { GL11.glTexCoord2f(p0, p1); }
    public static void glTexCoordPointer(int p0, int p1, FloatBuffer p2) { GL11.glTexCoordPointer(p0, p1, p2); }
    public static void glTexCoordPointer(int p0, int p1, int p2, long p3) { GL11.glTexCoordPointer(p0, p1, p2, p3); }
    public static void glTexEnvf(int p0, int p1, float p2) { GL11.glTexEnvf(p0, p1, p2); }
    public static void glTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, ByteBuffer p8) { GL11.glTexImage2D(p0, p1, p2, p3, p4, p5, p6, p7, p8); }
    public static void glTexParameteri(int p0, int p1, int p2) { GL11.glTexParameteri(p0, p1, p2); }
    public static void glTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, ByteBuffer p8) { GL11.glTexSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, p8); }
    public static void glTranslated(double p0, double p1, double p2) { GL11.glTranslated(p0, p1, p2); }
    public static void glTranslatef(float p0, float p1, float p2) { GL11.glTranslatef(p0, p1, p2); }
    public static void glVertex2f(float p0, float p1) { GL11.glVertex2f(p0, p1); }
    public static void glVertex3d(double p0, double p1, double p2) { GL11.glVertex3d(p0, p1, p2); }
    public static void glVertex3f(float p0, float p1, float p2) { GL11.glVertex3f(p0, p1, p2); }
    public static void glVertexPointer(int p0, int p1, FloatBuffer p2) { GL11.glVertexPointer(p0, p1, p2); }
    public static void glVertexPointer(int p0, int p1, IntBuffer p2) { GL11.glVertexPointer(p0, p1, p2); }
    public static void glVertexPointer(int p0, int p1, int p2, long p3) { GL11.glVertexPointer(p0, p1, p2, p3); }
    public static void glInterleavedArrays(int p0, int p1, FloatBuffer p2) { GL11.glInterleavedArrays(p0, p1, p2); }
    public static void glColorMaterial(int p0, int p1) { GL11.glColorMaterial(p0, p1); }
    public static void glViewport(int p0, int p1, int p2, int p3) { GL11.glViewport(p0, p1, p2, p3); }
}
