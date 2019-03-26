package xyz.fz.record.handler.client.full;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.service.InterceptorService;

public class FullClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FullClientHandler.class);

    private Channel serverChannel;

    public FullClientHandler(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {

        InterceptorService.interceptResponse(msg);

        msg.retain();
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
        LOGGER.error("full client handler err: {}", cause.getMessage());
    }
}
