package com.itheima.pinda.controller;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.LocationEntity;
import com.itheima.pinda.enums.CoordSystem;
import com.itheima.pinda.enums.LocationSource;
import com.itheima.pinda.service.RabbitSender;
import com.itheima.pinda.utils.GpsLocationValidator;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供Http接口方式接受司机端上报的位置信息
 */
@RestController
@RequestMapping("/netty")
@Api(tags = "车辆轨迹服务")
@Slf4j
public class NettyController {
    @Autowired
    private RabbitSender rabbitSender;

    /**
     * 将车辆定位信息发送到kafka队列（P0-4 北斗字段最小改造 · D-13）
     *
     * <p>校验与兜底逻辑：
     * <ul>
     *   <li>坐标系 coordSystem 缺失时默认 BD09（与后端 BaiduMapUtils 一致）</li>
     *   <li>坐标系不合法时拒绝上报（400）</li>
     *   <li>来源 source 缺失时默认 MOBILE（HTTP 入口默认移动端推送）</li>
     *   <li>来源不合法时拒绝上报（400）</li>
     * </ul>
     * 消费端 GpsTraceConsumer 会再做一次兜底，防御上游漏传。
     */
    @PostMapping("/push")
    public Result push(@RequestBody LocationEntity locationEntity){
        if (locationEntity == null) {
            return Result.error(400, "上报数据不能为空");
        }
        // 必填字段与经纬度校验（与 TCP 入口完全一致）：businessId/type/currentTime/lng/lat
        String invalidMsg = GpsLocationValidator.validateRequired(locationEntity);
        if (invalidMsg != null) {
            log.warn("[GPS上报] 上报数据校验未通过被拒: msg={}", invalidMsg);
            return Result.error(400, invalidMsg);
        }
        // 坐标系校验：缺失补默认 BD09，非法值拒绝
        String coordSystem = locationEntity.getCoordSystem();
        if (StringUtils.isBlank(coordSystem)) {
            locationEntity.setCoordSystem(CoordSystem.BD09.getCode());
            log.debug("[GPS上报] coordSystem 缺失，兜底为 BD09: businessId={}", locationEntity.getBusinessId());
        } else if (!CoordSystem.isValid(coordSystem)) {
            log.warn("[GPS上报] 坐标系非法被拒: businessId={}, coordSystem={}", locationEntity.getBusinessId(), coordSystem);
            return Result.error(400, "坐标系 coordSystem 非法，可选值: WGS84/GCJ02/BD09/CGCS2000");
        }
        // 来源校验：HTTP 入口缺失默认 MOBILE，非法值拒绝
        String source = locationEntity.getSource();
        if (StringUtils.isBlank(source)) {
            locationEntity.setSource(LocationSource.MOBILE.getCode());
        } else if (!LocationSource.isValid(source)) {
            log.warn("[GPS上报] 来源非法被拒: businessId={}, source={}", locationEntity.getBusinessId(), source);
            return Result.error(400, "数据来源 source 非法，可选值: MOBILE/PDA/JT808/NETTY_TCP/HTTP");
        }
        String message = JSON.toJSONString(locationEntity);
        rabbitSender.sendGpsTrace(locationEntity.getBusinessId(), message);
        log.info("HTTP接口方式推送位置信息到RabbitMQ:{}", message);
        return Result.ok();
    }
}
