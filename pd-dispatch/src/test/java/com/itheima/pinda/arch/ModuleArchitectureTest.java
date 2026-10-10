package com.itheima.pinda.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * pd-dispatch 架构守护测试
 * 把 CLAUDE.md /《12-开发纪律与红线》中的分层红线固化为 CI 可执行规则：
 * mvn test 即校验，违反直接构建失败。
 *
 * 说明：循环依赖（slices().beFreeOfCycles()）在存量代码中被 DTO/utils 等共享包
 * 与业务层的双向引用大量触发，ArchUnit 1.3 Slices API 又移除了 ignoreDependency，
 * 需 FreezingArchRule 冻结基线，维护成本高。待 R2-R8 业务包结构稳定后再补该规则。
 */
@AnalyzeClasses(packages = "com.itheima.pinda", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModuleArchitectureTest {

    /**
     * Controller 只作入口层：Service / Mapper / Listener / 定时任务不得反向依赖 Controller，
     * 否则会出现"业务层反过来调 HTTP 入口"的倒置。
     */
    @ArchTest
    static final ArchRule controller不得被下层反向依赖 =
            noClasses()
                    .that().resideInAnyPackage("..service..", "..mapper..", "..listener..", "..execute..", "..task..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .as("Controller 是入口层，Service/Mapper/Listener/Task 不得反向调用它");
}
