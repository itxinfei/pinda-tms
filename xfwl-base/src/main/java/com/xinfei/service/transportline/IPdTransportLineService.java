package com.xinfei.service.transportline;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.transportline.XfTransportLine;

import java.util.List;

/**
 * 业务线路 服务类
 */
public interface IPdTransportLineService extends IService<XfTransportLine> {

    /**
     * 添加线路
     */
    XfTransportLine saveTransportLine(XfTransportLine xfTransportLine);

    /**
     * 获取线路分页数据
     */
    IPage<XfTransportLine> findByPage(Integer page, Integer pageSize, String lineNumber, String name,
                                      Long transportLineTypeId);

    /**
     * 获取线路列表
     *
     * @param ids    线路id列表
     * @param orgId  机构id
     * @param orgIds 机构id列表
     */
    List<XfTransportLine> findAll(List<Long> ids, Long orgId, List<Long> orgIds);

    /**
     * 禁用线路
     */
    void disable(Long id);
}
