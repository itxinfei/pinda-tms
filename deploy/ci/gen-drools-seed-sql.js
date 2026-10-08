/*
 * 从 pd-oms/src/main/resources/rules/orderAmountCalc.drl 生成 docs/sql/运价规则_种子_orderAmountCalc.sql
 *
 * 用法（在仓库根目录）：node deploy/ci/gen-drools-seed-sql.js
 * 为什么要有这个生成器：rule 表里的 content 与 .drl 文件是同一份运价的两个副本，
 * 手写 SQL 迟早和代码漂移；单一真源保持是 .drl，改价后重跑本脚本即可。
 */
const fs = require('fs')
const path = require('path')

const repo = path.resolve(__dirname, '..', '..')
const drlPath = path.join(repo, 'pd-oms', 'src', 'main', 'resources', 'rules', 'orderAmountCalc.drl')
const outPath = path.join(repo, 'docs', 'sql', '运价规则_种子_orderAmountCalc.sql')

const drl = fs.readFileSync(drlPath, 'utf8').replace(/\r\n/g, '\n').trimEnd()

// SQL 单引号字面量里：' 要翻倍，反斜杠要再转一层（MySQL 默认允许 \ 转义）
if (drl.includes("'")) {
  console.error('DRL 里出现单引号，本生成器还没准备处理这种情况，请先改写规则文件')
  process.exit(1)
}
const literal = drl.replace(/\\/g, '\\\\')

const sql = `-- 运价规则种子：把仓库里的 Drools 规则灌进 pd_oms.rule（可重复执行）
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

USE pd_oms;

DELETE FROM rule WHERE rule_key = 'tms';

INSERT INTO rule (id, rule_key, content, version, last_modify_time, create_time)
VALUES (1, 'tms', '${literal}',
        '1',
        DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'),
        DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'));

-- 自检：应为 1 行，content 以 "package rules;" 开头
SELECT COUNT(*) AS rule_rows_for_tms FROM rule WHERE rule_key = 'tms';
SELECT LEFT(content, 40) AS content_head, LENGTH(content) AS content_len, version
  FROM rule WHERE rule_key = 'tms';
`

fs.writeFileSync(outPath, sql, 'utf8')
console.log(`已生成 ${path.relative(repo, outPath)}（DRL ${drl.length} 字符）`)
