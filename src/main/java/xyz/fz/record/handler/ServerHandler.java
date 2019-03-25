package xyz.fz.record.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

    private ChannelFuture clientChannelFuture;

    private volatile boolean isClientConnected;

    private final List<FullHttpRequest> msgList = new ArrayList<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        HttpHeaders httpHeaders = msg.headers();
        if (httpHeaders.size() == 0) {
            LOGGER.error("/bad-request, {}", "io.netty.handler.codec.TooLongFrameException: An HTTP line is larger than 4096 bytes.");
            return;
        }
        // retain for writeAndFlush because of SimpleChannelInboundHandler.channelRead0 will auto release msg
        // and writeAndFlush will auto release msg too
        msg.retain();
        if (clientChannelFuture == null) {
            String[] hostPort = hostPort(httpHeaders);
            clientChannelFuture = initClientBootstrap(ctx, hostPort[0], hostPort[1]).connect();
            clientChannelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(msg);
                    synchronized (msgList) {
                        msgList.forEach(m -> future.channel().writeAndFlush(m));
                        msgList.clear();
                        isClientConnected = true;
                    }
                } else {
                    msgList.forEach(ReferenceCountUtil::release);
                    msgList.clear();
                    future.channel().close();
                    ctx.close();
                    LOGGER.error("client connected false: {}", future.cause().getMessage());
                }
            });
        } else {
            if (isClientConnected) {
                clientChannelFuture.channel().writeAndFlush(msg);
            } else {
                synchronized (msgList) {
                    msgList.add(msg);
                }
            }
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
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

    private String[] hostPort(HttpHeaders httpHeaders) {
        String[] hostPort = httpHeaders.get(HttpHeaderNames.HOST).split(":");
        String host = hostPort[0];
        String port = defaultPort();
        if (hostPort.length > 1) {
            port = hostPort[1];
        }
        return new String[]{host, port};
    }

    public abstract String defaultPort();

    private Bootstrap initClientBootstrap(ChannelHandlerContext ctx, String host, String port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ClientHandler.CLIENT_GROUP)
                .remoteAddress(new InetSocketAddress(host, Integer.parseInt(port)))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("httpClientCodec", new HttpClientCodec());
                        ch.pipeline().addLast("httpContentDecompressor", new HttpContentDecompressor());
                        ch.pipeline().addLast("httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024));
                        ch.pipeline().addLast("httpClientHandler", new ClientHandler(ctx.channel()));
                        otherClientHandler(ch);
                    }
                });
        return bootstrap;
    }

    public abstract void otherClientHandler(SocketChannel ch) throws Exception;
}
