/**
 * 百度地图 JS API GL 动态加载器
 *
 * AK 由 BFF /api/manager/map-config-manager 下发（不进前端环境变量/仓库）；
 * 脚本按需加载一次，全局单例复用。BMapGL 无官方 TS 类型，统一 any。
 */
let loadPromise: Promise<any> | null = null;

/** WGS84→GCJ02/BD09 转换常量（公开通用算法，误差约 1~2 米） */
const WGS_A = 6378245.0;
const WGS_EE = 0.00669342162296594323;
const PI = Math.PI;
const X_PI = (PI * 3000.0) / 180.0;

function outOfChina(lng: number, lat: number): boolean {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function transformLat(x: number, y: number): number {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320.0 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0;
  return ret;
}

function transformLng(x: number, y: number): number {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0;
  return ret;
}

/** WGS84 → GCJ02（火星坐标系） */
function wgs84ToGcj02(lng: number, lat: number): { lng: number; lat: number } {
  if (outOfChina(lng, lat)) {
    return { lng, lat };
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * PI;
  let magic = Math.sin(radLat);
  magic = 1 - WGS_EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / (((WGS_A * (1 - WGS_EE)) / (magic * sqrtMagic)) * PI);
  dLng = (dLng * 180.0) / ((WGS_A / sqrtMagic) * Math.cos(radLat) * PI);
  return { lng: lng + dLng, lat: lat + dLat };
}

/** GCJ02 → BD09LL（百度经纬度） */
function gcj02ToBd09(lng: number, lat: number): { lng: number; lat: number } {
  const z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * X_PI);
  const theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * X_PI);
  return { lng: z * Math.cos(theta) + 0.0065, lat: z * Math.sin(theta) + 0.006 };
}

/**
 * WGS84 → BD09LL（百度地图直接可用坐标）
 *
 * 网点/机构档案坐标以 WGS84 落库，百度地图渲染前必须转换，否则出现数百米偏移。
 */
export function wgs84ToBd09(lng: number, lat: number): { lng: number; lat: number } {
  const gcj = wgs84ToGcj02(lng, lat);
  return gcj02ToBd09(gcj.lng, gcj.lat);
}

/**
 * 加载百度地图 JS API GL
 *
 * @param ak 百度地图 AK（空串视为未配置，直接拒绝）
 * @returns 全局 BMapGL 命名空间
 */
export function loadBaiduMap(ak: string): Promise<any> {
  const w = window as unknown as {
    BMapGL?: any;
    __bmapglInit?: () => void;
  };
  if (w.BMapGL) {
    return Promise.resolve(w.BMapGL);
  }
  if (!ak) {
    return Promise.reject(new Error("百度地图 AK 未配置"));
  }
  if (loadPromise) {
    return loadPromise;
  }
  loadPromise = new Promise((resolve, reject) => {
    w.__bmapglInit = () => resolve(w.BMapGL);
    const script = document.createElement("script");
    script.src = `https://api.map.baidu.com/api?v=1.0&type=webgl&ak=${encodeURIComponent(ak)}&callback=__bmapglInit`;
    script.async = true;
    script.onerror = () => {
      // 失败允许重试：清单例，下次调用重新拉脚本
      loadPromise = null;
      reject(new Error("百度地图脚本加载失败"));
    };
    document.head.appendChild(script);
  });
  return loadPromise;
}

/**
 * GL 版的Label 是独立覆盖物：构造时必须传 position，且**不支持**普通版
 * `marker.setLabel(label)` 的写法。若照搬 BMap3.0 的写法会在运行时抛错、整块地图白屏，
 * 故统一走这里创建带文字标注的 Marker（GL 与 BMap3.0 都成立的最小交集写法）。
 *
 * @param map 地图实例（BMapGL.Map）
 * @param ns 命名空间（BMapGL）
 * @param point 标注位置
 * @param text 标注文字
 * @param options 样式与交互
 * @returns 创建好的 Marker
 */
export function createLabeledMarker(
  map: any,
  ns: any,
  point: any,
  text: string,
  options: {
    background?: string;
    offset?: [number, number];
    onClick?: () => void;
  } = {}
) {
  const { background = "#2b6cff", offset = [14, -10], onClick } = options;

  const marker = new ns.Marker(point);
  const label = new ns.Label(text, { position: point, offset: new ns.Size(offset[0], offset[1]) });
  label.setStyle({
    padding: "2px 6px",
    fontSize: "12px",
    color: "#fff",
    background,
    border: "none",
    borderRadius: "4px",
  });
  map.addOverlay(marker);
  map.addOverlay(label);
  if (onClick) {
    marker.addEventListener("click", onClick);
  }
  return marker;
}

/**
 * 批量自适应视野：GL 版Map.setViewport 在部分版本缺失，降级为按首点定位。
 *
 * @param map 地图实例
 * @param points 坐标集合（至少一个有效点才动作）
 */
export function fitViewport(map: any, points: any[]) {
  const valid = points.filter((p) => p && Number.isFinite(Number(p.lng)) && Number.isFinite(Number(p.lat)));
  if (!valid || !valid.length) {
    return;
  }
  if (typeof map.setViewport === "function") {
    map.setViewport(valid);
    return;
  }
  map.centerAndZoom(valid[0], 12);
}

/**
 * Marker 位置更新：GL 与 BMap3.0 均为 setPosition(Point)，此处做存在性兜底。
 */
export function moveMarker(marker: any, point: any) {
  if (marker && typeof marker.setPosition === "function") {
    marker.setPosition(point);
  }
}
