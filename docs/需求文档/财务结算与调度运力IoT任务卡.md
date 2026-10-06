# 财务结算 · 调度运力 · IoT 逐项任务卡（第三 / 四 / 五梯队）

> 承接《友商定位对比与优化完善清单》：
> - **第三梯队（P1）财务结算**：FIN-1 运费结算对账 / FIN-2 司机趟次工资 / FIN-3 账期回款坏账 / FIN-4 每趟利润
> - **第四梯队（P1/P2）调度运力**：DSP-1 多约束路线规划（可解释）/ DSP-2 外协·加盟运力池 / DSP-3 抢派双模
> - **第五梯队（P2）IoT 增值（依赖硬件，按需）**：IOT-1 冷链温控 / IOT-2 重量·视频货损 / IOT-3 能源结算
>
> **前置必读**：`ClaudeCode开发纪律与红线.md`、`开发规范与需求规格说明书.md`、`多端数据打通与统一领域模型.md`、`运营可视化与成本管控任务卡.md`（OPS-4 已建 `pd_task_cost`）、`UI设计规范与页面模板.md`。

---

## 0. 硬约束（不可违反）

- JDK 1.8、**不升级任何依赖**、安全 CVE 不处理。
- **不引新中间件**：不用 MongoDB / Redis Stream / MQTT / XXL-JOB / ES / 专用求解器（如 OR-Tools）。
- **新建 MySQL 表允许**；能查询算出的一律不建表。
- IoT 设备上报**复用现有通道**：`pd-netty` 的 Netty + HTTP（`POST /netty/push`），**不引 MQTT**。
- 管理端维持 Vue2 + Element UI。
- 接口风格：`@RequestMapping("xxx")`（无前导斜杠）+ `@PostMapping("/page")`。
- **★新增接口三层落点约定（2026-10-06 自检补，全卡通用）**：本卡 FIN/DSP/IOT 新增接口定义在 `pd-oms`/`pd-work`/`pd-dispatch`/`pd-base` 等**内部服务**，但内部服务**网关无路由**（网关 `ignored-services:'*'`），前端不可直连。所有新增接口必须：
  1. **数据/服务层**：在业务内部服务实现（Feign 互调，如运费规则、VRP 求解）；
  2. **网关暴露层**：由 `pd-web-manager` 新建聚合 Controller 暴露，网关路径 `/api/web-manager/<模块>/*`（如 `/api/web-manager/freight-bill/*`、`/api/web-manager/payment/*`、`/api/web-manager/dispatch/*`、`/api/web-manager/iot/*`）；C 端查询类走 `pd-web-customer`；
  3. **权限注册**：接口注册进 `pd_auth.pd_auth_resource` 并绑定菜单角色（阻断 C，见《联调就绪检查清单》§2），否则网关判"未知请求"拒绝。
  **禁止**把内部服务路径（如 `pd-oms /pay/*`）直接写进前端 API 配置——必 404 或被网关拒。

### ⚠️ 关于"外协运力池"的红线澄清（必须先读）
用户已明确**不对接任何外部厂商/三方物流 API**。
**DSP-2 外协运力池 = 把加盟/外协的车和人"录入本系统"进行统一管理**（建档、派单、结算），
**≠ 调用外部厂商系统接口**（不对接顺丰/三通一达/EDI/ERP/外部平台 API）。
实现时**只允许本系统内部 CRUD + 内部派单**，禁止任何对外接口调用。

---

## 1. 现状核对小结

