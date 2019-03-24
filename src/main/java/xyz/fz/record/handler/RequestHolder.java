package xyz.fz.record.handler;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

class RequestHolder {

    static AttributeKey<Map<String, Object>> REQUEST_INFO = AttributeKey.valueOf("request.info");

    static void hold(HttpRequest httpRequest,
                     Attribute<Map<String, Object>> attr) {
        if (httpRequest.headers().size() > 0) {
            String host = httpRequest.headers().get(HttpHeaderNames.HOST).split(":")[0];
            Map<String, Object> requestInfo = new HashMap<>();
            requestInfo.put("headers", httpRequest.headers());
            requestInfo.put("method", httpRequest.method().name());
            if (httpRequest.uri().startsWith("http")) {
                requestInfo.put("uri", httpRequest.uri());
            } else {
                requestInfo.put("uri", "https://" + host + httpRequest.uri());
            }
            requestInfo.put("host", host);
            attr.set(requestInfo);
        }
    }

    static void clear(Attribute<Map<String, Object>> attr) {
        attr.set(null);
    }
}
