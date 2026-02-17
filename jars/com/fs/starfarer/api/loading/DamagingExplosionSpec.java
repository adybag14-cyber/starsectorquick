package com.fs.starfarer.api.loading;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Compatibility shim for CheerpJ runs where this API class fails to link at runtime.
public class DamagingExplosionSpec implements Cloneable {
    private OnHitEffectPlugin effect;
    private String soundSetId;
    private float soundVolume = 1f;

    private float duration;
    private float radius;
    private float coreRadius;
    private float maxDamage;
    private float minDamage;
    private float maxEMPDamage;
    private float minEMPDamage;

    private CollisionClass collisionClass;
    private CollisionClass collisionClassByFighter;

    private float particleSpawnRadius;
    private float particleSizeMin;
    private float particleSizeRange;
    private float particleDuration;
    private int particleCount;
    private Color particleColor;
    private Color explosionColor;

    private boolean showGraphic = true;
    private boolean useDetailedExplosion;
    private float detailedExplosionRadius;
    private float detailedExplosionFlashRadius;
    private Color detailedExplosionFlashColorFringe;
    private Color detailedExplosionFlashColorCore;
    private float detailedExplosionFlashDuration;

    private DamageType damageType = DamageType.HIGH_EXPLOSIVE;
    private MutableStat modifier = new MutableStat(1f);

    public static float getShipExplosionRadius(ShipAPI ship) {
        if (ship == null) {
            return 0f;
        }
        try {
            return Math.max(0f, ship.getCollisionRadius());
        } catch (Throwable t) {
            return 0f;
        }
    }

    public static DamagingExplosionSpec explosionSpecForShip(ShipAPI ship) {
        float r = getShipExplosionRadius(ship);
        if (r <= 0f) {
            r = 50f;
        }
        DamagingExplosionSpec spec =
                new DamagingExplosionSpec(
                        0.2f,
                        r,
                        r * 0.65f,
                        1000f,
                        500f,
                        CollisionClass.PROJECTILE_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        4f,
                        8f,
                        1.2f,
                        60,
                        new Color(255, 180, 80, 220),
                        new Color(255, 120, 60, 255));
        spec.setDamageType(DamageType.HIGH_EXPLOSIVE);
        return spec;
    }

    public static DamagingExplosionSpec loadFromJSON(JSONObject json) throws JSONException {
        if (json == null) {
            return null;
        }

        float duration = getFloat(json, "duration", 0.1f);
        float radius = getFloat(json, "radius", 0f);
        float coreRadius = getFloat(json, "coreRadius", radius * 0.5f);
        float maxDamage = getFloat(json, "maxDamage", 0f);
        float minDamage = getFloat(json, "minDamage", maxDamage);

        CollisionClass collisionClass =
                parseCollisionClass(json.opt("collisionClass"), CollisionClass.PROJECTILE_FF);
        CollisionClass collisionClassByFighter =
                parseCollisionClass(
                        json.opt("collisionClassByFighter"), CollisionClass.PROJECTILE_FIGHTER);

        float particleSizeMin = getFloat(json, "particleSizeMin", 4f);
        float particleSizeRange = getFloat(json, "particleSizeRange", 8f);
        float particleDuration = getFloat(json, "particleDuration", 1f);
        int particleCount = getInt(json, "particleCount", 40);
        Color particleColor = parseColor(json.opt("particleColor"), new Color(255, 255, 255, 255));
        Color explosionColor =
                parseColor(json.opt("explosionColor"), new Color(255, 180, 80, 255));

        DamagingExplosionSpec spec =
                new DamagingExplosionSpec(
                        duration,
                        radius,
                        coreRadius,
                        maxDamage,
                        minDamage,
                        collisionClass,
                        collisionClassByFighter,
                        particleSizeMin,
                        particleSizeRange,
                        particleDuration,
                        particleCount,
                        particleColor,
                        explosionColor);

        spec.setMaxEMPDamage(getFloat(json, "maxEmpDamage", getFloat(json, "maxEMPDamage", 0f)));
        spec.setMinEMPDamage(getFloat(json, "minEmpDamage", getFloat(json, "minEMPDamage", 0f)));
        spec.setParticleSpawnRadius(getFloat(json, "particleSpawnRadius", radius));
        spec.setShowGraphic(getBoolean(json, "showGraphic", true));
        spec.setUseDetailedExplosion(getBoolean(json, "useDetailedExplosion", false));
        spec.setDetailedExplosionRadius(getFloat(json, "detailedExplosionRadius", radius));
        spec.setDetailedExplosionFlashRadius(getFloat(json, "detailedExplosionFlashRadius", radius));
        spec.setDetailedExplosionFlashDuration(
                getFloat(json, "detailedExplosionFlashDuration", 0.3f));
        spec.setDetailedExplosionFlashColorFringe(
                parseColor(
                        json.opt("detailedExplosionFlashColorFringe"),
                        new Color(255, 200, 120, 255)));
        spec.setDetailedExplosionFlashColorCore(
                parseColor(
                        json.opt("detailedExplosionFlashColorCore"),
                        new Color(255, 255, 255, 255)));
        spec.setDamageType(parseDamageType(json.opt("damageType"), DamageType.HIGH_EXPLOSIVE));

        Object sound = json.opt("sound");
        if (sound != null) {
            spec.setSoundSetId(String.valueOf(sound));
        }
        spec.setSoundVolume(getFloat(json, "soundVolume", 1f));

        return spec;
    }

