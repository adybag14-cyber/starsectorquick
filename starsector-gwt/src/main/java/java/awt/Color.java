package java.awt;

import elemental2.dom.CanvasRenderingContext2D;

/**
 * GWT polyfill for java.awt.Color
 * Provides color representation and conversion utilities
 */
public class Color {
    private final int value;
    
    // Predefined colors
    public static final Color white = new Color(255, 255, 255);
    public static final Color lightGray = new Color(192, 192, 192);
    public static final Color gray = new Color(128, 128, 128);
    public static final Color darkGray = new Color(64, 64, 64);
    public static final Color black = new Color(0, 0, 0);
    public static final Color red = new Color(255, 0, 0);
    public static final Color pink = new Color(255, 175, 175);
    public static final Color orange = new Color(255, 200, 0);
    public static final Color yellow = new Color(255, 255, 0);
    public static final Color green = new Color(0, 255, 0);
    public static final Color magenta = new Color(255, 0, 255);
    public static final Color cyan = new Color(0, 255, 255);
    public static final Color blue = new Color(0, 0, 255);
    
    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }
    
    public Color(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
    }
    
    public Color(int rgb) {
        value = 0xff000000 | rgb;
    }
    
    public Color(float r, float g, float b) {
        this((int)(r * 255 + 0.5),
             (int)(g * 255 + 0.5),
             (int)(b * 255 + 0.5));
    }
    
    public Color(float r, float g, float b, float a) {
        this((int)(r * 255 + 0.5),
             (int)(g * 255 + 0.5),
             (int)(b * 255 + 0.5),
             (int)(a * 255 + 0.5));
    }
    
    public int getRed() {
        return (value >> 16) & 0xFF;
    }
    
    public int getGreen() {
        return (value >> 8) & 0xFF;
    }
    
    public int getBlue() {
        return (value >> 0) & 0xFF;
    }
    
    public int getAlpha() {
        return (value >> 24) & 0xFF;
    }
    
    public int getRGB() {
        return value;
    }
    
    public Color brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        
        int alpha = getAlpha();
        
        r = (int) Math.min((int) (r / 0.7), 255);
        g = (int) Math.min((int) (g / 0.7), 255);
        b = (int) Math.min((int) (b / 0.7), 255);
        
        return new Color(r, g, b, alpha);
    }
    
    public Color darker() {
        return new Color(Math.max((int) (getRed() * 0.7), 0),
                         Math.max((int) (getGreen() * 0.7), 0),
                         Math.max((int) (getBlue() * 0.7), 0),
                         getAlpha());
    }
    
    public String toString() {
        return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
    }
    
    /**
     * Convert to CSS color string
     */
    public String toCSS() {
        return "rgba(" + getRed() + "," + getGreen() + "," + getBlue() + "," + (getAlpha() / 255.0) + ")";
    }
    
    /**
     * Convert to hex color string
     */
    public String toHex() {
        return "#" + 
               Integer.toHexString(getRed()).toUpperCase() +
               Integer.toHexString(getGreen()).toUpperCase() +
               Integer.toHexString(getBlue()).toUpperCase();
    }
    
    /**
     * Parse hex color string
     */
    public static Color decode(String nm) {
        int i = Integer.decode(nm);
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }
    
    /**
     * Get HSB values
     */
    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;
        
        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }
    
    /**
     * Get RGB from HSB
     */
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5);
                    g = (int) (t * 255.0f + 0.5);
                    b = (int) (p * 255.0f + 0.5);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5);
                    g = (int) (brightness * 255.0f + 0.5);
                    b = (int) (p * 255.0f + 0.5);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5);
                    g = (int) (brightness * 255.0f + 0.5);
                    b = (int) (t * 255.0f + 0.5);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5);
                    g = (int) (q * 255.0f + 0.5);
                    b = (int) (brightness * 255.0f + 0.5);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5);
                    g = (int) (p * 255.0f + 0.5);
                    b = (int) (brightness * 255.0f + 0.5);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5);
                    g = (int) (p * 255.0f + 0.5);
                    b = (int) (q * 255.0f + 0.5);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }
    
    /**
     * Get color from HSB
     */
    public static Color getHSBColor(float h, float s, float b) {
        return new Color(HSBtoRGB(h, s, b));
    }
}