| 域 | 现状 | 结论 |
|---|---|---|
| 运费计算 | ✅ **Drools 运费规则已有**（`DroolsRulesServiceImpl`，支持热加载 `ReloadDroolsRulesService` / `RulesReloadController`） | 复用，但**计费明细未落库** |
| 订单金额 | ✅ `Order.amount`、`paymentMethod`(1预结/2到付)、`paymentStatus`(1未付/2已付) | 有字段，无结算流程 |
| 结算/对账/账单 | ❌ **完全无**（搜索"结算/对账/bill/承运商"几乎无匹配实体） | 全新域，需建表 |
| 趟次成本 | ✅ OPS-4 已建 `pd_task_cost` | 利润 = 收入 − 成本 |
| 调度链路 | ✅ **`DispatchTask` 5 阶段明确**：订单分类 → **路线规划 `ITaskRoutePlanningService`（实现 `TaskRoutePlanningServiceImpl`）** → 创建运输任务 → 车次车辆司机 `ITaskTripsSchedulingService` → 完善任务+司机作业；用 `@GlobalTransactional`(Seata) | **VRP 升级落点 = `TaskRoutePlanningServiceImpl`** |
| 车辆载重/体积 | ✅ `PdTruck.allowableLoad / allowableVolume`、`PdTruckType` | 多约束 VRP 的约束条件已具备 |
| 线路/车次 | ✅ `PdTransportLine`、`PdTransportTrips`、`PdTransportTripsTruckDriver` | 可复用 |
| 外协/承运商 | ❌ 无实体 | 需建表 |
| 状态机 | ✅ **`StateTransitionValidator` 已完备且已被调用** | **严禁新建/重写状态机**（见《多端数据打通》§3.2 修正） |
| IoT（温控/重量/能源） | ❌ 完全无 | P2，依赖硬件投入 |

---

# 第三梯队：财务结算（P1）

> 对标 G7 财运通"订单/车辆/运力三大利润中心打通、盈亏实时可见"；通用 TMS 的运费审计与结算。
> **解决**：品达财务侧完全空白 —— 不算账、不对账、不结算。

## FIN-1 运费结算与对账

### 新建表
```sql
-- 客户运费账单（对客户/加盟方的应收账单）
CREATE TABLE pd_freight_bill (
  id            VARCHAR(64) NOT NULL COMMENT '主键',
  bill_no       VARCHAR(64) NOT NULL COMMENT '账单号',
  member_id     VARCHAR(64) NOT NULL COMMENT '客户id',
  period_start  DATE        NOT NULL COMMENT '账期开始',
  period_end    DATE        NOT NULL COMMENT '账期结束',
  order_count   INT         NOT NULL DEFAULT 0 COMMENT '运单数',
  total_amount  DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '应收总额',
  paid_amount   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  status        INT         NOT NULL DEFAULT 0 COMMENT '0待结算 1部分结算 2已结算 3已坏账',
  due_date      DATE        NULL COMMENT '到期日',
  remark        VARCHAR(255) NULL,
  create_time   DATETIME    NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bill_no (bill_no),
  KEY idx_member (member_id),
  KEY idx_status_due (status, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户运费账单';

-- 账单明细（按订单/运单逐条，支持对账争议追溯）
CREATE TABLE pd_freight_bill_detail (
  id                VARCHAR(64) NOT NULL,
  bill_id           VARCHAR(64) NOT NULL COMMENT '账单id',
  order_id          VARCHAR(64) NULL COMMENT '订单id',
  transport_order_id VARCHAR(64) NULL COMMENT '运单id',
  base_freight      DECIMAL(12,2) NULL COMMENT '基础运费',
  extra_freight     DECIMAL(12,2) NULL COMMENT '附加费(保价/上楼/超区等)',
  adjust_amount     DECIMAL(12,2) NULL COMMENT '调整金额(争议后调整)',
  amount            DECIMAL(12,2) NOT NULL COMMENT '小计',
  create_time       DATETIME    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_bill (bill_id),
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费账单明细';
```

### 接口（`@RequestMapping("freight-bill")`）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/freight-bill/generate` | 按客户+账期生成账单（归集期内已完成订单） |
| POST | `/freight-bill/page` | 账单分页（客户/状态/到期日筛选） |
| GET | `/freight-bill/{id}` | 账单详情 + 明细 |
| POST | `/freight-bill/{id}/adjust` | 争议调账（写 `adjust_amount`，留痕） |
| GET | `/freight-bill/reconcile` | **对账视图**：账单 vs 运单 vs 支付记录差异 |

### 要点
- **计费明细落库**：把 Drools 算出的运费结果**持久化**到明细（现状只算不存，无法对账追溯）。
- 调账必须留痕，禁止直接改原金额。

### 合规约束（R10-1 · P1 · 依据《合规开发优先级清单》#16 /《物流行业合规与监管要求》R10）
- **运价透明可查**：运费计算规则（Drools 规则）须对司机/客户**透明展示**，结算单含计费明细（基础运费 + 附加项），禁止暗箱；呼应 §10.4 价格固化。
- **运费保障**：支持结算保障 / 垫付机制（COD / 月结账期），司机结算单（FIN-2）与回款（FIN-3）闭环，避免运费拖欠争议。
- 上述费率/结算规则变更须**留痕可审计**（呼应 R6-3 审计）。

