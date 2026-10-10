import request from "@/utils/request";

const AREA_BASE_URL = "/api/auth/area";

/** 行政区划层级：0省 1市 2县 3镇 4乡村 */
export type AreaLevel = 0 | 1 | 2 | 3 | 4;

/** 行政区划（国标，全局共享） */
export interface Area {
  id: number;
  parentId: number;
  name: string;
  areaCode?: string;
  cityCode?: string;
  mergerName?: string;
  shortName?: string;
  zipCode?: string;
  level: AreaLevel;
  lng?: number;
  lat?: number;
  pinyin?: string;
  first?: string;
}

const AreaAPI = {
  /** 查询直属下级区划（parentId 空时返回省级） */
  children(parentId?: number) {
    return request<unknown, Area[]>({
      url: `${AREA_BASE_URL}/children`,
      method: "get",
      params: parentId != null ? { parentId } : {},
    });
  },
  /** 按名称模糊搜索（后端限 20 条） */
  search(name: string) {
    return request<unknown, Area[]>({
      url: `${AREA_BASE_URL}/search`,
      method: "get",
      params: { name },
    });
  },
  /** 区划详情 */
  detail(id: number) {
    return request<unknown, Area>({
      url: `${AREA_BASE_URL}/detail`,
      method: "get",
      params: { id },
    });
  },
};

export default AreaAPI;
