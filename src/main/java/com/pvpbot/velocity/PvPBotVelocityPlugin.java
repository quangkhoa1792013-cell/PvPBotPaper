package com.pvpbot.velocity;

import com.google.inject.Inject;
import com.pvpbot.stats.StatsDatabase;
import com.pvpbot.web.DashboardWebServer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "pvpbot", name = "PvPBot", version = "1.0.0",
        url = "https://github.com/pvpbot", description = "Advanced PvP bot system dashboard proxy bridge")
public class PvPBotVelocityPlugin {

    private static final String CHANNEL = "pvpbot:stats";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private StatsDatabase database;
    private DashboardWebServer webServer;

    @Inject
    public PvPBotVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        database = StatsDatabase.getInstance();
        database.initialize(dataDirectory.toFile());

        webServer = new DashboardWebServer();
        webServer.start(8081);

        MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.create("pvpbot", "stats");
        server.getChannelRegistrar().register(channel);
        server.getEventManager().register(this, new VelocityStatsListener(database, webServer));

        logger.info("PvPBot Velocity plugin enabled");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (webServer != null) webServer.stop();
        if (database != null) database.shutdown();
        logger.info("PvPBot Velocity plugin disabled");
    }
}
