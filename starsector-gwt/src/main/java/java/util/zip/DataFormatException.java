package java.util.zip;

/**
 * GWT polyfill for java.util.zip.DataFormatException
 */
public class DataFormatException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public DataFormatException() {
        super();
    }
    
    public DataFormatException(String s) {
        super(s);
    }
}