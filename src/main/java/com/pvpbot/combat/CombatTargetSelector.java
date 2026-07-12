package com.pvpbot.combat;

import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.CustomBot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CombatTargetSelector {

    @Nullable
    public static LivingEntity findTarget(ServerLevel level, CustomBot bot, BotSettings settings) {
        double viewDist = settings.getViewDistance();
        double sqDist = viewDist * viewDist;

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                bot.getBoundingBox().inflate(viewDist),
                entity -> isValidTarget(bot, entity, settings, sqDist)
        );

        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distSq = bot.distanceToSqr(candidate);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = candidate;
            }
        }
        return closest;
    }

    private static boolean isValidTarget(CustomBot bot, LivingEntity target, BotSettings settings, double viewDistSq) {
        if (target == bot) return false;
        if (!target.isAlive()) return false;
        if (target.isRemoved()) return false;
        if (bot.distanceToSqr(target) > viewDistSq) return false;

        if (target instanceof CustomBot && settings.isTargetBots()) return true;
        if (target instanceof Player && settings.isTargetPlayers()) return true;
        if (target instanceof Mob && settings.isTargetMobs()) return true;

        return false;
    }
}
