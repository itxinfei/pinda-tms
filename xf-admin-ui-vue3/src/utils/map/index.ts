/**
 * 地图厂商抽象层（MapProvider）
 *
 * 目的：页面只依赖本接口，不感知具体地图厂商（SDK 全局对象、坐标系、覆盖物 API 均为实现细节）。
 * 现状：唯一实现 ./baidu.ts（百度 JS API GL）；后期新增高德/腾讯 = 新增一个 provider 实现 +
 * map-config 下发 vendor/ak，页面零改动（多厂商地图改造·批1，2026-10-17）。
 *
 * 约定：
 * - 页面与本接口全程只传 LngLat（{lng,lat}），厂商点对象仅在 provider 内部构造；
 * - toVendorCoord 输入恒为 WGS84（档案/机构坐标口径），输出为厂商底图坐标系（百度=BD09）；
 * - 轨迹类数据接口（bd09Lng/bd09Lat）已由后端转好，页面直接作为 LngLat 使用，不再转换；
 * - 厂商 SDK（BMapGL 等）无官方 TS 类型，实例句柄统一 any。
 */

import { BaiduMapProvider } from "./baidu";

export type LngLat = { lng: number; lat: number };

/** 厂商标识：批4 map-config 下发 vendor 后按值分发；新增厂商在此扩展联合类型 */
export type MapVendor = "baidu";

export interface CreateMapOptions {
  /** 初始中心（缺省全国视角 104.19,35.86） */
  center?: LngLat;
  /** 初始级别（缺省 5） */
  zoom?: number;
  /** 是否允许点击地图（缺省 false） */
  enableMapClick?: boolean;
  /** 是否开启滚轮缩放（缺省 false） */
  scrollWheelZoom?: boolean;
}

export interface LabeledMarkerOptions {
  /** 标签背景色（缺省品牌红由调用方传入） */
  background?: string;
  /** 标签相对偏移 [x,y]（缺省 [14,-10]） */
  offset?: [number, number];
  /** 点击回调（marker 与文字标签都会绑定） */
  onClick?: () => void;
}

export interface PolylineStyle {
  strokeColor?: string;
  strokeWeight?: number;
  strokeOpacity?: number;
}

export interface LabelStyle {
  padding?: string;
  fontSize?: string;
  color?: string;
  background?: string;
  border?: string;
  borderRadius?: string;
}

export interface MapProvider {
  /** 厂商标识 */
  readonly vendor: MapVendor;
  /** 厂商底图坐标系（数据渲染边界转换的依据） */
  readonly vendorCoordSystem: "BD09" | "GCJ02";

  /** 加载厂商 SDK（ak 为空串直接 reject，由页面提示未配置） */
  load(ak: string): Promise<void>;
  /** WGS84 → 厂商底图坐标（档案/机构坐标渲染前调用） */
  toVendorCoord(lng: number, lat: number): LngLat;

  /** 创建地图实例并完成初始中心/级别/滚轮配置 */
  createMap(el: HTMLElement, opts?: CreateMapOptions): any;
  /** 构造厂商点对象（仅 provider 方法需要，页面不直接使用返回值做厂商操作） */
  point(lngLat: LngLat): any;

  /** 平移中心点 */
  panTo(map: any, lngLat: LngLat): void;
  /** 批量视野自适应（无有效点不动作） */
  fitViewport(map: any, points: LngLat[]): void;

  /** 创建带文字标签的可点击标注（标签与标记同绑事件） */
  createLabeledMarker(map: any, lngLat: LngLat, text: string, opts?: LabeledMarkerOptions): any;
  /** 移动标注位置 */
  moveMarker(marker: any, lngLat: LngLat): void;
  /** 显隐标注 */
  setMarkerVisible(marker: any, visible: boolean): void;
  /** 标注选中态高亮 */
  highlightMarker(marker: any, active: boolean): void;

  /** 打开信息窗（html 为内容片段） */
  openInfoWindow(map: any, lngLat: LngLat, html: string, opts?: { width?: number }): void;

  /** 画折线并加入地图，返回覆盖物句柄 */
  drawPolyline(map: any, points: LngLat[], style?: PolylineStyle): any;
  /** 创建普通标注并加入地图 */
  createMarker(map: any, lngLat: LngLat): any;
  /** 创建文字标签并加入地图 */
  createLabel(map: any, text: string, lngLat: LngLat, style?: LabelStyle): any;
  /** 移除覆盖物 */
  removeOverlay(map: any, overlay: any): void;
}

/**
 * 按 vendor 创建 provider 实例。
 *
 * vendor 未传或未知值时回落百度（当前唯一实现；批4 后由 map-config 的 vendor 字段驱动）。
 */
export function createMapProvider(vendor?: string): MapProvider {
  switch ((vendor || "baidu").toLowerCase()) {
    case "baidu":
      return new BaiduMapProvider();
    default:
      // 未知厂商不静默兜底，避免 AK/SDK 张冠李戴；批4 接入新厂商前先扩 MapVendor
      throw new Error(`暂不支持的地图厂商: ${vendor}`);
  }
}
