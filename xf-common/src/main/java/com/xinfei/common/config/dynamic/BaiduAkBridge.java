package com.xinfei.common.config.dynamic;

import com.xinfei.common.utils.EntCoordSyncJob;
import com.xinfei.common.utils.LocationUtil;

import java.util.function.Supplier;

/**
 * 百度地图 AK 动态桥（CFG-01）：把注入的 AK 读取链路接到仍以静态工具形态暴露的
 * {@link EntCoordSyncJob} 与 {@link LocationUtil}，使调度分类、机构坐标同步等
 * 老调用点同样做到「AK 存库、后台可改、不重启生效」。
 *
 * <p>自动装配默认注入「参数库 sys_config → Spring Environment(-D/环境变量)」链路；
 * 无数据源的服务（如 BFF）可注册同名 Bean {@code baiduAkBridge} 覆盖为
 * 「ConfigFeign 读 xf-auth」链路（自动装配带 {@code @ConditionalOnMissingBean}，用户配置优先）。</p>
 */
public class BaiduAkBridge {

    /** 百度地图 AK 的参数配置键名（sys_config / -D / 环境变量 同键） */
    public static final String CONFIG_KEY_BAIDU_AK = "baidu.map.ak";

    public BaiduAkBridge(Supplier<String> akSupplier) {
        EntCoordSyncJob.setAkSupplier(akSupplier);
        LocationUtil.setAkSupplier(akSupplier);
    }
}
