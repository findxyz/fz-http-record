package xyz.fz.record.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import xyz.fz.record.util.CertGenerateUtil;
import xyz.fz.record.util.CertUtil;

import java.util.Map;

public class HandShakeHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            if (byteBuf.getByte(0) == 22) {
                Attribute<Map<String, Object>> attr = ctx.channel().attr(RequestHolder.REQUEST_INFO);
                String host = attr.get().get("host").toString();
                CertGenerateUtil.CertResult certResult = CertUtil.fetchCert(host);
                SslContext sslCtx = SslContextBuilder
                        .forServer(certResult.getPrivateKey(), certResult.getCertificate())
                        .build();
                ctx.pipeline().addFirst(new HttpServerCodec());
                ctx.pipeline().addFirst(sslCtx.newHandler(ctx.alloc()));
                ctx.pipeline().remove(HandShakeHandler.class);
                ctx.pipeline().addLast(new HttpHandler());
                ctx.pipeline().fireChannelRead(msg);
            }
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
        cause.printStackTrace();
        ctx.close();
    }
}
