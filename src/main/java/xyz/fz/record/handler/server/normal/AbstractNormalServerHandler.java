package xyz.fz.record.handler.server.normal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.client.normal.NormalClientHandler;
import xyz.fz.record.handler.server.CommonServerHandler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNormalServerHandler extends ChannelInboundHandlerAdapter implements CommonServerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNormalServerHandler.class);

    private ChannelFuture clientChannelFuture = null;

    private volatile boolean isClientConnected;

    private final List<Object> msgList = new ArrayList<>();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (clientChannelFuture != null) {
            clientChannelFuture.channel().close();
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        transferMsg(clientChannelFuture, ctx, msg, msgList, isClientConnected);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        LOGGER.error("switch handler err: {}", cause.getMessage());
        cause.printStackTrace();
    }

    @Override
    public void clientConnected() {
        isClientConnected = true;
    }

    @Override
    public Bootstrap initClientBootstrap(ChannelHandlerContext ctx, String host, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NormalClientHandler.NORMAL_CLIENT_GROUP)
                .remoteAddress(new InetSocketAddress(host, port))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        otherClientHandler(ch);
                        ch.pipeline().addLast("normalClientHandler", new NormalClientHandler(ctx.channel()));
                    }
                });
        return bootstrap;
    }
}
