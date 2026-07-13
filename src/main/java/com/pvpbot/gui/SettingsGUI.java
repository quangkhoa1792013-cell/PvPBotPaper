package com.pvpbot.gui;

import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.BotManager;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsGUI implements Listener {

    private static final String TITLE = "§8PvP Bot Config Dashboard";
    private static final int SIZE = 54;

    private static final Map<Integer, SettingDef> SLOT_MAP = new LinkedHashMap<>();

    private record SettingDef(String configKey, String displayName, Material icon,
                              SettingType type, double min, double max, double step) {}
    private enum SettingType { BOOLEAN, DOUBLE, INTEGER }

    static {
        SLOT_MAP.put(9,  new SettingDef("bot-settings.combat", "Combat", Material.DIAMOND_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(10, new SettingDef("bot-settings.auto-target", "Auto Target", Material.COMPASS, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(11, new SettingDef("bot-settings.criticals", "Criticals", Material.IRON_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(12, new SettingDef("bot-settings.revenge", "Revenge", Material.BLAZE_POWDER, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(13, new SettingDef("bot-settings.prefer-sword", "Prefer Sword", Material.WOODEN_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(14, new SettingDef("bot-settings.attack-cooldown", "Atk Cooldown", Material.CLOCK, SettingType.INTEGER, 2, 60, 1));
        SLOT_MAP.put(15, new SettingDef("bot-settings.melee-range", "Melee Range", Material.FEATHER, SettingType.DOUBLE, 2.0, 6.0, 0.5));

        SLOT_MAP.put(18, new SettingDef("bot-settings.auto-shield", "Auto Shield", Material.SHIELD, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(19, new SettingDef("bot-settings.shield-break", "Shield Break", Material.STONE_AXE, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(20, new SettingDef("bot-settings.shield-break-chance", "Shield Brk Chance", Material.SHIELD, SettingType.DOUBLE, 0, 100, 5));
        SLOT_MAP.put(21, new SettingDef("bot-settings.shield-hold-ticks", "Shield Hold Ticks", Material.CLOCK, SettingType.INTEGER, 10, 200, 5));
        SLOT_MAP.put(22, new SettingDef("bot-settings.shield-raise-ticks", "Shield Raise Ticks", Material.GOLDEN_SWORD, SettingType.INTEGER, 0, 40, 1));
        SLOT_MAP.put(23, new SettingDef("bot-settings.shield-mace", "Shield vs Mace", Material.MACE, SettingType.BOOLEAN, 0, 1, 1));

        SLOT_MAP.put(24, new SettingDef("bot-settings.move-speed", "Move Speed", Material.IRON_BOOTS, SettingType.DOUBLE, 0.1, 2.0, 0.1));
        SLOT_MAP.put(25, new SettingDef("bot-settings.bhop", "B-Hop", Material.RABBIT_FOOT, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(26, new SettingDef("bot-settings.aim-speed", "Aim Speed", Material.SPECTRAL_ARROW, SettingType.DOUBLE, 0.01, 1.0, 0.05));

        SLOT_MAP.put(27, new SettingDef("bot-settings.combat-strafe", "Combat Strafe", Material.LEATHER_BOOTS, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(28, new SettingDef("bot-settings.retreat", "Retreat", Material.ELYTRA, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(29, new SettingDef("bot-settings.retreat-health", "Retreat Health", Material.GOLDEN_APPLE, SettingType.DOUBLE, 0, 1.0, 0.05));
        SLOT_MAP.put(30, new SettingDef("bot-settings.idle", "Idle", Material.COMPASS, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(31, new SettingDef("bot-settings.idle-radius", "Idle Radius", Material.MAP, SettingType.INTEGER, 3, 50, 1));
        SLOT_MAP.put(32, new SettingDef("bot-settings.view-distance", "View Distance", Material.SPYGLASS, SettingType.INTEGER, 5, 128, 5));

        SLOT_MAP.put(33, new SettingDef("bot-settings.ranged", "Ranged", Material.BOW, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(34, new SettingDef("bot-settings.arrow-prediction", "Arrow Prediction", Material.TIPPED_ARROW, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(35, new SettingDef("bot-settings.ranged-strafe", "Ranged Strafe", Material.LEATHER_BOOTS, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(36, new SettingDef("bot-settings.ranged-retreat", "Ranged Retreat", Material.ELYTRA, SettingType.BOOLEAN, 0, 1, 1));

        SLOT_MAP.put(37, new SettingDef("bot-settings.bow-draw-ticks", "Bow Draw Ticks", Material.CLOCK, SettingType.INTEGER, 5, 100, 1));
        SLOT_MAP.put(38, new SettingDef("bot-settings.ranged-min-range", "Ranged Min Range", Material.ARROW, SettingType.DOUBLE, 3.0, 20.0, 1));
        SLOT_MAP.put(39, new SettingDef("bot-settings.ranged-optimal-range", "Ranged Optimal Range", Material.ARROW, SettingType.DOUBLE, 10.0, 50.0, 1));
        SLOT_MAP.put(40, new SettingDef("bot-settings.ranged-max-range", "Ranged Max Range", Material.ARROW, SettingType.DOUBLE, 15.0, 100.0, 5));

        SLOT_MAP.put(41, new SettingDef("bot-settings.mace", "Mace Combo", Material.MACE, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(42, new SettingDef("bot-settings.miss-chance", "Miss Chance %", Material.BARRIER, SettingType.DOUBLE, 0, 100, 5));
        SLOT_MAP.put(43, new SettingDef("bot-settings.mistake-chance", "Mistake Chance %", Material.NETHER_STAR, SettingType.DOUBLE, 0, 100, 5));
        SLOT_MAP.put(44, new SettingDef("bot-settings.profile-lagg-fix", "Profile Lag Fix", Material.ENDER_PEARL, SettingType.BOOLEAN, 0, 1, 1));

        SLOT_MAP.put(45, new SettingDef("bot-settings.auto-armor", "Auto Armor", Material.DIAMOND_CHESTPLATE, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(46, new SettingDef("bot-settings.auto-weapon", "Auto Weapon", Material.DIAMOND_SWORD, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(47, new SettingDef("bot-settings.auto-eat", "Auto Eat", Material.COOKED_BEEF, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(48, new SettingDef("bot-settings.auto-potion", "Auto Potion", Material.POTION, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(49, new SettingDef("bot-settings.auto-mend", "Auto Mend", Material.EXPERIENCE_BOTTLE, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(50, new SettingDef("bot-settings.auto-totem", "Auto Totem", Material.TOTEM_OF_UNDYING, SettingType.BOOLEAN, 0, 1, 1));
        SLOT_MAP.put(51, new SettingDef("bot-settings.totem-priority", "Totem Priority", Material.TOTEM_OF_UNDYING, SettingType.BOOLEAN, 0, 1, 1));
    }

    private final JavaPlugin plugin;
    private final BotManager botManager;
    private final BotSettings defaults;

    public SettingsGUI(JavaPlugin plugin, BotManager botManager) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.defaults = botManager.getDefaultSettings();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        populate(inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    private void populate(Inventory inv) {
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, makeFiller(i));
        }
        for (Map.Entry<Integer, SettingDef> entry : SLOT_MAP.entrySet()) {
            inv.setItem(entry.getKey(), makeSettingItem(entry.getValue()));
        }
    }

    private ItemStack makeFiller(int slot) {
        if (slot == 8) return makePlain(Material.BARRIER, "§c✕ Close", "§7Click to close");
        ItemStack pane = switch (slot) {
            case 0 -> makePlain(Material.RED_STAINED_GLASS_PANE, "§c⚔ Combat", null);
            case 1, 3, 5, 7 -> makePlain(Material.GRAY_STAINED_GLASS_PANE, "§8│", null);
            case 2 -> makePlain(Material.BLUE_STAINED_GLASS_PANE, "§b⚡ Movement", null);
            case 4 -> makePlain(Material.GREEN_STAINED_GLASS_PANE, "§a🏹 Ranged", null);
            case 6 -> makePlain(Material.YELLOW_STAINED_GLASS_PANE, "§e🎯 Realism", null);
            default -> null;
        };
        if (pane == null) return null;
        return pane;
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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;
        if (slot == 8) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            return;
        }

        SettingDef def = SLOT_MAP.get(slot);
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
        player.openInventory(event.getView().getTopInventory());
        populate(event.getView().getTopInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(TITLE)) {
            event.setCancelled(true);
        }
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
                int val = (int) Math.min((Integer) current + (int) def.step(), (int) def.max());
                yield val;
            }
            case DOUBLE -> {
                double val = Math.min((Double) current + def.step(), def.max());
                val = Math.round(val * 100.0) / 100.0;
                yield val;
            }
            case BOOLEAN -> !(Boolean) current;
        };
    }

    private Object subStep(Object current, SettingDef def) {
        return switch (def.type()) {
            case INTEGER -> {
                int val = (int) Math.max((Integer) current - (int) def.step(), (int) def.min());
                yield val;
            }
            case DOUBLE -> {
                double val = Math.max((Double) current - def.step(), def.min());
                val = Math.round(val * 100.0) / 100.0;
                yield val;
            }
            case BOOLEAN -> !(Boolean) current;
        };
    }

    private String formatValue(Object value) {
        if (value instanceof Double d) {
            return String.format("%.2f", d);
        }
        return String.valueOf(value);
    }

    private ItemStack makePlain(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (loreLine != null) {
            meta.setLore(List.of(loreLine));
        }
        item.setItemMeta(meta);
        return item;
    }
}
