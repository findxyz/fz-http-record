package xyz.fz.record.handler;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;

public class HostInfo {

    private String host;

    private int port;

    private HostInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }

    static HostInfo of(HttpRequest httpRequest, boolean isSsl) {
        if (httpRequest.headers().size() > 0) {
            String hostHeader = httpRequest.headers().get(HttpHeaderNames.HOST);
            String[] hostPort;
            if (hostHeader != null) {
                hostPort = hostHeader.split(":");
            } else {
                hostPort = httpRequest.uri().split(":");
            }
            String host = hostPort[0];
            int port = isSsl ? 443 : 80;
            if (hostPort.length > 1) {
                port = Integer.parseInt(hostPort[1]);
            }
            return new HostInfo(host, port);
        } else {
            throw new RuntimeException("bad request");
        }
    }

    public static String getUri(FullHttpRequest fullHttpRequest) {
        String uri = fullHttpRequest.uri();
        if (uri.startsWith("http")) {
            return uri;
        } else {
            String hostPort = fullHttpRequest.headers().get(HttpHeaderNames.HOST);
            return "https://" + hostPort + fullHttpRequest.uri();
        }
    }
}
