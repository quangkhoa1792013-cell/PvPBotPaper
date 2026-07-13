package com.pvpbot.stats;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatsDatabase {

    private static final long RETENTION_DAYS = 30;
    private static StatsDatabase instance;

    private Connection connection;
    private final Map<String, Integer> nameCountCache = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupScheduler;

    public static StatsDatabase getInstance() {
        if (instance == null) {
            instance = new StatsDatabase();
        }
        return instance;
    }

    public void initialize(File dataFolder) {
        try {
            File dbFile = new File(dataFolder, "metrics.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            loadNameCache();
            startCleanupTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS server_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "active_bots INTEGER NOT NULL DEFAULT 0, " +
                    "online_players INTEGER NOT NULL DEFAULT 0, " +
                    "tps REAL NOT NULL DEFAULT 20.0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS bot_records (" +
                    "key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS bot_names (" +
                    "name TEXT PRIMARY KEY, spawn_count INTEGER NOT NULL DEFAULT 1)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_server_history_ts ON server_history(timestamp)");
        }
    }

    private void loadNameCache() {
        nameCountCache.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, spawn_count FROM bot_names")) {
            while (rs.next()) {
                nameCountCache.put(rs.getString("name"), rs.getInt("spawn_count"));
            }
        } catch (SQLException ignored) {}
    }

    private void startCleanupTask() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        cleanupScheduler.scheduleAtFixedRate(this::cleanupOldRecords, 1, 24, TimeUnit.HOURS);
    }

    public void shutdown() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    public void recordServerSnapshot(int activeBots, int onlinePlayers, double tps) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO server_history (timestamp, active_bots, online_players, tps) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, Instant.now().getEpochSecond());
            ps.setInt(2, activeBots);
            ps.setInt(3, onlinePlayers);
            ps.setDouble(4, Math.min(20.0, Math.max(0, tps)));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordBotSpawn(String name) {
        nameCountCache.merge(name, 1, Integer::sum);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO bot_names (name, spawn_count) VALUES (?, 1) " +
                "ON CONFLICT(name) DO UPDATE SET spawn_count = spawn_count + 1")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateRecord("total_bots_spawned", String.valueOf(getTotalBotsSpawned() + 1));
    }

    public void recordBotDeath() {}

    public void updateRecord(String key, String value) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO bot_records (key, value) VALUES (?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void checkMaxBots(int current) {
        int saved = getMaxConcurrentBots();
        if (current > saved) {
            updateRecord("max_concurrent_bots", String.valueOf(current));
        }
    }

    public int getTotalBotsSpawned() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT value FROM bot_records WHERE key = 'total_bots_spawned'")) {
            if (rs.next()) return Integer.parseInt(rs.getString("value"));
        } catch (Exception ignored) {}
        return 0;
    }

    public int getMaxConcurrentBots() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT value FROM bot_records WHERE key = 'max_concurrent_bots'")) {
            if (rs.next()) return Integer.parseInt(rs.getString("value"));
        } catch (Exception ignored) {}
        return 0;
    }

    public List<Map<String, Object>> getHistory(long sinceEpoch) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT timestamp, active_bots, online_players, tps FROM server_history " +
                "WHERE timestamp >= ? ORDER BY timestamp ASC")) {
            ps.setLong(1, sinceEpoch);
            ResultSet rs = ps.executeQuery();
            int skip = 0;
            int total = 0;
            while (rs.next()) { total++; }
            rs.close();
            if (total > 500) skip = total / 500;

            ps.setLong(1, sinceEpoch);
            rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                if (skip > 0 && count % skip != 0 && count < total - 1) {
                    count++;
                    continue;
                }
                Map<String, Object> point = new HashMap<>();
                point.put("t", rs.getLong("timestamp") * 1000L);
                point.put("b", rs.getInt("active_bots"));
                point.put("p", rs.getInt("online_players"));
                point.put("tps", rs.getDouble("tps"));
                result.add(point);
                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Map<String, Object>> getTopNames(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, spawn_count FROM bot_names ORDER BY spawn_count DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", rs.getString("name"));
                entry.put("count", rs.getInt("spawn_count"));
                result.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void cleanupOldRecords() {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM server_history WHERE timestamp < ?")) {
            long cutoff = Instant.now().minusSeconds(RETENTION_DAYS * 86400).getEpochSecond();
            ps.setLong(1, cutoff);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("[PvPBot Stats] Cleaned up " + deleted + " old records");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
