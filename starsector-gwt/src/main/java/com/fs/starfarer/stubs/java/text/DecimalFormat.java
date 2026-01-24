package com.fs.starfarer.stubs.java.text;

public class DecimalFormat {
    public DecimalFormat() {}
    public DecimalFormat(String pattern) {}
    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {}
    public String format(double number) { return String.valueOf(number); }
    public String format(long number) { return String.valueOf(number); }
    public Number parse(String source) { return 0.0; }
}