package xyz.fz.record.handler.client.normal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalClientHandler.class);

    public static EventLoopGroup NORMAL_CLIENT_GROUP = new NioEventLoopGroup();

    private Channel serverChannel;

    public NormalClientHandler(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        serverChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        serverChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        serverChannel.close();
        LOGGER.error("http client handler err: {}", cause.getMessage());
        cause.printStackTrace();
    }
}
