package xyz.fz.record.handler.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ClientWorker {

    private ChannelFuture channelFuture;

    private volatile boolean isConnected;

    private final List<Object> msgList = new ArrayList<>();

    private static EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    public ClientWorker(Channel serverChannel, String host, int port, ChannelInitializer<SocketChannel> initializer) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(CLIENT_GROUP)
                .remoteAddress(new InetSocketAddress(host, port))
                .channel(NioSocketChannel.class)
                .handler(initializer);
        channelFuture = bootstrap.connect();
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                isConnected = true;
                synchronized (msgList) {
                    msgList.forEach(m -> future.channel().writeAndFlush(m));
                    msgList.clear();
                }
            } else {
                synchronized (msgList) {
                    msgList.forEach(ReferenceCountUtil::release);
                    msgList.clear();
                }
                future.channel().close();
                serverChannel.close();
            }
        });
    }

    public void sendMsg(Object msg) {
        if (isConnected) {
            channelFuture.channel().writeAndFlush(msg);
        } else {
            synchronized (msgList) {
                msgList.add(msg);
            }
        }
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }
}