### FIN-1.1 客户价格协议（P1 扩展 · 对标 G7 财运通结算 · 2026-10-06 并入）

> **解决**：B 端客户（企业/月结）没有专属计价，只能吃默认 Drools 规则 → 无法谈价、无法月结对账。

```sql
CREATE TABLE pd_price_agreement (
  id            VARCHAR(64) NOT NULL,
  member_id     VARCHAR(64) NOT NULL COMMENT '客户id（月结/企业客户）',
  line_id       VARCHAR(64) NULL COMMENT '线路id（空=全线路）',
  goods_type_id VARCHAR(64) NULL COMMENT '货物类型id（空=全部）',
  weight_min    DECIMAL(10,2) NULL COMMENT '重量段下限(kg)',
  weight_max    DECIMAL(10,2) NULL COMMENT '重量段上限(kg)',
  price_type    INT NOT NULL COMMENT '计价方式 1按票 2按吨 3按方',
  price         DECIMAL(12,2) NOT NULL COMMENT '单价',
  discount      DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '月结折扣(0.90=9折)',
  effective_from DATE NOT NULL,
  effective_to   DATE NULL,
  status        INT NOT NULL DEFAULT 1 COMMENT '1生效 0停用',
  create_time   DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户价格协议';
```

- **计费衔接**：Drools 计算前先查客户有效协议（member_id+线路+货物类型+重量段命中）→ **命中走协议价，未命中走默认规则**；协议变更留痕（R10-1 透明可查）。
- **接口**（归 FIN-1 `@RequestMapping("freight-bill")` 扩展）：`POST /price-agreement`（增改）、`GET /price-agreement/page`、`POST /price-agreement/{id}/disable`。
- **管理端页**：客户管理 → 价格协议（`pinda/customer/priceAgreement`，P1）。

### FIN-1.2 电子发票（P1 扩展 · 对标 G7 财运通结算 · 2026-10-06 并入）

> **解决**：对账后要开票，现无发票记录。**测试/开发环境不接真实税控**（税盘/百望等外部对接属"外部厂商 API"边界，不实现），只做申请-开具-邮寄状态管理。

```sql
CREATE TABLE pd_invoice (
  id            VARCHAR(64) NOT NULL,
  invoice_no    VARCHAR(64) NULL COMMENT '发票号（税控开具后回填；测试环境自编）',
  bill_id       VARCHAR(64) NOT NULL COMMENT '关联账单（FIN-1）',
  member_id     VARCHAR(64) NOT NULL,
  amount        DECIMAL(14,2) NOT NULL,
  type          INT NOT NULL DEFAULT 1 COMMENT '1增值税普票 2增值税专票',
  status        INT NOT NULL DEFAULT 1 COMMENT '1待申请 2已申请 3已开具 4已邮寄 5已作废',
  tax_no        VARCHAR(64) NULL COMMENT '客户税号',
  mail_address  VARCHAR(255) NULL COMMENT '邮寄地址',
  create_time   DATETIME NOT NULL,
  invoice_time  DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_bill (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子发票';
```

- **接口**（归 FIN-1 扩展）：`POST /invoice/apply`（按账单申请）、`POST /invoice/{id}/issue`（标记开具，测试环境自编发票号）、`POST /invoice/{id}/mail`（邮寄）、`GET /invoice/page`。
- **不做**：不接真实税控开票（边界条款）；不做发票真伪查验。

## FIN-2 司机趟次工资结算

