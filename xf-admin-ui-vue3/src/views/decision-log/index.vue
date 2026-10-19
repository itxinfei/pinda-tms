<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="决策类型" prop="businessType">
          <el-select v-model="query.businessType" placeholder="全部类型" clearable style="width: 160px">
            <el-option
              v-for="(label, value) in DECISION_TYPE_LABELS"
              :key="value"
              :value="value"
              :label="label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务单据ID" prop="businessId">
          <el-input
            v-model="query.businessId"
            placeholder="订单 id"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column label="决策类型" width="110" align="center">
            <template #default="scope">
              <el-tag :type="typeTagType(scope.row.businessType)" effect="plain">
                {{ DECISION_TYPE_LABELS[scope.row.businessType as DecisionType] ?? scope.row.businessType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="businessId" label="业务单据ID" min-width="180" show-overflow-tooltip />
          <el-table-column label="枚举理由" min-width="150">
            <template #default="scope">
              {{ reasonLabel(scope.row.businessType, scope.row.reasonEnum) }}
            </template>
          </el-table-column>
          <el-table-column prop="reasonDetail" label="补充说明" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.reasonDetail || "-" }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="180" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.operator || "-" }}</template>
          </el-table-column>
          <el-table-column prop="operateTime" label="操作时间" width="170" />
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

      <!-- 监管导出：按类型/单据/时间范围导出 CSV -->
      <div class="page-toolbar">
        <el-form inline>
          <el-form-item label="导出时间范围">
            <el-date-picker
              v-model="exportRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              v-hasPerm="'finance:decision:export'"
              type="primary"
              :loading="exporting"
              @click="handleExport"
            >
              导出 CSV
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import DecisionLogAPI, {
  DECISION_TYPE_LABELS,
  REASON_ENUMS,
  type DecisionLogRecord,
  type DecisionType,
} from "@/api/decision-log";

defineOptions({ name: "DecisionLog" });

type TagType = "primary" | "success" | "info" | "warning" | "danger";

/** 决策类型 → tag 样式（改价=成本类警示、取消=高风险、改派=调度中性） */
const TYPE_TAG: Record<string, TagType> = {
  REPRICE: "warning",
  CANCEL: "danger",
  REASSIGN: "primary",
};

const typeTagType = (type: string): TagType => TYPE_TAG[type] ?? "info";

/** 枚举理由中文（白名单外落库的脏值原码展示，不吞不造） */
function reasonLabel(businessType: string, reasonEnum: string): string {
  const label = REASON_ENUMS[businessType as DecisionType]?.[reasonEnum];
  return label ? `${reasonEnum}（${label}）` : reasonEnum;
}

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; businessType?: DecisionType; businessId?: string }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const list = ref<DecisionLogRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await DecisionLogAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      businessType: query.value.businessType || undefined,
      businessId: query.value.businessId?.trim() || undefined,
    });
    list.value = res?.records ?? [];
    total.value = res?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.value.page = 1;
  loadList();
}

function handleReset() {
  query.value = { page: 1, pageSize: 10 };
  loadList();
}

/** 导出（时间范围仅作用于导出，page 端点无时间过滤参数） */
const exportRange = ref<[string, string] | null>(null);
const exporting = ref(false);

async function handleExport() {
  exporting.value = true;
  try {
    const [beginDate, endDate] = exportRange.value ?? [];
    const rows = await DecisionLogAPI.export({
      businessType: query.value.businessType || undefined,
      businessId: query.value.businessId?.trim() || undefined,
      beginTime: beginDate ? `${beginDate} 00:00:00` : undefined,
      endTime: endDate ? `${endDate} 23:59:59` : undefined,
    });
    if (!rows || rows.length === 0) {
      ElMessage.warning("所选条件下没有留痕记录");
      return;
    }
    downloadCsv(rows);
    ElMessage.success(`已导出 ${rows.length} 条留痕`);
  } finally {
    exporting.value = false;
  }
}

/** 行数组 → CSV（BOM 头保证 Excel 中文不乱码） */
function downloadCsv(rows: { operateTime: string; businessType: string; businessId: string | number; reasonEnum: string; reasonDetail?: string; operator?: number | string }[]) {
  const header = ["操作时间", "决策类型", "业务单据ID", "枚举理由", "补充说明", "操作人"];
  const body = rows.map((row) =>
    [
      row.operateTime,
      DECISION_TYPE_LABELS[row.businessType as DecisionType] ?? row.businessType,
      row.businessId,
      reasonLabel(row.businessType, row.reasonEnum),
      row.reasonDetail ?? "",
      row.operator ?? "",
    ]
      .map((cell) => `"${String(cell).replace(/"/g, '""')}"`)
      .join(","),
  );
  const csv = `﻿${[header.join(","), ...body].join("\r\n")}`;
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  const stamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, "");
  link.download = `决策留痕_${stamp}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

onMounted(loadList);
</script>
