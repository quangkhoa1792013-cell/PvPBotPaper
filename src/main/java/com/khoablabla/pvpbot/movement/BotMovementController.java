// Phase 3.3.3: Hybrid Waypoint Pursuit — Interim Waypoint for Long-Distance Chase
package com.khoablabla.pvpbot.movement;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.khoablabla.pvpbot.utils.SafeLocationFinder;

import java.util.Random;

public class BotMovementController {

    private Location lastTargetLocation = null;
    private int lastRepathTick = -20;
    private final Random random = new Random();

    private static final int REPATH_INTERVAL = 5;
    private static final double THROTTLE_DISTANCE_SQ = 2.25;
    private static final double LONG_DISTANCE_THRESHOLD = 32.0;
    private static final double EXTREME_DISTANCE_THRESHOLD = 128.0;
    private static final double WAYPOINT_OFFSET = 24.0;

    public LivingEntity handleMovement(NPC npc, LivingEntity target, int currentTick) {
        if (npc.getEntity() == null) return target;
        if (!(npc.getEntity() instanceof Player botPlayer)) return target;

        Location botLoc = botPlayer.getLocation();
        Location targetLoc = target.getLocation();
        double distance = botLoc.distance(targetLoc);

        if (distance > EXTREME_DISTANCE_THRESHOLD) {
            npc.getNavigator().cancelNavigation();
            return null;
        }

        if (currentTick - lastRepathTick < REPATH_INTERVAL) return target;

        if (npc.getNavigator().isNavigating() && lastTargetLocation != null
                && targetLoc.distanceSquared(lastTargetLocation) <= THROTTLE_DISTANCE_SQ) return target;

        lastTargetLocation = targetLoc.clone();
        lastRepathTick = currentTick;

        if (distance <= LONG_DISTANCE_THRESHOLD) {
            npc.getNavigator().setTarget(target, true);
        } else {
            Vector direction = targetLoc.toVector().subtract(botLoc.toVector());
            direction.setY(0);
            direction.normalize();
            Location interim = botLoc.clone().add(direction.multiply(WAYPOINT_OFFSET));
            Location safe = SafeLocationFinder.findSafeLocation(interim);
            if (safe == null) {
                safe = interim.getWorld().getHighestBlockAt(interim).getLocation().add(0, 1, 0);
            }
            npc.getNavigator().setTarget(safe, false);
        }

        npc.getNavigator().getLocalParameters().speedModifier(1.5F);
        return target;
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
