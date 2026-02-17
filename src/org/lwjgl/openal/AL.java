package org.lwjgl.openal;

import org.lwjgl.LWJGLException;

public final class AL {
    public static void create() throws LWJGLException {}
    public static void create(String deviceArguments, int contextFrequency, int contextRefresh, boolean contextSynchronized) throws LWJGLException {}
    public static void create(String deviceArguments, int contextFrequency, int contextRefresh, boolean contextSynchronized, boolean openDevice) throws LWJGLException {}
    public static void destroy() {}
    public static boolean isCreated() { return true; }
    public static ALCdevice getDevice() { return null; }
    public static ALCcontext getContext() { return null; }
}
