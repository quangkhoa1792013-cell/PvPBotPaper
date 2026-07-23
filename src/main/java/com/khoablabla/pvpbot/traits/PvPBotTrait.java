// Phase 1: Citizens Trait Base Lifecycle
// Phase 2: Tablist & Player Simulation
// Phase 3: Core Melee Combat AI Integration
// Phase 3.3: Simplified tick sequence — no isJumping coordination
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
import com.khoablabla.pvpbot.movement.BotMovementController;

public class PvPBotTrait extends Trait {

    private int tickCounter = 0;
    private int idleTickCounter = 0;
    private int lastDamageTick = 0;
    private LivingEntity target = null;
    private final BotMovementController movementController = new BotMovementController();
    private final MeleeAttackController attackController = new MeleeAttackController();
    private static final double TARGET_RANGE = 256.0;

    public PvPBotTrait() {
        super("pvpbot");
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

        if (tickCounter % 10 == 0) {
            target = CombatTargetSelector.validateTarget(npc, TARGET_RANGE, target, tickCounter, lastDamageTick);
            if (target instanceof Player p
                    && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)) {
                target = null;
                npc.getNavigator().cancelNavigation();
            }
        }

        if (target != null && !target.isDead() && target.isValid()) {
            attackController.handleAttack(npc, target);
            movementController.handleMovement(npc, target, tickCounter);
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
