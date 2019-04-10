package xyz.fz.intercept;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import xyz.fz.entity.Record;
import xyz.fz.record.intercept.RecordIntercept;
import xyz.fz.service.RecordService;
import xyz.fz.util.BaseUtil;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DefaultIntercept implements RecordIntercept {

    @Resource
    private RecordService recordService;

    @Override
    public void interceptRequest(long proxyId, FullHttpRequest fullHttpRequest) {
        Record record = new Record();
        record.setId(proxyId);
        record.setMethod(fullHttpRequest.method().name());
        record.setUrl(fullHttpRequest.uri());
        record.setRequest(getMessageJson(fullHttpRequest.headers(), fullHttpRequest.content()));
        recordService.save(record);
    }

    @Override
    public void interceptResponse(long proxyId, FullHttpResponse fullHttpResponse) {
        recordService.updateResponse(proxyId, getMessageJson(fullHttpResponse.headers(), fullHttpResponse.content()));
    }

    private String getMessageJson(HttpHeaders httpHeaders, ByteBuf content) {
        List<String> messageList = formatHeaders(httpHeaders);
        if (content.readableBytes() > 0) {
            if (content.readableBytes() <= 280 * 1024) {
                String contentType = httpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
                if (StringUtils.containsIgnoreCase(contentType, "gbk")) {
                    messageList.add("body: " + content.toString(Charset.forName("gbk")));
                } else {
                    messageList.add("body: " + content.toString(StandardCharsets.UTF_8));
                }
            } else {
                messageList.add("body: 响应数据大于300K");
            }
        }
        String messageJson = "";
        try {
            messageJson = BaseUtil.toJson(messageList);
        } catch (Exception ignore) {
        }
        return messageJson;
    }

    private List<String> formatHeaders(HttpHeaders httpHeaders) {
        List<String> headers = new ArrayList<>();
        for (Map.Entry<String, String> entry : httpHeaders.entries()) {
            headers.add(entry.getKey() + ": " + entry.getValue());
        }
        return headers;
    }
}
