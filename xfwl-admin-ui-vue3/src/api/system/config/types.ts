// 参数配置（CFG-01）类型定义

import type { BaseQueryParams } from "@/api/common";

/** 值类型选项 */
export type ConfigValueType = "string" | "number" | "boolean" | "json";

/**
 * 配置分页查询参数（前端统一 pageNum，api 层映射为后端 page）
 */
export interface ConfigQueryParams extends BaseQueryParams {
  /** 名称/键名关键字 */
  keywords?: string;
  /** 状态筛选：1 启用 0 停用；不传查全部 */
  status?: number | "";
}

/**
 * 配置表单对象（新增/编辑提交）
 */
export interface ConfigForm {
  /** 配置 ID（编辑时携带） */
  id?: string;
  /** 参数名称 */
  configName?: string;
  /** 参数键名（字母/数字/点/下划线/中划线） */
  configKey?: string;
  /** 参数值明文；编辑敏感项留空表示不修改 */
  configValue?: string;
  /** 值类型 */
  valueType?: ConfigValueType;
  /** 是否敏感：1 是 0 否（新增后不可改） */
  isSensitive?: number;
  /** 状态：1 启用 0 停用 */
  status?: number;
  /** 备注 */
  remark?: string;
}

/**
 * 配置列表/详情对象
 */
export interface ConfigItem {
  /** 配置 ID（雪花 ID 字符串） */
  id?: string;
  /** 参数名称 */
  configName?: string;
  /** 参数键名 */
  configKey?: string;
  /** 参数值：非敏感项回显原值，敏感项为 null */
  configValue?: string | null;
  /** 敏感值脱敏展示串（仅敏感项返回） */
  valueMasked?: string | null;
  /** 值类型 */
  valueType?: ConfigValueType;
  /** 是否敏感：1 是 0 否 */
  isSensitive?: number;
  /** 状态：1 启用 0 停用 */
  status?: number;
  /** 备注 */
  remark?: string;
}
