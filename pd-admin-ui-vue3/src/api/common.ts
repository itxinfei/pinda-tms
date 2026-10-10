/**
 * API 响应壳
 */
export interface ApiResult<T = unknown> {
  /** 业务状态码（脚手架为字符串码，品达后端为数字码） */
  code: string | number;
  /** 业务数据 */
  data: T;
  /** 业务消息 */
  msg: string;
}

/**
 * 分页查询基础参数
 */
export interface BaseQueryParams {
  /** 页码 */
  pageNum: number;
  /** 每页记录数 */
  pageSize: number;

  /** 排序字段 */
  sortBy?: string;

  /** 排序方式（正序:ASC；反序:DESC） */
  order?: string;
}

/**
 * 分页接口结果
 */
export interface PageResult<T> {
  /** 数据列表 */
  list: T[];
  /** 总记录数 */
  total: number;
}

/**
 * 通用选项项
 */
export interface OptionItem {
  /** 选项值 */
  value: string | number;
  /** 选项标签 */
  label: string;
  /** 子选项 */
  children?: OptionItem[];
}

/**
 * Excel 导入结果
 */
export interface ExcelResult {
  /** 业务状态码 */
  code: string;
  /** 无效数据数量 */
  invalidCount: number;
  /** 有效数据数量 */
  validCount: number;
  /** 错误信息列表 */
  messageList: string[];
}

/**
 * 后端统一分页结果（com.itheima.pinda.common.utils.PageResponse）
 */
export interface PageResponse<T> {
  /** 总记录数 */
  counts: number;
  /** 每页记录数 */
  pagesize: number;
  /** 总页数 */
  pages: number;
  /** 当前页码 */
  page: number;
  /** 当前页数据 */
  items: T[];
}
