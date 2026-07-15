package com.pvpbot.bot;

import com.pvpbot.PvPBotPlugin;
import com.pvpbot.npc.PvPBotTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BotManager {

    private final Map<UUID, NPC> activeNPCs = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public BotManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public NPC spawnBot(Location loc, String name) {
        String uniqueName = name;
        int suffix = 1;
        while (getNPC(uniqueName) != null) {
            uniqueName = name + "_" + suffix;
            suffix++;
        }

        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        NPC npc = registry.createNPC(EntityType.PLAYER, uniqueName);

        PvPBotTrait trait = npc.getOrAddTrait(PvPBotTrait.class);
        BotSettings settings = new BotSettings();
        settings.loadFromConfig(plugin.getConfig());
        boolean showInTab = settings.isShowInTab();
        trait.setSettings(settings);
        npc.data().set(NPC.Metadata.REMOVE_FROM_TABLIST, !showInTab);
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, showInTab);

        npc.spawn(loc);

        if (!showInTab) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (npc.isSpawned()) {
                    npc.data().set(NPC.Metadata.REMOVE_FROM_TABLIST, true);
                    npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, false);
                }
            }, 40L);
        }

        activeNPCs.put(npc.getUniqueId(), npc);
        plugin.getLogger().info("Spawned bot: " + uniqueName + " at " + loc.getWorld().getName() + " " +
                loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        PvPBotPlugin.broadcastDebug("BotManager: Spawned bot '" + uniqueName + "'");
        return npc;
    }

    public void removeBot(String name) {
        for (Map.Entry<UUID, NPC> entry : activeNPCs.entrySet()) {
            NPC npc = entry.getValue();
            if (npc.getName().equals(name)) {
                npc.destroy();
                activeNPCs.remove(entry.getKey());
                plugin.getLogger().info("Removed bot: " + name);
                PvPBotPlugin.broadcastDebug("BotManager: Removed bot '" + name + "'");
                return;
            }
        }
        plugin.getLogger().warning("Bot not found: " + name);
    }

    public void removeAll() {
        for (NPC npc : activeNPCs.values()) {
            npc.destroy();
        }
        activeNPCs.clear();
        plugin.getLogger().info("All bots removed.");
    }

    public NPC getNPC(String name) {
        for (NPC npc : activeNPCs.values()) {
            if (npc.getName().equals(name)) {
                return npc;
            }
        }
        return null;
    }

    public Map<UUID, NPC> getActiveNPCs() {
        return activeNPCs;
    }
}
