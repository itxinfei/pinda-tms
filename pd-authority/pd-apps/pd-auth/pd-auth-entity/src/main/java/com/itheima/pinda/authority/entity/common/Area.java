package com.itheima.pinda.authority.entity.common;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itheima.pinda.base.entity.Entity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

import static com.baomidou.mybatisplus.annotation.SqlCondition.LIKE;

/**
 * <p>
 * 实体类
 * 行政区域
 * </p>
 *
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("pd_area")
@ApiModel(value = "Area", description = "行政区域")
public class Area extends Entity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 父级行政区域id
     */
    @ApiModelProperty(value = "父级区域id")
    @TableField("parent_id")
    private Long parentId;

    /**
     * 行政区域名称
     */
    @ApiModelProperty(value = "行政区域名称")
    @Length(max = 255, message = "行政区域名称长度不能超过255")
    @TableField(value = "name", condition = LIKE)
    private String name;

    /**
     * 行政区域编码
     */
    @ApiModelProperty(value = "行政区域编码")
    @Length(max = 255, message = "行政区域编码长度不能超过255")
    @TableField(value = "area_code", condition = LIKE)
    private String areaCode;

    /**
     * 城市编码
     */
    @ApiModelProperty(value = "城市编码")
    @Length(max = 255, message = "城市编码长度不能超过255")
    @TableField(value = "city_code", condition = LIKE)
    private String cityCode;

    /**
     * 合并名称
     */
    @ApiModelProperty(value = "合并名称")
    @Length(max = 255, message = "合并名称长度不能超过255")
    @TableField(value = "merger_name", condition = LIKE)
    private String mergerName;

    /**
     * 简称
     */
    @ApiModelProperty(value = "简称")
    @Length(max = 255, message = "简称长度不能超过255")
    @TableField(value = "short_name", condition = LIKE)
    private String shortName;

    /**
     * 邮政编码
     */
    @ApiModelProperty(value = "邮政编码")
    @Length(max = 255, message = "邮政编码长度不能超过255")
    @TableField(value = "zip_code", condition = LIKE)
    private String zipCode;

    /**
     * 行政区域等级（0: 省级 1:市级 2:县级 3:镇级 4:乡村级）
     */
    @ApiModelProperty(value = "行政区域等级（0: 省级 1:市级 2:县级 3:镇级 4:乡村级）")
    @TableField("level")
    private Integer level;

    /**
     * 经度
     */
    @ApiModelProperty(value = "经度")
    @Length(max = 255, message = "经度长度不能超过255")
    @TableField(value = "lng", condition = LIKE)
    private String lng;

    /**
     * 纬度
     */
    @ApiModelProperty(value = "纬度")
    @Length(max = 255, message = "纬度长度不能超过255")
    @TableField(value = "lat", condition = LIKE)
    private String lat;

    /**
     * 拼音
     */
    @ApiModelProperty(value = "拼音")
    @Length(max = 255, message = "拼音长度不能超过255")
    @TableField(value = "pinyin", condition = LIKE)
    private String pinyin;

    /**
     * 首字母
     */
    @ApiModelProperty(value = "首字母")
    @Length(max = 255, message = "首字母长度不能超过255")
    @TableField(value = "first", condition = LIKE)
    private String first;

    @Builder
    public Area(Long id, Long parentId, String name, String areaCode,
                String cityCode, String mergerName, String shortName,
                String zipCode, Integer level, String lng, String lat, String pinyin, String first) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.areaCode = areaCode;
        this.cityCode = cityCode;
        this.mergerName = mergerName;
        this.shortName = shortName;
        this.zipCode = zipCode;
        this.level = level;
        this.lng = lng;
        this.lat = lat;
        this.pinyin = pinyin;
        this.first = first;
    }
}
