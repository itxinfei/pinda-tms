package com.xinfei.feign.webManager;

import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
import com.xinfei.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.xinfei.DTO.webManager.TaskTransportQueryDTO;
import com.xinfei.DTO.webManager.TransportBoardDTO;
import com.xinfei.DTO.webManager.TransportOrderQueryDTO;
import com.xinfei.common.utils.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 管理端聚合 Feign 接口熔断降级。
 *
 * <p>管理端 4 个核心分页接口（运输任务/运单/司机作业/取派件）依赖 {@link WebManagerFeign}，
 * 一旦 xfwl-aggregation 抖动，这些列表页会直接 500。本 fallback 让分页查询在降级时返回
 * 空壳 {@link PageResponse}，看板聚合返回字段全为空列表的空对象，避免调用方 NPE。</p>
 */
@Component
public class WebManagerFeignFallback implements WebManagerFeign {

    private static final Logger log = LoggerFactory.getLogger(WebManagerFeignFallback.class);

    @Override
    public PageResponse<DriverJobDTO> findDriverJobByPage(DriverJobQueryDTO dto) {
        log.warn("远程调用 xfwl-aggregation 失败: findDriverJobByPage({}), 返回空分页", dto);
        return emptyPage();
    }

    @Override
    public PageResponse<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(TaskPickupDispatchQueryDTO dto) {
        log.warn("远程调用 xfwl-aggregation 失败: findTaskPickupDispatchJobByPage({}), 返回空分页", dto);
        return emptyPage();
    }

    @Override
    public PageResponse<TransportOrderDTO> findTransportOrderByPage(TransportOrderQueryDTO dto) {
        log.warn("远程调用 xfwl-aggregation 失败: findTransportOrderByPage({}), 返回空分页", dto);
        return emptyPage();
    }

    @Override
    public PageResponse<TaskTransportDTO> findTaskTransportByPage(TaskTransportQueryDTO dto) {
        log.warn("远程调用 xfwl-aggregation 失败: findTaskTransportByPage({}), 返回空分页", dto);
        return emptyPage();
    }

    @Override
    public TransportBoardDTO findTransportBoard() {
        log.warn("远程调用 xfwl-aggregation 失败: findTransportBoard(), 返回空看板");
        return new TransportBoardDTO();
    }

    /**
     * 构造一个字段全 0、items 为空的 {@link PageResponse}，与降级前空结果语义一致。
     */
    private static <T> PageResponse<T> emptyPage() {
        PageResponse<T> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(0);
        result.setPages(0L);
        result.setPage(0);
        result.setItems(Collections.emptyList());
        return result;
    }
}
