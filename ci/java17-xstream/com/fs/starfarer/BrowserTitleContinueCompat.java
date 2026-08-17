package com.fs.starfarer;

import com.fs.starfarer.title.TitleScreenState;

/** Browser-only guard for the two cosmetic title renders before Continue loads a save. */
public final class BrowserTitleContinueCompat {
    private static final java.lang.String ENABLE_PROPERTY =
            "starsector.browserContinueRenderGuard";
    private static volatile boolean loggedSuppressedRender;

    private BrowserTitleContinueCompat() {}

    public static void renderBeforeContinue(final TitleScreenState state, final float brightness) {
        if (!Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))) {
            state.render(brightness);
            return;
        }
        try {
            state.render(brightness);
        } catch (final NullPointerException browserTitleRenderNpe) {
            if (!loggedSuppressedRender) {
                loggedSuppressedRender = true;
                System.out.println(
                        "BrowserTitleContinueCompat: suppressed title render NPE before stock Continue load.");
            }
        }
    }
}
