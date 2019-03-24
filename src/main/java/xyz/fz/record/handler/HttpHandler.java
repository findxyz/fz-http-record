package xyz.fz.record.handler;

import io.netty.buffer.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHandler.class);

    private CompositeByteBuf cumulateContent;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Attribute<Map<String, Object>> attr = ctx.channel().attr(RequestHolder.REQUEST_INFO);
        if (msg instanceof HttpRequest) {
            RequestHolder.hold((HttpRequest) msg, attr);
            ReferenceCountUtil.release(msg);
        } else {
            if (msg instanceof HttpContent) {
                cumulateContent(((HttpContent) msg).content(), ctx.alloc());
                if (msg instanceof LastHttpContent) {
                    Map<String, Object> requestInfo = attr.get();
                    doProxy(ctx, requestInfo, cumulateContent);
                    RequestHolder.clear(attr);
                    cumulateContentRelease();
                }
            }
        }
    }

    private void doProxy(ChannelHandlerContext ctx, Map<String, Object> requestInfo, CompositeByteBuf cumulation) {
        HttpHeaders httpHeaders = (HttpHeaders) requestInfo.get("headers");
        String uri = requestInfo.get("uri").toString();
        String method = requestInfo.get("method").toString();

        LOGGER.warn("proxy uri: {}", uri);

        httpHeaders.remove("Proxy-Connection");
        httpHeaders.remove("Accept-Encoding");

        Request.Builder reqBuilder = new Request.Builder();
        reqBuilder.url(uri);
        RequestBody reqBody;
        String contentType = httpHeaders.get("Content-Type");
        if ((contentType != null || method.equalsIgnoreCase("POST"))
                && !method.equalsIgnoreCase("GET")) {
            reqBody = RequestBody.create(
                    MediaType.get(contentType != null ? contentType : "application/x-www-form-unlencoded"),
                    ByteBufUtil.getBytes(cumulation)
            );
        } else {
            reqBody = null;
        }
        reqBuilder.method(method, reqBody);
        for (Map.Entry<String, String> entry : httpHeaders.entries()) {
            reqBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS).build();
        client.newCall(reqBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                ctx.close();
            }

            @Override
            public void onResponse(Call call, Response okRes) throws IOException {
                ResponseBody okResBody = okRes.body();
                if (okResBody != null) {
                    DefaultFullHttpResponse proxyRes;
                    proxyRes = okRes.isSuccessful() ?
                            new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    new HttpResponseStatus(okRes.code(), okRes.message()),
                                    Unpooled.copiedBuffer(okResBody.bytes()))
                            :
                            new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    new HttpResponseStatus(okRes.code(), okRes.message()));

                    Headers okResHeaders = okRes.headers();
                    for (String header : okResHeaders.names()) {
                        proxyRes.headers().add(header, okResHeaders.get(header));
                    }

                    ctx.writeAndFlush(proxyRes);
                }
                okRes.close();
            }
        });
    }

    private void cumulateContent(ByteBuf data, ByteBufAllocator allocator) {
        if (cumulateContent == null) {
            cumulateContent = allocator.compositeBuffer(16);
            cumulateContent.addComponent(true, data);
        } else {
            cumulateContent.addComponent(true, data);
        }
    }

    private void cumulateContentRelease() {
        if (cumulateContent != null) {
            cumulateContent.release();
            cumulateContent = null;
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        cumulateContentRelease();
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        cumulateContentRelease();
        ctx.close();
    }
}
