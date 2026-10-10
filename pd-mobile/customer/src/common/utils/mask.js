/* ============================================
   品达TMS 移动端 · 个人信息脱敏工具
   合规：R6-4 地址簿/运单/轨迹展示的姓名、电话、地址须脱敏
   ============================================ */

/**
 * 手机号脱敏：138****1234
 */
export function maskPhone(phone) {
  if (!phone) return '-';
  const p = String(phone).trim();
  if (p.length < 7) return p;
  return p.slice(0, 3) + '****' + p.slice(-4);
}

/**
 * 姓名脱敏：保留首字，其余掩码（两个字 -> 张*；三个字及以上 -> 张*）
 */
export function maskName(name) {
  if (!name) return '-';
  const n = String(name).trim();
  if (n.length <= 1) return n;
  return n[0] + '*'.repeat(Math.min(n.length - 1, 2));
}

/**
 * 详细地址脱敏：隐去门牌号（去掉数字结尾段）
 */
export function maskAddress(address) {
  if (!address) return '-';
  const a = String(address).trim();
  return a.replace(/[\d]+(号|栋|楼|室|单元)?$/g, '**');
}
