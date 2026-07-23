package com.itheima.pinda.config;

import com.itheima.pinda.service.KafkaSender;
import com.itheima.pinda.service.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IdleStateHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;

/**
 * netty 服务启动类
 */
@Component
@Slf4j
public class NettyServer implements CommandLineRunner {
    @Autowired
    private KafkaSender kafkaSender;

    @Value("${netty.port}")
    private int port;

    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap server;
    private ChannelFuture future;

    public NettyServer() {
        // NIO线程组，用于处理网络事件
        mainGroup = new NioEventLoopGroup();
        subGroup = new NioEventLoopGroup();
        // 服务初始化工具，封装初始化服务的复杂代码
        server = new ServerBootstrap();
    }

    @Override
    public void run(String... args) throws Exception {
        // 配置netty服务端（kafkaSender此时已注入完毕）
        server.group(mainGroup, subGroup)
                .option(ChannelOption.SO_BACKLOG, 128)// 设置缓存
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioServerSocketChannel.class)// 指定使用NioServerSocketChannel产生一个Channel用来接收连接
                .childHandler(new ChannelInitializer<NioServerSocketChannel>() {
                    @Override
                    protected void initChannel(NioServerSocketChannel ch) {
                        ch.pipeline().addLast(new IdleStateHandler(120, 0, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new NettyServerHandler(kafkaSender));
                    }
                });//具体处理网络IO事件

        // 启动netty服务端，绑定端口
        this.future = server.bind(port);
        this.future.sync();
        if (!this.future.isSuccess()) {
            throw new IllegalStateException("Netty 绑定端口失败: " + port, this.future.cause());
        }
        log.info("Netty Server 启动成功，端口：{}", port);
    }

    /**
     * 优雅关闭，释放 Netty 资源
     */
    @PreDestroy
    public void destroy() {
        log.info("Netty Server 正在关闭...");
        try {
            if (future != null && future.channel() != null) {
                future.channel().close();
            }
        } catch (Exception e) {
            log.error("Netty关闭channel失败", e);
        }
        if (mainGroup != null) {
            mainGroup.shutdownGracefully();
        }
        if (subGroup != null) {
            subGroup.shutdownGracefully();
        }
        log.info("Netty Server 已关闭");
    }
}
