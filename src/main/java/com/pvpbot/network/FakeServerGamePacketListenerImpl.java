package com.pvpbot.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl {

    public FakeServerGamePacketListenerImpl(
            MinecraftServer server,
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie
    ) {
        super(server, connection, player, cookie);
    }

    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundRespawnPacket respawn) {
            this.player.hasChangedDimension();
        } else if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            if (motion.getId() == this.player.getId() && this.player.hurtMarked) {
                this.player.hurtMarked = true;
                this.player.lerpMotion(motion.getMovement());
            }
        }
    }

    @Override
    public void tick() {
        try {
            super.tick();
        } catch (Exception ignored) {
        }
    }
}
