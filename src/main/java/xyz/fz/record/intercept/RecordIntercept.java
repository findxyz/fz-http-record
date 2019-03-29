package xyz.fz.record.intercept;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface RecordIntercept {
    void interceptRequest(long proxyId, FullHttpRequest fullHttpRequest);

    void interceptResponse(long proxyId, FullHttpResponse fullHttpResponse);
}
