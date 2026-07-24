// Phase 4.1.3: Functional Settings — target-mobs, attack-invincible
package com.khoablabla.pvpbot.combat;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import com.khoablabla.pvpbot.traits.PvPBotTrait;

public final class CombatTargetSelector {

    private CombatTargetSelector() {
    }

    public static LivingEntity validateTarget(NPC npc, double range, LivingEntity currentTarget,
            int tickCounter, int lastDamageTick, boolean targetPlayers, boolean targetBots,
            boolean targetMobs, boolean attackInvincible) {
        if (currentTarget == null) return null;
        if (currentTarget.isDead() || !currentTarget.isValid()) return null;
        if (!(npc.getEntity() instanceof LivingEntity botEntity)) return null;

        if (currentTarget instanceof Player p && !CitizensAPI.getNPCRegistry().isNPC(p)) {
            if (!targetPlayers) return null;
            if (!attackInvincible
                    && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)) {
                return null;
            }
        }

        if (CitizensAPI.getNPCRegistry().isNPC(currentTarget)) {
            NPC targetNpc = CitizensAPI.getNPCRegistry().getNPC(currentTarget);
            if (targetNpc != null && targetNpc.hasTrait(PvPBotTrait.class) && !targetBots) return null;
        }

        if (currentTarget instanceof Monster && !targetMobs) return null;

        if (!botEntity.getWorld().equals(currentTarget.getWorld())) return null;

        double distSq = botEntity.getLocation().distanceSquared(currentTarget.getLocation());
        if (distSq > range * range) return null;

        return currentTarget;
    }

    public static LivingEntity scanForTarget(NPC npc, double range,
            boolean targetPlayers, boolean targetBots, boolean targetMobs,
            boolean attackInvincible) {
        if (!(npc.getEntity() instanceof LivingEntity botEntity)) return null;

        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        double rangeSq = range * range;

        for (Entity entity : botEntity.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity candidate)) continue;
            if (candidate.isDead() || !candidate.isValid()) continue;
            if (entity.equals(botEntity)) continue;
            if (!candidate.getWorld().equals(botEntity.getWorld())) continue;

            if (candidate instanceof Player p && !CitizensAPI.getNPCRegistry().isNPC(p)) {
                if (!targetPlayers) continue;
                if (!attackInvincible
                        && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)) {
                    continue;
                }
                if (!botEntity.hasLineOfSight(candidate)) continue;
            }

            if (CitizensAPI.getNPCRegistry().isNPC(candidate)) {
                NPC candidateNpc = CitizensAPI.getNPCRegistry().getNPC(candidate);
                if (candidateNpc != null && candidateNpc.hasTrait(PvPBotTrait.class) && !targetBots) continue;
            }

            if (candidate instanceof Monster && !targetMobs) continue;

            double distSq = botEntity.getLocation().distanceSquared(candidate.getLocation());
            if (distSq > rangeSq) continue;

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }

        return best;
    }
}
