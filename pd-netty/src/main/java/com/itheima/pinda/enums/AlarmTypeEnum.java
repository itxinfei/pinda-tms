package com.itheima.pinda.enums;

/**
 * GPS/人工告警类型枚举（iot_alarm_record.alarm_type 数字码唯一真源）
 *
 * <p>新表 {@code alarm_type} 为 TINYINT，检测链路内部仍沿用大写下划线语义码
 * （可读、不改动既有检测逻辑），仅在落库前经本枚举转换为数字码。</p>
 *
 * <p>分段约定：
 * <ul>
 *   <li>1-9：GPS 自动检测告警（1超速 2长时停留 3偏离路线 4车辆离线）；</li>
 *   <li>10-19：一线人员主动上报（货物破损/客户拒收/地址错误/车辆故障/货物损失/延误/其他）。</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-10-10
 */
public enum AlarmTypeEnum {

    /** GPS：超速 */
    SPEED_OVER(1, "超速"),

    /** GPS：长时间停留 */
    STAY_TOO_LONG(2, "长时间停留"),

    /** GPS：偏离路线 */
    DEVIATE_ROUTE(3, "偏离路线"),

    /** GPS：心跳超时车辆离线 */
    VEHICLE_OFFLINE(4, "车辆离线"),

    /** 人工：货物破损 */
    GOODS_DAMAGED(10, "货物破损"),

    /** 人工：客户拒收 */
    REJECTED(11, "客户拒收"),

    /** 人工：地址错误/无法联系 */
    ADDRESS_ERROR(12, "地址错误"),

    /** 人工：车辆故障 */
    VEHICLE_BREAKDOWN(13, "车辆故障"),

    /** 人工：货物损失 */
    CARGO_LOSS(14, "货物损失"),

    /** 人工：延误 */
    DELAY(15, "延误"),

    /** 人工：其他异常 */
    OTHER(16, "其他异常");

    private final int code;
    private final String description;

    AlarmTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 语义码（枚举名）转枚举，不区分大小写，不匹配返回 null
     */
    public static AlarmTypeEnum ofKey(String key) {
        if (key == null) {
            return null;
        }
        for (AlarmTypeEnum item : values()) {
            if (item.name().equalsIgnoreCase(key.trim())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 语义码 → 数字码，空白或不匹配时返回 null
     * （落库列 NOT NULL，调用方对 null 应有降级）
     */
    public static Integer codeOfKey(String key) {
        AlarmTypeEnum item = ofKey(key);
        return item == null ? null : item.code;
    }
}
