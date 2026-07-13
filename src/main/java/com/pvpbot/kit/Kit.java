package com.pvpbot.kit;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Kit {

    private final String name;
    private final Map<Integer, ItemStack> items;
    private final List<ItemStack> armorContents;

    public Kit(@NotNull String name) {
        this.name = name;
        this.items = new HashMap<>();
        this.armorContents = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) armorContents.add(null);
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setItem(int slot, @Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            items.remove(slot);
        } else {
            items.put(slot, stack.clone());
        }
    }

    @Nullable
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @NotNull
    public Map<Integer, ItemStack> getItems() {
        return items;
    }

    public void setArmor(int index, @Nullable ItemStack stack) {
        if (index >= 0 && index < 4) {
            armorContents.set(index, stack != null && !stack.getType().isAir() ? stack.clone() : null);
        }
    }

    @Nullable
    public ItemStack getArmor(int index) {
        return index >= 0 && index < 4 ? armorContents.get(index) : null;
    }

    @NotNull
    public List<ItemStack> getArmorContents() {
        return armorContents;
    }
}
