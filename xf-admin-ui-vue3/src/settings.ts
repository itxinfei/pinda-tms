// 应用配置

import {
  LayoutMode,
  ComponentSize,
  SidebarColor,
  ThemeMode,
  LanguageEnum,
  TagsViewStyle,
} from "@/enums";

const env = import.meta.env;
const { pkg } = __APP_INFO__;
const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;

export const themeColorNames = ["primary", "success", "warning", "danger", "info"] as const;

export type ThemeColorName = (typeof themeColorNames)[number];

export type ThemeColorMap = Record<ThemeColorName, string>;

export interface ThemePalettePreset {
  id: string;
  name: string;
  description: string;
  colors: ThemeColorMap;
}

export const themePalettePresets = [
  {
    id: "arco",
    name: "心飞红",
    description: "品牌心飞红，默认推荐",
    colors: {
      primary: "#FF0033",
      success: "#00B42A",
      warning: "#FF7D00",
      danger: "#F53F3F",
      info: "#86909C",
    },
  },
  {
    id: "ant-design",
    name: "AntD 蓝",
    description: "规范稳重，适合标准业务系统",
    colors: {
      primary: "#1677FF",
      success: "#52C41A",
      warning: "#FAAD14",
      danger: "#FF4D4F",
      info: "#1677FF",
    },
  },
  {
    id: "element-plus",
    name: "Element 蓝",
    description: "贴近组件默认色识别",
    colors: {
      primary: "#409EFF",
      success: "#67C23A",
      warning: "#E6A23C",
      danger: "#F56C6C",
      info: "#909399",
    },
  },
  {
    id: "emerald",
    name: "翡翠绿",
    description: "翡翠绿，清爽护眼，适合日常运营",
    colors: {
      primary: "#13A970",
      success: "#00B42A",
      warning: "#FF9A2E",
      danger: "#F53F3F",
      info: "#8D95A3",
    },
  },
  {
    id: "violet",
    name: "优雅紫",
    description: "优雅紫，个性鲜明，适合数据看板",
    colors: {
      primary: "#722ED1",
      success: "#52C41A",
      warning: "#FAAD14",
      danger: "#FF4D4F",
      info: "#86909C",
    },
  },
  {
    id: "cyan",
    name: "青瓷蓝",
    description: "青瓷蓝绿，冷静专业，适合监控场景",
    colors: {
      primary: "#0FB5BA",
      success: "#23C343",
      warning: "#FF7D00",
      danger: "#F76560",
      info: "#909399",
    },
  },
  {
    id: "orange",
    name: "活力橙",
    description: "活力暖橙，醒目亲和，适合业务一线",
    colors: {
      primary: "#FF7D00",
      success: "#00B42A",
      warning: "#FFB400",
      danger: "#F53F3F",
      info: "#86909C",
    },
  },
  {
    id: "slate",
    name: "石墨灰",
    description: "石墨灰蓝，低调沉稳，适合长时间使用",
    colors: {
      primary: "#4E5969",
      success: "#00B42A",
      warning: "#FF7D00",
      danger: "#F53F3F",
      info: "#8D95A3",
    },
  },
] as const satisfies readonly ThemePalettePreset[];

export const defaultThemePalette = themePalettePresets[0];

/**
 * 解析布尔环境变量，仅 "true" 视为开启，未设置或为空时取默认值
 *
 * @param value 环境变量原始值
 * @param defaultValue 未设置时的取值
 */
function envBool(value: string | undefined, defaultValue: boolean): boolean {
  return value == null || value === "" ? defaultValue : value === "true";
}

export const appConfig = {
  name: pkg.name as string,
  version: pkg.version as string,
  title: (env.VITE_APP_TITLE as string) || pkg.name,

  // 功能开关（缺省均关闭，需要时在 .env 中显式开启）
  tenantEnabled: envBool(env.VITE_TENANT_ENABLED, false),
  aiEnabled: envBool(env.VITE_AI_ENABLED, false),

  // D-42 用户信息端点（/api/v1/users/me）已落地（xf-auth v1，已实测返回
  // roles/perms）。置 true：守卫拉取真实身份驱动 v-hasPerm；失败仍降级临时身份。
  userInfoEndpointEnabled: true,
} as const;

export const defaults = {
  theme: prefersDark ? ThemeMode.DARK : ThemeMode.LIGHT,
  themePalette: defaultThemePalette.id,
  themeColors: { ...defaultThemePalette.colors },
  sidebarColorScheme: SidebarColor.MINIMAL_WHITE,
  layout: LayoutMode.LEFT,
  size: ComponentSize.DEFAULT,
  language: LanguageEnum.ZH_CN,
  showTagsView: true,
  tagsViewStyle: TagsViewStyle.CARD,
  showAppLogo: true,
  showWatermark: false,
  pageSwitchingAnimation: "fade-slide",
  showSettings: true,
  watermarkContent: pkg.name,
} as const;
