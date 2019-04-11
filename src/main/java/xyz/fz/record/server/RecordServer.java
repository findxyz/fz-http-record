package xyz.fz.record.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.SwitchHandler;
import xyz.fz.record.intercept.ProxyUtil;
import xyz.fz.record.intercept.RecordIntercept;
import xyz.fz.record.util.SnowFlake;
import xyz.fz.util.BaseUtil;
import xyz.fz.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

public class RecordServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordServer.class);

    private static ChannelFuture CHANNEL_FUTURE = null;

    private static volatile long startTime = SnowFlake.ofDefault().generateNextId();

    private static final int port = 23333;

    public static long getStartTime() {
        return startTime;
    }

    public static void startInterceptAll(List<RecordIntercept> interceptList) {
        ProxyUtil.setInterceptAll(true);
        ProxyUtil.setInterceptList(interceptList);
        ProxyUtil.setInterceptHost(new String[]{});
        start();
    }

    public static void startInterceptNone() {
        ProxyUtil.setInterceptAll(false);
        ProxyUtil.setInterceptList(new ArrayList<>());
        ProxyUtil.setInterceptHost(new String[]{});
        start();
    }

    public static void startInterceptHost(List<RecordIntercept> interceptList, String[] hosts) {
        ProxyUtil.setInterceptAll(false);
        ProxyUtil.setInterceptList(interceptList);
        ProxyUtil.setInterceptHost(hosts);
        start();
    }

    public synchronized static void stop() {
        if (CHANNEL_FUTURE != null) {
            CHANNEL_FUTURE.channel().close();
        }
    }

    private synchronized static void start() {
        startTime = SnowFlake.ofDefault().generateNextId();
        if (CHANNEL_FUTURE != null) {
            CHANNEL_FUTURE.channel().closeFuture().addListener((ChannelFutureListener) future -> start0());
            CHANNEL_FUTURE.channel().close();
        } else {
            start0();
        }
    }

    private static void start0() {
        ThreadUtil.execute(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast("httpServerCodec", new HttpServerCodec());
                                ch.pipeline().addLast("switchHandler", new SwitchHandler());
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                CHANNEL_FUTURE = serverBootstrap.bind("0.0.0.0", port).sync();
                LOGGER.info("http record server startup @ " + port);

                CHANNEL_FUTURE.channel().closeFuture().sync();
            } catch (Exception e) {
                LOGGER.error(BaseUtil.getExceptionStackTrace(e));
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
                LOGGER.info("http record server has been shutdown");
                CHANNEL_FUTURE = null;
            }
        });
    }
}
