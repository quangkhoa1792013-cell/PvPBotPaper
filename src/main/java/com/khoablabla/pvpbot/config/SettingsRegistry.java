// Phase 4: Unified Configuration Framework — Dual-Mode Settings Engine
package com.khoablabla.pvpbot.config;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsRegistry {

    private static SettingsRegistry instance;

    private final JavaPlugin plugin;
    private final Map<String, SettingMeta<?>> registry;
    private final Map<String, Object> globalCache;

    public record SettingMeta<T>(String key, Class<T> type, T defaultValue, Number min, Number max) {
    }

    private SettingsRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registry = new LinkedHashMap<>();
        this.globalCache = new HashMap<>();
        registerDefaults();
        reloadFromConfig();
    }

    public static synchronized void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new SettingsRegistry(plugin);
        }
    }

    public static SettingsRegistry getInstance() {
        return instance;
    }

    public SettingMeta<?> getMeta(String key) {
        return registry.get(key);
    }

    public Map<String, SettingMeta<?>> getAllMeta() {
        return Collections.unmodifiableMap(registry);
    }

    @SuppressWarnings("unchecked")
    public <T> T getForNpc(NPC npc, String key, Class<T> type) {
        SettingMeta<?> meta = registry.get(key);
        if (meta == null) return null;

        PvPBotTrait trait = npc.getTraitNullable(PvPBotTrait.class);
        if (trait != null) {
            Object localVal = trait.getLocalSettings().get(key);
            if (localVal != null) {
                return (T) localVal;
            }
        }

        return (T) globalCache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getGlobal(String key, Class<T> type) {
        SettingMeta<?> meta = registry.get(key);
        if (meta == null) return null;
        return (T) globalCache.get(key);
    }

    public <T> void setGlobal(String key, T value) {
        SettingMeta<?> meta = registry.get(key);
        if (meta == null) return;
        if (meta.type() == Integer.class) {
            int v = ((Number) value).intValue();
            globalCache.put(key, clampInt(v, (Integer) meta.min(), (Integer) meta.max()));
        } else if (meta.type() == Double.class) {
            double v = ((Number) value).doubleValue();
            globalCache.put(key, clampDouble(v, (Double) meta.min(), (Double) meta.max()));
        } else {
            globalCache.put(key, value);
        }
    }

    public void saveToConfig() {
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<String, Object> entry : globalCache.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        plugin.saveConfig();
    }

    public void reloadFromConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<String, SettingMeta<?>> entry : registry.entrySet()) {
            String key = entry.getKey();
            SettingMeta<?> meta = entry.getValue();
            if (config.contains(key)) {
                Object raw = config.get(key);
                Object validated = coerceToType(raw, meta);
                globalCache.put(key, validated);
            } else {
                globalCache.put(key, meta.defaultValue());
            }
        }
    }

    public Object parseValue(String key, String raw) {
        SettingMeta<?> meta = registry.get(key);
        if (meta == null) return null;
        try {
            if (meta.type() == Boolean.class) {
                return Boolean.parseBoolean(raw);
            } else if (meta.type() == Integer.class) {
                int val = Integer.parseInt(raw.trim());
                return clampInt(val, (Integer) meta.min(), (Integer) meta.max());
            } else if (meta.type() == Double.class) {
                double val = Double.parseDouble(raw.trim());
                return clampDouble(val, (Double) meta.min(), (Double) meta.max());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    public Object validateAndCast(Object raw, SettingMeta<?> meta) {
        return coerceToType(raw, meta);
    }

    @SuppressWarnings("unchecked")
    public <T> T coerce(Object raw, Class<T> type) {
        if (type == Integer.class && raw instanceof Number n) return (T) Integer.valueOf(n.intValue());
        if (type == Double.class && raw instanceof Number n) return (T) Double.valueOf(n.doubleValue());
        if (type == Boolean.class && raw instanceof Boolean b) return (T) b;
        if (type.isInstance(raw)) return (T) raw;
        return null;
    }

    private void registerDefaults() {
        reg("combat",               Boolean.class,  true);
        reg("revenge",              Boolean.class,  true);
        reg("auto-target",          Boolean.class,  true);
        reg("target-players",       Boolean.class,  true);
        reg("target-mobs",          Boolean.class,  false);
        reg("target-bots",          Boolean.class,  false);
        reg("criticals",            Boolean.class,  true);
        reg("attack-cooldown",      Integer.class,  8,    1, 40);
        reg("melee-range",          Double.class,   3.5,  2.0, 6.0);
        reg("move-speed",           Double.class,   1.45, 0.1, 2.0);
        reg("view-distance",        Double.class,   256.0, 5.0, 512.0);
        reg("retreat",              Boolean.class,  false);
        reg("friendly-fire",        Boolean.class,  false);
        reg("attack-invincible",    Boolean.class,  false);
        reg("safe-spawn",           Boolean.class,  true);
        reg("clear-on-remove",      Boolean.class,  true);
        reg("bot-leave-on-death",   Boolean.class,  true);
        reg("crit-fall-ticks",      Integer.class,  12,   1, 40);

        reg("ranged",               Boolean.class,  false);
        reg("ranged-min-range",     Double.class,   5.0,  1.0, 20.0);
        reg("ranged-optimal-range", Double.class,   15.0, 1.0, 50.0);
        reg("ranged-max-range",     Double.class,   50.0, 1.0, 128.0);
        reg("bow-draw-ticks",       Integer.class,  20,   5, 60);
        reg("arrow-prediction",     Boolean.class,  true);
        reg("ranged-strafe",        Boolean.class,  true);
        reg("ranged-retreat",       Boolean.class,  false);

        reg("auto-shield",          Boolean.class,  true);
        reg("shield-break",         Boolean.class,  true);
        reg("shield-break-chance",  Integer.class,  30,   0, 100);
        reg("shield-hold-ticks",    Integer.class,  80,   10, 200);
        reg("shield-raise-ticks",   Integer.class,  10,   2, 40);
        reg("shield-mace",          Boolean.class,  true);
        reg("prefer-sword",         Boolean.class,  true);
        reg("auto-totem",           Boolean.class,  true);
        reg("totem-priority",       Boolean.class,  false);

        reg("auto-armor",           Boolean.class,  true);
        reg("auto-weapon",          Boolean.class,  true);
        reg("drop-armor",           Boolean.class,  true);
        reg("drop-weapon",          Boolean.class,  true);
        reg("drop-distance",        Integer.class,  3,    1, 10);
        reg("interval",             Integer.class,  5,    1, 100);
        reg("auto-eat",             Boolean.class,  true);
        reg("auto-potion",          Boolean.class,  true);
        reg("auto-mend",            Boolean.class,  true);

        reg("miss-chance",          Integer.class,  5,    0, 100);
        reg("mistake-chance",       Integer.class,  3,    0, 100);
        reg("aim-speed",            Integer.class,  18,   3, 45);
        reg("special-names",        Boolean.class,  false);
        reg("max-mass-spawn",       Integer.class,  50,   50, 10000);
        reg("profile-lagg-fix",     Boolean.class,  true);
        reg("bhop",                 Boolean.class,  false);
        reg("idle",                 Boolean.class,  true);
        reg("idle-radius",          Integer.class,  10,   3, 50);
        reg("mace",                 Boolean.class,  false);
    }

    private <T> void reg(String key, Class<T> type, T def) {
        registry.put(key, new SettingMeta<>(key, type, def, null, null));
    }

    private <T extends Number> void reg(String key, Class<T> type, T def, T min, T max) {
        registry.put(key, new SettingMeta<>(key, type, def, min, max));
    }

    private Object coerceToType(Object raw, SettingMeta<?> meta) {
        if (raw == null) return meta.defaultValue();
        try {
            if (meta.type() == Integer.class) {
                int val = ((Number) raw).intValue();
                if (meta.min() != null && meta.max() != null) {
                    return clampInt(val, (Integer) meta.min(), (Integer) meta.max());
                }
                return val;
            } else if (meta.type() == Double.class) {
                double val = ((Number) raw).doubleValue();
                if (meta.min() != null && meta.max() != null) {
                    return clampDouble(val, (Double) meta.min(), (Double) meta.max());
                }
                return val;
            } else if (meta.type() == Boolean.class && raw instanceof Boolean b) {
                return b;
            }
        } catch (ClassCastException e) {
            return meta.defaultValue();
        }
        return meta.defaultValue();
    }

    private static int clampInt(int value, int min, int max) {
        if (min > max) return value;
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        if (min > max) return value;
        return Math.max(min, Math.min(max, value));
    }
}
