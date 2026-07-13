package com.pvpbot.navigation;

import com.pvpbot.bot.BotSettings;
import com.pvpbot.bot.CustomBot;
import com.pvpbot.combat.CombatTargetSelector;
import com.pvpbot.navigation.path.BotPath;
import com.pvpbot.navigation.path.BotPath.WalkType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BotMovementController {

    private static final double JUMP_VELOCITY = 0.42;
    private static final double BHOP_MOMENTUM = 1.2;
    private static final double GROUND_FRICTION = 0.6;

    private final CustomBot bot;
    private final BotSettings settings;

    private List<BlockPos> currentPath;
    private int pathIndex;
    private int pathRecalculateCooldown;
    private BlockPos pathTarget;
    private int pathTicks;

    private double targetYaw;
    private double targetPitch;
    private float currentYaw;
    private float currentPitch;

    private boolean wasOnGround;

    private int wanderCooldown;
    private BlockPos wanderTarget;

    private int strafeTimer;
    private boolean strafeLeft;

    @Nullable private LivingEntity combatTarget;
    @Nullable private Entity threatEntity;

    @Nullable private BotPath activeBotPath;
    private int pathWaypointIndex;

    private int targetSearchCooldown;

    private int shieldState;
    private int shieldTimer;
    private int attackCooldownTimer;
    private boolean awaitingCritJump;

    private int rangedDrawTimer;
    private int rangedCooldownTimer;
    private int rangedStrafeTimer;
    private boolean rangedStrafeLeft;
    private int rangedState;

    public BotMovementController(@NotNull CustomBot bot, @NotNull BotSettings settings) {
        this.bot = bot;
        this.settings = settings;
        this.currentYaw = bot.getYRot();
        this.currentPitch = bot.getXRot();
        this.wasOnGround = bot.onGround();
    }

    public void setCombatTarget(@Nullable LivingEntity target) {
        this.combatTarget = target;
        if (target != null) {
            this.pathTarget = target.blockPosition();
            this.currentPath = null;
        }
    }

    public void setThreatEntity(@Nullable Entity entity) {
        this.threatEntity = entity;
    }

    @Nullable
    public LivingEntity getCombatTarget() {
        return combatTarget;
    }

    public void moveTo(@NotNull BlockPos target) {
        if (!target.equals(this.pathTarget)) {
            this.pathTarget = target;
            this.currentPath = null;
            this.pathIndex = 0;
        }
    }

    public void stop() {
        this.currentPath = null;
        this.pathTarget = null;
        this.pathIndex = 0;
        this.combatTarget = null;
        this.threatEntity = null;
        this.wanderTarget = null;
        this.activeBotPath = null;
        this.pathWaypointIndex = 0;
        this.shieldState = 0;
        this.shieldTimer = 0;
        this.rangedState = 0;
        this.rangedDrawTimer = 0;
        bot.lowerShield();
        if (bot.isUsingItem()) bot.stopUsingItem();
        bot.setDeltaMovement(Vec3.ZERO);
    }

    public boolean hasPath() {
        return currentPath != null && pathIndex < currentPath.size();
    }

    public void setCurrentPath(@NotNull BotPath path) {
        this.activeBotPath = path;
        this.pathWaypointIndex = 0;
        this.currentPath = null;
        this.pathTarget = null;
    }

    public void clearActivePath() {
        this.activeBotPath = null;
        this.pathWaypointIndex = 0;
    }

    @Nullable
    public BotPath getActiveBotPath() {
        return activeBotPath;
    }

    private void followBotPath(@NotNull Level level) {
        if (activeBotPath == null) return;
        List<org.bukkit.Location> wps = activeBotPath.getWaypoints();
        if (wps.isEmpty()) {
            activeBotPath = null;
            return;
        }

        if (pathWaypointIndex >= wps.size()) {
            if (activeBotPath.isLoop()) {
                pathWaypointIndex = 0;
            } else {
                activeBotPath = null;
                pathWaypointIndex = 0;
                applyFriction(bot.onGround());
                return;
            }
        }

        org.bukkit.Location targetWp = wps.get(pathWaypointIndex);
        BlockPos targetBlock = new BlockPos(targetWp.getBlockX(), targetWp.getBlockY(), targetWp.getBlockZ());
        Vec3 botPos = bot.position();

        double dx = targetWp.getX() - botPos.x;
        double dz = targetWp.getZ() - botPos.z;
        double distSq = dx * dx + dz * dz;

        if (distSq < 1.5 * 1.5) {
            pathWaypointIndex++;
            if (pathWaypointIndex >= wps.size()) {
                if (activeBotPath.isLoop()) {
                    pathWaypointIndex = 0;
                } else {
                    activeBotPath = null;
                    pathWaypointIndex = 0;
                    applyFriction(bot.onGround());
                    return;
                }
            }
            targetWp = wps.get(pathWaypointIndex);
            if (targetWp == null) return;
            targetBlock = new BlockPos(targetWp.getBlockX(), targetWp.getBlockY(), targetWp.getBlockZ());
            dx = targetWp.getX() - botPos.x;
            dz = targetWp.getZ() - botPos.z;
        }

        int targetY = targetBlock.getY();
        int botY = bot.blockPosition().getY();
        boolean onGround = bot.onGround();

        WalkType wt = activeBotPath.getWalkType();
        double speed;
        boolean useBhop;

        switch (wt) {
            case SPRINT -> {
                speed = 0.35;
                useBhop = false;
            }
            case BHOP -> {
                speed = 0.25;
                useBhop = true;
            }
            default -> {
                speed = settings.getMoveSpeed();
                useBhop = false;
            }
        }

        if (targetY > botY && onGround) {
            bot.setDeltaMovement(bot.getDeltaMovement().x, JUMP_VELOCITY, bot.getDeltaMovement().z);
        } else if (useBhop && onGround) {
            Vec3 vel = bot.getDeltaMovement();
            if (vel.x * vel.x + vel.z * vel.z > 0.001) {
                bot.setDeltaMovement(vel.x, JUMP_VELOCITY, vel.z);
                bot.setDeltaMovement(vel.x * BHOP_MOMENTUM, JUMP_VELOCITY, vel.z * BHOP_MOMENTUM);
            }
        }

        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 0.01) {
            double velX = (dx / len) * speed;
            double velZ = (dz / len) * speed;
            Vec3 currentVel = bot.getDeltaMovement();
            if (onGround) {
                bot.setDeltaMovement(velX, currentVel.y, velZ);
            } else {
                bot.setDeltaMovement(
                        currentVel.x + (velX - currentVel.x) * 0.3,
                        currentVel.y,
                        currentVel.z + (velZ - currentVel.z) * 0.3
                );
            }
        }
    }

    private void updatePathRotation() {
        if (activeBotPath == null) return;
        List<org.bukkit.Location> wps = activeBotPath.getWaypoints();
        if (wps.isEmpty() || pathWaypointIndex >= wps.size()) return;
        org.bukkit.Location target = wps.get(pathWaypointIndex);
        smoothRotateTowards(target.getX(), target.getY() + 0.5, target.getZ());
    }

    public boolean isRetreating() {
        return settings.isRetreat() && shouldRetreat();
    }

    public boolean isMoving() {
        Vec3 vel = bot.getDeltaMovement();
        return vel.x * vel.x + vel.z * vel.z > 0.001;
    }

    public void tick() {
        Level level = bot.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (pathRecalculateCooldown > 0) pathRecalculateCooldown--;
        if (wanderCooldown > 0) wanderCooldown--;
        if (attackCooldownTimer > 0) attackCooldownTimer--;
        if (targetSearchCooldown > 0) targetSearchCooldown--;

        boolean onGround = bot.onGround();

        handleTargetSelection(serverLevel);
        verifyCombatTarget();

        boolean maceActive = bot.getMaceComboController().isActive();

        if (!maceActive) {
            if (activeBotPath != null) {
                followBotPath(level);
            } else if (combatTarget != null) {
                handleCombatMovement();
            } else if (settings.isRetreat() && shouldRetreat()) {
                handleRetreatMovement();
            } else if (settings.isIdle() && pathTarget == null && !hasPath()) {
                handleWanderMovement(level);
            } else if (pathTarget != null) {
                followPath(level);
            }

            if (activeBotPath == null && hasPath()) {
                moveAlongPath(level);
            }
        }

        if (combatTarget != null && !maceActive) {
            updateRotation(level);
        } else if (activeBotPath != null && !maceActive) {
            updatePathRotation();
        } else if (hasPath() && !maceActive) {
            updateRotation(level);
        }

        if (combatTarget != null && settings.isCombat() && !maceActive) {
            handleCombatDecision();
        }

        handleShield();

        applyFriction(onGround);
        if (!maceActive) {
            handleCritJump(onGround);
        }

        wasOnGround = onGround;
    }

    private void handleCombatDecision() {
        if (combatTarget == null) return;
        double distSq = bot.distanceToSqr(combatTarget);
        double meleeRangeSq = settings.getMeleeRange() * settings.getMeleeRange();

        boolean useRanged = settings.isRanged()
                && bot.hasBowOrCrossbowInInventory()
                && distSq >= settings.getRangedMinRange() * settings.getRangedMinRange()
                && distSq <= settings.getRangedMaxRange() * settings.getRangedMaxRange();

        if (useRanged) {
            handleRangedCombat(distSq);
        } else if (distSq <= meleeRangeSq) {
            handleMeleeAttack();
        }
    }

    private void handleTargetSelection(ServerLevel serverLevel) {
        if (settings.isAutoTarget() && combatTarget == null && targetSearchCooldown <= 0) {
            LivingEntity found = CombatTargetSelector.findTarget(serverLevel, bot, settings);
            if (found != null) {
                setCombatTarget(found);
            }
            targetSearchCooldown = 20;
        }
    }

    private void verifyCombatTarget() {
        if (combatTarget != null && (!combatTarget.isAlive() || combatTarget.isRemoved())) {
            combatTarget = null;
        }
    }

    private boolean shouldRetreat() {
        if (combatTarget != null) return false;
        float health = bot.getHealth();
        float maxHealth = bot.getMaxHealth();
        return maxHealth > 0 && (health / maxHealth) < settings.getRetreatHealth();
    }

    private void handleCombatMovement() {
        if (combatTarget == null) return;

        BlockPos targetPos = combatTarget.blockPosition();
        BlockPos botPos = bot.blockPosition();
        double distSq = botPos.distSqr(targetPos);
        double meleeRangeSq = settings.getMeleeRange() * settings.getMeleeRange();
        boolean isClose = distSq < meleeRangeSq;

        boolean rangedMode = settings.isRanged()
                && bot.hasBowOrCrossbowInInventory()
                && distSq >= settings.getRangedMinRange() * settings.getRangedMinRange()
                && distSq <= settings.getRangedMaxRange() * settings.getRangedMaxRange();

        if (pathRecalculateCooldown <= 0 || currentPath == null) {
            BlockPos offset;
            if (rangedMode) {
                offset = calcRangedOffset(botPos, targetPos, distSq);
            } else if (settings.isCombatStrafe() && distSq < 64.0) {
                strafeTimer++;
                if (strafeTimer > settings.getCombatStrafeInterval()) {
                    strafeLeft = !strafeLeft;
                    strafeTimer = 0;
                }
                int dx = targetPos.getX() - botPos.getX();
                int dz = targetPos.getZ() - botPos.getZ();
                double angle = Math.atan2(dz, dx);
                angle += strafeLeft ? Math.PI / 2 : -Math.PI / 2;
                int strafeX = (int) Math.round(Math.cos(angle) * 3);
                int strafeZ = (int) Math.round(Math.sin(angle) * 3);
                offset = new BlockPos(targetPos.getX() + strafeX, targetPos.getY(), targetPos.getZ() + strafeZ);
            } else {
                offset = targetPos;
            }
            recalculatePath(offset);
            pathRecalculateCooldown = 10;
        }

        if (rangedMode && settings.isArrowPrediction()) {
            double dist = Math.sqrt(distSq);
            Vec3 predicted = ArrowPrediction.predictPosition(combatTarget, dist);
            setLookTarget(predicted.x, predicted.y, predicted.z);
        } else {
            Vec3 lookTarget = combatTarget.position().add(0, combatTarget.getEyeHeight() * 0.7, 0);
            setLookTarget(lookTarget.x, lookTarget.y, lookTarget.z);
        }
    }

    private BlockPos calcRangedOffset(BlockPos botPos, BlockPos targetPos, double distSq) {
        double dist = Math.sqrt(distSq);
        int dx = botPos.getX() - targetPos.getX();
        int dz = botPos.getZ() - targetPos.getZ();

        if (settings.isRangedRetreat() && dist < settings.getRangedMinRange()) {
            if (dx == 0 && dz == 0) { dx = 1; dz = 0; }
            double len = Math.sqrt(dx * dx + dz * dz);
            int rx = botPos.getX() + (int) Math.round((dx / len) * 5);
            int rz = botPos.getZ() + (int) Math.round((dz / len) * 5);
            return new BlockPos(rx, botPos.getY(), rz);
        }

        if (settings.isRangedStrafe()) {
            rangedStrafeTimer++;
            if (rangedStrafeTimer > 20 + bot.getRandom().nextInt(20)) {
                rangedStrafeLeft = !rangedStrafeLeft;
                rangedStrafeTimer = 0;
            }
            double angle = Math.atan2(dz, dx);
            angle += rangedStrafeLeft ? Math.PI / 2 : -Math.PI / 2;
            int sx = targetPos.getX() + (int) Math.round(Math.cos(angle) * 4);
            int sz = targetPos.getZ() + (int) Math.round(Math.sin(angle) * 4);
            return new BlockPos(sx, botPos.getY(), sz);
        }

        return targetPos;
    }

    private void handleRangedCombat(double distSq) {
        double dist = Math.sqrt(distSq);

        if (!bot.isHoldingRangedWeapon()) {
            bot.equipRangedWeapon();
        }

        if (rangedCooldownTimer > 0) {
            rangedCooldownTimer--;
            return;
        }

        switch (rangedState) {
            case 0 -> {
                if (!bot.isUsingItem()) {
                    if (bot.isCrossbowCharged()) {
                        bot.fireChargedCrossbow();
                        rangedCooldownTimer = settings.getAttackCooldown() + settings.getBowDrawTicks();
                    } else {
                        bot.startBowDraw();
                        rangedState = 1;
                        rangedDrawTimer = 0;
                    }
                }
            }
            case 1 -> {
                rangedDrawTimer++;
                if (bot.isCrossbowCharged()) {
                    bot.stopUsingItem();
                    bot.fireChargedCrossbow();
                    rangedState = 0;
                    rangedCooldownTimer = settings.getAttackCooldown() + settings.getBowDrawTicks();
                } else if (rangedDrawTimer >= settings.getBowDrawTicks()) {
                    bot.releaseBow();
                    rangedState = 2;
                    rangedDrawTimer = 0;
                }
            }
            case 2 -> {
                rangedCooldownTimer = settings.getAttackCooldown();
                rangedState = 0;
            }
        }
    }

    private void handleRetreatMovement() {
        if (threatEntity == null || !threatEntity.isAlive()) {
            threatEntity = null;
            return;
        }

        BlockPos botPos = bot.blockPosition();
        BlockPos threatPos = threatEntity.blockPosition();
        int dx = botPos.getX() - threatPos.getX();
        int dz = botPos.getZ() - threatPos.getZ();
        if (dx == 0 && dz == 0) { dx = 1; dz = 0; }
        double len = Math.sqrt(dx * dx + dz * dz);
        int retreatX = botPos.getX() + (int) Math.round((dx / len) * 10);
        int retreatZ = botPos.getZ() + (int) Math.round((dz / len) * 10);

        BlockPos retreatPos = new BlockPos(retreatX, botPos.getY(), retreatZ);

        if (pathRecalculateCooldown <= 0 || currentPath == null) {
            recalculatePath(retreatPos);
            pathRecalculateCooldown = 15;
        }
    }

    private void handleWanderMovement(@NotNull Level level) {
        if (wanderCooldown > 0) return;

        if (wanderTarget != null && bot.blockPosition().distSqr(wanderTarget) < 2.0) {
            wanderTarget = null;
            wanderCooldown = 20 + bot.getRandom().nextInt(40);
            currentPath = null;
            return;
        }

        if (pathRecalculateCooldown <= 0 || currentPath == null) {
            BlockPos botPos = bot.blockPosition();
            for (int attempt = 0; attempt < 10; attempt++) {
                int rx = botPos.getX() + (bot.getRandom().nextInt(settings.getIdleRadius() * 2 + 1) - settings.getIdleRadius());
                int rz = botPos.getZ() + (bot.getRandom().nextInt(settings.getIdleRadius() * 2 + 1) - settings.getIdleRadius());
                int ry = botPos.getY();
                wanderTarget = new BlockPos(rx, ry, rz);
                break;
            }
            if (wanderTarget != null) {
                recalculatePath(wanderTarget);
                pathRecalculateCooldown = 20;
            }
        }
    }

    private void followPath(@NotNull Level level) {
        if (currentPath == null || pathTarget == null) return;

        pathTicks++;
        if (pathRecalculateCooldown <= 0 && pathTicks > 20) {
            BlockPos botPos = bot.blockPosition();
            if (botPos.distSqr(pathTarget) > 4.0) {
                recalculatePath(pathTarget);
            }
            pathTicks = 0;
            pathRecalculateCooldown = 20;
        }

        if (pathIndex >= currentPath.size()) {
            currentPath = null;
        }
    }

    private void moveAlongPath(@NotNull Level level) {
        if (currentPath == null || pathIndex >= currentPath.size()) return;

        BlockPos targetNode = currentPath.get(pathIndex);
        Vec3 botPos = bot.position();

        double dx = targetNode.getX() + 0.5 - botPos.x;
        double dz = targetNode.getZ() + 0.5 - botPos.z;
        double distSqr = dx * dx + dz * dz;

        if (distSqr < 0.4) {
            pathIndex++;
            if (pathIndex >= currentPath.size()) {
                currentPath = null;
                return;
            }
            targetNode = currentPath.get(pathIndex);
            dx = targetNode.getX() + 0.5 - botPos.x;
            dz = targetNode.getZ() + 0.5 - botPos.z;
        }

        int targetY = targetNode.getY();
        int botY = bot.blockPosition().getY();
        boolean onGround = bot.onGround();

        if (targetY > botY && onGround) {
            bot.setDeltaMovement(bot.getDeltaMovement().x, JUMP_VELOCITY, bot.getDeltaMovement().z);
        } else if (settings.isBhop() && onGround && isMoving()) {
            bot.setDeltaMovement(bot.getDeltaMovement().x, JUMP_VELOCITY, bot.getDeltaMovement().z);
            Vec3 vel = bot.getDeltaMovement();
            bot.setDeltaMovement(vel.x * BHOP_MOMENTUM, vel.y, vel.z * BHOP_MOMENTUM);
        }

        double speed = settings.getMoveSpeed();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 0.01) {
            double velX = (dx / len) * speed;
            double velZ = (dz / len) * speed;
            Vec3 currentVel = bot.getDeltaMovement();
            if (onGround) {
                bot.setDeltaMovement(velX, currentVel.y, velZ);
            } else {
                bot.setDeltaMovement(
                        currentVel.x + (velX - currentVel.x) * 0.3,
                        currentVel.y,
                        currentVel.z + (velZ - currentVel.z) * 0.3
                );
            }
        }
    }

    private void handleMeleeAttack() {
        if (combatTarget == null) return;

        double distSq = bot.distanceToSqr(combatTarget);
        double range = settings.getMeleeRange();
        if (distSq > range * range) return;

        if (attackCooldownTimer > 0) return;

        if (settings.isCriticals() && bot.onGround()) {
            bot.setDeltaMovement(bot.getDeltaMovement().x, JUMP_VELOCITY, bot.getDeltaMovement().z);
            awaitingCritJump = true;
            bot.setAwaitingCritJump(true);
            return;
        }

        if (settings.isCriticals() && awaitingCritJump) {
            if (!bot.onGround() && bot.getDeltaMovement().y < 0) {
                executeAttack();
                awaitingCritJump = false;
                bot.setAwaitingCritJump(false);
            }
            return;
        }

        executeAttack();
    }

    private void executeAttack() {
        if (combatTarget == null) return;

        double missChance = settings.getMissChance();
        if (missChance > 0 && bot.getRandom().nextDouble() * 100 < missChance) {
            bot.swing(InteractionHand.MAIN_HAND);
            bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, bot.getSoundSource(), 1.0f, 1.0f);
            attackCooldownTimer = settings.getAttackCooldown();
            return;
        }

        double mistakeChance = settings.getMistakeChance();
        if (mistakeChance > 0 && bot.getRandom().nextDouble() * 100 < mistakeChance) {
            float yawOffset = (bot.getRandom().nextFloat() - 0.5f) * 90f;
            float pitchOffset = (bot.getRandom().nextFloat() - 0.5f) * 30f;
            bot.setYRot(bot.getYRot() + yawOffset);
            bot.setXRot(bot.getXRot() + pitchOffset);
        }

        boolean wasBlocking = combatTarget.isBlocking();
        boolean isHoldingAxe = bot.getMainHandItem().getItem() instanceof AxeItem;

        bot.performMeleeAttack(combatTarget);
        attackCooldownTimer = settings.getAttackCooldown();

        if (wasBlocking && isHoldingAxe && settings.isShieldBreak()) {
            double roll = bot.getRandom().nextDouble() * 100.0;
            if (roll < settings.getShieldBreakChance()) {
                if (combatTarget instanceof net.minecraft.world.entity.player.Player targetPlayer) {
                    targetPlayer.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(Items.SHIELD), 100);
                }
            }
        }
    }

    private void handleCritJump(boolean onGround) {
        if (awaitingCritJump && onGround) {
            awaitingCritJump = false;
            bot.setAwaitingCritJump(false);
        }
    }

    private void handleShield() {
        if (!settings.isAutoShield()) {
            if (shieldState != 0) {
                shieldState = 0;
                shieldTimer = 0;
                bot.lowerShield();
            }
            return;
        }

        if (!bot.getOffhandItem().is(Items.SHIELD) && bot.hasShieldInInventory()) {
            bot.equipShield();
        }

        boolean shouldRaise = shouldRaiseShield();

        switch (shieldState) {
            case 0 -> {
                if (shouldRaise) {
                    shieldState = 1;
                    shieldTimer = 0;
                }
            }
            case 1 -> {
                shieldTimer++;
                if (!shouldRaise) {
                    shieldState = 0;
                    shieldTimer = 0;
                    break;
                }
                if (shieldTimer >= settings.getShieldRaiseTicks()) {
                    bot.raiseShield();
                    shieldState = 2;
                    shieldTimer = 0;
                }
            }
            case 2 -> {
                shieldTimer++;
                if (!shouldRaise) {
                    bot.lowerShield();
                    shieldState = 3;
                    shieldTimer = 0;
                    break;
                }
                if (shieldTimer >= settings.getShieldHoldTicks()) {
                    bot.lowerShield();
                    shieldState = 3;
                    shieldTimer = 0;
                }
            }
            case 3 -> {
                shieldTimer++;
                if (shieldTimer >= 10) {
                    shieldState = 0;
                    shieldTimer = 0;
                }
            }
        }
    }

    private boolean shouldRaiseShield() {
        if (combatTarget == null) return false;

        if (combatTarget.isUsingItem()) {
            if (isTargetRanged()) {
                if (isTargetAimingAtBot()) {
                    return true;
                }
            }
        }

        if (settings.isShieldMace() && isTargetUsingMace()) {
            if (!combatTarget.onGround() && combatTarget.getDeltaMovement().y < 0
                    && combatTarget.position().y > bot.position().y + 2.0) {
                return true;
            }
        }

        return false;
    }

    private boolean isTargetRanged() {
        if (combatTarget == null) return false;
        var useItem = combatTarget.getUseItem();
        return useItem.is(Items.BOW) || useItem.is(Items.CROSSBOW);
    }

    private boolean isTargetAimingAtBot() {
        if (combatTarget == null) return false;
        Vec3 lookVec = combatTarget.getLookAngle();
        Vec3 toBot = bot.position().subtract(combatTarget.position()).normalize();
        double dot = lookVec.dot(toBot);
        return dot > 0.5;
    }

    private boolean isTargetUsingMace() {
        if (combatTarget == null) return false;
        return combatTarget.getMainHandItem().is(Items.MACE);
    }

    private void updateRotation(@NotNull Level level) {
        if (combatTarget != null) {
            Vec3 target = combatTarget.position().add(0, combatTarget.getEyeHeight() * 0.7, 0);
            smoothRotateTowards(target.x, target.y, target.z);
        } else if (hasPath() && pathIndex < currentPath.size()) {
            BlockPos node = currentPath.get(pathIndex);
            smoothRotateTowards(node.getX() + 0.5, node.getY() + 0.5, node.getZ() + 0.5);
        }
    }

    private void setLookTarget(double x, double y, double z) {
        Vec3 botPos = bot.position();
        Vec3 eyePos = botPos.add(0, bot.getEyeHeight(), 0);
        double dx = x - eyePos.x;
        double dy = y - eyePos.y;
        double dz = z - eyePos.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        this.targetYaw = Math.toDegrees(Math.atan2(-dx, -dz));
        this.targetPitch = -Math.toDegrees(Math.atan2(dy, Math.max(horizDist, 0.001)));
    }

    private void smoothRotateTowards(double x, double y, double z) {
        setLookTarget(x, y, z);
        float aimSpeed = (float) settings.getAimSpeed();

        currentYaw = lerpAngle(currentYaw, (float) targetYaw, aimSpeed);
        currentPitch = lerpAngle(currentPitch, (float) targetPitch, aimSpeed * 0.8f);

        bot.setYRot(currentYaw);
        bot.setXRot(currentPitch);

        if (bot.getBukkitEntity() != null) {
            bot.getBukkitEntity().setRotation(currentYaw, currentPitch);
        }
    }

    private void applyFriction(boolean onGround) {
        if (onGround && !isMoving()) {
            Vec3 vel = bot.getDeltaMovement();
            bot.setDeltaMovement(vel.x * GROUND_FRICTION, vel.y, vel.z * GROUND_FRICTION);
        }
    }

    private void recalculatePath(@NotNull BlockPos target) {
        Level level = bot.level();
        if (level == null) return;
        BlockPos start = bot.blockPosition();
        List<BlockPos> path = AStarPathfinder.findPath(level, start, target);
        if (path != null && path.size() > 1) {
            this.currentPath = path;
            this.pathIndex = 1;
            this.pathTarget = target;
            this.pathTicks = 0;
        } else {
            this.currentPath = null;
        }
    }

    private static float lerpAngle(float from, float to, float speed) {
        float diff = to - from;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;
        return from + diff * Math.min(speed, 1.0f);
    }

    @Nullable
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }

    @Nullable
    public BlockPos getPathTarget() {
        return pathTarget;
    }
}
