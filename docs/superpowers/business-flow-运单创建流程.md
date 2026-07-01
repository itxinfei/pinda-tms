# 订单创建运单的职责变更说明

## 文档信息
- **创建日期**: 2026-07-01
- **版本**: V1.0
- **状态**: 已确认

---

## 一、业务流程全景

### 1.1 完整业务流程图

```
客户下单
  ↓
订单创建 (状态: 待取件 PENDING 23000)
  ↓
快递员揽收 (CourierController.detail)
  ├─ 检查订单是否已有运单
  ├─ 如果不存在 → 创建运单 (status=1 CREATED, schedulingStatus=1 TO_BE_SCHEDULED)
  └─ 更新订单状态 (status: 已取件 PICKED_UP 23001)
  ↓
快递员交件 (CourierController.warehousing)
  ├─ 更新订单状态 (status: 网点入库 OUTLETS_WAREHOUSE 23003)
  └─ 更新取派件任务状态 (completed)
  ↓
定时调度任务执行 (DispatchTask.run)
  ├─ 1. 订单分类 (TaskOrderClassifyServiceImpl)
  │   ├─ 查询新订单: status=网点入库(23003), currentAgencyId=当前机构
  │   └─ 查询中转订单: status=运输中(23005), currentAgencyId=当前机构
  ├─ 2. 路线规划 (TaskRoutePlanningServiceImpl)
  ├─ 3. 创建运输任务+关联运单 (BusinessOperationServiceImpl.createTransportOrderTask) ← 我们正在修改的方法
  │   ├─ 新订单 (isNew=true): 检查运单是否存在，不存在则创建
  │   └─ 中转订单 (isNew=false): 直接查询已存在的运单
  ├─ 4. 规划车次车辆司机 (TaskTripsSchedulingServiceImpl)
  └─ 5. 完善运输任务信息+创建司机作业 (BusinessOperationServiceImpl.updateTransportTask)
  ↓
运输任务执行
  ├─ 发车确认 (depart): 待执行(1) → 进行中(2)
  ├─ 到达确认 (arrive): 进行中(2) → 待确认(3)
  ├─ 交付确认 (deliver): 待确认(3) → 已完成(4)
  └─ 状态同步 (syncStatusOnComplete): 运输任务完成 → 运单状态(4) + 订单状态(23009)
```

---

## 二、关键代码位置

### 2.1 揽收时创建运单

**文件**: `pd-web/pd-web-courier/src/main/java/com/itheima/pinda/controller/CourierController.java`

**方法**: `detail(@PathVariable("id") String id, ...)` - 第254-305行

**代码**:
```java
@PutMapping("detail/{id}")
public Result detail(@PathVariable("id") String id, @RequestBody PickupDispatchDetailDTO pickupDispatchDetailDTO) {
    // ... 其他业务逻辑 ...

    // 检查订单是否已有运单
    TransportOrderDTO transportOrderDTO = transportOrderFeign.findByOrderId(pickupDispatchDetailDTO.getOrderNumber());
    log.info("查询是否存在运单:{}", transportOrderDTO);
    if (transportOrderDTO == null || StringUtils.isBlank(transportOrderDTO.getId())) {
        // 不存在则创建运单
        TransportOrderDTO transportDTO = new TransportOrderDTO();
        transportDTO.setOrderId(pickupDispatchDetailDTO.getOrderNumber());
        transportDTO.setStatus(TransportOrderStatus.CREATED.getCode());           // 1-新建
        transportDTO.setSchedulingStatus(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode()); // 1-待调度
        transportOrderFeign.save(transportDTO);
        log.info("不存在运单 创建新运单:{}", transportDTO);
    }

    return Result.ok();
}
```

**说明**:
- 快递员揽收货物时触发
- 如果订单已有运单（理论上不应该有），直接使用
- 如果订单没有运单，创建新的运单

---

### 2.2 调度时创建运单（修复后的代码）

**文件**: `pd-dispatch/src/main/java/com/itheima/pinda/service/impl/BusinessOperationServiceImpl.java`

**方法**: `createTransportOrderTask(List<OrderLineSimpleDTO> orderLineSimpleDTOS)` - 第64-86行

