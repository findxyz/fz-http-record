package xyz.fz.record.handler.server.full;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.client.ClientWorker;
import xyz.fz.record.handler.client.full.FullClientWorker;
import xyz.fz.record.handler.server.InitializingServer;

public class FullServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements InitializingServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FullServerHandler.class);

    private ClientWorker clientWorker;

    public FullServerHandler(Channel serverChannel) {
        clientWorker = new FullClientWorker(serverChannel);
    }

    @Override
    public ClientWorker getClientWorker() {
        return clientWorker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        HttpHeaders httpHeaders = msg.headers();
        if (httpHeaders.size() == 0) {
            LOGGER.error("/bad-request, {}", "TooLongFrameException: An HTTP line is larger than 4096 bytes.");
            return;
        }

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
        if (getClientWorker().getChannelFuture() != null) {
            getClientWorker().getChannelFuture().channel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        if (getClientWorker().getChannelFuture() != null) {
            getClientWorker().getChannelFuture().channel().close();
        }
        LOGGER.error("full server handler err: {}", cause.getMessage());
    }
}
