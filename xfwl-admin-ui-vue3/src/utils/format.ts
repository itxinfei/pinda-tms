/**
 * 通用数值/金额格式化（纯函数，零依赖）
 * 统一页面数值展示口径：空值显示 "-"，千分位分隔
 */

/** 空值判定：null / undefined / 空串视为无值（0 是有效值） */
function isEmptyValue(value: unknown): boolean {
  return value === null || value === undefined || value === "";
}

/**
 * 千分位数值格式化
 * @param value 数值或数字字符串
 * @param fractionDigits 指定时固定该小数位；不传则保留原值有效小数位
 * @returns 例如 1234.5 -> "1,234.5"；无值或非数字返回 "-"
 */
export function formatNumber(value: number | string | null | undefined, fractionDigits?: number): string {
  if (isEmptyValue(value)) {
    return "-";
  }
  const num = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(num)) {
    return "-";
  }
  if (fractionDigits !== undefined) {
    return num.toLocaleString("zh-CN", {
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    });
  }
  return num.toLocaleString("zh-CN");
}

/**
 * 金额格式化：千分位 + 固定两位小数
 * @returns 例如 26 -> "26.00"，1234.5 -> "1,234.50"；无值返回 "-"
 */
export function formatAmount(value: number | string | null | undefined): string {
  return formatNumber(value, 2);
}
