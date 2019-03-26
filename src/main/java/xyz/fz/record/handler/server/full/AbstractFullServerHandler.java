package xyz.fz.record.handler.server.full;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.client.full.FullClientHandler;
import xyz.fz.record.handler.server.CommonServerHandler;
import xyz.fz.record.service.InterceptorService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFullServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements CommonServerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFullServerHandler.class);

    private ChannelFuture clientChannelFuture = null;

    private volatile boolean isClientConnected;

    private final List<Object> msgList = new ArrayList<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        HttpHeaders httpHeaders = msg.headers();
        if (httpHeaders.size() == 0) {
            LOGGER.error("/bad-request, {}", "io.netty.handler.codec.TooLongFrameException: An HTTP line is larger than 4096 bytes.");
            return;
        }
        // retain for writeAndFlush because of SimpleChannelInboundHandler.channelRead0 will auto release msg
        // and writeAndFlush will auto release msg too
        InterceptorService.interceptRequest(msg);
        msg.retain();
        transferMsg(clientChannelFuture, ctx, msg, msgList, isClientConnected);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // AbstractChannel.AbstractUnsafe.close(final ChannelPromise promise)
        // AbstractChannel.AbstractUnsafe.close(final ChannelPromise promise, final Throwable cause, final ClosedChannelException closeCause, final boolean notify)
        // doClose0(promise); // close channel
        // fireChannelInactiveAndDeregister
        // pipeline.fireChannelInactive();
        // if (registered) {
        //     registered = false;
        //     pipeline.fireChannelUnregistered();
        // }

        // You should use channelInactive() which is triggered when a channel cannot perform communication anymore.
        // channelUnregistered() has different meaning although channelUnregistered() is always triggered after channelInactive().
        // https://stackoverflow.com/questions/23859182/netty-simplechannelinboundhandler-close-channel
        // by trustin
        if (clientChannelFuture != null) {
            clientChannelFuture.channel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        if (clientChannelFuture != null) {
            clientChannelFuture.channel().close();
        }
        LOGGER.error("http server handler err: {}", cause.getMessage());
        cause.printStackTrace();
    }

    @Override
    public void clientConnected() {
        isClientConnected = true;
    }

    @Override
    public Bootstrap initClientBootstrap(ChannelHandlerContext ctx, String host, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(FullClientHandler.FULL_CLIENT_GROUP)
                .remoteAddress(new InetSocketAddress(host, port))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("httpClientCodec", new HttpClientCodec());
                        ch.pipeline().addLast("httpContentDecompressor", new HttpContentDecompressor());
                        ch.pipeline().addLast("httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024));
                        ch.pipeline().addLast("fullClientHandler", new FullClientHandler(ctx.channel()));
                        otherClientHandler(ch);
                    }
                });
        return bootstrap;
    }
}
