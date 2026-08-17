package com.fs.starfarer.campaign.econ.reach;

import java.util.Random;

/** Exact Java Random state reuse for the browser economy stockpile hot path. */
public final class BrowserEconomyRandomCompat {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserQuickResourceLoad";
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1L;
    private static volatile int enabled = -1;

    private static final ThreadLocal<FastRandom> LOCAL = new ThreadLocal<FastRandom>() {
        @Override
        protected FastRandom initialValue() {
            return new FastRandom();
        }
    };

    private BrowserEconomyRandomCompat() {}

    public static Random acquire(long seed) {
        if (!enabled()) {
            return new Random(seed);
        }
        FastRandom random = LOCAL.get();
        random.reset(seed);
        return random;
    }

    private static boolean enabled() {
        int state = enabled;
        if (state < 0) {
            state = Boolean.getBoolean(ENABLE_PROPERTY) ? 1 : 0;
            enabled = state;
        }
        return state == 1;
    }

    static final class FastRandom extends Random {
        private static final long serialVersionUID = 1L;
        private long state;

        FastRandom() {
            super(0L);
        }

        void reset(long seed) {
            state = (seed ^ MULTIPLIER) & MASK;
        }

        @Override
        protected int next(int bits) {
            state = (state * MULTIPLIER + ADDEND) & MASK;
            return (int) (state >>> (48 - bits));
        }
    }
}
