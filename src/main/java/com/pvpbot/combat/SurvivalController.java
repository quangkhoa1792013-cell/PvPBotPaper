package com.pvpbot.combat;

import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.CustomBot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SurvivalController {

    private static final int ARMOR_SCAN_INTERVAL = 20;
    private static final int POTION_SCAN_INTERVAL = 40;
    private static final int COBWEB_SCAN_INTERVAL = 30;
    private static final int MEND_SCAN_INTERVAL = 50;
    private static final int EAT_TICKS = 32;
    private static final int OFFHAND_CHECK_INTERVAL = 10;

    private final CustomBot bot;
    private final BotSettings settings;

    private int armorScanTimer;
    private int potionScanTimer;
    private int cobwebScanTimer;
    private int mendScanTimer;
    private int eatTimer;
    private boolean eating;
    private int offhandCheckTimer;

    public SurvivalController(@NotNull CustomBot bot, @NotNull BotSettings settings) {
        this.bot = bot;
        this.settings = settings;
        this.armorScanTimer = 0;
        this.potionScanTimer = POTION_SCAN_INTERVAL;
        this.cobwebScanTimer = COBWEB_SCAN_INTERVAL;
        this.mendScanTimer = MEND_SCAN_INTERVAL;
        this.offhandCheckTimer = 0;
    }

    public void tick(@Nullable LivingEntity combatTarget, boolean isRetreating, boolean maceActive) {
        if (bot.isUsingItem() && !eating) return;

        if (armorScanTimer > 0) armorScanTimer--;
        if (potionScanTimer > 0) potionScanTimer--;
        if (cobwebScanTimer > 0) cobwebScanTimer--;
        if (mendScanTimer > 0) mendScanTimer--;
        if (offhandCheckTimer > 0) offhandCheckTimer--;

        if (eating) {
            handleEatingTick();
            return;
        }

        if (armorScanTimer <= 0 && settings.isAutoArmor()) {
            handleAutoArmor();
            armorScanTimer = ARMOR_SCAN_INTERVAL;
        }

        if (offhandCheckTimer <= 0) {
            handleAutoTotem();
            offhandCheckTimer = OFFHAND_CHECK_INTERVAL;
        }

        if (eatTimer > 0) {
            eatTimer--;
        } else if (settings.isAutoEat()) {
            handleAutoEat(combatTarget);
        }

        if (potionScanTimer <= 0 && settings.isAutoPotion()) {
            handleAutoPotion();
            potionScanTimer = POTION_SCAN_INTERVAL;
        }

        if (mendScanTimer <= 0 && settings.isAutoMend()) {
            handleAutoMend();
            mendScanTimer = MEND_SCAN_INTERVAL;
        }

        if (cobwebScanTimer <= 0 && hasItem(Items.COBWEB)) {
            handleCobweb(combatTarget, isRetreating);
            cobwebScanTimer = COBWEB_SCAN_INTERVAL;
        }
    }

    // --- Auto-Armor ---

    private void handleAutoArmor() {
        tryEquipSlot(EquipmentSlot.HEAD, getBestArmor(EquipmentSlot.HEAD));
        tryEquipSlot(EquipmentSlot.CHEST, getBestArmor(EquipmentSlot.CHEST));
        tryEquipSlot(EquipmentSlot.LEGS, getBestArmor(EquipmentSlot.LEGS));
        tryEquipSlot(EquipmentSlot.FEET, getBestArmor(EquipmentSlot.FEET));
    }

    private void tryEquipSlot(EquipmentSlot slot, int bestSlot) {
        if (bestSlot < 0) return;
        ItemStack currentEquipped = bot.getItemBySlot(slot);
        ItemStack candidate = bot.getInventory().getItem(bestSlot);
        if (currentEquipped.isEmpty() || scoreArmor(candidate) > scoreArmor(currentEquipped)) {
            bot.setItemSlot(slot, candidate.copy());
            bot.getInventory().setItem(bestSlot, currentEquipped.copy());
            currentEquipped.setCount(0);
        }
    }

    private int getBestArmor(EquipmentSlot slot) {
        int bestSlot = -1;
        int bestScore = -1;
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!isArmorForSlot(stack, slot)) continue;
            int score = scoreArmor(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private boolean isArmorForSlot(ItemStack stack, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> stack.is(Items.NETHERITE_HELMET) || stack.is(Items.DIAMOND_HELMET)
                    || stack.is(Items.IRON_HELMET) || stack.is(Items.GOLDEN_HELMET)
                    || stack.is(Items.CHAINMAIL_HELMET) || stack.is(Items.LEATHER_HELMET)
                    || stack.is(Items.TURTLE_HELMET);
            case CHEST -> stack.is(Items.NETHERITE_CHESTPLATE) || stack.is(Items.DIAMOND_CHESTPLATE)
                    || stack.is(Items.IRON_CHESTPLATE) || stack.is(Items.GOLDEN_CHESTPLATE)
                    || stack.is(Items.CHAINMAIL_CHESTPLATE) || stack.is(Items.LEATHER_CHESTPLATE);
            case LEGS -> stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.DIAMOND_LEGGINGS)
                    || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.GOLDEN_LEGGINGS)
                    || stack.is(Items.CHAINMAIL_LEGGINGS) || stack.is(Items.LEATHER_LEGGINGS);
            case FEET -> stack.is(Items.NETHERITE_BOOTS) || stack.is(Items.DIAMOND_BOOTS)
                    || stack.is(Items.IRON_BOOTS) || stack.is(Items.GOLDEN_BOOTS)
                    || stack.is(Items.CHAINMAIL_BOOTS) || stack.is(Items.LEATHER_BOOTS);
            default -> false;
        };
    }

    private int scoreArmor(ItemStack stack) {
        Item item = stack.getItem();
        int tier = armorTier(item);
        int enchantScore = getEnchantBonus(stack);
        return tier * 100 + enchantScore;
    }

    private int armorTier(Item item) {
        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
                || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) return 6;
        if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE
                || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) return 5;
        if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE
                || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) return 4;
        if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE
                || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) return 3;
        if (item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE
                || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS) return 2;
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE
                || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS) return 1;
        if (item == Items.TURTLE_HELMET) return 3;
        return 0;
    }

    // --- Auto-Totem ---

    private void handleAutoTotem() {
        if (!settings.isAutoTotem()) return;
        if (!hasItem(Items.TOTEM_OF_UNDYING)) return;

        ItemStack offhand = bot.getOffhandItem();
        boolean hasTotem = offhand.is(Items.TOTEM_OF_UNDYING);
        boolean hasShield = offhand.is(Items.SHIELD);

        if (hasTotem && settings.isTotemPriority()) return;

        if (settings.isTotemPriority()) {
            equipOffhand(Items.TOTEM_OF_UNDYING);
            return;
        }

        float health = bot.getHealth();
        if (health <= 6.0f && !hasTotem) {
            equipOffhand(Items.TOTEM_OF_UNDYING);
        } else if (health > 6.0f && hasTotem && settings.isAutoShield() && hasItem(Items.SHIELD)) {
            equipOffhand(Items.SHIELD);
        }
    }

    private void equipOffhand(Item item) {
        int slot = findItemSlot(item);
        if (slot < 0) return;
        ItemStack offhand = bot.getOffhandItem();
        ItemStack stack = bot.getInventory().getItem(slot);
        bot.getInventory().setItem(slot, offhand.copy());
        bot.getInventory().setItem(40, stack.copy());
        offhand.setCount(0);
        stack.setCount(0);
    }

    // --- Auto-Eat ---

    private void handleAutoEat(@Nullable LivingEntity combatTarget) {
        boolean inCombat = combatTarget != null;
        FoodData foodData = bot.getFoodData();
        int foodLevel = foodData.getFoodLevel();
        float health = bot.getHealth();

        boolean shouldEat = false;
        Item food = null;

        if (health < bot.getMaxHealth() * 0.5f && hasItem(Items.GOLDEN_APPLE)) {
            shouldEat = true;
            food = Items.GOLDEN_APPLE;
        } else if (health < bot.getMaxHealth() * 0.3f && hasItem(Items.ENCHANTED_GOLDEN_APPLE)) {
            shouldEat = true;
            food = Items.ENCHANTED_GOLDEN_APPLE;
        } else if (foodLevel < 16 && !inCombat) {
            shouldEat = true;
            food = findBestFood();
        }

        if (!shouldEat || food == null) return;

        int slot = findItemSlot(food);
        if (slot < 0) return;

        if (slot >= 9) {
            int currentSelected = bot.getInventory().getSelectedSlot();
            ItemStack currentItem = bot.getInventory().getItem(currentSelected);
            bot.getInventory().setItem(slot, currentItem);
            bot.getInventory().setItem(currentSelected, bot.getInventory().getItem(slot));
            slot = currentSelected;
        }
        bot.getInventory().setSelectedSlot(slot);
        bot.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        eating = true;
        eatTimer = EAT_TICKS;
    }

    @Nullable
    private Item findBestFood() {
        Item[] foods = {Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN,
                Items.COOKED_MUTTON, Items.COOKED_COD, Items.COOKED_SALMON,
                Items.BREAD, Items.BAKED_POTATO, Items.APPLE};
        for (Item f : foods) {
            if (hasItem(f)) return f;
        }
        return null;
    }

    private void handleEatingTick() {
        if (eatTimer > 0) {
            eatTimer--;
        }
        if (eatTimer <= 0) {
            if (bot.isUsingItem()) {
                bot.finishUsingItem();
            }
            eating = false;
            eatTimer = 0;
        }
    }

    // --- Auto-Potion ---

    private void handleAutoPotion() {
        float health = bot.getHealth();

        if (health < 10.0f) {
            splashPotionAtFeet(findSplashHealingPotion());
        }

        if (health < bot.getMaxHealth()) {
            if (!hasEffect(MobEffects.REGENERATION) && hasSplashPotion(MobEffects.REGENERATION)) {
                splashPotionAtFeet(findSplashPotion(MobEffects.REGENERATION));
            }
        }

        if (!hasEffect(MobEffects.STRENGTH) && hasSplashPotion(MobEffects.STRENGTH)) {
            splashPotionAtFeet(findSplashPotion(MobEffects.STRENGTH));
        }
        if (!hasEffect(MobEffects.SPEED) && hasSplashPotion(MobEffects.SPEED)) {
            splashPotionAtFeet(findSplashPotion(MobEffects.SPEED));
        }
        if (!hasEffect(MobEffects.FIRE_RESISTANCE) && hasSplashPotion(MobEffects.FIRE_RESISTANCE)) {
            splashPotionAtFeet(findSplashPotion(MobEffects.FIRE_RESISTANCE));
        }
    }

    private boolean hasEffect(Holder<MobEffect> effect) {
        return bot.getActiveEffects().stream()
                .anyMatch(e -> e.getEffect().equals(effect));
    }

    private boolean hasSplashPotion(Holder<MobEffect> targetEffect) {
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (!stack.is(Items.SPLASH_POTION)) continue;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                for (var effect : contents.getAllEffects()) {
                    if (effect.getEffect().equals(targetEffect)) return true;
                }
            }
        }
        return false;
    }

    private int findSplashHealingPotion() {
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (!stack.is(Items.SPLASH_POTION)) continue;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                for (var effect : contents.getAllEffects()) {
                    if (effect.getEffect().equals(MobEffects.INSTANT_HEALTH) && effect.getAmplifier() >= 1) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private int findSplashPotion(Holder<MobEffect> targetEffect) {
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (!stack.is(Items.SPLASH_POTION)) continue;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                for (var effect : contents.getAllEffects()) {
                    if (effect.getEffect().equals(targetEffect)) return i;
                }
            }
        }
        return -1;
    }

    private void splashPotionAtFeet(int slot) {
        if (slot < 0) return;
        int currentSelected = bot.getInventory().getSelectedSlot();
        int targetSlot = slot;
        if (slot >= 9) {
            ItemStack currentItem = bot.getInventory().getItem(currentSelected);
            bot.getInventory().setItem(slot, currentItem);
            bot.getInventory().setItem(currentSelected, bot.getInventory().getItem(slot));
            targetSlot = currentSelected;
        }
        bot.getInventory().setSelectedSlot(targetSlot);

        bot.setXRot(85.0f);
        ItemStack potion = bot.getMainHandItem();
        if (potion.is(Items.SPLASH_POTION)) {
            potion.use(bot.level(), bot, net.minecraft.world.InteractionHand.MAIN_HAND);
            potion.shrink(1);
        }

        if (slot >= 9) {
            ItemStack currentItem = bot.getInventory().getItem(currentSelected);
            bot.getInventory().setItem(targetSlot, currentItem);
            bot.getInventory().setItem(currentSelected, bot.getInventory().getItem(slot));
        }
        bot.getInventory().setSelectedSlot(currentSelected);
    }

    // --- Auto-Mend ---

    private void handleAutoMend() {
        LivingEntity target = bot.getMovementController().getCombatTarget();
        boolean inCombat = target != null;
        if (inCombat) return;

        boolean needsMend = false;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = bot.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            if (armor.isDamaged() && hasMending(armor)) {
                needsMend = true;
                break;
            }
        }

        if (!needsMend || !hasItem(Items.EXPERIENCE_BOTTLE)) return;

        int slot = findItemSlot(Items.EXPERIENCE_BOTTLE);
        if (slot < 0) return;

        int currentSelected = bot.getInventory().getSelectedSlot();
        int targetSlot = slot;
        if (slot >= 9) {
            ItemStack currentItem = bot.getInventory().getItem(currentSelected);
            bot.getInventory().setItem(slot, currentItem);
            bot.getInventory().setItem(currentSelected, bot.getInventory().getItem(slot));
            targetSlot = currentSelected;
        }
        bot.getInventory().setSelectedSlot(targetSlot);

        bot.setXRot(80.0f);
        ItemStack xpBottle = bot.getMainHandItem();
        if (xpBottle.is(Items.EXPERIENCE_BOTTLE)) {
            xpBottle.use(bot.level(), bot, net.minecraft.world.InteractionHand.MAIN_HAND);
            xpBottle.shrink(1);
        }

        if (slot >= 9) {
            ItemStack currentItem = bot.getInventory().getItem(currentSelected);
            bot.getInventory().setItem(targetSlot, currentItem);
            bot.getInventory().setItem(currentSelected, bot.getInventory().getItem(slot));
        }
        bot.getInventory().setSelectedSlot(currentSelected);
    }

    private boolean hasMending(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return false;
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(Enchantments.MENDING)) return true;
        }
        return false;
    }

    // --- Cobweb Trapping ---

    private void handleCobweb(@Nullable LivingEntity combatTarget, boolean isRetreating) {
        if (!hasItem(Items.COBWEB)) return;

        int cobwebSlot = findItemSlot(Items.COBWEB);
        if (cobwebSlot < 0) return;

        BlockPos placePos;
        if (isRetreating) {
            placePos = bot.blockPosition();
        } else if (combatTarget != null && combatTarget.isAlive()) {
            placePos = combatTarget.blockPosition();
        } else {
            return;
        }

        Level level = bot.level();
        if (level == null) return;

        if (level.getBlockState(placePos).isAir()) {
            level.setBlockAndUpdate(placePos, Blocks.COBWEB.defaultBlockState());
            bot.getInventory().getItem(cobwebSlot).shrink(1);
        } else if (level.getBlockState(placePos.above()).isAir()) {
            level.setBlockAndUpdate(placePos.above(), Blocks.COBWEB.defaultBlockState());
            bot.getInventory().getItem(cobwebSlot).shrink(1);
        }
    }

    // --- Helpers ---

    private boolean hasItem(Item item) {
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            if (bot.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            if (bot.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private int getEnchantBonus(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return 0;
        int bonus = 0;
        for (var entry : enchants.entrySet()) {
            bonus += entry.getIntValue();
        }
        return bonus;
    }
}
