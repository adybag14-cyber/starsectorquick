package java.awt.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

/**
 * GWT polyfill for java.awt.image.BufferedImage
 * Uses HTML5 Canvas API
 */
public class BufferedImage extends Image {
    private int width;
    private int height;
    private int imageType;
    private transient native JSCanvas canvas;
    private transient native JSContext2D context;
    
    public static final int TYPE_INT_RGB = 1;
    public static final int TYPE_INT_ARGB = 2;
    public static final int TYPE_INT_ARGB_PRE = 3;
    public static final int TYPE_INT_BGR = 4;
    public static final int TYPE_3BYTE_BGR = 5;
    public static final int TYPE_4BYTE_ABGR = 6;
    public static final int TYPE_4BYTE_ABGR_PRE = 7;
    public static final int TYPE_BYTE_GRAY = 10;
    public static final int TYPE_USHORT_GRAY = 11;
    public static final int TYPE_BYTE_BINARY = 12;
    public static final int TYPE_BYTE_INDEXED = 13;
    
    public BufferedImage(int width, int height, int imageType) {
        this.width = width;
        this.height = height;
        this.imageType = imageType;
        this.canvas = createCanvas(width, height);
        this.context = getContext2D(canvas);
    }
    
    public BufferedImage(int width, int height, int imageType, ColorModel cm) {
        this(width, height, imageType);
    }
    
    private native JSCanvas createCanvas(int width, int height) /*-{
        var canvas = $doc.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        return canvas;
    }-*/;
    
    private native JSContext2D getContext2D(JSCanvas canvas) /*-{
        return canvas.getContext('2d');
    }-*/;
    
    /**
     * Get the width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Get the height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Get the image type
     */
    public int getType() {
        return imageType;
    }
    
    /**
     * Get the color model
     */
    public ColorModel getColorModel() {
        return new DirectColorModel();
    }
    
    /**
     * Get the raster
     */
    public WritableRaster getRaster() {
        return new WritableRaster(width, height);
    }
    
    /**
     * Get the graphics
     */
    public Graphics getGraphics() {
        return new Graphics2D(context, width, height);
    }
    
    /**
     * Create a graphics 2D
     */
    public Graphics2D createGraphics() {
        return new Graphics2D(context, width, height);
    }
    
    /**
     * Get RGB value at position
     */
    public int getRGB(int x, int y) {
        return getNativeRGB(x, y);
    }
    
    private native int getNativeRGB(int x, int y) /*-{
        var imageData = this.@java.awt.image.BufferedImage::context.getImageData(x, y, 1, 1);
        var data = imageData.data;
        return (data[3] << 24) | (data[0] << 16) | (data[1] << 8) | data[2];
    }-*/;
    
    /**
     * Get RGB values for region
     */
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        if (rgbArray == null) {
            rgbArray = new int[w * h];
        }
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                rgbArray[offset + y * scansize + x] = getRGB(startX + x, startY + y);
            }
        }
        
        return rgbArray;
    }
    
    /**
     * Set RGB value at position
     */
    public void setRGB(int x, int y, int rgb) {
        setNativeRGB(x, y, rgb);
    }
    
    private native void setNativeRGB(int x, int y, int rgb) /*-{
        var r = (rgb >> 16) & 0xFF;
        var g = (rgb >> 8) & 0xFF;
        var b = rgb & 0xFF;
        var a = (rgb >> 24) & 0xFF;
        this.@java.awt.image.BufferedImage::context.fillStyle = 'rgba(' + r + ',' + g + ',' + b + ',' + (a/255) + ')';
        this.@java.awt.image.BufferedImage::context.fillRect(x, y, 1, 1);
    }-*/;
    
    /**
     * Set RGB values for region
     */
    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setRGB(startX + x, startY + y, rgbArray[offset + y * scansize + x]);
            }
        }
    }
    
    /**
     * Get the canvas element
     */
    public native JSCanvas getCanvas() /*-{
        return this.@java.awt.image.BufferedImage::canvas;
    }-*/;
    
    /**
     * Get the data URL
     */
    public native String getDataURL() /*-{
        return this.@java.awt.image.BufferedImage::canvas.toDataURL();
    }-*/;
    
    /**
     * Flush the image
     */
    public void flush() {
        // No-op in GWT
    }
    
    /**
     * Get a subimage
     */
    public BufferedImage getSubimage(int x, int y, int w, int h) {
        BufferedImage subimage = new BufferedImage(w, h, imageType);
        subimage.getGraphics().drawImage(this, 0, 0, w, h, x, y, x + w, y + h, null);
        return subimage;
    }
}