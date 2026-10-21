/**
 * 厂商无关点聚合层（规范《14》§6.4：点数 >50 启用聚合，点击下钻）。
 *
 * 设计：
 * - 点数不超过阈值时逐点渲染并增量更新（位置变化只 move，不重建）；
 * - 超过阈值按 Web Mercator 像素网格分桶（全球固定网格，分桶结果只随 zoom 变化，
 *   故仅订阅 zoomend 重算，不做视野裁剪——千级点全量分桶开销可忽略）；
 * - 桶内单点渲染为普通标注，多点渲染为数量标注（点击放大一级并居中到桶内均值）；
 * - 聚合结果带签名对比，轮询间数据未跨桶移动时跳过重建。
 *
 * 只依赖 MapProvider 接口，不感知具体厂商 SDK。
 */
import type { LngLat, MapProvider } from "./index";

/** 聚合层渲染的输入点 */
export interface ClusterPointInput {
  /** 业务唯一键（增量更新与高亮查找用） */
  key: string;
  lngLat: LngLat;
  /** 单点标注文字（车牌等） */
  label: string;
  /** 单点标注背景色（缺省品牌红由调用方传入） */
  background?: string;
  onClick?: () => void;
}

export interface ClusterLayerOptions {
  /** 启用聚合的最小点数（规范缺省 50） */
  threshold?: number;
  /** 聚合网格边长（屏幕像素） */
  gridSizePx?: number;
}

/** 聚合数量标注背景（色板中性灰，与单点品牌红区分层级） */
const CLUSTER_BACKGROUND = "#86909C";
const TILE_SIZE = 256;

export class MarkerClusterLayer {
  private provider: MapProvider;
  private map: any;
  private threshold: number;
  private gridSizePx: number;

  private points: ClusterPointInput[] = [];
  /** 已渲染标注：散射态 key=业务键；聚合态 key=桶键 */
  private rendered = new Map<string, any>();
  /** 散射模式下业务键 → 标注（高亮查找用）；聚合态为空 */
  private keyMarkers = new Map<string, any>();
  /** 上次聚合签名（避免轮询间无跨桶移动时整层重建） */
  private clusterSignature = "";
  private visible = true;
  private onZoomEnd = () => this.refresh();

  constructor(provider: MapProvider, map: any, options?: ClusterLayerOptions) {
    this.provider = provider;
    this.map = map;
    this.threshold = options?.threshold ?? 50;
    this.gridSizePx = options?.gridSizePx ?? 60;
    this.provider.onMapEvent(this.map, "zoomend", this.onZoomEnd);
  }

  /** 全量更新点集（轮询调用；散射态做增量 diff，聚合态签名变化才重建） */
  setPoints(points: ClusterPointInput[]): void {
    this.points = points;
    this.refresh();
  }

  /** 整层显隐（模式切换时保留数据只藏覆盖物） */
  setVisible(visible: boolean): void {
    this.visible = visible;
    this.rendered.forEach((marker) => this.provider.setMarkerVisible(marker, visible));
  }

  /** 高亮业务点（散射态生效；点已并入聚合桶时不动作） */
  highlight(key: string, active: boolean): void {
    const marker = this.keyMarkers.get(key);
    if (marker) {
      this.provider.highlightMarker(marker, active);
    }
  }

  /** 销毁：解绑事件并清空全部覆盖物 */
  destroy(): void {
    this.provider.offMapEvent(this.map, "zoomend", this.onZoomEnd);
    this.clearRendered();
    this.points = [];
  }

  private refresh(): void {
    if (this.points.length <= this.threshold) {
      this.renderScatter();
    } else {
      this.renderClustered();
    }
  }

