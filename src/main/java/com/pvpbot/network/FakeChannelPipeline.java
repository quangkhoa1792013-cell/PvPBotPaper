package com.pvpbot.network;

import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPipeline;

public class FakeChannelPipeline extends DefaultChannelPipeline {

    public FakeChannelPipeline(Channel channel) {
        super(channel);
    }
}
