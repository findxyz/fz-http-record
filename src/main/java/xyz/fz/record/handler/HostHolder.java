package xyz.fz.record.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import xyz.fz.record.exception.GreatFireWallException;

import java.util.HashSet;
import java.util.Set;

public class HostHolder {

    private static AttributeKey<HostInfo> HOST = AttributeKey.valueOf("host");

    private static Set<String> GFW_WORDS = new HashSet<>();

    static {
        GFW_WORDS.add("google");
        GFW_WORDS.add("appspot");
    }

    static HostInfo hold(ChannelHandlerContext ctx, HttpRequest httpRequest, int defaultPort) {
        if (httpRequest.headers().size() > 0) {
            String[] hostPort = httpRequest.headers().get(HttpHeaderNames.HOST).split(":");
            String host = hostPort[0];
            int port = defaultPort;
            if (hostPort.length > 1) {
                port = Integer.parseInt(hostPort[1]);
            }
            if (!host.contains(".")) {
                throw new RuntimeException("unknown host");
            }
            for (String gfwWord : GFW_WORDS) {
                if (host.contains(gfwWord)) {
                    throw new GreatFireWallException("Oh, no! It's great firewall!");
                }
            }
            HostInfo hostInfo = new HostInfo(host, port);
            ctx.channel().attr(HOST).set(hostInfo);
            return hostInfo;
        } else {
            throw new RuntimeException("bad request");
        }
    }

    public static HostInfo get(ChannelHandlerContext ctx) {
        return ctx.channel().attr(HOST).get();
    }

    public static class HostInfo {
        private String host;

        private int port;

        HostInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
