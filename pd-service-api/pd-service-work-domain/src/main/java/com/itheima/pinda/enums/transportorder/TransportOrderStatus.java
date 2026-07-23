package com.itheima.pinda.enums.transportorder;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 运单-状态
 */
public enum TransportOrderStatus implements BaseStatusEnum<Integer, String> {

    /**
     * 新建
     */
    CREATED(1, "新建"),

    /**
     * 已装车，发往x转运中心
     */
    LOADED(2, "已装车"),

    /**
     * 到达
     */
    ARRIVED(3, "到达"),
    /**
     * 到达终端网点
     */
    ARRIVED_END(4, "到达终端网点"),
    /**
     * 已签收
     */
    RECEIVED(5, "已签收"),
    /**
     * 拒收
     */
    REJECTED(6, "拒收");


    private final Integer code;

    private final String value;

    TransportOrderStatus(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * 根据 code 查找枚举项
     *
     * @param code 编码值
     * @return 匹配的枚举项，未找到返回 null
     */
    public static TransportOrderStatus lookup(Integer code) {
        if (code == null) {
            return null;
        }
        for (TransportOrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

}
