package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.server.full.FullServerHandler;
import xyz.fz.record.handler.server.normal.NormalHttpServerHandler;
import xyz.fz.record.handler.server.normal.NormalHttpsServerHandler;
import xyz.fz.record.intercept.ProxyUtil;

public class SwitchHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchHandler.class);

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            boolean isSsl = "CONNECT".equalsIgnoreCase(((HttpRequest) msg).method().name());
            HostInfo hostInfo = HostInfo.of((HttpRequest) msg, isSsl);
            // done 检查是否拦截此 host 下的请求
            boolean isIntercept = ProxyUtil.interceptCheck(hostInfo.getHost());
            String switchCase = (isSsl ? "https" : "http") + "@" + (isIntercept ? "intercept" : "");

            ctx.pipeline().remove("switchHandler");
            switch (switchCase) {
                case "https@intercept":
                    switchToHttpsIntercept(ctx, msg, hostInfo.getHost());
                    break;
                case "https@":
                    switchToHttps(ctx, msg, hostInfo.getHost(), hostInfo.getPort());
                    break;
                case "http@intercept":
                    switchToHttpIntercept(ctx, msg);
                    break;
                case "http@":
                    switchToHttp(ctx, msg, hostInfo.getHost(), hostInfo.getPort());
                    break;
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private void switchToHttpsIntercept(ChannelHandlerContext ctx, Object msg, String host) {
        ctx.pipeline().writeAndFlush(new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                new HttpResponseStatus(200, "Connection established")
        ));
        ctx.pipeline().remove("httpServerCodec");
        ctx.pipeline().addLast("handShakeHandler", new HandShakeHandler(host));
        ReferenceCountUtil.release(msg);
    }

    private void switchToHttps(ChannelHandlerContext ctx, Object msg, String host, int port) {
        ctx.pipeline().writeAndFlush(new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                new HttpResponseStatus(200, "Connection established")
        ));
        ctx.pipeline().remove("httpServerCodec");
        ctx.pipeline().addLast("normalHttpsServerHandler", new NormalHttpsServerHandler(ctx.channel(), host, port));
        ReferenceCountUtil.release(msg);
    }

    private void switchToHttpIntercept(ChannelHandlerContext ctx, final Object msg) {
        ctx.pipeline().addAfter("httpServerCodec", "httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024));
        ctx.pipeline().addAfter("httpObjectAggregator", "httpContentCompressor", new HttpContentCompressor());
        ctx.pipeline().addAfter("httpContentCompressor", "fullServerHandler", new FullServerHandler(ctx.channel()));
        ctx.pipeline().fireChannelRead(msg);
    }

    private void switchToHttp(final ChannelHandlerContext ctx, final Object msg, String host, int port) {
        ctx.pipeline().addAfter("httpServerCodec", "normalHttpServerHandler", new NormalHttpServerHandler(ctx.channel(), host, port));
        ctx.pipeline().fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        LOGGER.error("switch handler err: {}", cause.getMessage());
        cause.printStackTrace();
    }
}