**代码**:
```java
if (orderClassify.isNew()) {
    // 新订单  更新运单信息
    orderClassify.getOrders().forEach(item -> {
        // 创建运单（如果不存在则创建，已存在则直接返回）
        TransportOrderDTO transportOrderDto = transportOrderFeign.findByOrderId(item.getId());
        if (transportOrderDto == null) {
            transportOrderDto = new TransportOrderDTO();
            transportOrderDto.setOrderId(item.getId());
            // 使用运单状态枚举，确保状态一致
            transportOrderDto.setStatus(TransportOrderStatus.CREATED.getCode());
            transportOrderDto.setSchedulingStatus(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode());
            transportOrderDto = transportOrderFeign.save(transportOrderDto);
            log.info("创建运单: {}", transportOrderDto.getId());
        }
        transportOrderIds.add(transportOrderDto.getId());

        // 更新订单状态为待装车
        OrderDTO orderDto = new OrderDTO();
        orderDto.setStatus(OrderStatus.FOR_LOADING.getCode());
        orderFeign.updateById(item.getId(), orderDto);
        log.info("更新订单状态为待装车: {}", item.getId());
    });
}
```

**说明**:
- 定时调度任务执行时触发
- **新订单**（isNew=true）: 检查运单是否存在，不存在则创建
- **中转订单**（isNew=false）: 直接查询已存在的运单（第88-90行）
- 创建/获取运单后，更新订单状态为"待装车"(23004)

---

## 三、为什么需要两处创建逻辑？

### 3.1 原注释的问题

**原注释**: "此处无需创建，由快递员交件时创建运单"

**问题**: 这个注释是**错误的**！

**原因**:
1. **"交件"不是创建运单的时机**
   - `CourierController.warehousing()`方法（交件接口）**并不创建运单**
   - 交件只是更新订单状态为"网点入库"

2. **真正的创建时机是"揽收"**
   - `CourierController.detail()`方法（揽收接口）才是创建运单的地方

### 3.2 为什么调度时还需要检查？

虽然揽收时已经创建了运单，但调度时仍然需要检查并创建，原因如下：

#### 场景1: 调度任务早于揽收执行

```
时间线:
T1: 快递员揽收 (创建运单)
T2: 调度任务执行

实际情况可能是:
T1: 调度任务执行 (订单状态=网点入库)
T2: 快递员揽收 (创建运单) ← 揽收滞后
```

**问题**: 如果调度任务在揽收之前执行，运单还不存在，必须创建。

#### 场景2: 揽收时未创建运单

可能的情况：
- 揽收接口调用失败
- 网络异常导致创建失败
- 历史订单数据迁移，缺少运单

#### 场景3: 异常流程补单

- 订单异常，需要补录运单
- 调度任务提供兜底机制

### 3.3 正确的业务理解

**"快递员交件时创建运单"的真实含义**：

原注释想表达的是：
> "运单应该在揽收环节创建，而不是等到调度环节才创建"

但表述有误，应该是：
> "运单应该在揽收时创建，调度时作为兜底检查"

---

## 四、订单状态流转

### 4.1 新订单状态流转

```
待取件(23000) [PENDING]
  ↓ [揽收]
已取件(23001) [PICKED_UP]
  ↓ [交件]
网点入库(23003) [OUTLETS_WAREHOUSE]
  ↓ [调度] ← 调度任务执行，订单分类查询此状态
待装车(23004) [FOR_LOADING]
  ↓ [装车发运]
运输中(23005) [IN_TRANSIT]
  ↓ [到达网点]
网点出库(23006) [OUTLETS_EX_WAREHOUSE]
  ↓ [派送]
待派送(23007) [TO_BE_DISPATCHED]
  ↓ [开始派送]
派送中(23008) [DISPATCHING]
  ↓ [签收]
已签收(23009) [RECEIVED]
```

### 4.2 运单状态流转

```
新建(1) [CREATED]
  ↓ [调度，已关联运输任务]
待调度(1) [TO_BE_SCHEDULED]
  ↓ [调度完成，已分配车次]
已调度(3) [SCHEDULED]
  ↓ [装车发运]
已装车(2) [LOADED]
  ↓ [到达转运中心]
到达(3) [ARRIVED]
  ↓ [到达终端网点]
到达终端网点(4) [ARRIVED_END]
  ↓ [快递员派送签收]
已签收(5) [RECEIVED]
```

### 4.3 运输任务状态流转

```
待执行(1) [PENDING]
  ↓ [depart() 发车确认]
进行中(2) [IN_PROGRESS]
  ↓ [arrive() 到达确认]
待确认(3) [WAITING_CONFIRM]
  ↓ [deliver() 交付确认]
已完成(4) [COMPLETED]
```

