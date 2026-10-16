package com.xinfei.auth.service.v1;

import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.CacheKeyVO;
import com.xinfei.auth.common.v1.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存管理（USR-03）：Redis 键 SCAN 查看 + 单键删除。
 *
 * <p>dev 运维辅助页：单次扫描上限 {@value #SCAN_LIMIT} 个键（超出即截断，
 * 提示缩小 pattern），不做全库 KEYS 阻塞式扫描。删除操作由前端二次确认，
 * 审计留痕随操作日志拦截器（OperLogInterceptor）全局覆盖，本服务不重复记录。</p>
 */
@Service
@RequiredArgsConstructor
public class CacheService {

    /** 单次扫描上限：dev 数据量级小；达到上限即截断，引导用更精确的 pattern */
    private static final int SCAN_LIMIT = 2000;

    private final StringRedisTemplate redisTemplate;

    public PageResult<CacheKeyVO> page(long pageNum, long pageSize, String pattern) {
        String match = StringUtils.hasText(pattern) ? pattern.trim() : "*";
        List<CacheKeyVO> all = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(match).count(200).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                if (all.size() >= SCAN_LIMIT) {
                    break;
                }
                String key = cursor.next();
                CacheKeyVO vo = new CacheKeyVO();
                vo.setKey(key);
                vo.setType(redisTemplate.type(key).name().toLowerCase());
                vo.setTtl(redisTemplate.getExpire(key));
                all.add(vo);
            }
        }

        PageResult<CacheKeyVO> result = new PageResult<>();
        result.setTotal(all.size());
        int from = (int) Math.min((pageNum - 1) * pageSize, all.size());
        int to = (int) Math.min(from + pageSize, all.size());
        result.setList(all.subList(from, to));
        return result;
    }

    /** 删除单键（不可恢复；satoken:* 为登录会话键，删除即下线对应会话） */
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            throw BizException.validFail("缺少 key");
        }
        Boolean deleted = redisTemplate.delete(key);
        if (!Boolean.TRUE.equals(deleted)) {
            throw BizException.validFail("键不存在或已过期");
        }
    }
}
