package com.pvpbot;

import com.pvpbot.bot.BotManager;
import com.pvpbot.command.PvPBotCommand;
import com.pvpbot.npc.PvPBotTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.plugin.java.JavaPlugin;

public class PvPBotPlugin extends JavaPlugin {

    private BotManager botManager;
    private static PvPBotPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        botManager = new BotManager(this);

        PvPBotCommand commandExecutor = new PvPBotCommand(botManager);
        getCommand("pvpbot").setExecutor(commandExecutor);
        getCommand("pvpbot").setTabCompleter(commandExecutor);

        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(PvPBotTrait.class));

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
}
