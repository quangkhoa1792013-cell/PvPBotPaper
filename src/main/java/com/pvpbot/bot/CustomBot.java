package com.pvpbot.bot;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.pvpbot.combat.MaceComboController;
import com.pvpbot.combat.SurvivalController;
import com.pvpbot.faction.Faction;
import com.pvpbot.faction.FactionManager;
import com.pvpbot.kit.Kit;
import com.pvpbot.kit.KitManager;
import com.pvpbot.navigation.ArrowPrediction;
import com.pvpbot.navigation.BotMovementController;
import com.pvpbot.network.FakeConnection;
import com.pvpbot.network.FakeServerGamePacketListenerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CustomBot extends ServerPlayer {

    private static final String STEVE_TEXTURE = Base64.getEncoder().encodeToString(
            ("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" +
             "8dc4035488dc7cd80b48b7b36b593bad87d3b985cda259ed3cb4b62cd3cb4647\"}}}").getBytes()
    );

    private static final Item[] SWORDS = {
            Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
            Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.WOODEN_SWORD
    };

    private final UUID ownerUUID;
    private final String botName;
    private final FakeConnection fakeConnection;
    private final FakeServerGamePacketListenerImpl fakeListener;
    private final MinecraftServer serverInstance;
    private final BotSettings botSettings;
    private final BotMovementController movementController;
    private final MaceComboController maceComboController;
    private final SurvivalController survivalController;

    private int attackCooldownTicks;
    private boolean awaitingCritJump;
    private int weaponCheckCooldown;
    private boolean deathNotified;

    public CustomBot(
            @NotNull MinecraftServer server,
            @NotNull ServerLevel level,
            @NotNull GameProfile profile,
            @NotNull ClientInformation clientInfo,
            @NotNull UUID ownerUUID,
            @NotNull String botName
    ) {
        super(server, level, profile, clientInfo);
        this.ownerUUID = ownerUUID;
        this.botName = botName;
        this.serverInstance = server;
        this.botSettings = new BotSettings();
        this.fakeConnection = new FakeConnection();
        this.movementController = new BotMovementController(this, botSettings);
        this.maceComboController = new MaceComboController(this, botSettings);
        this.survivalController = new SurvivalController(this, botSettings);

        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        this.fakeListener = new FakeServerGamePacketListenerImpl(
                server, fakeConnection, this, cookie
        );
        this.connection = fakeListener;
    }

    @NotNull
    public static CustomBot spawn(
            @NotNull Location location,
            @Nullable String name,
            @NotNull UUID ownerUUID
    ) {
        return spawn(location, name, ownerUUID, false);
    }

    @NotNull
    public static CustomBot spawn(
            @NotNull Location location,
            @Nullable String name,
            @NotNull UUID ownerUUID,
            boolean profileLagFix
    ) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();

        String botName = (name != null && !name.isEmpty()) ? name : generateRandomName();
        UUID botUUID = UUID.randomUUID();
        GameProfile profile = new GameProfile(botUUID, botName);

        if (profileLagFix) {
            profile.properties().put("textures", new Property("textures", STEVE_TEXTURE, ""));
        }

        CustomBot bot = new CustomBot(server, level, profile, ClientInformation.createDefault(), ownerUUID, botName);

        bot.setPos(location.getX(), location.getY(), location.getZ());
        bot.setYRot(location.getYaw());
        bot.setXRot(location.getPitch());

        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        server.getPlayerList().placeNewPlayer(bot.fakeConnection, bot, cookie);

        bot.connection = bot.fakeListener;

        return bot;
    }

    public void broadcastSpawnPackets() {
        ClientboundPlayerInfoUpdatePacket infoPacket = new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, this
        );

        Vec3 pos = new Vec3(this.getX(), this.getY(), this.getZ());
        ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(
                this.getId(),
                this.getUUID(),
                pos.x(),
                pos.y(),
                pos.z(),
                this.getYRot(),
                this.getXRot(),
                this.getType(),
                0,
                Vec3.ZERO,
                0.0
        );

        for (ServerPlayer onlinePlayer : serverInstance.getPlayerList().getPlayers()) {
            if (onlinePlayer.equals(this)) continue;
            onlinePlayer.connection.send(infoPacket);
            onlinePlayer.connection.send(spawnPacket);
        }
    }

    public void broadcastRemovePackets() {
        ClientboundPlayerInfoRemovePacket removeInfoPacket =
                new ClientboundPlayerInfoRemovePacket(List.of(this.getUUID()));
        ClientboundRemoveEntitiesPacket removeEntityPacket =
                new ClientboundRemoveEntitiesPacket(this.getId());

        for (ServerPlayer onlinePlayer : serverInstance.getPlayerList().getPlayers()) {
            if (onlinePlayer.equals(this)) continue;
            onlinePlayer.connection.send(removeInfoPacket);
            onlinePlayer.connection.send(removeEntityPacket);
        }
    }

    @Override
    public void tick() {
        try {
            super.tick();
            if (attackCooldownTicks > 0) attackCooldownTicks--;
            if (weaponCheckCooldown > 0) weaponCheckCooldown--;
            if (isUsingItem()) {
                useItemRemaining--;
            }
            movementController.tick();
            maceComboController.tick(movementController.getCombatTarget());
            survivalController.tick(
                    movementController.getCombatTarget(),
                    movementController.isRetreating(),
                    maceComboController.isActive()
            );
            if (!deathNotified && !isAlive()) {
                deathNotified = true;
                com.pvpbot.stats.StatsDatabase.getInstance().recordBotDeath();
                org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("PvPBot");
                if (plugin != null) {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!isAlive()) {
                            if (botSettings.isBotLeaveOnDeath()) {
                                removeFromWorld();
                            } else {
                                setHealth(getMaxHealth());
                                getBukkitEntity().setFireTicks(0);
                                deathNotified = false;
                            }
                        }
                    }, 20L);
                }
            }
            onBotTick();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void aiStep() {
    }

    protected void onBotTick() {
    }

    public void removeFromWorld() {
        movementController.stop();
        broadcastRemovePackets();
        serverInstance.getPlayerList().remove(this);
        stopRiding();
        remove(Entity.RemovalReason.KILLED);
        deathNotified = true;
    }

    public void setCombatTarget(@Nullable LivingEntity target) {
        movementController.setCombatTarget(target);
    }

    public void setThreatEntity(@Nullable Entity entity) {
        movementController.setThreatEntity(entity);
    }

    public void moveTo(@NotNull BlockPos target) {
        movementController.moveTo(target);
    }

    public void stopMovement() {
        movementController.stop();
    }

    // --- Melee Combat ---

    public boolean canAttack() {
        return attackCooldownTicks <= 0;
    }

    public void resetAttackCooldown() {
        attackCooldownTicks = botSettings.getAttackCooldown();
    }

    public boolean isAwaitingCritJump() {
        return awaitingCritJump;
    }

    public void setAwaitingCritJump(boolean awaiting) {
        this.awaitingCritJump = awaiting;
    }

    public void performMeleeAttack(@NotNull LivingEntity target) {
        equipBestWeapon();
        attack(target);
    }

    public void equipBestWeapon() {
        if (weaponCheckCooldown > 0) return;
        weaponCheckCooldown = 10;

        boolean forceAxe = targetIsBlocking();
        boolean preferSword = botSettings.isPreferSword() && !forceAxe;

        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            ItemStack stack = getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            int score = scoreWeapon(stack, preferSword);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot >= 0) {
            int hotbarSlot = bestSlot;
            if (bestSlot >= 9) {
                int currentSelected = getInventory().getSelectedSlot();
                ItemStack currentItem = getInventory().getItem(currentSelected);
                getInventory().setItem(bestSlot, currentItem);
                getInventory().setItem(currentSelected, getInventory().getItem(bestSlot));
            }
            getInventory().setSelectedSlot(hotbarSlot);
        }
    }

    private int scoreWeapon(ItemStack stack, boolean preferSword) {
        Item item = stack.getItem();
        boolean isSword = isSword(item);
        boolean isAxe = item instanceof AxeItem;

        if (!isSword && !isAxe) return -1;

        int tier = getTier(item);
        int enchantScore = getEnchantBonus(stack);
        int typeBonus = preferSword ? (isSword ? 3 : 1) : (isAxe ? 3 : 1);

        return tier * 10 + enchantScore + typeBonus;
    }

    private int getTier(Item item) {
        if (item == Items.NETHERITE_SWORD || item == Items.NETHERITE_AXE) return 5;
        if (item == Items.DIAMOND_SWORD || item == Items.DIAMOND_AXE) return 4;
        if (item == Items.IRON_SWORD || item == Items.IRON_AXE) return 3;
        if (item == Items.STONE_SWORD || item == Items.STONE_AXE) return 2;
        if (item == Items.GOLDEN_SWORD || item == Items.GOLDEN_AXE) return 1;
        if (item == Items.WOODEN_SWORD || item == Items.WOODEN_AXE) return 1;
        if (item == Items.COPPER_SWORD || item == Items.COPPER_AXE) return 2;
        return 0;
    }

    private int getEnchantBonus(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return 0;
        int bonus = 0;
        for (var entry : enchants.entrySet()) {
            bonus += entry.getIntValue();
        }
        return bonus;
    }

    private boolean isSword(Item item) {
        for (Item sword : SWORDS) {
            if (item == sword) return true;
        }
        return false;
    }

    private boolean targetIsBlocking() {
        LivingEntity target = movementController.getCombatTarget();
        return target != null && target.isBlocking();
    }

    // --- Shield ---

    public void equipShield() {
        ItemStack offhand = getOffhandItem();
        if (offhand.is(Items.SHIELD)) return;

        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            ItemStack stack = getInventory().getItem(i);
            if (stack.is(Items.SHIELD)) {
                getInventory().setItem(i, offhand);
                getInventory().setItem(40, stack);
                break;
            }
        }
    }

    public boolean hasShieldInInventory() {
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            if (getInventory().getItem(i).is(Items.SHIELD)) return true;
        }
        return false;
    }

    public void raiseShield() {
        if (!isUsingItem()) {
            startUsingItem(InteractionHand.OFF_HAND);
        }
    }

    public void lowerShield() {
        if (isUsingItem()) {
            stopUsingItem();
        }
    }

    // --- Ranged Weapons ---

    public void startBowDraw() {
        if (!isUsingItem()) {
            startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    public void releaseBow() {
        if (isUsingItem()) {
            releaseUsingItem();
        }
    }

    public boolean hasBowOrCrossbowInInventory() {
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            ItemStack stack = getInventory().getItem(i);
            if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) return true;
        }
        return false;
    }

    public void equipRangedWeapon() {
        int bowSlot = -1;
        int crossbowSlot = -1;
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            ItemStack stack = getInventory().getItem(i);
            if (stack.is(Items.BOW) && bowSlot < 0) bowSlot = i;
            if (stack.is(Items.CROSSBOW) && crossbowSlot < 0) crossbowSlot = i;
        }
        int slot = bowSlot >= 0 ? bowSlot : crossbowSlot;
        if (slot < 0) return;

        int currentSelected = getInventory().getSelectedSlot();
        int targetSlot = slot;
        if (slot >= 9) {
            ItemStack currentItem = getInventory().getItem(currentSelected);
            getInventory().setItem(slot, currentItem);
            getInventory().setItem(currentSelected, getInventory().getItem(slot));
            targetSlot = currentSelected;
        }
        getInventory().setSelectedSlot(targetSlot);
    }

    public boolean isHoldingRangedWeapon() {
        ItemStack main = getMainHandItem();
        return main.is(Items.BOW) || main.is(Items.CROSSBOW);
    }

    public boolean isCrossbowCharged() {
        ItemStack stack = getMainHandItem();
        return stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
    }

    public void fireChargedCrossbow() {
        ItemStack crossbow = getMainHandItem();
        if (crossbow.is(Items.CROSSBOW) && CrossbowItem.isCharged(crossbow)) {
            crossbow.use(level(), this, InteractionHand.MAIN_HAND);
        }
    }

    // --- Wind Charge ---

    public void useWindCharge() {
        int slot = findItemSlot(Items.WIND_CHARGE);
        if (slot < 0) return;

        int currentSelected = getInventory().getSelectedSlot();
        equipFromSlot(slot);

        ItemStack windCharge = getMainHandItem();
        if (windCharge.is(Items.WIND_CHARGE)) {
            windCharge.use(level(), this, InteractionHand.MAIN_HAND);
        }

        if (slot >= 9) {
            ItemStack currentItem = getInventory().getItem(currentSelected);
            getInventory().setItem(slot, currentItem);
            getInventory().setItem(currentSelected, getInventory().getItem(slot));
        }
        getInventory().setSelectedSlot(currentSelected);
    }

    // --- Inventory Helpers ---

    public boolean hasItem(@NotNull Item item) {
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            if (getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }

    public int findItemSlot(@NotNull Item item) {
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            if (getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    public void equipFromSlot(int slot) {
        if (slot < 0) return;
        int currentSelected = getInventory().getSelectedSlot();
        if (slot >= 9) {
            ItemStack currentItem = getInventory().getItem(currentSelected);
            getInventory().setItem(slot, currentItem);
            getInventory().setItem(currentSelected, getInventory().getItem(slot));
        }
        getInventory().setSelectedSlot(slot >= 9 ? currentSelected : slot);
    }

    // --- Damage ---

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && !attacker.equals(this)) {
            FactionManager fm = getFactionManager();
            if (fm != null && !fm.canAttack(attacker.getUUID(), this.getUUID())) {
                return false;
            }
        }
        boolean result = super.hurtServer(level, source, amount);
        if (result && botSettings.isRevenge() && attacker instanceof LivingEntity living) {
            movementController.setCombatTarget(living);
        }
        return result;
    }

    @Nullable
    private FactionManager getFactionManager() {
        org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin("PvPBot");
        if (p instanceof com.pvpbot.PvPBotPlugin pp) {
            return pp.getFactionManager();
        }
        return null;
    }

    // --- Getters ---

    @NotNull
    public String getBotName() {
        return botName;
    }

    @NotNull
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @NotNull
    public BotSettings getSettings() {
        return botSettings;
    }

    @NotNull
    public BotMovementController getMovementController() {
        return movementController;
    }

    @NotNull
    public MaceComboController getMaceComboController() {
        return maceComboController;
    }

    @NotNull
    public SurvivalController getSurvivalController() {
        return survivalController;
    }

    public void finishUsingItem() {
        completeUsingItem();
    }

    public void clearInventory() {
        var inv = getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    @NotNull
    private static String generateRandomName() {
        String[] prefixes = {"Fighter", "Bot", "Dummy", "Target", "Sparring"};
        String prefix = prefixes[(int) (Math.random() * prefixes.length)];
        int suffix = 1000 + (int) (Math.random() * 9000);
        return prefix + suffix;
    }
}
