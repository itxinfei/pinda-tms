package com.itheima.pinda.service;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.entity.LocationEntity;
import com.itheima.pinda.enums.CoordSystem;
import com.itheima.pinda.enums.LocationSource;
import com.itheima.pinda.utils.GpsLocationValidator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * netty 业务处理
 *
 * <p>链路约定：
 * <ul>
 *   <li>报文以换行符 {@code \n} 分隔，pipeline 中 LineBasedFrameDecoder 已完成拆包/粘包处理，
 *       StringDecoder 已将整行解码为 String（换行符已被剥除），本类不再处理 ByteBuf 边界；</li>
 *   <li>必填字段与经纬度校验与 HTTP 入口一致，非法报文回复 ERR 行且不进 Kafka；</li>
 *   <li>通过 {@link ChannelGroup} 统一管理活跃连接，并限制最大连接数；</li>
 *   <li>读空闲超时（120s 无数据）主动关闭僵死连接。</li>
 * </ul>
 * 处理结果向设备回写一行简单应答（OK / ERR:原因），便于设备感知上报结果。</p>
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 最大活跃连接数（GPS 设备长连接，按单节点千级预留；超限直接拒绝新连接）
     */
    private static final int MAX_CONNECTIONS = 1000;

    /**
     * 读空闲秒数（与 NettyServer 中 IdleStateHandler 的 120s 保持一致，仅用于日志）
     */
    private static final int READ_IDLE_SECONDS = 120;

    /**
     * 活跃连接组（ChannelGroup 会在连接关闭后自动移除，手动 remove 仅用于日志计数即时准确）
     */
    private final ChannelGroup activeChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final RabbitSender rabbitSender;

    @Autowired
    public NettyServerHandler(RabbitSender rabbitSender) {
        this.rabbitSender = Objects.requireNonNull(rabbitSender, "rabbitSender must not be null");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 连接上限保护，防止异常设备重连风暴耗尽文件句柄
        if (activeChannels.size() >= MAX_CONNECTIONS) {
            log.warn("[TCP连接] 活跃连接数已达上限 {}，拒绝新连接: remote={}",
                    MAX_CONNECTIONS, ctx.channel().remoteAddress());
            ctx.writeAndFlush("ERR:too-many-connections\n");
            ctx.close();
            return;
        }
        activeChannels.add(ctx.channel());
        log.info("[TCP连接] 新连接接入: remote={}, 当前活跃连接数={}",
                ctx.channel().remoteAddress(), activeChannels.size());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        activeChannels.remove(ctx.channel());
        log.info("[TCP连接] 连接断开: remote={}, 当前活跃连接数={}",
                ctx.channel().remoteAddress(), activeChannels.size());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // msg 已是去掉换行符的整行字符串，不存在半包/粘包问题
        String line = (String) msg;
        log.info("[TCP上报] 收到报文: remote={}, body={}", ctx.channel().remoteAddress(), line);

        String message = parseMessage(line);
        if (message == null) {
            ctx.writeAndFlush("ERR:bad-request\n");
            return;
        }

        //发送至RabbitMQ队列
        // RabbitSender.sendGpsTrace 内部已 catch 异常，不会抛出
        com.alibaba.fastjson.JSONObject json = JSON.parseObject(message);
        String truckId = json.getString("businessId");
        rabbitSender.sendGpsTrace(truckId, message);
        ctx.writeAndFlush("OK\n");
    }

    /**
     * 解析报文（与 HTTP 入口同口径校验，TCP 入口默认 source=NETTY_TCP / coordSystem=BD09）
     *
     * <p>设备不同报文也不同，本次设备为移动端，直接使用json格式传输。
     * 后续接 JT/T 808 北斗终端时，按协议解析后改为 source=JT808 + coordSystem=CGCS2000。</p>
     *
     * @param body 已按行切分的报文原文
     * @return 校验通过后可直接发 Kafka 的 JSON 字符串；非法返回 null
     */
    private String parseMessage(String body) {
        if (body == null || body.trim().isEmpty()) {
            log.warn("[TCP上报] 报文为空");
            return null;
        }
        LocationEntity message;
        try {
            message = JSON.parseObject(body.trim(), LocationEntity.class);
        } catch (Exception e) {
            log.warn("[TCP上报] 报文JSON格式非法: body={}", body);
            return null;
        }
        if (message == null) {
            log.warn("[TCP上报] 报文内容异常");
            return null;
        }

        // 必填字段 + 经纬度统一校验（HTTP/TCP 一致）
        String invalidMsg = GpsLocationValidator.validateRequired(message);
        if (invalidMsg != null) {
            log.warn("[TCP上报] 报文校验未通过被拒: msg={}, body={}", invalidMsg, body);
            return null;
        }

        // 坐标系校验，缺失补默认 BD09，非法值拒绝
        String coordSystem = message.getCoordSystem();
        if (coordSystem == null || coordSystem.trim().isEmpty()) {
            message.setCoordSystem(CoordSystem.BD09.getCode());
        } else if (!CoordSystem.isValid(coordSystem)) {
            log.warn("[TCP上报] 坐标系非法被拒: businessId={}, coordSystem={}", message.getBusinessId(), coordSystem);
            return null;
        }
        // 来源校验，TCP 入口默认 NETTY_TCP，非法值拒绝
        String source = message.getSource();
        if (source == null || source.trim().isEmpty()) {
            message.setSource(LocationSource.NETTY_TCP.getCode());
        } else if (!LocationSource.isValid(source)) {
            log.warn("[TCP上报] 来源非法被拒: businessId={}, source={}", message.getBusinessId(), source);
            return null;
        }

        return JSON.toJSONString(message);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        // flush 写出区域，保持连接不关闭（GPS设备需持久连接）
        ctx.flush();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 读空闲超时：设备长时间无上报，判定为僵死连接，主动关闭释放资源。
            // 车辆在线状态不依赖连接本身，而由心跳超时扫描 pd_truck.last_heartbeat_time 置离线。
            log.warn("[TCP连接] 读空闲 {}s 无数据，关闭连接: remote={}",
                    READ_IDLE_SECONDS, ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Netty连接异常，关闭channel: remote={}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
