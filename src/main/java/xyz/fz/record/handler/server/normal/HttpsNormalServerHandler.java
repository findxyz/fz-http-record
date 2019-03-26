package xyz.fz.record.handler.server.normal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import xyz.fz.record.handler.client.ClientWorker;
import xyz.fz.record.handler.client.normal.NormalClientHandler;

public class HttpsNormalServerHandler extends NormalServerHandler {

    private ClientWorker clientWorker;

    public HttpsNormalServerHandler(Channel serverChannel, String host, int port) {
        clientWorker = new ClientWorker(serverChannel, host, port, new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                channel.pipeline().addLast("normalServerHandler", new NormalClientHandler(serverChannel));
            }
        });
    }

    @Override
    public ClientWorker getClientWorker() {
        return clientWorker;
    }
}