    public DamagingExplosionSpec(
            float duration,
            float radius,
            float coreRadius,
            float maxDamage,
            float minDamage,
            CollisionClass collisionClass,
            CollisionClass collisionClassByFighter,
            float particleSizeMin,
            float particleSizeRange,
            float particleDuration,
            int particleCount,
            Color particleColor,
            Color explosionColor) {
        this.duration = duration;
        this.radius = radius;
        this.coreRadius = coreRadius;
        this.maxDamage = maxDamage;
        this.minDamage = minDamage;
        this.collisionClass = collisionClass;
        this.collisionClassByFighter = collisionClassByFighter;
        this.particleSizeMin = particleSizeMin;
        this.particleSizeRange = particleSizeRange;
        this.particleDuration = particleDuration;
        this.particleCount = particleCount;
        this.particleColor = particleColor;
        this.explosionColor = explosionColor;
        this.particleSpawnRadius = radius;
        this.detailedExplosionFlashRadius = radius;
        this.detailedExplosionRadius = radius;
    }

    public OnHitEffectPlugin getEffect() {
        return effect;
    }

    public void setEffect(OnHitEffectPlugin effect) {
        this.effect = effect;
    }

    public String getSoundSetId() {
        return soundSetId;
    }

    public void setSoundSetId(String soundSetId) {
        this.soundSetId = soundSetId;
    }

    public float getParticleSpawnRadius() {
        return particleSpawnRadius;
    }

    public void setParticleSpawnRadius(float particleSpawnRadius) {
        this.particleSpawnRadius = particleSpawnRadius;
    }

    public Color getExplosionColor() {
        return explosionColor;
    }

