package com.xinfei.mock;

import com.xinfei.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * mock 订单数据生成端点（dev/test 专用，装配守门见 {@link MockGuardCondition}）。
 *
 * <p>手动触发（无任何定时任务）：
 * <pre>
 * # 生成 20 条（近 7 天分布，当天占一半）
 * docker exec xfwl-oms curl -s -X POST 'localhost:8186/mock/generate?count=20'
 * # 清理全部 AUTO-MOCK 订单域数据（先 netty → work 再 oms，逆依赖序）
 * docker exec xfwl-oms curl -s -X DELETE 'localhost:8186/mock/orders'
 * </pre>
 * 路径已加入 SameTokenConfigure 放行清单（生产 Bean 不装配即 404，放行无攻击面）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/mock")
@RequiredArgsConstructor
@Conditional(MockGuardCondition.class)
public class MockOmsController {

    /** 单次生成上限（防手滑造爆 dev 库） */
    private static final int MAX_COUNT = 200;

    private final MockOmsService mockOmsService;

    @PostMapping("/generate")
    public Result generate(
            @RequestParam(defaultValue = "20") Integer count,
            @RequestParam(defaultValue = "7") Integer daysBack) {
        if (count < 1 || count > MAX_COUNT) {
            return Result.error("count 须在 1~" + MAX_COUNT + " 之间");
        }
        if (daysBack < 0 || daysBack > 30) {
            return Result.error("daysBack 须在 0~30 之间");
        }
        try {
            return Result.ok(mockOmsService.generate(count, daysBack));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/orders")
    public Result cleanup() {
        return Result.ok(mockOmsService.cleanup());
    }
}
