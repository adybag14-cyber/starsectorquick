package java.awt.image;

/**
 * GWT polyfill for java.awt.image.ColorModel
 */
public abstract class ColorModel {
    protected int pixel_bits;
    protected int transferType;
    
    protected ColorModel(int bits) {
        this.pixel_bits = bits;
    }
    
    public int getRGB(int pixel) {
        return getRGB(pixel);
    }
    
    public int getRGB(Object inData) {
        return 0;
    }
    
    public int getRed(int pixel) {
        return (pixel >> 16) & 0xFF;
    }
    
    public int getGreen(int pixel) {
        return (pixel >> 8) & 0xFF;
    }
    
    public int getBlue(int pixel) {
        return pixel & 0xFF;
    }
    
    public int getAlpha(int pixel) {
        return (pixel >> 24) & 0xFF;
    }
    
    public int getPixelSize() {
        return pixel_bits;
    }
    
    public int getTransferType() {
        return transferType;
    }
}