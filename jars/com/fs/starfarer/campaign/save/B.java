package com.fs.starfarer.campaign.save;

/**
 * Minimal progress widget shim used during save/load/new-game operations.
 *
 * The original obfuscated class depends on UI text renderers that can be null in
 * the CheerpJ bridge, causing NPEs in campaign bootstrap paths. This shim keeps
 * the call signatures used by CampaignGameManager while avoiding renderer access.
 */
public class B {
    private volatile java.lang.String statusText;
    private volatile float percent;
    private volatile long lastLogAtMs;

    public B() {
        this(null);
    }

    public B(java.lang.String text) {
        this.statusText = text == null ? "Initializing..." : text;
        this.percent = 0f;
        this.lastLogAtMs = 0L;
    }

    // Signature used by CampaignGameManager and stream progress wrappers.
    public void o00000(java.lang.String text, float pct) {
        if (text != null && !text.isEmpty()) {
            statusText = text;
        }
        percent = pct;
        maybeLog();
    }

    // Compatibility overload (safe no-op style updates).
    public void o00000(float pct) {
        percent = pct;
        maybeLog();
    }

    // Compatibility overload for status text updates.
    public void o00000(java.lang.String text) {
        if (text != null) {
            statusText = text;
        }
    }

    // Compatibility accessor used by some save/load code paths.
    public java.lang.String o00000() {
        return statusText;
    }

    private void maybeLog() {
        long now = System.currentTimeMillis();
        if (now - lastLogAtMs < 2000L) {
            return;
        }
        lastLogAtMs = now;
        java.lang.String text = statusText == null ? "" : statusText;
        float p = percent;
        if (p < 0f) p = 0f;
        if (p > 100f) p = 100f;
        System.out.println("Fixer: save/load progress: " + text + " (" + p + "%)");
    }
}
