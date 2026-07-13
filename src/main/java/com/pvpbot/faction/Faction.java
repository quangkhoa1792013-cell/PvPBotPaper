package com.pvpbot.faction;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {

    private final String name;
    private final Set<UUID> members;
    private final Set<String> enemies;
    private boolean friendlyFire;

    public Faction(@NotNull String name) {
        this.name = name;
        this.members = new HashSet<>();
        this.enemies = new HashSet<>();
        this.friendlyFire = false;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Set<UUID> getMembers() {
        return members;
    }

    public boolean addMember(@NotNull UUID uuid) {
        return members.add(uuid);
    }

    public boolean removeMember(@NotNull UUID uuid) {
        return members.remove(uuid);
    }

    public boolean hasMember(@NotNull UUID uuid) {
        return members.contains(uuid);
    }

    public int getMemberCount() {
        return members.size();
    }

    @NotNull
    public Set<String> getEnemies() {
        return enemies;
    }

    public boolean isEnemy(@NotNull String factionName) {
        return enemies.contains(factionName.toLowerCase());
    }

    public boolean addEnemy(@NotNull String factionName) {
        return enemies.add(factionName.toLowerCase());
    }

    public boolean removeEnemy(@NotNull String factionName) {
        return enemies.remove(factionName.toLowerCase());
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }
}
