import { defineMock } from "./base";

/** 应用数据源（内存态，支持增删改与状态切换） */
const appList: Array<Record<string, any>> = [
  {
    id: "1",
    appName: "有来商城",
    appCode: "youlai-mall",
    platform: "WECHAT_MP",
    appId: "wx5d2e1a2b3c4d5e6f7a8b",
    appSecret: "c0a8d0c0d1f4a0b1e0f5a2c3d4e5f6a7",
    merchantId: "1900000001",
    merchantKey: "MCH_KEY_123456",
    status: 1,
    remark: "有来商城公众号应用",
    createTime: "2024-01-15 10:00:00",
  },
  {
    id: "2",
    appName: "有来商城小程序",
    appCode: "youlai-mall-mini",
    platform: "WECHAT_MINI",
    appId: "wx1f2e3d4c5b6a7f8e9d0c",
    appSecret: "b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6",
    merchantId: "1900000002",
    merchantKey: "MCH_KEY_654321",
    status: 1,
    remark: "有来商城微信小程序应用",
    createTime: "2024-02-20 14:30:00",
  },
  {
    id: "3",
    appName: "有来商城App",
    appCode: "youlai-mall-app",
    platform: "APPLE",
    appId: "com.youlai.mall.app",
    appSecret: "",
    status: 0,
    remark: "iOS 端应用（已停用）",
    createTime: "2023-11-08 09:12:00",
  },
];

export default defineMock([
  // 应用分页列表
  {
    url: "apps",
    method: ["GET"],
    body({ query }) {
      const { keywords, platform, status, pageNum = "1", pageSize = "10" } = query;
      const filtered = appList.filter((item) => {
        if (keywords && !`${item.appName}${item.appCode}${item.appId}`.includes(keywords)) {
          return false;
        }
        if (platform && item.platform !== platform) return false;
        if (status && String(item.status) !== String(status)) return false;
        return true;
      });
      const page = Number(pageNum);
      const size = Number(pageSize);
      const start = (page - 1) * size;
      return {
        code: "00000",
        data: {
          list: filtered.slice(start, start + size),
          total: filtered.length,
        },
        msg: "一切ok",
      };
    },
  },

  // 获取应用表单数据
  {
    url: "apps/:id/form",
    method: ["GET"],
    body({ params }) {
      const data = appList.find((item) => item.id === params.id);
      return {
        code: "00000",
        data: data ?? null,
        msg: "一切ok",
      };
    },
  },

  // 新增应用
  {
    url: "apps",
    method: ["POST"],
    body({ body }) {
      appList.unshift({
        id: String(appList.length + 1),
        status: 1,
        createTime: "2024-06-01 10:00:00",
        ...body,
      });
      return {
        code: "00000",
        data: null,
        msg: "新增成功",
      };
    },
  },

  // 修改应用
  {
    url: "apps/:id",
    method: ["PUT"],
    body({ params, body }) {
      const index = appList.findIndex((item) => item.id === params.id);
      if (index !== -1) {
        appList[index] = { ...appList[index], ...body };
      }
      return {
        code: "00000",
        data: null,
        msg: "修改成功",
      };
    },
  },

  // 删除应用（多个 ID 以英文逗号分隔）
  {
    url: "apps/:ids",
    method: ["DELETE"],
    body({ params }) {
      const idSet = new Set(params.ids.split(","));
      for (let i = appList.length - 1; i >= 0; i--) {
        if (idSet.has(appList[i].id)) {
          appList.splice(i, 1);
        }
      }
      return {
        code: "00000",
        data: null,
        msg: "删除成功",
      };
    },
  },

  // 修改应用状态
  {
    url: "apps/:id/status",
    method: ["PUT"],
    body({ params, body }) {
      const item = appList.find((item) => item.id === params.id);
      if (item) {
        item.status = body.status;
      }
      return {
        code: "00000",
        data: null,
        msg: "状态更新成功",
      };
    },
  },
]);
