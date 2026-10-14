/**
 * Copyright (c) 2019 联智合创 All rights reserved.
 * <p>
 * http://www.witlinked.com
 * <p>
 * 版权所有，侵权必究！
 */

package com.xinfei.service;


import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.DTO.OrgJobTreeDTO;
import com.xinfei.DTO.ScheduleJobDTO;
import com.xinfei.entity.ScheduleJobEntity;

import java.util.List;
import java.util.Map;

/**
 * 定时任务
 *
 * @author
 */
public interface IScheduleJobService extends IService<ScheduleJobEntity> {

    List<OrgJobTreeDTO> page(Map<String, Object> params);

    ScheduleJobDTO get(Long id);

    ScheduleJobDTO getByOrgId(Long orgId);

    /**
     * 保存定时任务
     */
    void save(ScheduleJobDTO dto);

    /**
     * 更新定时任务
     */
    void update(ScheduleJobDTO dto);

    /**
     * 批量删除定时任务
     */
    void deleteBatch(Long[] ids);

    /**
     * 批量更新定时任务状态
     */
    int updateBatch(Long[] ids, int status);

    /**
     * 立即执行
     */
    void run(Long[] ids);

    /**
     * 暂停运行
     */
    void pause(Long[] ids);

    /**
     * 恢复运行
     */
    void resume(Long[] ids);

    /**
     * 按机构ID立即执行一次调度任务（XXL-Job 手动/定时触发入口）
     */
    void runByOrgId(Long orgId);
}