### 新建表
```sql
CREATE TABLE pd_driver_settlement (
  id            VARCHAR(64) NOT NULL,
  settle_no     VARCHAR(64) NOT NULL COMMENT '结算单号',
  driver_id     VARCHAR(64) NOT NULL,
  period_start  DATE NOT NULL,
  period_end    DATE NOT NULL,
  trip_count    INT  NOT NULL DEFAULT 0 COMMENT '完成趟次',
  base_wage     DECIMAL(12,2) NULL COMMENT '底薪',
  trip_fee      DECIMAL(12,2) NULL COMMENT '趟次费合计',
  allowance     DECIMAL(12,2) NULL COMMENT '补贴(油补/过路/餐补)',
  deduction     DECIMAL(12,2) NULL COMMENT '扣款(违章/货损/迟到)',
  total_amount  DECIMAL(12,2) NOT NULL COMMENT '应发合计',
  status        INT NOT NULL DEFAULT 0 COMMENT '0待审 1已审 2已发放',
  create_time   DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_settle_no (settle_no),
  KEY idx_driver (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机结算单';

CREATE TABLE pd_driver_settlement_detail (
  id               VARCHAR(64) NOT NULL,
  settlement_id    VARCHAR(64) NOT NULL,
  task_transport_id VARCHAR(64) NULL COMMENT '趟次(运输任务)id',
  driver_job_id    VARCHAR(64) NULL,
  amount           DECIMAL(12,2) NOT NULL COMMENT '该趟金额',
  remark           VARCHAR(255) NULL,
  create_time      DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_settle (settlement_id),
  KEY idx_task (task_transport_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机结算明细(按趟次)';
```

### 接口（`@RequestMapping("driver-settlement")`）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/driver-settlement/generate` | 按司机+周期生成（归集 `DriverJob.status=4` 的趟次） |
| POST | `/driver-settlement/page` | 结算单分页 |
| GET | `/driver-settlement/{id}` | 详情 + 逐趟明细 |
| POST | `/driver-settlement/{id}/audit` | 审核 / 发放 |
| GET | `/driver-settlement/driver/{driverId}` | 某司机历史结算 |

### 要点
- **趟次是司机结算天然单位**（`DriverJob.taskTransportId`），按趟归集。
- 趟次费规则（每趟多少钱/按里程/按重量）**配置化进 yml 或字典**，禁止硬编码。

## FIN-3 账期 / 回款 / 坏账预警

### 新建表
```sql
-- 客户账期与信用
CREATE TABLE pd_customer_credit (
  id                VARCHAR(64) NOT NULL,
  member_id         VARCHAR(64) NOT NULL COMMENT '客户id',
  credit_period_days INT NOT NULL DEFAULT 0 COMMENT '账期天数',
  credit_limit      DECIMAL(14,2) NULL COMMENT '信用额度',
  used_amount       DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已用额度',
  risk_level        INT NOT NULL DEFAULT 0 COMMENT '0正常 1关注 2预警 3黑名单',
  create_time       DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户账期与信用';

-- 收付款流水（应收登记 / 回款 / 坏账核销）
CREATE TABLE pd_payment_record (
  id          VARCHAR(64) NOT NULL,
  bill_id     VARCHAR(64) NULL COMMENT '关联账单',
  type        INT NOT NULL COMMENT '1应收登记 2回款 3坏账核销 4退款',
  amount      DECIMAL(14,2) NOT NULL,
  pay_method  INT NULL COMMENT '1现金 2转账 3在线支付 4到付核销',
  occur_time  DATETIME NOT NULL,
  operator    VARCHAR(64) NULL,
  remark      VARCHAR(255) NULL,
  create_time DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_bill (bill_id),
  KEY idx_occur (occur_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收付款流水';
```

### 接口（`@RequestMapping("payment")`）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/payment/receive` | 回款登记（回写账单 `paid_amount` 与状态） |
| POST | `/payment/bad-debt` | 坏账核销 |
| GET | `/payment/aging` | **应收账龄报表**（0-30/31-60/61-90/90+ 天） |
| GET | `/payment/overdue` | 逾期账单列表（触发预警） |

### 要点
- 逾期预警：每天 `@Scheduled` 扫 `due_date < now 且 status != 2` 的账单 → 写告警（`pd_transport_alert` 扩 `bizType=BILL`）或站内通知。
- **超信用额度时拦截下单**（可选，配置开关）。

## FIN-4 每趟 / 每单利润（"老板看数"）

### 不建表 —— 查询计算
```
趟次利润 = Σ(该趟关联订单 Order.amount)  −  Σ(pd_task_cost where task_transport_id)
订单利润 = Order.amount − 分摊成本
```
数据来源：`pd_task_cost`（OPS-4）+ `TransportOrderTask` → `TransportOrder.orderId` → `Order.amount`。

