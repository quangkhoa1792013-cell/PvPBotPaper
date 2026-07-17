package com.khoablabla.pvpbot;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.plugin.java.JavaPlugin;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

public class PvPBot extends JavaPlugin {

    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().severe("Citizens is required for PvPBot to function. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(PvPBotTrait.class).withName("pvpbot")
        );

        getLogger().info("PvPBot trait registered successfully.");

        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().info("ProtocolLib detected — advanced packet features available.");
        } else {
            getLogger().info("ProtocolLib not detected — packet features will be unavailable.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("PvPBot shutting down cleanly.");
    }
}
