package com.itheima.pinda.DTO;

import com.itheima.pinda.entity.CacheLineEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CacheLineDTO extends CacheLineEntity {

    private List<CacheLineDetailDTO> cacheLineDetailDTOS;

}
