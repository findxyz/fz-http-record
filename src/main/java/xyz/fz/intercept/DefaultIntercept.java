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
        if (fullHttpRequest.uri().length() <= 999) {
            Record record = new Record();
            record.setId(proxyId);
            record.setHost(fullHttpRequest.headers().get(HttpHeaderNames.HOST));
            record.setMethod(fullHttpRequest.method().name());
            record.setUrl(fullHttpRequest.uri());
            record.setRequest(getHttp(fullHttpRequest.headers(), fullHttpRequest.content()));
            recordService.save(record);
        }
    }

    @Override
    public void interceptResponse(long proxyId, FullHttpResponse fullHttpResponse) {
        recordService.updateResponse(proxyId, getHttp(fullHttpResponse.headers(), fullHttpResponse.content()));
    }

    private String getHttp(HttpHeaders httpHeaders, ByteBuf content) {
        List<String> httpList = formatHeaders(httpHeaders);
        httpList.add(formatBody(httpHeaders, content));
        return BaseUtil.toJson(httpList);
    }

    private List<String> formatHeaders(HttpHeaders httpHeaders) {
        List<String> headers = new ArrayList<>();
        for (Map.Entry<String, String> entry : httpHeaders.entries()) {
            headers.add(entry.getKey() + ": " + entry.getValue());
        }
        return headers;
    }

    private String formatBody(HttpHeaders httpHeaders, ByteBuf content) {
        String result = "";
        if (content.readableBytes() > 0) {
            if (content.readableBytes() > 50 * 1024) {
                result = "body: 响应数据大于50K";
            } else {
                String contentType = httpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
                if (StringUtils.containsIgnoreCase(contentType, "image")) {
                    result = "body: 图片内容";
                } else {
                    if (StringUtils.containsIgnoreCase(contentType, "gbk")) {
                        result = "body: " + content.toString(Charset.forName("gbk"));
                    } else {
                        result = "body: " + content.toString(StandardCharsets.UTF_8);
                    }
                }
            }
        }
        return result;
    }
}
