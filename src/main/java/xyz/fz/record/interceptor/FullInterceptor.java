package xyz.fz.record.interceptor;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.util.HashSet;
import java.util.Set;

public class FullInterceptor {

    private static final Set<String> HOST_SET = new HashSet<>();

    static {
        HOST_SET.add("www.v2ex.com");
        HOST_SET.add("www.chuapp.com");
    }

    public static boolean interceptCheck(String host) {
        return HOST_SET.contains(host);
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

    public static void interceptRequest(FullHttpRequest fullHttpRequest) {
        System.out.println("request ==> uri: " + fullHttpRequest.uri() + ", headers: " + fullHttpRequest.headers());
    }

    public static void interceptResponse(FullHttpResponse fullHttpResponse) {
        System.out.println("response ==> headers: " + fullHttpResponse.headers());
    }
}
