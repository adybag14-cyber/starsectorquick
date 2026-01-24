package java.awt.image;

/**
 * GWT polyfill for java.awt.image.Raster
 */
public class Raster {
    protected SampleModel sampleModel;
    protected DataBuffer dataBuffer;
    
    protected Raster(SampleModel sampleModel, DataBuffer dataBuffer) {
        this.sampleModel = sampleModel;
        this.dataBuffer = dataBuffer;
    }
    
    public SampleModel getSampleModel() {
        return sampleModel;
    }
    
    public DataBuffer getDataBuffer() {
        return dataBuffer;
    }
    
    public int getWidth() {
        return 0;
    }
    
    public int getHeight() {
        return 0;
    }
    
    public int getMinX() {
        return 0;
    }
    
    public int getMinY() {
        return 0;
    }
    
    public int getNumBands() {
        return 4;
    }
}