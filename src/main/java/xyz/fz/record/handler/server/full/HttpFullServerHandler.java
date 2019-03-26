package xyz.fz.record.handler.server.full;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import xyz.fz.record.handler.client.ClientWorker;
import xyz.fz.record.handler.client.full.FullClientHandler;

public class HttpFullServerHandler extends FullServerHandler {

    private ClientWorker clientWorker;

    public HttpFullServerHandler(Channel serverChannel, String host, int port) {
        clientWorker = new ClientWorker(serverChannel, host, port, new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
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
