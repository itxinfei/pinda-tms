<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="订单号" prop="orderId">
          <el-input v-model="query.orderId" placeholder="请输入订单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="异常类型" prop="exceptionType">
          <el-select v-model="query.exceptionType" placeholder="全部类型" clearable style="width: 160px">
            <el-option
              v-for="item in EXCEPTION_TYPE_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态" prop="handleStatus">
          <el-select v-model="query.handleStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待处理" :value="HandleStatus.PENDING" />
            <el-option label="已处理" :value="HandleStatus.HANDLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="id" label="异常ID" min-width="170" show-overflow-tooltip />
          <el-table-column prop="orderId" label="订单号" min-width="170" show-overflow-tooltip />
          <el-table-column label="异常类型" width="100" align="center">
            <template #default="scope">
              <el-tag :type="typeTagType(scope.row.exceptionType)" effect="plain">
                {{ typeLabel(scope.row.exceptionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="异常描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="处理状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.handleStatus === HandleStatus.HANDLED ? 'success' : 'warning'">
                {{ scope.row.handleStatus === HandleStatus.HANDLED ? "已处理" : "待处理" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handleResult" label="处理结果" min-width="180" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.handleResult || "-" }}</template>
          </el-table-column>
          <el-table-column label="处理人" width="110">
            <template #default="scope">{{ scope.row.handlerUser || "-" }}</template>
          </el-table-column>
          <el-table-column prop="handleTime" label="处理时间" width="170">
            <template #default="scope">{{ scope.row.handleTime || "-" }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="登记时间" width="170" />
          <el-table-column label="操作" fixed="right" width="150" align="center">
            <template #default="scope">
              <el-button
                v-if="scope.row.handleStatus === HandleStatus.PENDING"
                type="primary"
                link
                size="small"
                v-hasPerm="['order:exception']"
                @click="openHandle(scope.row as OrderExceptionRecord)"
              >处理</el-button>
              <el-button type="primary" link size="small" @click="openRegister(scope.row.orderId)">再次登记</el-button>
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

      <div class="page-toolbar">
        <el-button type="primary" v-hasPerm="['order:exception']" @click="openRegister()">登记异常</el-button>
      </div>
    </el-card>

    <!-- 登记异常弹窗 -->
    <el-dialog v-model="registerVisible" title="登记订单异常" width="520px" append-to-body destroy-on-close>
      <el-form :model="registerForm" label-width="90px">
        <el-form-item label="订单号" required>
          <el-input v-model="registerForm.orderId" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="异常类型" required>
          <el-select v-model="registerForm.exceptionType" style="width: 100%">
            <el-option
              v-for="item in EXCEPTION_TYPE_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="异常描述" required>
          <el-input
            v-model="registerForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请描述异常情况（滞留原因/破损情况/退回原因等）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRegister">确定</el-button>
      </template>
    </el-dialog>

    <!-- 处理异常弹窗 -->
    <el-dialog v-model="handleVisible" title="处理订单异常" width="520px" append-to-body destroy-on-close>
      <el-descriptions :column="1" border class="handle-desc">
        <el-descriptions-item label="订单号">{{ current?.orderId }}</el-descriptions-item>
        <el-descriptions-item label="异常类型">{{ typeLabel(current?.exceptionType) }}</el-descriptions-item>
        <el-descriptions-item label="异常描述">{{ current?.description }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px" class="handle-form">
        <el-form-item label="处理结果" required>
          <el-input
            v-model="handleResult"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写处理结果（处理措施、责任归属、后续跟进等）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import OrderExceptionAPI, {
  OrderExceptionType,
  HandleStatus,
  type OrderExceptionRecord,
} from "@/api/order-exception";

type TagType = "primary" | "success" | "info" | "warning" | "danger";

/** 异常类型选项 */
const EXCEPTION_TYPE_OPTIONS: { value: OrderExceptionType; label: string; type: TagType }[] = [
  { value: OrderExceptionType.STALLED, label: "滞留", type: "warning" },
  { value: OrderExceptionType.DAMAGED, label: "破损", type: "danger" },
  { value: OrderExceptionType.RETURNED, label: "退回", type: "info" },
  { value: OrderExceptionType.OTHER, label: "其他", type: "primary" },
];

const typeLabel = (type?: number) =>
  EXCEPTION_TYPE_OPTIONS.find((item) => item.value === type)?.label ?? "其他";
const typeTagType = (type?: number): TagType =>
  EXCEPTION_TYPE_OPTIONS.find((item) => item.value === type)?.type ?? "info";

/** 查询与列表状态 */
const query = ref<{
  page: number;
  pageSize: number;
  orderId?: string;
  exceptionType?: OrderExceptionType;
  handleStatus?: HandleStatus;
}>({ page: 1, pageSize: 10 });

const loading = ref(false);
const list = ref<OrderExceptionRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await OrderExceptionAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      orderId: query.value.orderId || undefined,
      exceptionType: query.value.exceptionType,
      handleStatus: query.value.handleStatus,
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

/** 登记弹窗 */
const registerVisible = ref(false);
const submitting = ref(false);
const registerForm = ref<{ orderId: string; exceptionType: OrderExceptionType; description: string }>({
  orderId: "",
  exceptionType: OrderExceptionType.STALLED,
  description: "",
});

function openRegister(orderId?: string) {
  registerForm.value = {
    orderId: orderId ?? "",
    exceptionType: OrderExceptionType.STALLED,
    description: "",
  };
  registerVisible.value = true;
}

async function submitRegister() {
  if (!registerForm.value.orderId.trim()) {
    ElMessage.warning("请输入订单号");
    return;
  }
  if (!registerForm.value.description.trim()) {
    ElMessage.warning("请填写异常描述");
    return;
  }
  submitting.value = true;
  try {
    await OrderExceptionAPI.register(
      registerForm.value.orderId.trim(),
      registerForm.value.exceptionType,
      registerForm.value.description.trim(),
    );
    ElMessage.success("异常登记成功");
    registerVisible.value = false;
    loadList();
  } finally {
    submitting.value = false;
  }
}

/** 处理弹窗 */
const handleVisible = ref(false);
const current = ref<OrderExceptionRecord>();
const handleResult = ref("");

function openHandle(row: OrderExceptionRecord) {
  current.value = row;
  handleResult.value = "";
  handleVisible.value = true;
}

async function submitHandle() {
  if (!handleResult.value.trim()) {
    ElMessage.warning("请填写处理结果");
    return;
  }
  if (!current.value) return;
  submitting.value = true;
  try {
    await OrderExceptionAPI.handle(current.value.id, handleResult.value.trim());
    ElMessage.success("处理完成");
    handleVisible.value = false;
    loadList();
  } finally {
    submitting.value = false;
  }
}

onMounted(loadList);
</script>

<style lang="scss" scoped>
.page-toolbar {
  display: flex;
  justify-content: flex-start;
  margin-top: 12px;
}

.handle-desc {
  margin-bottom: 16px;
}
</style>
