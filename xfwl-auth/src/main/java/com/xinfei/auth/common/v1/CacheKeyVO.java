package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 缓存键条目（系统管理-缓存管理运维页）。
 */
@Data
public class CacheKeyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 键名 */
    private String key;

    /** Redis 类型：string/hash/list/set/zset/none */
    private String type;

    /** 剩余生存时间（秒）；-1=永不过期；-2=键不存在（扫描后过期的竞态） */
    private Long ttl;
}
