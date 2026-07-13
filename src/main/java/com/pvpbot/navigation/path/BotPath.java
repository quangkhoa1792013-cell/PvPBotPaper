package com.pvpbot.navigation.path;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BotPath {

    public enum WalkType {
        WALK,
        SPRINT,
        BHOP
    }

    private final String name;
    private final List<Location> waypoints;
    private boolean loop;
    private WalkType walkType;
    private boolean visible;

    public BotPath(@NotNull String name) {
        this.name = name;
        this.waypoints = new ArrayList<>();
        this.loop = false;
        this.walkType = WalkType.WALK;
        this.visible = false;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public List<Location> getWaypoints() {
        return waypoints;
    }

    public boolean addWaypoint(@NotNull Location loc) {
        return waypoints.add(loc.clone());
    }

    public void clearWaypoints() {
        waypoints.clear();
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    @NotNull
    public WalkType getWalkType() {
        return walkType;
    }

    public void setWalkType(@NotNull WalkType walkType) {
        this.walkType = walkType;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    @Nullable
    public Location getWaypoint(int index) {
        if (index < 0 || index >= waypoints.size()) return null;
        return waypoints.get(index);
    }

    public double getTotalLength() {
        if (waypoints.size() < 2) return 0;
        double total = 0;
        for (int i = 1; i < waypoints.size(); i++) {
            total += waypoints.get(i - 1).distance(waypoints.get(i));
        }
        return total;
    }
}
