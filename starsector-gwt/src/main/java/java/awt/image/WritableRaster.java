package java.awt.image;

/**
 * GWT polyfill for java.awt.image.WritableRaster
 */
public class WritableRaster extends Raster {
    private int width;
    private int height;
    private int[] data;
    
    protected WritableRaster(int width, int height) {
        super(null, null);
        this.width = width;
        this.height = height;
        this.data = new int[width * height];
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int[] getPixels(int x, int y, int w, int h, int[] iArray) {
        if (iArray == null) {
            iArray = new int[w * h];
        }
        
        int index = 0;
        for (int row = y; row < y + h; row++) {
            for (int col = x; col < x + w; col++) {
                iArray[index++] = data[row * width + col];
            }
        }
        
        return iArray;
    }
    
    public void setPixels(int x, int y, int w, int h, int[] iArray) {
        int index = 0;
        for (int row = y; row < y + h; row++) {
            for (int col = x; col < x + w; col++) {
                data[row * width + col] = iArray[index++];
            }
        }
    }
    
    public int getSample(int x, int y, int b) {
        return data[y * width + x];
    }
    
    public void setSample(int x, int y, int b, int s) {
        data[y * width + x] = s;
    }
}