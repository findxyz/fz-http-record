package xyz.fz.record.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    static EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    private Channel serverChannel;

    ClientHandler(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        msg.retain();
        serverChannel.writeAndFlush(msg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
        serverChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        serverChannel.close();
        LOGGER.error("http client handler err: {}", cause.getMessage());
    }
}
