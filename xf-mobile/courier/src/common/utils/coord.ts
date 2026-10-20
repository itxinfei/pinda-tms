// 坐标系转换：后端统一存 WGS84，微信/腾讯地图展示需 GCJ-02。
// 三端（driver/courier/customer）各持一份同实现，源头为本文件，改动须三端同步；
// 边界矩形与返回形状以通行实现为准（72.004~137.8347 / 0.8293~55.8271；返回 {lng,lat}，
// 2026-10-17 统一：旧版边界 73.66~135.05 漏边境区、返回元组与 customer 版不一致）。
const PI = Math.PI
const A = 6378245.0 // 长半轴
const EE = 0.00669342162296594323 // 偏心率平方

/** GCJ-02 坐标点（三端统一返回形状） */
export interface Gcj02Point {
  lng: number
  lat: number
}

function transformLat(lng: number, lat: number): number {
  let ret =
    -100.0 +
    2.0 * lng +
    3.0 * lat +
    0.2 * lat * lat +
    0.2 * lng * lat +
    0.2 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lat * PI) + 40.0 * Math.sin((lat / 3.0) * PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((lat / 12.0) * PI) + 320.0 * Math.sin((lat * PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLng(lng: number, lat: number): number {
  let ret =
    300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lng * PI) + 40.0 * Math.sin((lng / 3.0) * PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((lng / 12.0) * PI) + 300.0 * Math.sin((lng / 30.0) * PI)) * 2.0) / 3.0
  return ret
}

function outOfChina(lng: number, lat: number): boolean {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

// WGS84 -> GCJ-02（火星坐标）
export function wgs84ToGcj02(lng: number, lat: number): Gcj02Point {
  if (outOfChina(lng, lat)) return { lng, lat }
  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI)
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI)
  return { lng: lng + dLng, lat: lat + dLat }
}

// 安全转换：入参异常/为空时返回 null，不抛错
export function safeToGcj02(
  lng: number | string | null | undefined,
  lat: number | string | null | undefined
): Gcj02Point | null {
  if (lng === null || lng === undefined || lat === null || lat === undefined) return null
  const nl = Number(lng)
  const na = Number(lat)
  if (Number.isNaN(nl) || Number.isNaN(na)) return null
  return wgs84ToGcj02(nl, na)
}
