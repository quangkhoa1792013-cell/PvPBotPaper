// Phase 4: Melee Attack Loop — Dynamic settings from PvPBotTrait
package com.khoablabla.pvpbot.combat;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

public class MeleeAttackController {

    private int cooldownTicks = 0;
    private int jumpTicks = -1;

    private static final double JUMP_VELOCITY = 0.38;
    private static final double LEAP_SPEED = 0.18;
    private static final double MAX_HORIZONTAL_LEAP_SPEED = 0.24;
    private static final double MAX_PRE_JUMP_HORIZONTAL_SPEED_SQ = 0.18;
    private static final double NORMAL_DAMAGE = 2.0;
    private static final double CRITICAL_DAMAGE = 4.0;

    public boolean isJumping() {
        return jumpTicks >= 0;
    }

    public void handleAttack(NPC npc, LivingEntity target, PvPBotTrait trait) {
        if (cooldownTicks > 0) cooldownTicks--;

        if (!(npc.getEntity() instanceof Player botPlayer)) return;
        if (!isValidTarget(botPlayer, target)) return;

        double attackRange = trait.getSetting("melee-range", Double.class);
        int cooldown = trait.getSetting("attack-cooldown", Integer.class);
        boolean criticals = trait.getSetting("criticals", Boolean.class);

        if (jumpTicks >= 0) {
            jumpTicks++;
            npc.faceLocation(target.getEyeLocation());

            if (jumpTicks == 5 && canStrike(botPlayer, target, attackRange)) {
                executeStrike(botPlayer, target, true);
            }

            if (jumpTicks > 4 && botPlayer.getVelocity().getY() <= 0.0 && isGrounded(botPlayer)) {
                jumpTicks = -1;
                cooldownTicks = cooldown;
                return;
            }

            if (jumpTicks >= 12) {
                jumpTicks = -1;
                cooldownTicks = cooldown;
            }

            return;
        }

        if (!canStrike(botPlayer, target, attackRange)) return;

        if (cooldownTicks > 0) return;

        npc.faceLocation(target.getEyeLocation());
        if (criticals && canStartCritical(botPlayer, target)) {
            botPlayer.setVelocity(buildCriticalVelocity(botPlayer, target));
            jumpTicks = 0;
        } else {
            executeStrike(botPlayer, target, false);
            cooldownTicks = cooldown;
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

    private boolean isValidTarget(Player botPlayer, LivingEntity target) {
        return target != null
                && target.isValid()
                && !target.isDead()
                && target.getWorld().equals(botPlayer.getWorld());
    }

    private boolean canStrike(Player botPlayer, LivingEntity target, double attackRange) {
        return isValidTarget(botPlayer, target)
                && botPlayer.getLocation().distanceSquared(target.getLocation()) <= attackRange * attackRange
                && botPlayer.hasLineOfSight(target);
    }

    private boolean canStartCritical(Player botPlayer, LivingEntity target) {
        Vector velocity = botPlayer.getVelocity();
        double horizontalSpeedSq = velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ();
        if (horizontalSpeedSq > MAX_PRE_JUMP_HORIZONTAL_SPEED_SQ) return false;
        if (!isGrounded(botPlayer)) return false;

        Vector direction = target.getLocation().toVector().subtract(botPlayer.getLocation().toVector());
        direction.setY(0);
        return direction.lengthSquared() > 0.01;
    }

    private boolean isGrounded(Player botPlayer) {
        Location feet = botPlayer.getLocation();
        return feet.clone().subtract(0.0, 0.08, 0.0).getBlock().isSolid()
                || feet.clone().subtract(0.0, 0.2, 0.0).getBlock().isSolid();
    }

    private Vector buildCriticalVelocity(Player botPlayer, LivingEntity target) {
        Vector direction = target.getLocation().toVector().subtract(botPlayer.getLocation().toVector());
        direction.setY(0);
        direction.normalize();

        Vector currentVel = botPlayer.getVelocity();
        double finalX = currentVel.getX() * 0.15 + direction.getX() * LEAP_SPEED;
        double finalZ = currentVel.getZ() * 0.15 + direction.getZ() * LEAP_SPEED;
        Vector horizontal = clampHorizontal(new Vector(finalX, 0.0, finalZ), MAX_HORIZONTAL_LEAP_SPEED);
        return new Vector(horizontal.getX(), JUMP_VELOCITY, horizontal.getZ());
    }

    private Vector clampHorizontal(Vector velocity, double maxSpeed) {
        double lengthSq = velocity.lengthSquared();
        if (lengthSq <= maxSpeed * maxSpeed) return velocity;
        return velocity.normalize().multiply(maxSpeed);
    }
}
