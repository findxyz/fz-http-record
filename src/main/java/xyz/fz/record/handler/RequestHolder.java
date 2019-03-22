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
        Map<String, Object> requestInfo = attr.get();
        if (requestInfo == null) {
            requestInfo = new HashMap<>();
            requestInfo.put("headers", httpRequest.headers());
            requestInfo.put("method", httpRequest.method().name());
            requestInfo.put("uri", httpRequest.uri());
            requestInfo.put("host", httpRequest.headers().get(HttpHeaderNames.HOST).toString().split(":")[0]);
            attr.set(requestInfo);
        }
    }

    static void clear(Attribute<Map<String, Object>> attr) {
        attr.set(null);
    }
}
