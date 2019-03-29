package xyz.fz.intercept;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import xyz.fz.record.intercept.RecordIntercept;

public class DefaultIntercept implements RecordIntercept {
    @Override
    public void interceptRequest(long proxyId, FullHttpRequest fullHttpRequest) {
        System.out.println("request ==> (" + proxyId + ") : " + fullHttpRequest.uri());
    }

    @Override
    public void interceptResponse(long proxyId, FullHttpResponse fullHttpResponse) {
        System.out.println("response ==> (" + proxyId + ") : " + fullHttpResponse.status());
    }
}
