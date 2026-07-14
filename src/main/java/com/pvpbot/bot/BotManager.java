package com.pvpbot.bot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BotManager {

    private final ConcurrentHashMap<UUID, CustomBot> bots = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();
    private int maxTotalBots;
    private int maxBotsPerPlayer;
    private final BotSettings defaultSettings = new BotSettings();

    public BotManager() {
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = Bukkit.getPluginManager().getPlugin("PvPBot").getConfig();
        this.maxTotalBots = config.getInt("settings.max-total-bots", 50);
        this.maxBotsPerPlayer = config.getInt("settings.max-bots-per-player", 5);
        this.defaultSettings.loadFromConfig(config);
    }

    @Nullable
    public CustomBot spawnBot(@NotNull Location location, @Nullable String name, @NotNull Player owner) {
        if (bots.size() >= maxTotalBots) {
            owner.sendMessage("§cMaximum total bots reached (" + maxTotalBots + ")");
            return null;
        }

        long ownedCount = bots.values().stream()
                .filter(b -> b.getOwnerUUID().equals(owner.getUniqueId()))
                .count();
        if (ownedCount >= maxBotsPerPlayer) {
            owner.sendMessage("§cYou have reached the maximum of " + maxBotsPerPlayer + " bots");
            return null;
        }

        String finalName = name;
        if (finalName != null && !finalName.isEmpty()) {
            if (nameIndex.containsKey(finalName.toLowerCase())) {
                owner.sendMessage("§cA bot with the name '" + finalName + "' already exists");
                return null;
            }
        }

        boolean profileLagFix = defaultSettings.isProfileLagFix();
        CustomBot bot = CustomBot.spawn(location, finalName, owner.getUniqueId(), profileLagFix, this);

        BotSettings botSettings = bot.getSettings();
        botSettings.setMoveSpeed(defaultSettings.getMoveSpeed());
        botSettings.setBhop(defaultSettings.isBhop());
        botSettings.setIdle(defaultSettings.isIdle());
        botSettings.setIdleRadius(defaultSettings.getIdleRadius());
        botSettings.setRetreat(defaultSettings.isRetreat());
        botSettings.setRetreatHealth(defaultSettings.getRetreatHealth());
        botSettings.setViewDistance(defaultSettings.getViewDistance());
        botSettings.setAimSpeed(defaultSettings.getAimSpeed());
        botSettings.setCombat(defaultSettings.isCombat());
        botSettings.setAutoTarget(defaultSettings.isAutoTarget());
        botSettings.setTargetPlayers(defaultSettings.isTargetPlayers());
        botSettings.setTargetMobs(defaultSettings.isTargetMobs());
        botSettings.setTargetBots(defaultSettings.isTargetBots());
        botSettings.setRevenge(defaultSettings.isRevenge());
        botSettings.setCriticals(defaultSettings.isCriticals());
        botSettings.setAttackCooldown(defaultSettings.getAttackCooldown());
        botSettings.setMeleeRange(defaultSettings.getMeleeRange());
        botSettings.setPreferSword(defaultSettings.isPreferSword());
        botSettings.setAutoShield(defaultSettings.isAutoShield());
        botSettings.setShieldBreak(defaultSettings.isShieldBreak());
        botSettings.setShieldBreakChance(defaultSettings.getShieldBreakChance());
        botSettings.setShieldHoldTicks(defaultSettings.getShieldHoldTicks());
        botSettings.setShieldRaiseTicks(defaultSettings.getShieldRaiseTicks());
        botSettings.setShieldMace(defaultSettings.isShieldMace());
        botSettings.setRanged(defaultSettings.isRanged());
        botSettings.setMace(defaultSettings.isMace());
        botSettings.setRangedMinRange(defaultSettings.getRangedMinRange());
        botSettings.setRangedOptimalRange(defaultSettings.getRangedOptimalRange());
        botSettings.setRangedMaxRange(defaultSettings.getRangedMaxRange());
        botSettings.setBowDrawTicks(defaultSettings.getBowDrawTicks());
        botSettings.setArrowPrediction(defaultSettings.isArrowPrediction());
        botSettings.setRangedStrafe(defaultSettings.isRangedStrafe());
        botSettings.setRangedRetreat(defaultSettings.isRangedRetreat());
        botSettings.setAutoArmor(defaultSettings.isAutoArmor());
        botSettings.setAutoWeapon(defaultSettings.isAutoWeapon());
        botSettings.setAutoEat(defaultSettings.isAutoEat());
        botSettings.setAutoPotion(defaultSettings.isAutoPotion());
        botSettings.setAutoMend(defaultSettings.isAutoMend());
        botSettings.setAutoTotem(defaultSettings.isAutoTotem());
        botSettings.setTotemPriority(defaultSettings.isTotemPriority());
        botSettings.setMissChance(defaultSettings.getMissChance());
        botSettings.setMistakeChance(defaultSettings.getMistakeChance());
        botSettings.setProfileLagFix(defaultSettings.isProfileLagFix());
        botSettings.setBotLeaveOnDeath(defaultSettings.isBotLeaveOnDeath());
        botSettings.setShowInTab(defaultSettings.isShowInTab());

        bot.hideFromTabList();

        bots.put(bot.getUUID(), bot);
        nameIndex.put(bot.getBotName().toLowerCase(), bot.getUUID());

        org.bukkit.entity.Player botPlayer = bot.getBukkitEntity();
        if (botPlayer != null) {
            botPlayer.setPlayerListName("§7[Bot] §f" + bot.getBotName());
        }

        com.pvpbot.stats.StatsDatabase.getInstance().recordBotSpawn(bot.getBotName());

        return bot;
    }

    public boolean removeBot(@NotNull String name) {
        UUID uuid = nameIndex.remove(name.toLowerCase());
        if (uuid == null) {
            return false;
        }
        CustomBot bot = bots.remove(uuid);
        if (bot == null) {
            return false;
        }
        bot.removeFromWorld();
        return true;
    }

    public boolean removeBot(@NotNull CustomBot bot) {
        if (bots.remove(bot.getUUID()) == null) {
            return false;
        }
        nameIndex.remove(bot.getBotName().toLowerCase());
        bot.removeFromWorld();
        return true;
    }

    public void removeAll() {
        Collection<CustomBot> allBots = bots.values();
        bots.clear();
        nameIndex.clear();
        for (CustomBot bot : allBots) {
            try {
                bot.removeFromWorld();
            } catch (Exception ignored) {
            }
        }
    }

    public void removeAllForPlayer(@NotNull UUID ownerUUID) {
        bots.values().stream()
                .filter(b -> b.getOwnerUUID().equals(ownerUUID))
                .toList()
                .forEach(this::removeBot);
    }

    @Nullable
    public CustomBot getBot(@NotNull String name) {
        UUID uuid = nameIndex.get(name.toLowerCase());
        if (uuid == null) {
            return null;
        }
        return bots.get(uuid);
    }

    @Nullable
    public CustomBot getBot(@NotNull UUID uuid) {
        return bots.get(uuid);
    }

    @NotNull
    public Collection<CustomBot> getAllBots() {
        return Collections.unmodifiableCollection(bots.values());
    }

    public int getBotCount() {
        return bots.size();
    }

    public boolean isBot(@NotNull UUID uuid) {
        return bots.containsKey(uuid);
    }

    @NotNull
    public java.util.List<String> getBotNames() {
        return bots.values().stream()
                .map(CustomBot::getBotName)
                .toList();
    }

    @NotNull
    public BotSettings getDefaultSettings() {
        return defaultSettings;
    }
}
