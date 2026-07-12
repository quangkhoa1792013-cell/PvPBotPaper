package com.pvpbot.navigation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ArrowPrediction {

    private static final double ARROW_VELOCITY = 3.0;
    private static final double GRAVITY_COMPENSATION = 0.006;

    @NotNull
    public static Vec3 predictPosition(@NotNull LivingEntity target, double distance) {
        double ticks = distance / ARROW_VELOCITY;
        Vec3 velocity = target.getDeltaMovement();
        Vec3 pos = target.position().add(0, target.getEyeHeight() * 0.7, 0);
        double predictedY = pos.y + velocity.y * ticks + GRAVITY_COMPENSATION * distance * distance;
        return new Vec3(
                pos.x + velocity.x * ticks,
                predictedY,
                pos.z + velocity.z * ticks
        );
    }

    public static double arrowVelocity() {
        return ARROW_VELOCITY;
    }
}
