package com.itheima.pinda.common.utils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 常量
 */
public class Constant {
    /**
     * 超级管理员ID
     */
    public static final int SUPER_ADMIN = 1;
    /**
     * 当前页码
     */
    public static final String PAGE = "page";
    /**
     * 每页显示记录数
     */
    public static final String LIMIT = "limit";
    /**
     * 排序字段
     */
    public static final String ORDER_FIELD = "sidx";
    /**
     * 排序方式
     */
    public static final String ORDER = "order";
    /**
     * 升序
     */
    public static final String ASC = "asc";

    /**
     * 数据状态-可用
     */
    public static final Integer DATA_DEFAULT_STATUS = 1;
    /**
     * 数据状态-禁用
     */
    public static final Integer DATA_DISABLE_STATUS = 0;
    /**
     * 数据删除状态-正常
     */
    public static final Integer DATA_DEFAULT_DELETE = 1;
    /**
     * 数据删除状态-删除
     */
    public static final Integer DATA_DISABLE_DELETE = 0;

    /**
     * 标准时间格式
     */
    public static final String STAND_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 菜单类型
     *
     * @author chenshun
     * @email sunlightcs@gmail.com
     * @date 2016年11月15日 下午1:24:29
     */
    public enum MenuType {
        /**
         * 目录
         */
        CATALOG(0),
        /**
         * 菜单
         */
        MENU(1),
        /**
         * 按钮
         */
        BUTTON(2);

        private int value;

        MenuType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 定时任务状态
     *
     * @author chenshun
     * @email sunlightcs@gmail.com
     * @date 2016年12月3日 上午12:07:22
     */
    public enum ScheduleStatus {
        /**
         * 正常
         */
        NORMAL(0),
        /**
         * 暂停
         */
        PAUSE(1);

        private int value;

        ScheduleStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 云服务商
     */
    public enum CloudService {
        /**
         * 七牛云
         */
        QINIU(1),
        /**
         * 阿里云
         */
        ALIYUN(2),
        /**
         * 腾讯云
         */
        QCLOUD(3);

        private int value;

        CloudService(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 车次周期
     */
    public enum TransportTripsPeriod {
        DAY(1, "天"), WEEK(2, "周"), MONTH(3, "月");
        private Integer period;
        private String name;

        TransportTripsPeriod(Integer period, String name) {
            this.period = period;
            this.name = name;
        }

        public Integer getPeriod() {
            return period;
        }

        public String getName() {
            return name;
        }

        public static TransportTripsPeriod getEnumByPeriod(Integer period) {
            if (null == period) {
                return null;
            }
            for (TransportTripsPeriod temp : TransportTripsPeriod.values()) {
                if (temp.getPeriod().equals(period)) {
                    return temp;
                }
            }
            return null;
        }
    }

    /**
     * 岗位
     *
     * <p><b>重要说明</b>：本枚举描述的是<b>客户端角色编码</b>（App/前端传参用的业务角色编号），
     * 与数据库中的岗位 ID（pd_core_station，见 pd-authority 的 {@code StaticStation}）是<b>两套不同的编码体系</b>：
     * <ul>
     *   <li>本枚举：PERSONNEL=1、COURIER=2、DRIVER=3（客户端角色编码）</li>
     *   <li>StaticStation：COURIER_ID=3L、DRIVER_ID=2L（数据库岗位ID）</li>
     * </ul>
     * 二者映射关系为：客户端 COURIER(2) ↔ 数据库岗位3；客户端 DRIVER(3) ↔ 数据库岗位2。
     * 使用时务必区分，切勿将本枚举值直接当作数据库岗位ID使用。</p>
     */
    public enum UserStation {
        PERSONNEL(1, "员工"), COURIER(2, "快递员"), DRIVER(3, "司机");
        private Integer station;
        private String name;

        UserStation(Integer station, String name) {
            this.station = station;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Integer getStation() {
            return station;
        }

        public static UserStation getEnumByStation(Integer station) {
            if (null == station) {
                return null;
            }
            for (UserStation temp : UserStation.values()) {
                if (temp.getStation().equals(station)) {
                    return temp;
                }
            }
            return null;
        }
    }
}