### 接口（`@RequestMapping("profit")`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/profit/task/{taskId}` | 单趟利润（收入、成本明细、毛利、毛利率） |
| GET | `/profit/order/{orderId}` | 单订单利润 |
| GET | `/profit/summary` | 利润汇总（按车队/车辆/线路/月份，支持排序找亏损线） |
| GET | `/profit/ranking` | 线路/车辆利润排行（找出最赚与最亏） |
| GET | `/profit/drill/{dimension}` | **联动钻取（P1 · 承接 OPS-4 §6.7）**：dimension=order/truck/line/fleet，订单→车辆→趟次→利润逐层下钻 |

### 要点
- **V1 只做毛利**（收入 − 直接成本），不做分摊间接成本（避免过度设计）。
- 管理端**利润驾驶舱页**：卡片 + 排行（对标 G7"打开老板看数就能看到利润"）。
- **移动端老板视图（P1 扩展 · 2026-10-06 并入）**：复用司机端 App（admin 角色可见）或管理端 H5 适配，**只看 KPI**（今日业务量 / 毛利 / 在途异常数 / 成本占比 / 亏损线 Top3），调 `/profit/summary` + `/profit/ranking`，**不做明细操作**。

---

# 第四梯队：调度与运力（P1/P2）

## DSP-1 多约束路线规划（可解释）

### 目标
现状 DFS 路径规划**过程黑盒不可解释**（已知痛点）。对标满帮/货拉拉/G7 的智能调度。

### 落点（已确认）
**`pd-dispatch` 的 `TaskRoutePlanningServiceImpl`**（`DispatchTask` 第 2 阶段调用）。第 4 阶段 `ITaskTripsSchedulingService` 负责车次/车辆/司机匹配。

### 升级：从 DFS → 多约束启发式（**不引求解器依赖**）
约束（数据均已具备）：
| 约束 | 数据来源 |
|---|---|
| 车型匹配 | `PdTruckType`、`PdTruck.truckTypeId` |
| 载重 | `PdTruck.allowableLoad` vs 货物 `OrderCargo.totalWeight` |
| 体积 | `PdTruck.allowableVolume` vs `OrderCargo.totalVolume` |
| 时效 | `TaskTransport.planArrivalTime` / 线路时长 `PdTransportLine` |
| 多点取送 | `OrderLocation` / 订单收发地址 |
| 司机资质 | `PdTruckDriverLicense`、`PdTruckDriver.drivingAge` |

**算法建议（JDK8 纯 Java，零新依赖）**：
1. 先按约束做**可行性剪枝**（车型/载重/体积/时效不达标直接排除）。
2. DFS/贪心构造初始解 → **局部搜索改进**：2-opt（路径内换位）+ 插入法（订单换线）。
3. **保留可解释性**：每一步决策记录原因。

### 新增表（可解释性，解决"黑盒"痛点）
```sql
CREATE TABLE pd_dispatch_decision_log (
  id           VARCHAR(64) NOT NULL,
  job_id       VARCHAR(64) NULL COMMENT '调度任务id',
  business_id  VARCHAR(64) NULL COMMENT '机构id',
  order_id     VARCHAR(64) NULL,
  stage        VARCHAR(32) NOT NULL COMMENT 'CLASSIFY分类 ROUTE路线 TRUKSCHED车次调度',
  decision     VARCHAR(255) NOT NULL COMMENT '决策结果(选了哪条线/哪辆车)',
  reason       VARCHAR(512) NULL COMMENT '决策原因(约束命中/成本最低/时效最优)',
  rejected     VARCHAR(512) NULL COMMENT '被否决项及原因',
  create_time  DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_job (job_id),
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度决策日志(可解释)';
```

