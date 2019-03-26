package xyz.fz.record.handler.server.normal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import xyz.fz.record.handler.client.ClientWorker;
import xyz.fz.record.handler.client.normal.NormalClientHandler;

public class HttpNormalServerHandler extends NormalServerHandler {

    private ClientWorker clientWorker;

    public HttpNormalServerHandler(Channel serverChannel, String host, int port) {
        clientWorker = new ClientWorker(serverChannel, host, port, new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                channel.pipeline().addLast("httpClientCodec", new HttpClientCodec());
                channel.pipeline().addLast("normalClientHandler", new NormalClientHandler(serverChannel));
            }
        });
    }

    @Override
    public ClientWorker getClientWorker() {
        return clientWorker;
    }
}
