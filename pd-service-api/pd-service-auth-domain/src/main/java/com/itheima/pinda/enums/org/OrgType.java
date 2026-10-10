package com.itheima.pinda.enums.org;

/**
 * 组织类型枚举：口径与数据库注释一致（1分公司 2一级转运中心 3二级转运中心 4网点）。
 */
public enum OrgType {

    BRANCH_OFFICE(1, "分公司"),
    TOP_TRANSFER_CENTER(2, "一级转运中心"),
    SECONDARY_TRANSFER_CENTER(3, "二级转运中心"),
    BUSINESS_HALL(4, "网点");

    private final Integer type;
    private final String name;

    OrgType(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * 按类型码获取枚举，无匹配返回 null
     */
    public static OrgType getEnumByType(Integer type) {
        if (type == null) {
            return null;
        }
        for (OrgType item : OrgType.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }
}
