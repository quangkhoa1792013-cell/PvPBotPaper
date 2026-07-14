package com.pvpbot.command;

import com.pvpbot.bot.BotManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PvPBotCommand implements TabExecutor {

    private final BotManager botManager;

    private static final List<String> SUBCOMMANDS = Arrays.asList("spawn", "remove", "removeall");

    public PvPBotCommand(BotManager botManager) {
        this.botManager = botManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /pvpbot <spawn|remove|removeall> [name]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn":
                return handleSpawn(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "removeall":
                return handleRemoveAll(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use: spawn, remove, removeall");
                return true;
        }
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can spawn bots.");
            return true;
        }

        String name = args.length >= 2 ? args[1] : "PvPBot";
        botManager.spawnBot(player.getLocation(), name);
        sender.sendMessage(ChatColor.GREEN + "Spawned bot: " + name);
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pvpbot remove <name>");
            return true;
        }

        String name = args[1];
        botManager.removeBot(name);
        sender.sendMessage(ChatColor.GREEN + "Removed bot: " + name);
        return true;
    }

    private boolean handleRemoveAll(CommandSender sender) {
        botManager.removeAll();
        sender.sendMessage(ChatColor.GREEN + "All bots removed.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    result.add(sub);
                }
            }
            return result;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            String partial = args[1].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String name : botManager.getActiveNPCs().values().stream()
                    .map(npc -> npc.getName()).toList()) {
                if (name.toLowerCase().startsWith(partial)) {
                    result.add(name);
                }
            }
            return result;
        }

        return List.of();
    }
}
