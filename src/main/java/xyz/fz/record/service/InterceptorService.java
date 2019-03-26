package xyz.fz.record.service;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.HashSet;
import java.util.Set;

public class InterceptorService {

    private static final Set<String> INTERCEPT_HOSTS = new HashSet<>();

    static {
        INTERCEPT_HOSTS.add("baidu.com");
    }

    public static boolean intercept(String host) {
        for (String interceptHost : INTERCEPT_HOSTS) {
            if (host.contains(interceptHost)) {
                return true;
            }
        }
        return false;
    }

    public static void interceptRequest(FullHttpRequest fullHttpRequest) {
        System.out.println(fullHttpRequest);
    }

    public static void interceptResponse(FullHttpResponse fullHttpResponse) {
        System.out.println(fullHttpResponse);
    }
}
