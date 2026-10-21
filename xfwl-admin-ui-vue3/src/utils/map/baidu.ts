/**
 * 百度地图 MapProvider 实现（JS API GL）
 *
 * 全部百度专有知识（BMapGL 全局对象、SDK URL、BD09 转换、覆盖物 API 形状）收敛在本文件，
 * 复用 ../baidu-map 既有工具（加载器/标注/视野/高亮/位移），页面不直接引用。
 */
import {
  createLabeledMarker,
  fitViewport,
  highlightMarker,
  loadBaiduMap,
  moveMarker,
  wgs84ToBd09,
} from "@/utils/baidu-map";
import type {
  CreateMapOptions,
  LabelStyle,
  LabeledMarkerOptions,
  LngLat,
  MapProvider,
  PolylineStyle,
} from "./index";

/** 起终点等通用文字标签的缺省样式 */
const DEFAULT_LABEL_STYLE = {
  padding: "2px 6px",
  fontSize: "12px",
  color: "#fff",
  border: "none",
  borderRadius: "4px",
};

export class BaiduMapProvider implements MapProvider {
  readonly vendor = "baidu" as const;
  readonly vendorCoordSystem = "BD09" as const;

  /** BMapGL 命名空间，load() 成功后可用 */
  private ns: any = null;

  async load(ak: string): Promise<void> {
    this.ns = await loadBaiduMap(ak);
  }

  toVendorCoord(lng: number, lat: number): LngLat {
    return wgs84ToBd09(lng, lat);
  }

  point(lngLat: LngLat): any {
    return new this.ns.Point(lngLat.lng, lngLat.lat);
  }

  createMap(el: HTMLElement, opts?: CreateMapOptions): any {
    const map = new this.ns.Map(el, { enableMapClick: opts?.enableMapClick ?? false });
    map.centerAndZoom(this.point(opts?.center ?? { lng: 104.19, lat: 35.86 }), opts?.zoom ?? 5);
    if (opts?.scrollWheelZoom) {
      map.enableScrollWheelZoom(true);
    }
    return map;
  }

  panTo(map: any, lngLat: LngLat): void {
    map.panTo(this.point(lngLat));
  }

  fitViewport(map: any, points: LngLat[]): void {
    fitViewport(map, points);
  }

  createLabeledMarker(map: any, lngLat: LngLat, text: string, opts?: LabeledMarkerOptions): any {
    return createLabeledMarker(map, this.ns, this.point(lngLat), text, opts);
  }

  moveMarker(marker: any, lngLat: LngLat): void {
    moveMarker(marker, this.point(lngLat));
  }

  onMapEvent(map: any, name: string, handler: () => void): void {
    map.addEventListener(name, handler);
  }

  offMapEvent(map: any, name: string, handler: () => void): void {
    map.removeEventListener(name, handler);
  }

  getZoom(map: any): number {
    return map.getZoom();
  }

  setZoom(map: any, zoom: number): void {
    map.setZoom(zoom);
  }

  /**
   * 标注显隐：Marker 与其文字标签（createLabeledMarker 内是两个独立覆盖物）必须同步，
   * 只藏 marker 会把车牌文字残留在地图上（模式切换场景实测问题）。
   */
  setMarkerVisible(marker: any, visible: boolean): void {
    if (!marker) return;
    const targets = [marker, marker.__label].filter(Boolean);
    for (const target of targets) {
      if (visible && typeof target.show === "function") {
        target.show();
      } else if (!visible && typeof target.hide === "function") {
        target.hide();
      }
    }
  }

  highlightMarker(marker: any, active: boolean): void {
    highlightMarker(marker, active);
  }

  openInfoWindow(map: any, lngLat: LngLat, html: string, opts?: { width?: number }): void {
    const infoWindow = new this.ns.InfoWindow(html, { width: opts?.width ?? 220 });
    map.openInfoWindow(infoWindow, this.point(lngLat));
  }

  drawPolyline(map: any, points: LngLat[], style?: PolylineStyle): any {
    const polyline = new this.ns.Polyline(
      points.map((p) => this.point(p)),
      {
        strokeColor: "#FF0033",
        strokeWeight: 4,
        strokeOpacity: 0.8,
        ...style,
      }
    );
    map.addOverlay(polyline);
    return polyline;
  }

  setPolylinePath(overlay: any, points: LngLat[]): void {
    if (overlay && typeof overlay.setPath === "function") {
      overlay.setPath(points.map((p) => this.point(p)));
    }
  }

  createMarker(map: any, lngLat: LngLat): any {
    const marker = new this.ns.Marker(this.point(lngLat));
    map.addOverlay(marker);
    return marker;
  }

  createLabel(map: any, text: string, lngLat: LngLat, style?: LabelStyle): any {
    const label = new this.ns.Label(text, {
      position: this.point(lngLat),
      offset: new this.ns.Size(14, -10),
    });
    label.setStyle({ ...DEFAULT_LABEL_STYLE, ...style });
    map.addOverlay(label);
    return label;
  }

  removeOverlay(map: any, overlay: any): void {
    if (overlay) {
      map.removeOverlay(overlay);
    }
  }
}
