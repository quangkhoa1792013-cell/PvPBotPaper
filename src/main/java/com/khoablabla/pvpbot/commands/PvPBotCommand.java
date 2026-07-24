// Phase 2: Core Commands, Mass Spawn, and Tab Completion
// Phase 4: Dynamic /pvpbot settings command tree
package com.khoablabla.pvpbot.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.khoablabla.pvpbot.PvPBot;
import com.khoablabla.pvpbot.config.SettingsRegistry;
import com.khoablabla.pvpbot.listeners.PlayerSimulationListener;
import com.khoablabla.pvpbot.traits.PvPBotTrait;
import com.khoablabla.pvpbot.utils.SafeLocationFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
            sender.sendMessage("Usage: /pvpbot spawn [name|number] [name2...] | /pvpbot remove [name] | /pvpbot removeall | /pvpbot settings [botName] [key] [value]");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "removeall" -> handleRemoveAll(sender);
            case "settings" -> handleSettings(sender, args);
            default -> {
                sender.sendMessage("Unknown subcommand. Usage: /pvpbot spawn [name|number] [name2...] | /pvpbot remove [name] | /pvpbot removeall | /pvpbot settings [botName] [key] [value]");
                yield true;
            }
        };
    }

    private boolean handleSettings(CommandSender sender, String[] args) {
        SettingsRegistry reg = SettingsRegistry.getInstance();

        if (args.length == 1) {
            sender.sendMessage("§6=== PvPBot Global Settings ===");
            for (var entry : reg.getImplementedMeta().entrySet()) {
                String key = entry.getKey();
                Object val = reg.getGlobal(key, Object.class);
                sender.sendMessage("§e" + key + "§f: " + val);
            }
            return true;
        }

        if (args.length == 2) {
            String key = args[1];
            SettingsRegistry.SettingMeta<?> meta = reg.getMeta(key);
            if (meta == null) {
                sender.sendMessage("§cUnknown setting: " + key);
                return true;
            }
            Object val = reg.getGlobal(key, Object.class);
            sender.sendMessage("§e" + key + "§f: " + val + "  §7(default: " + meta.defaultValue() + ")");
            return true;
        }

        if (args.length == 3) {
            String key = args[1];
            String rawVal = args[2];
            Object parsed = reg.parseValue(key, rawVal);
            if (parsed == null) {
                sender.sendMessage("§cInvalid value for '" + key + "'. Check type and range.");
                return true;
            }
            reg.setGlobal(key, parsed);
            reg.saveToConfig();
            sender.sendMessage("§aGlobal §e" + key + "§a set to §f" + parsed);
            return true;
        }

        if (args.length == 4) {
            String botName = args[1];
            String key = args[2];
            String rawVal = args[3];

            NPC npc = findPvPBotByName(botName);
            if (npc == null) {
                sender.sendMessage("§cNo PvPBot named '" + botName + "' found.");
                return true;
            }

            PvPBotTrait trait = npc.getTraitNullable(PvPBotTrait.class);
            if (trait == null) {
                sender.sendMessage("§cThat NPC does not have a PvPBot trait.");
                return true;
            }

            SettingsRegistry.SettingMeta<?> meta = reg.getMeta(key);
            if (meta == null) {
                sender.sendMessage("§cUnknown setting: " + key);
                return true;
            }

            Object parsed = reg.parseValue(key, rawVal);
            if (parsed == null) {
                sender.sendMessage("§cInvalid value for '" + key + "'. Check type and range.");
                return true;
            }

            trait.setLocalSetting(key, parsed);
            sender.sendMessage("§a" + botName + "§a: §e" + key + "§a set to §f" + parsed + " §7(local override)");
            return true;
        }

        sender.sendMessage("§cUsage: /pvpbot settings [botName] [key] [value]");
        return true;
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can spawn bots.");
            return true;
        }

        if (args.length == 1) {
            String name = generateUniqueRealisticName();
            if (spawnSingleBot(player, name)) {
                broadcastJoin(name);
                player.sendMessage("Spawned PvPBot '" + name + "'");
            }
            return true;
        }

        if (args.length == 2) {
            try {
                int count = Integer.parseInt(args[1]);
                count = Math.max(1, Math.min(50, count));
                int spawned = 0;
                for (int i = 0; i < count; i++) {
                    String name = generateUniqueRealisticName();
                    if (spawnSingleBot(player, name)) {
                        spawned++;
                        broadcastJoin(name);
                    }
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
                if (spawnSingleBot(player, name)) {
                    broadcastJoin(name);
                    player.sendMessage("Spawned PvPBot '" + name + "'");
                }
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
            if (spawnSingleBot(player, name)) {
                spawned++;
                broadcastJoin(name);
            }
        }
        player.sendMessage("Spawned " + spawned + " custom PvPBot(s).");
        return true;
    }

    private boolean spawnSingleBot(Player player, String name) {
        var safeLocation = SafeLocationFinder.findSafeLocation(player.getLocation());
        if (safeLocation == null) return false;

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
        npc.data().set(NPC.Metadata.REMOVE_FROM_PLAYERLIST, false);
        npc.data().set(NPC.Metadata.REMOVE_FROM_TABLIST, false);
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, true);
        npc.addTrait(PvPBotTrait.class);
        npc.getOrAddTrait(net.citizensnpcs.trait.Gravity.class);
        if (npc.hasTrait(net.citizensnpcs.trait.LookClose.class)) {
            npc.getOrAddTrait(net.citizensnpcs.trait.LookClose.class).lookClose(false);
        }
        npc.getNavigator().getDefaultParameters()
            .distanceMargin(1.0)
            .pathDistanceMargin(1.0)
            .attackRange(1.0);
        if (!npc.spawn(safeLocation)) {
            npc.destroy();
            return false;
        }
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
        String[] prefixes = {"Epic", "Dark", "Swift", "Mega", "Pro", "Hyper", "Crazy", "Shadow",
                "Light", "Ghost", "Silent", "Iron", "Steel", "Frost", "Blaze", "Wild",
                "Night", "Fire", "Ice", "Alpha", "Omega"};
        String[] suffixes = {"Slayer", "Hunter", "Gamer", "Knight", "Viper", "Rider", "Seeker",
                "Runner", "Glider", "Slasher", "Warrior", "Walker", "Brawler", "Master",
                "Fighter", "Sniper", "Wolf", "Hawk", "Eagle", "Fox"};

        String name;
        int attempts = 0;
        do {
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String suffix = suffixes[random.nextInt(suffixes.length)];
            String separator = random.nextBoolean() ? "_" : "";
            String digits = String.valueOf(random.nextInt(9990) + 10);
            boolean lowercase = random.nextBoolean();

            String raw = prefix + separator + suffix + digits;
            name = lowercase ? raw.toLowerCase() : raw;

            if (name.length() > 16) name = name.substring(0, 16);
            if (name.length() < 3) name = "Bot" + digits;

            attempts++;
        } while (isNameTaken(name) && attempts < 100);

        if (isNameTaken(name)) {
            name = (name + "_" + System.currentTimeMillis());
            if (name.length() > 16) name = name.substring(0, 16);
        }

        return name;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String name = args[1];
            NPC npc = findPvPBotByName(name);
            if (npc == null) {
                sender.sendMessage("§c[PvPBot] No PvPBot named '" + name + "' found.");
                return true;
            }
            PlayerSimulationListener.cancelRespawn(npc.getId());
            npc.destroy();
            sender.sendMessage("Removed PvPBot '" + name + "'");
            return true;
        }

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

        PlayerSimulationListener.cancelRespawn(npc.getId());
        npc.destroy();
        sender.sendMessage("Removed PvPBot (ID: " + npc.getId() + ")");
        return true;
    }

    private boolean handleRemoveAll(CommandSender sender) {
        List<NPC> toRemove = new ArrayList<>();
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(PvPBotTrait.class)) {
                toRemove.add(npc);
            }
        }

        for (NPC npc : toRemove) {
            PlayerSimulationListener.cancelRespawn(npc.getId());
        }

        int total = toRemove.size();
        if (total <= 20) {
            for (NPC npc : toRemove) {
                npc.destroy();
            }
            sender.sendMessage("Removed " + total + " PvPBot(s).");
            return true;
        }

        sender.sendMessage("§a[PvPBot] Initiating safe batch deletion for " + total + " bots (20 bots per 5 ticks)...");

        final String senderName = sender.getName();
        final int[] index = {0};
        new BukkitRunnable() {
            @Override
            public void run() {
                int end = Math.min(index[0] + 20, total);
                for (; index[0] < end; index[0]++) {
                    toRemove.get(index[0]).destroy();
                }
                if (index[0] >= total) {
                    cancel();
                    Player player = Bukkit.getPlayerExact(senderName);
                    if (player != null) {
                        player.sendMessage("§e[PvPBot] Safely cleared all " + total + " bots in batches.");
                    } else {
                        Bukkit.getLogger().info("[PvPBot] Safely cleared all " + total + " bots in batches (sender offline).");
                    }
                }
            }
        }.runTaskTimer(JavaPlugin.getPlugin(PvPBot.class), 0L, 5L);

        return true;
    }

    private NPC findPvPBotByName(String name) {
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(PvPBotTrait.class) && npc.getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }
        return null;
    }

    private void broadcastJoin(String name) {
        Bukkit.getServer().broadcast(Component.text(name + " joined the game", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return List.of("spawn", "remove", "removeall", "settings").stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("settings")) {
                String partial = args[1].toLowerCase();
                return SettingsRegistry.getInstance().getImplementedMeta().keySet().stream()
                        .filter(k -> k.startsWith(partial))
                        .toList();
            }
            return List.of();
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("settings")) {
                SettingsRegistry.SettingMeta<?> meta = SettingsRegistry.getInstance().getMeta(args[1]);
                if (meta != null && meta.type() == Boolean.class) {
                    String partial = args[2].toLowerCase();
                    return List.of("true", "false").stream()
                            .filter(s -> s.startsWith(partial))
                            .toList();
                }
            }
            return List.of();
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("settings")) {
                SettingsRegistry.SettingMeta<?> meta = SettingsRegistry.getInstance().getMeta(args[2]);
                if (meta != null && meta.type() == Boolean.class) {
                    String partial = args[3].toLowerCase();
                    return List.of("true", "false").stream()
                            .filter(s -> s.startsWith(partial))
                            .toList();
                }
            }
            return List.of();
        }

        return List.of();
    }
}
