package xyz.fz.record.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;
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
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer("Connection established\r\n\r\n", Charset.forName("utf-8"))
                );
                ctx.pipeline().remove(HttpServerCodec.class);
                ctx.pipeline().addLast(new HandShakeHandler());
                ctx.writeAndFlush(connectedResponse);
            } else {
                ctx.pipeline().addLast(new HttpHandler());
            }
            ctx.pipeline().remove(SwitchHandler.class);
        }

        ReferenceCountUtil.release(msg);
    }
}
