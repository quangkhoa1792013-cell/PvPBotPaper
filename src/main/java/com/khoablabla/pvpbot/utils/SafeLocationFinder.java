// Phase 2: Safe Spawn Offset and Height Boundary Guards
package com.khoablabla.pvpbot.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafeLocationFinder {

    private static final java.util.Set<Material> HAZARDOUS_BLOCKS = java.util.Set.of(
        Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.CACTUS,
        Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
        Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW, Material.NETHER_PORTAL
    );

    private SafeLocationFinder() {
    }

    public static Location findSafeLocation(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        if (isSafe(origin)) return origin;

        Location worldOrigin = origin.clone();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Location candidate = worldOrigin.clone().add(dx, dy, dz);
                    if (isSafe(candidate)) return candidate;
                }
            }
        }

        int startY = origin.getBlockY();
        int maxY = Math.min(origin.getWorld().getMaxHeight() - 2, startY + 5);
        for (int y = startY + 1; y <= maxY; y++) {
            Location candidate = origin.clone();
            candidate.setY(y);
            if (isSafe(candidate)) return candidate;
        }

        return null;
    }

    private static boolean isSafe(Location loc) {
        World world = loc.getWorld();
        int y = loc.getBlockY();
        if (y < world.getMinHeight() + 1 || y > world.getMaxHeight() - 2) return false;

        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block below = loc.clone().add(0, -1, 0).getBlock();

        if (!isNonSolid(feet) || !isNonSolid(head)) return false;
        if (HAZARDOUS_BLOCKS.contains(below.getType())) return false;
        if (feet.isLiquid() || head.isLiquid()) return false;
        return isSolidSupport(below);
    }

    private static boolean isNonSolid(Block block) {
        return block.isEmpty() || !block.getType().isSolid();
    }

    private static boolean isSolidSupport(Block block) {
        if (block.isEmpty() || block.isLiquid()) return false;
        Material type = block.getType();
        return type != Material.LAVA && type != Material.FIRE;
    }
}
