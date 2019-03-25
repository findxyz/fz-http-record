package xyz.fz.record.handler;

import io.netty.channel.socket.SocketChannel;

public class HttpServerHandler extends ServerHandler {
    @Override
    public String defaultPort() {
        return "80";
    }

    @Override
    public void otherClientHandler(SocketChannel ch) throws Exception {
    }
}
