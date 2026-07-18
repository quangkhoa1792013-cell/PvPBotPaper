// Phase 2.4: Real Player Join, Leave, Death Announcements & 5s Respawn Loop
package com.khoablabla.pvpbot.listeners;

import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

public class PlayerSimulationListener implements Listener {

    private final JavaPlugin plugin;

    public PlayerSimulationListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCSpawn(NPCSpawnEvent event) {
        NPC npc = event.getNPC();
        if (!npc.hasTrait(PvPBotTrait.class)) return;
        if (!(npc.getEntity() instanceof Player botPlayer)) return;

        Bukkit.getLogger().info("§e" + npc.getName() + " joined the game");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, botPlayer);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();

        for (NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(PvPBotTrait.class) && npc.isSpawned()
                    && npc.getEntity() instanceof Player botPlayer) {
                joiner.showPlayer(plugin, botPlayer);
            }
        }
    }

    @EventHandler
    public void onNPCDespawn(NPCDespawnEvent event) {
        NPC npc = event.getNPC();
        if (!npc.hasTrait(PvPBotTrait.class)) return;

        if (event.getReason() != DespawnReason.DEATH) {
            Bukkit.broadcastMessage("§e" + npc.getName() + " left the game");
        }
    }

    @EventHandler
    public void onNPCDeath(NPCDeathEvent event) {
        NPC npc = event.getNPC();
        if (!npc.hasTrait(PvPBotTrait.class)) return;

        EntityDeathEvent bukkitEvent = event.getEvent();
        Player killer = bukkitEvent.getEntity().getKiller();
        String killerName = (killer != null) ? killer.getName() : "an unknown entity";

        Bukkit.getLogger().info("§c" + npc.getName() + " was slain by " + killerName);

        Location respawnLocation = npc.getStoredLocation();
        if (respawnLocation == null) {
            respawnLocation = npc.getEntity().getLocation();
        }

        final NPC finalNpc = npc;
        final Location finalLoc = respawnLocation;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!finalNpc.isSpawned()) {
                    finalNpc.spawn(finalLoc);
                }
            }
        }.runTaskLater(plugin, 100);
    }
}
