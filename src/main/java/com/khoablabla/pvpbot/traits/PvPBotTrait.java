package com.khoablabla.pvpbot.traits;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.khoablabla.pvpbot.PvPBot;

public class PvPBotTrait extends Trait {

    private int tickCounter = 0;

    public PvPBotTrait() {
        super("pvpbot");
    }

    @Override
    public void onAttach() {
        if (npc.getEntity() instanceof Player) {
            JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
            plugin.getLogger().info("PvPBotTrait attached to NPC " + npc.getName() + " (ID: " + npc.getId() + ")");
        } else if (npc.getEntity() != null) {
            JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
            plugin.getLogger().warning("PvPBotTrait attached to NPC " + npc.getName()
                    + " (ID: " + npc.getId() + ") but its entity is not a Player — PvP functionality may be limited.");
        }
    }

    @Override
    public void onSpawn() {
        if (npc.getEntity() instanceof Player player) {
            npc.setProtected(false);
            npc.data().set(NPC.Metadata.DAMAGE_BY_PLAYER, true);

            JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
            plugin.getLogger().info("PvPBot NPC '" + npc.getName() + "' (ID: " + npc.getId()
                    + ") has spawned in world: " + player.getWorld().getName());
        }
    }

    @Override
    public void onDespawn() {
        JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
        plugin.getLogger().info("PvPBot NPC (ID: " + npc.getId() + ") has despawned from world.");
    }

    @Override
    public void run() {
        tickCounter++;
        if (tickCounter % 100 != 0) return;

        if (npc.getEntity() instanceof Player player) {
            double health = player.getHealth();
            Bukkit.getLogger().info("[PvPBot] " + npc.getName() + " (ID: " + npc.getId()
                    + ") is alive with HP: " + health);
        }
    }
}
