package com.itheima.auth.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.auth.entity.CoreOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 组织 Mapper。走租户插件自动隔离。
 */
@Mapper
public interface CoreOrgMapper extends BaseMapper<CoreOrg> {

    /**
     * 按组织ID反查其租户ID。绕过租户插件（系统态定时任务尚不知道租户时使用），
     * 仅暴露 tenant_id 单值，端点受 Same-Token 保护，不泄露其它租户数据。
     */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT tenant_id FROM sys_org WHERE id = #{orgId} LIMIT 1")
    Long selectTenantIdByOrgId(@Param("orgId") Long orgId);

    /**
     * 分页模糊查询附近组织：按与传入经纬度的距离（公里）升序。
     */
    @Select("""
            <script>
            SELECT *,(ST_DISTANCE(POINT(CAST(latitude AS DECIMAL(10,6)),CAST(longitude AS DECIMAL(10,6))),
            POINT(CAST(#{latitude} AS DECIMAL(10,6)),CAST(#{longitude} AS DECIMAL(10,6))))*111195/1000) AS jl
            FROM sys_org
            <where>
                <if test="cityId != null"> AND city_id = #{cityId} </if>
                <if test="keyword != null">
                    AND (address LIKE CONCAT('%',#{keyword},'%')
                      OR name LIKE CONCAT('%',#{keyword},'%')
                      OR abbreviation LIKE CONCAT('%',#{keyword},'%')
                      OR contract_number LIKE CONCAT('%',#{keyword},'%'))
                </if>
            </where>
            ORDER BY jl ASC
            </script>
            """)
    IPage<CoreOrg> pageLike(Page<CoreOrg> page,
                            @Param("cityId") Long cityId,
                            @Param("keyword") String keyword,
                            @Param("latitude") String latitude,
                            @Param("longitude") String longitude);
}
