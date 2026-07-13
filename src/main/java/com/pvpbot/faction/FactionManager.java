package com.pvpbot.faction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FactionManager {

    private final File factionsFile;
    private final Map<String, Faction> factions;
    private final JavaPlugin plugin;
    private int gradualTeleportTaskId;

    public FactionManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.factionsFile = new File(plugin.getDataFolder(), "factions.yml");
        this.factions = new HashMap<>();
        this.gradualTeleportTaskId = -1;
        loadFactions();
    }

    private void loadFactions() {
        if (!factionsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(factionsFile);
        for (String name : config.getKeys(false)) {
            Faction faction = new Faction(name);
            faction.setFriendlyFire(config.getBoolean(name + ".friendly-fire", false));
            List<String> memberList = config.getStringList(name + ".members");
            for (String uuidStr : memberList) {
                try {
                    faction.addMember(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }
            List<String> enemyList = config.getStringList(name + ".enemies");
            for (String enemyName : enemyList) {
                faction.addEnemy(enemyName);
            }
            factions.put(name.toLowerCase(), faction);
        }
    }

    public void saveFactions() {
        YamlConfiguration config = new YamlConfiguration();
        for (Faction faction : factions.values()) {
            String prefix = faction.getName() + ".";
            config.set(prefix + "friendly-fire", faction.isFriendlyFire());
            config.set(prefix + "members", faction.getMembers().stream().map(UUID::toString).toList());
            config.set(prefix + "enemies", faction.getEnemies().stream().toList());
        }
        try {
            config.save(factionsFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[PvPBot] Failed to save factions.yml: " + e.getMessage());
        }
    }

    public boolean createFaction(@NotNull String name) {
        if (factionExists(name)) return false;
        factions.put(name.toLowerCase(), new Faction(name));
        saveFactions();
        return true;
    }

    public boolean deleteFaction(@NotNull String name) {
        if (!factionExists(name)) return false;
        factions.remove(name.toLowerCase());
        saveFactions();
        return true;
    }

    public boolean factionExists(@NotNull String name) {
        return factions.containsKey(name.toLowerCase());
    }

    @Nullable
    public Faction getFaction(@NotNull String name) {
        return factions.get(name.toLowerCase());
    }

    @Nullable
    public Faction getFactionOf(@NotNull UUID uuid) {
        for (Faction faction : factions.values()) {
            if (faction.hasMember(uuid)) return faction;
        }
        return null;
    }

    public boolean addToFaction(@NotNull String factionName, @NotNull UUID uuid) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null) return false;
        faction.addMember(uuid);
        saveFactions();
        return true;
    }

    public boolean removeFromFaction(@NotNull String factionName, @NotNull UUID uuid) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null) return false;
        faction.removeMember(uuid);
        saveFactions();
        return true;
    }

    public boolean setHostile(@NotNull String factionName1, @NotNull String factionName2, boolean hostile) {
        Faction f1 = factions.get(factionName1.toLowerCase());
        Faction f2 = factions.get(factionName2.toLowerCase());
        if (f1 == null || f2 == null) return false;
        String key2 = factionName2.toLowerCase();
        String key1 = factionName1.toLowerCase();
        if (hostile) {
            f1.addEnemy(key2);
            f2.addEnemy(key1);
        } else {
            f1.removeEnemy(key2);
            f2.removeEnemy(key1);
        }
        saveFactions();
        return true;
    }

    public boolean areHostile(@NotNull String factionName1, @NotNull String factionName2) {
        Faction f1 = factions.get(factionName1.toLowerCase());
        return f1 != null && f1.isEnemy(factionName2);
    }

    public boolean canAttack(@NotNull UUID attacker, @NotNull UUID target) {
        Faction attackerFaction = getFactionOf(attacker);
        Faction targetFaction = getFactionOf(target);
        if (attackerFaction == null || targetFaction == null) return true;
        if (!attackerFaction.equals(targetFaction)) return true;
        return attackerFaction.isFriendlyFire();
    }

    @NotNull
    public Collection<Faction> getAllFactions() {
        return factions.values();
    }

    public void teleportFactionGradually(@NotNull Faction faction, @NotNull Location targetLoc) {
        List<UUID> memberList = new ArrayList<>(faction.getMembers());
        if (memberList.isEmpty()) return;

        int[] index = {0};
        gradualTeleportTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (index[0] >= memberList.size()) {
                if (gradualTeleportTaskId >= 0) {
                    Bukkit.getScheduler().cancelTask(gradualTeleportTaskId);
                    gradualTeleportTaskId = -1;
                }
                return;
            }
            UUID memberId = memberList.get(index[0]);
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                player.teleport(targetLoc);
            }
            index[0]++;
        }, 0L, 2L).getTaskId();
    }

    public void stopGradualTeleport() {
        if (gradualTeleportTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(gradualTeleportTaskId);
            gradualTeleportTaskId = -1;
        }
    }
}
