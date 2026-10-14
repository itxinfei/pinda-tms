package com.xinfei.DTO;

import com.xinfei.DTO.transportline.TransportLineDto;
import com.xinfei.entity.CacheLineDetailEntity;
import com.xinfei.entity.CacheLineEntity;
import com.xinfei.utils.IdConvert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 线路规划结果分析模型：把 {@link RoutePlanningDTO} 递归树拆解、聚合为可落库的
 * 缓存线路（CacheLineEntity + 明细 CacheLineDetailEntity）。
 *
 * <p>机构/线路ID在实体侧为 Long；pd-base 线路 Feign（{@link TransportLineDto}）仍为 String 边界，
 * 构造明细时在本模型内完成转换。缓存线路主键不预生成，交由 MyBatis-Plus ASSIGN_ID 在 insert 时回填，
 * 明细的 cacheLineId 在 {@code TaskRoutePlanningServiceImpl.mergeLine} 新线落库后补齐。</p>
 */
public class AnalysisRoutePlanningDTO {
    private Map<Integer, List<CacheLineDetailEntity>> lineMap = new HashMap<>();
    private Map<String, TransportLineDto> transportLineDtoMap = new HashMap<>();

    private RoutePlanningDTO routePlanningDTO;
    private Long startOrgId;
    private Long endOrgId;

    private Integer index = 0;

    /**
     * 在此加工可用线路信息 返回完整的数据格式
     */
    public Map<CacheLineEntity, List<CacheLineDetailEntity>> get() {
        Map<CacheLineEntity, List<CacheLineDetailEntity>> result = new HashMap<>();
        for (List<CacheLineDetailEntity> value : lineMap.values()) {
            // 集合反转
            Collections.reverse(value);
            BigDecimal distance = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;
            int estimatedTime = 0;
            StringBuffer sbf = new StringBuffer();
            for (int i = 0; i < value.size(); i++) {
                CacheLineDetailEntity cacheLineDetailEntity = value.get(i);
                cacheLineDetailEntity.setSort(i + 1);
                // 实体 transportLineId 为 Long，线路 Map 以 pd-base 的 String ID 为键
                TransportLineDto transportLineDto = transportLineDtoMap.get(IdConvert.toStr(cacheLineDetailEntity.getTransportLineId()));
                if (transportLineDto != null) {
                    if (transportLineDto.getDistance() != null) {
                        distance = distance.add(transportLineDto.getDistance());
                    }
                    if (transportLineDto.getCost() != null) {
                        cost = cost.add(transportLineDto.getCost());
                    }
                    if (transportLineDto.getEstimatedTime() != null) {
                        estimatedTime += transportLineDto.getEstimatedTime().intValue();
                    }
                    sbf.append(transportLineDto.getStartAgencyId())
                            .append("#")
                            .append(transportLineDto.getEndAgencyId())
                            .append("#")
                            .append(transportLineDto.getDistance() == null ? "" : transportLineDto.getDistance().toString())
                            .append("#")
                            .append(transportLineDto.getCost() == null ? "" : transportLineDto.getCost().toString())
                            .append("#")
                            .append(transportLineDto.getEstimatedTime() == null ? "" : transportLineDto.getEstimatedTime().toString())
                            .append("#");
                }
            }
            CacheLineEntity cacheLineEntity = new CacheLineEntity();
            cacheLineEntity.setDistance(distance);
            cacheLineEntity.setCost(cost);
            cacheLineEntity.setEstimatedTime(estimatedTime);
            cacheLineEntity.setStartOrgId(this.startOrgId);
            cacheLineEntity.setEndOrgId(this.endOrgId);
            cacheLineEntity.setTransferCount(value.size());
            cacheLineEntity.setIsCurrent(1);
            cacheLineEntity.setVerifyKey(DigestUtils.md5DigestAsHex(sbf.toString().getBytes()));
            result.put(cacheLineEntity, value);
        }
        return result;
    }

    public AnalysisRoutePlanningDTO(RoutePlanningDTO routePlanningDTO, Long startOrgId, Long endOrgId) {
        this.routePlanningDTO = routePlanningDTO;
        this.startOrgId = startOrgId;
        this.endOrgId = endOrgId;
    }

    public AnalysisRoutePlanningDTO build() {
        build(this.routePlanningDTO);
        return this;
    }

    /**
     * 获取全部可到达线路
     * 遍历之前查找到的线路，feasibleTransportLineDtos 不为空的时候表示该条线路可以通行。
     * @param routePlanningDTO
     * @return
     */
    private Integer[] build(RoutePlanningDTO routePlanningDTO) {
        List<Integer> indexList = new ArrayList<>();
        List<TransportLineDto> dtos = routePlanningDTO.getNext();
        if (!CollectionUtils.isEmpty(dtos)) {
            for (int i = 0; i < dtos.size(); i++) {
                TransportLineDto item = dtos.get(i);
                RoutePlanningDTO routePlanning = routePlanningDTO.get(item);
                Integer[] result = build(routePlanning);
                if (result.length > 0) {
                    // 含有可行线路，增加上级中转点
                    for (Integer integer : result) {
                        CacheLineDetailEntity cacheLineDetailEntity = new CacheLineDetailEntity();
                        cacheLineDetailEntity.setStartOrgId(IdConvert.toLong(item.getStartAgencyId()));
                        cacheLineDetailEntity.setEndOrgId(IdConvert.toLong(item.getEndAgencyId()));
                        cacheLineDetailEntity.setTransportLineId(IdConvert.toLong(item.getId()));
                        lineMap.get(integer).add(cacheLineDetailEntity);
                        indexList.add(integer);
                        transportLineDtoMap.put(item.getId(), item);
                    }
                }
            }
        }

        List<TransportLineDto> feasibleDtos = routePlanningDTO.getFeasible();
        if (!CollectionUtils.isEmpty(feasibleDtos)) {
            for (int i = 0; i < feasibleDtos.size(); i++) {
                TransportLineDto item = feasibleDtos.get(i);
                // 可行路线  增加路线集合数据
                CacheLineDetailEntity cacheLineDetailEntity = new CacheLineDetailEntity();
                cacheLineDetailEntity.setStartOrgId(IdConvert.toLong(item.getStartAgencyId()));
                cacheLineDetailEntity.setEndOrgId(IdConvert.toLong(item.getEndAgencyId()));
                cacheLineDetailEntity.setTransportLineId(IdConvert.toLong(item.getId()));
                List<CacheLineDetailEntity> cacheLineDetailEntities = new ArrayList<>();
                cacheLineDetailEntities.add(cacheLineDetailEntity);
                lineMap.put(++index, cacheLineDetailEntities);
                indexList.add(index);
                transportLineDtoMap.put(item.getId(), item);
            }
        }
        if (indexList.size() > 0) {
            return indexList.toArray(new Integer[indexList.size()]);
        } else {
            return new Integer[0];
        }
    }

}
