// Phase 3: Core Melee Combat AI — Revenge-Only Target Validation
package com.khoablabla.pvpbot.combat;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public final class CombatTargetSelector {

    private CombatTargetSelector() {
    }

    public static LivingEntity validateTarget(NPC npc, double range, LivingEntity currentTarget, int tickCounter, int lastDamageTick) {
        if (currentTarget == null) return null;
        if (currentTarget.isDead() || !currentTarget.isValid()) return null;
        if (!(npc.getEntity() instanceof LivingEntity botEntity)) return null;

        double distSq = botEntity.getLocation().distanceSquared(currentTarget.getLocation());
        if (distSq > range * range) return null;

        double meleeRangeSq = 3.0 * 3.0;
        if (tickCounter - lastDamageTick > 200 && distSq > meleeRangeSq) return null;

        return currentTarget;
    }
}
