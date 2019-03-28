package xyz.fz.record.interceptor;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.HashSet;
import java.util.Set;

public class ProxyInterceptor {

    private static final Set<String> HOST_SET = new HashSet<>();

    static {
        HOST_SET.add("cn.bing.com");
        HOST_SET.add("www.v2ex.com");
        HOST_SET.add("www.baidu.com");
        HOST_SET.add("www.chuapp.com");
    }

    public static boolean interceptCheck(String host) {
        return HOST_SET.contains(host);
    }

    public static void interceptRequest(long proxyId, FullHttpRequest fullHttpRequest) {
        System.out.println("request ==> (" + proxyId + ") uri: " + fullHttpRequest.uri() + ", headers: " + fullHttpRequest.headers());
    }

    public static void interceptResponse(long proxyId, FullHttpResponse fullHttpResponse) {
        System.out.println("response ==> (" + proxyId + ") headers: " + fullHttpResponse.headers());
    }
}