### 接口
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/dispatch/decision/{jobId}` | 某次调度的决策过程（管理端"为什么这么调度"） |
| POST | `/dispatch/simulate` | **调度试算**（不落库，对比不同参数结果） |

### 红线
- **禁止引入 OR-Tools / 任何求解器依赖**（不升级技术栈）。
- **禁止为了"智能"牺牲可解释性** —— 每条调度决策必须能说出原因。
- 不改动 `DispatchTask` 的 5 阶段结构与 `@GlobalTransactional`。

## DSP-2 外协 / 加盟运力池

> ⚠️ **红线**：仅"录入本系统管理"，**禁止调用任何外部厂商 API**。

### 新建表
```sql
CREATE TABLE pd_carrier (
  id           VARCHAR(64) NOT NULL COMMENT '承运商/外协id',
  code         VARCHAR(64) NOT NULL COMMENT '编码',
  name         VARCHAR(128) NOT NULL COMMENT '名称',
  contact_name VARCHAR(64) NULL,
  contact_phone VARCHAR(32) NULL,
  settle_type  INT NULL COMMENT '结算方式 1现结 2月结 3趟结',
  status       INT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常',
  remark       VARCHAR(255) NULL,
  create_time  DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承运商/外协(加盟运力主体)';

CREATE TABLE pd_carrier_truck (
  id          VARCHAR(64) NOT NULL,
  carrier_id  VARCHAR(64) NOT NULL COMMENT '所属承运商',
  license_plate VARCHAR(32) NOT NULL COMMENT '车牌',
  truck_type_id VARCHAR(64) NULL,
  allowable_load DECIMAL(12,2) NULL,
  allowable_volume DECIMAL(12,2) NULL,
  status      INT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常 2在途',
  create_time DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_carrier (carrier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外协车辆';

CREATE TABLE pd_carrier_driver (
  id          VARCHAR(64) NOT NULL,
  carrier_id  VARCHAR(64) NOT NULL,
  name        VARCHAR(64) NOT NULL,
  phone       VARCHAR(32) NULL,
  license_no  VARCHAR(64) NULL COMMENT '驾驶证号',
  status      INT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常 2在途',
  create_time DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_carrier (carrier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外协司机';
```

### 接口（`@RequestMapping("carrier")`）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST/GET/PUT | `/carrier`、`/carrier/{id}` | 承运商 CRUD |
| GET | `/carrier/{id}/trucks`、`/carrier/{id}/drivers` | 外协车辆/司机列表 |
| GET | `/carrier/capacity` | **可用运力池查询**（按车型/载重/区域筛选空闲外协车） |
| POST | `/carrier/dispatch` | 派单给外协（生成运输任务，标记来源=外协） |

### 要点
- `TaskTransport` 需区分**自营/外协**（可加 `carrier_id` 字段，为空即自营）。
- 外协结算走 FIN-1 的账单（对承运商的应付）。

### 承运商 KPI 考核（P1 扩展 · 行业基准：教材 TMS/芸柚/HashMicro · 2026-10-06 并入）

> **解决**：外协车"谁准时率高、谁货损多、谁异常多"无量化，靠感觉；KPI 考核给派单决策与结算（续约/淘汰）数据支撑。

- **指标**（纯统计查询，零表改动）：准时率（任务实际到达 vs 计划到达）、货损率（异常货损件数/总件数）、异常率（`pd_transport_alert` 按承运商聚合）、月完成趟次。
- **接口**（归 DSP-2 `@RequestMapping("carrier")` 扩展，P1）：`GET /carrier/{id}/kpi`（单承运商考核卡）、`GET /carrier/kpi/list`（承运商考核对比表，支持按月筛选）。
- **落点**：管理端承运商管理页加"考核"入口；考核结果仅展示，不做自动封禁（避免误伤，红线保留人工判定）。
- **DoD**：指标口径可解释（在接口文档注明计算公式）；可按月/承运商筛选；不建新表。

### 承运商端自助门户（P1 扩展 · 参照联云 SLMS 承运商端 · 2026-10-06 并入）

> **解决**：外协承运商（加盟）接单/节点确认/回单/对账全在微信电话里来回，无自助入口。联云把承运商端做成了独立产品线（SLMS：全部运单/未指派/待调度/已调度/待称重/已交接/已装车/运输中/到达签收/已签收/异常闭环/黑名单记录）。G7 亦强调外协"统一管理、可控运力池"。本项目 DSP-2 已有外协池，补**承运商端入口**即闭环。

- **形态（P1）**：司机端 App 增加"承运商"身份（复用统一账号体系，外协司机登录即承运商视图），不新开发独立 App——承运商/外协司机看到：**待接单任务（接/拒）→ 节点确认（装车/在途/到达/签收，同司机端节点）→ 回单上传（复用 POD）→ 运费对账（应收对账单确认，复用 FIN-1 账单）**。
- **接口**（归 DSP-2 `@RequestMapping("carrier")`，P1）：`GET /carrier/task/list`（待接单任务）、`POST /carrier/task/{id}/accept|reject`（接/拒单）、`GET /carrier/settlement/statement`（承运商应收对账单）。
- **权限**：`pd_auth_resource` 新增承运商角色（复用 driver 角色扩展或新增 `CARRIER`），路由走 `/api/web-driver` 承运商视图。
- **不做**：独立承运商门户系统（SLMS 级）、承运商入驻审批流（P2 再议）。

### 承运商黑名单记录（P2 扩展 · 参照联云 SLMS 黑名单 · 2026-10-06 并入）

- 在 DSP-2 KPI 基础上，管理端可**人工拉黑/备注**承运商（原因+时间+操作人，入 `pd_carrier` 扩展字段或新表 `pd_carrier_blacklist`）；调度派单时黑名单承运商不出现在 `/carrier/capacity` 候选；仅人工判定，不自动封禁（红线）。
- **DoD**：黑名单可增删查；拉黑后调度候选自动排除；操作留痕。

## DSP-3 抢派双模

### 新建表
```sql
CREATE TABLE pd_task_grab_record (
  id               VARCHAR(64) NOT NULL,
  task_transport_id VARCHAR(64) NOT NULL COMMENT '运输任务id',
  driver_id        VARCHAR(64) NOT NULL COMMENT '抢单司机',
  grab_time        DATETIME NOT NULL,
  result           INT NOT NULL DEFAULT 0 COMMENT '0抢单成功 1被抢走 2取消',
  create_time      DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_driver (task_transport_id, driver_id),
  KEY idx_task (task_transport_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机抢单记录';
```

### 接口（`@RequestMapping("dispatch-assign")`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/dispatch-assign/mode` | 获取派单模式（自动派 / 抢单，按机构配置） |
| POST | `/dispatch-assign/mode` | 配置派单模式 |
| GET | `/dispatch-assign/pool` | **抢单池**（司机端：可抢任务列表） |
| POST | `/dispatch-assign/grab` | 司机抢单（并发控制：同一任务只允许一人成功） |

### 要点
- 并发控制：抢单用**数据库唯一键 `uk_task_driver` + 条件更新**控制，不用分布式锁（不引中间件）。
- 抢单成功后更新 `DriverJob` 与 `TaskTransport.assignedStatus`。

---

# 第五梯队：IoT 增值（P2，依赖硬件，按需）

> **前提**：需采购/配备相应硬件。**无硬件则本期不做**。
> **上报通道统一复用 `pd-netty` 的 Netty + HTTP（`POST /netty/push`），不引 MQTT。**

## IOT-1 冷链温控
```sql
CREATE TABLE pd_iot_temperature (
  id            VARCHAR(64) NOT NULL,
  truck_id      VARCHAR(64) NULL COMMENT '车辆id',
  task_transport_id VARCHAR(64) NULL COMMENT '运输任务id',
  temperature   DECIMAL(5,2) NOT NULL COMMENT '温度(℃)',
  humidity      DECIMAL(5,2) NULL COMMENT '湿度',
  device_time   DATETIME NOT NULL COMMENT '设备采集时间',
  create_time   DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_task (task_transport_id),
  KEY idx_truck_time (truck_id, device_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冷链温控记录';
```
- 阈值配置进 yml（如 2~6℃），超阈值 → 告警类型 `TEMP_ABNORMAL`（写入 `pd_transport_alert`，复用 OPS-0 告警中心）。
- 接口：`POST /iot/temperature/report`（上报）、`GET /iot/temperature/task/{taskId}`（曲线 + 超标点）。
- 前端：温度曲线图（管理端 ECharts）+ 司机端超标提醒。

## IOT-2 重量 / 视频货损监控
```sql
CREATE TABLE pd_iot_weight (
  id            VARCHAR(64) NOT NULL,
  truck_id      VARCHAR(64) NULL,
  task_transport_id VARCHAR(64) NULL,
  weight        DECIMAL(12,2) NOT NULL COMMENT '重量(kg)',
  device_time   DATETIME NOT NULL,
  create_time   DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_task (task_transport_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='载重监控记录';
```
- 异常判定：装货后/运输中重量骤降 → 告警 `WEIGHT_ABNORMAL`（防盗/防货损）。
- **视频复用已规划的司机视频能力（VID）**，不另起视频系统。
- **合规约束（R2-4 · P1 · 依据《合规开发优先级清单》#9 /《物流行业合规与监管要求》R2/R11）**：司机视频（盲区预警 / 驾驶员状态）数据须**直传监管平台或本平台**，不得经不可控的中间转发环节（GB/T 28181 / JT/T 1078 国标接入，或云厂商 RTC 直传）。视频/状态结果落库用于 OPS-5 驾驶行为分析，但**原始视频流路径须可审计、不落地到非授权节点**。
- 接口：`POST /iot/weight/report`、`GET /iot/weight/task/{taskId}`。

## IOT-3 能源（油卡 / 加油）结算
- 复用 OPS-4 的 `pd_truck_fuel`（加油记录），扩展字段区分**油卡/现金**。
```sql
-- 在 pd_truck_fuel 上扩展（或新建油卡表）
CREATE TABLE pd_fuel_card (
  id           VARCHAR(64) NOT NULL,
  card_no      VARCHAR(64) NOT NULL COMMENT '油卡号',
  truck_id     VARCHAR(64) NULL,
  carrier_id   VARCHAR(64) NULL COMMENT '承运商(外协)',
  balance      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '余额',
  status       INT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常',
  create_time  DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_card (card_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='油卡';
```
- 加油记录关联油卡 → 与加油站/油卡对账（`GET /fuel/reconcile`）。
- **前提**：需与加油站或油卡服务商有合作；无合作则不做。

---

## 2. 新建表 DDL 汇总

| 表 | 库 | 用途 | 梯队 |
|---|---|---|---|
| `pd_freight_bill` / `pd_freight_bill_detail` | pd-work(或新建财务库) | 客户运费账单与明细 | FIN-1 |
| `pd_driver_settlement` / `pd_driver_settlement_detail` | 同上 | 司机趟次结算 | FIN-2 |
| `pd_customer_credit` / `pd_payment_record` | 同上 | 账期信用、收付款流水 | FIN-3 |
| （FIN-4 利润 **不建表**，查询计算） | — | — | FIN-4 |
| `pd_dispatch_decision_log` | pd-dispatch | 调度决策可解释日志 | DSP-1 |
| `pd_carrier` / `pd_carrier_truck` / `pd_carrier_driver` | pd-base | 外协运力池 | DSP-2 |
| `pd_task_grab_record` | pd-work/pd-dispatch | 抢单记录 | DSP-3 |
| `pd_iot_temperature` | pd-netty | 冷链温控 | IOT-1 |
| `pd_iot_weight` | pd-netty | 载重监控 | IOT-2 |
| `pd_fuel_card` | pd-base | 油卡 | IOT-3 |

> 另：`TaskTransport` 建议加 `carrier_id`（区分自营/外协），为空即自营 —— **唯一建议的存量表加字段项**。

## 3. 验收 DoD

- [ ] 能按客户账期生成运费账单，明细可追溯到每单；支持调账留痕与对账视图。
- [ ] 能按司机生成趟次结算单（逐趟明细），支持审核发放。
- [ ] 应收账龄报表可出；逾期账单能触发预警。
- [ ] 能算出单趟/单订单毛利，管理端利润驾驶舱可见排行。
- [ ] 路线规划引入车型/载重/体积/时效约束，且**调度决策可解释**（决策日志可查）。
- [ ] 外协承运商/车辆/司机可建档并派单，**且未调用任何外部厂商 API**。
- [ ] 抢单池可用，同一任务并发只一人成功。
- [ ] IoT 三项**仅在硬件具备时**落地；上报走 `/netty/push`，未引 MQTT。
- [ ] **未新增任何中间件、未升级任何依赖**，JDK 1.8 编译通过。
- [ ] 阈值与规则进配置，无硬编码。

## 4. 实施顺序

```
第三梯队（先做，财务闭环）
  FIN-1 运费结算 → FIN-2 司机结算 → FIN-3 账期回款 → FIN-4 利润驾驶舱(依赖 OPS-4 成本)

第四梯队
  DSP-1 多约束可解释路线规划（痛点：黑盒）
  → DSP-2 外协运力池（加盟模式需要）
  → DSP-3 抢派双模

第五梯队（按需，硬件具备才做）
  IOT-1 冷链温控 → IOT-2 重量视频货损 → IOT-3 能源结算
```

> 依赖提醒：**FIN-4 依赖 OPS-4（`pd_task_cost`）先完成**，否则无成本数据算不出利润。
