package com.xinfei.DTO;

import com.xinfei.entity.CacheLineDetailEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CacheLineDetailDTO extends CacheLineDetailEntity {

    /**
     * 起始机构
     */
    private String startAgency;

    /**
     * 终点机构
     */
    private String endAgency;

}
