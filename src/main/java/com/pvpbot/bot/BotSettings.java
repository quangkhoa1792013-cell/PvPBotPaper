package com.pvpbot.bot;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public class BotSettings {

    private double moveSpeed = 0.25;
    private boolean bhop = false;
    private boolean idle = false;
    private int idleRadius = 15;
    private int viewDistance = 32;
    private boolean retreat = false;
    private boolean combat = true;
    private boolean revenge = true;
    private boolean autoTarget = true;
    private boolean criticals = true;
    private boolean targetPlayers = true;
    private boolean targetMobs = false;
    private boolean targetBots = true;
    private int attackCooldown = 12;
    private double meleeRange = 3.0;
    private boolean preferSword = true;
    private boolean ranged = true;
    private boolean mace = true;
    private double rangedMinRange = 3.0;
    private double rangedOptimalRange = 10.0;
    private double rangedMaxRange = 20.0;
    private int bowDrawTicks = 20;
    private boolean arrowPrediction = true;
    private boolean rangedStrafe = true;
    private boolean rangedRetreat = true;
    private boolean autoShield = true;
    private boolean shieldBreak = true;
    private int shieldBreakChance = 30;
    private int shieldHoldTicks = 60;
    private int shieldRaiseTicks = 5;
    private boolean shieldMace = true;
    private boolean autoArmor = true;
    private boolean autoWeapon = true;
    private boolean autoEat = true;
    private boolean autoPotion = true;
    private boolean autoMend = true;
    private boolean autoTotem = true;
    private boolean totemPriority = false;
    private int missChance = 5;
    private int mistakeChance = 5;
    private double aimSpeed = 15.0;
    private boolean showInTab = true;
    private boolean botLeaveOnDeath = false;
    private boolean debug = false;

    private final Object lock = new Object();

    public static final String[] SETTING_KEYS = {
        "move-speed", "bhop", "idle", "idle-radius", "view-distance", "retreat",
        "combat", "revenge", "auto-target", "criticals",
        "target-players", "target-mobs", "target-bots",
        "attack-cooldown", "melee-range", "prefer-sword",
        "ranged", "mace", "ranged-min-range", "ranged-optimal-range", "ranged-max-range",
        "bow-draw-ticks", "arrow-prediction", "ranged-strafe", "ranged-retreat",
        "auto-shield", "shield-break", "shield-break-chance", "shield-hold-ticks",
        "shield-raise-ticks", "shield-mace",
        "auto-armor", "auto-weapon", "auto-eat", "auto-potion", "auto-mend",
        "auto-totem", "totem-priority",
        "miss-chance", "mistake-chance", "aim-speed",
        "show-in-tab", "bot-leave-on-death", "debug"
    };

    private static final Map<String, SettingType> SETTING_TYPES = new LinkedHashMap<>();

    static {
        for (String key : SETTING_KEYS) {
            SETTING_TYPES.put(key, null);
        }
        SETTING_TYPES.put("move-speed", SettingType.DOUBLE);
        SETTING_TYPES.put("bhop", SettingType.BOOLEAN);
        SETTING_TYPES.put("idle", SettingType.BOOLEAN);
        SETTING_TYPES.put("idle-radius", SettingType.INTEGER);
        SETTING_TYPES.put("view-distance", SettingType.INTEGER);
        SETTING_TYPES.put("retreat", SettingType.BOOLEAN);
        SETTING_TYPES.put("combat", SettingType.BOOLEAN);
        SETTING_TYPES.put("revenge", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-target", SettingType.BOOLEAN);
        SETTING_TYPES.put("criticals", SettingType.BOOLEAN);
        SETTING_TYPES.put("target-players", SettingType.BOOLEAN);
        SETTING_TYPES.put("target-mobs", SettingType.BOOLEAN);
        SETTING_TYPES.put("target-bots", SettingType.BOOLEAN);
        SETTING_TYPES.put("attack-cooldown", SettingType.INTEGER);
        SETTING_TYPES.put("melee-range", SettingType.DOUBLE);
        SETTING_TYPES.put("prefer-sword", SettingType.BOOLEAN);
        SETTING_TYPES.put("ranged", SettingType.BOOLEAN);
        SETTING_TYPES.put("mace", SettingType.BOOLEAN);
        SETTING_TYPES.put("ranged-min-range", SettingType.DOUBLE);
        SETTING_TYPES.put("ranged-optimal-range", SettingType.DOUBLE);
        SETTING_TYPES.put("ranged-max-range", SettingType.DOUBLE);
        SETTING_TYPES.put("bow-draw-ticks", SettingType.INTEGER);
        SETTING_TYPES.put("arrow-prediction", SettingType.BOOLEAN);
        SETTING_TYPES.put("ranged-strafe", SettingType.BOOLEAN);
        SETTING_TYPES.put("ranged-retreat", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-shield", SettingType.BOOLEAN);
        SETTING_TYPES.put("shield-break", SettingType.BOOLEAN);
        SETTING_TYPES.put("shield-break-chance", SettingType.INTEGER);
        SETTING_TYPES.put("shield-hold-ticks", SettingType.INTEGER);
        SETTING_TYPES.put("shield-raise-ticks", SettingType.INTEGER);
        SETTING_TYPES.put("shield-mace", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-armor", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-weapon", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-eat", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-potion", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-mend", SettingType.BOOLEAN);
        SETTING_TYPES.put("auto-totem", SettingType.BOOLEAN);
        SETTING_TYPES.put("totem-priority", SettingType.BOOLEAN);
        SETTING_TYPES.put("miss-chance", SettingType.INTEGER);
        SETTING_TYPES.put("mistake-chance", SettingType.INTEGER);
        SETTING_TYPES.put("aim-speed", SettingType.DOUBLE);
        SETTING_TYPES.put("show-in-tab", SettingType.BOOLEAN);
        SETTING_TYPES.put("bot-leave-on-death", SettingType.BOOLEAN);
        SETTING_TYPES.put("debug", SettingType.BOOLEAN);
    }

    public enum SettingType {
        BOOLEAN, INTEGER, DOUBLE
    }

    public static SettingType getType(String key) {
        return SETTING_TYPES.get(key);
    }

    public Object getValue(String key) {
        switch (key) {
            case "move-speed": return getMoveSpeed();
            case "bhop": return isBhop();
            case "idle": return isIdle();
            case "idle-radius": return getIdleRadius();
            case "view-distance": return getViewDistance();
            case "retreat": return isRetreat();
            case "combat": return isCombat();
            case "revenge": return isRevenge();
            case "auto-target": return isAutoTarget();
            case "criticals": return isCriticals();
            case "target-players": return isTargetPlayers();
            case "target-mobs": return isTargetMobs();
            case "target-bots": return isTargetBots();
            case "attack-cooldown": return getAttackCooldown();
            case "melee-range": return getMeleeRange();
            case "prefer-sword": return isPreferSword();
            case "ranged": return isRanged();
            case "mace": return isMace();
            case "ranged-min-range": return getRangedMinRange();
            case "ranged-optimal-range": return getRangedOptimalRange();
            case "ranged-max-range": return getRangedMaxRange();
            case "bow-draw-ticks": return getBowDrawTicks();
            case "arrow-prediction": return isArrowPrediction();
            case "ranged-strafe": return isRangedStrafe();
            case "ranged-retreat": return isRangedRetreat();
            case "auto-shield": return isAutoShield();
            case "shield-break": return isShieldBreak();
            case "shield-break-chance": return getShieldBreakChance();
            case "shield-hold-ticks": return getShieldHoldTicks();
            case "shield-raise-ticks": return getShieldRaiseTicks();
            case "shield-mace": return isShieldMace();
            case "auto-armor": return isAutoArmor();
            case "auto-weapon": return isAutoWeapon();
            case "auto-eat": return isAutoEat();
            case "auto-potion": return isAutoPotion();
            case "auto-mend": return isAutoMend();
            case "auto-totem": return isAutoTotem();
            case "totem-priority": return isTotemPriority();
            case "miss-chance": return getMissChance();
            case "mistake-chance": return getMistakeChance();
            case "aim-speed": return getAimSpeed();
            case "show-in-tab": return isShowInTab();
            case "bot-leave-on-death": return isBotLeaveOnDeath();
            case "debug": return isDebug();
            default: return null;
        }
    }

    public void setValue(String key, Object value) {
        switch (key) {
            case "move-speed": setMoveSpeed(asDouble(value, 0.25)); break;
            case "bhop": setBhop(asBoolean(value, false)); break;
            case "idle": setIdle(asBoolean(value, false)); break;
            case "idle-radius": setIdleRadius(asInt(value, 15)); break;
            case "view-distance": setViewDistance(asInt(value, 32)); break;
            case "retreat": setRetreat(asBoolean(value, false)); break;
            case "combat": setCombat(asBoolean(value, true)); break;
            case "revenge": setRevenge(asBoolean(value, true)); break;
            case "auto-target": setAutoTarget(asBoolean(value, true)); break;
            case "criticals": setCriticals(asBoolean(value, true)); break;
            case "target-players": setTargetPlayers(asBoolean(value, true)); break;
            case "target-mobs": setTargetMobs(asBoolean(value, false)); break;
            case "target-bots": setTargetBots(asBoolean(value, true)); break;
            case "attack-cooldown": setAttackCooldown(asInt(value, 12)); break;
            case "melee-range": setMeleeRange(asDouble(value, 3.0)); break;
            case "prefer-sword": setPreferSword(asBoolean(value, true)); break;
            case "ranged": setRanged(asBoolean(value, true)); break;
            case "mace": setMace(asBoolean(value, true)); break;
            case "ranged-min-range": setRangedMinRange(asDouble(value, 3.0)); break;
            case "ranged-optimal-range": setRangedOptimalRange(asDouble(value, 10.0)); break;
            case "ranged-max-range": setRangedMaxRange(asDouble(value, 20.0)); break;
            case "bow-draw-ticks": setBowDrawTicks(asInt(value, 20)); break;
            case "arrow-prediction": setArrowPrediction(asBoolean(value, true)); break;
            case "ranged-strafe": setRangedStrafe(asBoolean(value, true)); break;
            case "ranged-retreat": setRangedRetreat(asBoolean(value, true)); break;
            case "auto-shield": setAutoShield(asBoolean(value, true)); break;
            case "shield-break": setShieldBreak(asBoolean(value, true)); break;
            case "shield-break-chance": setShieldBreakChance(asInt(value, 30)); break;
            case "shield-hold-ticks": setShieldHoldTicks(asInt(value, 60)); break;
            case "shield-raise-ticks": setShieldRaiseTicks(asInt(value, 5)); break;
            case "shield-mace": setShieldMace(asBoolean(value, true)); break;
            case "auto-armor": setAutoArmor(asBoolean(value, true)); break;
            case "auto-weapon": setAutoWeapon(asBoolean(value, true)); break;
            case "auto-eat": setAutoEat(asBoolean(value, true)); break;
            case "auto-potion": setAutoPotion(asBoolean(value, true)); break;
            case "auto-mend": setAutoMend(asBoolean(value, true)); break;
            case "auto-totem": setAutoTotem(asBoolean(value, true)); break;
            case "totem-priority": setTotemPriority(asBoolean(value, false)); break;
            case "miss-chance": setMissChance(asInt(value, 5)); break;
            case "mistake-chance": setMistakeChance(asInt(value, 5)); break;
            case "aim-speed": setAimSpeed(asDouble(value, 15.0)); break;
            case "show-in-tab": setShowInTab(asBoolean(value, true)); break;
            case "bot-leave-on-death": setBotLeaveOnDeath(asBoolean(value, false)); break;
            case "debug": setDebug(asBoolean(value, false)); break;
        }
    }

    private double asDouble(Object obj, double def) {
        if (obj instanceof Number num) return num.doubleValue();
        if (obj instanceof String str) {
            try { return Double.parseDouble(str); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private int asInt(Object obj, int def) {
        if (obj instanceof Number num) return num.intValue();
        if (obj instanceof String str) {
            try { return Integer.parseInt(str); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private boolean asBoolean(Object obj, boolean def) {
        if (obj instanceof Boolean bool) return bool;
        if (obj instanceof String str) return Boolean.parseBoolean(str);
        return def;
    }

    public void loadFromConfig(FileConfiguration config) {
        String p = "bot-settings.";
        synchronized (lock) {
            moveSpeed = clamp(config.getDouble(p + "move-speed", 0.25), 0.0, 10.0);
            bhop = config.getBoolean(p + "bhop", false);
            idle = config.getBoolean(p + "idle", false);
            idleRadius = clamp(config.getInt(p + "idle-radius", 15), 0, 100);
            viewDistance = clamp(config.getInt(p + "view-distance", 32), 1, 128);
            retreat = config.getBoolean(p + "retreat", false);
            combat = config.getBoolean(p + "combat", true);
            revenge = config.getBoolean(p + "revenge", true);
            autoTarget = config.getBoolean(p + "auto-target", true);
            criticals = config.getBoolean(p + "criticals", true);
            targetPlayers = config.getBoolean(p + "target-players", true);
            targetMobs = config.getBoolean(p + "target-mobs", false);
            targetBots = config.getBoolean(p + "target-bots", true);
            attackCooldown = clamp(config.getInt(p + "attack-cooldown", 12), 0, 40);
            meleeRange = clamp(config.getDouble(p + "melee-range", 3.0), 1.0, 8.0);
            preferSword = config.getBoolean(p + "prefer-sword", true);
            ranged = config.getBoolean(p + "ranged", true);
            mace = config.getBoolean(p + "mace", true);
            rangedMinRange = clamp(config.getDouble(p + "ranged-min-range", 3.0), 1.0, 50.0);
            rangedOptimalRange = clamp(config.getDouble(p + "ranged-optimal-range", 10.0), 1.0, 50.0);
            rangedMaxRange = clamp(config.getDouble(p + "ranged-max-range", 20.0), 1.0, 50.0);
            bowDrawTicks = clamp(config.getInt(p + "bow-draw-ticks", 20), 0, 100);
            arrowPrediction = config.getBoolean(p + "arrow-prediction", true);
            rangedStrafe = config.getBoolean(p + "ranged-strafe", true);
            rangedRetreat = config.getBoolean(p + "ranged-retreat", true);
            autoShield = config.getBoolean(p + "auto-shield", true);
            shieldBreak = config.getBoolean(p + "shield-break", true);
            shieldBreakChance = clamp(config.getInt(p + "shield-break-chance", 30), 0, 100);
            shieldHoldTicks = clamp(config.getInt(p + "shield-hold-ticks", 60), 1, 200);
            shieldRaiseTicks = clamp(config.getInt(p + "shield-raise-ticks", 5), 1, 40);
            shieldMace = config.getBoolean(p + "shield-mace", true);
            autoArmor = config.getBoolean(p + "auto-armor", true);
            autoWeapon = config.getBoolean(p + "auto-weapon", true);
            autoEat = config.getBoolean(p + "auto-eat", true);
            autoPotion = config.getBoolean(p + "auto-potion", true);
            autoMend = config.getBoolean(p + "auto-mend", true);
            autoTotem = config.getBoolean(p + "auto-totem", true);
            totemPriority = config.getBoolean(p + "totem-priority", false);
            missChance = clamp(config.getInt(p + "miss-chance", 5), 0, 100);
            mistakeChance = clamp(config.getInt(p + "mistake-chance", 5), 0, 100);
            aimSpeed = clamp(config.getDouble(p + "aim-speed", 15.0), 0.0, 100.0);
            showInTab = config.getBoolean(p + "show-in-tab", true);
            botLeaveOnDeath = config.getBoolean(p + "bot-leave-on-death", false);
            debug = config.getBoolean(p + "debug", false);
        }
    }

    public void saveToConfig(FileConfiguration config) {
        String p = "bot-settings.";
        synchronized (lock) {
            config.set(p + "move-speed", moveSpeed);
            config.set(p + "bhop", bhop);
            config.set(p + "idle", idle);
            config.set(p + "idle-radius", idleRadius);
            config.set(p + "view-distance", viewDistance);
            config.set(p + "retreat", retreat);
            config.set(p + "combat", combat);
            config.set(p + "revenge", revenge);
            config.set(p + "auto-target", autoTarget);
            config.set(p + "criticals", criticals);
            config.set(p + "target-players", targetPlayers);
            config.set(p + "target-mobs", targetMobs);
            config.set(p + "target-bots", targetBots);
            config.set(p + "attack-cooldown", attackCooldown);
            config.set(p + "melee-range", meleeRange);
            config.set(p + "prefer-sword", preferSword);
            config.set(p + "ranged", ranged);
            config.set(p + "mace", mace);
            config.set(p + "ranged-min-range", rangedMinRange);
            config.set(p + "ranged-optimal-range", rangedOptimalRange);
            config.set(p + "ranged-max-range", rangedMaxRange);
            config.set(p + "bow-draw-ticks", bowDrawTicks);
            config.set(p + "arrow-prediction", arrowPrediction);
            config.set(p + "ranged-strafe", rangedStrafe);
            config.set(p + "ranged-retreat", rangedRetreat);
            config.set(p + "auto-shield", autoShield);
            config.set(p + "shield-break", shieldBreak);
            config.set(p + "shield-break-chance", shieldBreakChance);
            config.set(p + "shield-hold-ticks", shieldHoldTicks);
            config.set(p + "shield-raise-ticks", shieldRaiseTicks);
            config.set(p + "shield-mace", shieldMace);
            config.set(p + "auto-armor", autoArmor);
            config.set(p + "auto-weapon", autoWeapon);
            config.set(p + "auto-eat", autoEat);
            config.set(p + "auto-potion", autoPotion);
            config.set(p + "auto-mend", autoMend);
            config.set(p + "auto-totem", autoTotem);
            config.set(p + "totem-priority", totemPriority);
            config.set(p + "miss-chance", missChance);
            config.set(p + "mistake-chance", mistakeChance);
            config.set(p + "aim-speed", aimSpeed);
            config.set(p + "show-in-tab", showInTab);
            config.set(p + "bot-leave-on-death", botLeaveOnDeath);
            config.set(p + "debug", debug);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public double getMoveSpeed() { synchronized (lock) { return moveSpeed; } }
    public boolean isBhop() { synchronized (lock) { return bhop; } }
    public boolean isIdle() { synchronized (lock) { return idle; } }
    public int getIdleRadius() { synchronized (lock) { return idleRadius; } }
    public int getViewDistance() { synchronized (lock) { return viewDistance; } }
    public boolean isRetreat() { synchronized (lock) { return retreat; } }
    public boolean isCombat() { synchronized (lock) { return combat; } }
    public boolean isRevenge() { synchronized (lock) { return revenge; } }
    public boolean isAutoTarget() { synchronized (lock) { return autoTarget; } }
    public boolean isCriticals() { synchronized (lock) { return criticals; } }
    public boolean isTargetPlayers() { synchronized (lock) { return targetPlayers; } }
    public boolean isTargetMobs() { synchronized (lock) { return targetMobs; } }
    public boolean isTargetBots() { synchronized (lock) { return targetBots; } }
    public int getAttackCooldown() { synchronized (lock) { return attackCooldown; } }
    public double getMeleeRange() { synchronized (lock) { return meleeRange; } }
    public boolean isPreferSword() { synchronized (lock) { return preferSword; } }
    public boolean isRanged() { synchronized (lock) { return ranged; } }
    public boolean isMace() { synchronized (lock) { return mace; } }
    public double getRangedMinRange() { synchronized (lock) { return rangedMinRange; } }
    public double getRangedOptimalRange() { synchronized (lock) { return rangedOptimalRange; } }
    public double getRangedMaxRange() { synchronized (lock) { return rangedMaxRange; } }
    public int getBowDrawTicks() { synchronized (lock) { return bowDrawTicks; } }
    public boolean isArrowPrediction() { synchronized (lock) { return arrowPrediction; } }
    public boolean isRangedStrafe() { synchronized (lock) { return rangedStrafe; } }
    public boolean isRangedRetreat() { synchronized (lock) { return rangedRetreat; } }
    public boolean isAutoShield() { synchronized (lock) { return autoShield; } }
    public boolean isShieldBreak() { synchronized (lock) { return shieldBreak; } }
    public int getShieldBreakChance() { synchronized (lock) { return shieldBreakChance; } }
    public int getShieldHoldTicks() { synchronized (lock) { return shieldHoldTicks; } }
    public int getShieldRaiseTicks() { synchronized (lock) { return shieldRaiseTicks; } }
    public boolean isShieldMace() { synchronized (lock) { return shieldMace; } }
    public boolean isAutoArmor() { synchronized (lock) { return autoArmor; } }
    public boolean isAutoWeapon() { synchronized (lock) { return autoWeapon; } }
    public boolean isAutoEat() { synchronized (lock) { return autoEat; } }
    public boolean isAutoPotion() { synchronized (lock) { return autoPotion; } }
    public boolean isAutoMend() { synchronized (lock) { return autoMend; } }
    public boolean isAutoTotem() { synchronized (lock) { return autoTotem; } }
    public boolean isTotemPriority() { synchronized (lock) { return totemPriority; } }
    public int getMissChance() { synchronized (lock) { return missChance; } }
    public int getMistakeChance() { synchronized (lock) { return mistakeChance; } }
    public double getAimSpeed() { synchronized (lock) { return aimSpeed; } }
    public boolean isShowInTab() { synchronized (lock) { return showInTab; } }
    public boolean isBotLeaveOnDeath() { synchronized (lock) { return botLeaveOnDeath; } }
    public boolean isDebug() { synchronized (lock) { return debug; } }

    public void setMoveSpeed(double value) { synchronized (lock) { this.moveSpeed = clamp(value, 0.0, 10.0); } }
    public void setBhop(boolean value) { synchronized (lock) { this.bhop = value; } }
    public void setIdle(boolean value) { synchronized (lock) { this.idle = value; } }
    public void setIdleRadius(int value) { synchronized (lock) { this.idleRadius = clamp(value, 0, 100); } }
    public void setViewDistance(int value) { synchronized (lock) { this.viewDistance = clamp(value, 1, 128); } }
    public void setRetreat(boolean value) { synchronized (lock) { this.retreat = value; } }
    public void setCombat(boolean value) { synchronized (lock) { this.combat = value; } }
    public void setRevenge(boolean value) { synchronized (lock) { this.revenge = value; } }
    public void setAutoTarget(boolean value) { synchronized (lock) { this.autoTarget = value; } }
    public void setCriticals(boolean value) { synchronized (lock) { this.criticals = value; } }
    public void setTargetPlayers(boolean value) { synchronized (lock) { this.targetPlayers = value; } }
    public void setTargetMobs(boolean value) { synchronized (lock) { this.targetMobs = value; } }
    public void setTargetBots(boolean value) { synchronized (lock) { this.targetBots = value; } }
    public void setAttackCooldown(int value) { synchronized (lock) { this.attackCooldown = clamp(value, 0, 40); } }
    public void setMeleeRange(double value) { synchronized (lock) { this.meleeRange = clamp(value, 1.0, 8.0); } }
    public void setPreferSword(boolean value) { synchronized (lock) { this.preferSword = value; } }
    public void setRanged(boolean value) { synchronized (lock) { this.ranged = value; } }
    public void setMace(boolean value) { synchronized (lock) { this.mace = value; } }
    public void setRangedMinRange(double value) { synchronized (lock) { this.rangedMinRange = clamp(value, 1.0, 50.0); } }
    public void setRangedOptimalRange(double value) { synchronized (lock) { this.rangedOptimalRange = clamp(value, 1.0, 50.0); } }
    public void setRangedMaxRange(double value) { synchronized (lock) { this.rangedMaxRange = clamp(value, 1.0, 50.0); } }
    public void setBowDrawTicks(int value) { synchronized (lock) { this.bowDrawTicks = clamp(value, 0, 100); } }
    public void setArrowPrediction(boolean value) { synchronized (lock) { this.arrowPrediction = value; } }
    public void setRangedStrafe(boolean value) { synchronized (lock) { this.rangedStrafe = value; } }
    public void setRangedRetreat(boolean value) { synchronized (lock) { this.rangedRetreat = value; } }
    public void setAutoShield(boolean value) { synchronized (lock) { this.autoShield = value; } }
    public void setShieldBreak(boolean value) { synchronized (lock) { this.shieldBreak = value; } }
    public void setShieldBreakChance(int value) { synchronized (lock) { this.shieldBreakChance = clamp(value, 0, 100); } }
    public void setShieldHoldTicks(int value) { synchronized (lock) { this.shieldHoldTicks = clamp(value, 1, 200); } }
    public void setShieldRaiseTicks(int value) { synchronized (lock) { this.shieldRaiseTicks = clamp(value, 1, 40); } }
    public void setShieldMace(boolean value) { synchronized (lock) { this.shieldMace = value; } }
    public void setAutoArmor(boolean value) { synchronized (lock) { this.autoArmor = value; } }
    public void setAutoWeapon(boolean value) { synchronized (lock) { this.autoWeapon = value; } }
    public void setAutoEat(boolean value) { synchronized (lock) { this.autoEat = value; } }
    public void setAutoPotion(boolean value) { synchronized (lock) { this.autoPotion = value; } }
    public void setAutoMend(boolean value) { synchronized (lock) { this.autoMend = value; } }
    public void setAutoTotem(boolean value) { synchronized (lock) { this.autoTotem = value; } }
    public void setTotemPriority(boolean value) { synchronized (lock) { this.totemPriority = value; } }
    public void setMissChance(int value) { synchronized (lock) { this.missChance = clamp(value, 0, 100); } }
    public void setMistakeChance(int value) { synchronized (lock) { this.mistakeChance = clamp(value, 0, 100); } }
    public void setAimSpeed(double value) { synchronized (lock) { this.aimSpeed = clamp(value, 0.0, 100.0); } }
    public void setShowInTab(boolean value) { synchronized (lock) { this.showInTab = value; } }
    public void setBotLeaveOnDeath(boolean value) { synchronized (lock) { this.botLeaveOnDeath = value; } }
    public void setDebug(boolean value) { synchronized (lock) { this.debug = value; } }
}
