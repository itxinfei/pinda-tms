package com.itheima.pinda.config;

import com.itheima.pinda.service.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * netty 服务启动类
 */
@Component
@Slf4j
public class NettyServer implements CommandLineRunner {
    /**
     * 业务处理器（Sharable 单例，内部用 ChannelGroup 管理连接，故必须复用同一实例）
     */
    @Autowired
    private NettyServerHandler nettyServerHandler;

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
        // 配置netty服务端（nettyServerHandler 及其依赖此时已注入完毕）
        server.group(mainGroup, subGroup)
                .option(ChannelOption.SO_BACKLOG, 128)// 设置缓存
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioServerSocketChannel.class)// 指定使用NioServerSocketChannel产生一个Channel用来接收连接
                .childHandler(new ChannelInitializer<NioServerSocketChannel>() {
                    @Override
                    protected void initChannel(NioServerSocketChannel ch) {
                        // 报文约定：以换行符 \n 分隔。LineBasedFrameDecoder 解决 TCP 拆包/粘包，
                        // 单帧最大 4096 字节，超出抛 TooLongFrameException（由 exceptionCaught 关连接）
                        ch.pipeline().addLast(new LineBasedFrameDecoder(4096));
                        ch.pipeline().addLast(new StringDecoder(StandardCharsets.UTF_8));
                        ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
                        ch.pipeline().addLast(new IdleStateHandler(120, 0, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(nettyServerHandler);
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
