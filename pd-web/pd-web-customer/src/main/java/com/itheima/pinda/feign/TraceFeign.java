package com.itheima.pinda.feign;

import com.itheima.pinda.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 轨迹查询 Feign 客户端（指向 pd-netty）。
 *
 * <p>客户小程序需要"凭订单查在途轨迹"，轨迹数据落在 pd-netty 的 pd_truck_location，
 * 按运输任务 id 关联；客户侧不能直连，须经此 Feign 走内部服务调用。</p>
 */
@FeignClient(name = "pd-netty")
public interface TraceFeign {

    /**
     * 按运输任务ID批量查询轨迹点。
     *
     * @param taskIds 运输任务ID列表（去重后不超过 50 个）
     * @return Result.data 为轨迹点列表，元素是通用 Map 结构（本端不依赖 pd-netty 的实体类）
     */
    @GetMapping("/trace/byTasks")
    Result byTasks(@RequestParam("taskIds") List<String> taskIds);

}