  /** 散射态：按业务键增量更新（位置变化 move、新增 create、消失 remove） */
  private renderScatter(): void {
    if (this.clusterSignature) {
      // 从聚合态回到散射态（点数降回阈值内），清掉聚合标注
      this.clearRendered();
    }
    const seen = new Set<string>();
    for (const point of this.points) {
      seen.add(point.key);
      const existing = this.rendered.get(point.key);
      if (existing) {
        this.provider.moveMarker(existing, point.lngLat);
        continue;
      }
      const marker = this.provider.createLabeledMarker(this.map, point.lngLat, point.label, {
        background: point.background,
        onClick: point.onClick,
      });
      if (!this.visible) {
        this.provider.setMarkerVisible(marker, false);
      }
      this.rendered.set(point.key, marker);
      this.keyMarkers.set(point.key, marker);
    }
    for (const key of [...this.rendered.keys()]) {
      if (!seen.has(key)) {
        this.provider.removeOverlay(this.map, this.rendered.get(key));
        this.rendered.delete(key);
        this.keyMarkers.delete(key);
      }
    }
  }

  /** 聚合态：像素网格分桶渲染，签名未变跳过重建 */
  private renderClustered(): void {
    const zoom = this.provider.getZoom(this.map);
    const buckets = this.bucketByGrid(zoom);
    const signature = [...buckets.entries()]
      .map(([bucketKey, list]) => {
        const center = this.bucketCenter(list);
        return `${bucketKey}:${list.length}:${center.lng.toFixed(4)},${center.lat.toFixed(4)}`;
      })
      .sort()
      .join("|");
    if (signature === this.clusterSignature) {
      return;
    }
    this.clusterSignature = signature;
    this.clearRendered();

    for (const list of buckets.values()) {
      if (list.length === 1) {
        const point = list[0];
        const marker = this.provider.createLabeledMarker(this.map, point.lngLat, point.label, {
          background: point.background,
          onClick: point.onClick,
        });
        if (!this.visible) {
          this.provider.setMarkerVisible(marker, false);
        }
        this.rendered.set(point.key, marker);
        this.keyMarkers.set(point.key, marker);
        continue;
      }
      const center = this.bucketCenter(list);
      const bucketKey = `cluster:${center.lng.toFixed(4)},${center.lat.toFixed(4)}`;
      const marker = this.provider.createLabeledMarker(this.map, center, String(list.length), {
        background: CLUSTER_BACKGROUND,
        onClick: () => {
          // 下钻一级并居中到桶内均值；放大后该桶自然拆分为更细网格
          this.provider.setZoom(this.map, zoom + 1);
          this.provider.panTo(this.map, center);
        },
      });
      this.rendered.set(bucketKey, marker);
      if (!this.visible) {
        this.provider.setMarkerVisible(marker, false);
      }
    }
  }

  /** Web Mercator 像素网格分桶（全球固定网格，桶键与视野无关） */
  private bucketByGrid(zoom: number): Map<string, ClusterPointInput[]> {
    const size = TILE_SIZE * Math.pow(2, zoom);
    const buckets = new Map<string, ClusterPointInput[]>();
    for (const point of this.points) {
      const { lng, lat } = point.lngLat;
      const px = ((lng + 180) / 360) * size;
      const sin = Math.sin((Math.max(Math.min(lat, 85.05), -85.05) * Math.PI) / 180);
      const py = (0.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI)) * size;
      const bucketKey = `${Math.floor(px / this.gridSizePx)}_${Math.floor(py / this.gridSizePx)}`;
      const bucket = buckets.get(bucketKey);
      if (bucket) {
        bucket.push(point);
      } else {
        buckets.set(bucketKey, [point]);
      }
    }
    return buckets;
  }

  private bucketCenter(list: ClusterPointInput[]): LngLat {
    let lng = 0;
    let lat = 0;
    for (const point of list) {
      lng += point.lngLat.lng;
      lat += point.lngLat.lat;
    }
    return { lng: lng / list.length, lat: lat / list.length };
  }

  private clearRendered(): void {
    this.rendered.forEach((marker) => this.provider.removeOverlay(this.map, marker));
    this.rendered.clear();
    this.keyMarkers.clear();
    this.clusterSignature = "";
  }
}
