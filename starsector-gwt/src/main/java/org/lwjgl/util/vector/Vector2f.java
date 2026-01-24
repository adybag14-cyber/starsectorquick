package org.lwjgl.util.vector;

/**
 * GWT polyfill for org.lwjgl.util.vector.Vector2f
 * Provides 2D vector math operations
 */
public class Vector2f implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    public float x;
    public float y;
    
    public Vector2f() {
        this(0, 0);
    }
    
    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2f(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
    }
    
    /**
     * Set the x and y values
     */
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Set from another vector
     */
    public void set(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
    }
    
    /**
     * Get length of vector
     */
    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }
    
    /**
     * Get squared length (faster than length)
     */
    public float lengthSquared() {
        return x * x + y * y;
    }
    
    /**
     * Normalize this vector
     */
    public Vector2f normalise() {
        float len = length();
        if (len != 0) {
            x /= len;
            y /= len;
        }
        return this;
    }
    
    /**
     * Get normalized copy
     */
    public Vector2f normalise(Vector2f dest) {
        float len = length();
        if (len != 0) {
            dest.x = x / len;
            dest.y = y / len;
        } else {
            dest.x = 0;
            dest.y = 0;
        }
        return dest;
    }
    
    /**
     * Add another vector to this one
     */
    public Vector2f add(Vector2f v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }
    
    /**
     * Add two vectors and store in dest
     */
    public static Vector2f add(Vector2f left, Vector2f right, Vector2f dest) {
        if (dest == null) {
            return new Vector2f(left.x + right.x, left.y + right.y);
        }
        dest.set(left.x + right.x, left.y + right.y);
        return dest;
    }
    
    /**
     * Subtract another vector from this one
     */
    public Vector2f sub(Vector2f v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }
    
    /**
     * Subtract two vectors and store in dest
     */
    public static Vector2f sub(Vector2f left, Vector2f right, Vector2f dest) {
        if (dest == null) {
            return new Vector2f(left.x - right.x, left.y - right.y);
        }
        dest.set(left.x - right.x, left.y - right.y);
        return dest;
    }
    
    /**
     * Scale this vector
     */
    public Vector2f scale(float scale) {
        this.x *= scale;
        this.y *= scale;
        return this;
    }
    
    /**
     * Dot product with another vector
     */
    public float dot(Vector2f v) {
        return x * v.x + y * v.y;
    }
    
    /**
     * Cross product (returns scalar in 2D)
     */
    public float cross(Vector2f v) {
        return x * v.y - y * v.x;
    }
    
    /**
     * Get distance to another vector
     */
    public float distance(Vector2f v) {
        float dx = x - v.x;
        float dy = y - v.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Get squared distance (faster than distance)
     */
    public float distanceSquared(Vector2f v) {
        float dx = x - v.x;
        float dy = y - v.y;
        return dx * dx + dy * dy;
    }
    
    /**
     * Negate this vector
     */
    public Vector2f negate() {
        x = -x;
        y = -y;
        return this;
    }
    
    /**
     * Negate and store in dest
     */
    public Vector2f negate(Vector2f dest) {
        if (dest == null) {
            dest = new Vector2f();
        }
        dest.x = -x;
        dest.y = -y;
        return dest;
    }
    
    /**
     * Translate this vector
     */
    public Vector2f translate(float dx, float dy) {
        x += dx;
        y += dy;
        return this;
    }
    
    /**
     * Rotate this vector by angle (in radians)
     */
    public Vector2f rotate(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float newX = x * cos - y * sin;
        float newY = x * sin + y * cos;
        x = newX;
        y = newY;
        return this;
    }
    
    /**
     * Get angle of this vector
     */
    public float getAngle() {
        return (float) Math.atan2(y, x);
    }
    
    /**
     * Get angle between this and another vector
     */
    public float angleBetween(Vector2f v) {
        float dotProduct = dot(v);
        float lengths = length() * v.length();
        if (lengths == 0) {
            return 0;
        }
        return (float) Math.acos(dotProduct / lengths);
    }
    
    /**
     * Lerp between this and another vector
     */
    public Vector2f lerp(Vector2f v, float alpha) {
        x = x + (v.x - x) * alpha;
        y = y + (v.y - y) * alpha;
        return this;
    }
    
    /**
     * Check if equals another vector
     */
    public boolean equals(Vector2f v) {
        return x == v.x && y == v.y;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2f vector2f = (Vector2f) obj;
        return Float.compare(vector2f.x, x) == 0 && Float.compare(vector2f.y, y) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "Vector2f[" + x + ", " + y + "]";
    }
    
    /**
     * Create a copy of this vector
     */
    public Vector2f copy() {
        return new Vector2f(this);
    }
}