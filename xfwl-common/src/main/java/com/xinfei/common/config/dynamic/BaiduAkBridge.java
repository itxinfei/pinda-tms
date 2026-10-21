package com.xinfei.common.config.dynamic;

import com.xinfei.common.geo.BaiduGeoClient;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 百度地图 AK 动态桥（CFG-01）：把注入的 AK 读取链路接到 {@link BaiduGeoClient}
 * （批3 收敛后的唯一地理 HTTP 出口），做到「AK 存库、后台可改、不重启生效」。
 *
 * <p>自动装配默认注入「参数库 sys_config → Spring Environment(-D/环境变量)」链路，
 * 并在百度返回失败（status!=0）时 evict 参数库 AK 本地缓存；无数据源的服务（如 BFF）
 * 可注册同名 Bean {@code baiduAkBridge} 覆盖为「ConfigFeign 读 xfwl-auth」链路
 * （自动装配带 {@code @ConditionalOnMissingBean}，用户配置优先；Feign 链自带 30s TTL，
 * 单参构造的 evict 为 no-op 即可）。</p>
 */
public class BaiduAkBridge {

    /** 百度地图 AK 的参数配置键名（sys_config / -D / 环境变量 同键） */
    public static final String CONFIG_KEY_BAIDU_AK = "baidu.map.ak";

    /**
     * 无 evict 链路（BFF 等 Feign 读取场景，读取器自带 TTL）。
     */
    public BaiduAkBridge(Supplier<String> akSupplier) {
        this(akSupplier, null);
    }

    /**
     * 带 evict 链路（服务侧参数库场景，百度失败时立即失效 AK 本地缓存）。
     */
    public BaiduAkBridge(Supplier<String> akSupplier, Consumer<String> akEvict) {
        BaiduGeoClient.setAkReader(akSupplier, akEvict);
    }
}
