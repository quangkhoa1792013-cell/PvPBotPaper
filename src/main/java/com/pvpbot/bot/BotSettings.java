package com.pvpbot.bot;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public class BotSettings {

    private double moveSpeed;
    private boolean bhop;
    private boolean idle;
    private int idleRadius;
    private boolean retreat;
    private double retreatHealth;
    private double viewDistance;
    private double aimSpeed;
    private boolean combatStrafe;
    private int combatStrafeInterval;

    private boolean combat;
    private boolean autoTarget;
    private boolean targetPlayers;
    private boolean targetMobs;
    private boolean targetBots;
    private boolean revenge;
    private boolean criticals;
    private int attackCooldown;
    private double meleeRange;
    private boolean preferSword;
    private boolean autoShield;
    private boolean shieldBreak;
    private double shieldBreakChance;
    private int shieldHoldTicks;
    private int shieldRaiseTicks;
    private boolean shieldMace;

    private boolean ranged;
    private boolean mace;
    private double rangedMinRange;
    private double rangedOptimalRange;
    private double rangedMaxRange;
    private int bowDrawTicks;
    private boolean arrowPrediction;
    private boolean rangedStrafe;
    private boolean rangedRetreat;

    private boolean autoArmor;
    private boolean autoWeapon;
    private boolean autoEat;
    private boolean autoPotion;
    private boolean autoMend;
    private boolean autoTotem;
    private boolean totemPriority;

    private double missChance;
    private double mistakeChance;
    private boolean profileLagFix;

    public BotSettings() {
        setDefaults();
    }

    public BotSettings(@NotNull FileConfiguration config) {
        loadFromConfig(config);
    }

    public void setDefaults() {
        this.moveSpeed = 0.25;
        this.bhop = false;
        this.idle = false;
        this.idleRadius = 10;
        this.retreat = false;
        this.retreatHealth = 0.3;
        this.viewDistance = 32.0;
        this.aimSpeed = 0.15;
        this.combatStrafe = true;
        this.combatStrafeInterval = 20;

        this.combat = false;
        this.autoTarget = false;
        this.targetPlayers = true;
        this.targetMobs = false;
        this.targetBots = false;
        this.revenge = false;
        this.criticals = false;
        this.attackCooldown = 10;
        this.meleeRange = 3.0;
        this.preferSword = true;
        this.autoShield = false;
        this.shieldBreak = false;
        this.shieldBreakChance = 30.0;
        this.shieldHoldTicks = 40;
        this.shieldRaiseTicks = 5;
        this.shieldMace = false;

        this.ranged = false;
        this.mace = false;
        this.rangedMinRange = 5.0;
        this.rangedOptimalRange = 20.0;
        this.rangedMaxRange = 50.0;
        this.bowDrawTicks = 20;
        this.arrowPrediction = true;
        this.rangedStrafe = true;
        this.rangedRetreat = true;

        this.autoArmor = false;
        this.autoWeapon = false;
        this.autoEat = false;
        this.autoPotion = false;
        this.autoMend = false;
        this.autoTotem = false;
        this.totemPriority = false;

        this.missChance = 0;
        this.mistakeChance = 0;
        this.profileLagFix = false;
    }

    public void loadFromConfig(@NotNull FileConfiguration config) {
        this.moveSpeed = Math.max(0.1, Math.min(2.0, config.getDouble("bot-settings.move-speed", 0.25)));
        this.bhop = config.getBoolean("bot-settings.bhop", false);
        this.idle = config.getBoolean("bot-settings.idle", false);
        this.idleRadius = Math.max(3, Math.min(50, config.getInt("bot-settings.idle-radius", 10)));
        this.retreat = config.getBoolean("bot-settings.retreat", false);
        this.retreatHealth = Math.max(0.0, Math.min(1.0, config.getDouble("bot-settings.retreat-health", 0.3)));
        this.viewDistance = Math.max(5, Math.min(128, config.getDouble("bot-settings.view-distance", 32.0)));
        this.aimSpeed = Math.max(0.01, Math.min(1.0, config.getDouble("bot-settings.aim-speed", 0.15)));
        this.combatStrafe = config.getBoolean("bot-settings.combat-strafe", true);
        this.combatStrafeInterval = Math.max(5, Math.min(100, config.getInt("bot-settings.combat-strafe-interval", 20)));

        this.combat = config.getBoolean("bot-settings.combat", false);
        this.autoTarget = config.getBoolean("bot-settings.auto-target", false);
        this.targetPlayers = config.getBoolean("bot-settings.target-players", true);
        this.targetMobs = config.getBoolean("bot-settings.target-mobs", false);
        this.targetBots = config.getBoolean("bot-settings.target-bots", false);
        this.revenge = config.getBoolean("bot-settings.revenge", false);
        this.criticals = config.getBoolean("bot-settings.criticals", false);
        this.attackCooldown = Math.max(2, Math.min(60, config.getInt("bot-settings.attack-cooldown", 10)));
        this.meleeRange = Math.max(2.0, Math.min(6.0, config.getDouble("bot-settings.melee-range", 3.0)));
        this.preferSword = config.getBoolean("bot-settings.prefer-sword", true);
        this.autoShield = config.getBoolean("bot-settings.auto-shield", false);
        this.shieldBreak = config.getBoolean("bot-settings.shield-break", false);
        this.shieldBreakChance = Math.max(0, Math.min(100, config.getDouble("bot-settings.shield-break-chance", 30.0)));
        this.shieldHoldTicks = Math.max(10, Math.min(200, config.getInt("bot-settings.shield-hold-ticks", 40)));
        this.shieldRaiseTicks = Math.max(0, Math.min(40, config.getInt("bot-settings.shield-raise-ticks", 5)));
        this.shieldMace = config.getBoolean("bot-settings.shield-mace", false);

        this.ranged = config.getBoolean("bot-settings.ranged", false);
        this.mace = config.getBoolean("bot-settings.mace", false);
        this.rangedMinRange = Math.max(3.0, Math.min(20.0, config.getDouble("bot-settings.ranged-min-range", 5.0)));
        this.rangedOptimalRange = Math.max(10.0, Math.min(50.0, config.getDouble("bot-settings.ranged-optimal-range", 20.0)));
        this.rangedMaxRange = Math.max(15.0, Math.min(100.0, config.getDouble("bot-settings.ranged-max-range", 50.0)));
        this.bowDrawTicks = Math.max(5, Math.min(100, config.getInt("bot-settings.bow-draw-ticks", 20)));
        this.arrowPrediction = config.getBoolean("bot-settings.arrow-prediction", true);
        this.rangedStrafe = config.getBoolean("bot-settings.ranged-strafe", true);
        this.rangedRetreat = config.getBoolean("bot-settings.ranged-retreat", true);

        this.autoArmor = config.getBoolean("bot-settings.auto-armor", false);
        this.autoWeapon = config.getBoolean("bot-settings.auto-weapon", false);
        this.autoEat = config.getBoolean("bot-settings.auto-eat", false);
        this.autoPotion = config.getBoolean("bot-settings.auto-potion", false);
        this.autoMend = config.getBoolean("bot-settings.auto-mend", false);
        this.autoTotem = config.getBoolean("bot-settings.auto-totem", false);
        this.totemPriority = config.getBoolean("bot-settings.totem-priority", false);

        this.missChance = Math.max(0, Math.min(100, config.getDouble("bot-settings.miss-chance", 0)));
        this.mistakeChance = Math.max(0, Math.min(100, config.getDouble("bot-settings.mistake-chance", 0)));
        this.profileLagFix = config.getBoolean("bot-settings.profile-lagg-fix", false);
    }

    public double getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(double moveSpeed) { this.moveSpeed = Math.max(0.1, Math.min(2.0, moveSpeed)); }

    public boolean isBhop() { return bhop; }
    public void setBhop(boolean bhop) { this.bhop = bhop; }

    public boolean isIdle() { return idle; }
    public void setIdle(boolean idle) { this.idle = idle; }

    public int getIdleRadius() { return idleRadius; }
    public void setIdleRadius(int idleRadius) { this.idleRadius = Math.max(3, Math.min(50, idleRadius)); }

    public boolean isRetreat() { return retreat; }
    public void setRetreat(boolean retreat) { this.retreat = retreat; }

    public double getRetreatHealth() { return retreatHealth; }
    public void setRetreatHealth(double retreatHealth) { this.retreatHealth = Math.max(0.0, Math.min(1.0, retreatHealth)); }

    public double getViewDistance() { return viewDistance; }
    public void setViewDistance(double viewDistance) { this.viewDistance = Math.max(5, Math.min(128, viewDistance)); }

    public double getAimSpeed() { return aimSpeed; }
    public void setAimSpeed(double aimSpeed) { this.aimSpeed = Math.max(0.01, Math.min(1.0, aimSpeed)); }

    public boolean isCombatStrafe() { return combatStrafe; }
    public void setCombatStrafe(boolean combatStrafe) { this.combatStrafe = combatStrafe; }

    public int getCombatStrafeInterval() { return combatStrafeInterval; }
    public void setCombatStrafeInterval(int interval) { this.combatStrafeInterval = Math.max(5, Math.min(100, interval)); }

    public boolean isCombat() { return combat; }
    public void setCombat(boolean combat) { this.combat = combat; }

    public boolean isAutoTarget() { return autoTarget; }
    public void setAutoTarget(boolean autoTarget) { this.autoTarget = autoTarget; }

    public boolean isTargetPlayers() { return targetPlayers; }
    public void setTargetPlayers(boolean targetPlayers) { this.targetPlayers = targetPlayers; }

    public boolean isTargetMobs() { return targetMobs; }
    public void setTargetMobs(boolean targetMobs) { this.targetMobs = targetMobs; }

    public boolean isTargetBots() { return targetBots; }
    public void setTargetBots(boolean targetBots) { this.targetBots = targetBots; }

    public boolean isRevenge() { return revenge; }
    public void setRevenge(boolean revenge) { this.revenge = revenge; }

    public boolean isCriticals() { return criticals; }
    public void setCriticals(boolean criticals) { this.criticals = criticals; }

    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int attackCooldown) { this.attackCooldown = Math.max(2, Math.min(60, attackCooldown)); }

    public double getMeleeRange() { return meleeRange; }
    public void setMeleeRange(double meleeRange) { this.meleeRange = Math.max(2.0, Math.min(6.0, meleeRange)); }

    public boolean isPreferSword() { return preferSword; }
    public void setPreferSword(boolean preferSword) { this.preferSword = preferSword; }

    public boolean isAutoShield() { return autoShield; }
    public void setAutoShield(boolean autoShield) { this.autoShield = autoShield; }

    public boolean isShieldBreak() { return shieldBreak; }
    public void setShieldBreak(boolean shieldBreak) { this.shieldBreak = shieldBreak; }

    public double getShieldBreakChance() { return shieldBreakChance; }
    public void setShieldBreakChance(double shieldBreakChance) { this.shieldBreakChance = Math.max(0, Math.min(100, shieldBreakChance)); }

    public int getShieldHoldTicks() { return shieldHoldTicks; }
    public void setShieldHoldTicks(int shieldHoldTicks) { this.shieldHoldTicks = Math.max(10, Math.min(200, shieldHoldTicks)); }

    public int getShieldRaiseTicks() { return shieldRaiseTicks; }
    public void setShieldRaiseTicks(int shieldRaiseTicks) { this.shieldRaiseTicks = Math.max(0, Math.min(40, shieldRaiseTicks)); }

    public boolean isShieldMace() { return shieldMace; }
    public void setShieldMace(boolean shieldMace) { this.shieldMace = shieldMace; }

    public boolean isRanged() { return ranged; }
    public void setRanged(boolean ranged) { this.ranged = ranged; }

    public boolean isMace() { return mace; }
    public void setMace(boolean mace) { this.mace = mace; }

    public double getRangedMinRange() { return rangedMinRange; }
    public void setRangedMinRange(double rangedMinRange) { this.rangedMinRange = Math.max(3.0, Math.min(20.0, rangedMinRange)); }

    public double getRangedOptimalRange() { return rangedOptimalRange; }
    public void setRangedOptimalRange(double rangedOptimalRange) { this.rangedOptimalRange = Math.max(10.0, Math.min(50.0, rangedOptimalRange)); }

    public double getRangedMaxRange() { return rangedMaxRange; }
    public void setRangedMaxRange(double rangedMaxRange) { this.rangedMaxRange = Math.max(15.0, Math.min(100.0, rangedMaxRange)); }

    public int getBowDrawTicks() { return bowDrawTicks; }
    public void setBowDrawTicks(int bowDrawTicks) { this.bowDrawTicks = Math.max(5, Math.min(100, bowDrawTicks)); }

    public boolean isArrowPrediction() { return arrowPrediction; }
    public void setArrowPrediction(boolean arrowPrediction) { this.arrowPrediction = arrowPrediction; }

    public boolean isRangedStrafe() { return rangedStrafe; }
    public void setRangedStrafe(boolean rangedStrafe) { this.rangedStrafe = rangedStrafe; }

    public boolean isRangedRetreat() { return rangedRetreat; }
    public void setRangedRetreat(boolean rangedRetreat) { this.rangedRetreat = rangedRetreat; }

    public boolean isAutoArmor() { return autoArmor; }
    public void setAutoArmor(boolean autoArmor) { this.autoArmor = autoArmor; }

    public boolean isAutoWeapon() { return autoWeapon; }
    public void setAutoWeapon(boolean autoWeapon) { this.autoWeapon = autoWeapon; }

    public boolean isAutoEat() { return autoEat; }
    public void setAutoEat(boolean autoEat) { this.autoEat = autoEat; }

    public boolean isAutoPotion() { return autoPotion; }
    public void setAutoPotion(boolean autoPotion) { this.autoPotion = autoPotion; }

    public boolean isAutoMend() { return autoMend; }
    public void setAutoMend(boolean autoMend) { this.autoMend = autoMend; }

    public boolean isAutoTotem() { return autoTotem; }
    public void setAutoTotem(boolean autoTotem) { this.autoTotem = autoTotem; }

    public boolean isTotemPriority() { return totemPriority; }
    public void setTotemPriority(boolean totemPriority) { this.totemPriority = totemPriority; }

    public double getMissChance() { return missChance; }
    public void setMissChance(double missChance) { this.missChance = Math.max(0, Math.min(100, missChance)); }

    public double getMistakeChance() { return mistakeChance; }
    public void setMistakeChance(double mistakeChance) { this.mistakeChance = Math.max(0, Math.min(100, mistakeChance)); }

    public boolean isProfileLagFix() { return profileLagFix; }
    public void setProfileLagFix(boolean profileLagFix) { this.profileLagFix = profileLagFix; }
}
