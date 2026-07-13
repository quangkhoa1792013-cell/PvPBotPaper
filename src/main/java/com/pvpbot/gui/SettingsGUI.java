package com.pvpbot.gui;

import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.BotManager;
import com.pvpbot.bot.CustomBot;
import com.pvpbot.kit.Kit;
import com.pvpbot.kit.KitManager;
import com.pvpbot.faction.Faction;
import com.pvpbot.faction.FactionManager;
import com.pvpbot.navigation.path.BotPath;
import com.pvpbot.navigation.path.PathManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class SettingsGUI implements Listener {

    private enum Page {
        MAIN, COMBAT, MOVEMENT, RANGED, REALISM, FACTIONS, KITS, KIT_BOT_SELECT, PATHS
    }

    private static final Map<Page, String> TITLES = new HashMap<>();
    private static final Map<Page, Material> BORDERS = new HashMap<>();
    private static final Map<Page, Map<Integer, SettingDef>> PAGE_SETTINGS = new LinkedHashMap<>();
    private static final Map<UUID, String> KIT_SELECT_MAP = new HashMap<>();

    private record SettingDef(String configKey, String displayName, Material icon,
                              SettingType type, double min, double max, double step) {}
    private enum SettingType { BOOLEAN, DOUBLE, INTEGER }

    static {
        TITLES.put(Page.MAIN, "§8PvP Bot Config");
        TITLES.put(Page.COMBAT, "§c⚔ Combat & Shield");
        TITLES.put(Page.MOVEMENT, "§b👟 Movement & B-Hop");
        TITLES.put(Page.RANGED, "§a🏹 Ranged & Mace");
        TITLES.put(Page.REALISM, "§e👁 Realism & Difficulty");
        TITLES.put(Page.FACTIONS, "§f👥 Factions & Groups");
        TITLES.put(Page.KITS, "§6🎒 Kit Manager");
        TITLES.put(Page.KIT_BOT_SELECT, "§6🎒 Select Bot");
        TITLES.put(Page.PATHS, "§b🛤 Path Patrols");

        BORDERS.put(Page.COMBAT, Material.RED_STAINED_GLASS_PANE);
        BORDERS.put(Page.MOVEMENT, Material.BLUE_STAINED_GLASS_PANE);
        BORDERS.put(Page.RANGED, Material.GREEN_STAINED_GLASS_PANE);
        BORDERS.put(Page.REALISM, Material.YELLOW_STAINED_GLASS_PANE);
        BORDERS.put(Page.FACTIONS, Material.WHITE_STAINED_GLASS_PANE);
        BORDERS.put(Page.KITS, Material.ORANGE_STAINED_GLASS_PANE);
        BORDERS.put(Page.PATHS, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        Map<Integer, SettingDef> combat = new LinkedHashMap<>();
        combat.put(0, new SettingDef("bot-settings.combat", "Combat", Material.DIAMOND_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(1, new SettingDef("bot-settings.auto-target", "Auto Target", Material.COMPASS, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(2, new SettingDef("bot-settings.criticals", "Criticals", Material.IRON_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(3, new SettingDef("bot-settings.revenge", "Revenge", Material.BLAZE_POWDER, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(4, new SettingDef("bot-settings.prefer-sword", "Prefer Sword", Material.WOODEN_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(5, new SettingDef("bot-settings.attack-cooldown", "Atk Cooldown", Material.CLOCK, SettingType.INTEGER, 2, 60, 1));
        combat.put(6, new SettingDef("bot-settings.melee-range", "Melee Range", Material.FEATHER, SettingType.DOUBLE, 2.0, 6.0, 0.5));
        combat.put(7, new SettingDef("bot-settings.combat-strafe", "Combat Strafe", Material.LEATHER_BOOTS, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(9, new SettingDef("bot-settings.auto-shield", "Auto Shield", Material.SHIELD, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(10, new SettingDef("bot-settings.shield-break", "Shield Break", Material.STONE_AXE, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(11, new SettingDef("bot-settings.shield-break-chance", "Shield Brk Chance", Material.SHIELD, SettingType.DOUBLE, 0, 100, 5));
        combat.put(12, new SettingDef("bot-settings.shield-hold-ticks", "Shield Hold Ticks", Material.CLOCK, SettingType.INTEGER, 10, 200, 5));
        combat.put(13, new SettingDef("bot-settings.shield-raise-ticks", "Shield Raise Ticks", Material.GOLDEN_SWORD, SettingType.INTEGER, 0, 40, 1));
        combat.put(14, new SettingDef("bot-settings.shield-mace", "Shield vs Mace", Material.MACE, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(15, new SettingDef("bot-settings.auto-armor", "Auto Armor", Material.DIAMOND_CHESTPLATE, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(16, new SettingDef("bot-settings.auto-weapon", "Auto Weapon", Material.DIAMOND_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(17, new SettingDef("bot-settings.auto-totem", "Auto Totem", Material.TOTEM_OF_UNDYING, SettingType.BOOLEAN, 0, 1, 1));
        combat.put(18, new SettingDef("bot-settings.totem-priority", "Totem Priority", Material.TOTEM_OF_UNDYING, SettingType.BOOLEAN, 0, 1, 1));
        PAGE_SETTINGS.put(Page.COMBAT, combat);

        Map<Integer, SettingDef> movement = new LinkedHashMap<>();
        movement.put(0, new SettingDef("bot-settings.move-speed", "Move Speed", Material.IRON_BOOTS, SettingType.DOUBLE, 0.1, 2.0, 0.1));
        movement.put(1, new SettingDef("bot-settings.bhop", "B-Hop", Material.RABBIT_FOOT, SettingType.BOOLEAN, 0, 1, 1));
        movement.put(2, new SettingDef("bot-settings.aim-speed", "Aim Speed", Material.SPECTRAL_ARROW, SettingType.DOUBLE, 0.01, 1.0, 0.05));
        movement.put(3, new SettingDef("bot-settings.retreat", "Retreat", Material.ELYTRA, SettingType.BOOLEAN, 0, 1, 1));
        movement.put(4, new SettingDef("bot-settings.retreat-health", "Retreat Health", Material.GOLDEN_APPLE, SettingType.DOUBLE, 0, 1.0, 0.05));
        movement.put(5, new SettingDef("bot-settings.idle", "Idle", Material.COMPASS, SettingType.BOOLEAN, 0, 1, 1));
        movement.put(6, new SettingDef("bot-settings.idle-radius", "Idle Radius", Material.MAP, SettingType.INTEGER, 3, 50, 1));
        movement.put(7, new SettingDef("bot-settings.view-distance", "View Distance", Material.SPYGLASS, SettingType.INTEGER, 5, 128, 5));
        PAGE_SETTINGS.put(Page.MOVEMENT, movement);

        Map<Integer, SettingDef> ranged = new LinkedHashMap<>();
        ranged.put(0, new SettingDef("bot-settings.ranged", "Ranged", Material.BOW, SettingType.BOOLEAN, 0, 1, 1));
        ranged.put(1, new SettingDef("bot-settings.arrow-prediction", "Arrow Prediction", Material.TIPPED_ARROW, SettingType.BOOLEAN, 0, 1, 1));
        ranged.put(2, new SettingDef("bot-settings.ranged-strafe", "Ranged Strafe", Material.LEATHER_BOOTS, SettingType.BOOLEAN, 0, 1, 1));
        ranged.put(3, new SettingDef("bot-settings.ranged-retreat", "Ranged Retreat", Material.ELYTRA, SettingType.BOOLEAN, 0, 1, 1));
        ranged.put(4, new SettingDef("bot-settings.bow-draw-ticks", "Bow Draw Ticks", Material.CLOCK, SettingType.INTEGER, 5, 100, 1));
        ranged.put(5, new SettingDef("bot-settings.ranged-min-range", "Ranged Min Range", Material.ARROW, SettingType.DOUBLE, 3.0, 20.0, 1));
        ranged.put(6, new SettingDef("bot-settings.ranged-optimal-range", "Ranged Optimal Range", Material.ARROW, SettingType.DOUBLE, 10.0, 50.0, 1));
        ranged.put(7, new SettingDef("bot-settings.ranged-max-range", "Ranged Max Range", Material.ARROW, SettingType.DOUBLE, 15.0, 100.0, 5));
        ranged.put(8, new SettingDef("bot-settings.mace", "Mace Combo", Material.MACE, SettingType.BOOLEAN, 0, 1, 1));
        PAGE_SETTINGS.put(Page.RANGED, ranged);

        Map<Integer, SettingDef> realism = new LinkedHashMap<>();
        realism.put(0, new SettingDef("bot-settings.miss-chance", "Miss Chance %", Material.BARRIER, SettingType.DOUBLE, 0, 100, 5));
        realism.put(1, new SettingDef("bot-settings.mistake-chance", "Mistake Chance %", Material.NETHER_STAR, SettingType.DOUBLE, 0, 100, 5));
        realism.put(2, new SettingDef("bot-settings.profile-lagg-fix", "Profile Lag Fix", Material.ENDER_PEARL, SettingType.BOOLEAN, 0, 1, 1));
        realism.put(3, new SettingDef("bot-settings.auto-eat", "Auto Eat", Material.COOKED_BEEF, SettingType.BOOLEAN, 0, 1, 1));
        realism.put(4, new SettingDef("bot-settings.auto-potion", "Auto Potion", Material.POTION, SettingType.BOOLEAN, 0, 1, 1));
        realism.put(5, new SettingDef("bot-settings.auto-mend", "Auto Mend", Material.EXPERIENCE_BOTTLE, SettingType.BOOLEAN, 0, 1, 1));
        PAGE_SETTINGS.put(Page.REALISM, realism);
    }

    private final JavaPlugin plugin;
    private final BotManager botManager;
    private final BotSettings defaults;
    private final KitManager kitManager;
    private final FactionManager factionManager;
    private final PathManager pathManager;

    public SettingsGUI(JavaPlugin plugin, BotManager botManager, KitManager kitManager,
                       FactionManager factionManager, PathManager pathManager) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.defaults = botManager.getDefaultSettings();
        this.kitManager = kitManager;
        this.factionManager = factionManager;
        this.pathManager = pathManager;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLES.get(Page.MAIN));
        populateMain(inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    public void open(Player player) {
        openMain(player);
    }

    private void populateMain(Inventory inv) {
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, makePane(Material.GRAY_STAINED_GLASS_PANE, "§8│"));
        }
        inv.setItem(10, makePlain(Material.NETHERITE_SWORD, "§c⚔ Combat & Shield",
                List.of("§7Melee, targeting, shield config", "", "§eClick to open")));
        inv.setItem(11, makePlain(Material.IRON_BOOTS, "§b👟 Movement & B-Hop",
                List.of("§7Speed, retreat, idle settings", "", "§eClick to open")));
        inv.setItem(12, makePlain(Material.BOW, "§a🏹 Ranged & Mace",
                List.of("§7Bow, crossbow, mace combo", "", "§eClick to open")));
        inv.setItem(13, makePlain(Material.SPYGLASS, "§e👁 Realism & Difficulty",
                List.of("§7Miss chance, mistake, auto-survival", "", "§eClick to open")));
        inv.setItem(14, makePlain(Material.WHITE_BANNER, "§f👥 Factions & Groups",
                List.of("§7Member list, enemies, info", "", "§eClick to open")));
        inv.setItem(15, makePlain(Material.CHEST, "§6🎒 Kit Manager",
                List.of("§7Create, equip, delete kits", "", "§eClick to open")));
        inv.setItem(16, makePlain(Material.COMPASS, "§b🛤 Path Patrols",
                List.of("§7View paths, waypoint counts", "", "§eClick to open")));
    }

    private void openPage(Player player, Page page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLES.get(page));
        populatePage(inv, page);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    private void populatePage(Inventory inv, Page page) {
        Material border = BORDERS.getOrDefault(page, Material.GRAY_STAINED_GLASS_PANE);
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 9; c++) {
                int slot = r * 9 + c;
                if (r == 0 || r == 5 || c == 0 || c == 8) {
                    inv.setItem(slot, makePane(border, "§8│"));
                }
            }
        }
        inv.setItem(45, makePlain(Material.ARROW, "§c⬅ Back to Main Menu",
                List.of("§7Return to config dashboard")));
        Map<Integer, SettingDef> settings = PAGE_SETTINGS.get(page);
        if (settings != null) {
            for (Map.Entry<Integer, SettingDef> entry : settings.entrySet()) {
                int slot = entry.getKey();
                if (slot >= 0 && slot < 54) {
                    inv.setItem(slot, makeSettingItem(entry.getValue()));
                }
            }
        }
        if (page == Page.FACTIONS) {
            populateFactionsPage(inv);
        } else if (page == Page.KITS) {
            populateKitsPage(inv);
        } else if (page == Page.KIT_BOT_SELECT) {
            populateKitBotSelect(inv);
        } else if (page == Page.PATHS) {
            populatePathsPage(inv);
        }
    }

    private void populateFactionsPage(Inventory inv) {
        int slot = 1;
        for (Faction f : factionManager.getAllFactions()) {
            if (slot >= 44) break;
            List<String> lore = new ArrayList<>();
            lore.add("§7Members: §f" + f.getMemberCount());
            if (!f.getEnemies().isEmpty())
                lore.add("§7Enemies: §f" + String.join(", ", f.getEnemies()));
            lore.add("§7Friendly-fire: §f" + (f.isFriendlyFire() ? "§aON" : "§cOFF"));
            inv.setItem(slot, makePlain(Material.NAME_TAG, "§f" + f.getName(), lore));
            slot++;
        }
        if (slot == 1) {
            inv.setItem(22, makePlain(Material.BARRIER, "§7No factions", List.of("§7Use /pvpbot faction create")));
        }
    }

    private void populateKitsPage(Inventory inv) {
        int slot = 0;
        for (Kit kit : kitManager.getAllKits()) {
            if (slot >= 44) break;
            ItemStack icon = makeKitIcon(kit);
            inv.setItem(slot, icon);
            slot++;
        }
        inv.setItem(53, makePlain(Material.ANVIL, "§a➕ Save Inventory as New Kit",
                List.of("§7Click to save your current inventory", "§7as a new kit")));
    }

    private ItemStack makeKitIcon(Kit kit) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§6" + kit.getName());
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7● Armor:");
        for (int i = 0; i < 4; i++) {
            ItemStack a = kit.getArmor(i);
            String line = "  §8" + (i == 0 ? "Boots" : i == 1 ? "Legs" : i == 2 ? "Chest" : "Head") + ": ";
            if (a != null) {
                String cleaned = a.getType().name().toLowerCase().replace('_', ' ');
                String[] parts = cleaned.split(" ");
                StringBuilder name = new StringBuilder();
                for (String p : parts) {
                    if (!p.isEmpty()) name.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
                }
                line += "§f" + name.toString().trim();
            } else {
                line += "§7empty";
            }
            lore.add(line);
        }
        lore.add("");
        lore.add("§7● Hotbar & Offhand:");
        List<Integer> hotbarSlots = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 40);
        int shown = 0;
        for (int s : hotbarSlots) {
            ItemStack stack = kit.getItem(s);
            if (stack != null) {
                String cleaned = stack.getType().name().toLowerCase().replace('_', ' ');
                String[] parts = cleaned.split(" ");
                StringBuilder name = new StringBuilder();
                for (String p : parts) {
                    if (!p.isEmpty()) name.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
                }
                lore.add("  §f" + name.toString().trim() + " §7x" + stack.getAmount());
                shown++;
                if (shown >= 5) { lore.add("  §8..."); break; }
            }
        }
        if (shown == 0) lore.add("  §7empty");
        lore.add("");
        lore.add("§eLeft-click §7→ Equip to you");
        lore.add("§dRight-click §7→ Give to a bot");
        lore.add("§cShift+Right-click §7→ Delete kit");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void populateKitBotSelect(Inventory inv) {
        for (int i = 0; i < 54; i++) {
            if (i == 45) continue;
            inv.setItem(i, makePane(Material.GRAY_STAINED_GLASS_PANE, "§8│"));
        }
        inv.setItem(45, makePlain(Material.ARROW, "§c⬅ Back to Kit Manager",
                List.of("§7Return to kit list")));
        int slot = 0;
        for (CustomBot bot : botManager.getAllBots()) {
            if (slot >= 44) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + bot.getBotName());
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(bot.getUUID()));
                meta.setLore(List.of("§7Click to equip kit", "§7to this bot"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
            slot++;
        }
        if (slot == 0) {
            inv.setItem(22, makePlain(Material.BARRIER, "§7No bots available",
                    List.of("§7Spawn a bot first")));
        }
    }

    private void populatePathsPage(Inventory inv) {
        int slot = 1;
        for (BotPath path : pathManager.getAllPaths()) {
            if (slot >= 44) break;
            List<String> lore = new ArrayList<>();
            lore.add("§7Waypoints: §f" + path.getWaypointCount());
            lore.add("§7Loop: §f" + (path.isLoop() ? "§aYes" : "§cNo"));
            lore.add("§7Walk type: §f" + path.getWalkType().name().toLowerCase());
            lore.add("§7Visible: §f" + (path.isVisible() ? "§aYes" : "§cNo"));
            inv.setItem(slot, makePlain(Material.FILLED_MAP, "§b" + path.getName(), lore));
            slot++;
        }
        if (slot == 1) {
            inv.setItem(22, makePlain(Material.BARRIER, "§7No paths", List.of("§7Use /pvpbot path create")));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        Page current = null;
        for (Map.Entry<Page, String> e : TITLES.entrySet()) {
            if (e.getValue().equals(title)) { current = e.getKey(); break; }
        }
        if (current == null) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (current == Page.MAIN) {
            handleMainClick(player, slot);
            return;
        }

        if (slot == 45) {
            openMain(player);
            return;
        }

        switch (current) {
            case COMBAT, MOVEMENT, RANGED, REALISM -> handleSettingClick(player, event, current, slot);
            case KITS -> handleKitsClick(player, event, slot);
            case KIT_BOT_SELECT -> handleKitBotSelectClick(player, event, slot);
            default -> {}
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (TITLES.containsValue(title)) {
            event.setCancelled(true);
        }
    }

    private void handleMainClick(Player player, int slot) {
        Page target = switch (slot) {
            case 10 -> Page.COMBAT;
            case 11 -> Page.MOVEMENT;
            case 12 -> Page.RANGED;
            case 13 -> Page.REALISM;
            case 14 -> Page.FACTIONS;
            case 15 -> Page.KITS;
            case 16 -> Page.PATHS;
            default -> null;
        };
        if (target != null) {
            openPage(player, target);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, 1.0f);
        }
    }

    private void handleSettingClick(Player player, InventoryClickEvent event, Page page, int slot) {
        Map<Integer, SettingDef> settings = PAGE_SETTINGS.get(page);
        if (settings == null) return;
        SettingDef def = settings.get(slot);
        if (def == null) return;

        boolean increment = event.isLeftClick();
        boolean decrement = event.isRightClick();

        Object current = getCurrentValue(def.configKey());
        Object newValue;

        if (def.type() == SettingType.BOOLEAN) {
            newValue = !(Boolean) current;
        } else if (increment) {
            newValue = addStep(current, def);
        } else if (decrement) {
            newValue = subStep(current, def);
        } else {
            return;
        }

        applyValue(def.configKey(), newValue);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, 1.0f);
        populatePage(event.getView().getTopInventory(), page);
    }

    private void handleKitsClick(Player player, InventoryClickEvent event, int slot) {
        if (slot == 53) {
            player.closeInventory();
            player.sendMessage("§aType a name in chat to save your inventory as a new kit.");
            player.sendMessage("§7Use: §f/§7pvpbot kit create <name>");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.0f);
            return;
        }
        if (slot < 0 || slot >= kitManager.getAllKits().size()) return;
        Kit kit = new ArrayList<>(kitManager.getAllKits()).get(slot);
        if (kit == null) return;

        if (event.isShiftClick() && (event.isRightClick() || event.getClick().name().contains("SHIFT"))) {
            kitManager.deleteKit(kit.getName());
            player.sendMessage("§cKit '" + kit.getName() + "' deleted.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.5f, 1.0f);
            openPage(player, Page.KITS);
            return;
        }

        if (event.isRightClick()) {
            KIT_SELECT_MAP.put(player.getUniqueId(), kit.getName());
            openPage(player, Page.KIT_BOT_SELECT);
            return;
        }

        kitManager.giveKitToPlayer(kit, player);
        player.sendMessage("§aKit '" + kit.getName() + "' equipped to you.");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 0.5f, 1.0f);
    }

    private void handleKitBotSelectClick(Player player, InventoryClickEvent event, int slot) {
        if (slot < 0 || slot >= botManager.getBotCount()) return;
        CustomBot bot = new ArrayList<>(botManager.getAllBots()).get(slot);
        String kitName = KIT_SELECT_MAP.remove(player.getUniqueId());
        if (kitName == null || bot == null) return;
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit '" + kitName + "' no longer exists.");
            return;
        }
        kitManager.giveKit(bot, kit);
        player.sendMessage("§aKit '" + kitName + "' applied to bot '" + bot.getBotName() + "'.");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 0.5f, 1.0f);
        openPage(player, Page.KITS);
    }

    private ItemStack makeSettingItem(SettingDef def) {
        Object value = getCurrentValue(def.configKey());
        String valStr = formatValue(value);

        Material displayMat = def.type() == SettingType.BOOLEAN
                ? (Boolean.TRUE.equals(value) ? Material.LIME_WOOL : Material.RED_WOOL)
                : def.icon();

        ItemStack item = new ItemStack(displayMat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String status = def.type() == SettingType.BOOLEAN
                ? (Boolean.TRUE.equals(value) ? "§a✔ Enabled" : "§c✘ Disabled")
                : "§e" + valStr;

        meta.setDisplayName("§f" + def.displayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7Value: " + status);
        if (def.type() != SettingType.BOOLEAN) {
            lore.add("§7Range: §e" + formatValue(def.min()) + " §7- §e" + formatValue(def.max()));
            lore.add("§7Step: §e" + def.step());
        }
        lore.add("");
        lore.add("§aLeft-click §7to increase");
        lore.add("§cRight-click §7to decrease");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Object getCurrentValue(String configKey) {
        return switch (configKey) {
            case "bot-settings.combat" -> defaults.isCombat();
            case "bot-settings.auto-target" -> defaults.isAutoTarget();
            case "bot-settings.criticals" -> defaults.isCriticals();
            case "bot-settings.revenge" -> defaults.isRevenge();
            case "bot-settings.prefer-sword" -> defaults.isPreferSword();
            case "bot-settings.attack-cooldown" -> defaults.getAttackCooldown();
            case "bot-settings.melee-range" -> defaults.getMeleeRange();
            case "bot-settings.auto-shield" -> defaults.isAutoShield();
            case "bot-settings.shield-break" -> defaults.isShieldBreak();
            case "bot-settings.shield-break-chance" -> defaults.getShieldBreakChance();
            case "bot-settings.shield-hold-ticks" -> defaults.getShieldHoldTicks();
            case "bot-settings.shield-raise-ticks" -> defaults.getShieldRaiseTicks();
            case "bot-settings.shield-mace" -> defaults.isShieldMace();
            case "bot-settings.move-speed" -> defaults.getMoveSpeed();
            case "bot-settings.bhop" -> defaults.isBhop();
            case "bot-settings.aim-speed" -> defaults.getAimSpeed();
            case "bot-settings.combat-strafe" -> defaults.isCombatStrafe();
            case "bot-settings.retreat" -> defaults.isRetreat();
            case "bot-settings.retreat-health" -> defaults.getRetreatHealth();
            case "bot-settings.idle" -> defaults.isIdle();
            case "bot-settings.idle-radius" -> defaults.getIdleRadius();
            case "bot-settings.view-distance" -> defaults.getViewDistance();
            case "bot-settings.ranged" -> defaults.isRanged();
            case "bot-settings.arrow-prediction" -> defaults.isArrowPrediction();
            case "bot-settings.ranged-strafe" -> defaults.isRangedStrafe();
            case "bot-settings.ranged-retreat" -> defaults.isRangedRetreat();
            case "bot-settings.bow-draw-ticks" -> defaults.getBowDrawTicks();
            case "bot-settings.ranged-min-range" -> defaults.getRangedMinRange();
            case "bot-settings.ranged-optimal-range" -> defaults.getRangedOptimalRange();
            case "bot-settings.ranged-max-range" -> defaults.getRangedMaxRange();
            case "bot-settings.mace" -> defaults.isMace();
            case "bot-settings.miss-chance" -> defaults.getMissChance();
            case "bot-settings.mistake-chance" -> defaults.getMistakeChance();
            case "bot-settings.profile-lagg-fix" -> defaults.isProfileLagFix();
            case "bot-settings.auto-armor" -> defaults.isAutoArmor();
            case "bot-settings.auto-weapon" -> defaults.isAutoWeapon();
            case "bot-settings.auto-eat" -> defaults.isAutoEat();
            case "bot-settings.auto-potion" -> defaults.isAutoPotion();
            case "bot-settings.auto-mend" -> defaults.isAutoMend();
            case "bot-settings.auto-totem" -> defaults.isAutoTotem();
            case "bot-settings.totem-priority" -> defaults.isTotemPriority();
            default -> throw new IllegalArgumentException("Unknown key: " + configKey);
        };
    }

    private void applyValue(String configKey, Object value) {
        switch (configKey) {
            case "bot-settings.combat" -> defaults.setCombat((Boolean) value);
            case "bot-settings.auto-target" -> defaults.setAutoTarget((Boolean) value);
            case "bot-settings.criticals" -> defaults.setCriticals((Boolean) value);
            case "bot-settings.revenge" -> defaults.setRevenge((Boolean) value);
            case "bot-settings.prefer-sword" -> defaults.setPreferSword((Boolean) value);
            case "bot-settings.attack-cooldown" -> defaults.setAttackCooldown((Integer) value);
            case "bot-settings.melee-range" -> defaults.setMeleeRange((Double) value);
            case "bot-settings.auto-shield" -> defaults.setAutoShield((Boolean) value);
            case "bot-settings.shield-break" -> defaults.setShieldBreak((Boolean) value);
            case "bot-settings.shield-break-chance" -> defaults.setShieldBreakChance((Double) value);
            case "bot-settings.shield-hold-ticks" -> defaults.setShieldHoldTicks((Integer) value);
            case "bot-settings.shield-raise-ticks" -> defaults.setShieldRaiseTicks((Integer) value);
            case "bot-settings.shield-mace" -> defaults.setShieldMace((Boolean) value);
            case "bot-settings.move-speed" -> defaults.setMoveSpeed((Double) value);
            case "bot-settings.bhop" -> defaults.setBhop((Boolean) value);
            case "bot-settings.aim-speed" -> defaults.setAimSpeed((Double) value);
            case "bot-settings.combat-strafe" -> defaults.setCombatStrafe((Boolean) value);
            case "bot-settings.retreat" -> defaults.setRetreat((Boolean) value);
            case "bot-settings.retreat-health" -> defaults.setRetreatHealth((Double) value);
            case "bot-settings.idle" -> defaults.setIdle((Boolean) value);
            case "bot-settings.idle-radius" -> defaults.setIdleRadius((Integer) value);
            case "bot-settings.view-distance" -> defaults.setViewDistance((Integer) value);
            case "bot-settings.ranged" -> defaults.setRanged((Boolean) value);
            case "bot-settings.arrow-prediction" -> defaults.setArrowPrediction((Boolean) value);
            case "bot-settings.ranged-strafe" -> defaults.setRangedStrafe((Boolean) value);
            case "bot-settings.ranged-retreat" -> defaults.setRangedRetreat((Boolean) value);
            case "bot-settings.bow-draw-ticks" -> defaults.setBowDrawTicks((Integer) value);
            case "bot-settings.ranged-min-range" -> defaults.setRangedMinRange((Double) value);
            case "bot-settings.ranged-optimal-range" -> defaults.setRangedOptimalRange((Double) value);
            case "bot-settings.ranged-max-range" -> defaults.setRangedMaxRange((Double) value);
            case "bot-settings.mace" -> defaults.setMace((Boolean) value);
            case "bot-settings.miss-chance" -> defaults.setMissChance((Double) value);
            case "bot-settings.mistake-chance" -> defaults.setMistakeChance((Double) value);
            case "bot-settings.profile-lagg-fix" -> defaults.setProfileLagFix((Boolean) value);
            case "bot-settings.auto-armor" -> defaults.setAutoArmor((Boolean) value);
            case "bot-settings.auto-weapon" -> defaults.setAutoWeapon((Boolean) value);
            case "bot-settings.auto-eat" -> defaults.setAutoEat((Boolean) value);
            case "bot-settings.auto-potion" -> defaults.setAutoPotion((Boolean) value);
            case "bot-settings.auto-mend" -> defaults.setAutoMend((Boolean) value);
            case "bot-settings.auto-totem" -> defaults.setAutoTotem((Boolean) value);
            case "bot-settings.totem-priority" -> defaults.setTotemPriority((Boolean) value);
            default -> throw new IllegalArgumentException("Unknown key: " + configKey);
        }
        FileConfiguration config = plugin.getConfig();
        config.set(configKey, value);
        plugin.saveConfig();
    }

    private Object addStep(Object current, SettingDef def) {
        return switch (def.type()) {
            case INTEGER -> {
                Number num = (Number) current;
                int val = (int) Math.min(num.intValue() + (int) def.step(), (int) def.max());
                yield val;
            }
            case DOUBLE -> {
                Number num = (Number) current;
                double val = Math.min(num.doubleValue() + def.step(), def.max());
                val = Math.round(val * 100.0) / 100.0;
                yield val;
            }
            case BOOLEAN -> !(Boolean) current;
        };
    }

    private Object subStep(Object current, SettingDef def) {
        return switch (def.type()) {
            case INTEGER -> {
                Number num = (Number) current;
                int val = (int) Math.max(num.intValue() - (int) def.step(), (int) def.min());
                yield val;
            }
            case DOUBLE -> {
                Number num = (Number) current;
                double val = Math.max(num.doubleValue() - def.step(), def.min());
                val = Math.round(val * 100.0) / 100.0;
                yield val;
            }
            case BOOLEAN -> !(Boolean) current;
        };
    }

    private String formatValue(Object value) {
        if (value instanceof Number n) {
            if (n.doubleValue() == n.intValue()) return String.valueOf(n.intValue());
            return String.format("%.2f", n.doubleValue());
        }
        return String.valueOf(value);
    }

    private ItemStack makePlain(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makePlain(Material mat, String name, String loreLine) {
        return makePlain(mat, name, loreLine != null ? List.of(loreLine) : null);
    }

    private ItemStack makePane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
