package com.xinfei.auth.service.v1;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.session.SaSession;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.OnlineVO;
import com.xinfei.auth.common.v1.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 在线用户（Sa-Token Redis 会话）：枚举全部有效 token → TokenSession 富化，
 * 内存过滤分页（dev 会话量级小；并发踢人后列表下次查询自然收敛）。
 */
@Service
@RequiredArgsConstructor
public class OnlineService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PageResult<OnlineVO> page(long pageNum, long pageSize, String keywords) {
        List<OnlineVO> all = listAll(keywords);
        // 关键字匹配 account/name/tenantCode（不匹配 token，防撞库式探测）
        PageResult<OnlineVO> result = new PageResult<>();
        result.setTotal(all.size());
        int from = (int) Math.min((pageNum - 1) * pageSize, all.size());
        int to = (int) Math.min(from + pageSize, all.size());
        result.setList(all.subList(from, to));
        return result;
    }

    /** 强制下线：Sa-Token kickout 保留登出痕迹（token 标记已踢，再次请求即失效） */
    public void kickout(String token) {
        if (!StringUtils.hasText(token)) {
            throw BizException.validFail("缺少 token");
        }
        if (token.equals(StpUtil.getTokenValue())) {
            throw BizException.validFail("不能强制下线当前登录会话");
        }
        if (StpUtil.getLoginIdByToken(token) == null) {
            throw BizException.validFail("会话不存在或已失效");
        }
        StpUtil.kickout(token);
    }

    private List<OnlineVO> listAll(String keywords) {
        // key 形如 <prefix>login:token:<value>，取末段即 tokenValue
        List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
        List<OnlineVO> list = new ArrayList<>();
        for (String key : keys) {
            String token = key.substring(key.lastIndexOf(':') + 1);
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                continue;
            }
            SaSession session = StpUtil.getTokenSessionByToken(token);
            OnlineVO vo = new OnlineVO();
            vo.setToken(token);
            vo.setTokenMask(token.length() <= 8 ? token + "…" : token.substring(0, 8) + "…");
            vo.setUserId(Long.parseLong(String.valueOf(loginId)));
            vo.setAccount(str(session.get("account")));
            vo.setName(str(session.get("name")));
            vo.setTenantCode(str(session.get("tenantCode")));
            // SaSession 创建时间即登录（token 签发）时间
            vo.setLoginTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(session.getCreateTime()),
                    java.time.ZoneId.systemDefault()).format(FMT));
            if (StringUtils.hasText(keywords)) {
                String kw = keywords.trim();
                boolean hit = contains(vo.getAccount(), kw) || contains(vo.getName(), kw)
                        || contains(vo.getTenantCode(), kw);
                if (!hit) {
                    continue;
                }
            }
            list.add(vo);
        }
        // 按 userId 排序让同账号相邻（token 签发时间相同的多端会话聚在一处）
        list.sort((a, b) -> Long.compare(a.getUserId(), b.getUserId()));
        return list;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
