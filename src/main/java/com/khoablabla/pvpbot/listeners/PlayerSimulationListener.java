// Phase 3.3: Indirect Damage Revenge Tracking & Phantom Respawn Prevention
// Phase 4.1.1: Revenge gate — respect revenge + combat settings
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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

import java.util.HashMap;
import java.util.Map;

public class PlayerSimulationListener implements Listener {

    private static final Map<Integer, BukkitTask> respawnTasks = new HashMap<>();
    private final JavaPlugin plugin;
    private final Map<Integer, Location> spawnLocations = new HashMap<>();

    public PlayerSimulationListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void cancelRespawn(int npcId) {
        BukkitTask task = respawnTasks.remove(npcId);
        if (task != null) {
            task.cancel();
        }
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

        int oldId = npc.getId();
        cancelRespawn(oldId);

        EntityDeathEvent bukkitEvent = event.getEvent();
        org.bukkit.entity.Entity deadEntity = bukkitEvent.getEntity();
        if (deadEntity != null) {
            deadEntity.remove();
        }

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

        npc.destroy();

        final Location finalLoc = respawnLocation;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                respawnTasks.remove(oldId);

                NPC replacement = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, botName);
                replacement.data().set(NPC.Metadata.REMOVE_FROM_PLAYERLIST, false);
                replacement.data().set(NPC.Metadata.REMOVE_FROM_TABLIST, false);
                replacement.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, true);
                replacement.addTrait(PvPBotTrait.class);
                replacement.getOrAddTrait(net.citizensnpcs.trait.Gravity.class);
                replacement.getNavigator().getDefaultParameters()
                    .distanceMargin(1.0)
                    .pathDistanceMargin(1.0)
                    .attackRange(1.0);

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
        respawnTasks.put(oldId, task);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Tameable tame && tame.getOwner() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;
        if (attacker.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.SPECTATOR) return;
        if (!CitizensAPI.getNPCRegistry().isNPC(event.getEntity())) return;
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        if (!npc.hasTrait(PvPBotTrait.class)) return;

        PvPBotTrait trait = npc.getTraitNullable(PvPBotTrait.class);
        if (trait != null) {
            boolean revenge = trait.getSetting("revenge", Boolean.class);
            boolean combat = trait.getSetting("combat", Boolean.class);
            if (revenge && combat) {
                trait.setTarget(attacker);
            }
        }
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + " "
                + location.getBlockX() + ","
                + location.getBlockY() + ","
                + location.getBlockZ();
    }
}
