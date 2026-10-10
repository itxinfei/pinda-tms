package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.ImmutableList;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.OrgJobTreeDTO;
import com.itheima.pinda.DTO.ScheduleJobDTO;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.ScheduleJobEntity;
import com.itheima.pinda.enums.org.OrgType;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.service.IScheduleJobService;
import com.itheima.pinda.utils.IdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 定时任务
 *
 * @author
 */
@RestController
@RequestMapping("/schedule")
@Tag(name = "定时任务")
public class ScheduleJobController {
    private static final List<Integer> ORG_TYPE = ImmutableList.of(OrgType.BUSINESS_HALL.getType(), OrgType.TOP_TRANSFER_CENTER.getType()).asList();

    @Autowired
    private IScheduleJobService scheduleJobService;

    @Autowired
    private OrgFeign orgFeign;


    @GetMapping("page")
    @Operation(summary = "分页")
    public List<OrgJobTreeDTO> page(@Parameter(hidden = true) @RequestParam Map<String, Object> params) {

        return scheduleJobService.page(params);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    public ScheduleJobDTO info(@PathVariable("id") String id) {
        ScheduleJobDTO schedule = scheduleJobService.get(id);
        return schedule;
    }

    @GetMapping("dispatch/{id}")
    @Operation(summary = "调度信息")
    public Result dispatchInfo(@PathVariable("id") String id) {

        LambdaQueryWrapper<ScheduleJobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleJobEntity::getBusinessId, id);
        ScheduleJobEntity scheduleJobEntity = scheduleJobService.getOne(wrapper);
        if (scheduleJobEntity == null) {
            return Result.error(404, "机构没有任务信息");
        }

        ScheduleJobDTO schedule = new ScheduleJobDTO();
        BeanUtils.copyProperties(scheduleJobEntity, schedule);
        return Result.ok().put("data", schedule);
    }

    @PostMapping
    @Operation(summary = "保存")
    public Result save(@RequestBody ScheduleJobDTO dto) {

        scheduleJobService.save(dto);

        return Result.ok();
    }

    @PostMapping("dispatch")
    @Operation(summary = "保存或修改")
    public Result dispatch(@RequestBody ScheduleJobDTO dto) {

        String businessId = dto.getBusinessId();
        if (StringUtils.isBlank(businessId)) {
            return Result.error(400, "机构ID不能为空");
        }
        OrgDTO org = orgFeign.get(Long.valueOf(businessId));
        if (org == null) {
            return Result.error(404, "机构不存在");
        }
        Integer orgType = org.getOrgType();
        if (!ORG_TYPE.contains(orgType)) {
            return Result.error(400, "无法给转运中心以上的机构增加调度任务");
        }

        if (StringUtils.isNotBlank(dto.getId())) {
            dto.setUpdateDate(new Date());
            scheduleJobService.update(dto);
            return Result.ok();
        } else {
            dto.setId(IdUtils.get());
            dto.setBeanName("dispatchTask");
            dto.setCreateDate(new Date());
            scheduleJobService.save(dto);

            return Result.ok();
        }
    }

    @PutMapping
    @Operation(summary = "修改")
    public Result update(@RequestBody ScheduleJobDTO dto) {

        scheduleJobService.update(dto);

        return Result.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    public Result delete(@RequestBody String[] ids) {
        scheduleJobService.deleteBatch(ids);

        return Result.ok();
    }

    @PutMapping("/run/{id}")
    @Operation(summary = "立即执行")
    public Result run(@PathVariable String id) {
        scheduleJobService.run(new String[]{id});

        return Result.ok();
    }

    @PutMapping("/run")
    @Operation(summary = "立即执行")
    public Result run(@RequestBody String[] ids) {
        scheduleJobService.run(ids);

        return Result.ok();
    }

    @PutMapping("/pause/{id}")
    @Operation(summary = "暂停")
    public Result pause(@PathVariable String id) {
        scheduleJobService.pause(new String[]{id});

        return Result.ok();
    }

    @PutMapping("/pause")
    @Operation(summary = "暂停")
    public Result pause(@RequestBody String[] ids) {
        scheduleJobService.pause(ids);

        return Result.ok();
    }

    @PutMapping("/resume/{id}")
    @Operation(summary = "恢复")
    public Result resume(@PathVariable String id) {
        scheduleJobService.resume(new String[]{id});

        return Result.ok();
    }

    @PutMapping("/resume")
    @Operation(summary = "恢复")
    public Result resume(@RequestBody String[] ids) {
        scheduleJobService.resume(ids);

        return Result.ok();
    }

}
