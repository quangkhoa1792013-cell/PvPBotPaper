package com.pvpbot.network;

import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class FakeChannel extends AbstractChannel {

    private static final NioEventLoopGroup EVENT_LOOP_GROUP = new NioEventLoopGroup(1, new DefaultThreadFactory("fake-bot-netty", true));
    private static final ChannelMetadata METADATA = new ChannelMetadata(true);
    private static final InetSocketAddress DUMMY_ADDRESS = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

    private final ChannelConfig config;
    private final FakeChannelPipeline pipeline;
    private final InetSocketAddress remoteAddress;
    private boolean active;

    public FakeChannel(InetSocketAddress remoteAddress) {
        super(null);
        this.config = new DefaultChannelConfig(this);
        this.pipeline = new FakeChannelPipeline(this);
        this.remoteAddress = remoteAddress != null ? remoteAddress : DUMMY_ADDRESS;
        this.active = true;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
                active = true;
                safeSetSuccess(promise);
            }
        };
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    protected SocketAddress localAddress0() {
        return DUMMY_ADDRESS;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return remoteAddress;
    }

    @Override
    protected void doBind(SocketAddress localAddress) {
    }

    @Override
    protected void doDisconnect() {
        active = false;
    }

    @Override
    protected void doClose() {
        active = false;
    }

    @Override
    protected void doBeginRead() {
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer buffer) {
        for (;;) {
            Object msg = buffer.current();
            if (msg == null) {
                break;
            }
            buffer.remove();
        }
    }

    @Override
    public ChannelConfig config() {
        config.setAutoRead(true);
        return config;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public EventLoop eventLoop() {
        return EVENT_LOOP_GROUP.next();
    }
}
