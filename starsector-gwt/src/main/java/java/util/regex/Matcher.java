package java.util.regex;

/**
 * GWT polyfill for java.util.regex.Matcher
 * Works with Pattern to perform regex operations
 */
public class Matcher implements java.io.MatchResult {
    private final Pattern pattern;
    private final String input;
    private int start;
    private int end;
    private boolean found;
    
    Matcher(Pattern pattern, String input) {
        this.pattern = pattern;
        this.input = input;
        this.start = 0;
        this.end = 0;
        this.found = false;
    }
    
    /**
     * Attempt to find the next match
     */
    public boolean find() {
        return find(start);
    }
    
    /**
     * Attempt to find a match starting at the given position
     */
    public boolean find(int start) {
        String substring = input.substring(start);
        if (pattern.matches(substring)) {
            this.start = start;
            this.end = start + substring.length();
            this.found = true;
            return true;
        }
        return false;
    }
    
    /**
     * Attempt to match the entire region
     */
    public boolean matches() {
        if (pattern.matches(input)) {
            this.start = 0;
            this.end = input.length();
            this.found = true;
            return true;
        }
        return false;
    }
    
    /**
     * Attempt to match the input starting at the beginning
     */
    public boolean lookingAt() {
        if (pattern.matches(input)) {
            this.start = 0;
            this.end = input.length();
            this.found = true;
            return true;
        }
        return false;
    }
    
    /**
     * Reset this matcher
     */
    public Matcher reset() {
        start = 0;
        end = 0;
        found = false;
        return this;
    }
    
    /**
     * Reset this matcher with new input
     */
    public Matcher reset(CharSequence input) {
        return new Matcher(pattern, input.toString());
    }
    
    /**
     * Replace all matches with replacement string
     */
    public String replaceAll(String replacement) {
        return input.replaceAll(pattern.pattern(), replacement);
    }
    
    /**
     * Replace first match with replacement string
     */
    public String replaceFirst(String replacement) {
        return input.replaceFirst(pattern.pattern(), replacement);
    }
    
    /**
     * Append replacement to string buffer
     */
    public Matcher appendReplacement(StringBuffer sb, String replacement) {
        if (found) {
            sb.append(input.substring(start, end));
        }
        return this;
    }
    
    /**
     * Append tail of input to string buffer
     */
    public StringBuffer appendTail(StringBuffer sb) {
        sb.append(input.substring(end));
        return sb;
    }
    
    /**
     * Get the start index of the match
     */
    public int start() {
        return start;
    }
    
    /**
     * Get the start index of the specified group
     */
    public int start(int group) {
        return start;
    }
    
    /**
     * Get the end index of the match
     */
    public int end() {
        return end;
    }
    
    /**
     * Get the end index of the specified group
     */
    public int end(int group) {
        return end;
    }
    
    /**
     * Get the matched text
     */
    public String group() {
        if (found) {
            return input.substring(start, end);
        }
        return null;
    }
    
    /**
     * Get the matched text for the specified group
     */
    public String group(int group) {
        return group();
    }
    
    /**
     * Get the number of capturing groups
     */
    public int groupCount() {
        return 0;
    }
    
    /**
     * Get the pattern
     */
    public Pattern pattern() {
        return pattern;
    }
    
    /**
     * Get the input
     */
    public CharSequence input() {
        return input;
    }
}