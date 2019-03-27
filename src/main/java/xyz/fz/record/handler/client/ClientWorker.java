package xyz.fz.record.handler.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import okhttp3.*;
import xyz.fz.record.interceptor.FullInterceptor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientWorker {

    private Channel serverChannel;

    private Bootstrap bootstrap;

    private ChannelFuture channelFuture = null;

    private volatile boolean isConnected;

    private final List<Object> msgList = new ArrayList<>();

    private static EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    public ClientWorker(Channel serverChannel, String host, int port, ChannelInitializer<SocketChannel> initializer) {
        this.serverChannel = serverChannel;
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(CLIENT_GROUP)
                .remoteAddress(new InetSocketAddress(host, port))
                .channel(NioSocketChannel.class)
                .handler(initializer);
        this.bootstrap = bootstrap;
    }

    public void sendMsg(Object msg) {
        if (msg instanceof FullHttpRequest) {
            sendInterceptMsg((FullHttpRequest) msg);
        } else {
            sendDirectMsg(msg);
        }
    }

    private void sendInterceptMsg(FullHttpRequest fullHttpRequest) {
        CLIENT_GROUP.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request.Builder reqBuilder = new Request.Builder();
            reqBuilder.url(FullInterceptor.getUri(fullHttpRequest));
            if (fullHttpRequest.headers().get("Content-Type") != null) {
                RequestBody reqBody = RequestBody.create(
                        MediaType.get(fullHttpRequest.headers().get("Content-Type")),
                        ByteBufUtil.getBytes(fullHttpRequest.content())
                );
                reqBuilder.put(reqBody);
            }
            for (Map.Entry<String, String> entry : fullHttpRequest.headers().entries()) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            FullInterceptor.interceptRequest(fullHttpRequest);

            ReferenceCountUtil.release(fullHttpRequest);

            try (Response clientResponse = client.newCall(reqBuilder.build()).execute()) {
                if (clientResponse.isSuccessful()) {
                    ResponseBody clientResponseBody = clientResponse.body();
                    if (clientResponseBody != null) {
                        DefaultFullHttpResponse serverResponse = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1,
                                new HttpResponseStatus(clientResponse.code(), clientResponse.message()),
                                Unpooled.copiedBuffer(clientResponseBody.bytes())
                        );

                        Headers clientResponseHeaders = clientResponse.headers();
                        for (String header : clientResponseHeaders.names()) {
                            serverResponse.headers().add(header, clientResponseHeaders.get(header));
                        }

                        FullInterceptor.interceptResponse(serverResponse);

                        serverChannel.writeAndFlush(serverResponse);
                    }
                } else {
                    serverChannel.writeAndFlush(
                            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(clientResponse.code(), clientResponse.message()))
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendDirectMsg(Object msg) {
        // channelFuture 无需做同步处理
        // 是因为 ClientWorker 只会在 ServerHandler 的 channelRead 事件中被调用
        // 而 ServerHandler 的 ChannelHandlerContext 的 Channel 只会在其 EventLoop 中运行
        // 因为 EventLoop 是一个单线程
        // 所以就不存在竞争关系了

        // msgList 需要同步
        // 是因为 channelFuture 的 Channel 运行在 CLIENT_GROUP 所分配的线程中
        // 又因为 ServerHandler 的所在线程中也会用到 msgList
        // 故在使用前需要对 msgList 做同步处理
        // 以避免不安全的访问

        // isConnected 需要 volatile 关键字修饰
        // 以保证 ServerHandler 所在线程中读到的 isConnected 值为最新的值
        if (channelFuture == null) {
            channelFuture = bootstrap.connect();
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    isConnected = true;
                    channelFuture.channel().writeAndFlush(msg);
                    synchronized (msgList) {
                        msgList.forEach(m -> future.channel().writeAndFlush(m));
                        msgList.clear();
                    }
                } else {
                    synchronized (msgList) {
                        msgList.forEach(ReferenceCountUtil::release);
                        msgList.clear();
                    }
                    future.channel().close();
                    serverChannel.close();
                }
            });
        } else {
            if (isConnected) {
                channelFuture.channel().writeAndFlush(msg);
            } else {
                synchronized (msgList) {
                    msgList.add(msg);
                }
            }
        }
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }
}
