package com.pvpbot.stats;

import com.pvpbot.bot.BotManager;
import com.pvpbot.bot.CustomBot;
import com.pvpbot.web.DashboardWebServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsCollector {

    private final JavaPlugin plugin;
    private final BotManager botManager;
    private final StatsDatabase database;
    private final DashboardWebServer webServer;
    private int taskId;

    public StatsCollector(JavaPlugin plugin, BotManager botManager, StatsDatabase database, DashboardWebServer webServer) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.database = database;
        this.webServer = webServer;
    }

    public void start() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 100, 100);
    }

    public void stop() {
        if (taskId > 0) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void tick() {
        int bots = botManager.getBotCount();
        int players = Bukkit.getOnlinePlayers().size();
        double tps = getTps();

        database.recordServerSnapshot(bots, players, tps);
        database.checkMaxBots(bots);
        webServer.updateStats(bots, players, tps);
        webServer.broadcastSnapshot();
    }

    public void onBotSpawn(CustomBot bot) {
        database.recordBotSpawn(bot.getBotName());
        database.checkMaxBots(botManager.getBotCount());
    }

    public void onBotDeath(CustomBot bot) {
        database.recordBotDeath();
    }

    private double getTps() {
        try {
            Object mspt = Bukkit.class.getMethod("getAverageTickTime").invoke(null);
            double tickTime = ((Number) mspt).doubleValue();
            return Math.min(20.0, 1000.0 / Math.max(1, tickTime));
        } catch (Exception e) {
            return 20.0;
        }
    }
}
