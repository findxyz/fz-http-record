package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;

import java.util.Map;

public class SwitchHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

        if (msg instanceof HttpRequest) {
            if ("CONNECT".equalsIgnoreCase(((HttpRequest) msg).method().name())) {
                // 1.response connect ok
                // 2.add new handlers (sslHandler and httpsHandler) to channel pipeline
                // 3.make self-signed cert
                System.out.println("https todo: " + ((HttpRequest) msg).uri());
            } else {
                Attribute<Map<String, Object>> attr = ctx.channel().attr(RequestHolder.REQUEST_INFO);
                RequestHolder.hold((HttpRequest) msg, attr);
                ctx.pipeline().remove(SwitchHandler.class);
                ctx.pipeline().addLast(new HttpHandler());
            }
        }

        ReferenceCountUtil.release(msg);
    }
}
