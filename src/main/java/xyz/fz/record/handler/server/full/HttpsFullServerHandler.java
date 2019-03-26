package xyz.fz.record.handler.server.full;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import xyz.fz.record.handler.client.ClientWorker;
import xyz.fz.record.handler.client.full.FullClientHandler;

public class HttpsFullServerHandler extends FullServerHandler {

    private ClientWorker clientWorker;

    public HttpsFullServerHandler(Channel serverChannel, String host, int port) {
        clientWorker = new ClientWorker(serverChannel, host, port, new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addFirst("sslHandler",
                        SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build().newHandler(ch.alloc()));
                ch.pipeline().addLast("httpClientCodec", new HttpClientCodec());
                ch.pipeline().addLast("httpContentDecompressor", new HttpContentDecompressor());
                ch.pipeline().addLast("httpObjectAggregator", new HttpObjectAggregator(8 * 1024 * 1024));
                ch.pipeline().addLast("fullClientHandler", new FullClientHandler(serverChannel));
            }
        });
    }

    @Override
    public ClientWorker getClientWorker() {
        return clientWorker;
    }
}
