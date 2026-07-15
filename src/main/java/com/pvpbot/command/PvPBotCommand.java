package com.pvpbot.command;

import com.pvpbot.PvPBotPlugin;
import com.pvpbot.bot.BotManager;
import com.pvpbot.bot.BotSettings;
import com.pvpbot.gui.SettingsGUI;
import com.pvpbot.npc.PvPBotTrait;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PvPBotCommand implements TabExecutor {

    private final BotManager botManager;
    private final BotSettings defaultSettings;
    private final SettingsGUI settingsGUI;

    private static final List<String> SUBCOMMANDS = Arrays.asList("spawn", "remove", "removeall", "settings", "gui");

    public PvPBotCommand(BotManager botManager, BotSettings defaultSettings, SettingsGUI settingsGUI) {
        this.botManager = botManager;
        this.defaultSettings = defaultSettings;
        this.settingsGUI = settingsGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /pvpbot <spawn|remove|removeall|settings|gui> [args]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn":
                return handleSpawn(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "removeall":
                return handleRemoveAll(sender);
            case "settings":
                return handleSettings(sender, args);
            case "gui":
                return handleGUI(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use: spawn, remove, removeall, settings, gui");
                return true;
        }
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can spawn bots.");
            return true;
        }
        String name = args.length >= 2 ? args[1] : "PvPBot";
        NPC spawned = botManager.spawnBot(player.getLocation(), name);
        String finalName = spawned != null ? spawned.getName() : name;
        sender.sendMessage(ChatColor.GREEN + "Spawned bot: " + finalName);
        PvPBotPlugin.broadcastDebug("Player " + player.getName() + " spawned bot '" + finalName + "' at " +
                player.getLocation().getWorld().getName() + " " + player.getLocation().getBlockX() + " " +
                player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
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
        if (sender instanceof Player player) {
            PvPBotPlugin.broadcastDebug("Player " + player.getName() + " removed bot '" + name + "'");
        }
        return true;
    }

    private boolean handleRemoveAll(CommandSender sender) {
        botManager.removeAll();
        sender.sendMessage(ChatColor.GREEN + "All bots removed.");
        return true;
    }

    private boolean handleSettings(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(ChatColor.GOLD + "=== Global Bot Settings ===");
            for (String key : BotSettings.SETTING_KEYS) {
                Object val = defaultSettings.getValue(key);
                sender.sendMessage(ChatColor.YELLOW + key + ChatColor.WHITE + ": " + ChatColor.AQUA + val);
            }
            return true;
        }

        String botName = args[1];
        NPC npc = botManager.getNPC(botName);
        if (npc == null) {
            sender.sendMessage(ChatColor.RED + "Bot not found: " + botName);
            return true;
        }
        PvPBotTrait trait = npc.getTraitNullable(PvPBotTrait.class);
        if (trait == null) {
            sender.sendMessage(ChatColor.RED + "Bot has no trait data: " + botName);
            return true;
        }
        BotSettings botSettings = trait.getSettings();

        if (args.length == 2) {
            sender.sendMessage(ChatColor.GOLD + "=== Bot Settings: " + botName + " ===");
            for (String key : BotSettings.SETTING_KEYS) {
                Object val = botSettings.getValue(key);
                sender.sendMessage(ChatColor.YELLOW + key + ChatColor.WHITE + ": " + ChatColor.AQUA + val);
            }
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /pvpbot settings <bot> <key> <value>");
            return true;
        }

        String key = args[2];
        String valueStr = args[3];
        BotSettings.SettingType type = BotSettings.getType(key);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown setting key: " + key);
            return true;
        }

        try {
            switch (type) {
                case BOOLEAN:
                    boolean bVal = valueStr.equalsIgnoreCase("true") || valueStr.equals("1") || valueStr.equalsIgnoreCase("yes");
                    botSettings.setValue(key, bVal);
                    break;
                case INTEGER:
                    int iVal = Integer.parseInt(valueStr);
                    botSettings.setValue(key, iVal);
                    break;
                case DOUBLE:
                    double dVal = Double.parseDouble(valueStr);
                    botSettings.setValue(key, dVal);
                    break;
            }
            sender.sendMessage(ChatColor.GREEN + "Set " + key + " to " + valueStr + " for " + botName);
            PvPBotPlugin.broadcastDebug("Command: Set setting '" + key + "' to " + valueStr + " for bot '" + botName + "'");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format: " + valueStr);
        }
        return true;
    }

    private boolean handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the GUI.");
            return true;
        }

        NPC target = null;
        if (args.length >= 2) {
            String botName = args[1];
            target = botManager.getNPC(botName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Bot not found: " + botName);
                return true;
            }
            if (!target.isSpawned()) {
                sender.sendMessage(ChatColor.RED + "Bot " + botName + " is not spawned.");
                return true;
            }
        }

        settingsGUI.openGUI(player, target);
        if (target != null) {
            sender.sendMessage(ChatColor.GREEN + "Opening GUI for bot: " + target.getName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Opening global settings GUI.");
        }
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

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            List<String> names = botManager.getActiveNPCs().values().stream()
                    .map(NPC::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());

            if (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("settings")) {
                return names;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                return names;
            }
            return List.of();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("settings")) {
            String partial = args[2].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String key : BotSettings.SETTING_KEYS) {
                if (key.startsWith(partial)) {
                    result.add(key);
                }
            }
            return result;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("settings")) {
            String key = args[2];
            BotSettings.SettingType type = BotSettings.getType(key);
            if (type == BotSettings.SettingType.BOOLEAN) {
                String partial = args[3].toLowerCase();
                List<String> result = new ArrayList<>();
                for (String opt : Arrays.asList("true", "false")) {
                    if (opt.startsWith(partial)) {
                        result.add(opt);
                    }
                }
                return result;
            }
        }

        return List.of();
    }
}
