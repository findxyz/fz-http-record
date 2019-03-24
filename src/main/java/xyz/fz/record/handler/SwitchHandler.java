package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;

import java.util.Map;

public class SwitchHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

        if (msg instanceof HttpRequest) {
            Attribute<Map<String, Object>> attr = ctx.channel().attr(RequestHolder.REQUEST_INFO);
            RequestHolder.hold((HttpRequest) msg, attr);
            if ("CONNECT".equalsIgnoreCase(((HttpRequest) msg).method().name())) {
                DefaultFullHttpResponse connectedResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        new HttpResponseStatus(200, "Connection established")
                );
                ctx.pipeline().writeAndFlush(connectedResponse);
                ctx.pipeline().addLast(new HandShakeHandler());
                ctx.pipeline().remove(HttpServerCodec.class);
            } else {
                ctx.pipeline().addLast(new HttpHandler());
            }
            ctx.pipeline().remove(SwitchHandler.class);
        }

        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
