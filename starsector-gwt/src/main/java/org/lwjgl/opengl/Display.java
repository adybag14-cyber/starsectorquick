package org.lwjgl.opengl;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(namespace = JsPackage.GLOBAL, name = "gwtDisplay")
public class Display {
    @JsMethod
    public static void create() throws Exception {}
    @JsMethod
    public static void update() {}
    @JsMethod
    public static boolean isCloseRequested() { return false; }
    @JsMethod
    public static void destroy() {}
    @JsMethod
    public static void setTitle(String title) {}
    @JsMethod
    public static int getWidth() { return 800; }
    @JsMethod
    public static int getHeight() { return 600; }
    @JsMethod
    public static boolean isActive() { return true; }
    @JsMethod
    public static void setVSyncEnabled(boolean enabled) {}
}
