package java.util;

/**
 * GWT polyfill for StringFormat.format()
 * Provides string formatting functionality
 */
public class StringFormat {
    
    /**
     * Format a string with arguments
     * Supports: %s (string), %d (integer), %f (float), %.2f (float with 2 decimals)
     */
    public static String format(String format, Object... args) {
        if (format == null) {
            return "";
        }
        
        if (args == null || args.length == 0) {
            return format;
        }
        
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        
        while (i < format.length()) {
            char c = format.charAt(i);
            
            if (c == '%' && i + 1 < format.length()) {
                char next = format.charAt(i + 1);
                
                if (next == '%') {
                    result.append('%');
                    i += 2;
                } else if (argIndex < args.length) {
                    Object arg = args[argIndex++];
                    String formatted = formatSpecifier(next, arg);
                    result.append(formatted);
                    i += 2;
                } else {
                    result.append(c);
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Format a single specifier
     */
    private static String formatSpecifier(char specifier, Object arg) {
        if (arg == null) {
            return "null";
        }
        
        switch (specifier) {
            case 's':
                return arg.toString();
            case 'd':
                if (arg instanceof Number) {
                    return String.valueOf(((Number) arg).intValue());
                }
                return arg.toString();
            case 'f':
                if (arg instanceof Number) {
                    return String.valueOf(((Number) arg).floatValue());
                }
                return arg.toString();
            case 'x':
                if (arg instanceof Number) {
                    return Integer.toHexString(((Number) arg).intValue());
                }
                return arg.toString();
            case 'X':
                if (arg instanceof Number) {
                    return Integer.toHexString(((Number) arg).intValue()).toUpperCase();
                }
                return arg.toString();
            case 'b':
                return String.valueOf(arg instanceof Boolean ? arg : arg != null);
            case 'c':
                if (arg instanceof Character) {
                    return String.valueOf(arg);
                } else if (arg instanceof Number) {
                    return String.valueOf((char) ((Number) arg).intValue());
                }
                return arg.toString();
            default:
                return arg.toString();
        }
    }
    
    /**
     * Format with precision (e.g., %.2f)
     */
    public static String formatWithPrecision(String format, Object... args) {
        if (format == null) {
            return "";
        }
        
        if (args == null || args.length == 0) {
            return format;
        }
        
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        
        while (i < format.length()) {
            char c = format.charAt(i);
            
            if (c == '%' && i + 1 < format.length()) {
                // Check for precision specifier like %.2f
                if (i + 2 < format.length() && format.charAt(i + 1) == '.') {
                    int precisionStart = i + 2;
                    int precisionEnd = precisionStart;
                    
                    while (precisionEnd < format.length() && 
                           Character.isDigit(format.charAt(precisionEnd))) {
                        precisionEnd++;
                    }
                    
                    if (precisionEnd < format.length() && precisionEnd > precisionStart) {
                        int precision = Integer.parseInt(format.substring(precisionStart, precisionEnd));
                        char type = format.charAt(precisionEnd);
                        
                        if (argIndex < args.length) {
                            Object arg = args[argIndex++];
                            String formatted = formatWithPrecision(type, arg, precision);
                            result.append(formatted);
                            i = precisionEnd + 1;
                            continue;
                        }
                    }
                }
                
                char next = format.charAt(i + 1);
                
                if (next == '%') {
                    result.append('%');
                    i += 2;
                } else if (argIndex < args.length) {
                    Object arg = args[argIndex++];
                    String formatted = formatSpecifier(next, arg);
                    result.append(formatted);
                    i += 2;
                } else {
                    result.append(c);
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Format with precision
     */
    private static String formatWithPrecision(char type, Object arg, int precision) {
        if (arg == null) {
            return "null";
        }
        
        if (type == 'f' && arg instanceof Number) {
            double value = ((Number) arg).doubleValue();
            return StringFormat.format("%." + precision + "f", value);
        }
        
        return arg.toString();
    }
}