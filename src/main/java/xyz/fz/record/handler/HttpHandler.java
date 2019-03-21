package xyz.fz.record.handler;

import io.netty.buffer.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import okhttp3.*;

import java.util.Map;

public class HttpHandler extends ChannelInboundHandlerAdapter {

    private CompositeByteBuf cumulation;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Attribute<Map<String, Object>> attr = ctx.channel().attr(RequestHolder.REQUEST_INFO);
        if (msg instanceof HttpRequest) {
            RequestHolder.hold((HttpRequest) msg, attr);
            ReferenceCountUtil.release(msg);
        } else {
            if (msg instanceof HttpContent) {
                cumulate(((HttpContent) msg).content(), ctx.alloc());
                if (msg instanceof LastHttpContent) {
                    Map<String, Object> requestInfo = attr.get();
                    doProxy(ctx, requestInfo, cumulation);
                    RequestHolder.clear(attr);
                    cumulationRelease();
                }
            }
        }
    }

    private void doProxy(ChannelHandlerContext ctx, Map<String, Object> requestInfo, CompositeByteBuf cumulation) {
        HttpHeaders httpHeaders = (HttpHeaders) requestInfo.get("headers");
        String uri = requestInfo.get("uri").toString();
        String method = requestInfo.get("method").toString();

        System.out.println("doProxy uri: " + uri);

        httpHeaders.remove("Proxy-Connection");
        httpHeaders.remove("Accept-Encoding");

        OkHttpClient client = new OkHttpClient();
        Request.Builder reqBuilder = new Request.Builder();
        reqBuilder.url(uri);
        RequestBody reqBody;
        if (httpHeaders.get("Content-Type") != null) {
            reqBody = RequestBody.create(
                    MediaType.get(httpHeaders.get("Content-Type")),
                    ByteBufUtil.getBytes(cumulation)
            );
        } else {
            reqBody = null;
        }
        reqBuilder.method(method, reqBody);
        for (Map.Entry<String, String> entry : httpHeaders.entries()) {
            reqBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        try (Response okRes = client.newCall(reqBuilder.build()).execute()) {
            ResponseBody okResBody = okRes.body();
            if (okResBody != null) {
                DefaultFullHttpResponse proxyRes;
                proxyRes = okRes.isSuccessful()
                        ? new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(okRes.code(), okRes.message()), Unpooled.copiedBuffer(okResBody.bytes()))
                        : new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(okRes.code(), okRes.message()));

                Headers okResHeaders = okRes.headers();
                for (String header : okResHeaders.names()) {
                    proxyRes.headers().add(header, okResHeaders.get(header));
                }

                ctx.writeAndFlush(proxyRes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cumulate(ByteBuf data, ByteBufAllocator allocator) {
        if (cumulation == null) {
            cumulation = allocator.compositeBuffer(16);
            cumulation.addComponent(true, data);
        } else {
            cumulation.addComponent(true, data);
        }
    }

    private void cumulationRelease() {
        if (cumulation != null) {
            cumulation.release();
            cumulation = null;
        }
    }
}
