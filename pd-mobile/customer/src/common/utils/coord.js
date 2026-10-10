/* ============================================
   品达TMS 移动端 · 坐标转换工具
   后端统一存储 WGS84（GPS/北斗原始），
   微信小程序 <map> 固定腾讯 GCJ-02，展示层必须转换。
   规范来源：docs/需求文档/51-移动三端建设规划.md §三
   ============================================ */

const PI = 3.1415926535897932384626;
const A = 6378245.0;
const EE = 0.00669342162296594323;

function outOfChina(lng, lat) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
  return ret;
}

function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
  return ret;
}

/**
 * WGS84 经纬度 → GCJ-02 经纬度（火星坐标）
 * @param {number} lng 经度
 * @param {number} lat 纬度
 * @returns {{lng: number, lat: number}} GCJ-02 坐标
 */
export function wgs84ToGcj02(lng, lat) {
  if (outOfChina(lng, lat)) {
    return { lng, lat };
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * PI;
  let magic = Math.sin(radLat);
  magic = 1 - EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
  dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
  return { lng: lng + dLng, lat: lat + dLat };
}

/**
 * 安全转换：入参异常/为空时返回 null，不抛错
 */
export function safeToGcj02(lng, lat) {
  try {
    if (lng === null || lng === undefined || lat === null || lat === undefined) return null;
    const nl = Number(lng);
    const na = Number(lat);
    if (Number.isNaN(nl) || Number.isNaN(na)) return null;
    return wgs84ToGcj02(nl, na);
  } catch (e) {
    return null;
  }
}
