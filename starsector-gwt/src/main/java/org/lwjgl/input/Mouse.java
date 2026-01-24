package org.lwjgl.input;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(namespace = JsPackage.GLOBAL, name = "gwtMouse")
public class Mouse {
    private static int x, y, dx, dy, dwheel;
    private static boolean[] buttons = new boolean[10];
    
    @JsMethod
    public static boolean isButtonDown(int button) {
        if (button >= 0 && button < buttons.length) return buttons[button];
        return false;
    }
    
    @JsMethod
    public static int getX() { return x; }
    @JsMethod
    public static int getY() { return y; }
    @JsMethod
    public static int getDX() { return dx; }
    @JsMethod
    public static int getDY() { return dy; }
    @JsMethod
    public static int getDWheel() { return dwheel; }
    
    @JsMethod
    public static boolean next() { return false; }
    
    @JsMethod
    public static void setPosition(int newX, int newY) {
        dx = newX - x;
        dy = newY - y;
        x = newX;
        y = newY;
    }
    
    @JsMethod
    public static void setCursorPosition(int newX, int newY) {
        setPosition(newX, newY);
    }
    
    @JsMethod
    public static void setButton(int button, boolean down) {
        if (button >= 0 && button < buttons.length) buttons[button] = down;
    }
    
    @JsMethod
    public static void setDWheel(int d) { dwheel = d; }
}