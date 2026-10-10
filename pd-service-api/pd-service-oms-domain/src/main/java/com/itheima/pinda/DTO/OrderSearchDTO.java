package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "订单模糊搜索参数")
@Data
public class OrderSearchDTO extends OrderDTO {

    /**
     * 页码
     */
    @Schema(description = "页码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer page;


    /**
     * 总页数
     */
    @Schema(description = "总页数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer pageSize;

    /**
     * 订单编号
     */
    @Schema(description = "订单编号")
    private String orderId;

    /**
     * 发件人姓名
     */
    @Schema(description = "发件人姓名")
    private String senderName;

    /**
     * 发件人电话
     */
    @Schema(description = "发件人电话")
    private String senderPhone;

    /**
     * 发件人所在省份Id
     */
    @Schema(description = "发件人所在省份Id")
    private String senderProvinceId;


    /**
     * 发件人所在市Id
     */
    @Schema(description = "发件人所在市Id")
    private String senderCityId;

    /**
     * 发件人所在区Id 集合
     */
    @Schema(description = "发件人所在区Id集合")
    private List<String> senderCountyIds;

    /**
     * 收件人姓名
     */
    @Schema(description = "收件人姓名")
    private String receiverName;

    /**
     * 收件人电话
     */
    @Schema(description = "收件人电话")
    private String receiverPhone;


    /**
     * 收件人所在省份Id
     */
    @Schema(description = "收件人所在省份Id")
    private String receiverProvinceId;


    /**
     * 收件人所在市Id
     */
    @Schema(description = "收件人所在市Id")
    private String receiverCityId;

    /**
     * 收件人所在区Id 集合
     */
    @Schema(description = "收件人所在区Id集合")
    private List<String> receiverCountyIds;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer orderStatus;

    /**
     * 客户id
     */
    @Schema(description = "客户id")
    private String memberId;

    /**
     * 公用搜索字段
     */
    @Schema(description = "公用搜索字段")
    private String keyword;

    public Integer getPage() {
        return page == null ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null ? 10 : pageSize;
    }

}
