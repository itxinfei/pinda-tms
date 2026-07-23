package com.itheima.pinda.common.base;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 状态枚举基类
 *
 * <p>提供统一的 code/value 存储、lookup 查找能力，
 * 消除各状态枚举类中重复的 LOOKUP Map 与 lookup() 方法代码。</p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * public enum OrderStatus implements BaseStatusEnum<Integer, String> {
 *     PENDING(23000, "PENDING"),
 *     RECEIVED(23009, "RECEIVED");
 *
 *     private final Integer code;
 *     private final String value;
 *
 *     // 静态初始化（枚举内部，无需额外 LOOKUP）
 *     OrderStatus(Integer code, String value) {
 *         this.code = code;
 *         this.value = value;
 *     }
 *
 *     // 直接使用基类的 lookup() 方法，无需重复定义
 * }
 * }</pre>
 *
 * @param <K> 编码类型（通常为 Integer 或 String）
 * @param <V> 值类型（通常为 String）
 */
public interface BaseStatusEnum<K, V> {

    /**
     * 获取编码值
     *
     * @return 状态编码
     */
    K getCode();

    /**
     * 获取描述值
     *
     * @return 状态描述
     */
    V getValue();

    /**
     * 基类查找器 — 每个枚举实例自动注册
     */
    abstract class Lookup<K, V, E extends Enum<E> & BaseStatusEnum<K, V>> {

        private final Map<K, E> lookup = new HashMap<>();

        protected Lookup(Class<E> enumClass) {
            for (E e : EnumSet.allOf(enumClass)) {
                lookup.put(e.getCode(), e);
            }
        }

        /**
         * 根据 code 查找枚举值
         *
         * @param code 编码
         * @return 匹配的枚举值，未找到返回 null
         */
        @SuppressWarnings("unchecked")
        public E lookup(K code) {
            if (code == null) {
                return null;
            }
            return lookup.get(code);
        }

        /**
         * 获取所有 code 的集合（用于校验等场景）
         */
        public Map<K, E> getAll() {
            return new HashMap<>(lookup);
        }
    }
}
