// Phase 2.4: Real Player Join, Leave, Death Announcements & 10-tick Respawn Loop
package com.khoablabla.pvpbot.listeners;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

import java.util.HashMap;
import java.util.Map;

public class PlayerSimulationListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<Integer, Location> spawnLocations = new HashMap<>();

    public PlayerSimulationListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCSpawn(NPCSpawnEvent event) {
        NPC npc = event.getNPC();
        if (!npc.hasTrait(PvPBotTrait.class)) return;
        if (!(npc.getEntity() instanceof Player botPlayer)) return;

        spawnLocations.putIfAbsent(npc.getId(), botPlayer.getLocation().clone());

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
    public void onNPCDeath(NPCDeathEvent event) {
        NPC npc = event.getNPC();
        if (!npc.hasTrait(PvPBotTrait.class)) return;

        EntityDeathEvent bukkitEvent = event.getEvent();
        String botName = npc.getName();

        Location respawnLocation = spawnLocations.remove(npc.getId());
        if (respawnLocation == null || respawnLocation.getWorld() == null) {
            Location storedLocation = npc.getStoredLocation();
            respawnLocation = storedLocation == null ? null : storedLocation.clone();
        }
        if (respawnLocation == null || respawnLocation.getWorld() == null) {
            respawnLocation = bukkitEvent.getEntity().getLocation().clone();
        }

        if (respawnLocation == null || respawnLocation.getWorld() == null) {
            plugin.getLogger().warning("Cannot respawn " + botName + " (ID: " + npc.getId()
                    + "): no valid death or stored location.");
            npc.destroy();
            return;
        }

        int oldId = npc.getId();
        npc.destroy();

        final Location finalLoc = respawnLocation;
        new BukkitRunnable() {
            @Override
            public void run() {
                NPC replacement = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, botName);
                replacement.data().set(NPC.Metadata.REMOVE_FROM_PLAYERLIST, false);
                replacement.data().set(NPC.Metadata.REMOVE_FROM_TABLIST, false);
                replacement.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, true);
                replacement.addTrait(PvPBotTrait.class);

                boolean spawned = replacement.spawn(finalLoc);
                if (!spawned) {
                    plugin.getLogger().warning("Failed to respawn " + botName + " after death. Old NPC ID: "
                            + oldId + ", new NPC ID: " + replacement.getId() + ", location: "
                            + formatLocation(finalLoc));
                    replacement.destroy();
                    return;
                }

                if (replacement.getEntity() instanceof Player player) {
                    AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHealth != null) {
                        player.setHealth(maxHealth.getValue());
                    }
                    player.setFoodLevel(20);
                    player.setFireTicks(0);
                    player.setFallDistance(0.0F);
                    player.setNoDamageTicks(10);
                }

            }
        }.runTaskLater(plugin, 10);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.SPECTATOR) return;
        if (!CitizensAPI.getNPCRegistry().isNPC(event.getEntity())) return;
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        if (!npc.hasTrait(PvPBotTrait.class)) return;

        PvPBotTrait trait = npc.getTraitNullable(PvPBotTrait.class);
        if (trait != null) {
            trait.setTarget(attacker);
        }
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + " "
                + location.getBlockX() + ","
                + location.getBlockY() + ","
                + location.getBlockZ();
    }
}
