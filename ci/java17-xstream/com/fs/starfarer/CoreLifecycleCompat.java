package com.fs.starfarer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

/** Narrow browser compatibility helpers for stock core lifecycle callbacks. */
public final class CoreLifecycleCompat {
    private CoreLifecycleCompat() {}

    /** Preserve the stock tag operation when the market exists; absent markets are a no-op. */
    public static void addMarketTagIfPresent(MarketAPI market, java.lang.String tag) {
        if (market != null) {
            market.addTag(tag);
        }
    }

    /**
     * Equivalent to Misc.makeStoryCritical(String,String) for an existing market,
     * but unlike the stock helper it tolerates a market omitted by the partial
     * browser world bootstrap.
     */
    public static void makeStoryCriticalIfMarketPresent(
            java.lang.String marketId, java.lang.String reason) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }
        EconomyAPI economy = sector.getEconomy();
        if (economy == null) {
            return;
        }
        MarketAPI market = economy.getMarket(marketId);
        if (market != null) {
            Misc.makeStoryCritical(market, reason);
        }
    }
}
