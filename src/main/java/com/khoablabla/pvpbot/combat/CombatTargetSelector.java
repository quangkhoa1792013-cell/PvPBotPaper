// Phase 3.3.2: Relentless Pursuit — No LOS, No Timeout, World-Safe
package com.khoablabla.pvpbot.combat;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class CombatTargetSelector {

    private CombatTargetSelector() {
    }

    public static LivingEntity validateTarget(NPC npc, double range, LivingEntity currentTarget, int tickCounter, int lastDamageTick) {
        if (currentTarget == null) return null;
        if (currentTarget.isDead() || !currentTarget.isValid()) return null;
        if (!(npc.getEntity() instanceof LivingEntity botEntity)) return null;

        if (!botEntity.getWorld().equals(currentTarget.getWorld())) return null;

        double distSq = botEntity.getLocation().distanceSquared(currentTarget.getLocation());
        if (distSq > range * range) return null;

        return currentTarget;
    }
}
