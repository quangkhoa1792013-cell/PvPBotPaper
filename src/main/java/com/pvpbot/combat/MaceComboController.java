package com.pvpbot.combat;

import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.CustomBot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaceComboController {

    public enum State {
        IDLE,
        WIND_CHARGE_JUMP,
        SWAP_MACE,
        FALL_AND_AIM,
        MACE_SMASH
    }

    private State state = State.IDLE;
    private int stateTimer;
    private boolean smashPerformed;

    private final CustomBot bot;
    private final BotSettings settings;

    public MaceComboController(@NotNull CustomBot bot, @NotNull BotSettings settings) {
        this.bot = bot;
        this.settings = settings;
    }

    public State getState() {
        return state;
    }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public void reset() {
        state = State.IDLE;
        stateTimer = 0;
        smashPerformed = false;
    }

    public void tick(@Nullable LivingEntity target) {
        if (!settings.isCombat() || !settings.isMace() || target == null) {
            reset();
            return;
        }

        boolean hasMace = bot.getMainHandItem().is(Items.MACE) || hasItemInInventory(Items.MACE);
        boolean hasWindCharge = hasItemInInventory(Items.WIND_CHARGE);

        if (!hasMace || !hasWindCharge) {
            reset();
            return;
        }

        switch (state) {
            case IDLE -> {
                if (bot.onGround() && bot.canAttack()) {
                    state = State.WIND_CHARGE_JUMP;
                    stateTimer = 0;
                }
            }
            case WIND_CHARGE_JUMP -> {
                stateTimer++;
                if (stateTimer == 1) {
                    equipItem(Items.WIND_CHARGE);
                    bot.setXRot(90F);
                    bot.setYRot(bot.getYRot());
                    ItemStack windCharge = bot.getMainHandItem();
                    if (windCharge.is(Items.WIND_CHARGE)) {
                        windCharge.use(bot.level(), bot, InteractionHand.MAIN_HAND);
                    }
                }
                if (bot.getDeltaMovement().y > 0.3) {
                    state = State.SWAP_MACE;
                    stateTimer = 0;
                }
                if (bot.onGround()) {
                    reset();
                }
            }
            case SWAP_MACE -> {
                equipItem(Items.MACE);
                state = State.FALL_AND_AIM;
                stateTimer = 0;
            }
            case FALL_AND_AIM -> {
                stateTimer++;
                if (bot.getDeltaMovement().y < 0 && target != null) {
                    Vec3 targetHead = target.position().add(0, target.getEyeHeight(), 0);
                    double dx = targetHead.x - bot.getX();
                    double dy = targetHead.y - bot.getEyeY();
                    double dz = targetHead.z - bot.getZ();
                    double horizDist = Math.sqrt(dx * dx + dz * dz);
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, -dz));
                    float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(horizDist, 0.001)));
                    bot.setYRot(yaw);
                    bot.setXRot(pitch);
                    if (bot.getBukkitEntity() != null) {
                        bot.getBukkitEntity().setRotation(yaw, pitch);
                    }
                }
                double verticalDist = target != null ? bot.getY() - target.getY() : 0;
                if (verticalDist <= 3.0 && verticalDist >= -1.0 && bot.getDeltaMovement().y < 0 && !smashPerformed) {
                    state = State.MACE_SMASH;
                    stateTimer = 0;
                }
                if (bot.onGround()) {
                    reset();
                }
            }
            case MACE_SMASH -> {
                if (!smashPerformed && target != null) {
                    bot.attack(target);
                    smashPerformed = true;
                    bot.resetAttackCooldown();
                }
                if (bot.onGround()) {
                    reset();
                }
            }
        }
    }

    private boolean hasItemInInventory(@NotNull Item item) {
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            if (bot.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }

    private void equipItem(@NotNull Item item) {
        int slot = -1;
        int currentSelected = bot.getInventory().getSelectedSlot();
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            if (bot.getInventory().getItem(i).is(item)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;
        int targetSlot = slot;
        if (slot >= 9) {
            ItemStack currentItem = bot.getInventory().getItem(currentSelected);
            bot.getInventory().setItem(slot, currentItem);
            bot.getInventory().setItem(currentSelected, bot.getInventory().getItem(slot));
            targetSlot = currentSelected;
        }
        bot.getInventory().setSelectedSlot(targetSlot);
    }
}
