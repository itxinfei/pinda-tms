package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 算路缓存明细表（disp_cache_line_detail）：各段实际线路
 *
 * <p>业务线路ID（transportLineId）落库为 BIGINT；pd-base 线路 Feign 以 String 协议交互，
 * 落库点完成转换。主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_cache_line_detail")
public class CacheLineDetailEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CacheLineDetailEntity) {
            CacheLineDetailEntity other = (CacheLineDetailEntity) obj;
            return id == null ? other.id == null : id.equals(other.id);
        }
        return super.equals(obj);
    }

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 缓存路线ID
     */
    private Long cacheLineId;

    /**
     * 段顺序
     */
    private Integer sort;

    /**
     * 本段起始机构ID
     */
    private Long startOrgId;

    /**
     * 本段目的机构ID
     */
    private Long endOrgId;

    /**
     * 业务线路ID（base_transport_line.id，落库 BIGINT）
     */
    private Long transportLineId;

    /**
     * 创建时间
     */
    private Date createTime;
}