    public void setExplosionColor(Color explosionColor) {
        this.explosionColor = explosionColor;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public void setDamageType(DamageType damageType) {
        this.damageType = damageType;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getMaxDamage() {
        return maxDamage;
    }

    public void setMaxDamage(float maxDamage) {
        this.maxDamage = maxDamage;
    }

    public float getMinDamage() {
        return minDamage;
    }

    public void setMinDamage(float minDamage) {
        this.minDamage = minDamage;
    }

    public CollisionClass getCollisionClass() {
        return collisionClass;
    }

    public void setCollisionClass(CollisionClass collisionClass) {
        this.collisionClass = collisionClass;
    }

    public CollisionClass getCollisionClassIfByFighter() {
        return collisionClassByFighter;
    }

    public void setCollisionClassByFighter(CollisionClass collisionClassByFighter) {
        this.collisionClassByFighter = collisionClassByFighter;
    }

    public float getParticleSizeMin() {
        return particleSizeMin;
    }

    public void setParticleSizeMin(float particleSizeMin) {
        this.particleSizeMin = particleSizeMin;
    }

    public float getParticleSizeRange() {
        return particleSizeRange;
    }

    public void setParticleSizeRange(float particleSizeRange) {
        this.particleSizeRange = particleSizeRange;
    }

    public float getParticleDuration() {
        return particleDuration;
    }

    public void setParticleDuration(float particleDuration) {
        this.particleDuration = particleDuration;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public void setParticleCount(int particleCount) {
        this.particleCount = particleCount;
    }

    public Color getParticleColor() {
        return particleColor;
    }

    public void setParticleColor(Color particleColor) {
        this.particleColor = particleColor;
    }

    public float getCoreRadius() {
        return coreRadius;
    }

    public void setCoreRadius(float coreRadius) {
        this.coreRadius = coreRadius;
    }

    public boolean isShowGraphic() {
        return showGraphic;
    }

    public void setShowGraphic(boolean showGraphic) {
        this.showGraphic = showGraphic;
    }

    public DamagingExplosionSpec clone() {
        try {
            DamagingExplosionSpec copy = (DamagingExplosionSpec) super.clone();
            if (modifier != null) {
                copy.modifier = modifier.createCopy();
            }
            return copy;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isUseDetailedExplosion() {
        return useDetailedExplosion;
    }

    public void setUseDetailedExplosion(boolean useDetailedExplosion) {
        this.useDetailedExplosion = useDetailedExplosion;
    }

    public CollisionClass getCollisionClassByFighter() {
        return collisionClassByFighter;
    }

    public MutableStat getModifier() {
        if (modifier == null) {
            modifier = new MutableStat(1f);
        }
        return modifier;
    }

    public void setModifier(MutableStat modifier) {
        this.modifier = modifier;
    }

    public float getDetailedExplosionRadius() {
        return detailedExplosionRadius;
    }

    public void setDetailedExplosionRadius(float detailedExplosionRadius) {
        this.detailedExplosionRadius = detailedExplosionRadius;
    }

    public float getDetailedExplosionFlashRadius() {
        return detailedExplosionFlashRadius;
    }

    public void setDetailedExplosionFlashRadius(float detailedExplosionFlashRadius) {
        this.detailedExplosionFlashRadius = detailedExplosionFlashRadius;
    }

    public Color getDetailedExplosionFlashColorFringe() {
        return detailedExplosionFlashColorFringe;
    }

    public void setDetailedExplosionFlashColorFringe(Color detailedExplosionFlashColorFringe) {
        this.detailedExplosionFlashColorFringe = detailedExplosionFlashColorFringe;
    }

    public Color getDetailedExplosionFlashColorCore() {
        return detailedExplosionFlashColorCore;
    }

    public void setDetailedExplosionFlashColorCore(Color detailedExplosionFlashColorCore) {
        this.detailedExplosionFlashColorCore = detailedExplosionFlashColorCore;
    }

    public float getDetailedExplosionFlashDuration() {
        return detailedExplosionFlashDuration;
    }

    public void setDetailedExplosionFlashDuration(float detailedExplosionFlashDuration) {
        this.detailedExplosionFlashDuration = detailedExplosionFlashDuration;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
    }

    public float getMaxEMPDamage() {
        return maxEMPDamage;
    }

    public void setMaxEMPDamage(float maxEMPDamage) {
        this.maxEMPDamage = maxEMPDamage;
    }

    public float getMinEMPDamage() {
        return minEMPDamage;
    }

    public void setMinEMPDamage(float minEMPDamage) {
        this.minEMPDamage = minEMPDamage;
    }

    private static float getFloat(JSONObject json, String key, float defaultValue) {
        Object raw = json.opt(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number) {
            return ((Number) raw).floatValue();
        }
        String text = String.valueOf(raw).trim();
        if (text.endsWith("f") || text.endsWith("F")) {
            text = text.substring(0, text.length() - 1);
        }
        try {
            return Float.parseFloat(text);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    private static int getInt(JSONObject json, String key, int defaultValue) {
        Object raw = json.opt(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    private static boolean getBoolean(JSONObject json, String key, boolean defaultValue) {
        Object raw = json.opt(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Boolean) {
            return ((Boolean) raw).booleanValue();
        }
        String text = String.valueOf(raw).trim().toLowerCase();
        if ("true".equals(text)) {
            return true;
        }
        if ("false".equals(text)) {
            return false;
        }
        return defaultValue;
    }

    private static Color parseColor(Object raw, Color defaultValue) {
        if (!(raw instanceof JSONArray)) {
            return defaultValue;
        }
        JSONArray array = (JSONArray) raw;
        int r = array.optInt(0, defaultValue.getRed());
        int g = array.optInt(1, defaultValue.getGreen());
        int b = array.optInt(2, defaultValue.getBlue());
        int a = array.optInt(3, defaultValue.getAlpha());
        return new Color(clamp255(r), clamp255(g), clamp255(b), clamp255(a));
    }

    private static int clamp255(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > 255) {
            return 255;
        }
        return v;
    }

    private static CollisionClass parseCollisionClass(Object raw, CollisionClass defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        try {
            return CollisionClass.valueOf(String.valueOf(raw).trim());
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    private static DamageType parseDamageType(Object raw, DamageType defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        try {
            return DamageType.valueOf(String.valueOf(raw).trim());
        } catch (Throwable t) {
            return defaultValue;
        }
    }
}
