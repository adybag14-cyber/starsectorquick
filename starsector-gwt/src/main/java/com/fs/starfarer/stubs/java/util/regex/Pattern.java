package com.fs.starfarer.stubs.java.util.regex;

import com.google.gwt.regexp.shared.RegExp;

public class Pattern {
    private final RegExp regExp;
    private final String pattern;

    private Pattern(String pattern, String flags) {
        this.pattern = pattern;
        this.regExp = RegExp.compile(pattern, flags);
    }

    public static Pattern compile(String regex) {
        return new Pattern(regex, "");
    }

    public static Pattern compile(String regex, int flags) {
        return new Pattern(regex, "");
    }

    public Matcher matcher(CharSequence input) {
        return new Matcher(this, input);
    }

    public String pattern() {
        return pattern;
    }
    
    public RegExp getRegExp() {
        return regExp;
    }
    
    public static String quote(String s) {
        // Simple escaping for regex
        return s.replace("\\", "\\\\").replace(".", "\\.").replace("[", "\\[").replace("]", "\\]");
    }
}
