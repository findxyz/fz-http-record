package xyz.fz.record.handler.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import xyz.fz.record.handler.HostHolder;

import java.util.List;

public interface CommonServerHandler {

    default void transferMsg(ChannelFuture clientChannelFuture,
                             ChannelHandlerContext ctx,
                             Object msg,
                             List<Object> msgList,
                             boolean isClientConnected) {
        if (clientChannelFuture == null) {
            HostHolder.HostInfo hostInfo = HostHolder.get(ctx);
            clientChannelFuture = initClientBootstrap(ctx, hostInfo.getHost(), hostInfo.getPort()).connect();
            clientChannelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(msg);
                    synchronized (msgList) {
                        msgList.forEach(m -> future.channel().writeAndFlush(m));
                        msgList.clear();
                        clientConnected();
                    }
                } else {
                    msgList.forEach(ReferenceCountUtil::release);
                    msgList.clear();
                    future.channel().close();
                    ctx.close();
                }
            });
        } else {
            if (isClientConnected) {
                clientChannelFuture.channel().writeAndFlush(msg);
            } else {
                synchronized (msgList) {
                    msgList.add(msg);
                }
            }
        }
    }

    void clientConnected();

    Bootstrap initClientBootstrap(ChannelHandlerContext ctx, String host, int port);

    void otherClientHandler(SocketChannel ch) throws Exception;
}
