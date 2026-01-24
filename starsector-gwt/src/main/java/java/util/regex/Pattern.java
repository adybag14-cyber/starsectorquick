package java.util.regex;

/**
 * GWT polyfill for java.util.regex.Pattern
 * Uses JavaScript's RegExp engine
 */
public class Pattern implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String regex;
    private final int flags;
    private transient native RegExp nativeRegExp;
    
    public static final int UNIX_LINES = 0x01;
    public static final int CASE_INSENSITIVE = 0x02;
    public static final int COMMENTS = 0x04;
    public static final int MULTILINE = 0x08;
    public static final int LITERAL = 0x10;
    public static final int DOTALL = 0x20;
    public static final int UNICODE_CASE = 0x40;
    public static final int CANON_EQ = 0x80;
    
    private Pattern(String regex, int flags) {
        this.regex = regex;
        this.flags = flags;
        this.nativeRegExp = compileNative(regex, flags);
    }
    
    /**
     * Compile a regex pattern
     */
    public static Pattern compile(String regex) {
        return compile(regex, 0);
    }
    
    /**
     * Compile a regex pattern with flags
     */
    public static Pattern compile(String regex, int flags) {
        return new Pattern(regex, flags);
    }
    
    /**
     * Compile to native JavaScript RegExp
     */
    private native RegExp compileNative(String regex, int flags) /*-{
        var jsFlags = "";
        if (flags & @java.util.regex.Pattern::CASE_INSENSITIVE) {
            jsFlags += "i";
        }
        if (flags & @java.util.regex.Pattern::MULTILINE) {
            jsFlags += "m";
        }
        return new RegExp(regex, jsFlags);
    }-*/;
    
    /**
     * Create a matcher for this pattern
     */
    public Matcher matcher(CharSequence input) {
        return new Matcher(this, input.toString());
    }
    
    /**
     * Test if the pattern matches the entire input
     */
    public boolean matches(String input) {
        return nativeRegExp.test(input);
    }
    
    /**
     * Split the input by this pattern
     */
    public String[] split(CharSequence input) {
        return split(input, 0);
    }
    
    /**
     * Split the input by this pattern with limit
     */
    public String[] split(CharSequence input, int limit) {
        String[] result = nativeSplit(input.toString(), limit);
        return result;
    }
    
    private native String[] nativeSplit(String input, int limit) /*-{
        if (limit == 0) {
            return input.split(this.@java.util.regex.Pattern::nativeRegExp);
        } else {
            return input.split(this.@java.util.regex.Pattern::nativeRegExp, limit);
        }
    }-*/;
    
    /**
     * Get the pattern string
     */
    public String pattern() {
        return regex;
    }
    
    /**
     * Get the flags
     */
    public int flags() {
        return flags;
    }
    
    /**
     * Quote the string to be used as a literal pattern
     */
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\Q");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length() && s.charAt(i + 1) == 'E') {
                sb.append("\\E\\\\E\\Q");
            } else {
                sb.append(c);
            }
        }
        sb.append("\\E");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return regex;
    }
}