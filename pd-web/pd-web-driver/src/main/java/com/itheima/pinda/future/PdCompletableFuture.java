package com.itheima.pinda.future;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.constant.StaticStation;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.AreaSimpleVo;
import com.itheima.pinda.vo.SysUserVo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PdCompletableFuture {
    /**
     * 获取map类型用户数据集合
     *
     * @param feign    数据接口
     * @param userSet 用户id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> userMapFuture(UserFeign feign, Set<Long> userSet, Integer station, String name, String agencyId) {
        return CompletableFuture.supplyAsync(() -> {
            //查询创建者信息列表
            Long stationId = null;
            if (station != null && station == Constant.UserStation.COURIER.getStation()) {
                stationId = StaticStation.COURIER_ID;
            } else if (station != null && station == Constant.UserStation.DRIVER.getStation()) {
                stationId = StaticStation.DRIVER_ID;
            }
            List<UserDTO> userList = new ArrayList<>();
            List<UserDTO> result = feign.list(new ArrayList<>(userSet), stationId, name, StringUtils.isNotEmpty(agencyId) ? Long.valueOf(agencyId) : null);
            if (result != null) {
                userList.addAll(result);
            }
            return userList.stream().map(user -> BeanUtil.parseUser2Vo(user, null, null)).collect(Collectors.toMap(SysUserVo::getUserId, vo -> vo));
        });
    }

    /**
     * 获取机构数据列表
     *
     * @param feign       数据接口
     * @param agencyType 机构类型
     * @param ids        机构id列表
     * @return 执行结果
     */
    public static final CompletableFuture<List<OrgDTO>> agencyListFuture(OrgFeign feign, Integer agencyType, Set<Long> ids, Long countyId) {
        return CompletableFuture.supplyAsync(() -> {
            List<OrgDTO> result = feign.list(agencyType, new ArrayList<>(ids), countyId, null, null);
            // 远程调用失败(fallback 返回 null)按空列表降级
            return Rx.list(result);
        });
    }

    public static final CompletableFuture<Map> areaMapFuture(AreaFeign feign, Long parentId, Set<Long> areaSet) {
        List<AreaDTO> result = feign.findAll(parentId, new ArrayList<>(areaSet));
        // 远程调用结果可能为 null，统一通过 Rx 安全取值，避免 NPE
        return CompletableFuture.supplyAsync(() -> Rx.list(result).stream().map(BeanUtil::parseArea2Vo).collect(Collectors.toMap(AreaSimpleVo::getId, vo -> vo)));
    }

    public static CompletableFuture<Map> tripMapFuture(TransportTripsFeign feign, Set<String> tripSet) {
        return CompletableFuture.supplyAsync(() -> {
            List<TransportTripsDto> transportTripsDtoList = feign.findAll(null, new ArrayList<>(tripSet));
            return Rx.list(transportTripsDtoList).stream().collect(Collectors.toMap(TransportTripsDto::getId, TransportTripsDto::getName));
        });
    }

    public static CompletableFuture<Map<Long, TaskTransportDTO>> taskTramsportMapFuture(TransportTaskFeign feign, Set<Long> taskTransportSet) {
        return CompletableFuture.supplyAsync(() -> {
            List<TaskTransportDTO> taskTransportDTOList = new ArrayList<>();
            for (Long s : taskTransportSet) {
                TaskTransportDTO taskTransportDto = feign.findById(s);
                taskTransportDTOList.add(taskTransportDto);
            }
            return taskTransportDTOList.stream().collect(Collectors.toMap(TaskTransportDTO::getId, item -> item));
        });
    }
}
