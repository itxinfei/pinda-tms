package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 算路缓存使用记录表（disp_cache_line_use）：缓存被哪次分类采用
 *
 * <p>主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_cache_line_use")
public class CacheLineUseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 分类ID
     */
    private Long classifyId;

    /**
     * 创建时间
     */
    private Date createTime;
}
