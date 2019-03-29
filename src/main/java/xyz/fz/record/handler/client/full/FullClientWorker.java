package xyz.fz.record.handler.client.full;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;
import xyz.fz.record.handler.HostInfo;
import xyz.fz.record.handler.client.ClientWorker;
import xyz.fz.record.intercept.ProxyUtil;
import xyz.fz.record.util.SnowFlake;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FullClientWorker implements ClientWorker {

    private static SnowFlake snowFlake = new SnowFlake(1, 1);

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    static {
        client.dispatcher().setMaxRequests(128);
        client.dispatcher().setMaxRequestsPerHost(128);
    }

    private Channel serverChannel;

    private ChannelFuture channelFuture = null;

    public FullClientWorker(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

    @Override
    public void sendMsg(Object msg) {
        try {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(HostInfo.getUri(fullHttpRequest));

            String contentType = fullHttpRequest.headers().get("Content-Type");
            if (contentType != null) {
                RequestBody requestBody = RequestBody.create(MediaType.get(contentType), ByteBufUtil.getBytes(fullHttpRequest.content()));
                requestBuilder.post(requestBody);
            }

            for (Map.Entry<String, String> entry : fullHttpRequest.headers().entries()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            // todo 拦截 FullHttpRequest
            long proxyId = snowFlake.generateNextId();
            ProxyUtil.interceptRequest(proxyId, fullHttpRequest);

            client.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                @EverythingIsNonNull
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                @EverythingIsNonNull
                public void onResponse(Call call, Response clientResponse) throws IOException {
                    try (ResponseBody clientResponseBody = clientResponse.body()) {
                        DefaultFullHttpResponse serverResponse = null;
                        HttpResponseStatus clientResponseStatus = new HttpResponseStatus(clientResponse.code(), clientResponse.message());
                        if (clientResponse.isSuccessful()) {
                            if (clientResponseBody != null) {
                                serverResponse = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1,
                                        clientResponseStatus,
                                        Unpooled.copiedBuffer(clientResponseBody.bytes())
                                );
                            }
                        }
                        if (serverResponse == null) {
                            serverResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, clientResponseStatus);
                        }

                        Headers clientResponseHeaders = clientResponse.headers();
                        for (String header : clientResponseHeaders.names()) {
                            serverResponse.headers().add(header, clientResponseHeaders.get(header));
                        }

                        // todo 拦截 FullHttpResponse
                        ProxyUtil.interceptResponse(proxyId, serverResponse);

                        serverChannel.writeAndFlush(serverResponse);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }
}
