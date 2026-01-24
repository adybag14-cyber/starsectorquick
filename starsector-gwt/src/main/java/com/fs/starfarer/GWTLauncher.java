package com.fs.starfarer;

import com.google.gwt.core.client.EntryPoint;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLCanvasElement;
import org.lwjgl.opengl.GL11;

public class GWTLauncher implements EntryPoint {
    @Override
    public void onModuleLoad() {
        DomGlobal.console.log("Starsector GWT Initializing...");
        HTMLCanvasElement canvas = (HTMLCanvasElement) DomGlobal.document.createElement("canvas");
        canvas.id = "gameCanvas";
        canvas.width = 1024;
        canvas.height = 768;
        DomGlobal.document.body.appendChild(canvas);
        
        // Initialize and touch classes to ensure they are compiled and exported via JsInterop
        GL11.init(canvas);
        org.lwjgl.input.Keyboard.isKeyDown(0);
        org.lwjgl.input.Mouse.getX();
        org.lwjgl.opengl.Display.getWidth();
        
        DomGlobal.console.log("WebGL Canvas Created and Bridge Active.");
    }
}