package com.pvpbot.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class AStarPathfinder {

    private static final int MAX_PATH_LENGTH = 64;
    private static final int MAX_ITERATIONS = 500;
    private static final double SQRT_2 = 1.41421356237;
    private static final double VERTICAL_COST = 1.2;

    private static final int[][] CARDINAL_DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
    private static final int[][] DIAGONAL_DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    public static class PathNode {
        public final int x, y, z;
        public double gCost, hCost, fCost;
        @Nullable public PathNode parent;

        public PathNode(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PathNode other)) return false;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return (x * 31 + y) * 31 + z;
        }
    }

    private static boolean isPassable(@NotNull Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty();
    }

    private static boolean isSolidGround(@NotNull Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty();
    }

    private static boolean isNodeValid(@NotNull Level level, int x, int y, int z) {
        return isPassable(level, x, y, z) && isPassable(level, x, y + 1, z) && isSolidGround(level, x, y - 1, z);
    }

    @Nullable
    public static List<BlockPos> findPath(@NotNull Level level, @NotNull BlockPos start, @NotNull BlockPos goal) {
        if (start.equals(goal)) return List.of(goal);

        if (!isNodeValid(level, start.getX(), start.getY(), start.getZ())) return null;
        if (!isNodeValid(level, goal.getX(), goal.getY(), goal.getZ())) return null;

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        HashMap<Long, PathNode> openMap = new HashMap<>();
        Set<Long> closedSet = new HashSet<>();

        PathNode startNode = new PathNode(start.getX(), start.getY(), start.getZ());
        startNode.gCost = 0;
        startNode.hCost = heuristic(startNode, goal);
        startNode.fCost = startNode.hCost;
        openSet.add(startNode);
        openMap.put(key(startNode), startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            PathNode current = openSet.poll();
            long cKey = key(current);
            if (closedSet.contains(cKey)) continue;
            closedSet.add(cKey);
            openMap.remove(cKey);

            if (current.x == goal.getX() && current.y == goal.getY() && current.z == goal.getZ()) {
                return reconstructPath(current);
            }

            int cx = current.x, cy = current.y, cz = current.z;

            for (int[] dir : CARDINAL_DIRS) {
                evaluateNeighbor(level, cx, cy, cz, cx + dir[0], cz + dir[1], goal, current, openSet, openMap, closedSet, 1.0);
            }

            for (int[] dir : DIAGONAL_DIRS) {
                int nx = cx + dir[0], nz = cz + dir[1];
                if (isPassable(level, cx + dir[0], cy, cz) && isPassable(level, cx, cy, cz + dir[1])) {
                    evaluateNeighbor(level, cx, cy, cz, nx, nz, goal, current, openSet, openMap, closedSet, SQRT_2);
                }
            }

            iterations++;
        }

        return null;
    }

    private static void evaluateNeighbor(
            @NotNull Level level, int cx, int cy, int cz,
            int nx, int nz, @NotNull BlockPos goal,
            @NotNull PathNode current,
            @NotNull PriorityQueue<PathNode> openSet,
            @NotNull HashMap<Long, PathNode> openMap,
            @NotNull Set<Long> closedSet,
            double horizontalCost
    ) {
        if (isNodeValid(level, nx, cy, nz)) {
            addOrUpdateNode(nx, cy, nz, current, goal, openSet, openMap, closedSet, horizontalCost);
        }

        if (isPassable(level, nx, cy + 1, nz) && isPassable(level, nx, cy, nz)
                && isSolidGround(level, nx, cy - 1, nz)
                && isPassable(level, nx, cy + 2, nz)) {
            addOrUpdateNode(nx, cy + 1, nz, current, goal, openSet, openMap, closedSet, horizontalCost + VERTICAL_COST);
        }

        if (cy - 1 > level.getMinY()
                && isNodeValid(level, nx, cy - 1, nz)
                && isPassable(level, nx, cy, nz)) {
            addOrUpdateNode(nx, cy - 1, nz, current, goal, openSet, openMap, closedSet, horizontalCost + 0.5);
        }

        if (cy - 2 > level.getMinY()
                && isNodeValid(level, nx, cy - 2, nz)
                && isPassable(level, nx, cy - 1, nz)
                && isPassable(level, nx, cy, nz)) {
            addOrUpdateNode(nx, cy - 2, nz, current, goal, openSet, openMap, closedSet, horizontalCost + 0.6);
        }
    }

    private static void addOrUpdateNode(
            int x, int y, int z,
            @NotNull PathNode parent,
            @NotNull BlockPos goal,
            @NotNull PriorityQueue<PathNode> openSet,
            @NotNull HashMap<Long, PathNode> openMap,
            @NotNull Set<Long> closedSet,
            double stepCost
    ) {
        long key = key(x, y, z);
        if (closedSet.contains(key)) return;

        double newG = parent.gCost + stepCost;
        PathNode existing = openMap.get(key);

        if (existing != null) {
            if (newG < existing.gCost) {
                existing.gCost = newG;
                existing.fCost = newG + existing.hCost;
                existing.parent = parent;
                openSet.remove(existing);
                openSet.add(existing);
            }
        } else {
            PathNode node = new PathNode(x, y, z);
            node.gCost = newG;
            node.hCost = heuristic(node, goal);
            node.fCost = newG + node.hCost;
            node.parent = parent;
            openSet.add(node);
            openMap.put(key, node);
        }
    }

    @NotNull
    private static List<BlockPos> reconstructPath(@NotNull PathNode end) {
        List<BlockPos> path = new ArrayList<>();
        PathNode current = end;
        while (current != null) {
            path.add(0, current.toBlockPos());
            current = current.parent;
        }
        if (path.size() > MAX_PATH_LENGTH + 1) {
            return path.subList(0, MAX_PATH_LENGTH + 1);
        }
        return path;
    }

    private static double heuristic(@NotNull PathNode node, @NotNull BlockPos goal) {
        double dx = Math.abs(node.x - goal.getX());
        double dy = Math.abs(node.y - goal.getY());
        double dz = Math.abs(node.z - goal.getZ());
        double diag = Math.min(Math.min(dx, dz), dy);
        double straight = Math.max(Math.max(dx, dz), dy) - diag;
        return SQRT_2 * diag + straight + VERTICAL_COST * dy;
    }

    private static long key(@NotNull PathNode node) {
        return key(node.x, node.y, node.z);
    }

    private static long key(int x, int y, int z) {
        long hash = x;
        hash = hash * 31 + y;
        hash = hash * 31 + z;
        return hash;
    }

    @Nullable
    public static PathNode findNextNode(@NotNull Level level, @NotNull BlockPos start, @NotNull BlockPos goal) {
        List<BlockPos> path = findPath(level, start, goal);
        if (path == null || path.size() < 2) return null;
        BlockPos next = path.get(1);
        return new PathNode(next.getX(), next.getY(), next.getZ());
    }
}
