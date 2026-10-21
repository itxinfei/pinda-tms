package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.SysNoticeRead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知-已读 Mapper。
 */
@Mapper
public interface NoticeReadMapper extends BaseMapper<SysNoticeRead> {
}