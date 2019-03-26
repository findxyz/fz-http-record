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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.server.full.HttpsFullServerHandler;
import xyz.fz.record.util.CertGenerateUtil;
import xyz.fz.record.util.CertUtil;

public class HandShakeHandler extends ChannelInboundHandlerAdapter {

    private static Logger LOGGER = LoggerFactory.getLogger(HandShakeHandler.class);

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            if (byteBuf.getByte(0) == 22) {
                HostHolder.HostInfo hostInfo = HostHolder.get(ctx);
                CertGenerateUtil.CertResult certResult = CertUtil.fetchCert(hostInfo.getHost());
                SslContext sslCtx = SslContextBuilder
                        .forServer(certResult.getPrivateKey(), certResult.getCertificate())
                        .build();
                ctx.pipeline().remove("handShakeHandler");
                ctx.pipeline().addLast("sslHandler", sslCtx.newHandler(ctx.alloc()));
                ctx.pipeline().addLast("httpServerCodec", new HttpServerCodec());
                ctx.pipeline().addLast("httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024));
                ctx.pipeline().addLast("httpContentCompressor", new HttpContentCompressor());
                ctx.pipeline().addLast("httpsFullServerHandler", new HttpsFullServerHandler(ctx.channel(), hostInfo.getHost(), hostInfo.getPort()));
                ctx.pipeline().fireChannelRead(msg);
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        LOGGER.error("hand shake handler err: {}", cause.getMessage());
    }
}
