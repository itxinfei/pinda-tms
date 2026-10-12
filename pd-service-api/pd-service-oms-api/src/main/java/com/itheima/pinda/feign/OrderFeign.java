package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.OrderCancelDTO;
import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.OrderLocationDto;
import com.itheima.pinda.DTO.OrderSearchDTO;
import com.itheima.pinda.DTO.dashboard.OrderOverviewDTO;
import com.itheima.pinda.DTO.dashboard.OrderTrendDTO;
import com.itheima.pinda.DTO.dashboard.PendingOrderDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
// R2④：list 返回类型已改为 OrderDTO，不再依赖 entity 包
import com.itheima.pinda.feign.fallback.OrderFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;
import java.util.Map;

@FeignClient(value = "pd-oms", fallback = OrderFeignFallback.class, path = "/order")
@Hidden
public interface OrderFeign {
    /**
     * 新增订单
     *
     * @param orderDTO 订单信息
     * @return 订单信息
     */
    @PostMapping("")
    OrderDTO save(@RequestBody OrderDTO orderDTO);

    /**
     * 修改订单信息
     *
     * @param id       订单id
     * @param orderDTO 订单信息
     * @return 订单信息
     */
    @PutMapping("/{id}")
    OrderDTO updateById(@PathVariable(name = "id") String id, @RequestBody OrderDTO orderDTO);

    /**
     * 订单支付确认（专用端点，服务端校验后置为已支付）
     *
     * @param id 订单id
     * @return 支付结果
     */
    @PutMapping("/{id}/pay")
    Result pay(@PathVariable(name = "id") String id);

    /**
     * 订单改价（专用端点，服务端重算运费）
     *
     * @param id       订单id
     * @param orderDTO 订单信息（用于重算运费）
     * @return 改价结果
     */
    @PutMapping("/{id}/reprice")
    Result reprice(@PathVariable(name = "id") String id, @RequestBody OrderDTO orderDTO);

    /**
     * 订单取消（专用端点，原因必填，状态矩阵由 pd-oms 校验）
     *
     * @param id        订单id
     * @param cancelDTO 取消入参
     * @return 取消结果（code=0 成功）
     */
    @PutMapping("/{id}/cancel")
    Result cancel(@PathVariable(name = "id") String id, @RequestBody OrderCancelDTO cancelDTO);

    /**
     * 获取订单分页数据
     *
     * @param orderDTO 查询条件
     * @return 订单分页数据
     */
    @PostMapping("/page")
    PageResponse<OrderDTO> findByPage(@RequestBody OrderDTO orderDTO);

    /**
     * 根据id获取订单详情
     *
     * @param id 订单Id
     * @return 订单详情
     */
    @GetMapping("/{id}")
    OrderDTO findById(@PathVariable(name = "id") String id);

    /**
     * 客户端订单模糊搜索
     *
     * @param orderSearchDTO
     * @return
     */
    @RequestMapping(value = "pageLikeForCustomer", method = RequestMethod.POST)
    PageResponse<OrderDTO> pageLikeForCustomer(@RequestBody OrderSearchDTO orderSearchDTO);

    /**
     * 获取id集合
     *
     * @param ids
     * @return
     */
    @GetMapping("ids")
    List<OrderDTO> findByIds(@RequestParam("ids") List<String> ids);

    @PostMapping("list")
    List<OrderDTO> list(@RequestBody OrderSearchDTO orderSearchDTO);

    @PostMapping("orderMsg")
    Map getOrderMsg(@RequestBody OrderDTO orderAddDto);

    @PostMapping("location/saveOrUpdate")
    OrderLocationDto saveOrUpdateLoccation(@RequestBody OrderLocationDto orderLocationDto);

    @GetMapping("orderId")
    OrderLocationDto selectByOrderId(@RequestParam(name = "orderId") String orderId);

    @PostMapping("del")
    int deleteOrderLocation(@RequestBody OrderLocationDto orderLocationDto);

    /**
     * 工作台-订单总览（今日/待处理/运输中/已签收计数）
     *
     * @return 总览统计
     */
    @GetMapping("/dashboard/overview")
    OrderOverviewDTO dashboardOverview();

    /**
     * 工作台-近 N 日运输量趋势
     *
     * @param days 天数（服务端钳制 1~30，默认 7）
     * @return 每日趋势点（缺日补 0）
     */
    @GetMapping("/dashboard/trend")
    List<OrderTrendDTO> dashboardTrend(@RequestParam(name = "days", defaultValue = "7") Integer days);

    /**
     * 工作台-最新待处理运单
     *
     * @param limit 返回条数（服务端钳制 1~20，默认 5）
     * @return 待处理运单列表
     */
    @GetMapping("/dashboard/pending")
    List<PendingOrderDTO> dashboardPending(@RequestParam(name = "limit", defaultValue = "5") Integer limit);

}