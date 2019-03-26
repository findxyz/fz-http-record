package xyz.fz.record.handler.server.normal;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;

public class HttpNormalServerHandler extends AbstractNormalServerHandler {
    @Override
    public void otherClientHandler(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("httpClientCodec", new HttpClientCodec());
    }
}
