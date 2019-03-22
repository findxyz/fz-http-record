package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.Attribute;
import xyz.fz.record.util.CertGenerateUtil;
import xyz.fz.record.util.CertUtil;

import java.util.Map;

public class HandShakeHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

        Attribute<Map<String, Object>> attr = ctx.channel().attr(RequestHolder.REQUEST_INFO);
        CertGenerateUtil.CertResult certResult = CertUtil.fetchCert(attr.get().get("host").toString());
        SslContext sslCtx = SslContextBuilder
                .forServer(certResult.getPrivateKey(), certResult.getCertificate())
                .build();
        ctx.pipeline().addFirst(new HttpServerCodec());
        ctx.pipeline().addFirst(sslCtx.newHandler(ctx.alloc()));
        ctx.pipeline().remove(HandShakeHandler.class);
        ctx.pipeline().addLast(new HttpHandler());
        ctx.pipeline().fireChannelRead(msg);

    }
}
