package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.exception.GreatFireWallException;
import xyz.fz.record.handler.server.full.HttpFullServerHandler;
import xyz.fz.record.handler.server.normal.HttpNormalServerHandler;
import xyz.fz.record.handler.server.normal.HttpsNormalServerHandler;
import xyz.fz.record.service.InterceptorService;

public class SwitchHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchHandler.class);

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            if ("CONNECT".equalsIgnoreCase(((HttpRequest) msg).method().name())) {
                initHttpsHandler(ctx, msg);
            } else {
                initHttpHandler(ctx, msg);
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private void initHttpsHandler(final ChannelHandlerContext ctx, final Object msg) {
        HostHolder.HostInfo hostInfo = HostHolder.hold(ctx, (HttpRequest) msg, 443);
        ctx.pipeline().remove("switchHandler");
        DefaultFullHttpResponse connectedResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                new HttpResponseStatus(200, "Connection established")
        );
        ctx.pipeline().writeAndFlush(connectedResponse);
        ctx.pipeline().remove("httpServerCodec");
        if (InterceptorService.intercept(hostInfo.getHost())) {
            ctx.pipeline().addLast("handShakeHandler", new HandShakeHandler());
        } else {
            ctx.pipeline().addLast("httpsNormalServerHandler", new HttpsNormalServerHandler());
        }
        ReferenceCountUtil.release(msg);
    }

    private void initHttpHandler(final ChannelHandlerContext ctx, final Object msg) {
        HostHolder.HostInfo hostInfo = HostHolder.hold(ctx, (HttpRequest) msg, 80);
        ctx.pipeline().remove("switchHandler");
        if (InterceptorService.intercept(hostInfo.getHost())) {
            ctx.pipeline().addAfter("httpServerCodec", "httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024));
            ctx.pipeline().addAfter("httpObjectAggregator", "httpContentCompressor", new HttpContentCompressor());
            ctx.pipeline().addAfter("httpContentCompressor", "httpFullServerHandler", new HttpFullServerHandler());
        } else {
            ctx.pipeline().addLast("httpNormalServerHandler", new HttpNormalServerHandler());
        }
        ctx.pipeline().fireChannelRead(msg);
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
