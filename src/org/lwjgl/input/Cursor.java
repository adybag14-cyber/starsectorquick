package org.lwjgl.input;

import org.lwjgl.LWJGLException;
import java.nio.IntBuffer;

public class Cursor {
    public Cursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) throws LWJGLException {}
    public void destroy() {}
    public static int getMinCursorSize() { return 1; }
    public static int getMaxCursorSize() { return 64; }
    public static int getCapabilities() { return 0; }
}
