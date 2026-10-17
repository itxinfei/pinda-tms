<template>
  <div class="page-container">
    <!-- 页头：标题 + 主操作（刷新） -->
    <div class="page-toolbar board-toolbar">
      <div class="page-toolbar__left">
        <span class="board-title">运输闭环看板</span>
        <span class="board-subtitle">调度 → 分配 → 发车 → 在途 → 到达 → 交付 全链路实时视图</span>
      </div>
      <div class="page-toolbar__right">
        <el-button type="primary" :loading="loading" @click="loadBoard">刷新</el-button>
      </div>
    </div>

    <!-- 错态：重试 -->
    <el-card v-if="loadError" shadow="never" class="board-error">
      <el-empty description="看板数据加载失败">
        <el-button type="primary" @click="loadBoard">重试</el-button>
      </el-empty>
    </el-card>

    <!-- 主体：六阶段流程条 + 状态分布图表 -->
    <div v-else v-loading="loading" element-loading-text="加载中…" class="board-body">
      <el-row :gutter="12" class="stage-row">
        <el-col v-for="(stage, index) in board.stages" :key="stage.key" :xs="12" :sm="8" :md="4">
          <el-card
            shadow="hover"
            class="stage-card"
            role="button"
            tabindex="0"
            :aria-label="`阶段 ${stage.name}，${stage.count ?? 0} 项，点击下钻`"
            @click="openDrill(stage)"
            @keydown.enter.prevent="openDrill(stage)"
            @keydown.space.prevent="openDrill(stage)"
          >
            <div class="stage-card__head">
              <span class="stage-card__index">{{ index + 1 }}</span>
              <span class="stage-card__name">{{ stage.name }}</span>
            </div>
            <div class="stage-card__count">{{ stage.count ?? 0 }}</div>
            <div class="stage-card__hint">点击下钻</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="12" class="chart-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="chart-card">
            <template #header>运输任务状态分布</template>
            <el-empty v-if="!hasTaskChartData" description="暂无数据" />
            <ECharts v-else :options="taskChartOptions" height="300px" @click="onChartClick($event, 'task')" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="chart-card">
            <template #header>运单状态分布</template>
            <el-empty v-if="!hasOrderChartData" description="暂无数据" />
            <ECharts v-else :options="orderChartOptions" height="300px" @click="onChartClick($event, 'transportOrder')" />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 一级下钻抽屉（读操作，居右）：按阶段/状态过滤的任务或运单列表 -->
    <el-drawer v-model="drillVisible" :title="drillTitle" size="860px" append-to-body>
      <div v-loading="drillLoading" element-loading-text="加载中…" class="drill-body">
        <!-- 运输任务列表 -->
        <el-table v-if="drillStage?.drillType === 'task'" :data="taskRows" border size="default">
          <el-table-column prop="id" label="任务号" min-width="150" show-overflow-tooltip />
          <el-table-column label="车次" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.transportTrips?.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="起止机构" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.startAgency?.name || "-" }} → {{ row.endAgency?.name || "-" }}
            </template>
          </el-table-column>
          <el-table-column label="车辆" min-width="100">
            <template #default="{ row }">{{ row.truck?.licensePlate || "-" }}</template>
          </el-table-column>
          <el-table-column label="司机" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ driverNames(row) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag effect="plain" size="small" :type="statusTagType(taskStatusName(row.status))">
                {{ taskStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="planDepartureTime" label="计划发车" width="165" />
          <el-table-column label="操作" fixed="right" width="80" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openTaskDetail(row as TaskTransportRecord)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 运单列表 -->
        <el-table v-else :data="orderRows" border size="default">
          <el-table-column prop="id" label="运单号" min-width="170" show-overflow-tooltip />
          <el-table-column label="寄件人" min-width="150">
            <template #default="{ row }">
              <div>{{ row.order?.senderName || "-" }}</div>
              <div class="page-text-sub">{{ row.order?.senderPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="收件人" min-width="150">
            <template #default="{ row }">
              <div>{{ row.order?.receiverName || "-" }}</div>
              <div class="page-text-sub">{{ row.order?.receiverPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag effect="plain" size="small" :type="statusTagType(orderStatusName(row.status))">
                {{ orderStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" fixed="right" width="80" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openOrderDetail(row as TransportOrderRecord)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="page-pagination drill-pagination">
          <el-pagination
            v-model:current-page="drillQuery.page"
            v-model:page-size="drillQuery.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="drillTotal"
            background
            layout="total, sizes, prev, pager, next"
            @size-change="onDrillSizeChange"
            @current-change="loadDrill"
          />
        </div>
      </div>
    </el-drawer>

    <!-- 二级详情抽屉（读操作，关联对象浏览） -->
    <el-drawer v-model="detailVisible" :title="detailTitle" size="560px" append-to-body modal-class="board-detail-modal">
      <div v-loading="detailLoading" element-loading-text="加载中…" class="detail-body">
        <!-- 运输任务详情 -->
        <template v-if="taskDetail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="任务号" :span="2">{{ taskDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag effect="plain" size="small" :type="statusTagType(taskStatusName(taskDetail.status))">
                {{ taskStatusName(taskDetail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="车次">{{ taskDetail.transportTrips?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="车辆">{{ taskDetail.truck?.licensePlate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="司机">{{ driverNames(taskDetail) }}</el-descriptions-item>
            <el-descriptions-item label="起点机构">{{ taskDetail.startAgency?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="终点机构">{{ taskDetail.endAgency?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="计划发车">{{ taskDetail.planDepartureTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="实际发车">{{ taskDetail.actualDepartureTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="计划到达">{{ taskDetail.planArrivalTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="实际到达">{{ taskDetail.actualArrivalTime || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="taskDetail.transportOrders?.length" class="detail-block">
            <div class="detail-block__title">关联运单（{{ taskDetail.transportOrders.length }}）</div>
            <el-table :data="taskDetail.transportOrders" border size="small">
              <el-table-column prop="id" label="运单号" min-width="170" show-overflow-tooltip />
              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">{{ orderStatusName(row.status) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>

        <!-- 运单详情 -->
        <template v-if="orderDetail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="运单号" :span="2">{{ orderDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag effect="plain" size="small" :type="statusTagType(orderStatusName(orderDetail.status))">
                {{ orderStatusName(orderDetail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ orderDetail.createTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="寄件人">{{ orderDetail.order?.senderName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="寄件电话">{{ orderDetail.order?.senderPhone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="收件人">{{ orderDetail.order?.receiverName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="收件电话">{{ orderDetail.order?.receiverPhone || "-" }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import type { EChartsCoreOption } from "echarts/core";
import ECharts from "@/components/ECharts/index.vue";
import TransportBoardAPI, { type BoardStage, type StatusCount, type TransportBoard } from "@/api/transport-board";
import TransportTaskAPI, {
  type TaskTransportDetail,
  type TaskTransportPageQuery,
  type TaskTransportRecord,
} from "@/api/transport-task";
import TransportOrderAPI, {
  type TransportOrderDetail,
  type TransportOrderPageQuery,
  type TransportOrderRecord,
} from "@/api/transport-order";

/** 图表色板（《14-管理端UI规范》§2.3，图表允许使用色板常量） */
const CHART_COLORS = ["#FF0033", "#00B42A", "#FF7D00", "#F53F3F", "#8E44AD", "#13C2C2", "#FFC53D", "#86909C"];

const EMPTY_BOARD: TransportBoard = { stages: [], taskStatusCounts: [], transportOrderStatusCounts: [] };

/** 看板聚合数据 */
const board = ref<TransportBoard>({ ...EMPTY_BOARD });
const loading = ref(false);
const loadError = ref(false);

async function loadBoard() {
  loading.value = true;
  loadError.value = false;
  try {
    board.value = (await TransportBoardAPI.getBoard()) ?? { ...EMPTY_BOARD };
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

/** 状态中文名映射表：code→name 全部来自后端下发（枚举真源），前端不做状态码硬编码 */
const taskStatusNameMap = computed(() => new Map(board.value.taskStatusCounts.map((i) => [i.code, i.name])));
const orderStatusNameMap = computed(() => new Map(board.value.transportOrderStatusCounts.map((i) => [i.code, i.name])));
const taskStatusName = (code?: number) => (code == null ? "-" : taskStatusNameMap.value.get(code) ?? "未知");
const orderStatusName = (code?: number) => (code == null ? "-" : orderStatusNameMap.value.get(code) ?? "未知");

type TagType = "primary" | "success" | "info" | "warning" | "danger";

/** 状态标签色（《14》状态色映射）：只按后端已下发的状态中文名归类，不引入新状态码 */
const STATUS_TAG_RULES: [RegExp, TagType][] = [
  [/拒收|失败|异常/, "danger"],
  [/取消|作废/, "info"],
  [/签收|完成/, "success"],
  [/待|新建|未/, "warning"],
  [/已装车|到达|进行中|运输|派送|出库|发车|在途/, "primary"],
];
const statusTagType = (name?: string): TagType =>
  STATUS_TAG_RULES.find(([pattern]) => pattern.test(name ?? ""))?.[1] ?? "info";

const driverNames = (row: { drivers?: { name?: string }[] }) =>
  row.drivers?.map((d) => d.name).filter(Boolean).join("、") || "-";

/** 状态分布饼图（0 计数项不进扇区，保持图例干净） */
function buildPieOptions(items: StatusCount[]): EChartsCoreOption {
  const data = items
    .filter((i) => (i.count ?? 0) > 0)
    .map((i) => ({ name: i.name, value: i.count, code: i.code }));
  return {
    color: CHART_COLORS,
    tooltip: { trigger: "item", formatter: "{b}：{c}（{d}%）" },
    legend: { type: "scroll", bottom: 0 },
    series: [
      {
        type: "pie",
        radius: ["42%", "68%"],
        center: ["50%", "44%"],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: "#fff", borderWidth: 2 },
        label: { show: true, formatter: "{b}\n{c}" },
        data,
      },
    ],
  };
}

const hasTaskChartData = computed(() => board.value.taskStatusCounts.some((i) => (i.count ?? 0) > 0));
const hasOrderChartData = computed(() => board.value.transportOrderStatusCounts.some((i) => (i.count ?? 0) > 0));
const taskChartOptions = computed(() => buildPieOptions(board.value.taskStatusCounts));
const orderChartOptions = computed(() => buildPieOptions(board.value.transportOrderStatusCounts));

/** 一级下钻抽屉：阶段卡/图表扇区点击进入 */
const drillVisible = ref(false);
const drillLoading = ref(false);
const drillStage = ref<BoardStage>();
const drillTotal = ref(0);
const drillQuery = ref({ page: 1, pageSize: 10 });
const taskRows = ref<TaskTransportRecord[]>([]);
const orderRows = ref<TransportOrderRecord[]>([]);

const drillTitle = computed(() => `${drillStage.value?.name ?? "明细"}下钻`);

function openDrill(stage: BoardStage) {
  drillStage.value = stage;
  drillVisible.value = true;
  drillQuery.value = { page: 1, pageSize: 10 };
  loadDrill();
}

async function loadDrill() {
  const stage = drillStage.value;
  if (!stage) return;
  drillLoading.value = true;
  try {
    const statuses = stage.drillStatuses ?? [];
    if (stage.drillType === "task") {
      const payload: TaskTransportPageQuery = { page: drillQuery.value.page, pageSize: drillQuery.value.pageSize };
      if (stage.drillField === "status" && statuses.length === 1) {
        payload.status = statuses[0];
      }
      const res = await TransportTaskAPI.page(payload);
      taskRows.value = res?.items ?? [];
      orderRows.value = [];
      drillTotal.value = res?.counts ?? 0;
    } else {
      const payload: TransportOrderPageQuery = { page: drillQuery.value.page, pageSize: drillQuery.value.pageSize };
      if (stage.drillField === "schedulingStatus" && statuses.length === 1) {
        payload.schedulingStatus = statuses[0];
      } else if (stage.drillField === "status" && statuses.length === 1) {
        payload.status = statuses[0];
      } else if (stage.drillField === "status" && statuses.length > 1) {
        payload.statusList = statuses;
      }
      const res = await TransportOrderAPI.page(payload);
      orderRows.value = res?.items ?? [];
      taskRows.value = [];
      drillTotal.value = res?.counts ?? 0;
    }
  } finally {
    drillLoading.value = false;
  }
}

function onDrillSizeChange() {
  drillQuery.value.page = 1;
  loadDrill();
}

/** 图表扇区点击下钻（code 由后端下发，前端不硬编码） */
function onChartClick(params: unknown, type: "task" | "transportOrder") {
  const data = (params as { data?: { code?: number; name?: string } })?.data;
  if (!data?.code) return;
  openDrill({
    key: type === "task" ? "TASK_STATUS" : "ORDER_STATUS",
    name: data.name ?? "状态明细",
    count: 0,
    drillType: type,
    drillField: "status",
    drillStatuses: [data.code],
  });
}

/** 二级详情抽屉 */
const detailVisible = ref(false);
const detailLoading = ref(false);
const taskDetail = ref<TaskTransportDetail>();
const orderDetail = ref<TransportOrderDetail>();
const detailTitle = computed(() =>
  taskDetail.value ? `运输任务详情 · ${taskDetail.value.id}` : orderDetail.value ? `运单详情 · ${orderDetail.value.id}` : "详情"
);

async function openTaskDetail(row: TaskTransportRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  taskDetail.value = undefined;
  orderDetail.value = undefined;
  try {
    taskDetail.value = await TransportTaskAPI.getDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

async function openOrderDetail(row: TransportOrderRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  orderDetail.value = undefined;
  taskDetail.value = undefined;
  try {
    orderDetail.value = await TransportOrderAPI.getDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

onMounted(loadBoard);
</script>

<style lang="scss" scoped>
.board-toolbar {
  margin-bottom: 0;
}

.board-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.board-subtitle {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.board-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: auto;
}

.board-error {
  flex: 1;
}

/* 六阶段流程条 */
.stage-row {
  flex-shrink: 0;
}

.stage-card {
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;

  &:hover {
    transform: translateY(-2px);
  }

  :deep(.el-card__body) {
    padding: 12px 16px;
  }

  &__head {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  &__index {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    font-size: 12px;
    font-weight: 600;
    color: #fff;
    background: var(--el-color-primary);
    border-radius: 50%;
  }

  &__name {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__count {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    line-height: 1;
    color: var(--el-color-primary);
  }

  &__hint {
    margin-top: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

/* 状态分布图表 */
.chart-row {
  flex-shrink: 0;
}

.chart-card {
  :deep(.el-card__body) {
    padding-top: 8px;
  }
}

/* 下钻抽屉 */
.drill-body {
  position: relative;
  min-height: 100%;
}

.drill-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 二级详情抽屉 */
.detail-body {
  position: relative;
  min-height: 100%;
}

.detail-block {
  margin-top: 16px;

  &__title {
    margin: 16px 0 12px;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}
</style>

<style lang="scss">
/* 二级抽屉遮罩降浅（《14》§4.3.1 二级弹层侧滑面板感） */
.board-detail-modal {
  background: var(--el-overlay-color-lighter);
}
</style>
