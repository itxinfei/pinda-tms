package com.xinfei.vo.dispatch;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 一键派单入参（FR-04 / DSP-03）
 */
@Data
public class DispatchAssignForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待调度订单 id 列表（同机构、同目的网点的一批订单） */
    private List<String> orderIds;

    /** 承运车辆 id */
    private String truckId;

    /** 车次 id（可选：指定车次则按车次规划，不传按线路规划） */
    private String tripsId;
}