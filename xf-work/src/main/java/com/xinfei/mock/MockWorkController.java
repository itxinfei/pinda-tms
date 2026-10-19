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
 * mock 作业数据生成端点（dev/test 专用，装配守门见 {@link MockGuardCondition}）。
 *
 * <p>手动触发（无任何定时任务，须在 xf-oms 生成订单之后调）：
 * <pre>
 * # 为最近 30 条 AUTO-MOCK 订单补运单/干线任务/作业单/取派件（最多 5 个干线任务）
 * docker exec xf-work curl -s -X POST 'localhost:8187/mock/generate?orderLimit=30&taskLimit=5'
 * # 清理 AUTO-MOCK 订单关联的全部作业链数据（清理总序第二步：netty → work → oms）
 * docker exec xf-work curl -s -X DELETE 'localhost:8187/mock/all'
 * </pre>
 * 路径已加入 SameTokenConfigure 放行清单（生产 Bean 不装配即 404，放行无攻击面）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/mock")
@RequiredArgsConstructor
@Conditional(MockGuardCondition.class)
public class MockWorkController {

    /** 取单/任务上限（防手滑造爆 dev 库） */
    private static final int MAX_ORDER_LIMIT = 100;
    private static final int MAX_TASK_LIMIT = 20;

    private final MockWorkService mockWorkService;

    @PostMapping("/generate")
    public Result generate(
            @RequestParam(defaultValue = "30") Integer orderLimit,
            @RequestParam(defaultValue = "5") Integer taskLimit) {
        if (orderLimit < 1 || orderLimit > MAX_ORDER_LIMIT) {
            return Result.error("orderLimit 须在 1~" + MAX_ORDER_LIMIT + " 之间");
        }
        if (taskLimit < 1 || taskLimit > MAX_TASK_LIMIT) {
            return Result.error("taskLimit 须在 1~" + MAX_TASK_LIMIT + " 之间");
        }
        try {
            return Result.ok(mockWorkService.generate(orderLimit, taskLimit));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/all")
    public Result cleanup() {
        return Result.ok(mockWorkService.cleanup());
    }
}
