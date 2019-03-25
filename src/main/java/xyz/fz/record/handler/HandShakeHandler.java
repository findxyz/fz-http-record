package xyz.fz.record.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;
import xyz.fz.record.util.CertGenerateUtil;
import xyz.fz.record.util.CertUtil;

public class HandShakeHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            if (byteBuf.getByte(0) == 22) {
                String host = HostHolder.get(ctx);
                CertGenerateUtil.CertResult certResult = CertUtil.fetchCert(host);
                SslContext sslCtx = SslContextBuilder
                        .forServer(certResult.getPrivateKey(), certResult.getCertificate())
                        .build();
                ctx.pipeline().remove("handShakeHandler");
                ctx.pipeline().addLast("sslHandler", sslCtx.newHandler(ctx.alloc()));
                ctx.pipeline().addLast("httpServerCodec", new HttpServerCodec());
                ctx.pipeline().addLast("httpObjectAggregator", new HttpObjectAggregator(64 * 1024));
                ctx.pipeline().addLast("httpContentCompressor", new HttpContentCompressor());
                ctx.pipeline().addLast("httpsServerHandler", new HttpsServerHandler());
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
