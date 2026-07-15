package com.pvpbot;

import com.pvpbot.bot.BotManager;
import com.pvpbot.bot.BotSettings;
import com.pvpbot.command.PvPBotCommand;
import com.pvpbot.gui.SettingsGUI;
import com.pvpbot.npc.PvPBotTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PvPBotPlugin extends JavaPlugin {

    private BotManager botManager;
    private BotSettings defaultSettings;
    private SettingsGUI settingsGUI;
    private static PvPBotPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        defaultSettings = new BotSettings();
        defaultSettings.loadFromConfig(getConfig());

        botManager = new BotManager(this);
        settingsGUI = new SettingsGUI(this, botManager);

        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(PvPBotTrait.class));

        PvPBotCommand commandExecutor = new PvPBotCommand(botManager, defaultSettings, settingsGUI);
        getCommand("pvpbot").setExecutor(commandExecutor);
        getCommand("pvpbot").setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(settingsGUI, this);

        getLogger().info("PvPBot v" + getDescription().getVersion() + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.removeAll();
        }
        getLogger().info("PvPBot disabled.");
    }

    public static PvPBotPlugin getInstance() {
        return instance;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public BotSettings getDefaultSettings() {
        return defaultSettings;
    }

    public void saveDefaultSettings() {
        defaultSettings.saveToConfig(getConfig());
        saveConfig();
    }

    public static void broadcastDebug(String message) {
        if (instance == null) return;
        if (!instance.getDefaultSettings().isDebug()) return;
        instance.getLogger().info("[DEBUG] " + message);
        for (Player player : instance.getServer().getOnlinePlayers()) {
            if (player.hasPermission("pvpbot.admin")) {
                player.sendMessage(ChatColor.GRAY + "[PvPDebug] " + ChatColor.WHITE + message);
            }
        }
    }
}
