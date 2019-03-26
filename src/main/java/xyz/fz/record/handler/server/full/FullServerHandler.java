package xyz.fz.record.handler.server.full;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.server.InitServer;
import xyz.fz.record.service.InterceptorService;

public abstract class FullServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements InitServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FullServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        HttpHeaders httpHeaders = msg.headers();
        if (httpHeaders.size() == 0) {
            LOGGER.error("/bad-request, {}", "TooLongFrameException: An HTTP line is larger than 4096 bytes.");
            return;
        }

        InterceptorService.interceptRequest(msg);

        msg.retain();
        getClientWorker().sendMsg(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // AbstractChannel.AbstractUnsafe.close(final ChannelPromise promise)
        // AbstractChannel.AbstractUnsafe.close(final ChannelPromise promise, final Throwable cause, final ClosedChannelException closeCause, final boolean notify)
        // doClose0(promise); --> channel will close here
        // fireChannelInactiveAndDeregister(...)
        // ...
        // pipeline.fireChannelInactive();
        // if (registered) {
        //     registered = false;
        //     pipeline.fireChannelUnregistered();
        // }

        // You should use channelInactive() which is triggered when a channel cannot perform communication anymore.
        // channelUnregistered() has different meaning although channelUnregistered() is always triggered after channelInactive().
        //
        // https://stackoverflow.com/questions/23859182/netty-simplechannelinboundhandler-close-channel
        // by trustin
        getClientWorker().getChannelFuture().channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        getClientWorker().getChannelFuture().channel().close();
        LOGGER.error("full server handler err: {}", cause.getMessage());
    }
}
