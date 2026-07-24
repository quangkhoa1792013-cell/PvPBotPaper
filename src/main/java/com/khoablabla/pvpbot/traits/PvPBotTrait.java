// Phase 1: Citizens Trait Base Lifecycle
// Phase 2: Tablist & Player Simulation
// Phase 3: Core Melee Combat AI Integration
// Phase 3.3: Simplified tick sequence — no isJumping coordination
// Phase 4: Per-NPC local settings overrides
// Phase 4.1.1: Functional settings — combat, auto-target, target-players, target-bots
// Phase 4.1.3: target-mobs, attack-invincible, shield defense controller
package com.khoablabla.pvpbot.traits;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.khoablabla.pvpbot.PvPBot;
import com.khoablabla.pvpbot.combat.CombatTargetSelector;
import com.khoablabla.pvpbot.combat.MeleeAttackController;
import com.khoablabla.pvpbot.combat.ShieldDefenseController;
import com.khoablabla.pvpbot.config.SettingsRegistry;
import com.khoablabla.pvpbot.movement.BotMovementController;

import java.util.HashMap;
import java.util.Map;

public class PvPBotTrait extends Trait {

    private int tickCounter = 0;
    private int idleTickCounter = 0;
    private int lastDamageTick = 0;
    private LivingEntity target = null;
    private final BotMovementController movementController = new BotMovementController();
    private final MeleeAttackController attackController = new MeleeAttackController();
    private final ShieldDefenseController shieldController = new ShieldDefenseController();
    private final Map<String, Object> localSettings = new HashMap<>();

    public PvPBotTrait() {
        super("pvpbot");
    }

    public <T> T getSetting(String key, Class<T> type) {
        if (localSettings.containsKey(key)) {
            return SettingsRegistry.getInstance().coerce(localSettings.get(key), type);
        }
        return SettingsRegistry.getInstance().getGlobal(key, type);
    }

    public void setLocalSetting(String key, Object value) {
        SettingsRegistry reg = SettingsRegistry.getInstance();
        SettingsRegistry.SettingMeta<?> meta = reg.getMeta(key);
        if (meta == null) return;
        Object coerced = reg.validateAndCast(value, meta);
        if (coerced != null) {
            localSettings.put(key, coerced);
        }
    }

    public void clearLocalSetting(String key) {
        localSettings.remove(key);
    }

    public Map<String, Object> getLocalSettings() {
        return localSettings;
    }

    public void setTarget(LivingEntity newTarget) {
        this.target = newTarget;
        if (newTarget != null) {
            lastDamageTick = tickCounter;
        }
    }

    @Override
    public void onAttach() {
        if (npc.getEntity() instanceof Player) {
            JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
            plugin.getLogger().info("PvPBotTrait attached to NPC " + npc.getName() + " (ID: " + npc.getId() + ")");
        } else if (npc.getEntity() != null) {
            JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
            plugin.getLogger().warning("PvPBotTrait attached to NPC " + npc.getName()
                    + " (ID: " + npc.getId() + ") but its entity is not a Player — PvP functionality may be limited.");
        }
    }

    @Override
    public void onSpawn() {
        if (npc.getEntity() instanceof Player player) {
            npc.setProtected(false);
            npc.data().set(NPC.Metadata.DAMAGE_OTHERS, true);

            JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
            plugin.getLogger().info("PvPBot NPC '" + npc.getName() + "' (ID: " + npc.getId()
                    + ") has spawned in world: " + player.getWorld().getName());
        }
    }

    @Override
    public void onDespawn() {
        JavaPlugin plugin = JavaPlugin.getPlugin(PvPBot.class);
        plugin.getLogger().info("PvPBot NPC (ID: " + npc.getId() + ") has despawned from world.");
    }

    @Override
    public void run() {
        tickCounter++;

        boolean combat = getSetting("combat", Boolean.class);
        if (!combat) {
            target = null;
            npc.getNavigator().cancelNavigation();
            idleTickCounter++;
            if (idleTickCounter >= 100) {
                idleTickCounter = 0;
                movementController.handleIdleWander(npc);
            }
            return;
        }

        if (tickCounter % 10 == 0) {
            double viewDist = getSetting("view-distance", Double.class);
            boolean targetPlayers = getSetting("target-players", Boolean.class);
            boolean targetBots = getSetting("target-bots", Boolean.class);
            boolean targetMobs = getSetting("target-mobs", Boolean.class);
            boolean attackInvincible = getSetting("attack-invincible", Boolean.class);

            if (target == null) {
                boolean autoTarget = getSetting("auto-target", Boolean.class);
                if (autoTarget) {
                    target = CombatTargetSelector.scanForTarget(npc, viewDist,
                            targetPlayers, targetBots, targetMobs, attackInvincible);
                }
            } else {
                target = CombatTargetSelector.validateTarget(npc, viewDist, target, tickCounter,
                        lastDamageTick, targetPlayers, targetBots, targetMobs, attackInvincible);
            }

            if (!attackInvincible && target instanceof Player p
                    && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)) {
                target = null;
                npc.getNavigator().cancelNavigation();
            }
        }

        if (target != null && !target.isDead() && target.isValid()) {
            attackController.handleAttack(npc, target, this);
            target = movementController.handleMovement(npc, target, tickCounter, this);
            shieldController.handleDefense(npc, target, this);
            idleTickCounter = 0;
        } else {
            target = null;
            npc.getNavigator().cancelNavigation();

            idleTickCounter++;
            if (idleTickCounter >= 100) {
                idleTickCounter = 0;
                movementController.handleIdleWander(npc);
            }
        }
    }
}
