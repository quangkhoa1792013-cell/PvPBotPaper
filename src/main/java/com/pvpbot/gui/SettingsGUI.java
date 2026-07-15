package com.pvpbot.gui;

import com.pvpbot.PvPBotPlugin;
import com.pvpbot.bot.BotManager;
import com.pvpbot.bot.BotSettings;
import com.pvpbot.npc.PvPBotTrait;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SettingsGUI implements Listener {

    private final PvPBotPlugin plugin;
    private final BotManager botManager;
    private final Map<UUID, OpenGUI> openGUIs = new HashMap<>();

    enum GUIType {
        MAIN, COMBAT, MOVEMENT, RANGED, REALISM, FACTIONS, KITS, PATHS
    }

    private static class OpenGUI {
        final GUIType type;
        final NPC target;
        OpenGUI(GUIType type, NPC target) {
            this.type = type;
            this.target = target;
        }
    }

    private static final Map<GUIType, String> TITLES = new EnumMap<>(GUIType.class);
    static {
        TITLES.put(GUIType.MAIN, "§0⚔ PvPBot Control Panel");
        TITLES.put(GUIType.COMBAT, "§c⚔ Combat & Shield");
        TITLES.put(GUIType.MOVEMENT, "§9👟 Movement & B-Hop");
        TITLES.put(GUIType.RANGED, "§a🏹 Ranged & Mace Combos");
        TITLES.put(GUIType.REALISM, "§e👁 Realism & Difficulty");
        TITLES.put(GUIType.FACTIONS, "§f👥 Factions & Group Control");
        TITLES.put(GUIType.KITS, "§6🎒 Kit Manager");
        TITLES.put(GUIType.PATHS, "§b🛤 Path Patrols");
    }

    private static final Map<GUIType, Material> BORDER_COLORS = new EnumMap<>(GUIType.class);
    static {
        BORDER_COLORS.put(GUIType.COMBAT, Material.RED_STAINED_GLASS_PANE);
        BORDER_COLORS.put(GUIType.MOVEMENT, Material.BLUE_STAINED_GLASS_PANE);
        BORDER_COLORS.put(GUIType.RANGED, Material.GREEN_STAINED_GLASS_PANE);
        BORDER_COLORS.put(GUIType.REALISM, Material.YELLOW_STAINED_GLASS_PANE);
        BORDER_COLORS.put(GUIType.FACTIONS, Material.WHITE_STAINED_GLASS_PANE);
        BORDER_COLORS.put(GUIType.KITS, Material.ORANGE_STAINED_GLASS_PANE);
        BORDER_COLORS.put(GUIType.PATHS, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
    }

    private static final List<String> COMBAT_KEYS = Arrays.asList(
        "combat", "revenge", "auto-target", "criticals",
        "target-players", "target-mobs", "target-bots",
        "prefer-sword", "attack-cooldown", "melee-range",
        "auto-shield", "shield-break", "shield-break-chance",
        "shield-hold-ticks", "shield-raise-ticks", "shield-mace"
    );

    private static final List<String> MOVEMENT_KEYS = Arrays.asList(
        "move-speed", "bhop", "idle", "idle-radius",
        "view-distance", "retreat"
    );

    private static final List<String> RANGED_KEYS = Arrays.asList(
        "ranged", "mace", "ranged-min-range", "ranged-optimal-range",
        "ranged-max-range", "bow-draw-ticks", "arrow-prediction",
        "ranged-strafe", "ranged-retreat"
    );

    private static final List<String> REALISM_KEYS = Arrays.asList(
        "miss-chance", "mistake-chance", "aim-speed",
        "show-in-tab", "bot-leave-on-death", "debug"
    );

    private static final Map<GUIType, List<String>> PAGE_KEYS = new EnumMap<>(GUIType.class);
    static {
        PAGE_KEYS.put(GUIType.COMBAT, COMBAT_KEYS);
        PAGE_KEYS.put(GUIType.MOVEMENT, MOVEMENT_KEYS);
        PAGE_KEYS.put(GUIType.RANGED, RANGED_KEYS);
        PAGE_KEYS.put(GUIType.REALISM, REALISM_KEYS);
    }

    public SettingsGUI(PvPBotPlugin plugin, BotManager botManager) {
        this.plugin = plugin;
        this.botManager = botManager;
    }

    public void openGUI(Player player, NPC target) {
        PvPBotPlugin.broadcastDebug("Player " + player.getName() + " opened GUI: " + (target == null ? "GLOBAL" : "NPC-" + target.getName()));
        openMainMenu(player, target);
    }

    private void openMainMenu(Player player, NPC target) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLES.get(GUIType.MAIN));
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i >= 10 && i <= 16) continue;
            inv.setItem(i, border);
        }
        inv.setItem(10, createItem(Material.NETHERITE_SWORD, "§c⚔ Combat & Shield",
                "§7Configure combat and shield settings"));
        inv.setItem(11, createItem(Material.IRON_BOOTS, "§9👟 Movement & B-Hop",
                "§7Configure movement and B-Hop settings"));
        inv.setItem(12, createItem(Material.BOW, "§a🏹 Ranged & Mace Combos",
                "§7Configure ranged and mace combo settings"));
        inv.setItem(13, createItem(Material.SPYGLASS, "§e👁 Realism & Difficulty",
                "§7Configure realism and difficulty settings"));
        inv.setItem(14, createItem(Material.WHITE_BANNER, "§f👥 Factions & Group Control",
                "§7Manage factions and group control", "§cComing soon..."));
        inv.setItem(15, createItem(Material.CHEST, "§6🎒 Kit Manager",
                "§7Manage bot kits and loadouts", "§cComing soon..."));
        inv.setItem(16, createItem(Material.COMPASS, "§b🛤 Path Patrols",
                "§7Configure path patrol routes", "§cComing soon..."));
        openGUIs.put(player.getUniqueId(), new OpenGUI(GUIType.MAIN, target));
        player.openInventory(inv);
    }

    private void openSubMenu(Player player, GUIType page, NPC target) {
        if (page == GUIType.MAIN) {
            openMainMenu(player, target);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLES.get(page));
        Material borderMat = BORDER_COLORS.getOrDefault(page, Material.GRAY_STAINED_GLASS_PANE);
        ItemStack border = createItem(borderMat, " ");
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                inv.setItem(i, border);
            }
        }
        inv.setItem(45, createItem(Material.ARROW, "§c⬅ Back to Main Menu",
                "§7Click to return to the main menu"));
        List<String> keys = PAGE_KEYS.getOrDefault(page, Collections.emptyList());
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                              28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        BotSettings settings = getSettings(target);
        for (int i = 0; i < keys.size() && i < contentSlots.length; i++) {
            inv.setItem(contentSlots[i], createSettingItem(keys.get(i), settings));
        }
        openGUIs.put(player.getUniqueId(), new OpenGUI(page, target));
        player.openInventory(inv);
    }

    private BotSettings getSettings(NPC target) {
        if (target != null && target.isSpawned()) {
            PvPBotTrait trait = target.getTraitNullable(PvPBotTrait.class);
            if (trait != null) {
                return trait.getSettings();
            }
        }
        return plugin.getDefaultSettings();
    }

    private ItemStack createSettingItem(String key, BotSettings settings) {
        Object value = settings.getValue(key);
        BotSettings.SettingType type = BotSettings.getType(key);
        String displayName = "§f" + formatKey(key);
        List<String> lore = new ArrayList<>();
        if (type == BotSettings.SettingType.BOOLEAN) {
            boolean boolVal = value instanceof Boolean && (Boolean) value;
            Material mat = boolVal ? Material.LIME_WOOL : Material.RED_WOOL;
            lore.add(boolVal ? "§a§lENABLED" : "§c§lDISABLED");
            lore.add("§7Click to toggle");
            return createItem(mat, displayName, lore.toArray(new String[0]));
        } else {
            Material mat = Material.PAPER;
            lore.add("§eValue: §f" + value);
            if (type == BotSettings.SettingType.DOUBLE) {
                lore.add("§7Left-click: +0.5  |  Right-click: -0.5");
            } else {
                lore.add("§7Left-click: +1  |  Right-click: -1");
            }
            lore.add("§7Shift + click: ±10");
            return createItem(mat, displayName, lore.toArray(new String[0]));
        }
    }

    private String formatKey(String key) {
        String[] parts = key.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            if (part.length() > 0) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        OpenGUI gui = openGUIs.get(player.getUniqueId());
        if (gui == null) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;
        boolean rightClick = e.isRightClick();
        if (gui.type == GUIType.MAIN) {
            handleMainClick(player, slot, gui.target);
        } else {
            handleSubClick(player, slot, gui, rightClick);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, 1.0f);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        openGUIs.remove(e.getPlayer().getUniqueId());
    }

    private void handleMainClick(Player player, int slot, NPC target) {
        GUIType page;
        switch (slot) {
            case 10: page = GUIType.COMBAT; break;
            case 11: page = GUIType.MOVEMENT; break;
            case 12: page = GUIType.RANGED; break;
            case 13: page = GUIType.REALISM; break;
            case 14: page = GUIType.FACTIONS; break;
            case 15: page = GUIType.KITS; break;
            case 16: page = GUIType.PATHS; break;
            default: return;
        }
        if (page == GUIType.FACTIONS || page == GUIType.KITS || page == GUIType.PATHS) {
            player.sendMessage("§cThis feature is coming soon!");
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> openSubMenu(player, page, target), 1L);
    }

    private void handleSubClick(Player player, int slot, OpenGUI gui, boolean rightClick) {
        if (slot == 45) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> openMainMenu(player, gui.target), 1L);
            return;
        }
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                              28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int index = -1;
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        List<String> keys = PAGE_KEYS.getOrDefault(gui.type, Collections.emptyList());
        if (index >= keys.size()) return;
        String key = keys.get(index);
        BotSettings settings = getSettings(gui.target);
        BotSettings.SettingType type = BotSettings.getType(key);
        boolean shift = player.isSneaking();
        if (type == BotSettings.SettingType.BOOLEAN) {
            Object val = settings.getValue(key);
            boolean oldVal = val instanceof Boolean && (Boolean) val;
            settings.setValue(key, !oldVal);
            PvPBotPlugin.broadcastDebug("GUI: Modified setting '" + key + "': " + oldVal + " -> " + !oldVal);
        } else if (type == BotSettings.SettingType.INTEGER) {
            Object val = settings.getValue(key);
            int current = val instanceof Number ? ((Number) val).intValue() : 0;
            int delta = shift ? (rightClick ? -10 : 10) : (rightClick ? -1 : 1);
            settings.setValue(key, current + delta);
            PvPBotPlugin.broadcastDebug("GUI: Modified setting '" + key + "': " + current + " -> " + (current + delta));
        } else if (type == BotSettings.SettingType.DOUBLE) {
            Object val = settings.getValue(key);
            double current = val instanceof Number ? ((Number) val).doubleValue() : 0.0;
            double delta = shift ? (rightClick ? -5.0 : 5.0) : (rightClick ? -0.5 : 0.5);
            settings.setValue(key, current + delta);
            PvPBotPlugin.broadcastDebug("GUI: Modified setting '" + key + "': " + current + " -> " + (current + delta));
        }
        if (gui.target == null) {
            plugin.saveDefaultSettings();
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> openSubMenu(player, gui.type, gui.target), 1L);
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines.length > 0) {
                meta.setLore(Arrays.asList(loreLines));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
