package com.fs.starfarer;

import com.fs.starfarer.launcher.ModManager;

/**
 * Browser-runtime compatibility for the headless Starsector launcher path.
 *
 * The desktop launcher normally installs ModManager before a new campaign is
 * created. The CheerpJ direct-launch path intentionally bypasses that desktop
 * launcher, so CampaignGameManager can observe ModManager.getInstance() == null.
 * Vanilla new-game creation only needs an empty enabled-mod/plugin list in that
 * case; dereferencing the missing manager otherwise aborts sector creation.
 */
public final class ModManagerCompat {
    private static boolean nullManagerLogged;
    private static boolean pluginFailureLogged;
    private static boolean modFailureLogged;

    private ModManagerCompat() {}

    public static java.util.List getEnabledModPlugins(ModManager manager) {
        if (manager == null) {
            logNullManagerOnce();
            return java.util.Collections.emptyList();
        }
        try {
            java.util.List result = manager.getEnabledModPlugins();
            return result == null ? java.util.Collections.emptyList() : result;
        } catch (Throwable t) {
            if (!pluginFailureLogged) {
                pluginFailureLogged = true;
                System.out.println(
                        "Fixer: ModManager enabled-plugin lookup failed; treating as vanilla/no plugins: "
                                + t.getClass().getName()
                                + (t.getMessage() == null ? "" : ": " + t.getMessage()));
            }
            return java.util.Collections.emptyList();
        }
    }

    public static java.util.List getEnabledMods(ModManager manager) {
        if (manager == null) {
            logNullManagerOnce();
            return java.util.Collections.emptyList();
        }
        try {
            java.util.List result = manager.getEnabledMods();
            return result == null ? java.util.Collections.emptyList() : result;
        } catch (Throwable t) {
            if (!modFailureLogged) {
                modFailureLogged = true;
                System.out.println(
                        "Fixer: ModManager enabled-mod lookup failed; treating as vanilla/no mods: "
                                + t.getClass().getName()
                                + (t.getMessage() == null ? "" : ": " + t.getMessage()));
            }
            return java.util.Collections.emptyList();
        }
    }

    private static void logNullManagerOnce() {
        if (nullManagerLogged) {
            return;
        }
        nullManagerLogged = true;
        System.out.println(
                "Fixer: ModManager is absent in CheerpJ direct-launch mode; using empty vanilla mod/plugin lists.");
    }
}
