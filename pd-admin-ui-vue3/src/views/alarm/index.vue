<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="运输任务ID" prop="transportTaskId">
              <el-input
                v-model="query.transportTaskId"
                placeholder="精确匹配运输任务ID"
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="告警类型" prop="alarmType">
              <el-select v-model="query.alarmType" placeholder="全部类型" clearable style="width: 160px">
                <el-option-group v-for="group in ALARM_TYPE_GROUPS" :key="group.label" :label="group.label">
                  <el-option v-for="item in group.options" :key="item.key" :value="item.key" :label="item.label" />
                </el-option-group>
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态" prop="status">
              <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="未处理" :value="AlarmHandleStatus.PENDING" />
                <el-option label="已处理" :value="AlarmHandleStatus.HANDLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-tag type="danger" effect="light">未处理 {{ pendingCount }} 条</el-tag>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column label="告警类型" min-width="120" align="center">
            <template #default="scope">
              <el-tag :type="alarmTagType(scope.row.alarmType)" size="small">
                {{ alarmTypeText(scope.row.alarmType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="transportTaskId" label="运输任务ID" min-width="180" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.transportTaskId || "-" }}</template>
          </el-table-column>
          <el-table-column prop="truckId" label="车辆ID" min-width="160" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.truckId || "-" }}</template>
          </el-table-column>
          <el-table-column prop="driverId" label="上报人ID" min-width="160" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.driverId || "-" }}</template>
          </el-table-column>
          <el-table-column label="告警位置(经度,纬度)" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ formatLocation(scope.row as AlarmRecord) }}</template>
          </el-table-column>
          <el-table-column prop="alarmContent" label="告警内容" min-width="220" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.alarmContent || "-" }}</template>
          </el-table-column>
          <el-table-column label="告警时间" min-width="170">
            <template #default="scope">{{ formatTime(scope.row.alarmTime) }}</template>
          </el-table-column>
          <el-table-column label="处理状态" min-width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === AlarmHandleStatus.HANDLED ? 'success' : 'danger'" size="small">
                {{ scope.row.status === AlarmHandleStatus.HANDLED ? "已处理" : "未处理" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="120" align="center">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === AlarmHandleStatus.PENDING"
                type="primary"
                link
                size="small"
                @click="handleMarkHandled(scope.row as AlarmRecord)"
                >标记已处理</el-button
              >
              <span v-else class="alarm-page__done">-</span>
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
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import AlarmAPI from "@/api/alarm";
import {
  AlarmHandleStatus,
  AlarmTypeKey,
  GPS_ALARM_TYPES,
  MANUAL_ALARM_TYPES,
  alarmTypeKeyOf,
  alarmTypeText,
  isGpsAlarm,
  type AlarmRecord,
  type AlarmPageQuery,
} from "@/api/alarm";

/** 告警类型下拉分组（查询传语义码字符串） */
const ALARM_TYPE_GROUPS = [
  { label: "GPS 自动告警", options: GPS_ALARM_TYPES },
  { label: "人工上报", options: MANUAL_ALARM_TYPES },
];

/** 超速用 danger 突出，其余 GPS 用 warning，人工上报用 info */
function alarmTagType(code: number): "danger" | "warning" | "info" {
  if (alarmTypeKeyOf(code) === AlarmTypeKey.SPEED_OVER) {
    return "danger";
  }
  return isGpsAlarm(code) ? "warning" : "info";
}

/** 经纬度均为空时显示 -，后端 BigDecimal 序列化为字符串 */
function formatLocation(row: AlarmRecord): string {
  if (!row.longitude && !row.latitude) {
    return "-";
  }
  return `${row.longitude ?? "-"}, ${row.latitude ?? "-"}`;
}

/**
 * 时间展示兼容：正常为 yyyy-MM-dd HH:mm:ss 字符串；
 * 若 Jackson 输出 ISO 带 T 形式则规整为空格，兜底支持 JSR-310 数组形态
 */
function formatTime(value?: string | number[] | null): string {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0, s = 0] = value;
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(min)}:${pad(s)}`;
  }
  return String(value).replace("T", " ").slice(0, 19);
}

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<AlarmRecord[]>([]);
const total = ref(0);
const pendingCount = ref(0);
const query = ref<AlarmPageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): AlarmPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: AlarmPageQuery = { page, pageSize };
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined) {
      (params as unknown as Record<string, unknown>)[key] = value;
    }
  });
  return params;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await AlarmAPI.page(buildParams());
    list.value = res?.items ?? [];
    total.value = res?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

/** 未处理总数（独立于列表过滤条件，始终查 status=0） */
async function loadPendingCount() {
  const res = await AlarmAPI.page({ page: 1, pageSize: 1, status: AlarmHandleStatus.PENDING });
  pendingCount.value = res?.total ?? 0;
}

function handleSearch() {
  query.value.page = 1;
  loadList();
}

function handleReset() {
  query.value = { page: 1, pageSize: 10 };
  loadList();
}

async function handleMarkHandled(row: AlarmRecord) {
  try {
    await ElMessageBox.confirm(
      `确认将该条${alarmTypeText(row.alarmType)}告警标记为已处理？标记后不可撤销。`,
      "处理确认",
      { type: "warning", confirmButtonText: "标记已处理", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await AlarmAPI.handle(row.id);
    ElMessage.success("已标记为已处理");
    // 筛选"未处理"时，处理后记录会从结果消失；末页仅剩一条则回退一页，避免停在空页
    if (query.value.status === AlarmHandleStatus.PENDING && list.value.length === 1 && query.value.page > 1) {
      query.value.page -= 1;
    }
    loadList();
    loadPendingCount();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "标记失败");
  }
}

onMounted(() => {
  loadList();
  loadPendingCount();
});
</script>

<style scoped lang="scss">
.alarm-page__done {
  color: var(--el-text-color-placeholder);
}
</style>
