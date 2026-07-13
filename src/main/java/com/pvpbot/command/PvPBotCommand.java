package com.pvpbot.command;

import com.pvpbot.bot.BotManager;
import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.CustomBot;
import com.pvpbot.faction.Faction;
import com.pvpbot.faction.FactionManager;
import com.pvpbot.kit.Kit;
import com.pvpbot.kit.KitManager;
import com.pvpbot.navigation.path.BotPath;
import com.pvpbot.navigation.path.PathManager;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
            "ranged", "mace", "arrow-prediction", "ranged-strafe", "ranged-retreat",
            "auto-armor", "auto-weapon", "auto-eat", "auto-potion", "auto-mend",
            "auto-totem", "totem-priority"
    );

    private static final List<String> KIT_SUBCOMMANDS = List.of(
            "create-kit", "delete-kit", "list", "give-kit", "give-kit-near", "give-kit-near-random"
    );

    private static final List<String> PATH_SUBCOMMANDS = List.of(
            "create", "delete", "add-point", "remove-point", "clear",
            "list", "loop", "walk-type", "show", "start", "stop", "distribute"
    );

    private static final List<String> FACTION_SUBCOMMANDS = List.of(
            "list", "create", "delete", "info", "add", "remove",
            "add-near", "add-all", "hostile", "attack", "give",
            "path", "tp", "kit"
    );

    private static final List<String> FACTION_KIT_SUBCOMMANDS = List.of("give-kit", "give-kit-random");

    private final BotManager botManager;
    private final KitManager kitManager;
    private final PathManager pathManager;
    private final FactionManager factionManager;
    private final com.pvpbot.gui.SettingsGUI settingsGUI;

    public PvPBotCommand(@NotNull BotManager botManager, @NotNull KitManager kitManager,
                         @NotNull PathManager pathManager, @NotNull FactionManager factionManager,
                         @NotNull com.pvpbot.gui.SettingsGUI settingsGUI) {
        this.botManager = botManager;
        this.kitManager = kitManager;
        this.pathManager = pathManager;
        this.factionManager = factionManager;
        this.settingsGUI = settingsGUI;
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
            case "kit" -> handleKit(sender, args);
            case "path" -> handlePath(sender, args);
            case "gui" -> handleGui(sender);
            case "faction" -> handleFaction(sender, args);
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
            sender.sendMessage("§7  auto-armor: §f" + s.isAutoArmor());
            sender.sendMessage("§7  auto-weapon: §f" + s.isAutoWeapon());
            sender.sendMessage("§7  auto-eat: §f" + s.isAutoEat());
            sender.sendMessage("§7  auto-potion: §f" + s.isAutoPotion());
            sender.sendMessage("§7  auto-mend: §f" + s.isAutoMend());
            sender.sendMessage("§7  auto-totem: §f" + s.isAutoTotem());
            sender.sendMessage("§7  totem-priority: §f" + s.isTotemPriority());
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
            case "auto-armor" -> setBool(sender, value, s::setAutoArmor);
            case "auto-weapon" -> setBool(sender, value, s::setAutoWeapon);
            case "auto-eat" -> setBool(sender, value, s::setAutoEat);
            case "auto-potion" -> setBool(sender, value, s::setAutoPotion);
            case "auto-mend" -> setBool(sender, value, s::setAutoMend);
            case "auto-totem" -> setBool(sender, value, s::setAutoTotem);
            case "totem-priority" -> setBool(sender, value, s::setTotemPriority);
            default ->
                sender.sendMessage("§cUnknown setting: " + setting + ". Available: " + String.join(", ", SETTING_KEYS));
        }
    }

    // --- Kit Subcommands ---

    private void handleKit(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pvpbot kit <" + String.join("|", KIT_SUBCOMMANDS) + ">");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create-kit" -> handleCreateKit(sender, args);
            case "delete-kit" -> handleDeleteKit(sender, args);
            case "list" -> handleKitList(sender);
            case "give-kit" -> handleGiveKit(sender, args);
            case "give-kit-near" -> handleGiveKitNear(sender, args);
            case "give-kit-near-random" -> handleGiveKitNearRandom(sender, args);
            default ->
                sender.sendMessage("§cUnknown kit subcommand. Use: " + String.join(", ", KIT_SUBCOMMANDS));
        }
    }

    private void handleCreateKit(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can create kits");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot kit create-kit <name>");
            return;
        }
        String name = args[2];
        if (kitManager.createKit(name, player)) {
            sender.sendMessage("§aKit '" + name + "' created from your inventory");
        } else {
            sender.sendMessage("§cKit '" + name + "' already exists");
        }
    }

    private void handleDeleteKit(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot kit delete-kit <name>");
            return;
        }
        String name = args[2];
        if (kitManager.deleteKit(name)) {
            sender.sendMessage("§aKit '" + name + "' deleted");
        } else {
            sender.sendMessage("§cNo kit found with name '" + name + "'");
        }
    }

    private void handleKitList(@NotNull CommandSender sender) {
        var allKits = kitManager.getAllKits();
        if (allKits.isEmpty()) {
            sender.sendMessage("§eNo kits available");
            return;
        }
        sender.sendMessage("§6Available kits (" + allKits.size() + "):");
        for (Kit kit : allKits) {
            int slots = kit.getItems().size();
            sender.sendMessage("§7 - §f" + kit.getName() + " §7(" + slots + " item slots)");
        }
    }

    private void handleGiveKit(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot kit give-kit <botname> <kitname>");
            return;
        }
        String botName = args[2];
        String kitName = args[3];

        CustomBot bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage("§cNo bot found with name '" + botName + "'");
            return;
        }
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            sender.sendMessage("§cNo kit found with name '" + kitName + "'");
            return;
        }
        kitManager.giveKit(bot, kit);
        sender.sendMessage("§aKit '" + kitName + "' applied to bot '" + botName + "'");
    }

    private void handleGiveKitNear(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot kit give-kit-near <kitname> [radius]");
            return;
        }
        String kitName = args[2];
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            sender.sendMessage("§cNo kit found with name '" + kitName + "'");
            return;
        }
        double radius = 10.0;
        if (args.length >= 4) {
            try {
                radius = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid radius");
                return;
            }
        }

        Location origin = player.getLocation();
        int count = 0;
        for (CustomBot bot : botManager.getAllBots()) {
            org.bukkit.entity.Player bp = bot.getBukkitEntity();
            if (bp != null && bp.getLocation().distance(origin) <= radius) {
                kitManager.giveKit(bot, kit);
                count++;
            }
        }
        sender.sendMessage("§aKit '" + kitName + "' applied to " + count + " bot(s) within " + radius + " blocks");
    }

    private void handleGiveKitNearRandom(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command");
            return;
        }
        if (args.length < 5 || (args.length - 3) % 2 != 0) {
            sender.sendMessage("§cUsage: /pvpbot kit give-kit-near-random <radius> <kit1> <w1>% [<kit2> <w2>% ...]");
            return;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid radius");
            return;
        }

        Map<Kit, Double> weightedKits = new HashMap<>();
        for (int i = 3; i < args.length; i += 2) {
            String kitName = args[i];
            String weightStr = args[i + 1];
            Kit kit = kitManager.getKit(kitName);
            if (kit == null) {
                sender.sendMessage("§cNo kit found with name '" + kitName + "'");
                return;
            }
            double weight;
            try {
                weight = Double.parseDouble(weightStr.replace("%", ""));
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid weight: " + weightStr);
                return;
            }
            weightedKits.put(kit, weight);
        }

        Location origin = player.getLocation();
        List<CustomBot> nearbyBots = new ArrayList<>();
        for (CustomBot bot : botManager.getAllBots()) {
            org.bukkit.entity.Player bp = bot.getBukkitEntity();
            if (bp != null && bp.getLocation().distance(origin) <= radius) {
                nearbyBots.add(bot);
            }
        }

        if (nearbyBots.isEmpty()) {
            sender.sendMessage("§eNo bots found within " + radius + " blocks");
            return;
        }

        kitManager.giveRandomWeightedKit(nearbyBots, weightedKits);
        sender.sendMessage("§aWeighted kits applied to " + nearbyBots.size() + " bot(s)");
    }

    // --- Path Subcommands ---

    private void handlePath(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pvpbot path <" + String.join("|", PATH_SUBCOMMANDS) + ">");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handlePathCreate(sender, args);
            case "delete" -> handlePathDelete(sender, args);
            case "add-point" -> handlePathAddPoint(sender, args);
            case "remove-point" -> handlePathRemovePoint(sender, args);
            case "clear" -> handlePathClear(sender, args);
            case "list" -> handlePathList(sender);
            case "loop" -> handlePathLoop(sender, args);
            case "walk-type" -> handlePathWalkType(sender, args);
            case "show" -> handlePathShow(sender, args);
            case "start" -> handlePathStart(sender, args);
            case "stop" -> handlePathStop(sender, args);
            case "distribute" -> handlePathDistribute(sender, args);
            default ->
                sender.sendMessage("§cUnknown path subcommand. Use: " + String.join(", ", PATH_SUBCOMMANDS));
        }
    }

    private void handlePathCreate(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot path create <name>");
            return;
        }
        String name = args[2];
        if (pathManager.createPath(name)) {
            sender.sendMessage("§aPath '" + name + "' created");
        } else {
            sender.sendMessage("§cPath '" + name + "' already exists");
        }
    }

    private void handlePathDelete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot path delete <name>");
            return;
        }
        String name = args[2];
        if (pathManager.deletePath(name)) {
            sender.sendMessage("§aPath '" + name + "' deleted");
        } else {
            sender.sendMessage("§cNo path found with name '" + name + "'");
        }
    }

    private void handlePathAddPoint(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot path add-point <name>");
            return;
        }
        String name = args[2];
        if (pathManager.addPoint(name, player)) {
            BotPath path = pathManager.getPath(name);
            int count = path != null ? path.getWaypointCount() : 0;
            sender.sendMessage("§aPoint added to path '" + name + "' (now " + count + " points)");
        } else {
            sender.sendMessage("§cNo path found with name '" + name + "'");
        }
    }

    private void handlePathRemovePoint(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot path remove-point <name> [index]");
            return;
        }
        String name = args[2];
        BotPath path = pathManager.getPath(name);
        if (path == null) {
            sender.sendMessage("§cNo path found with name '" + name + "'");
            return;
        }
        int index = path.getWaypointCount() - 1;
        if (args.length >= 4) {
            try {
                index = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid index");
                return;
            }
        }
        if (pathManager.removePoint(name, index)) {
            sender.sendMessage("§aRemoved point " + index + " from path '" + name + "'");
        } else {
            sender.sendMessage("§cInvalid index " + index + " for path '" + name + "'");
        }
    }

    private void handlePathClear(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot path clear <name>");
            return;
        }
        String name = args[2];
        BotPath path = pathManager.getPath(name);
        if (path == null) {
            sender.sendMessage("§cNo path found with name '" + name + "'");
            return;
        }
        path.clearWaypoints();
        pathManager.savePaths();
        sender.sendMessage("§aAll waypoints cleared from path '" + name + "'");
    }

    private void handlePathList(@NotNull CommandSender sender) {
        var allPaths = pathManager.getAllPaths();
        if (allPaths.isEmpty()) {
            sender.sendMessage("§eNo paths available");
            return;
        }
        sender.sendMessage("§6Available paths (" + allPaths.size() + "):");
        for (BotPath path : allPaths) {
            sender.sendMessage("§7 - §f" + path.getName()
                    + " §7(" + path.getWaypointCount() + " pts"
                    + (path.isLoop() ? ", loop" : "")
                    + ", " + path.getWalkType().name().toLowerCase()
                    + (path.isVisible() ? ", visible" : "")
                    + ")");
        }
    }

    private void handlePathLoop(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot path loop <name> <true/false>");
            return;
        }
        String name = args[2];
        boolean loop = parseBoolean(args[3]);
        if (pathManager.setLoop(name, loop)) {
            sender.sendMessage("§aPath '" + name + "' loop set to " + loop);
        } else {
            sender.sendMessage("§cNo path found with name '" + name + "'");
        }
    }

    private void handlePathWalkType(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot path walk-type <name> <walk/sprint/bhop>");
            return;
        }
        String name = args[2];
        String typeStr = args[3].toUpperCase();
        BotPath.WalkType walkType;
        try {
            walkType = BotPath.WalkType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid walk type. Use: walk, sprint, or bhop");
            return;
        }
        if (pathManager.setWalkType(name, walkType)) {
            sender.sendMessage("§aPath '" + name + "' walk type set to " + walkType.name().toLowerCase());
        } else {
            sender.sendMessage("§cNo path found with name '" + name + "'");
        }
    }

    private void handlePathShow(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot path show <name> <true/false>");
            return;
        }
        String name = args[2];
        boolean visible = parseBoolean(args[3]);
        if (pathManager.setVisible(name, visible)) {
            sender.sendMessage("§aPath '" + name + "' visibility set to " + visible);
        } else {
            sender.sendMessage("§cNo path found with name '" + name + "'");
        }
    }

    private void handlePathStart(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot path start <botname> <pathname>");
            return;
        }
        String botName = args[2];
        String pathName = args[3];

        CustomBot bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage("§cNo bot found with name '" + botName + "'");
            return;
        }
        BotPath path = pathManager.getPath(pathName);
        if (path == null) {
            sender.sendMessage("§cNo path found with name '" + pathName + "'");
            return;
        }
        pathManager.assignPathToBot(bot, path);
        sender.sendMessage("§aBot '" + botName + "' started on path '" + pathName + "'");
    }

    private void handlePathStop(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot path stop <botname>");
            return;
        }
        String botName = args[2];
        CustomBot bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage("§cNo bot found with name '" + botName + "'");
            return;
        }
        pathManager.removeBotFromPath(bot);
        sender.sendMessage("§aBot '" + botName + "' removed from path");
    }

    private void handlePathDistribute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot path distribute <pathname>");
            return;
        }
        String pathName = args[2];
        BotPath path = pathManager.getPath(pathName);
        if (path == null) {
            sender.sendMessage("§cNo path found with name '" + pathName + "'");
            return;
        }
        if (path.getWaypointCount() < 2) {
            sender.sendMessage("§cPath '" + pathName + "' needs at least 2 waypoints");
            return;
        }

        List<CustomBot> allBots = new ArrayList<>(botManager.getAllBots());
        if (allBots.isEmpty()) {
            sender.sendMessage("§eNo bots to distribute");
            return;
        }

        pathManager.distributeBots(path, allBots);
        sender.sendMessage("§aDistributed " + allBots.size() + " bot(s) along path '" + pathName + "'");
    }

    // --- Faction Subcommands ---

    private void handleFaction(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pvpbot faction <" + String.join("|", FACTION_SUBCOMMANDS) + ">");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list" -> handleFactionList(sender);
            case "create" -> handleFactionCreate(sender, args);
            case "delete" -> handleFactionDelete(sender, args);
            case "info" -> handleFactionInfo(sender, args);
            case "add" -> handleFactionAdd(sender, args);
            case "remove" -> handleFactionRemove(sender, args);
            case "add-near" -> handleFactionAddNear(sender, args);
            case "add-all" -> handleFactionAddAll(sender, args);
            case "hostile" -> handleFactionHostile(sender, args);
            case "attack" -> handleFactionAttack(sender, args);
            case "give" -> handleFactionGive(sender, args);
            case "path" -> handleFactionPath(sender, args);
            case "tp" -> handleFactionTp(sender, args);
            case "kit" -> handleFactionKit(sender, args);
            default ->
                sender.sendMessage("§cUnknown faction subcommand. Use: " + String.join(", ", FACTION_SUBCOMMANDS));
        }
    }

    private void handleFactionList(@NotNull CommandSender sender) {
        var all = factionManager.getAllFactions();
        if (all.isEmpty()) {
            sender.sendMessage("§eNo factions exist");
            return;
        }
        sender.sendMessage("§6Factions (" + all.size() + "):");
        for (Faction f : all) {
            sender.sendMessage("§7 - §f" + f.getName()
                    + " §7(" + f.getMemberCount() + " members"
                    + (f.isFriendlyFire() ? ", friendly-fire" : "")
                    + (f.getEnemies().isEmpty() ? "" : ", enemies: " + String.join(", ", f.getEnemies()))
                    + ")");
        }
    }

    private void handleFactionCreate(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot faction create <name>");
            return;
        }
        String name = args[2];
        if (factionManager.createFaction(name)) {
            sender.sendMessage("§aFaction '" + name + "' created");
        } else {
            sender.sendMessage("§cFaction '" + name + "' already exists");
        }
    }

    private void handleFactionDelete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot faction delete <name>");
            return;
        }
        String name = args[2];
        if (factionManager.deleteFaction(name)) {
            sender.sendMessage("§aFaction '" + name + "' deleted");
        } else {
            sender.sendMessage("§cNo faction found with name '" + name + "'");
        }
    }

    private void handleFactionInfo(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot faction info <name>");
            return;
        }
        Faction f = factionManager.getFaction(args[2]);
        if (f == null) {
            sender.sendMessage("§cNo faction found with name '" + args[2] + "'");
            return;
        }
        sender.sendMessage("§6Faction '" + f.getName() + "':");
        sender.sendMessage("§7  Members (" + f.getMemberCount() + "):");
        for (UUID uid : f.getMembers()) {
            String name = Bukkit.getOfflinePlayer(uid).getName();
            sender.sendMessage("§7    - §f" + (name != null ? name : uid.toString()));
        }
        if (!f.getEnemies().isEmpty()) {
            sender.sendMessage("§7  Enemies: §f" + String.join(", ", f.getEnemies()));
        }
        sender.sendMessage("§7  Friendly-fire: §f" + f.isFriendlyFire());
    }

    private void handleFactionAdd(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot faction add <faction> <player>");
            return;
        }
        String factionName = args[2];
        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer '" + playerName + "' not found");
            return;
        }
        if (factionManager.addToFaction(factionName, target.getUniqueId())) {
            sender.sendMessage("§aAdded " + playerName + " to faction '" + factionName + "'");
        } else {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
        }
    }

    private void handleFactionRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot faction remove <faction> <player>");
            return;
        }
        String factionName = args[2];
        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer '" + playerName + "' not found");
            return;
        }
        if (factionManager.removeFromFaction(factionName, target.getUniqueId())) {
            sender.sendMessage("§aRemoved " + playerName + " from faction '" + factionName + "'");
        } else {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
        }
    }

    private void handleFactionAddNear(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot faction add-near <faction> [radius]");
            return;
        }
        String factionName = args[2];
        if (!factionManager.factionExists(factionName)) {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
            return;
        }
        double radius = 15.0;
        if (args.length >= 4) {
            try { radius = Double.parseDouble(args[3]); }
            catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid radius");
                return;
            }
        }
        Location origin = player.getLocation();
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getLocation().distance(origin) <= radius) {
                if (factionManager.addToFaction(factionName, p.getUniqueId())) count++;
            }
        }
        for (CustomBot bot : botManager.getAllBots()) {
            org.bukkit.entity.Player bp = bot.getBukkitEntity();
            if (bp != null && bp.getLocation().distance(origin) <= radius) {
                if (factionManager.addToFaction(factionName, bot.getUUID())) count++;
            }
        }
        sender.sendMessage("§aAdded " + count + " member(s) to faction '" + factionName + "'");
    }

    private void handleFactionAddAll(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot faction add-all <faction>");
            return;
        }
        String factionName = args[2];
        if (!factionManager.factionExists(factionName)) {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
            return;
        }
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (factionManager.addToFaction(factionName, p.getUniqueId())) count++;
        }
        for (CustomBot bot : botManager.getAllBots()) {
            if (factionManager.addToFaction(factionName, bot.getUUID())) count++;
        }
        sender.sendMessage("§aAdded " + count + " member(s) to faction '" + factionName + "'");
    }

    private void handleFactionHostile(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot faction hostile <f1> <f2> [true/false]");
            return;
        }
        String f1 = args[2];
        String f2 = args[3];
        boolean hostile = true;
        if (args.length >= 5) hostile = parseBoolean(args[4]);
        if (factionManager.setHostile(f1, f2, hostile)) {
            sender.sendMessage("§aHostile relation between '" + f1 + "' and '" + f2 + "' set to " + hostile);
        } else {
            sender.sendMessage("§cOne or both factions not found");
        }
    }

    private void handleFactionAttack(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot faction attack <faction> <target>");
            return;
        }
        String factionName = args[2];
        String targetName = args[3];
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null) {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer '" + targetName + "' not found");
            return;
        }
        CustomBot targetBot = botManager.getBot(targetName);
        LivingEntity attackTarget = null;
        if (targetBot != null) {
            attackTarget = targetBot;
        } else {
            var craftPlayer = (org.bukkit.craftbukkit.entity.CraftPlayer) targetPlayer;
            attackTarget = craftPlayer.getHandle();
        }
        if (attackTarget == null) {
            sender.sendMessage("§cCould not resolve target entity");
            return;
        }
        final LivingEntity finalTarget = attackTarget;
        int count = 0;
        for (CustomBot bot : botManager.getAllBots()) {
            if (faction.hasMember(bot.getUUID())) {
                bot.setCombatTarget(finalTarget);
                count++;
            }
        }
        sender.sendMessage("§a" + count + " bot(s) in faction '" + factionName + "' now attacking '" + targetName + "'");
    }

    private void handleFactionGive(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot faction give <faction> <item>");
            return;
        }
        String factionName = args[2];
        String itemName = args[3];
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null) {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
            return;
        }
        ItemStack givenItem = player.getInventory().getItemInMainHand().clone();
        if (givenItem == null || givenItem.getType().isAir()) {
            sender.sendMessage("§cYou must hold an item in your main hand");
            return;
        }
        int count = 0;
        for (CustomBot bot : botManager.getAllBots()) {
            if (faction.hasMember(bot.getUUID())) {
                var nmsStack = net.minecraft.world.item.ItemStack.fromBukkitCopy(givenItem);
                bot.getInventory().add(nmsStack);
                count++;
            }
        }
        sender.sendMessage("§aGave " + givenItem.getType().name() + " x" + givenItem.getAmount()
                + " to " + count + " bot(s) in faction '" + factionName + "'");
    }

    private void handleFactionPath(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot faction path start|stop <faction> [pathname]");
            return;
        }
        String action = args[2].toLowerCase();
        String factionName = args[3];
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null) {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
            return;
        }
        switch (action) {
            case "start" -> {
                if (args.length < 5) {
                    sender.sendMessage("§cUsage: /pvpbot faction path start <faction> <pathname>");
                    return;
                }
                String pathName = args[4];
                BotPath path = pathManager.getPath(pathName);
                if (path == null) {
                    sender.sendMessage("§cNo path found with name '" + pathName + "'");
                    return;
                }
                int count = 0;
                for (CustomBot bot : botManager.getAllBots()) {
                    if (faction.hasMember(bot.getUUID())) {
                        pathManager.assignPathToBot(bot, path);
                        count++;
                    }
                }
                sender.sendMessage("§a" + count + " bot(s) in faction '" + factionName + "' started on path '" + pathName + "'");
            }
            case "stop" -> {
                int count = 0;
                for (CustomBot bot : botManager.getAllBots()) {
                    if (faction.hasMember(bot.getUUID())) {
                        pathManager.removeBotFromPath(bot);
                        count++;
                    }
                }
                sender.sendMessage("§a" + count + " bot(s) in faction '" + factionName + "' stopped from path");
            }
            default ->
                sender.sendMessage("§cUsage: /pvpbot faction path start|stop <faction> [pathname]");
        }
    }

    private void handleFactionTp(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pvpbot faction tp <faction> <x y z|player>");
            return;
        }
        String factionName = args[2];
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null) {
            sender.sendMessage("§cNo faction found with name '" + factionName + "'");
            return;
        }
        Location targetLoc;
        if (args.length >= 5) {
            try {
                double x = Double.parseDouble(args[3]);
                double y = Double.parseDouble(args[4]);
                double z = Double.parseDouble(args[5]);
                targetLoc = new Location(sender instanceof Player p ? p.getWorld() : null, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid coordinates");
                return;
            }
        } else {
            Player tpTarget = Bukkit.getPlayer(args[3]);
            if (tpTarget == null) {
                sender.sendMessage("§cPlayer '" + args[3] + "' not found");
                return;
            }
            targetLoc = tpTarget.getLocation();
        }
        if (targetLoc.getWorld() == null) {
            sender.sendMessage("§cInvalid target location");
            return;
        }
        factionManager.teleportFactionGradually(faction, targetLoc);
        sender.sendMessage("§aTeleporting " + faction.getMemberCount() + " member(s) of '" + factionName + "'");
    }

    private void handleFactionKit(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pvpbot faction kit give-kit|give-kit-random <faction> ...");
            return;
        }
        String action = args[2].toLowerCase();
        if (action.equals("give-kit")) {
            if (args.length < 5) {
                sender.sendMessage("§cUsage: /pvpbot faction kit give-kit <faction> <kit>");
                return;
            }
            String factionName = args[3];
            String kitName = args[4];
            Faction faction = factionManager.getFaction(factionName);
            if (faction == null) {
                sender.sendMessage("§cNo faction found with name '" + factionName + "'");
                return;
            }
            Kit kit = kitManager.getKit(kitName);
            if (kit == null) {
                sender.sendMessage("§cNo kit found with name '" + kitName + "'");
                return;
            }
            int count = 0;
            for (CustomBot bot : botManager.getAllBots()) {
                if (faction.hasMember(bot.getUUID())) {
                    kitManager.giveKit(bot, kit);
                    count++;
                }
            }
            sender.sendMessage("§aKit '" + kitName + "' applied to " + count + " bot(s) in faction '" + factionName + "'");
        } else if (action.equals("give-kit-random")) {
            if (args.length < 6 || (args.length - 4) % 2 != 0) {
                sender.sendMessage("§cUsage: /pvpbot faction kit give-kit-random <faction> <kit1> <w1>% [<kit2> <w2>% ...]");
                return;
            }
            String factionName = args[3];
            Faction faction = factionManager.getFaction(factionName);
            if (faction == null) {
                sender.sendMessage("§cNo faction found with name '" + factionName + "'");
                return;
            }
            Map<Kit, Double> weightedKits = new HashMap<>();
            for (int i = 4; i < args.length; i += 2) {
                String kn = args[i];
                String ws = args[i + 1];
                Kit k = kitManager.getKit(kn);
                if (k == null) {
                    sender.sendMessage("§cNo kit found with name '" + kn + "'");
                    return;
                }
                try {
                    weightedKits.put(k, Double.parseDouble(ws.replace("%", "")));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid weight: " + ws);
                    return;
                }
            }
            List<CustomBot> factionBots = new ArrayList<>();
            for (CustomBot bot : botManager.getAllBots()) {
                if (faction.hasMember(bot.getUUID())) factionBots.add(bot);
            }
            if (factionBots.isEmpty()) {
                sender.sendMessage("§eNo bots in faction '" + factionName + "'");
                return;
            }
            kitManager.giveRandomWeightedKit(factionBots, weightedKits);
            sender.sendMessage("§aWeighted kits applied to " + factionBots.size() + " bot(s) in faction '" + factionName + "'");
        } else {
            sender.sendMessage("§cUnknown faction kit subcommand. Use: give-kit or give-kit-random");
        }
    }

    // --- Utility Methods ---

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

    private void handleGui(@NotNull CommandSender sender) {
        if (!sender.hasPermission("pvpbot.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can open the GUI");
            return;
        }
        settingsGUI.open(player);
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
        sender.sendMessage("§e/pvpbot gui §7- Open configuration dashboard");
        sender.sendMessage("§e/pvpbot kit ... §7- Kit management commands");
        sender.sendMessage("§e/pvpbot path ... §7- Path management commands");
        sender.sendMessage("§e/pvpbot faction ... §7- Faction management commands");
        sender.sendMessage("§6Use /pvpbot <kit|path|faction> for subcommand help");
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("spawn", "remove", "list", "settings", "move", "stop", "kit", "path", "faction"));
            if (sender.hasPermission("pvpbot.admin")) {
                subs.addAll(List.of("removeall", "reload", "gui"));
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
            if (cmd.equals("kit")) {
                return KIT_SUBCOMMANDS.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (cmd.equals("path")) {
                return PATH_SUBCOMMANDS.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (cmd.equals("faction")) {
                return FACTION_SUBCOMMANDS.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String cmd = args[0].toLowerCase();
            String sub = args[1].toLowerCase();
            if (cmd.equals("kit")) {
                if (sub.equals("give-kit")) {
                    return botManager.getBotNames().stream()
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("delete-kit") || sub.equals("give-kit") || sub.equals("give-kit-near")) {
                    return kitManager.getAllKits().stream()
                            .map(Kit::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (cmd.equals("path")) {
                if (sub.equals("delete") || sub.equals("add-point") || sub.equals("clear")
                        || sub.equals("loop") || sub.equals("walk-type") || sub.equals("show")
                        || sub.equals("start") || sub.equals("distribute") || sub.equals("remove-point")) {
                    return pathManager.getAllPaths().stream()
                            .map(BotPath::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("start") || sub.equals("stop")) {
                    return botManager.getBotNames().stream()
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (cmd.equals("faction")) {
                sub = args[1].toLowerCase();
                if (sub.equals("info") || sub.equals("add") || sub.equals("remove")
                        || sub.equals("add-near") || sub.equals("add-all") || sub.equals("delete")
                        || sub.equals("attack") || sub.equals("give") || sub.equals("tp")) {
                    return factionManager.getAllFactions().stream()
                            .map(Faction::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("hostile")) {
                    return factionManager.getAllFactions().stream()
                            .map(Faction::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("path")) {
                    return Stream.of("start", "stop")
                            .filter(s -> s.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("kit")) {
                    return FACTION_KIT_SUBCOMMANDS.stream()
                            .filter(s -> s.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (cmd.equals("settings")) {
                return SETTING_KEYS.stream()
                        .filter(k -> k.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4) {
            String cmd = args[0].toLowerCase();
            String sub = args[1].toLowerCase();
            if (cmd.equals("settings")) {
                String key = args[2].toLowerCase();
                if (BOOLEAN_KEYS.contains(key)) {
                    return Stream.of("true", "false")
                            .filter(v -> v.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (cmd.equals("kit") && sub.equals("give-kit")) {
                return kitManager.getAllKits().stream()
                        .map(Kit::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (cmd.equals("path")) {
                if (sub.equals("loop") || sub.equals("show")) {
                    return Stream.of("true", "false")
                            .filter(v -> v.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("walk-type")) {
                    return Stream.of("walk", "sprint", "bhop")
                            .filter(v -> v.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("start")) {
                    return pathManager.getAllPaths().stream()
                            .map(BotPath::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (cmd.equals("faction")) {
                if (sub.equals("hostile")) {
                    return factionManager.getAllFactions().stream()
                            .map(Faction::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("path")) {
                    return factionManager.getAllFactions().stream()
                            .map(Faction::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("kit")) {
                    return factionManager.getAllFactions().stream()
                            .map(Faction::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("faction")) {
                String sub = args[1].toLowerCase();
                if (sub.equals("path") && args[2].equalsIgnoreCase("start")) {
                    return pathManager.getAllPaths().stream()
                            .map(BotPath::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (sub.equals("kit") && args[2].equalsIgnoreCase("give-kit")) {
                    return kitManager.getAllKits().stream()
                            .map(Kit::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }
}
