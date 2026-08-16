package com.fs.starfarer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Test-only browser gameplay telemetry. Disabled unless explicitly enabled. */
public final class BrowserGameplayProbe {
    private static final java.lang.String ENABLE_PROPERTY = "starsector.browserGameplayProbe";
    private static final java.lang.String SPEEDUP_PROPERTY = "starsector.browserGameplaySpeedupMult";
    private static final AtomicLong SEQ = new AtomicLong();
    private static final java.util.concurrent.atomic.AtomicBoolean SPEEDUP_APPLIED =
            new java.util.concurrent.atomic.AtomicBoolean();
    private static final Map<AbilityPlugin, Boolean> SETTLED =
            Collections.synchronizedMap(new WeakHashMap<AbilityPlugin, Boolean>());
    private static final Map<AbilityPlugin, Long> ACTIVATED_AT =
            Collections.synchronizedMap(new WeakHashMap<AbilityPlugin, Long>());
    private static final Map<AbilityPlugin, Boolean> READY =
            Collections.synchronizedMap(new WeakHashMap<AbilityPlugin, Boolean>());
    private static final Map<AbilityPlugin, Integer> READY_STREAK =
            Collections.synchronizedMap(new WeakHashMap<AbilityPlugin, Integer>());
    private static final Map<AbilityPlugin, Boolean> READY_STABLE =
            Collections.synchronizedMap(new WeakHashMap<AbilityPlugin, Boolean>());
    private static final Map<Object, java.lang.String> UI_STATE =
            Collections.synchronizedMap(new WeakHashMap<Object, java.lang.String>());

    private BrowserGameplayProbe() {}

