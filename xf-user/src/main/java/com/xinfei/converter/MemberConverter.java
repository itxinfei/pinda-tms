package com.xinfei.converter;

import com.xinfei.DTO.MemberDTO;
import com.xinfei.controller.support.IdConverter;
import com.xinfei.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 会员实体与 DTO 转换器（MapStruct 编译期生成）。
 *
 * <p>对外 DTO 的 id 保持 String（雪花精度铁律）；nickname ↔ name、
 * idCardVerify ↔ idCardNoVerify 为改名映射。tenant_id 不进 DTO。</p>
 */
@Mapper(componentModel = "spring")
public interface MemberConverter {

    @Mapping(source = "nickname", target = "name")
    @Mapping(source = "idCardVerify", target = "idCardNoVerify")
    MemberDTO toDto(Member member);

    List<MemberDTO> toDtoList(List<Member> memberList);

    /** DTO 转实体（status/createTime 由业务逻辑写入，审计字段由 MyBatis-Plus 自动填充） */
    @Mapping(source = "name", target = "nickname")
    @Mapping(source = "idCardNoVerify", target = "idCardVerify")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Member toEntity(MemberDTO memberDTO);

    /** 雪花 ID：Long 实体 → String DTO */
    default String mapId(Long id) {
        return IdConverter.toStr(id);
    }

    /** 雪花 ID：String DTO → Long 实体（非法值返回 null，不抛异常） */
    default Long mapId(String id) {
        return IdConverter.toLong(id);
    }
}
