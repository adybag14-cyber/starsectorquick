package org.lwjgl.util.vector;

public class Vector3f {
    public float x, y, z;
    public Vector3f() {}
    public Vector3f(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public Vector3f set(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
    public float length() { return (float) Math.sqrt(x*x + y*y + z*z); }
    public static Vector3f sub(Vector3f v1, Vector3f v2, Vector3f dest) {
        if (dest == null) dest = new Vector3f();
        dest.x = v1.x - v2.x;
        dest.y = v1.y - v2.y;
        dest.z = v1.z - v2.z;
        return dest;
    }
}