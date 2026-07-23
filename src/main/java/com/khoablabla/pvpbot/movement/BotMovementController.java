// Phase 3.3: Throttled Pathfinder & Repath Cache
package com.khoablabla.pvpbot.movement;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.khoablabla.pvpbot.utils.SafeLocationFinder;

import java.util.Random;

public class BotMovementController {

    private Location lastTargetLocation = null;
    private int lastRepathTick = -20;
    private final Random random = new Random();

    private static final int REPATH_INTERVAL = 5;
    private static final double THROTTLE_DISTANCE_SQ = 2.25;

    public void handleMovement(NPC npc, LivingEntity target, int currentTick) {
        if (npc.getEntity() == null) return;

        if (currentTick - lastRepathTick < REPATH_INTERVAL) return;

        if (npc.getNavigator().isNavigating() && lastTargetLocation != null
                && target.getLocation().distanceSquared(lastTargetLocation) <= THROTTLE_DISTANCE_SQ) return;

        lastTargetLocation = target.getLocation().clone();
        lastRepathTick = currentTick;
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
