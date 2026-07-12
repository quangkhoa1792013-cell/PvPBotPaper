package com.pvpbot.command;

import com.pvpbot.bot.BotManager;
import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.CustomBot;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PvPBotCommand implements TabExecutor {

    private static final List<String> SETTING_KEYS = List.of(
            "move-speed", "bhop", "idle", "idle-radius",
            "retreat", "retreat-health", "view-distance", "aim-speed",
            "combat", "auto-target", "target-players", "target-mobs", "target-bots",
            "revenge", "criticals", "attack-cooldown", "melee-range",
            "prefer-sword", "auto-shield", "shield-break", "shield-break-chance",
            "shield-hold-ticks", "shield-raise-ticks", "shield-mace",
            "ranged", "mace", "ranged-min-range", "ranged-optimal-range",
            "ranged-max-range", "bow-draw-ticks", "arrow-prediction",
            "ranged-strafe", "ranged-retreat"
    );

    private static final List<String> BOOLEAN_KEYS = List.of(
            "bhop", "idle", "retreat", "combat", "auto-target",
            "target-players", "target-mobs", "target-bots",
            "revenge", "criticals", "prefer-sword", "auto-shield",
            "shield-break", "shield-mace",
            "ranged", "mace", "arrow-prediction", "ranged-strafe", "ranged-retreat"
    );

    private final BotManager botManager;

    public PvPBotCommand(@NotNull BotManager botManager) {
        this.botManager = botManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "removeall" -> handleRemoveAll(sender);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "settings" -> handleSettings(sender, args);
            case "move" -> handleMove(sender, args);
            case "stop" -> handleStop(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleSpawn(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can spawn bots");
            return;
        }

        String name = args.length >= 2 ? args[1] : null;

        if (name != null && name.length() > 16) {
            sender.sendMessage("§cBot name must be 16 characters or less");
            return;
        }

        Location spawnLocation = player.getLocation();
        CustomBot bot = botManager.spawnBot(spawnLocation, name, player);

        if (bot != null) {
            sender.sendMessage("§aBot '" + bot.getBotName() + "' spawned at your location");
        }
    }

    private void handleRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pvpbot remove <name>");
            return;
        }

        String name = args[1];
        if (botManager.removeBot(name)) {
            sender.sendMessage("§aBot '" + name + "' removed");
        } else {
            sender.sendMessage("§cNo bot found with name '" + name + "'");
        }
    }

    private void handleRemoveAll(@NotNull CommandSender sender) {
        if (!sender.hasPermission("pvpbot.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command");
            return;
        }

        int count = botManager.getBotCount();
        botManager.removeAll();
        sender.sendMessage("§aRemoved " + count + " bot(s)");
    }

    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("pvpbot.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command");
            return;
        }

        try {
            org.bukkit.plugin.java.JavaPlugin plugin =
                    (org.bukkit.plugin.java.JavaPlugin) org.bukkit.Bukkit.getPluginManager().getPlugin("PvPBot");
            if (plugin != null) {
                plugin.reloadConfig();
            }
            botManager.loadConfig();
            sender.sendMessage("§aConfiguration reloaded");
        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload config: " + e.getMessage());
        }
    }

    private void handleList(@NotNull CommandSender sender) {
        List<CustomBot> allBots = new ArrayList<>(botManager.getAllBots());
        if (allBots.isEmpty()) {
            sender.sendMessage("§eNo bots active");
            return;
        }

        sender.sendMessage("§6Active bots (" + allBots.size() + "):");
        for (CustomBot bot : allBots) {
            sender.sendMessage("§7 - §f" + bot.getBotName() + " §7(UUID: " + bot.getUUID() + ")");
        }
    }

    private void handleSettings(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pvpbot settings <botname> [<setting> <value>]");
            sender.sendMessage("§7Settings: " + String.join(", ", SETTING_KEYS));
            return;
        }

        String botName = args[1];
        CustomBot bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage("§cNo bot found with name '" + botName + "'");
            return;
        }

        BotSettings s = bot.getSettings();

        if (args.length == 2) {
            sender.sendMessage("§6Settings for bot '" + bot.getBotName() + "':");
            sender.sendMessage("§7  move-speed: §f" + s.getMoveSpeed());
            sender.sendMessage("§7  bhop: §f" + s.isBhop());
            sender.sendMessage("§7  idle: §f" + s.isIdle());
            sender.sendMessage("§7  idle-radius: §f" + s.getIdleRadius());
            sender.sendMessage("§7  retreat: §f" + s.isRetreat());
            sender.sendMessage("§7  retreat-health: §f" + s.getRetreatHealth());
            sender.sendMessage("§7  view-distance: §f" + s.getViewDistance());
            sender.sendMessage("§7  aim-speed: §f" + s.getAimSpeed());
            sender.sendMessage("§7  combat: §f" + s.isCombat());
            sender.sendMessage("§7  auto-target: §f" + s.isAutoTarget());
            sender.sendMessage("§7  target-players: §f" + s.isTargetPlayers());
            sender.sendMessage("§7  target-mobs: §f" + s.isTargetMobs());
            sender.sendMessage("§7  target-bots: §f" + s.isTargetBots());
            sender.sendMessage("§7  revenge: §f" + s.isRevenge());
            sender.sendMessage("§7  criticals: §f" + s.isCriticals());
            sender.sendMessage("§7  attack-cooldown: §f" + s.getAttackCooldown());
            sender.sendMessage("§7  melee-range: §f" + s.getMeleeRange());
            sender.sendMessage("§7  prefer-sword: §f" + s.isPreferSword());
            sender.sendMessage("§7  auto-shield: §f" + s.isAutoShield());
            sender.sendMessage("§7  shield-break: §f" + s.isShieldBreak());
            sender.sendMessage("§7  shield-break-chance: §f" + s.getShieldBreakChance());
            sender.sendMessage("§7  shield-hold-ticks: §f" + s.getShieldHoldTicks());
            sender.sendMessage("§7  shield-raise-ticks: §f" + s.getShieldRaiseTicks());
            sender.sendMessage("§7  shield-mace: §f" + s.isShieldMace());
            sender.sendMessage("§7  ranged: §f" + s.isRanged());
            sender.sendMessage("§7  mace: §f" + s.isMace());
            sender.sendMessage("§7  ranged-min-range: §f" + s.getRangedMinRange());
            sender.sendMessage("§7  ranged-optimal-range: §f" + s.getRangedOptimalRange());
            sender.sendMessage("§7  ranged-max-range: §f" + s.getRangedMaxRange());
            sender.sendMessage("§7  bow-draw-ticks: §f" + s.getBowDrawTicks());
            sender.sendMessage("§7  arrow-prediction: §f" + s.isArrowPrediction());
            sender.sendMessage("§7  ranged-strafe: §f" + s.isRangedStrafe());
            sender.sendMessage("§7  ranged-retreat: §f" + s.isRangedRetreat());
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot settings <botname> <setting> <value>");
            return;
        }

        String setting = args[2].toLowerCase();
        String value = args[3];

        switch (setting) {
            case "move-speed" -> setDouble(sender, value, s::setMoveSpeed, 0.1, 2.0);
            case "bhop" -> setBool(sender, value, s::setBhop);
            case "idle" -> setBool(sender, value, s::setIdle);
            case "idle-radius" -> setInt(sender, value, s::setIdleRadius, 3, 50);
            case "retreat" -> setBool(sender, value, s::setRetreat);
            case "retreat-health" -> setDouble(sender, value, s::setRetreatHealth, 0.0, 1.0);
            case "view-distance" -> setDouble(sender, value, s::setViewDistance, 5, 128);
            case "aim-speed" -> setDouble(sender, value, s::setAimSpeed, 0.01, 1.0);
            case "combat" -> setBool(sender, value, s::setCombat);
            case "auto-target" -> setBool(sender, value, s::setAutoTarget);
            case "target-players" -> setBool(sender, value, s::setTargetPlayers);
            case "target-mobs" -> setBool(sender, value, s::setTargetMobs);
            case "target-bots" -> setBool(sender, value, s::setTargetBots);
            case "revenge" -> setBool(sender, value, s::setRevenge);
            case "criticals" -> setBool(sender, value, s::setCriticals);
            case "attack-cooldown" -> setInt(sender, value, s::setAttackCooldown, 2, 60);
            case "melee-range" -> setDouble(sender, value, s::setMeleeRange, 2.0, 6.0);
            case "prefer-sword" -> setBool(sender, value, s::setPreferSword);
            case "auto-shield" -> setBool(sender, value, s::setAutoShield);
            case "shield-break" -> setBool(sender, value, s::setShieldBreak);
            case "shield-break-chance" -> setDouble(sender, value, s::setShieldBreakChance, 0, 100);
            case "shield-hold-ticks" -> setInt(sender, value, s::setShieldHoldTicks, 10, 200);
            case "shield-raise-ticks" -> setInt(sender, value, s::setShieldRaiseTicks, 0, 40);
            case "shield-mace" -> setBool(sender, value, s::setShieldMace);
            case "ranged" -> setBool(sender, value, s::setRanged);
            case "mace" -> setBool(sender, value, s::setMace);
            case "ranged-min-range" -> setDouble(sender, value, s::setRangedMinRange, 3.0, 20.0);
            case "ranged-optimal-range" -> setDouble(sender, value, s::setRangedOptimalRange, 10.0, 50.0);
            case "ranged-max-range" -> setDouble(sender, value, s::setRangedMaxRange, 15.0, 100.0);
            case "bow-draw-ticks" -> setInt(sender, value, s::setBowDrawTicks, 5, 100);
            case "arrow-prediction" -> setBool(sender, value, s::setArrowPrediction);
            case "ranged-strafe" -> setBool(sender, value, s::setRangedStrafe);
            case "ranged-retreat" -> setBool(sender, value, s::setRangedRetreat);
            default ->
                sender.sendMessage("§cUnknown setting: " + setting + ". Available: " + String.join(", ", SETTING_KEYS));
        }
    }

    private void setBool(CommandSender sender, String value, java.util.function.Consumer<Boolean> setter) {
        boolean v = parseBoolean(value);
        setter.accept(v);
        sender.sendMessage("§aSet to " + v);
    }

    private void setInt(CommandSender sender, String value, java.util.function.Consumer<Integer> setter, int min, int max) {
        try {
            int v = Integer.parseInt(value);
            setter.accept(v);
            sender.sendMessage("§aSet to " + v + " (clamped " + min + "-" + max + ")");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: " + value);
        }
    }

    private void setDouble(CommandSender sender, String value, java.util.function.Consumer<Double> setter, double min, double max) {
        try {
            double v = Double.parseDouble(value);
            setter.accept(v);
            sender.sendMessage("§aSet to " + v + " (clamped " + min + "-" + max + ")");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: " + value);
        }
    }

    private void handleMove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command");
            return;
        }

        if (args.length < 5) {
            sender.sendMessage("§cUsage: /pvpbot move <botname> <x> <y> <z>");
            return;
        }

        String botName = args[1];
        CustomBot bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage("§cNo bot found with name '" + botName + "'");
            return;
        }

        try {
            int x = Integer.parseInt(args[2]);
            int y = Integer.parseInt(args[3]);
            int z = Integer.parseInt(args[4]);
            bot.moveTo(new net.minecraft.core.BlockPos(x, y, z));
            sender.sendMessage("§aBot '" + botName + "' is moving to (" + x + ", " + y + ", " + z + ")");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates");
        }
    }

    private void handleStop(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pvpbot stop <botname>");
            return;
        }

        String botName = args[1];
        CustomBot bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage("§cNo bot found with name '" + botName + "'");
            return;
        }

        bot.stopMovement();
        sender.sendMessage("§aBot '" + botName + "' stopped");
    }

    private boolean parseBoolean(@NotNull String value) {
        return value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("yes");
    }

    private void sendUsage(@NotNull CommandSender sender) {
        sender.sendMessage("§6=== PvPBot Commands ===");
        sender.sendMessage("§e/pvpbot spawn [name] §7- Spawn a bot at your location");
        sender.sendMessage("§e/pvpbot remove <name> §7- Remove a specific bot");
        sender.sendMessage("§e/pvpbot removeall §7- Remove all bots");
        sender.sendMessage("§e/pvpbot list §7- List active bots");
        sender.sendMessage("§e/pvpbot settings <bot> [<key> <val>] §7- View/change bot settings");
        sender.sendMessage("§e/pvpbot move <bot> <x> <y> <z> §7- Move a bot to a location");
        sender.sendMessage("§e/pvpbot stop <bot> §7- Stop a bot's movement");
        sender.sendMessage("§e/pvpbot reload §7- Reload configuration");
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("spawn", "remove", "list", "settings", "move", "stop"));
            if (sender.hasPermission("pvpbot.admin")) {
                subs.addAll(List.of("removeall", "reload"));
            }
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("remove") || cmd.equals("settings") || cmd.equals("move") || cmd.equals("stop")) {
                return botManager.getBotNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("settings")) {
            return SETTING_KEYS.stream()
                    .filter(k -> k.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("settings")) {
            String key = args[2].toLowerCase();
            if (BOOLEAN_KEYS.contains(key)) {
                return Stream.of("true", "false")
                        .filter(v -> v.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
