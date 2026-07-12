package com.pvpbot;

import com.pvpbot.bot.BotManager;
import com.pvpbot.command.PvPBotCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class PvPBotPlugin extends JavaPlugin {

    private BotManager botManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.botManager = new BotManager();
        registerCommand();
        getLogger().info("PvPBot v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            int count = botManager.getBotCount();
            botManager.removeAll();
            if (count > 0) {
                getLogger().info("Cleaned up " + count + " bot(s) on shutdown");
            }
        }
        getLogger().info("PvPBot disabled");
    }

    private void registerCommand() {
        PluginCommand command = getCommand("pvpbot");
        if (command != null) {
            PvPBotCommand executor = new PvPBotCommand(botManager);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Failed to register /pvpbot command - check plugin.yml");
        }
    }

    @NotNull
    public BotManager getBotManager() {
        return botManager;
    }
}
