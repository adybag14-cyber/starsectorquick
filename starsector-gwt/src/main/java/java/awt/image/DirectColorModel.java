package java.awt.image;

/**
 * GWT polyfill for java.awt.image.DirectColorModel
 */
public class DirectColorModel extends ColorModel {
    private int redMask;
    private int greenMask;
    private int blueMask;
    private int alphaMask;
    
    public DirectColorModel() {
        super(32);
        this.redMask = 0x00FF0000;
        this.greenMask = 0x0000FF00;
        this.blueMask = 0x000000FF;
        this.alphaMask = 0xFF000000;
    }
    
    public DirectColorModel(int bits, int rmask, int gmask, int bmask, int amask) {
        super(bits);
        this.redMask = rmask;
        this.greenMask = gmask;
        this.blueMask = bmask;
        this.alphaMask = amask;
    }
    
    @Override
    public int getRed(int pixel) {
        return (pixel & redMask) >>> 16;
    }
    
    @Override
    public int getGreen(int pixel) {
        return (pixel & greenMask) >>> 8;
    }
    
    @Override
    public int getBlue(int pixel) {
        return pixel & blueMask;
    }
    
    @Override
    public int getAlpha(int pixel) {
        return (pixel & alphaMask) >>> 24;
    }
    
    @Override
    public int getRGB(int pixel) {
        return pixel;
    }
}