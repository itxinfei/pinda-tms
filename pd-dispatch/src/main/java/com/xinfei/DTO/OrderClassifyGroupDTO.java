package com.xinfei.DTO;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Objects;

/**
 * 订单分类分组模型（调度内部）
 *
 * <p>分组ID与机构ID为 Long；{@link #key} 为「起始机构#目的机构#当前机构」自产分组键，
 * 来源 {@link OrderClassifyDTO#groupBy()}（Long 拼串，数值完全可控，可安全拆回 Long）；
 * 订单ID集合保持 String，仅在落 disp_classify_order 时转 Long（pd-oms String 边界）。</p>
 */
@Data
@Builder
@NoArgsConstructor
public class OrderClassifyGroupDTO {
    private Long id;
    // 分组维度 key
    private String key;

    public void setKey(String key) {
        // ERROR 分组（机构信息缺失）不拆分，三机构保持 null
        if ("ERROR".equals(key)) {
            this.key = key;
            return;
        }
        String[] keys = key.split("#");
        for (int i = 0; i < keys.length; i++) {
            switch (i) {
                case 0:
                    this.startOrgId = Long.valueOf(keys[i]);
                    break;
                case 1:
                    this.endOrgId = Long.valueOf(keys[i]);
                    break;
                case 2:
                    this.currentOrgId = Long.valueOf(keys[i]);
                    break;
//                case 3:
//                    this.goodsType = keys[i];
//                    break;
                // 扩充部分
            }
        }
    }

    /**
     * 分组信息
     */
    private Long startOrgId;
    private Long endOrgId;
    private Integer orderType;
    private String goodsType;
    private Long currentOrgId;

    /**
     * 是否为新订单，如果起始机构和当前机构相同则为新订单，否则为中转订单
     * @return
     */
    public boolean isNew() {
        return Objects.equals(this.startOrgId, this.currentOrgId);
    }

    /**
     * 原订单ID集合（pd-oms String 边界）
     */
    private List<String> orders;

    public OrderClassifyGroupDTO(Long id, String key, Long startOrgId, Long endOrgId, Integer orderType, String goodsType, Long currentOrgId, List<String> orders) {
        this.id = id;
        this.key = key;
        this.startOrgId = startOrgId;
        this.endOrgId = endOrgId;
        this.goodsType = goodsType;
        this.orderType = orderType;
        this.orders = orders;
        this.currentOrgId = currentOrgId;
        setKey(this.key);
    }
}
