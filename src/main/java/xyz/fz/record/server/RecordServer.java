package xyz.fz.record.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.fz.record.handler.SwitchHandler;
import xyz.fz.record.intercept.ProxyUtil;
import xyz.fz.record.intercept.RecordIntercept;

import java.util.ArrayList;
import java.util.List;

public class RecordServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordServer.class);

    private static ChannelFuture CHANNEL_FUTURE = null;

    private static volatile boolean RUNNING = false;

    public static void startInterceptAll(List<RecordIntercept> interceptList) {
        ProxyUtil.setInterceptAll(true);
        ProxyUtil.setInterceptList(interceptList);
        ProxyUtil.setInterceptHost(new String[]{});
        while (!RUNNING && CHANNEL_FUTURE == null) {
            start();
        }
    }

    public static void startInterceptNone() {
        ProxyUtil.setInterceptAll(false);
        ProxyUtil.setInterceptList(new ArrayList<>());
        ProxyUtil.setInterceptHost(new String[]{});
        while (!RUNNING && CHANNEL_FUTURE == null) {
            start();
        }
    }

    public static void startInterceptHost(List<RecordIntercept> interceptList, String[] hosts) {
        ProxyUtil.setInterceptAll(false);
        ProxyUtil.setInterceptList(interceptList);
        ProxyUtil.setInterceptHost(hosts);
        while (!RUNNING && CHANNEL_FUTURE == null) {
            start();
        }
    }

    private synchronized static void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("httpServerCodec", new HttpServerCodec());
                            ch.pipeline().addLast("switchHandler", new SwitchHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            CHANNEL_FUTURE = serverBootstrap.bind("0.0.0.0", 8088).sync();
            RUNNING = true;
            LOGGER.warn("http record server startup @ 8088");

            CHANNEL_FUTURE.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            LOGGER.warn("http record server shutdown...");
            RUNNING = false;
        }
    }
}