---

## 五、修复后的代码改进

### 5.1 修复前的问题

```java
// ❌ 原代码：硬编码魔法数字
transportOrderDto.setStatus(1);
transportOrderDto.setSchedulingStatus(1);
```

### 5.2 修复后的代码

```java
// ✅ 修复后：使用枚举常量
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderSchedulingStatus;

transportOrderDto.setStatus(TransportOrderStatus.CREATED.getCode());                    // 1-新建
transportOrderDto.setSchedulingStatus(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode()); // 1-待调度
```

### 5.3 改进点

| 改进项 | 修复前 | 修复后 |
|--------|--------|--------|
| **状态值定义** | 硬编码 `1` | 使用枚举 `TransportOrderStatus.CREATED.getCode()` |
| **并发安全** | 无保护 | 数据库唯一索引 `uk_transport_order_order_id` |
| **参数校验** | 无 | 所有方法添加 `StringUtils.isBlank(id)` 校验 |
| **事务控制** | 无 | 添加 `@Transactional(rollbackFor = Exception.class)` |
| **时间记录** | 仅 `updateTime` | 新增 `actualDepartureTime`/`actualArrivalTime`/`actualDeliveryTime` |
| **功能完整性** | `syncStatusOnComplete()` 空实现 | 完整实现运单和订单状态同步 |
| **日志格式** | 不一致 | 统一使用 `{}` 占位符，添加空格分隔 |
| **Lambda表达式** | `item -> item.getId()` | `OrderDTO::getId` 方法引用 |

---

## 六、业务逻辑验证

### 6.1 已确认的业务规则

✅ **规则1**: 揽收时创建运单（CourierController.detail）
- 如果运单不存在，创建新运单
- 状态：CREATED(1), TO_BE_SCHEDULED(1)

✅ **规则2**: 调度时兜底创建运单（BusinessOperationServiceImpl.createTransportOrderTask）
- 如果运单不存在（理论上应该已被揽收时创建），再次创建
- 状态：CREATED(1), TO_BE_SCHEDULED(1)
- 这是**兜底机制**，不是主要流程

✅ **规则3**: 调度完成后更新订单状态
- 新订单: 网点入库(23003) → 待装车(23004)

✅ **规则4**: 运输任务完成后同步状态
- 运输任务: 已完成(4)
- 运单: 到达终端网点(4) [ARRIVED_END]
- 订单: 已签收(23009) [RECEIVED]

### 6.2 待业务方确认的问题

⚠️ **问题1**: 揽收和调度的时间顺序
- 在什么情况下调度会早于揽收执行？
- 这是正常业务场景，还是异常场景？

⚠️ **问题2**: 一个运输任务下的多个运单是否都属于同一个订单？
- 当前代码只更新第一个运单对应的订单状态
- 是否需要遍历更新所有关联订单？

⚠️ **问题3**: 运单状态定义
- 接口注释说"运单状态更新为已完成"
- 但枚举中没有"已完成"，只有"到达终端网点(4)"和"已签收(5)"
- 应该使用哪个状态？

---

## 七、数据库设计

### 7.1 新增唯一索引

**文件**: `docs/sql/V1.0__add_transport_order_unique_index.sql`

```sql
CREATE UNIQUE INDEX uk_transport_order_order_id
ON pd_transport_order (order_id)
WHERE order_id IS NOT NULL;
```

**作用**:
- 防止并发场景下重复创建运单
- 即使代码检查有漏洞，数据库层面保证数据一致性

---

## 八、相关文档

- **代码审查修复报告**: `docs/superpowers/code-review-fixes.md`
- **数据库迁移脚本**: `docs/sql/V1.0__add_transport_order_unique_index.sql`
- **关键代码文件**:
  - `pd-web/pd-web-courier/src/main/java/com/itheima/pinda/controller/CourierController.java`
  - `pd-dispatch/src/main/java/com/itheima/pinda/service/impl/BusinessOperationServiceImpl.java`
  - `pd-dispatch/src/main/java/com/itheima/pinda/task/DispatchTask.java`
  - `pd-dispatch/src/main/java/com/itheima/pinda/service/impl/TaskOrderClassifyServiceImpl.java`

---

**文档维护**: Claude Code
**最后更新**: 2026-07-01
