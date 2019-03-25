package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.exception.GreatFireWallException;

public class SwitchHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchHandler.class);

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

        if (msg instanceof HttpRequest) {
            if ("CONNECT".equalsIgnoreCase(((HttpRequest) msg).method().name())) {
                HostHolder.hold((HttpRequest) msg, ctx);
                DefaultFullHttpResponse connectedResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        new HttpResponseStatus(200, "Connection established")
                );
                ctx.pipeline().writeAndFlush(connectedResponse);
                ctx.pipeline().remove("httpServerCodec");
                ctx.pipeline().addLast("handShakeHandler", new HandShakeHandler());
                ReferenceCountUtil.release(msg);
            } else {
                ctx.pipeline().addAfter("httpServerCodec", "httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024));
                ctx.pipeline().addAfter("httpObjectAggregator", "httpContentCompressor", new HttpContentCompressor());
                ctx.pipeline().addAfter("httpContentCompressor", "httpServerHandler", new HttpServerHandler());
                ctx.pipeline().fireChannelRead(msg);
            }
            ctx.pipeline().remove("switchHandler");
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        if (!(cause instanceof GreatFireWallException)) {
            LOGGER.error("switch handler err: {}", cause.getMessage());
            cause.printStackTrace();
        }
    }
}
