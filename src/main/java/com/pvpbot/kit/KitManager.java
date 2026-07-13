package com.pvpbot.kit;

import com.pvpbot.bot.CustomBot;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class KitManager {

    private final File kitsFile;
    private final Map<String, Kit> kits;
    private final Random random;

    public KitManager(@NotNull File dataFolder) {
        this.kitsFile = new File(dataFolder, "kits.yml");
        this.kits = new HashMap<>();
        this.random = new Random();
        loadKits();
    }

    private void loadKits() {
        if (!kitsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        for (String name : config.getKeys(false)) {
            Kit kit = new Kit(name);
            String base = name + ".";
            for (int i = 0; i < 4; i++) {
                String armorPath = base + "armor." + i;
                if (config.contains(armorPath)) {
                    kit.setArmor(i, config.getItemStack(armorPath));
                }
            }
            String itemsPath = base + "items";
            if (config.contains(itemsPath)) {
                var section = config.getConfigurationSection(itemsPath);
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(key);
                            kit.setItem(slot, config.getItemStack(itemsPath + "." + key));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            kits.put(name.toLowerCase(), kit);
        }
    }

    public void saveKits() {
        YamlConfiguration config = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String base = kit.getName() + ".";
            for (int i = 0; i < 4; i++) {
                ItemStack armor = kit.getArmor(i);
                if (armor != null) {
                    config.set(base + "armor." + i, armor);
                }
            }
            for (var entry : kit.getItems().entrySet()) {
                config.set(base + "items." + entry.getKey(), entry.getValue());
            }
        }
        try {
            config.save(kitsFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[PvPBot] Failed to save kits.yml: " + e.getMessage());
        }
    }

    @Nullable
    public Kit getKit(@NotNull String name) {
        return kits.get(name.toLowerCase());
    }

    @NotNull
    public Collection<Kit> getAllKits() {
        return kits.values();
    }

    public boolean kitExists(@NotNull String name) {
        return kits.containsKey(name.toLowerCase());
    }

    public boolean createKit(@NotNull String name, @NotNull Player player) {
        if (kitExists(name)) return false;

        Kit kit = new Kit(name);
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < 4; i++) {
            ItemStack armor = inv.getArmorContents()[i];
            if (armor != null && !armor.getType().isAir()) {
                kit.setArmor(i, armor.clone());
            }
        }

        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            kit.setItem(40, offhand.clone());
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.getType().isAir()) {
                kit.setItem(i, stack.clone());
            }
        }

        kits.put(name.toLowerCase(), kit);
        saveKits();
        return true;
    }

    public boolean deleteKit(@NotNull String name) {
        if (!kitExists(name)) return false;
        kits.remove(name.toLowerCase());
        saveKits();
        return true;
    }

    public void giveKit(@NotNull CustomBot bot, @NotNull Kit kit) {
        clearBotInventory(bot);
        var nmsInventory = bot.getInventory();
        for (var entry : kit.getItems().entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = entry.getValue();
            if (slot < nmsInventory.getContainerSize()) {
                nmsInventory.setItem(slot, net.minecraft.world.item.ItemStack.fromBukkitCopy(stack));
            }
        }
        for (int i = 0; i < 4; i++) {
            ItemStack armor = kit.getArmor(i);
            if (armor != null) {
                nmsInventory.setItem(36 + (3 - i), net.minecraft.world.item.ItemStack.fromBukkitCopy(armor));
            }
        }
    }

    public void clearBotInventory(@NotNull CustomBot bot) {
        var nmsInventory = bot.getInventory();
        for (int i = 0; i < nmsInventory.getContainerSize(); i++) {
            nmsInventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    public void giveRandomWeightedKit(@NotNull List<? extends CustomBot> bots, @NotNull Map<Kit, Double> weightedKits) {
        if (weightedKits.isEmpty() || bots.isEmpty()) return;

        double totalWeight = weightedKits.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight <= 0) return;

        for (CustomBot bot : bots) {
            double roll = random.nextDouble() * totalWeight;
            double cumulative = 0;
            for (var entry : weightedKits.entrySet()) {
                cumulative += entry.getValue();
                if (roll <= cumulative) {
                    giveKit(bot, entry.getKey());
                    break;
                }
            }
        }
    }
}
