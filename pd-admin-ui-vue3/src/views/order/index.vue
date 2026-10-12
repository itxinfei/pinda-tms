<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
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
          <el-table-column label="操作" fixed="right" width="190" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as OrderRecord)">详情</el-button>
              <el-button type="primary" link size="small" @click="goCargo(scope.row as OrderRecord)">货物</el-button>
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
    <el-drawer v-model="detailVisible" title="订单详情" size="min(620px, 92%)">
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
            <el-empty
              description="货物明细在「货物维护」页查看与维护（新增 / 编辑 / 删除）"
              :image-size="56"
            >
              <el-button type="primary" @click="goCargoFromDetail">管理货物</el-button>
            </el-empty>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import OrderAPI from "@/api/order";
import { OrderStatus, type OrderRecord, type OrderDetail, type OrderPageQuery, type PickupDispatchTask } from "@/api/order";
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
  { value: OrderStatus.PENDING_PICKUP, label: "待取件", type: "info" },
  { value: OrderStatus.PICKED_UP, label: "已取件", type: "primary" },
  { value: OrderStatus.SELF_SHIPPED, label: "网点自寄", type: "primary" },
  { value: OrderStatus.INBOUND, label: "网点入库", type: "primary" },
  { value: OrderStatus.PENDING_LOADING, label: "待装车", type: "warning" },
  { value: OrderStatus.TRANSPORTING, label: "运输中", type: "warning" },
  { value: OrderStatus.OUTBOUND, label: "网点出库", type: "warning" },
  { value: OrderStatus.PENDING_DELIVERY, label: "待派送", type: "warning" },
  { value: OrderStatus.DELIVERING, label: "派送中", type: "warning" },
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

/** 跳转货物编辑页（列表操作列 / 详情抽屉共用） */
const router = useRouter();
function goCargo(row: OrderRecord) {
  router.push({ path: "/order/cargoEdit", query: { orderId: row.id, orderNo: row.id } });
}
function goCargoFromDetail() {
  if (detail.value?.id) {
    router.push({ path: "/order/cargoEdit", query: { orderId: detail.value.id, orderNo: detail.value.id } });
  }
}

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
  margin-left: 4px;
  padding: 0;
  height: auto;
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
    padding-bottom: 6px;
  }

  :deep(.el-timeline-item__content) {
    font-size: 14px;
  }
}
</style>
