// Phase 2: Core Commands, Mass Spawn, and Tab Completion
package com.khoablabla.pvpbot.commands;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.khoablabla.pvpbot.traits.PvPBotTrait;
import com.khoablabla.pvpbot.utils.SafeLocationFinder;

import java.util.List;
import java.util.Random;

public class PvPBotCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_TARGET_DISTANCE = 5;
    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pvpbot.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /pvpbot spawn [name|number] [name2...] | /pvpbot remove | /pvpbot removeall");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender);
            case "removeall" -> handleRemoveAll(sender);
            default -> {
                sender.sendMessage("Unknown subcommand. Usage: /pvpbot spawn [name|number] [name2...] | /pvpbot remove | /pvpbot removeall");
                yield true;
            }
        };
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can spawn bots.");
            return true;
        }

        if (args.length == 1) {
            String name = generateUniqueRealisticName();
            spawnSingleBot(player, name);
            player.sendMessage("Spawned PvPBot '" + name + "'");
            return true;
        }

        if (args.length == 2) {
            try {
                int count = Integer.parseInt(args[1]);
                count = Math.max(1, Math.min(50, count));
                int spawned = 0;
                for (int i = 0; i < count; i++) {
                    String name = generateUniqueRealisticName();
                    if (spawnSingleBot(player, name)) spawned++;
                }
                player.sendMessage("Spawned " + spawned + " random PvPBot(s).");
                return true;
            } catch (NumberFormatException e) {
                String name = args[1];
                if (name.contains("<") || name.contains(">")) {
                    player.sendMessage("§c[PvPBot] Invalid name '" + name + "' — name must not contain brackets.");
                    return true;
                }
                if (isNameTaken(name)) {
                    player.sendMessage("§c[PvPBot] A bot with the name '" + name + "' already exists!");
                    return true;
                }
                spawnSingleBot(player, name);
                player.sendMessage("Spawned PvPBot '" + name + "'");
                return true;
            }
        }

        int spawned = 0;
        for (int i = 1; i < args.length; i++) {
            String name = args[i];
            if (name.contains("<") || name.contains(">")) {
                player.sendMessage("§c[PvPBot] Skipping '" + name + "' — name must not contain brackets.");
                continue;
            }
            if (isNameTaken(name)) {
                player.sendMessage("§c[PvPBot] Skipping '" + name + "' — a bot with that name already exists.");
                continue;
            }
            if (spawnSingleBot(player, name)) spawned++;
        }
        player.sendMessage("Spawned " + spawned + " custom PvPBot(s).");
        return true;
    }

    private boolean spawnSingleBot(Player player, String name) {
        var safeLocation = SafeLocationFinder.findSafeLocation(player.getLocation());
        if (safeLocation == null) return false;

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
        npc.data().set(NPC.Metadata.REMOVE_FROM_PLAYERLIST, false);
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, true);
        npc.addTrait(PvPBotTrait.class);
        npc.spawn(safeLocation);
        return true;
    }

    private boolean isNameTaken(String name) {
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(PvPBotTrait.class) && npc.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private String generateUniqueRealisticName() {
        String[] prefixes = {"Bread", "Beard", "Steve", "Alex", "Miner", "Slayer", "Gamer", "Hunter", "Pvp", "Pro", "Craft", "Knight"};
        String[] suffixes = {"1412", "99", "123", "77", "88", "_pvp", "_pro", "_gg", "456", "321"};

        String name;
        int attempts = 0;
        do {
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String suffix = suffixes[random.nextInt(suffixes.length)];
            name = prefix + suffix;
            attempts++;
        } while (isNameTaken(name) && attempts < 100);

        return name;
    }

    private boolean handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Entity target = player.getTargetEntity(MAX_TARGET_DISTANCE);
        if (target == null || !CitizensAPI.getNPCRegistry().isNPC(target)) {
            sender.sendMessage("No PvPBot found in your line of sight.");
            return true;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getNPC(target);
        if (!npc.hasTrait(PvPBotTrait.class)) {
            sender.sendMessage("That NPC is not a PvPBot.");
            return true;
        }

        npc.destroy();
        sender.sendMessage("Removed PvPBot (ID: " + npc.getId() + ")");
        return true;
    }

    private boolean handleRemoveAll(CommandSender sender) {
        int removed = 0;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(PvPBotTrait.class)) {
                npc.destroy();
                removed++;
            }
        }
        sender.sendMessage("Removed " + removed + " PvPBot(s).");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return List.of("spawn", "remove", "removeall").stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }
        return List.of();
    }
}
