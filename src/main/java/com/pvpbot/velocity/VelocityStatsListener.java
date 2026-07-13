package com.pvpbot.velocity;

import com.pvpbot.stats.StatsDatabase;
import com.pvpbot.web.DashboardWebServer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.nio.charset.StandardCharsets;

public class VelocityStatsListener {

    private static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create("pvpbot", "stats");

    private final StatsDatabase database;
    private final DashboardWebServer webServer;

    public VelocityStatsListener(StatsDatabase database, DashboardWebServer webServer) {
        this.database = database;
        this.webServer = webServer;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        String message = new String(event.getData(), StandardCharsets.UTF_8);
        String[] parts = message.split("\\|");
        if (parts.length < 2) return;

        String type = parts[0];
        switch (type) {
            case "HEARTBEAT" -> {
                if (parts.length >= 4) {
                    try {
                        int bots = Integer.parseInt(parts[1]);
                        int players = Integer.parseInt(parts[2]);
                        double tps = Double.parseDouble(parts[3]);
                        database.recordServerSnapshot(bots, players, tps);
                        database.checkMaxBots(bots);
                        webServer.updateStats(bots, players, tps);
                        webServer.broadcastSnapshot();
                    } catch (NumberFormatException ignored) {}
                }
            }
            case "SPAWN" -> {
                if (parts.length >= 2) {
                    database.recordBotSpawn(parts[1]);
                }
            }
        }
    }
}
