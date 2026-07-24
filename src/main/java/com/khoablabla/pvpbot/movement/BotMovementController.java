// Phase 4.1.2: B-hop toggle + dynamic speed modulation
package com.khoablabla.pvpbot.movement;

import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.khoablabla.pvpbot.traits.PvPBotTrait;
import com.khoablabla.pvpbot.utils.SafeLocationFinder;

import java.util.Random;
import java.util.UUID;

public class BotMovementController {

    private Location lastTargetLocation = null;
    private Location lastProgressLocation = null;
    private UUID lastTargetId = null;
    private int lastRepathTick = -100;
    private int lastProgressTick = 0;
    private final Random random = new Random();

    private static final int REPATH_INTERVAL = 5;
    private static final int STUCK_TICKS = 20;
    private static final double TARGET_REPATH_DISTANCE_SQ = 2.25;
    private static final double PROGRESS_DISTANCE_SQ = 0.04;
    private static final double EXTREME_DISTANCE_THRESHOLD = 128.0;
    private static final double PURSUIT_MARGIN = 1.25;

    public LivingEntity handleMovement(NPC npc, LivingEntity target, int currentTick, PvPBotTrait trait) {
        if (npc.getEntity() == null) return target;
        if (!(npc.getEntity() instanceof Player botPlayer)) return target;

        double baseMoveSpeed = trait.getSetting("move-speed", Double.class);
        double attackRange = trait.getSetting("melee-range", Double.class);
        boolean bhop = trait.getSetting("bhop", Boolean.class);

        Location botLoc = botPlayer.getLocation();
        Location targetLoc = target.getLocation();
        double distance = botLoc.distance(targetLoc);

        float effectiveSpeed;
        if (bhop && distance > 5.0) {
            effectiveSpeed = (float) (baseMoveSpeed * 1.3);
        } else {
            effectiveSpeed = (float) baseMoveSpeed;
        }

        if (distance > EXTREME_DISTANCE_THRESHOLD) {
            npc.getNavigator().cancelNavigation();
            return null;
        }

        configureCombatNavigation(npc, effectiveSpeed, attackRange);
        updateProgress(botLoc, currentTick);

        boolean targetChanged = !target.getUniqueId().equals(lastTargetId);
        boolean targetMoved = lastTargetLocation == null
                || targetLoc.distanceSquared(lastTargetLocation) >= TARGET_REPATH_DISTANCE_SQ;
        boolean stuckOutsideMelee = distance > attackRange
                && currentTick - lastProgressTick >= STUCK_TICKS;
        boolean repathReady = currentTick - lastRepathTick >= REPATH_INTERVAL;
        boolean shouldRepath = targetChanged
                || (!npc.getNavigator().isNavigating() && repathReady)
                || (stuckOutsideMelee && repathReady)
                || (targetMoved && repathReady);

        if (!shouldRepath) return target;

        lastTargetLocation = targetLoc.clone();
        lastTargetId = target.getUniqueId();
        lastRepathTick = currentTick;
        npc.getNavigator().setTarget(target, true);

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

    private void configureCombatNavigation(NPC npc, float speed, double attackRange) {
        NavigatorParameters params = npc.getNavigator().getLocalParameters();
        params.speedModifier(speed);
        params.attackRange(attackRange);
        params.distanceMargin(PURSUIT_MARGIN);
        params.pathDistanceMargin(PURSUIT_MARGIN);
        params.straightLineTargetingDistance((float) EXTREME_DISTANCE_THRESHOLD);
        params.range((float) EXTREME_DISTANCE_THRESHOLD);
        params.updatePathRate(REPATH_INTERVAL);
    }

    private void updateProgress(Location botLoc, int currentTick) {
        if (lastProgressLocation == null || !botLoc.getWorld().equals(lastProgressLocation.getWorld())) {
            lastProgressLocation = botLoc.clone();
            lastProgressTick = currentTick;
            return;
        }

        if (botLoc.distanceSquared(lastProgressLocation) >= PROGRESS_DISTANCE_SQ) {
            lastProgressLocation = botLoc.clone();
            lastProgressTick = currentTick;
        }
    }
}
