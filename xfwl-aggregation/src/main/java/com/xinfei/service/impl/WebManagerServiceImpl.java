package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.dashboard.NameValueDTO;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
import com.xinfei.DTO.webManager.StageCountDTO;
import com.xinfei.DTO.webManager.StatusCountDTO;
import com.xinfei.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.xinfei.DTO.webManager.TaskTransportQueryDTO;
import com.xinfei.DTO.webManager.TransportBoardDTO;
import com.xinfei.DTO.webManager.TransportOrderQueryDTO;
import com.xinfei.common.base.BaseStatusEnum;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.enums.transportorder.TransportOrderSchedulingStatus;
import com.xinfei.enums.transportorder.TransportOrderStatus;
import com.xinfei.enums.transporttask.TransportTaskStatus;
import com.xinfei.mapper.WebManagerMapper;
import com.xinfei.service.WebManagerService;
import com.xinfei.support.PageResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebManagerServiceImpl implements WebManagerService {

    @Autowired
    private WebManagerMapper webManagerMapper;

    @Override
    public PageResponse<DriverJobDTO> findDriverJobByPage(DriverJobQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<DriverJobDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findDriverJobByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }

    @Override
    public PageResponse<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(TaskPickupDispatchQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<TaskPickupDispatchDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTaskPickupDispatchJobByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }

    @Override
    public PageResponse<TransportOrderDTO> findTransportOrderByPage(TransportOrderQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<TransportOrderDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTransportOrderByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }

    @Override
    public PageResponse<TaskTransportDTO> findTaskTransportByPage(TaskTransportQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<TaskTransportDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTaskTransportByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }

    @Override
    public TransportBoardDTO findTransportBoard() {
        Map<Integer, Long> taskCounts = toCountMap(webManagerMapper.countTaskTransportByStatus());
        Map<Integer, Long> orderCounts = toCountMap(webManagerMapper.countTransportOrderByStatus());
        Map<Integer, Long> schedulingCounts = toCountMap(webManagerMapper.countTransportOrderBySchedulingStatus());

        TransportBoardDTO board = new TransportBoardDTO();
        board.setStages(buildStages(taskCounts, orderCounts, schedulingCounts));
        board.setTaskStatusCounts(toStatusCounts(taskCounts, TransportTaskStatus.values()));
        board.setTransportOrderStatusCounts(toStatusCounts(orderCounts, TransportOrderStatus.values()));
        return board;
    }

    /**
     * 六阶段口径（DSP-01，状态码全部取自《07-数据字典与枚举口径》枚举真源，禁散落魔数）：
     * 调度=运单待调度；分配=运输任务待执行（已分配待发车）；发车=运输任务进行中（已发车）；
     * 在途=运单已装车；到达=运单到达+到达终端网点；交付=运单已签收。
     * 下钻参数随阶段下发，前端透传给既有分页接口，不做状态码硬编码。
     */
    private List<StageCountDTO> buildStages(Map<Integer, Long> taskCounts,
                                            Map<Integer, Long> orderCounts,
                                            Map<Integer, Long> schedulingCounts) {
        List<StageCountDTO> stages = new ArrayList<>();
        stages.add(new StageCountDTO("DISPATCH", "调度",
                schedulingCounts.getOrDefault(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode(), 0L),
                "transportOrder", "schedulingStatus",
                List.of(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode())));
        stages.add(new StageCountDTO("ASSIGN", "分配",
                taskCounts.getOrDefault(TransportTaskStatus.PENDING.getCode(), 0L),
                "task", "status", List.of(TransportTaskStatus.PENDING.getCode())));
        stages.add(new StageCountDTO("DEPART", "发车",
                taskCounts.getOrDefault(TransportTaskStatus.PROCESSING.getCode(), 0L),
                "task", "status", List.of(TransportTaskStatus.PROCESSING.getCode())));
        stages.add(new StageCountDTO("TRANSIT", "在途",
                orderCounts.getOrDefault(TransportOrderStatus.LOADED.getCode(), 0L),
                "transportOrder", "status", List.of(TransportOrderStatus.LOADED.getCode())));
        stages.add(new StageCountDTO("ARRIVE", "到达",
                orderCounts.getOrDefault(TransportOrderStatus.ARRIVED.getCode(), 0L)
                        + orderCounts.getOrDefault(TransportOrderStatus.ARRIVED_END.getCode(), 0L),
                "transportOrder", "status",
                List.of(TransportOrderStatus.ARRIVED.getCode(), TransportOrderStatus.ARRIVED_END.getCode())));
        stages.add(new StageCountDTO("DELIVER", "交付",
                orderCounts.getOrDefault(TransportOrderStatus.RECEIVED.getCode(), 0L),
                "transportOrder", "status", List.of(TransportOrderStatus.RECEIVED.getCode())));
        return stages;
    }

    /** 聚合查询结果转 code→count（聚合 SQL 的 name 列为状态码字符串） */
    private Map<Integer, Long> toCountMap(List<NameValueDTO> rows) {
        Map<Integer, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (NameValueDTO row : rows) {
            if (row == null || row.getName() == null) {
                continue;
            }
            try {
                map.put(Integer.valueOf(row.getName()), row.getValue() == null ? 0L : row.getValue());
            } catch (NumberFormatException ignore) {
                // 非状态码行不参与看板聚合
            }
        }
        return map;
    }

    /** 全枚举状态分布（含 0 计数保持图例稳定；中文取枚举真源，前端不做状态码映射） */
    private <E extends BaseStatusEnum<Integer, String>> List<StatusCountDTO> toStatusCounts(
            Map<Integer, Long> counts, E[] enumValues) {
        List<StatusCountDTO> result = new ArrayList<>();
        for (E e : enumValues) {
            result.add(new StatusCountDTO(e.getCode(), e.getValue(), counts.getOrDefault(e.getCode(), 0L)));
        }
        return result;
    }
}
