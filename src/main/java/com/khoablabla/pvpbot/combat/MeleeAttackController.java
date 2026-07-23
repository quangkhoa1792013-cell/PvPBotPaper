// Phase 3.3.1: Melee Attack Loop — Forward Sprint-Leap with Navigation Suspension
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
    private static final double JUMP_VELOCITY = 0.38;
    private static final double LEAP_SPEED = 0.22;
    private static final int SWORD_COOLDOWN = 8;
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

            if (jumpTicks == 5 && target.getWorld().equals(botPlayer.getWorld())
                    && botPlayer.getLocation().distanceSquared(target.getLocation()) <= ATTACK_RANGE * ATTACK_RANGE) {
                executeStrike(botPlayer, target, true);
            }

            if (jumpTicks > 4 && botPlayer.getVelocity().getY() <= 0.0) {
                if (botPlayer.isOnGround() || botPlayer.getLocation().subtract(0, 0.2, 0).getBlock().isSolid()) {
                    jumpTicks = -1;
                    cooldownTicks = SWORD_COOLDOWN;
                }
            }

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

            Vector direction = target.getLocation().toVector().subtract(botPlayer.getLocation().toVector());
            direction.setY(0);

            Vector currentVel = botPlayer.getVelocity();
            double finalX;
            double finalZ;

            if (direction.lengthSquared() == 0) {
                finalX = currentVel.getX() * 0.3;
                finalZ = currentVel.getZ() * 0.3;
            } else {
                direction.normalize();
                finalX = currentVel.getX() * 0.3 + (direction.getX() * LEAP_SPEED) * 0.7;
                finalZ = currentVel.getZ() * 0.3 + (direction.getZ() * LEAP_SPEED) * 0.7;
            }

            botPlayer.setVelocity(new Vector(finalX, JUMP_VELOCITY, finalZ));
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
