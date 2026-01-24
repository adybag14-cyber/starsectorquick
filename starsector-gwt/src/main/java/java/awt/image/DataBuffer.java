package java.awt.image;

/**
 * GWT polyfill for java.awt.image.DataBuffer
 */
public abstract class DataBuffer {
    public static final int TYPE_BYTE = 0;
    public static final int TYPE_USHORT = 1;
    public static final int TYPE_SHORT = 2;
    public static final int TYPE_INT = 3;
    public static final int TYPE_FLOAT = 4;
    public static final int TYPE_DOUBLE = 5;
    public static final int TYPE_UNDEFINED = 32;
    
    protected int dataType;
    protected int banks;
    protected int offset;
    protected int size;
    
    protected DataBuffer(int dataType, int size) {
        this.dataType = dataType;
        this.size = size;
        this.banks = 1;
        this.offset = 0;
    }
    
    protected DataBuffer(int dataType, int size, int numBanks) {
        this.dataType = dataType;
        this.size = size;
        this.banks = numBanks;
        this.offset = 0;
    }
    
    public int getDataType() {
        return dataType;
    }
    
    public int getSize() {
        return size;
    }
    
    public int getNumBanks() {
        return banks;
    }
    
    public int getOffset() {
        return offset;
    }
}