package xyz.fz.record.handler.server.normal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.server.InitializingServer;

public abstract class NormalServerHandler extends ChannelInboundHandlerAdapter implements InitializingServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalServerHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (getClientWorker().getChannelFuture() != null) {
            getClientWorker().getChannelFuture().channel().close();
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        getClientWorker().sendMsg(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        if (getClientWorker().getChannelFuture() != null) {
            getClientWorker().getChannelFuture().channel().close();
        }
        LOGGER.error("normal server handler err: {}", cause.getMessage());
    }
}
