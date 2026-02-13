package org.lwjgl.util;

import org.lwjgl.opengl.DisplayMode;

public class Display {
    public static DisplayMode[] getAvailableDisplayModes(int minWidth, int minHeight, int maxWidth, int maxHeight, int minBPP, int maxBPP, int minFreq, int maxFreq) {
        return new DisplayMode[] { new DisplayMode(1024, 768) };
    }
    public static DisplayMode setDisplayMode(DisplayMode[] modes, String[] criteria) {
        if (modes != null && modes.length > 0) return modes[0];
        return new DisplayMode(1024, 768);
    }
}