    public static void abilityPress(AbilityPlugin ability) { emit("ability-press", ability); }
    public static void abilityActivate(AbilityPlugin ability) {
        if (Boolean.getBoolean(ENABLE_PROPERTY) && ability != null) {
            SETTLED.put(ability, Boolean.FALSE);
            READY_STREAK.put(ability, Integer.valueOf(0));
            READY_STABLE.put(ability, Boolean.FALSE);
            long timestamp = safeCampaignTimestamp();
            if (timestamp != Long.MIN_VALUE) ACTIVATED_AT.put(ability, Long.valueOf(timestamp));
        }
        emit("ability-activate", ability);
    }
    public static void abilityDeactivate(AbilityPlugin ability) { emit("ability-deactivate", ability); }
    public static void abilityAdvance(AbilityPlugin ability) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || ability == null) return;
        maybeApplySpeedupOverride();
        boolean ready;
        boolean settled;
        try {
            ready = ability.isUsable();
            settled = !ability.isActive() && !ability.isInProgress() && ability.getLevel() <= 0.0001f;
        } catch (Throwable ignored) {
            return;
        }
        Boolean previousReady = READY.put(ability, Boolean.valueOf(ready));
        if (previousReady == null || previousReady.booleanValue() != ready) {
            emit(ready ? "ability-ready" : "ability-unready", ability);
        }

        int streak = 0;
        Integer previousStreak = READY_STREAK.get(ability);
        if (ready) streak = (previousStreak == null ? 0 : previousStreak.intValue()) + 1;
        READY_STREAK.put(ability, Integer.valueOf(streak));
        boolean stableReady = ready && streak >= 3;
        Boolean previousStable = READY_STABLE.put(ability, Boolean.valueOf(stableReady));
        if (stableReady && !Boolean.TRUE.equals(previousStable)) {
            emit("ability-ready-stable", ability);
        }

        Boolean previousSettled = SETTLED.put(ability, Boolean.valueOf(settled));
        if (settled && Boolean.FALSE.equals(previousSettled)) emit("ability-settled", ability);
    }

    public static void abilityUiAdvance(Object panel) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || panel == null) return;
        AbilityPlugin ability = reflectAbility(panel);
        if (ability == null) return;
        boolean usable;
        boolean enabled;
        try {
            usable = ability.isUsable();
            enabled = reflectButtonEnabled(panel);
        } catch (Throwable ignored) {
            return;
        }
        java.lang.String state;
        java.lang.String event;
        if (usable && enabled) {
            state = "ready";
            event = "ability-ui-ready";
        } else if (!usable && !enabled) {
            state = "unready";
            event = "ability-ui-unready";
        } else if (usable) {
            state = "lag";
            event = "ability-ui-lag";
        } else {
            state = "stale-enabled";
            event = "ability-ui-stale-enabled";
        }
        java.lang.String previous = UI_STATE.put(panel, state);
        if (!state.equals(previous)) emitUi(event, ability, enabled);
    }

    public static void abilityUiAction(Object panel) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || panel == null) return;
        AbilityPlugin ability = reflectAbility(panel);
        if (ability == null) return;
        boolean enabled = reflectButtonEnabled(panel);
        emitUi("ability-ui-action", ability, enabled);
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
            long gameTs = safeCampaignTimestamp();
            float elapsedDays = -1f;
            Long activatedAt = ACTIVATED_AT.get(ability);
            if (activatedAt != null) elapsedDays = safeElapsedDaysSince(activatedAt.longValue());
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
                            + " cooldown=" + cooldownFraction
                            + " gameTs=" + gameTs
                            + " elapsedDays=" + elapsedDays);
        } catch (Throwable error) {
            System.out.println("BrowserGameplayProbe: telemetry failure event=" + event + " error=" + describe(error));
        }
    }

    private static AbilityPlugin reflectAbility(Object panel) {
        try {
            java.lang.reflect.Method method = panel.getClass().getMethod("getPlugin");
            Object value = method.invoke(panel);
            return value instanceof AbilityPlugin ? (AbilityPlugin)value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean reflectButtonEnabled(Object panel) {
        Class<?> type = panel.getClass();
        while (type != null) {
            try {
                for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                    if (!"com.fs.starfarer.ui.n".equals(field.getType().getName())) continue;
                    field.setAccessible(true);
                    Object button = field.get(panel);
                    if (button == null) continue;
                    java.lang.reflect.Method method = button.getClass().getMethod("isEnabled");
                    Object value = method.invoke(button);
                    if (value instanceof Boolean) return ((Boolean)value).booleanValue();
                }
            } catch (Throwable ignored) {
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static void emitUi(java.lang.String event, AbilityPlugin ability, boolean buttonEnabled) {
        try {
            System.out.println("BrowserGameplayProbe: seq=" + SEQ.incrementAndGet()
                    + " event=" + event
                    + " id=" + safe(ability == null ? null : ability.getId())
                    + " usable=" + (ability != null && safeBool(ability, 0))
                    + " buttonEnabled=" + buttonEnabled
                    + " active=" + (ability != null && safeBool(ability, 1))
                    + " inProgress=" + (ability != null && safeBool(ability, 2))
                    + " level=" + (ability == null ? -1f : safeFloat(ability, 0)));
        } catch (Throwable ignored) {
        }
    }

    public static void controlMatch(Object control, int eventValue, boolean keyDown, boolean consumed, boolean matched) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || control == null || !keyDown) return;
        try {
            java.lang.String name = control instanceof Enum<?>
                    ? ((Enum<?>)control).name()
                    : java.lang.String.valueOf(control);
            if (!("CORE_ABILITY_6".equals(name) || "CORE_ABILITY_7".equals(name) || "CORE_ABILITY_8".equals(name))) return;
            System.out.println("BrowserGameplayProbe: seq=" + SEQ.incrementAndGet()
                    + " event=control-match control=" + safe(name)
                    + " eventValue=" + eventValue
                    + " keyDown=true"
                    + " consumed=" + consumed
                    + " matched=" + matched);
        } catch (Throwable ignored) {
        }
    }

    private static void maybeApplySpeedupOverride() {
        if (SPEEDUP_APPLIED.get()) return;
        java.lang.String raw = System.getProperty(SPEEDUP_PROPERTY, "").trim();
        if (raw.isEmpty()) return;
        try {
            float value = Float.parseFloat(raw);
            if (value < 2f) return;
            value = Math.min(32f, value);
            Global.getSettings().setFloat("campaignSpeedupMult", Float.valueOf(value));
            if (SPEEDUP_APPLIED.compareAndSet(false, true)) {
                System.out.println("BrowserGameplayProbe: seq=" + SEQ.incrementAndGet()
                        + " event=gameplay-speedup mult=" + value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static long safeCampaignTimestamp() {
        try {
            if (Global.getSector() == null || Global.getSector().getClock() == null) return Long.MIN_VALUE;
            return Global.getSector().getClock().getTimestamp();
        } catch (Throwable ignored) {
            return Long.MIN_VALUE;
        }
    }

    private static float safeElapsedDaysSince(long timestamp) {
        try {
            if (timestamp == Long.MIN_VALUE || Global.getSector() == null || Global.getSector().getClock() == null) return -1f;
            return Global.getSector().getClock().getElapsedDaysSince(timestamp);
        } catch (Throwable ignored) {
            return -1f;
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
