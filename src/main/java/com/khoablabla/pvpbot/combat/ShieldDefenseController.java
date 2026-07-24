// Phase 4.1.3: Modular Shield Defense — auto-shield, shield-raise-ticks, shield-hold-ticks
package com.khoablabla.pvpbot.combat;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

public class ShieldDefenseController {

    private int shieldActiveTicks = 0;
    private boolean shieldRaised = false;

    public void handleDefense(NPC npc, LivingEntity target, PvPBotTrait trait) {
        if (!(npc.getEntity() instanceof Player botPlayer)) return;

        boolean autoShield = trait.getSetting("auto-shield", Boolean.class);
        if (!autoShield) {
            if (shieldRaised) {
                botPlayer.clearActiveItem();
                shieldRaised = false;
                shieldActiveTicks = 0;
            }
            return;
        }

        PlayerInventory inv = botPlayer.getInventory();
        ItemStack offHand = inv.getItemInOffHand();
        if (offHand.getType() != Material.SHIELD) {
            if (shieldRaised) {
                botPlayer.clearActiveItem();
                shieldRaised = false;
                shieldActiveTicks = 0;
            }
            return;
        }

        double dist = botPlayer.getLocation().distance(target.getLocation());
        int raiseTicks = trait.getSetting("shield-raise-ticks", Integer.class);
        int holdTicks = trait.getSetting("shield-hold-ticks", Integer.class);

        boolean hasBow = false;
        if (target instanceof Player targetPlayer) {
            ItemStack mainHand = targetPlayer.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType().name().contains("BOW")) {
                hasBow = true;
            }
        }

        boolean shouldRaise = dist <= 4.0 || hasBow;

        if (shouldRaise && !shieldRaised) {
            shieldRaised = true;
            shieldActiveTicks = 0;
            botPlayer.startUsingItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        }

        if (shieldRaised) {
            shieldActiveTicks++;
            if (shieldActiveTicks >= holdTicks || dist > 6.0) {
                botPlayer.clearActiveItem();
                shieldRaised = false;
                shieldActiveTicks = 0;
            }
        }
    }
}
