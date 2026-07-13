package com.pvpbot;

import com.pvpbot.bot.BotManager;
import com.pvpbot.command.PvPBotCommand;
import com.pvpbot.faction.FactionManager;
import com.pvpbot.gui.SettingsGUI;
import com.pvpbot.kit.KitManager;
import com.pvpbot.navigation.path.PathManager;
import com.pvpbot.stats.StatsCollector;
import com.pvpbot.stats.StatsDatabase;
import com.pvpbot.web.DashboardWebServer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class PvPBotPlugin extends JavaPlugin {

    private BotManager botManager;
    private KitManager kitManager;
    private PathManager pathManager;
    private FactionManager factionManager;
    private SettingsGUI settingsGUI;
    private StatsDatabase statsDatabase;
    private DashboardWebServer dashboardWebServer;
    private StatsCollector statsCollector;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.botManager = new BotManager();
        this.kitManager = new KitManager(getDataFolder());
        this.pathManager = new PathManager(this);
        this.factionManager = new FactionManager(this);
        this.settingsGUI = new SettingsGUI(this, botManager, kitManager, factionManager, pathManager);

        this.statsDatabase = StatsDatabase.getInstance();
        this.statsDatabase.initialize(getDataFolder());

        this.dashboardWebServer = new DashboardWebServer();
        this.dashboardWebServer.start(8080);

        this.statsCollector = new StatsCollector(this, botManager, statsDatabase, dashboardWebServer);
        this.statsCollector.start();

        getServer().getPluginManager().registerEvents(settingsGUI, this);
        registerCommand();
        getLogger().info("PvPBot v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (statsCollector != null) statsCollector.stop();
        if (dashboardWebServer != null) dashboardWebServer.stop();
        if (statsDatabase != null) statsDatabase.shutdown();
        if (factionManager != null) {
            factionManager.stopGradualTeleport();
        }
        if (pathManager != null) {
            pathManager.stopParticleTask();
        }
        if (botManager != null) {
            int count = botManager.getBotCount();
            botManager.removeAll();
            if (count > 0) {
                getLogger().info("Cleaned up " + count + " bot(s) on shutdown");
            }
        }
        getLogger().info("PvPBot disabled");
    }

    private void registerCommand() {
        PluginCommand command = getCommand("pvpbot");
        if (command != null) {
            PvPBotCommand executor = new PvPBotCommand(botManager, kitManager, pathManager,
                    factionManager, settingsGUI);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Failed to register /pvpbot command - check plugin.yml");
        }
    }

    @NotNull
    public BotManager getBotManager() {
        return botManager;
    }

    @NotNull
    public KitManager getKitManager() {
        return kitManager;
    }

    @NotNull
    public PathManager getPathManager() {
        return pathManager;
    }

    @NotNull
    public FactionManager getFactionManager() {
        return factionManager;
    }

    @NotNull
    public SettingsGUI getSettingsGUI() {
        return settingsGUI;
    }
}
