package com.itheima.pinda.enums;

/**
 * 异常上报类型枚举（移动三端 · D-41 快递员异常上报 / D-57 司机在途异常上报）
 *
 * <p>用于标记"人工上报"的异常类别，与 GPS 自动检测告警
 * （{@code SPEED_OVER / STAY_TOO_LONG / DEVIATE_ROUTE}）区分：
 * 那些由系统在轨迹消费链路上自动检测，本枚举是<b>由一线人员在 App 上主动上报</b>。</p>
 *
 * <p>落库字段：{@code pd_alarm_record.alarm_type}，取值沿用大写下划线风格，
 * 与该列已有的自动告警类型可共存（列表查询时按 type 过滤即可）。</p>
 */
public enum ExceptionType {

    /** 货物破损（快递员上报 · D-41） */
    GOODS_DAMAGED("货物破损"),

    /** 客户拒收（快递员上报；妥投时也可由 delivered status=0 触发） */
    REJECTED("客户拒收"),

    /** 地址错误/无法联系（快递员上报） */
    ADDRESS_ERROR("地址错误"),

    /** 车辆故障（在途上报 · D-57） */
    VEHICLE_BREAKDOWN("车辆故障"),

    /** 货物损失（在途上报 · D-57） */
    CARGO_LOSS("货物损失"),

    /** 延误（在途上报 · D-57） */
    DELAY("延误"),

    /** 其他异常（在途上报兜底类型 · D-57） */
    OTHER("其他异常");

    private final String description;

    ExceptionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 字符串转枚举（不区分大小写），不匹配返回 null
     *
     * @param code 类型代码
     * @return 匹配的类型，未匹配返回 null
     */
    public static ExceptionType of(String code) {
        if (code == null) {
            return null;
        }
        for (ExceptionType t : values()) {
            if (t.name().equalsIgnoreCase(code.trim())) {
                return t;
            }
        }
        return null;
    }

    /**
     * 校验类型代码是否合法
     */
    public static boolean isValid(String code) {
        return of(code) != null;
    }

    /**
     * 判断是否为快递员侧可上报的类型（配送环节）
     *
     * @param code 类型代码
     * @return true-属于快递员侧类型
     */
    public static boolean isCourierScoped(String code) {
        ExceptionType t = of(code);
        return t == GOODS_DAMAGED || t == REJECTED || t == ADDRESS_ERROR;
    }
}