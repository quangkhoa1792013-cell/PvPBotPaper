package com.pvpbot.navigation.path;

import com.pvpbot.bot.CustomBot;
import com.pvpbot.navigation.path.BotPath.WalkType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathManager {

    private final File pathsFile;
    private final Map<String, BotPath> paths;
    private final JavaPlugin plugin;
    private int particleTaskId;

    public PathManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.pathsFile = new File(plugin.getDataFolder(), "paths.yml");
        this.paths = new HashMap<>();
        this.particleTaskId = -1;
        loadPaths();
        startParticleTask();
    }

    private void loadPaths() {
        if (!pathsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(pathsFile);
        for (String name : config.getKeys(false)) {
            BotPath path = new BotPath(name);
            path.setLoop(config.getBoolean(name + ".loop", false));
            try {
                path.setWalkType(WalkType.valueOf(config.getString(name + ".walk-type", "WALK").toUpperCase()));
            } catch (IllegalArgumentException e) {
                path.setWalkType(WalkType.WALK);
            }
            path.setVisible(config.getBoolean(name + ".visible", false));
            var section = config.getConfigurationSection(name + ".waypoints");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    Location loc = (Location) section.get(key);
                    if (loc != null) {
                        path.addWaypoint(loc);
                    }
                }
            }
            paths.put(name.toLowerCase(), path);
        }
    }

    public void savePaths() {
        YamlConfiguration config = new YamlConfiguration();
        for (BotPath path : paths.values()) {
            String prefix = path.getName();
            config.set(prefix + ".loop", path.isLoop());
            config.set(prefix + ".walk-type", path.getWalkType().name());
            config.set(prefix + ".visible", path.isVisible());
            List<Location> wps = path.getWaypoints();
            for (int i = 0; i < wps.size(); i++) {
                config.set(prefix + ".waypoints." + i, wps.get(i));
            }
        }
        try {
            config.save(pathsFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[PvPBot] Failed to save paths.yml: " + e.getMessage());
        }
    }

    private void startParticleTask() {
        particleTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (BotPath path : paths.values()) {
                if (!path.isVisible()) continue;
                List<Location> wps = path.getWaypoints();
                if (wps.size() < 2) continue;
                for (int i = 0; i < wps.size(); i++) {
                    Location loc = wps.get(i);
                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0.3, 0.3, 0.3, 0);
                    if (i > 0) {
                        Location from = wps.get(i - 1);
                        drawParticleLine(from, loc, loc.getWorld());
                    }
                }
            }
        }, 0L, 15L).getTaskId();
    }

    private void drawParticleLine(@NotNull Location from, @NotNull Location to, @NotNull org.bukkit.World world) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length < 0.1) return;
        direction.normalize();
        int steps = Math.max(1, (int) (length / 0.5));
        Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0f);
        for (int i = 0; i <= steps; i++) {
            Location point = from.clone().add(direction.clone().multiply((length * i) / steps));
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
        }
    }

    public void stopParticleTask() {
        if (particleTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
        }
    }

    @Nullable
    public BotPath getPath(@NotNull String name) {
        return paths.get(name.toLowerCase());
    }

    @NotNull
    public Collection<BotPath> getAllPaths() {
        return paths.values();
    }

    public boolean pathExists(@NotNull String name) {
        return paths.containsKey(name.toLowerCase());
    }

    public boolean createPath(@NotNull String name) {
        if (pathExists(name)) return false;
        paths.put(name.toLowerCase(), new BotPath(name));
        savePaths();
        return true;
    }

    public boolean deletePath(@NotNull String name) {
        if (!pathExists(name)) return false;
        paths.remove(name.toLowerCase());
        savePaths();
        return true;
    }

    public boolean addPoint(@NotNull String name, @NotNull Player player) {
        BotPath path = paths.get(name.toLowerCase());
        if (path == null) return false;
        path.addWaypoint(player.getLocation());
        savePaths();
        return true;
    }

    public boolean removePoint(@NotNull String name, int index) {
        BotPath path = paths.get(name.toLowerCase());
        if (path == null) return false;
        List<Location> wps = path.getWaypoints();
        if (index < 0 || index >= wps.size()) return false;
        wps.remove(index);
        savePaths();
        return true;
    }

    public boolean setLoop(@NotNull String name, boolean loop) {
        BotPath path = paths.get(name.toLowerCase());
        if (path == null) return false;
        path.setLoop(loop);
        savePaths();
        return true;
    }

    public boolean setWalkType(@NotNull String name, @NotNull WalkType walkType) {
        BotPath path = paths.get(name.toLowerCase());
        if (path == null) return false;
        path.setWalkType(walkType);
        savePaths();
        return true;
    }

    public boolean setVisible(@NotNull String name, boolean visible) {
        BotPath path = paths.get(name.toLowerCase());
        if (path == null) return false;
        path.setVisible(visible);
        savePaths();
        return true;
    }

    public void distributeBots(@NotNull BotPath botPath, @NotNull List<CustomBot> bots) {
        List<Location> wps = botPath.getWaypoints();
        if (wps.size() < 2 || bots.isEmpty()) return;

        double totalLength = botPath.getTotalLength();
        if (totalLength <= 0) return;

        double segmentLength = totalLength / bots.size();

        for (int b = 0; b < bots.size(); b++) {
            double targetDist = segmentLength * b;
            Location loc = getPointAtDistance(botPath, targetDist);
            if (loc == null) continue;
            CustomBot bot = bots.get(b);
            bot.setPos(loc.getX(), loc.getY(), loc.getZ());
            bot.setYRot(loc.getYaw());
            bot.setXRot(loc.getPitch());
            org.bukkit.entity.Player bp = bot.getBukkitEntity();
            if (bp != null) bp.teleport(loc);

            bot.stopMovement();
            assignPathToBot(bot, botPath);
        }
    }

    @Nullable
    public static Location getPointAtDistance(@NotNull BotPath path, double distance) {
        List<Location> wps = path.getWaypoints();
        if (wps.size() < 2) return null;
        double accumulated = 0;
        for (int i = 1; i < wps.size(); i++) {
            Location a = wps.get(i - 1);
            Location b = wps.get(i);
            double segLen = a.distance(b);
            if (accumulated + segLen >= distance || i == wps.size() - 1) {
                double frac = (distance - accumulated) / segLen;
                frac = Math.max(0, Math.min(1, frac));
                double x = a.getX() + (b.getX() - a.getX()) * frac;
                double y = a.getY() + (b.getY() - a.getY()) * frac;
                double z = a.getZ() + (b.getZ() - a.getZ()) * frac;
                float yaw = a.getYaw() + (b.getYaw() - a.getYaw()) * (float) frac;
                float pitch = a.getPitch() + (b.getPitch() - a.getPitch()) * (float) frac;
                return new Location(a.getWorld(), x, y, z, yaw, pitch);
            }
            accumulated += segLen;
        }
        return null;
    }

    public void assignPathToBot(@NotNull CustomBot bot, @NotNull BotPath path) {
        bot.stopMovement();
        bot.getMovementController().setCurrentPath(path);
    }

    public void removeBotFromPath(@NotNull CustomBot bot) {
        bot.getMovementController().clearActivePath();
    }
}
