// Phase 3: Core Melee Combat AI — Pursuit Navigation & Idle Wander
package com.khoablabla.pvpbot.movement;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.khoablabla.pvpbot.utils.SafeLocationFinder;

import java.util.Random;

public class BotMovementController {

    private final Random random = new Random();

    public void handleMovement(NPC npc, LivingEntity target, int currentTick, boolean isJumping) {
        if (npc.getEntity() == null) return;

        if (isJumping) {
            if (npc.getNavigator().isNavigating()) {
                npc.getNavigator().cancelNavigation();
            }
            return;
        }

        npc.getNavigator().setTarget(target, true);
        npc.getNavigator().getLocalParameters().speedModifier(1.5F);
    }

    public void handleIdleWander(NPC npc) {
        if (!(npc.getEntity() instanceof Player botPlayer)) return;
        if (npc.getNavigator().isNavigating()) return;

        Location origin = botPlayer.getLocation();
        for (int attempt = 0; attempt < 5; attempt++) {
            double dx = (random.nextDouble() - 0.5) * 10;
            double dz = (random.nextDouble() - 0.5) * 10;
            Location candidate = origin.clone().add(dx, 0, dz);
            Location safe = SafeLocationFinder.findSafeLocation(candidate);
            if (safe != null) {
                npc.getNavigator().setTarget(safe);
                return;
            }
        }
    }
}
