package com.itheima.pinda.controller.transportline;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.controller.support.IdConverter;
import com.itheima.pinda.entity.transportline.PdTransportLine;
import com.itheima.pinda.entity.transportline.PdTransportLineType;
import com.itheima.pinda.service.transportline.IPdTransportLineService;
import com.itheima.pinda.service.transportline.IPdTransportLineTypeService;
import com.itheima.pinda.DTO.transportline.TransportLineTypeDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TransportLineTypeController
 */
@Slf4j
@RestController
@RequestMapping("base/transportLine/type")
public class TransportLineTypeController {
    @Autowired
    private IPdTransportLineTypeService transportLineTypeService;

    @Autowired
    private IPdTransportLineService transportLineService;

    /**
     * 添加线路类型
     *
     * @param dto 线路类型信息
     * @return 线路类型信息
     */
    @PostMapping("")
    public TransportLineTypeDto saveTransportLineType(@RequestBody TransportLineTypeDto dto) {
        PdTransportLineType pdTransportLineType = new PdTransportLineType();
        BeanUtils.copyProperties(dto, pdTransportLineType);
        pdTransportLineType = transportLineTypeService.saveTransportLineType(pdTransportLineType);
        return toDto(pdTransportLineType);
    }

    /**
     * 根据id获取线路类型详情
     *
     * @param id 线路类型id
     * @return 线路类型详情
     */
    @GetMapping("/{id}")
    public TransportLineTypeDto findById(@PathVariable(name = "id") String id) {
        PdTransportLineType pdTransportLineType = transportLineTypeService.getById(IdConverter.toLong(id));
        if (pdTransportLineType == null) {
            return new TransportLineTypeDto();
        }
        return toDto(pdTransportLineType);
    }

    /**
     * 获取线路类型分页数据
     *
     * @param page       页码
     * @param pageSize   页尺寸
     * @param typeNumber 类型编号
     * @param name       类型名称
     * @param agencyType 机构类型
     * @return 线路类型分页数据
     */
    @GetMapping("/page")
    public PageResponse<TransportLineTypeDto> findByPage(@RequestParam(name = "page") Integer page,
                                                         @RequestParam(name = "pageSize") Integer pageSize,
                                                         @RequestParam(name = "typeNumber", required = false) String typeNumber,
                                                         @RequestParam(name = "name", required = false) String name,
                                                         @RequestParam(name = "agencyType", required = false) Integer agencyType) {
        IPage<PdTransportLineType> transportLineTypePage = transportLineTypeService.findByPage(page, pageSize,
                typeNumber, name, agencyType);
        List<TransportLineTypeDto> dtoList = new ArrayList<>();
        transportLineTypePage.getRecords().forEach(pdTransportLineType -> dtoList.add(toDto(pdTransportLineType)));
        return PageResponse.<TransportLineTypeDto>builder().items(dtoList).pagesize(pageSize).page(page)
                .counts(transportLineTypePage.getTotal()).pages(transportLineTypePage.getPages()).build();
    }

    /**
     * 获取线路类型列表
     *
     * @param ids 线路类型id列表
     * @return 线路类型列表
     */
    @GetMapping("")
    public List<TransportLineTypeDto> findAll(@RequestParam(name = "ids", required = false) List<String> ids) {
        return transportLineTypeService.findAll(IdConverter.toLongList(ids)).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 更新线路类型信息
     *
     * @param id  线路类型id
     * @param dto 线路类型信息
     * @return 线路类型信息
     */
    @PutMapping("/{id}")
    public TransportLineTypeDto update(@PathVariable(name = "id") String id, @RequestBody TransportLineTypeDto dto) {
        PdTransportLineType pdTransportLineType = new PdTransportLineType();
        BeanUtils.copyProperties(dto, pdTransportLineType);
        pdTransportLineType.setId(IdConverter.toLong(id));
        transportLineTypeService.updateById(pdTransportLineType);
        dto.setId(id);
        return dto;
    }

    /**
     * 删除线路类型
     *
     * @param id 线路类型id
     * @return 返回信息
     */
    @PutMapping("/{id}/disable")
    public Result disable(@PathVariable(name = "id") String id) {
        Long typeId = IdConverter.toLong(id);
        // 关联校验：存在引用该类型的线路时禁止删除
        IPage<PdTransportLine> linePage = transportLineService.findByPage(1, 1, null, null, typeId);
        if (linePage != null && linePage.getTotal() > 0) {
            log.warn("[线路类型] 存在 {} 条关联线路，禁止删除: typeId={}", linePage.getTotal(), id);
            return Result.error(400, "该线路类型下存在关联线路，无法删除");
        }
        transportLineTypeService.disableById(typeId);
        return Result.ok();
    }

    /**
     * 实体 → DTO：id 转回 String（DTO 中 lastUpdateTime/updater 在新表已移除，保持为 null）
     */
    private TransportLineTypeDto toDto(PdTransportLineType entity) {
        TransportLineTypeDto dto = new TransportLineTypeDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setId(IdConverter.toStr(entity.getId()));
        return dto;
    }
}
