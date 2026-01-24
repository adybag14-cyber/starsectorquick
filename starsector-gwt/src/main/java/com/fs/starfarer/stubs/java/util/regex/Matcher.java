package com.fs.starfarer.stubs.java.util.regex;

import com.google.gwt.regexp.shared.MatchResult;

public class Matcher {
    private final Pattern pattern;
    private final String input;
    private MatchResult lastResult;

    Matcher(Pattern pattern, CharSequence input) {
        this.pattern = pattern;
        this.input = input.toString();
    }

    public boolean matches() {
        lastResult = pattern.getRegExp().exec(input);
        return lastResult != null && lastResult.getGroup(0).length() == input.length();
    }

    public boolean find() {
        lastResult = pattern.getRegExp().exec(input);
        return lastResult != null;
    }

    public String group(int group) {
        if (lastResult == null) return null;
        return lastResult.getGroup(group);
    }
    
    public String replaceFirst(String replacement) {
        return pattern.getRegExp().replace(input, replacement);
    }
    
    public String replaceAll(String replacement) {
        // GWT RegExp.replace with global flag
        return pattern.getRegExp().replace(input, replacement);
    }
}