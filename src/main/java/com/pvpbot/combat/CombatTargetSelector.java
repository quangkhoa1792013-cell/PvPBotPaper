package com.pvpbot.combat;

import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.CustomBot;
import com.pvpbot.faction.Faction;
import com.pvpbot.faction.FactionManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
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

        FactionManager factionManager = getFactionManager();

        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (!canTarget(bot, candidate, factionManager)) continue;
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

    private static boolean canTarget(CustomBot bot, LivingEntity target, @Nullable FactionManager fm) {
        if (fm == null) return true;
        Faction botFaction = fm.getFactionOf(bot.getUUID());
        Faction targetFaction = fm.getFactionOf(target.getUUID());
        if (botFaction == null || targetFaction == null) return true;
        if (!botFaction.equals(targetFaction)) {
            if (botFaction.isEnemy(targetFaction.getName())) return true;
            return true;
        }
        return botFaction.isFriendlyFire();
    }

    @Nullable
    private static FactionManager getFactionManager() {
        org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin("PvPBot");
        if (p instanceof com.pvpbot.PvPBotPlugin pp) {
            return pp.getFactionManager();
        }
        return null;
    }
}
