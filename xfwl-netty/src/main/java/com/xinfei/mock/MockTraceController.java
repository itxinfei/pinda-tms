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
 * mock 轨迹/告警数据生成端点（dev/test 专用，装配守门见 {@link MockGuardCondition}）。
 *
 * <p>手动触发（无任何定时任务，须在 xfwl-oms / xfwl-work 生成之后调）：
 * <pre>
 * # 生成当日轨迹点串 + 4 条未处理告警（每车 24 点）
 * docker exec xfwl-netty curl -s -X POST 'localhost:8192/mock/generate?pointsPerTruck=24'
 * # 清理全部 mock 轨迹与告警（清理总序第一步：netty → work → oms）
 * docker exec xfwl-netty curl -s -X DELETE 'localhost:8192/mock/all'
 * </pre>
 * 路径已加入 SaTokenConfigure 两道拦截放行清单（生产 Bean 不装配即 404，放行无攻击面）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/mock")
@RequiredArgsConstructor
@Conditional(MockGuardCondition.class)
public class MockTraceController {

    /** 单车轨迹点上限（3 车 × 50 点 = 150 点封顶，防手滑造爆） */
    private static final int MAX_POINTS = 50;

    private final MockTraceService mockTraceService;

    @PostMapping("/generate")
    public Result generate(
            @RequestParam(defaultValue = "24") Integer pointsPerTruck) {
        if (pointsPerTruck < 8 || pointsPerTruck > MAX_POINTS) {
            return Result.error("pointsPerTruck 须在 8~" + MAX_POINTS + " 之间");
        }
        return Result.ok(mockTraceService.generate(pointsPerTruck));
    }

    @DeleteMapping("/all")
    public Result cleanup() {
        return Result.ok(mockTraceService.cleanup());
    }
}
