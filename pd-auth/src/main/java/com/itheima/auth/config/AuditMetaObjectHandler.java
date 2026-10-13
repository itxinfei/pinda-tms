package com.itheima.auth.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充。
 *
 * <p>对实体标注了 {@code @TableField(fill=INSERT/INSERT_UPDATE)} 的 createBy/createTime/
 * updateBy/updateTime 生效；strictFill 仅在字段为 null 时填，Service 已显式赋值不覆盖。
 * 内部 Feign（/internal）无登录态时操作人置 null（不打断写入），时间戳照常。</p>
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_BY = "createBy";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_BY = "updateBy";
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
        Long userId = currentUserId();
        if (userId != null) {
            strictInsertFill(metaObject, CREATE_BY, Long.class, userId);
            strictInsertFill(metaObject, UPDATE_BY, Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        Long userId = currentUserId();
        if (userId != null) {
            strictUpdateFill(metaObject, UPDATE_BY, Long.class, userId);
        }
    }

    /** 取当前登录用户ID；无登录态（内部 Feign/未登录）返回 null */
    private Long currentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignore) {
            // 无会话上下文
        }
        return null;
    }
}
