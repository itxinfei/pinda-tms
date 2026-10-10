package com.itheima.pinda.rules;

import com.itheima.pinda.entity.fact.AddressCheckResult;
import com.itheima.pinda.entity.fact.AddressRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 运价规则计算测试
 *
 * <p>用途有两层：一是把 orderAmountCalc.drl 的档位价格钉住（20 首重 / 续重 6·9·15），
 * 二是给"rule 表里那份 content"提供可核对的基线——线上 pd_oms.rule 曾长期是 0 行，
 * 计价直接返回 null，而这件事在页面上只表现为"运费是空的"，没有任何报错。</p>
 */
public class OrderAmountCalcRuleTest {

    private static String drl;

    @BeforeAll
    public static void readRule() throws Exception {
        try (InputStream in = OrderAmountCalcRuleTest.class.getClassLoader()
                .getResourceAsStream("rules/orderAmountCalc.drl")) {
            Assertions.assertNotNull(in, "classpath 上找不到 rules/orderAmountCalc.drl");
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) > 0) {
                buf.write(chunk, 0, n);
            }
            drl = new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** 与 ReloadDroolsRulesService.loadContainerFromString 一致的装配方式 */
    private String calc(double totalWeight, double distance) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/rules/orderAmountCalc.drl", drl);
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        Assertions.assertFalse(kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR),
                "DRL 编译失败：" + kb.getResults().getMessages());

        KieContainer container = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        KieSession session = container.newKieSession();
        try {
            AddressRule rule = new AddressRule();
            rule.setTotalWeight(totalWeight);
            rule.setDistance(distance);
            AddressCheckResult result = new AddressCheckResult();
            session.insert(rule);
            session.insert(result);
            int fired = session.fireAllRules();
            Assertions.assertTrue(fired > 0, "没有任何规则命中，重量/距离组合落到了规则空档");
            Assertions.assertTrue(result.isPostCodeResult(), "规则未置通过位");
            return result.getResult();
        } finally {
            session.destroy();
            container.dispose();
        }
    }

    @Test
    public void testFirstWeightOnly() {
        Assertions.assertEquals("20", calc(0.5, 100));
    }

    @Test
    public void testContinuedFeeWithin200km() {
        // 首重 1kg=20 元，续重 4kg×6=24 → 44（5.9kg 也只按 4kg 续重计，向下取整是现有口径）
        Assertions.assertEquals("44", calc(5.0, 150));
        Assertions.assertEquals("44", calc(5.9, 150));
    }

    @Test
    public void testContinuedFee200To500km() {
        Assertions.assertEquals("29", calc(2.0, 300));
    }

    @Test
    public void testContinuedFeeAbove500km() {
        Assertions.assertEquals("35", calc(2.0, 600));
    }
}
