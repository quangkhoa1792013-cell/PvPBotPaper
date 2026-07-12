package com.pvpbot.network;

import java.net.InetSocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public class FakeConnection extends Connection {

    public FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        FakeChannel ch = new FakeChannel(InetSocketAddress.createUnresolved("127.0.0.1", 0));
        this.channel = ch;
        this.address = ch.remoteAddress();
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public void send(Packet<?> packet) {
    }
}
