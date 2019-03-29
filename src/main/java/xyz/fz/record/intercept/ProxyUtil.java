package xyz.fz.record.intercept;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.*;

public class ProxyUtil {

    private static final Set<String> HOST_SET = new HashSet<>();

    private static volatile boolean INTERCEPT_ALL = false;

    private static final List<RecordIntercept> INTERCEPT_LIST = new ArrayList<>();

    public static void setInterceptHost(String[] hosts) {
        HOST_SET.clear();
        Collections.addAll(HOST_SET, hosts);
    }

    public static void setInterceptAll(boolean interceptAll) {
        INTERCEPT_ALL = interceptAll;
    }

    public static void setInterceptList(List<RecordIntercept> interceptList) {
        INTERCEPT_LIST.clear();
        INTERCEPT_LIST.addAll(interceptList);
    }

    public static boolean interceptCheck(String host) {
        return INTERCEPT_ALL || HOST_SET.contains(host);
    }

    public static void interceptRequest(long proxyId, FullHttpRequest fullHttpRequest) {
        for (RecordIntercept intercept : INTERCEPT_LIST) {
            intercept.interceptRequest(proxyId, fullHttpRequest);
        }
    }

    public static void interceptResponse(long proxyId, FullHttpResponse fullHttpResponse) {
        for (RecordIntercept intercept : INTERCEPT_LIST) {
            intercept.interceptResponse(proxyId, fullHttpResponse);
        }
    }
}
