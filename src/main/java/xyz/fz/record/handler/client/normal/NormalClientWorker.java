package xyz.fz.record.handler.client.normal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.util.ReferenceCountUtil;
import xyz.fz.record.handler.client.ClientWorker;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NormalClientWorker implements ClientWorker {
    private Channel serverChannel;

    private Bootstrap bootstrap;

    private ChannelFuture channelFuture = null;

    private volatile boolean isConnected;

    private final List<Object> msgList = new ArrayList<>();

    public static ChannelInitializer<SocketChannel> ofHttps(Channel serverChannel) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("normalClientHandler", new NormalClientHandler(serverChannel));
            }
        };
    }

    public static ChannelInitializer<SocketChannel> ofHttp(Channel serverChannel) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("httpClientCodec", new HttpClientCodec());
                ch.pipeline().addLast("normalClientHandler", new NormalClientHandler(serverChannel));
            }
        };
    }

    public NormalClientWorker(Channel serverChannel, String host, int port, ChannelInitializer<SocketChannel> initializer) {
        this.serverChannel = serverChannel;
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(CLIENT_GROUP)
                .remoteAddress(new InetSocketAddress(host, port))
                .channel(NioSocketChannel.class)
                .handler(initializer);
        this.bootstrap = bootstrap;
    }

    @Override
    public void sendMsg(Object msg) {
        // channelFuture 无需做同步处理
        // 是因为 ClientWorker 只会在 ServerHandler 的 channelRead 事件中被调用
        // 而 ServerHandler 的 ChannelHandlerContext 的 Channel 只会在其 EventLoop 中运行
        // 因为 EventLoop 是一个单线程
        // 所以就不存在竞争关系了

        // msgList 需要同步
        // 是因为 channelFuture 的 Channel 运行在 CLIENT_GROUP 所分配的线程中
        // 又因为 ServerHandler 的所在线程中也会用到 msgList
        // 故在使用前需要对 msgList 做同步处理
        // 以避免不安全的访问

        // isConnected 需要 volatile 关键字修饰
        // 以保证 ServerHandler 所在线程中读到的 isConnected 值始终为最新的值
        if (channelFuture == null) {
            channelFuture = bootstrap.connect();
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    isConnected = true;
                    channelFuture.channel().writeAndFlush(msg);
                    synchronized (msgList) {
                        msgList.forEach(m -> future.channel().writeAndFlush(m));
                        msgList.clear();
                    }
                } else {
                    future.channel().close();
                    serverChannel.close();
                    ReferenceCountUtil.release(msg);
                    synchronized (msgList) {
                        msgList.forEach(ReferenceCountUtil::release);
                        msgList.clear();
                    }
                }
            });
        } else {
            if (isConnected) {
                channelFuture.channel().writeAndFlush(msg);
            } else {
                synchronized (msgList) {
                    msgList.add(msg);
                }
            }
        }
    }

    @Override
    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }
}
