// Phase 3: Core Melee Combat AI — Melee Attack Loop & Tick-Based Jump Crit State Machine
package com.khoablabla.pvpbot.combat;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MeleeAttackController {

    private int cooldownTicks = 0;
    private int jumpTicks = -1;
    private boolean criticalsEnabled = true;

    private static final double ATTACK_RANGE = 3.5;
    private static final double JUMP_VELOCITY = 0.42;
    private static final int SWORD_COOLDOWN = 12;
    private static final double NORMAL_DAMAGE = 2.0;
    private static final double CRITICAL_DAMAGE = 4.0;

    public boolean isJumping() {
        return jumpTicks >= 0;
    }

    public void handleAttack(NPC npc, LivingEntity target) {
        if (cooldownTicks > 0) cooldownTicks--;

        if (!(npc.getEntity() instanceof Player botPlayer)) return;

        if (jumpTicks >= 0) {
            jumpTicks++;

            // Ticks 0-4: Ascending phase — do NOT check for landing
            // Tick 5: Execute crit strike at apex
            if (jumpTicks == 5) {
                executeStrike(botPlayer, target, true);
            }

            // Ticks 5+: Descending phase — check for landing only if falling
            if (jumpTicks > 4 && botPlayer.getVelocity().getY() <= 0.0) {
                if (botPlayer.isOnGround() || botPlayer.getLocation().subtract(0, 0.2, 0).getBlock().isSolid()) {
                    jumpTicks = -1;
                    cooldownTicks = SWORD_COOLDOWN;
                }
            }

            // Absolute timeout safety net
            if (jumpTicks >= 12) {
                jumpTicks = -1;
                cooldownTicks = SWORD_COOLDOWN;
            }

            return;
        }

        double dist = botPlayer.getLocation().distance(target.getLocation());
        if (dist > ATTACK_RANGE) return;

        if (cooldownTicks > 0) return;

        if (criticalsEnabled) {
            Location below = botPlayer.getLocation().subtract(0, 0.1, 0);
            if (!below.getBlock().isSolid()) return;

            Vector currentVel = botPlayer.getVelocity();
            botPlayer.setVelocity(new Vector(currentVel.getX(), JUMP_VELOCITY, currentVel.getZ()));
            npc.getNavigator().cancelNavigation();
            jumpTicks = 0;
        } else {
            executeStrike(botPlayer, target, false);
            cooldownTicks = SWORD_COOLDOWN;
        }
    }

    private void executeStrike(Player botPlayer, LivingEntity target, boolean critical) {
        Location targetLoc = target.getLocation().add(0, 1, 0);
        if (critical) {
            target.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 0.5, 0.5, 0.5, 0.1);
        }
        botPlayer.swingMainHand();
        target.damage(critical ? CRITICAL_DAMAGE : NORMAL_DAMAGE, botPlayer);
    }
}
