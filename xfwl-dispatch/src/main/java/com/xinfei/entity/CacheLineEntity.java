package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 算路缓存表（disp_cache_line）：两机构间路线，多版本
 *
 * <p>主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_cache_line")
public class CacheLineEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int hashCode() {
        return verifyKey == null ? 0 : verifyKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CacheLineEntity) {
            CacheLineEntity other = (CacheLineEntity) obj;
            return verifyKey == null ? other.verifyKey == null : verifyKey.equals(other.verifyKey);
        }
        return super.equals(obj);
    }

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 起始机构ID
     */
    private Long startOrgId;

    /**
     * 目的机构ID
     */
    private Long endOrgId;

    /**
     * 校验 key，md5(各明细起点拼接)
     */
    private String verifyKey;

    /**
     * 总距离(km)
     */
    private BigDecimal distance;

    /**
     * 总成本(元)
     */
    private BigDecimal cost;

    /**
     * 预计耗时(分钟)
     */
    private Integer estimatedTime;

    /**
     * 中转次数
     */
    private Integer transferCount;

    /**
     * 版本，从 1 开始递增
     */
    private Integer version;

    /**
     * 是否当前最新版本 0：否 1：是
     */
    private Integer isCurrent;

    /**
     * 创建时间
     */
    private Date createTime;
}
