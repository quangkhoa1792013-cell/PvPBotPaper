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
            sender.sendMessage("Usage: /pvpbot spawn [name] | /pvpbot remove | /pvpbot removeall");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender);
            case "removeall" -> handleRemoveAll(sender);
            default -> {
                sender.sendMessage("Unknown subcommand. Usage: /pvpbot spawn [name] | /pvpbot remove | /pvpbot removeall");
                yield true;
            }
        };
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can spawn bots.");
            return true;
        }

        String name = (args.length >= 2) ? args[1] : "Bot_" + (random.nextInt(9000) + 1000);

        var safeLocation = SafeLocationFinder.findSafeLocation(player.getLocation());
        if (safeLocation == null) {
            sender.sendMessage("§c[PvPBot] Cannot find a safe location to spawn the bot in this area!");
            return true;
        }

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
        npc.addTrait(PvPBotTrait.class);
        npc.spawn(safeLocation);

        player.sendMessage("Spawned PvPBot '" + name + "' (ID: " + npc.getId() + ")");
        return true;
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
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return List.of("<name>");
        }
        return List.of();
    }
}
