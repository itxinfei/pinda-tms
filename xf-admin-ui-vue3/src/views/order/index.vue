<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item v-if="drillDate" label="下单日期">
          <el-tag closable type="primary" effect="plain" @close="clearDrillDate">
            {{ drillDate }}（来自仪表盘下钻）
          </el-tag>
        </el-form-item>
        <el-form-item label="订单号" prop="id">
          <el-input v-model="query.id" placeholder="请输入订单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单状态" prop="status">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option
              v-for="item in ORDER_STATUS_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="订单类型" prop="orderType">
          <el-select v-model="query.orderType" placeholder="全部类型" clearable style="width: 150px">
            <el-option
              v-for="item in ORDER_TYPE_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="query.receiverName" placeholder="收件人姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="收件电话" prop="receiverPhone">
          <el-input v-model="query.receiverPhone" placeholder="收件人电话" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="付款方式" prop="paymentMethod">
          <el-select v-model="query.paymentMethod" placeholder="全部方式" clearable style="width: 150px">
            <el-option
              v-for="item in PAYMENT_METHOD_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="danger" plain @click="openCreate">新建订单</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="id" label="订单号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="orderType" label="订单类型" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.orderType === OrderType.INCITY ? 'primary' : 'warning'">
                {{ orderTypeLabel(scope.row.orderType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="收件人" min-width="160">
            <template #default="scope">
              <div>{{ scope.row.receiverName || "-" }}</div>
              <div v-if="scope.row.receiverPhone" class="page-text-sub">{{ scope.row.receiverPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="收件地址" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.receiverAddress || "-" }}</template>
          </el-table-column>
          <el-table-column label="发件人" min-width="150">
            <template #default="scope">
              <div>{{ scope.row.senderName || "-" }}</div>
              <div v-if="scope.row.senderPhone" class="page-text-sub">{{ scope.row.senderPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="付款方式" width="100" align="center">
            <template #default="scope">{{ paymentMethodLabel(scope.row.paymentMethod) }}</template>
          </el-table-column>
          <el-table-column label="金额(元)" width="110" align="right">
            <template #default="scope">{{ formatAmount(scope.row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="订单状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="scope">{{ scope.row.createTime || "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="240" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as OrderRecord)">详情</el-button>
              <el-button type="primary" link size="small" @click="goCargo(scope.row as OrderRecord)">货物</el-button>
              <el-button
                v-if="canPay(scope.row as OrderRecord)"
                v-hasPerm="['order:pay']"
                type="success"
                link
                size="small"
                :loading="payingId === scope.row.id"
                @click="handlePay(scope.row as OrderRecord)"
              >
                支付
              </el-button>
              <el-button
                v-if="canReprice(scope.row.status)"
                v-hasPerm="['order:reprice']"
                type="warning"
                link
                size="small"
                @click="openReprice(scope.row as OrderRecord)"
              >
                改价
              </el-button>
              <el-button
                v-if="canCancel(scope.row.status)"
                type="danger"
                link
                size="small"
                :loading="cancellingId === scope.row.id"
                @click="handleCancel(scope.row as OrderRecord)"
              >
                取消
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 订单详情 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="min(980px, 94%)">
      <div v-loading="detailLoading" class="order-detail">
        <el-result v-if="detailError" icon="error" title="详情加载失败" sub-title="请检查网络后重试">
          <template #extra>
            <el-button type="primary" @click="reloadDetail">重新加载</el-button>
          </template>
        </el-result>
        <template v-else-if="detail">
          <!-- 订单进度：12 级状态机时间轴（T-044） -->
          <div class="order-detail__block">
            <div class="order-detail__title">订单进度</div>
            <el-timeline class="order-timeline">
              <el-timeline-item
                v-for="node in statusTimeline"
                :key="node.code"
                :type="node.type"
                :hollow="node.hollow"
                :timestamp="node.time"
                placement="top"
              >
                {{ node.label }}
              </el-timeline-item>
            </el-timeline>
          </div>

          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="订单号" :span="2">
              {{ detail.id }}
              <CopyButton :text="detail.id" class="order-detail__copy" />
            </el-descriptions-item>
            <el-descriptions-item label="订单类型">{{ orderTypeLabel(detail.orderType) }}</el-descriptions-item>
            <el-descriptions-item label="取件类型">{{ pickupTypeLabel(detail.pickupType) }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ detail.createTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="付款方式">{{ paymentMethodLabel(detail.paymentMethod) }}</el-descriptions-item>
            <el-descriptions-item label="付款状态">{{ paymentStatusLabel(detail.paymentStatus) }}</el-descriptions-item>
            <el-descriptions-item label="金额(元)">{{ formatAmount(detail.amount) }}</el-descriptions-item>
            <el-descriptions-item label="距离(公里)">{{ formatNumber(detail.distance) }}</el-descriptions-item>
            <el-descriptions-item label="预计到达">{{ detail.estimatedArrivalTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="客户ID">
              <template v-if="detail.memberId">
                {{ detail.memberId }}
                <CopyButton :text="detail.memberId" class="order-detail__copy" />
              </template>
              <template v-else>-</template>
            </el-descriptions-item>
            <template v-if="detail.status === OrderStatus.CANCELLED">
              <el-descriptions-item label="取消时间" :span="2">{{ detail.cancelTime || "-" }}</el-descriptions-item>
              <el-descriptions-item label="取消原因" :span="2">{{ detail.cancelReason || "-" }}</el-descriptions-item>
            </template>
          </el-descriptions>

          <div class="order-detail__block">
            <div class="order-detail__title">货物信息</div>
            <CargoPanel
              v-if="detail.id"
              :order-id="detail.id"
              :editable="cargoEditable"
              @change="reloadDetail"
            />
          </div>

          <el-descriptions title="收件信息" :column="2" border class="order-detail__block">
            <el-descriptions-item label="收件人">{{ detail.receiverName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ detail.receiverPhone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">{{ receiverArea }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ detail.receiverAddress || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="发件信息" :column="2" border class="order-detail__block">
            <el-descriptions-item label="发件人">{{ detail.senderName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ detail.senderPhone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">{{ senderArea }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ detail.senderAddress || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div class="order-detail__block">
            <div class="order-detail__title">取派件任务</div>
            <template v-if="pickupDispatchBlocks.length">
              <div v-for="item in pickupDispatchBlocks" :key="item.title" class="order-detail__task-block">
                <div class="order-detail__subtitle">{{ item.title }}</div>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="任务编号">
                    {{ item.task.id }}
                    <CopyButton :text="item.task.id" class="order-detail__copy" />
                  </el-descriptions-item>
                  <el-descriptions-item label="任务状态">{{ taskStatusLabel(item.task.status) }}</el-descriptions-item>
                  <el-descriptions-item label="所属网点">{{ item.task.agency?.name || "-" }}</el-descriptions-item>
                  <el-descriptions-item label="快递员">{{ item.task.courier?.name || "-" }}</el-descriptions-item>
                  <el-descriptions-item label="快递员电话">{{ item.task.courier?.mobile || "-" }}</el-descriptions-item>
                  <el-descriptions-item label="分配状态">{{ assignedStatusLabel(item.task.assignedStatus) }}</el-descriptions-item>
                  <el-descriptions-item label="预计开始">{{ item.task.estimatedStartTime || "-" }}</el-descriptions-item>
                  <el-descriptions-item label="实际开始">{{ item.task.actualStartTime || "-" }}</el-descriptions-item>
                  <el-descriptions-item label="预计完成">{{ item.task.estimatedEndTime || "-" }}</el-descriptions-item>
                  <el-descriptions-item label="实际完成">{{ item.task.actualEndTime || "-" }}</el-descriptions-item>
                  <el-descriptions-item label="备注" :span="2">{{ item.task.mark || "-" }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </template>
            <el-empty v-else description="暂无取派件任务" :image-size="56" />
          </div>

          <el-descriptions
            v-if="detail.transportOrder"
            title="运单信息"
            :column="2"
            border
            class="order-detail__block"
          >
            <el-descriptions-item label="运单号" :span="2">
              {{ detail.transportOrder.id }}
              <CopyButton :text="detail.transportOrder.id" class="order-detail__copy" />
            </el-descriptions-item>
            <el-descriptions-item label="运单状态">{{ transportStatusLabel(detail.transportOrder.status) }}</el-descriptions-item>
            <el-descriptions-item label="调度状态">{{ schedulingStatusLabel(detail.transportOrder.schedulingStatus) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ detail.transportOrder.createTime || "-" }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无运单信息" :image-size="56" class="order-detail__block" />
        </template>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            v-if="detail && canPay(detail)"
            v-hasPerm="['order:pay']"
            type="success"
            :loading="payingId === detail.id"
            @click="handlePay(detail)"
          >
            支付确认
          </el-button>
          <el-button
            v-if="detail && canReprice(detail.status)"
            v-hasPerm="['order:reprice']"
            type="warning"
            @click="openReprice(detail)"
          >
            改价
          </el-button>
          <el-button
            v-if="detail && canCancel(detail.status)"
            type="danger"
            :loading="cancellingId === detail.id"
            @click="handleCancel(detail)"
          >
            取消订单
          </el-button>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 改价（服务端重算运费 + FR-75 决策留痕） -->
    <el-dialog v-model="repriceVisible" title="订单改价" width="480px" :close-on-click-modal="false">
      <el-alert
        type="info"
        :closable="false"
        title="按订单地址与货物明细由服务端重算运费（百度测距 + Drools 计价），金额以重算结果为准"
        style="margin-bottom: 16px"
      />
      <el-form :model="repriceForm" label-width="90px">
        <el-form-item label="订单号">
          <el-input :model-value="repriceTarget?.id" disabled />
        </el-form-item>
        <el-form-item label="当前金额">
          <el-input :model-value="repriceTarget?.amount != null ? `¥${repriceTarget.amount}` : '-'" disabled />
        </el-form-item>
        <el-form-item label="改价理由" required>
          <el-select v-model="repriceForm.reasonEnum" placeholder="请选择改价理由" style="width: 100%">
            <el-option label="成本修正" value="COST_CORRECTION" />
            <el-option label="报价错误" value="QUOTATION_ERROR" />
            <el-option label="客户优惠" value="CUSTOMER_DISCOUNT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repriceVisible = false">取消</el-button>
        <el-button type="primary" :loading="repricing" @click="handleReprice">确认重算</el-button>
      </template>
    </el-dialog>

    <!-- 新建订单（B 端录单，复用客户端 Drools 试算契约） -->
    <el-dialog v-model="createVisible" title="新建订单" width="720px" :close-on-click-modal="false" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" label-width="90px">
        <div class="order-detail__title">发件人</div>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="发件人" required>
              <el-input v-model="createForm.senderName" placeholder="姓名" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话" required>
              <el-input v-model="createForm.senderPhone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在地区" required>
              <el-cascader
                v-model="senderAreaPath"
                :props="areaCascaderProps"
                placeholder="省 / 市 / 区县"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" required>
              <el-input v-model="createForm.senderAddress" placeholder="街道门牌（用于地图算距与计费，请填真实地址）" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="order-detail__title">收件人</div>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="收件人" required>
              <el-input v-model="createForm.receiverName" placeholder="姓名" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话" required>
              <el-input v-model="createForm.receiverPhone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在地区" required>
              <el-cascader
                v-model="receiverAreaPath"
                :props="areaCascaderProps"
                placeholder="省 / 市 / 区县"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" required>
              <el-input v-model="createForm.receiverAddress" placeholder="街道门牌（用于地图算距与计费，请填真实地址）" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="order-detail__title">货物信息</div>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="货物类型" required>
              <el-select v-model="createForm.orderCargoDto.goodsTypeId" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="g in goodsTypeOptions"
                  :key="g.id"
                  :value="g.id"
                  :label="g.name"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="货物名称">
              <el-input v-model="createForm.orderCargoDto.name" placeholder="选填" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="件数" required>
              <el-input-number v-model="createForm.orderCargoDto.quantity" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单件重量" required>
              <el-input-number
                v-model="createForm.orderCargoDto.weight"
                :min="0.01"
                :precision="2"
                :step="1"
                style="width: 100%"
                @change="recalcTotalWeight"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单件体积">
              <el-input-number v-model="createForm.orderCargoDto.volume" :min="0" :precision="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="取件方式" required>
              <el-radio-group v-model="createForm.pickupType">
                <el-radio :value="2">上门取件</el-radio>
                <el-radio :value="1">网点自寄</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式" required>
              <el-radio-group v-model="createForm.paymentMethod">
                <el-radio :value="1">预结</el-radio>
                <el-radio :value="2">到付</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-alert v-if="createPriceHint" :title="createPriceHint" :type="createPriceType" show-icon :closable="false" class="order-create__price" />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <span v-if="createAmount" class="order-create__amount">运费：¥{{ createAmount }}</span>
          <el-button :loading="estimating" @click="handleEstimate">试算运费</el-button>
          <el-button type="primary" :loading="creating" @click="handleCreateSubmit">提交订单</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { CascaderProps } from "element-plus";
import OrderAPI from "@/api/order";
import {
  OrderStatus,
  type OrderRecord,
  type OrderDetail,
  type OrderPageQuery,
  type PickupDispatchTask,
  type OrderCreateParams,
  type OrderCargo,
} from "@/api/order";
import AreaAPI, { type Area } from "@/api/area";
import CommonSimpleAPI, { type GoodsTypeSimple } from "@/api/common-simple";
import CargoPanel from "./components/CargoPanel.vue";
import { TransportOrderStatus, SchedulingStatus } from "@/api/transport-order";
import {
  OrderType,
  ORDER_TYPE_LABELS,
  OrderPaymentMethod,
  ORDER_PAYMENT_METHOD_LABELS,
  OrderPaymentStatus,
  ORDER_PAYMENT_STATUS_LABELS,
  OrderPickupType,
  ORDER_PICKUP_TYPE_LABELS,
} from "@/enums";
import { formatNumber, formatAmount } from "@/utils/format";

/** 查询筛选项：订单类型 / 付款方式（文案与表格标签共用枚举出口，禁止另写） */
const ORDER_TYPE_OPTIONS = [
  { value: OrderType.INCITY, label: ORDER_TYPE_LABELS[OrderType.INCITY] },
  { value: OrderType.OUTCITY, label: ORDER_TYPE_LABELS[OrderType.OUTCITY] },
];
const PAYMENT_METHOD_OPTIONS = [
  { value: OrderPaymentMethod.PRE_PAY, label: ORDER_PAYMENT_METHOD_LABELS[OrderPaymentMethod.PRE_PAY] },
  { value: OrderPaymentMethod.END_PAY, label: ORDER_PAYMENT_METHOD_LABELS[OrderPaymentMethod.END_PAY] },
];

/** 订单状态选项（搜索下拉 + 标签） */
const ORDER_STATUS_OPTIONS: { value: OrderStatus; label: string; type: TagType }[] = [
  { value: OrderStatus.PENDING_PICKUP, label: "待取件", type: "warning" },
  { value: OrderStatus.PICKED_UP, label: "已取件", type: "primary" },
  { value: OrderStatus.SELF_SHIPPED, label: "网点自寄", type: "primary" },
  { value: OrderStatus.INBOUND, label: "网点入库", type: "primary" },
  { value: OrderStatus.PENDING_LOADING, label: "待装车", type: "warning" },
  { value: OrderStatus.TRANSPORTING, label: "运输中", type: "primary" },
  { value: OrderStatus.OUTBOUND, label: "网点出库", type: "primary" },
  { value: OrderStatus.PENDING_DELIVERY, label: "待派送", type: "warning" },
  { value: OrderStatus.DELIVERING, label: "派送中", type: "primary" },
  { value: OrderStatus.SIGNED, label: "已签收", type: "success" },
  { value: OrderStatus.REJECTED, label: "拒收", type: "danger" },
  { value: OrderStatus.CANCELLED, label: "已取消", type: "info" },
];

type TagType = "primary" | "success" | "info" | "warning" | "danger";

const statusLabel = (status?: number) =>
  ORDER_STATUS_OPTIONS.find((item) => item.value === status)?.label ?? "未知";
const statusTagType = (status?: number): TagType =>
  ORDER_STATUS_OPTIONS.find((item) => item.value === status)?.type ?? "info";

const orderTypeLabel = (type?: number) => (type != null ? ORDER_TYPE_LABELS[type as OrderType] : undefined) ?? "-";
const pickupTypeLabel = (type?: number) =>
  (type != null ? ORDER_PICKUP_TYPE_LABELS[type as OrderPickupType] : undefined) ?? "-";
const paymentMethodLabel = (type?: number) =>
  (type != null ? ORDER_PAYMENT_METHOD_LABELS[type as OrderPaymentMethod] : undefined) ?? "-";
const paymentStatusLabel = (type?: number) =>
  (type != null ? ORDER_PAYMENT_STATUS_LABELS[type as OrderPaymentStatus] : undefined) ?? "-";
const transportStatusLabel = (type?: number) =>
  (
    {
      [TransportOrderStatus.CREATED]: "新建",
      [TransportOrderStatus.LOADED]: "已装车",
      [TransportOrderStatus.ARRIVED]: "到达",
      [TransportOrderStatus.AT_TERMINAL]: "到达终端网点",
      [TransportOrderStatus.RECEIVED]: "已签收",
      [TransportOrderStatus.REJECTED]: "拒收",
    } as Record<number, string>
  )[type ?? -1] ?? "-";
const schedulingStatusLabel = (type?: number) =>
  (
    {
      [SchedulingStatus.PENDING]: "待调度",
      [SchedulingStatus.NO_LINE]: "未匹配线路",
      [SchedulingStatus.SCHEDULED]: "已调度",
    } as Record<number, string>
  )[type ?? -1] ?? "-";

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<OrderRecord[]>([]);
const total = ref(0);
const query = ref<OrderPageQuery>({ page: 1, pageSize: 10 });

/** 仪表盘日期下钻：/order?date=2026-10-12，自动转为当天 00:00:00 ~ 23:59:59 时间范围 */
const route = useRoute();
const drillDate = ref<string>("");
if (typeof route.query.date === "string" && route.query.date) {
  const day = route.query.date.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(day)) {
    drillDate.value = day;
    query.value.startTime = `${day} 00:00:00`;
    query.value.endTime = `${day} 23:59:59`;
  }
}

/** 移除日期下钻条件并刷新 */
function clearDrillDate() {
  drillDate.value = "";
  delete query.value.startTime;
  delete query.value.endTime;
  handleSearch();
}

/** 组装请求体：剔除空字符串/空值，仅提交有效过滤条件 */
function buildPayload(): OrderPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const payload: OrderPageQuery = { page, pageSize };
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined) {
      (payload as unknown as Record<string, unknown>)[key] = value;
    }
  });
  return payload;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await OrderAPI.page(buildPayload());
    list.value = res?.items ?? [];
    total.value = res?.counts ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.value.page = 1;
  loadList();
}

/** 切换每页条数后必须回第 1 页，否则高页码会请求空页 */
function handleSizeChange() {
  query.value.page = 1;
  loadList();
}

function handleReset() {
  query.value = { page: 1, pageSize: 10 };
  loadList();
}

/** 非终态订单（1~9）可取消；10已签收/11拒收/12已取消 不显示取消入口 */
const canCancel = (status?: number) => typeof status === "number" && status >= 1 && status <= 9;

/** 取消订单：必填原因（≤200字）；在途单（6~9）提示运单/干线需人工处理；后端 warnings 非空时弹窗列示 */
async function handleCancel(row: OrderRecord) {
  const inTransit = typeof row.status === "number" && row.status >= 6 && row.status <= 9;
  let reason: string;
  try {
    const { value } = await ElMessageBox.prompt(
      inTransit
        ? "该订单处于在途环节，取消后关联运单及干线运输任务不会自动联动，需人工核实处理。请填写取消原因："
        : "取消后未完成的取件/派件任务将一并取消。请填写取消原因：",
      `取消订单 ${row.id}`,
      {
        confirmButtonText: "确认取消",
        cancelButtonText: "再想想",
        type: inTransit ? "warning" : undefined,
        inputType: "textarea",
        inputPlaceholder: "请输入取消原因（必填，不超过200字）",
        inputValidator: (val: string) => {
          const text = (val ?? "").trim();
          if (!text) return "取消原因不能为空";
          if (text.length > 200) return "取消原因不能超过200字";
          return true;
        },
      }
    );
    reason = value.trim();
  } catch {
    // 用户点「再想想」或关闭弹窗，放弃取消
    return;
  }

  // 行级防重复提交；后端校验失败（状态不合法/重复取消等）由请求拦截器统一弹错
  cancellingId.value = row.id;
  try {
    const res = await OrderAPI.cancel(row.id, reason);
    loadList();
    // 若详情抽屉正展示该单，同步刷新取消时间/原因
    if (detailVisible.value && detail.value?.id === row.id) {
      openDetail(row);
    }

    const warnings = res?.warnings ?? [];
    if (warnings.length > 0) {
      ElMessageBox.alert(warnings.map((item) => `· ${item}`).join("\n"), "订单已取消，以下事项需人工处理", {
        type: "warning",
        confirmButtonText: "知道了",
        customStyle: { whiteSpace: "pre-line" },
      });
    } else {
      ElMessage.success("订单已取消");
    }
  } catch {
    // 失败已由拦截器统一提示，不刷新列表（按钮 loading 由 finally 解除）
  } finally {
    cancellingId.value = undefined;
  }
}

/** 取消中订单 id：列表行与抽屉底部按钮共用防重 */
const cancellingId = ref<string>();

// ===== 支付确认 / 改价 =====
/** 未付（paymentStatus=1）且非终态（1~9）可发起支付确认 */
const canPay = (row: OrderRecord) => row.paymentStatus === 1 && canCancel(row.status);
/** 非终态订单（1~9）可改价 */
const canReprice = (status?: number) => canCancel(status);

const payingId = ref<string>();

/** 支付确认：状态流转+支付时间落库 */
async function handlePay(row: OrderRecord) {
  try {
    await ElMessageBox.confirm(
      `确认订单 ${row.id} 已收到款项 ¥${row.amount ?? "-"}？确认后订单状态与支付时间将落库。`,
      "支付确认",
      { confirmButtonText: "确认已收款", cancelButtonText: "取消", type: "warning" }
    );
  } catch {
    return;
  }
  payingId.value = row.id;
  try {
    await OrderAPI.pay(row.id);
    ElMessage.success("支付确认成功");
    loadList();
    if (detailVisible.value && detail.value?.id === row.id) {
      openDetail(row);
    }
  } catch {
    // 失败已由拦截器统一提示
  } finally {
    payingId.value = undefined;
  }
}

const repriceVisible = ref(false);
const repricing = ref(false);
const repriceTarget = ref<OrderRecord>();
const repriceForm = ref<{ reasonEnum?: string }>({});

function openReprice(row: OrderRecord) {
  repriceTarget.value = row;
  repriceForm.value = {};
  repriceVisible.value = true;
}

/** 改价提交：服务端按地址+货物重算，返回新金额并留痕 */
async function handleReprice() {
  if (!repriceForm.value.reasonEnum) {
    ElMessage.warning("请选择改价理由");
    return;
  }
  repricing.value = true;
  try {
    const res = await OrderAPI.reprice(repriceTarget.value!.id, repriceForm.value.reasonEnum);
    repriceVisible.value = false;
    loadList();
    if (detailVisible.value && detail.value?.id === repriceTarget.value?.id) {
      openDetail(repriceTarget.value!);
    }
    ElMessage.success(`改价成功，重算后金额 ¥${res?.amount ?? "?"}`);
  } catch {
    // 失败已由拦截器统一提示（含决策理由校验/运费重算失败）
  } finally {
    repricing.value = false;
  }
}

// ===== 新建订单（B 端录单） =====
const createVisible = ref(false);
const creating = ref(false);
const estimating = ref(false);
const createAmount = ref("");
const createPriceHint = ref("");
const createPriceType = ref<"info" | "warning" | "success">("info");
const goodsTypeOptions = ref<GoodsTypeSimple[]>([]);
const senderAreaPath = ref<number[]>([]);
const receiverAreaPath = ref<number[]>([]);

const emptyCargo = (): OrderCargo => ({
  goodsTypeId: "",
  quantity: 1,
  weight: 1,
  totalWeight: 1,
});

const createForm = ref<OrderCreateParams>({
  pickupType: 2,
  paymentMethod: 1,
  senderName: "",
  senderPhone: "",
  senderAddress: "",
  receiverName: "",
  receiverPhone: "",
  receiverAddress: "",
  orderCargoDto: emptyCargo(),
});

/** 省/市/县三级懒加载（国标区划，全局共享） */
const areaCascaderProps: CascaderProps = {
  lazy: true,
  lazyLoad: async (node, resolve) => {
    try {
      const children: Area[] = await AreaAPI.children(node.level === 0 ? undefined : Number(node.value));
      resolve(children.map((a) => ({ value: a.id, label: a.name, leaf: node.level >= 2 })));
    } catch {
      resolve([]);
    }
  },
};

function openCreate() {
  createVisible.value = true;
  createPriceHint.value = "";
  createAmount.value = "";
  if (goodsTypeOptions.value.length === 0) {
    CommonSimpleAPI.goodsTypeOptions()
      .then((list) => {
        goodsTypeOptions.value = list ?? [];
      })
      .catch(() => {
        ElMessage.error("货物类型加载失败，请稍后重试");
      });
  }
}

function resetCreateForm() {
  createForm.value = {
    pickupType: 2,
    paymentMethod: 1,
    senderName: "",
    senderPhone: "",
    senderAddress: "",
    receiverName: "",
    receiverPhone: "",
    receiverAddress: "",
    orderCargoDto: emptyCargo(),
  };
  senderAreaPath.value = [];
  receiverAreaPath.value = [];
  createAmount.value = "";
  createPriceHint.value = "";
}

function recalcTotalWeight() {
  const q = createForm.value.orderCargoDto.quantity ?? 0;
  const w = createForm.value.orderCargoDto.weight ?? 0;
  createForm.value.orderCargoDto.totalWeight = Number((q * w).toFixed(2));
}

/** 组装请求体（区划 id 从级联取，金额/状态由服务端回填） */
function buildCreatePayload(): OrderCreateParams | null {
  recalcTotalWeight();
  const f = createForm.value;
  const missing: string[] = [];
  if (!f.senderName) missing.push("发件人");
  if (!f.senderPhone) missing.push("发件人电话");
  if (senderAreaPath.value.length < 3) missing.push("发件地区");
  if (!f.senderAddress) missing.push("发件详细地址");
  if (!f.receiverName) missing.push("收件人");
  if (!f.receiverPhone) missing.push("收件人电话");
  if (receiverAreaPath.value.length < 3) missing.push("收件地区");
  if (!f.receiverAddress) missing.push("收件详细地址");
  if (!f.orderCargoDto.goodsTypeId) missing.push("货物类型");
  if (!f.orderCargoDto.totalWeight || f.orderCargoDto.totalWeight <= 0) missing.push("货物重量");
  if (missing.length > 0) {
    ElMessage.warning(`请填写：${missing.join("、")}`);
    return null;
  }
  return {
    pickupType: f.pickupType,
    paymentMethod: f.paymentMethod,
    senderProvinceId: String(senderAreaPath.value[0]),
    senderCityId: String(senderAreaPath.value[1]),
    senderCountyId: String(senderAreaPath.value[2]),
    senderAddress: f.senderAddress,
    senderName: f.senderName,
    senderPhone: f.senderPhone,
    receiverProvinceId: String(receiverAreaPath.value[0]),
    receiverCityId: String(receiverAreaPath.value[1]),
    receiverCountyId: String(receiverAreaPath.value[2]),
    receiverAddress: f.receiverAddress,
    receiverName: f.receiverName,
    receiverPhone: f.receiverPhone,
    orderCargoDto: { ...f.orderCargoDto },
  };
}

async function handleEstimate() {
  const payload = buildCreatePayload();
  if (!payload) return;
  estimating.value = true;
  try {
    const msg = await OrderAPI.estimate(payload);
    // xf-oms 契约：参数不全返回 null（响应空）、货物缺失返回空 Map、地址解析失败 amount="0"+errorMsg
    if (msg == null || typeof msg !== "object" || Object.keys(msg).length === 0) {
      createAmount.value = "";
      createPriceHint.value = "运费试算失败：请确认货物类型与重量已填写完整";
      createPriceType.value = "warning";
      return;
    }
    if (msg.errorMsg || msg.amount === "0") {
      createAmount.value = "";
      createPriceHint.value = msg.errorMsg || "无法计算运费：请检查地址是否真实有效";
      createPriceType.value = "warning";
      return;
    }
    if (msg.amount) {
      createAmount.value = msg.amount;
      createPriceHint.value = "";
      createPriceType.value = "success";
    }
  } catch {
    createAmount.value = "";
    createPriceHint.value = "运费试算请求失败，请稍后重试";
    createPriceType.value = "warning";
  } finally {
    estimating.value = false;
  }
}

async function handleCreateSubmit() {
  const payload = buildCreatePayload();
  if (!payload) return;
  creating.value = true;
  try {
    const created = await OrderAPI.create(payload);
    if (!created || !created.id) {
      ElMessage.error("订单创建失败：请核对必填信息后重试（货物明细与地址必填）");
      return;
    }
    ElMessage.success(`订单创建成功：${created.id}`);
    createVisible.value = false;
    handleSearch();
  } catch {
    ElMessage.error("订单创建请求失败，请稍后重试");
  } finally {
    creating.value = false;
  }
}
// ===== 新建订单 end =====

/** 详情 */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailError = ref(false);
const detail = ref<OrderDetail>();
/** 当前查看的行：详情失败时供"重新加载"复用 */
const detailRow = ref<OrderRecord>();

async function openDetail(row: OrderRecord) {
  detailRow.value = row;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  detailError.value = false;
  try {
    detail.value = await OrderAPI.getDetail(row.id);
  } catch {
    // 抽屉内展示错误态与重试入口，错误 toast 由拦截器统一处理
    detailError.value = true;
  } finally {
    detailLoading.value = false;
  }
}

function reloadDetail() {
  if (detailRow.value) {
    openDetail(detailRow.value);
  }
}

/** 跳转货物维护页（列表操作列；详情抽屉内已内联货物面板，无需跳转） */
const router = useRouter();
function goCargo(row: OrderRecord) {
  router.push({ path: "/order/cargoEdit", query: { orderId: row.id, orderNo: row.id } });
}

/** 终态订单（已签收/已拒收/已取消）货物明细只读，避免改单影响已结算运单 */
const ORDER_FINAL_STATUS: number[] = [OrderStatus.SIGNED, OrderStatus.REJECTED, OrderStatus.CANCELLED];
const cargoEditable = computed(() => {
  const status = detail.value?.status;
  return status !== undefined && !ORDER_FINAL_STATUS.includes(status);
});

const joinArea = (...areas: ({ name?: string } | undefined)[]) =>
  areas.map((area) => area?.name).filter(Boolean).join(" / ") || "-";
const receiverArea = computed(() =>
  joinArea(detail.value?.receiverProvince, detail.value?.receiverCity, detail.value?.receiverCounty)
);
const senderArea = computed(() =>
  joinArea(detail.value?.senderProvince, detail.value?.senderCity, detail.value?.senderCounty)
);

/** 取派件任务状态/分配状态文案 */
const taskStatusLabel = (status?: number) =>
  ({ 1: "待执行", 2: "进行中", 3: "待确认", 4: "已完成", 5: "已取消" } as Record<number, string>)[status ?? -1] ?? "-";
const assignedStatusLabel = (status?: number) =>
  ({ 1: "未分配", 2: "已分配", 3: "待人工分配" } as Record<number, string>)[status ?? -1] ?? "-";

/** 订单进度时间轴（T-044）：正常链路 1~10 顺序推进；11 拒收 / 12 已取消为终态分支 */
type TimelineNode = { code: OrderStatus; label: string; time: string; type: "success" | "primary" | "danger" | "info"; hollow: boolean };

const STATUS_FLOW: { code: OrderStatus; label: string }[] = [
  { code: OrderStatus.PENDING_PICKUP, label: "待取件" },
  { code: OrderStatus.PICKED_UP, label: "已取件" },
  { code: OrderStatus.SELF_SHIPPED, label: "网点自寄" },
  { code: OrderStatus.INBOUND, label: "网点入库" },
  { code: OrderStatus.PENDING_LOADING, label: "待装车" },
  { code: OrderStatus.TRANSPORTING, label: "运输中" },
  { code: OrderStatus.OUTBOUND, label: "网点出库" },
  { code: OrderStatus.PENDING_DELIVERY, label: "待派送" },
  { code: OrderStatus.DELIVERING, label: "派送中" },
  { code: OrderStatus.SIGNED, label: "已签收" },
];

/** 从订单/取派件任务/运单既有时间字段推断各节点时间（无状态流水表，尽力展示） */
const statusTimeMap = computed<Partial<Record<number, string>>>(() => {
  const d = detail.value;
  if (!d) return {};
  const map: Partial<Record<number, string>> = {};
  if (d.createTime) map[OrderStatus.PENDING_PICKUP] = d.createTime;
  if (d.taskPickup?.actualStartTime) map[OrderStatus.PICKED_UP] = d.taskPickup.actualStartTime;
  // 运单创建发生在出库发运环节，早于实际运输开始；无运输起始时间字段，暂借其锚定运输中节点（尽力推断）
  if (d.transportOrder?.createTime) map[OrderStatus.TRANSPORTING] = d.transportOrder.createTime;
  if (d.taskDispatch?.actualStartTime) map[OrderStatus.DELIVERING] = d.taskDispatch.actualStartTime;
  if (d.taskDispatch?.actualEndTime) map[OrderStatus.SIGNED] = d.taskDispatch.actualEndTime;
  return map;
});

const statusTimeline = computed<TimelineNode[]>(() => {
  const d = detail.value;
  if (!d) return [];
  const cur = d.status;
  const terminal = cur === OrderStatus.REJECTED || cur === OrderStatus.CANCELLED;

  // 2 已取件（上门取件链）与 3 网点自寄（自寄链）是互斥入口，按取件方式只保留实际链路，避免两节点同时点亮
  const flow = STATUS_FLOW.filter(({ code }) => {
    if (code === OrderStatus.PICKED_UP) return d.pickupType !== OrderPickupType.NO_PICKUP;
    if (code === OrderStatus.SELF_SHIPPED) return d.pickupType !== OrderPickupType.PICKUP;
    return true;
  });
  const timeMap = statusTimeMap.value;

  // 链路中断位置：非终态 = 当前状态节点；终态(11/12)不在正常链内，
  // 以最后一个有真实时间戳的锚点为界——无状态流水表，不能按状态码大小比较，
  // 否则待取件阶段就取消的单会把"已签收"也标绿
  let breakIndex = -1;
  if (!terminal) {
    breakIndex = flow.findIndex(({ code }) => code === cur);
  } else {
    flow.forEach(({ code }, index) => {
      if (timeMap[code]) breakIndex = index;
    });
  }

  const nodes: TimelineNode[] = flow.map(({ code, label }, index) => {
    const time = timeMap[code] ?? "";
    if (index < breakIndex) {
      // 已越过节点：即使无时间字段，链路顺序也保证其已发生
      return { code, label, time, type: "success", hollow: false };
    }
    if (index === breakIndex) {
      // 非终态：当前节点主色进行中；终态：最后真实发生的节点绿色收尾
      return { code, label, time, type: terminal ? "success" : "primary", hollow: false };
    }
    // 中断点之后：未发生，全部置灰空心
    return { code, label, time, type: "info", hollow: true };
  });

  if (terminal && typeof cur === "number") {
    nodes.push({
      code: cur,
      label: ORDER_STATUS_OPTIONS.find((item) => item.value === cur)?.label ?? "未知",
      // 取消节点时间取服务端落库的 cancelTime；拒收暂无对应时间字段
      time: cur === OrderStatus.CANCELLED ? (d.cancelTime ?? "") : "",
      type: "danger",
      hollow: false,
    });
  }
  return nodes;
});

/** 详情中的取件/派件任务块 */
const pickupDispatchBlocks = computed(() => {
  const blocks: { title: string; task: PickupDispatchTask }[] = [];
  if (detail.value?.taskPickup) {
    blocks.push({ title: "取件任务", task: detail.value.taskPickup });
  }
  if (detail.value?.taskDispatch) {
    blocks.push({ title: "派件任务", task: detail.value.taskDispatch });
  }
  return blocks;
});

onMounted(loadList);
</script>

<style lang="scss" scoped>
/* 订单详情抽屉：容器作为 loading 遮罩的定位上下文；各描述区块之间留白；自定义区块标题与 el-descriptions 默认标题视觉对齐 */
.order-detail {
  position: relative;
  min-height: 100%;
}

.order-detail__block {
  margin-top: 16px;
}

.order-detail__title {
  margin: 16px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

/* 复制按钮贴在编号/ID 文本之后，去除按钮自身的高度与左右内边距 */
.order-detail__copy {
  height: auto;
  padding: 0;
  margin-left: 4px;
}

/* 取件 / 派件两个任务子块 */
.order-detail__task-block {
  margin-top: 12px;

  &:first-child {
    margin-top: 0;
  }
}

.order-detail__subtitle {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 订单进度时间轴：收紧节点纵向间距，与描述区块视觉密度一致 */
.order-timeline {
  padding-left: 4px;

  :deep(.el-timeline-item) {
    padding-bottom: 8px;
  }

  :deep(.el-timeline-item__content) {
    font-size: 14px;
  }
}

/* 新建订单弹窗：运费金额强调展示，与 footer 按钮同行 */
.order-create__amount {
  margin-right: auto;
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-color-danger);
}

.order-create__price {
  margin-bottom: 12px;
}
</style>
