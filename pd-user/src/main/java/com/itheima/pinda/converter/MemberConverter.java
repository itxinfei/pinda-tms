package com.itheima.pinda.converter;

import com.itheima.pinda.DTO.MemberDTO;
import com.itheima.pinda.entity.Member;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 会员实体与 DTO 转换器（MapStruct 编译期生成）。
 * 实体仅 5 个字段，DTO 额外携带 avatar/name 展示字段，toEntity 时自动忽略。
 */
@Mapper(componentModel = "spring")
public interface MemberConverter {

    MemberDTO toDto(Member member);

    List<MemberDTO> toDtoList(List<Member> memberList);

    Member toEntity(MemberDTO memberDTO);
}
