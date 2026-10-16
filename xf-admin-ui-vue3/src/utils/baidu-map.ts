/**
 * 百度地图 JS API GL 动态加载器
 *
 * AK 由 BFF /api/manager/map-config-manager 下发（不进前端环境变量/仓库）；
 * 脚本按需加载一次，全局单例复用。BMapGL 无官方 TS 类型，统一 any。
 */
let loadPromise: Promise<any> | null = null;

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
