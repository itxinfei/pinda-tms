import type { OptionItem } from "@/api/common";
import request from "@/utils/request";

const ORG_BASE_URL = "/api/auth/org";

/** 组织类型：1分公司 2一级转运中心 3二级转运中心 4网点 */
export type OrgType = 1 | 2 | 3 | 4;

/** 组织节点（树形） */
export interface OrgNode {
  id: number;
  tenantId?: number;
  name: string;
  abbreviation?: string;
  parentId: number;
  orgType: OrgType;
  provinceId?: number;
  cityId?: number;
  countyId?: number;
  address?: string;
  contractNumber?: string;
  managerId?: number;
  treePath?: string;
  sortValue?: number;
  /** 状态：0停用 1启用（对齐后端 CoreOrg Integer，见 CommonStatus） */
  status: number;
  /** 简介（后端字段名 description） */
  description?: string;
  latitude?: number;
  longitude?: number;
  businessHours?: string;
  children?: OrgNode[];
  createTime?: string;
}

/** 组织表单提交体 */
export type OrgForm = Partial<OrgNode> & {
  name: string;
  parentId: number;
  orgType: OrgType;
};

/** 组织树 → 通用下拉选项树（{value,label,children}，供 el-tree-select/el-cascader 用） */
export function orgTreeToOptions(nodes: OrgNode[]): OptionItem[] {
  return (nodes ?? []).map((n) => ({
    value: String(n.id),
    label: n.name,
    children: n.children?.length ? orgTreeToOptions(n.children) : undefined,
  }));
}

const OrgAPI = {
  /** 组织树 */
  getTree() {
    return request<unknown, OrgNode[]>({ url: `${ORG_BASE_URL}/tree`, method: "get" });
  },
  /** 组织下拉选项树（替代 youlai 的 /api/v1/depts/options） */
  async getOptions(): Promise<OptionItem[]> {
    const tree = await this.getTree();
    return orgTreeToOptions(tree);
  },
  /** 组织详情 */
  getDetail(id: number) {
    return request<unknown, OrgNode>({
      url: `${ORG_BASE_URL}/detail`,
      method: "get",
      params: { id },
    });
  },
  /** 新增组织，返回新组织ID */
  create(data: OrgForm) {
    return request<unknown, number>({ url: `${ORG_BASE_URL}`, method: "post", data });
  },
  /** 修改组织 */
  update(data: OrgForm) {
    return request({ url: `${ORG_BASE_URL}`, method: "put", data });
  },
  /** 删除组织（存在下级时后端拒绝） */
  remove(id: number) {
    return request({ url: `${ORG_BASE_URL}`, method: "delete", params: { id } });
  },
};

export default OrgAPI;
