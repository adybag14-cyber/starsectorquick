package com.fs.starfarer.api.util;

/** Exact browser fast path for Misc.fill()'s fractal-noise exponent scale. */
public final class BrowserFractalNoiseCompat {
    private static final String ENABLE_PROPERTY = "starsector.browserQuickResourceLoad";
    private static int enabled = -1;

    private BrowserFractalNoiseCompat() {}

    public static double pow(double base, double exponent) {
        int state = enabled;
        if (state < 0) {
            state = Boolean.getBoolean(ENABLE_PROPERTY) ? 1 : 0;
            enabled = state;
        }
        // Misc.fill() obtains exponent by widening an int, so it is always finite.
        // Math.pow(1d, finite) is exactly 1d; preserve every other case verbatim.
        if (state == 1 && base == 1.0d) {
            return 1.0d;
        }
        return Math.pow(base, exponent);
    }
}
