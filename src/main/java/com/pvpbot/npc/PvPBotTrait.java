package com.pvpbot.npc;

import com.pvpbot.PvPBotPlugin;
import com.pvpbot.bot.BotSettings;
import net.citizensnpcs.api.trait.Trait;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PvPBotTrait extends Trait {

    private BotSettings settings;
    private int tickCounter = 0;

    public PvPBotTrait() {
        super("pvpbot");
    }

    @Override
    public void onAttach() {
        this.settings = new BotSettings();
        this.settings.loadFromConfig(PvPBotPlugin.getInstance().getConfig());
    }

    @Override
    public void run() {
        if (!npc.isSpawned()) return;

        try {
            Player player = (Player) npc.getEntity();
            if (player == null) return;

            ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            tickCounter++;

            if (tickCounter % 100 == 0) {
                PvPBotPlugin.getInstance().getLogger().info(
                    "PvPBot Trait ticking for " + npc.getName()
                );
            }
        } catch (Exception e) {
            PvPBotPlugin.getInstance().getLogger().warning(
                "Error in PvPBotTrait tick: " + e.getMessage()
            );
        }
    }

    public BotSettings getSettings() {
        return settings;
    }

    public void setSettings(BotSettings settings) {
        this.settings = settings;
    }
}
