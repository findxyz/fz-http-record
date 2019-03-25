package xyz.fz.record.handler;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class HttpsServerHandler extends ServerHandler {
    @Override
    public String defaultPort() {
        return "443";
    }

    @Override
    public void otherClientHandler(SocketChannel ch) throws Exception {
        ch.pipeline().addFirst("sslHandler",
                SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build().newHandler(ch.alloc()));
    }
}
