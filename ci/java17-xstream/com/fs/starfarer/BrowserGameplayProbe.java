package com.fs.starfarer;

import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Test-only browser gameplay telemetry. Disabled unless explicitly enabled. */
public final class BrowserGameplayProbe {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserGameplayProbe";
    private static final AtomicLong SEQ = new AtomicLong();
    private static final Map<AbilityPlugin, Boolean> SETTLED =
            Collections.synchronizedMap(new WeakHashMap<AbilityPlugin, Boolean>());

    private BrowserGameplayProbe() {}

    public static void abilityPress(AbilityPlugin ability) { emit("ability-press", ability); }
    public static void abilityActivate(AbilityPlugin ability) {
        if (Boolean.getBoolean(ENABLE_PROPERTY) && ability != null) SETTLED.put(ability, Boolean.FALSE);
        emit("ability-activate", ability);
    }
    public static void abilityDeactivate(AbilityPlugin ability) { emit("ability-deactivate", ability); }
    public static void abilityAdvance(AbilityPlugin ability) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || ability == null) return;
        boolean settled;
        try {
            settled = !ability.isActive() && !ability.isInProgress() && ability.getLevel() <= 0.0001f;
        } catch (Throwable ignored) {
            return;
        }
        Boolean previous = SETTLED.put(ability, Boolean.valueOf(settled));
        if (settled && Boolean.FALSE.equals(previous)) emit("ability-settled", ability);
    }

    public static void coreTabStart(Object tab) { emitCore("core-tab-start", tab); }
    public static void coreTabReady(Object tab) { emitCore("core-tab-ready", tab); }
    public static void coreUiDismissed() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        System.out.println("BrowserGameplayProbe: seq=" + SEQ.incrementAndGet() + " event=core-ui-dismissed");
    }

    private static void emitCore(java.lang.String event, Object tab) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        java.lang.String value = tab == null ? "<null>" : safe(java.lang.String.valueOf(tab));
        System.out.println("BrowserGameplayProbe: seq=" + SEQ.incrementAndGet() + " event=" + event + " tab=" + value);
    }

    private static void emit(java.lang.String event, AbilityPlugin ability) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        try {
            if (ability == null) {
                System.out.println("BrowserGameplayProbe: seq=" + SEQ.incrementAndGet() + " event=" + event + " id=<null>");
                return;
            }
            java.lang.String id = safe(ability.getId());
            java.lang.String name = "";
            try {
                AbilitySpecAPI spec = ability.getSpec();
                if (spec != null) name = safe(spec.getName());
            } catch (Throwable ignored) {}
            boolean usable = safeBool(ability, 0);
            boolean active = safeBool(ability, 1);
            boolean progress = safeBool(ability, 2);
            boolean cooldown = safeBool(ability, 3);
            float level = safeFloat(ability, 0);
            float progressFraction = safeFloat(ability, 1);
            float cooldownFraction = safeFloat(ability, 2);
            System.out.println(
                    "BrowserGameplayProbe: seq=" + SEQ.incrementAndGet()
                            + " event=" + event
                            + " id=" + id
                            + " name=" + name
                            + " usable=" + usable
                            + " active=" + active
                            + " inProgress=" + progress
                            + " onCooldown=" + cooldown
                            + " level=" + level
                            + " progress=" + progressFraction
                            + " cooldown=" + cooldownFraction);
        } catch (Throwable error) {
            System.out.println("BrowserGameplayProbe: telemetry failure event=" + event + " error=" + describe(error));
        }
    }

    private static boolean safeBool(AbilityPlugin ability, int which) {
        try {
            switch (which) {
                case 0: return ability.isUsable();
                case 1: return ability.isActive();
                case 2: return ability.isInProgress();
                default: return ability.isOnCooldown();
            }
        } catch (Throwable ignored) { return false; }
    }

    private static float safeFloat(AbilityPlugin ability, int which) {
        try {
            switch (which) {
                case 0: return ability.getLevel();
                case 1: return ability.getProgressFraction();
                default: return ability.getCooldownFraction();
            }
        } catch (Throwable ignored) { return -1f; }
    }

    private static java.lang.String safe(java.lang.String value) {
        if (value == null) return "<null>";
        return value.replace('\n', ' ').replace('\r', ' ').replace(' ', '_');
    }

    private static java.lang.String describe(Throwable error) {
        if (error == null) return "unknown";
        java.lang.String message = error.getMessage();
        return error.getClass().getName() + (message == null ? "" : ":" + message.replace(' ', '_'));
    }
}
