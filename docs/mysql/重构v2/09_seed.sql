-- =====================================================================
-- 种子数据（空库重建）· 09_seed.sql
-- 目标：执行后即可用 企业编码 pinda / 账号 admin / 密码 123456 登录管理端。
-- 说明：
--   1) 使用固定可读 ID（非雪花），仅用于初始化；后续新增数据走雪花。
--   2) BCrypt 哈希取自旧 pd_auth_user（即 123456），勿明文存密码。
--   3) sys_menu 暂不灌：现有已迁移页面走静态路由，菜单随 Vue3 动态路由
--      切换批次按页面再插入，避免与静态路由重复。
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- 租户：品达物流 -------------------------------------------------------
INSERT INTO `sys_tenant`
  (`id`,`code`,`name`,`status`,`expire_time`,`description`,
   `create_time`,`update_time`,`deleted`)
VALUES
  (1,'pinda','品达物流总公司',1,NULL,'默认初始化租户',
   NOW(),NOW(),0);

-- 根组织（分公司）-----------------------------------------------------
-- 根节点 parent_id=0，tree_path 为默认 ','（仅含自身路径由维护逻辑补全）
INSERT INTO `sys_org`
  (`id`,`tenant_id`,`name`,`abbreviation`,`parent_id`,`org_type`,
   `address`,`tree_path`,`sort_value`,`status`,`description`,
   `create_time`,`update_time`,`deleted`)
VALUES
  (100,1,'品达物流总公司','品达',0,1,
   NULL,',',1,1,'平台默认根组织',
   NOW(),NOW(),0);

-- 内置角色：超级管理员 -------------------------------------------------
INSERT INTO `sys_role`
  (`id`,`tenant_id`,`name`,`code`,`description`,`status`,`readonly`,
   `create_time`,`update_time`,`deleted`)
VALUES
  (1,1,'超级管理员','ADMIN','拥有租户内全部权限',1,1,
   NOW(),NOW(),0);

-- 账号：admin / 123456 -------------------------------------------------
-- sex 1男；status 1启用；org_id=100
INSERT INTO `sys_user`
  (`id`,`tenant_id`,`account`,`name`,`org_id`,`station_id`,
   `email`,`mobile`,`sex`,`status`,`avatar`,`work_describe`,
   `password`,`password_expire_time`,`last_login_time`,
   `create_time`,`update_time`,`deleted`)
VALUES
  (1000,1,'admin','超级管理员',100,NULL,
   NULL,'13800000000',1,1,'','系统内置超管',
   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES',NULL,NULL,
   NOW(),NOW(),0);

-- 用户-角色绑定 -------------------------------------------------------
INSERT INTO `sys_user_role` (`id`,`tenant_id`,`user_id`,`role_id`)
VALUES (1,1,1000,1);

-- 计费规则（Drools 系统默认规则，tenant_id=0）--------------------------
-- content 真源 = pd-oms/src/main/resources/rules/orderAmountCalc.drl
-- 启动时 ReloadDroolsRulesService 按 rule_key='tms' 加载（租户回落0）；
-- 若不灌此条，规则加载为空，下单算价直接失败。
INSERT INTO `oms_charge_rule`
  (`id`,`tenant_id`,`rule_key`,`version`,`content`,`status`,
   `create_time`,`update_time`,`deleted`)
VALUES
  (1,0,'tms','1.0',
'package rules;

import com.itheima.pinda.entity.fact.AddressRule;
import com.itheima.pinda.entity.fact.AddressCheckResult;
import com.itheima.pinda.service.DroolsRulesService;
import com.itheima.pinda.service.impl.DroolsRulesServiceImpl;

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
1,NOW(),NOW(),0);

