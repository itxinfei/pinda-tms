-- 运价规则种子：把仓库里的 Drools 规则灌进 pd_oms.rule（可重复执行）
--
-- 为什么需要：ReloadDroolsRulesService.loadRules() 只认 rule_key='tms'，而线上
-- pd_oms.rule 是 0 行（2026-10-08 实测）→ getKieContainer() 为空 →
-- OrderServiceImpl.calculateAmount() 直接 return null：下单与报价都算不出运费。
-- 仓库里从来没有这条 INSERT，所以"计价代码齐全"和"能算出价"是两回事。
--
-- 单一真源是 pd-oms/src/main/resources/rules/orderAmountCalc.drl；改价后重新生成：
--   node deploy/ci/gen-drools-seed-sql.js
-- 灌完必须让每个运行中的实例重载（规则装在单 JVM 内存里，多实例要逐台刷）：
--   curl -X POST http://<pd-oms 实例>:8186/rules/reload
-- 只灌 pd_oms：pd-oms 的 datasource 实测就是 pd_oms（Nacos pd-oms-prod.yml:14），
-- pinda_tms 里的同名 rule 表是历史副本，不喂代码。
--
-- ⚠️ 重构/删码提示（2026-10-09）：本 content 里三条续重规则调用
--   new DroolsRulesServiceImpl().calcFee(addressRule)（.drl 第 33/50/67 行）。该调用
--   写在 then 块内、被编译进规则字节码，Java 侧 grep 查不到引用，曾被多份文档误判为
--   "死代码可清理"——删掉该类全站算价即挂。重写计费时必须保持语义等价：
--   超出首重部分 setScale(0, ROUND_DOWN) 向下截断（不足 1kg 舍去），返回 String。
--   口径与算例的唯一真源：docs/需求文档/52-旧代码独占知识固化_计费事件与异常上报契约.md §1
--   （旧代码删除后，本 SQL 与《52》§1 共同构成运价规则的幸存副本）。

USE pd_oms;

DELETE FROM rule WHERE rule_key = 'tms';

INSERT INTO rule (id, rule_key, content, version, last_modify_time, create_time)
VALUES (1, 'tms', 'package rules;

import com.itheima.pinda.entity.fact.AddressRule
import com.itheima.pinda.entity.fact.AddressCheckResult
import com.itheima.pinda.service.DroolsRulesService
import com.itheima.pinda.service.impl.DroolsRulesServiceImpl

dialect "java"

rule "1千克以内20元"
    activation-group "mygroup"
    salience 10
    when
        addressRule : AddressRule(totalWeight != null && totalWeight <= 1.00)
        checkResult : AddressCheckResult()
    then
        checkResult.setPostCodeResult(true);
        checkResult.setResult("20");
        System.out.println("1千克以内20元");
end

rule "1千克以上，订单距离在200公里以下的，首重1千克，首重价格20元，续重每1千克资费为6元"
    activation-group "mygroup"
    salience 9
    when
        addressRule : AddressRule(totalWeight != null && totalWeight > 1.00 && distance <= 200.00)
        checkResult : AddressCheckResult()
    then
        addressRule.setFirstFee(20.00);
        addressRule.setFirstWeight(1.00);
        addressRule.setContinuedFee(6.00);
        DroolsRulesService droolsRulesService = new DroolsRulesServiceImpl();
        String orderAmount = droolsRulesService.calcFee(addressRule);
        checkResult.setPostCodeResult(true);
        checkResult.setResult(orderAmount);
        System.out.println("1千克以上，订单距离在200公里以下的，首重1千克，首重价格20元，续重每1千克资费为6元");
end

rule "1千克以上，订单距离在200~500公里的，首重1千克，首重价格20元，续重每1千克资费为9元"
    activation-group "mygroup"
    salience 8
    when
        addressRule : AddressRule(totalWeight != null && totalWeight > 1.00 && distance <= 500.00)
        checkResult : AddressCheckResult()
    then
        addressRule.setFirstFee(20.00);
        addressRule.setFirstWeight(1.00);
        addressRule.setContinuedFee(9.00);
        DroolsRulesService droolsRulesService = new DroolsRulesServiceImpl();
        String orderAmount = droolsRulesService.calcFee(addressRule);
        checkResult.setPostCodeResult(true);
        checkResult.setResult(orderAmount);
        System.out.println("1千克以上，订单距离在200~500公里的，首重1千克，首重价格20元，续重每1千克资费为9元");
end

rule "1千克以上，订单距离在500公里以上的，首重1千克，首重价格20元，续重每1千克资费为15元"
    activation-group "mygroup"
    salience 7
    when
        addressRule : AddressRule(totalWeight != null && totalWeight > 1.00 && distance > 500.00)
        checkResult : AddressCheckResult()
    then
        addressRule.setFirstFee(20.00);
        addressRule.setFirstWeight(1.00);
        addressRule.setContinuedFee(15.00);
        DroolsRulesService droolsRulesService = new DroolsRulesServiceImpl();
        String orderAmount = droolsRulesService.calcFee(addressRule);
        checkResult.setPostCodeResult(true);
        checkResult.setResult(orderAmount);
        System.out.println("1千克以上，订单距离在500公里以上的，首重1千克，首重价格20元，续重每1千克资费为15元");
end',
        '1',
        DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'),
        DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'));

-- 自检：应为 1 行，content 以 "package rules;" 开头
SELECT COUNT(*) AS rule_rows_for_tms FROM rule WHERE rule_key = 'tms';
SELECT LEFT(content, 40) AS content_head, LENGTH(content) AS content_len, version
  FROM rule WHERE rule_key = 'tms';
