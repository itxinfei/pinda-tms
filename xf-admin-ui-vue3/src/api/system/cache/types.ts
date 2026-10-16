import type { BaseQueryParams, PageResult } from "@/api/common";

/** 缓存键查询参数 */
export interface CacheKeyQueryParams extends BaseQueryParams {
  /** 匹配模式（Redis glob），如 satoken:*、 */
  pattern?: string;
}

/** 缓存键对象 */
export interface CacheKeyItem {
  /** 键名 */
  key: string;
  /** Redis 类型：string/hash/list/set/zset */
  type: string;
  /** 剩余生存时间（秒）；-1=永不过期 */
  ttl: number;
}

export type { PageResult };
