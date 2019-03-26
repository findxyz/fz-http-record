package xyz.fz.record.handler.server.full;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class HttpsFullServerHandler extends AbstractFullServerHandler {
    @Override
    public void otherClientHandler(SocketChannel ch) throws Exception {
        ch.pipeline().addFirst("sslHandler",
                SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build().newHandler(ch.alloc()));
    }
}
