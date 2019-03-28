package xyz.fz.record.handler.server.normal;

import io.netty.channel.Channel;
import xyz.fz.record.handler.client.ClientWorker;
import xyz.fz.record.handler.client.normal.NormalClientWorker;

public class NormalHttpServerHandler extends NormalServerHandler {

    private ClientWorker clientWorker;

    public NormalHttpServerHandler(Channel serverChannel, String host, int port) {
        clientWorker = new NormalClientWorker(serverChannel, host, port, NormalClientWorker.ofHttp(serverChannel));
    }

    @Override
    public ClientWorker getClientWorker() {
        return clientWorker;
    }
}
