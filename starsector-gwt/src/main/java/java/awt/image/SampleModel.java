package java.awt.image;

/**
 * GWT polyfill for java.awt.image.SampleModel
 */
public abstract class SampleModel {
    protected int width;
    protected int height;
    protected int numBands;
    protected int dataType;
    
    protected SampleModel(int dataType, int w, int h, int numBands) {
        this.dataType = dataType;
        this.width = w;
        this.height = h;
        this.numBands = numBands;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getNumBands() {
        return numBands;
    }
    
    public int getDataType() {
        return dataType;
    }
}