package com.fs.starfarer;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

/** Narrow browser compatibility helpers for stock core lifecycle callbacks. */
public final class CoreLifecycleCompat {
    private CoreLifecycleCompat() {}

    /** Preserve the stock tag operation when the market exists; absent markets are a no-op. */
    public static void addMarketTagIfPresent(MarketAPI market, String tag) {
        if (market != null) {
            market.addTag(tag);
        }
    }
}
