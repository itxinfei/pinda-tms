package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.ScheduleJobDTO;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.ScheduleExceptionOrder;
import com.itheima.pinda.service.IScheduleExceptionOrderService;
import com.itheima.pinda.service.IScheduleJobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 异常调度订单管理
 *
 * <p>查询调度过程中无法完成线路规划的订单（ERROR 分组），并支持人工处理流转：
 * 查询待处理列表 → 运营人员修正数据 → 标记已处理；同时支持按机构触发重新调度。</p>
 */
@Slf4j
@RestController
@RequestMapping("/scheduleExceptionOrder")
@Api(tags = "异常调度订单")
public class ScheduleExceptionOrderController {

    @Autowired
    private IScheduleExceptionOrderService scheduleExceptionOrderService;

    @Autowired
    private IScheduleJobService scheduleJobService;

    /**
     * 分页查询异常调度订单
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   状态筛选（可选：0-待处理 1-已处理）
     * @param orderId  订单ID模糊筛选（可选）
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询异常调度订单")
    @GetMapping("/page")
    public Result page(@RequestParam(name = "page", defaultValue = "1") Integer page,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       @RequestParam(name = "status", required = false) Integer status,
                       @RequestParam(name = "orderId", required = false) String orderId) {
        LambdaQueryWrapper<ScheduleExceptionOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, ScheduleExceptionOrder::getStatus, status);
        wrapper.like(StringUtils.isNotBlank(orderId), ScheduleExceptionOrder::getOrderId, orderId);
        wrapper.orderByAsc(ScheduleExceptionOrder::getStatus).orderByDesc(ScheduleExceptionOrder::getCreateTime);

        IPage<ScheduleExceptionOrder> result = scheduleExceptionOrderService.page(new Page<>(page, pageSize), wrapper);
        return Result.ok().put("data", result);
    }

    /**
     * 标记异常调度订单为已处理（人工处理闭环）
     *
     * @param id     异常订单登记ID
     * @param remark 处理备注（可选）
     * @return 处理结果
     */
    @ApiOperation(value = "标记异常调度订单为已处理")
    @PutMapping("/{id}/handle")
    public Result handle(@PathVariable(name = "id") String id,
                         @RequestParam(name = "remark", required = false) String remark) {
        ScheduleExceptionOrder record = scheduleExceptionOrderService.getById(id);
        if (record == null) {
            return Result.error(400, "异常调度订单记录不存在");
        }
        if (ScheduleExceptionOrder.STATUS_HANDLED == record.getStatus()) {
            return Result.error(400, "该记录已处理，请勿重复操作");
        }
        ScheduleExceptionOrder update = new ScheduleExceptionOrder();
        update.setId(id);
        update.setStatus(ScheduleExceptionOrder.STATUS_HANDLED);
        update.setRemark(remark);
        update.setHandleTime(LocalDateTime.now());
        scheduleExceptionOrderService.updateById(update);
        log.info("[异常调度] 订单[{}]标记为已处理，备注: {}", record.getOrderId(), remark);
        return Result.ok();
    }

    /**
     * 按机构触发重新调度（人工/自动重试入口）
     *
     * <p>运营人员修正基础数据后，可调用本接口对该机构重新执行一次智能调度，
     * 若调度成功（订单不再进入 ERROR 分组）则对应异常登记被自动清理。</p>
     *
     * @param agencyId 机构ID
     * @return 处理结果
     */
    @ApiOperation(value = "按机构触发重新调度")
    @PutMapping("/retry/{agencyId}")
    public Result retry(@PathVariable(name = "agencyId") String agencyId) {
        if (StringUtils.isBlank(agencyId)) {
            return Result.error(400, "机构ID不能为空");
        }
        ScheduleJobDTO scheduleJob = scheduleJobService.getByOrgId(agencyId);
        if (scheduleJob == null || StringUtils.isBlank(scheduleJob.getId())) {
            log.warn("[异常调度] 机构[{}]未配置调度任务，无法触发重新调度", agencyId);
            return Result.error(400, "该机构未配置调度任务，无法重新调度");
        }
        // 触发该机构的智能调度任务立即执行
        scheduleJobService.run(new String[]{scheduleJob.getId()});
        log.info("[异常调度] 机构[{}]已触发重新调度，任务ID: {}", agencyId, scheduleJob.getId());
        return Result.ok();
    }
}
