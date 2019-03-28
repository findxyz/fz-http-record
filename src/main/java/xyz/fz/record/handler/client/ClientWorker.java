package xyz.fz.record.handler.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public interface ClientWorker {
    EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    void sendMsg(Object msg);

    ChannelFuture getChannelFuture();
}
