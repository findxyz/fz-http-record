package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import xyz.fz.record.exception.GreatFireWallException;

class HostHolder {

    private static AttributeKey<String> HOST = AttributeKey.valueOf("host");

    static void hold(HttpRequest httpRequest,
                     ChannelHandlerContext ctx) {
        if (httpRequest.headers().size() > 0) {
            String host = httpRequest.headers().get(HttpHeaderNames.HOST).split(":")[0];
            if (!host.contains(".")) {
                throw new RuntimeException("unknown host");
            }
            if (host.contains("google")) {
                throw new GreatFireWallException();
            }
            ctx.channel().attr(HOST).set(host);
        } else {
            throw new RuntimeException("bad request");
        }
    }

    static String get(ChannelHandlerContext ctx) {
        return ctx.channel().attr(HOST).get();
    }
}
